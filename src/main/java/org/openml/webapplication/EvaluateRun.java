package org.openml.webapplication;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.ArrayUtils;
import org.json.JSONObject;
import org.openml.apiconnector.algorithms.Conversion;
import org.openml.apiconnector.algorithms.TaskInformation;
import org.openml.apiconnector.io.ApiException;
import org.openml.apiconnector.io.HttpConnector;
import org.openml.apiconnector.xml.DataSetDescription;
import org.openml.apiconnector.xml.EvaluationRequest;
import org.openml.apiconnector.xml.EvaluationScore;
import org.openml.apiconnector.xml.Run;
import org.openml.apiconnector.xml.RunEvaluation;
import org.openml.apiconnector.xml.RunTrace;
import org.openml.apiconnector.xml.Task;
import org.openml.apiconnector.xml.Task.Input.Data_set;
import org.openml.apiconnector.xstream.XstreamXmlMapping;
import org.openml.webapplication.evaluate.EvaluateBatchPredictions;
import org.openml.webapplication.evaluate.EvaluateStreamPredictions;
import org.openml.webapplication.evaluate.EvaluateSurvivalAnalysisPredictions;
import org.openml.webapplication.evaluate.PredictionEvaluator;
import org.openml.webapplication.evaluate.TaskType;
import org.openml.webapplication.settings.ApiErrorMapping;
import org.openml.webapplication.settings.Settings;
import org.openml.weka.io.OpenmlWekaConnector;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;

import com.thoughtworks.xstream.XStream;

public class EvaluateRun {
	private final XStream xstream;
	private final OpenmlWekaConnector apiconnector;
	private final int MAX_LENGTH_WARNING = 1024;
	
	public EvaluateRun(OpenmlWekaConnector ac) throws Exception {
		this(ac, null, "normal", null, null, null, null);
	}
	
	public EvaluateRun(OpenmlWekaConnector ac, Integer run_id, String evaluationMode, int[] ttids, String taskIds, String tag, Integer uploaderId) throws Exception {
		apiconnector = ac;
		xstream = XstreamXmlMapping.getInstance();
		if(run_id != null) {
			evaluate(run_id);
		} else {
			try {
				// while loop will be broken when when there are no runs left on server (catch)
				Map<String, String> filters = new TreeMap<>();
				if (ttids != null) {
					String ttidString = Arrays.toString(ttids).replaceAll(" ", "").replaceAll("\\[", "").replaceAll("\\]", "");
					filters.put("ttid", ttidString);
				}
				if (taskIds != null) {
					filters.put("task", taskIds);
				}
				if (tag != null) {
					filters.put("tag", tag);
				}
				if (uploaderId != null) {
					filters.put("uploader", "" + uploaderId);
				}
				
				// this is the loop that keeps the evaluation engine getting new unevaluated runs
				while(true) {
					int numRequests = 1000; // this number must be high to keep the evaluation engine fast
					EvaluationRequest er = ac.evaluationRequest(Settings.EVALUATION_ENGINE_ID, evaluationMode, numRequests, filters);
					Conversion.log("INFO", "Evaluate Run", "Obtained " + er.getRuns().length + " unevaluated runs");
					
					// this loops over the unevaluated runs that we obtained with the evaluation request.
					for (Run r : er.getRuns()) {
						run_id = r.getRun_id();
						Conversion.log("INFO", "Evaluate Run", "Downloading run " + run_id);
						evaluate(run_id);
					}
				}
			} catch(ApiException e) {
				if (e.getCode() == ApiErrorMapping.NO_UNEVALUATED_RUNS) {
					Conversion.log( "OK", "Process Run", "No more runs to evaluate. Api response: " + e.getMessage());
				} else {
					throw e;
				}
			}
		}
	}
	
	public void evaluate(int runId) throws Exception {
		Conversion.log("OK", "Process Run", "Start processing run: " + runId);
		final DataSetDescription dataset;
		final Run runServer = apiconnector.runGet(runId);
		final Task task = apiconnector.taskGet(runServer.getTask_id());
		if (!ArrayUtils.contains(Settings.SUPPORTED_TASK_TYPES_EVALUATION, task.getTask_type_id())) {
			throw new Exception("Task type not supported: " + task.getTask_type());
		}
		
		final Map<String, Run.Data.File> runFiles = runServer.getOutputFileAsMap();
		final Data_set source_data = TaskInformation.getSourceData(task);
		final Integer dataset_id = source_data.getLabeled_data_set_id() != null ? source_data.getLabeled_data_set_id() : source_data.getData_set_id();
		final int task_id = runServer.getTask_id();
		
		PredictionEvaluator predictionEvaluator;
		RunEvaluation runevaluation = new RunEvaluation(runId, Settings.EVALUATION_ENGINE_ID);
		RunTrace trace = null;
		
		try {

			Conversion.log("OK", "Process Run", "Task: " + task_id + "; dataset id: " + source_data.getData_set_id());
			
			
			if(runFiles.get("description") == null) {
				runevaluation.setError("Run description file not present. ", MAX_LENGTH_WARNING);
				
				int receivedId = apiconnector.runEvaluate(runevaluation);
				Conversion.log("Error", "Process Run", "Run processed, but with error: " + receivedId);
				return;
			}
			
			if(runFiles.get("predictions") == null && runFiles.get("subgroups") == null && runFiles.get("predictions_0") == null) { // TODO: this is currently true, but later on we might have tasks that do not require evaluations!
				runevaluation.setError("Required output files not present (e.g., arff predictions). ", MAX_LENGTH_WARNING);
				
				int receivedId = apiconnector.runEvaluate(runevaluation);
				Conversion.log("Error", "Process Run", "Run processed, but with error: " + receivedId);
				return;
			}
			
			if (runFiles.get("trace") != null) {
				trace = traceToXML(runFiles.get("trace").getFileId(), task_id, runId);
			}
			String description_url = apiconnector.getOpenmlFileUrl(runFiles.get("description").getFileId(), "Run_" + runId + "_description.xml").toString();
			// we do not get the "real" run, as this does not contain user defined evaluations
			File runDescriptionFile = HttpConnector.getTempFileFromUrl(new URL(description_url), "xml");
			String description = Conversion.fileToString(runDescriptionFile);
			
			Run run_description = (Run) xstream.fromXML(description);
			dataset = apiconnector.dataGet(dataset_id);
			
			Conversion.log( "OK", "Process Run", "Start prediction evaluator. " );
			
			String filename_prefix = "Run_" + runId + "_";
			URL datasetUrl = apiconnector.getOpenmlFileUrl(dataset.getFile_id(), dataset.getName());
			
			if( task.getTask_type_id() == 4) { // Supervised Data Stream Classification
				URL predictionsUrl = apiconnector.getOpenmlFileUrl(runFiles.get("predictions").getFileId(), filename_prefix + "predictions.arff");
				predictionEvaluator = new EvaluateStreamPredictions(datasetUrl, predictionsUrl, source_data.getTarget_feature());
			} else if (task.getTask_type_id() == 7) { //Survival Analysis
				predictionEvaluator = new EvaluateSurvivalAnalysisPredictions(apiconnector, task, run_description);
			} else if (task.getTask_type_id() == 2) {
				predictionEvaluator = new EvaluateBatchPredictions( 
					apiconnector,
					task,
					TaskType.REGRESSION,
					runFiles.get( "predictions" ).getFileId());
			} else if (task.getTask_type_id() == 3) {
				predictionEvaluator = new EvaluateBatchPredictions( 
					apiconnector,
					task,
					TaskType.LEARNINGCURVE,
					runFiles.get( "predictions" ).getFileId());
			} else if (task.getTask_type_id() == 1 || task.getTask_type_id() == 5 || task.getTask_type_id() == 6 || task.getTask_type_id() == 8) {
				predictionEvaluator = new EvaluateBatchPredictions( 
					apiconnector,
					task,
					TaskType.CLASSIFICATION,
					runFiles.get( "predictions" ).getFileId());
			} else {
				throw new Exception("Unsupported Task Type: " + task.getTask_type_id());
			}
			runevaluation.addEvaluationMeasures(predictionEvaluator.getEvaluationScores());
			
			if(run_description.getOutputEvaluation() != null) {
				Conversion.log( "OK", "Process Run", "Start consistency check with user defined measures. (x " + run_description.getOutputEvaluation().length + ")" );
				
				// TODO: This can be done so much faster ... 
				String warningMessage = "";
				boolean warningFound = false;
				
				for( EvaluationScore recorded : run_description.getOutputEvaluation() ) {
					boolean foundSame = false;
					
					// important check: because of legacy (implementation_id), the flow id might be missing
					if (recorded.getFunction() != null) { 
						for( EvaluationScore calculated : runevaluation.getEvaluation_scores() ) {
							if( recorded.isSame( calculated ) ) {
								foundSame = true;
								if( recorded.sameValue( calculated ) == false ) {
									String offByStr = "";
									try {
										double diff = Math.abs(recorded.getValue() - calculated.getValue());
										offByStr = " (off by " + diff + ")";
									} catch( NumberFormatException nfe ) { }
									
									warningMessage += "Inconsistent Evaluation score: " + recorded + offByStr;
									warningFound = true;
								} 
							}
						}
						if( foundSame == false ) {
							// give the record the correct sample size
							if( recorded.getSample() != null && recorded.getSample_size() == null ) {
								recorded.setSample_size( predictionEvaluator.getPredictionCounter().getShadowTypeSize(
										recorded.getRepeat(), recorded.getFold(), recorded.getSample()));
							}
							runevaluation.addEvaluationMeasure( recorded );
						}
					}
				}
				if( warningFound ) runevaluation.setWarning( warningMessage, MAX_LENGTH_WARNING );
			} else {
				Conversion.log( "OK", "Process Run", "No local evaluation measures to compare to. " );
			}
		} catch(Exception e) {
			e.printStackTrace();
			Conversion.log( "Warning", "Process Run", "Unexpected error, will proceed with upload process: " + e.getMessage() );
			runevaluation.setError(e.getMessage(), MAX_LENGTH_WARNING);
		}
		
		Conversion.log("OK", "Process Run", "Start uploading results ... ");
		try {
			int receivedId = apiconnector.runEvaluate(runevaluation);

			if (trace != null) {
				apiconnector.runTraceUpload(trace);
			}

			Conversion.log("OK", "Process Run", "Run processed: " + receivedId);
		} catch (ApiException e) {
			Conversion.log("ERROR", "Process Run", "An error occured during API call: " + e.getMessage());
			RunEvaluation errorEvaluation = new RunEvaluation(runId, Settings.EVALUATION_ENGINE_ID);
			errorEvaluation.setError(e.getMessage(), MAX_LENGTH_WARNING);
			apiconnector.runEvaluate(errorEvaluation);
			Conversion.log("ERROR", "Process Run", "Processed run with error message. ");
		} catch (Exception e) {
			Conversion.log("ERROR", "Process Run", "An error occured during API call: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	private RunTrace traceToXML(int file_id, int task_id, int run_id) throws Exception {
		RunTrace trace = new RunTrace(run_id);
		Instances traceDataset = new Instances(apiconnector.getArffFromUrl(file_id));
		List<Integer> parameterIndexes = new ArrayList<Integer>();
		
		if (traceDataset.attribute("repeat") == null || 
			traceDataset.attribute("fold") == null || 
			traceDataset.attribute("iteration") == null || 
			traceDataset.attribute("evaluation") == null ||
			traceDataset.attribute("selected") == null) {
			throw new Exception("trace file missing mandatory attributes. ");
		}
		
		for (int i = 0; i < traceDataset.numAttributes(); ++i) {
			if (traceDataset.attribute(i).name().startsWith("parameter_")) {
				parameterIndexes.add(i);
			}
		}
		if (parameterIndexes.size() == 0) {
			throw new Exception("trace file contains no fields with prefix 'parameter_' (i.e., parameters are not registered). ");
		}
		if (traceDataset.numAttributes() > 6 + parameterIndexes.size()) {
			throw new Exception("trace file contains illegal attributes (only allow for repeat, fold, iteration, evaluation, selected, setup_string and parameter_*). ");
		}
		
		for (int i = 0; i < traceDataset.numInstances(); ++i) {
			Instance current = traceDataset.get(i);
			Integer repeat = (int) current.value(traceDataset.attribute("repeat").index());
			Integer fold = (int) current.value(traceDataset.attribute("fold").index());
			Integer iteration = (int) current.value(traceDataset.attribute("iteration").index());
			Double evaluation = current.value(traceDataset.attribute("evaluation").index());
			Boolean selected = current.stringValue(traceDataset.attribute("selected").index()).equals("true");
			
			Map<String,String> parameters = new HashMap<String, String>();
			for (int j = 0; j < parameterIndexes.size(); ++j) {
				int attIdx = parameterIndexes.get(j);
				if (Utils.isMissingValue(current.value(attIdx))) {
					continue;
				} else if (traceDataset.attribute(attIdx).isNumeric()) {
					parameters.put(traceDataset.attribute(attIdx).name(),current.value(attIdx) + "");
				} else {
					parameters.put(traceDataset.attribute(attIdx).name(),current.stringValue(attIdx));
				}
			}
			String setup_string = new JSONObject(parameters).toString();
			
			trace.addIteration(new RunTrace.Trace_iteration(repeat,fold,iteration,setup_string,evaluation,selected));
		}
		
		return trace;
	}
	
}