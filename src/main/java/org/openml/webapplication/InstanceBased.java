package org.openml.webapplication;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openml.apiconnector.algorithms.TaskInformation;
import org.openml.apiconnector.xml.DataSetDescription;
import org.openml.apiconnector.xml.Run;
import org.openml.apiconnector.xml.Task;
import org.openml.webapplication.io.Output;
import org.openml.weka.io.OpenmlWekaConnector;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class InstanceBased {
	
	private final Integer task_id;
	private final Instances task_splits;
	private final Instances dataset;
	
	private final int task_splits_size;
	
	private final OpenmlWekaConnector openml;
	private final List<Integer> run_ids;
	private final List<Integer> setup_ids;
	private final Map<Integer,Run> runs;
	private final Map<Integer,String> correct;
	private final Map<Integer,Map<Integer,Map<Integer,Map<Integer,Map<Integer,String>>>>> predictions;
	
	private Instances resultSet;
	
	public InstanceBased(OpenmlWekaConnector openml, List<Integer> run_ids, Integer task_id) throws Exception {
		this.run_ids = run_ids;
		this.openml = openml;
		
		this.predictions = new HashMap<Integer, Map<Integer,Map<Integer,Map<Integer,Map<Integer,String>>>>>();
		this.runs = new HashMap<Integer,Run>();
		this.setup_ids = new ArrayList<Integer>();
		
		this.task_id = task_id; 
		Task currentTask = openml.taskGet(task_id);
		
		if (currentTask.getTask_type().equals("Supervised Classification") == false
				&& currentTask.getTask_type().equals("Supervised Data Stream Classification") == false) { // TODO: no string based comp.
			throw new RuntimeException("Experimental function, only works with 'Supervised Classification' tasks for now (ttid / 1)" );
		}
		

		DataSetDescription dsd = openml.dataGet(TaskInformation.getSourceData(currentTask).getData_set_id());
		dataset = new Instances(openml.getDataset(dsd));
		
		if (currentTask.getTask_type().equals("Supervised Data Stream Classification")) {
			// simulate task splits file. 
			
			int numberOfInstances = dataset.numInstances();
			
			ArrayList<Attribute> attributes = new ArrayList<Attribute>();
			List<String> typeValues = new ArrayList<String>();
			typeValues.add("TEST");
			attributes.add(new Attribute("repeat"));
			attributes.add(new Attribute("fold"));
			attributes.add(new Attribute("rowid"));
			attributes.add(new Attribute("type",typeValues)); // don't need train
			
			task_splits = new Instances("task" + task_id +"splits-simulated", attributes, numberOfInstances);
			
			for (int i = 0; i < numberOfInstances; ++i) {
				double[] attValues = {0,0,i,0};
				task_splits.add(new DenseInstance(1.0, attValues));
			}
		} else {
			task_splits = new Instances(openml.getSplitsFromTask(currentTask));
		}
			
		for (Integer run_id : run_ids) {
			Run current = this.openml.runGet(run_id);
			runs.put(run_id,current);
			Run.Data.File[] outputFiles = current.getOutputFile();
			setup_ids.add(current.getSetup_id());
			
			boolean found = false;
			for (Run.Data.File f : outputFiles) {
				if (f.getName().equals("predictions")) {
					found = true;
					Instances runPredictions = new Instances(openml.getArffFromUrl(f.getFileId()));
					predictions.put(run_id,predictionsToHashMap(runPredictions));
				}
			}
			
			if (found == false) {
				throw new RuntimeException("No prediction files associated with run. Id: " + run_id );
			}
			if (task_id != current.getTask_id()) {
				throw new RuntimeException("Runs are not of the same task type: Should be: " + this.task_id + "; found " + current.getTask_id() + " (and maybe more)" );
			}
		}
		task_splits_size = task_splits.numInstances();
		correct = datasetToHashMap(dataset, TaskInformation.getSourceData(currentTask).getTarget_feature());
	}
	
	public int taskSplitSize() {
		return task_splits_size;
	}
	
	public int calculateDifference() throws Exception {
		if (run_ids.size() != 2) {
			throw new RuntimeException("Too many runs to compare. Should be 2. ");
		}
		
		List<String> values = new ArrayList<String>();
		for (Integer run : run_ids) {
			values.add(run + "");
		}
		values.add("none");
		
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		attributes.add(new Attribute("repeat"));
		attributes.add(new Attribute("fold"));
		attributes.add(new Attribute("rowid"));
		attributes.add(new Attribute("whichCorrect", values));
		
		resultSet = new Instances("difference",attributes,task_splits.numInstances());
		
		for (int i = 0; i < task_splits.numInstances(); ++i) {
			Instance current = task_splits.get(i);
			boolean test = current.stringValue(task_splits.attribute("type")).equals("TEST");
			if (!test) {
				continue;
			}
			
			Integer row_id = (int) current.value(task_splits.attribute("rowid"));
			Integer repeat = (int) current.value(task_splits.attribute("repeat"));
			Integer fold = (int) current.value(task_splits.attribute("fold"));
			Integer sample = 0;
			try {
				sample = (int) current.value(task_splits.attribute("sample"));
			} catch(Exception e) {}
			
			String label = null;
			boolean difference = false;
			String correctLabel = correct.get(row_id);
			double whichCorrect = resultSet.attribute("whichCorrect").indexOfValue("none");
			
			for (Integer run_id : run_ids) {
				String currentLabel = predictions.get(run_id).get(repeat).get(fold).get(sample).get(row_id);
				// check for difference
				if (label == null) {
					label = currentLabel;
				} else if (label.equals(currentLabel) == false) {
					difference = true;
				}
				
				// check for correct label
				if (currentLabel.equals(correctLabel)) {
					whichCorrect = resultSet.attribute("whichCorrect").indexOfValue(run_id + "");
				}
			}
			
			if (difference) {
				double[] instance = {repeat,fold,row_id, whichCorrect};
				resultSet.add(new DenseInstance(1.0,instance));
			}
		}
		
		
		openml.setupDifferences(setup_ids.get(0), setup_ids.get(1), task_id, task_splits_size, resultSet.size()); 
		
		return resultSet.size();
	}
	
	public int calculateAllWrong() {
		if (run_ids.size() < 2) {
			throw new RuntimeException("Too few runs to compare. Should be at least 2. ");
		}
		
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		attributes.add(new Attribute("repeat"));
		attributes.add(new Attribute("fold"));
		attributes.add(new Attribute("rowid"));
		resultSet = new Instances("all-wrong",attributes,task_splits.numInstances());
		
		for (int i = 0; i < task_splits.numInstances(); ++i) {
			Instance current = task_splits.get(i);
			boolean test = current.stringValue(task_splits.attribute("type")).equals("TEST");
			if (!test) {
				continue;
			}
			
			Integer row_id = (int) current.value(task_splits.attribute("rowid"));
			Integer repeat = (int) current.value(task_splits.attribute("repeat"));
			Integer fold = (int) current.value(task_splits.attribute("fold"));
			Integer sample = 0;
			try {
				sample = (int) current.value(task_splits.attribute("sample"));
			} catch(Exception e) {}
			
			String correctLabel = correct.get(row_id);
			Integer correctPredictions = 0;
			
			for (Integer run_id : run_ids) {
				
				//System.out.println(predictions.get(run_id));
				//System.out.println(repeat + "," + fold + "," + sample + "," + row_id);
				
				if (predictions.get(run_id).get(repeat).get(fold).get(sample).get(row_id).equals(correctLabel)) {
					correctPredictions += 1;
				}
			}
			
			if (correctPredictions == 0) {
				double[] instance = {repeat,fold,row_id};
				resultSet.add(new DenseInstance(1.0,instance));
			}
		}
		return resultSet.size();
	}
	
	public void toStdout(String[] leadingComments) throws IOException {
		Output.instances2file(resultSet, new OutputStreamWriter(System.out), leadingComments);
	}
	
	private static Map<Integer,Map<Integer,Map<Integer,Map<Integer,String>>>> predictionsToHashMap(Instances predictions) {
		Map<Integer,Map<Integer,Map<Integer,Map<Integer,String>>>> results = new HashMap<Integer, Map<Integer,Map<Integer,Map<Integer,String>>>>();
		
		for (int i = 0; i < predictions.numInstances(); ++i) {
			Instance current = predictions.get(i);
			
			Integer repeat = 0;
			Integer fold = 0;
			Integer sample = 0;
			
			try { 
				repeat = (int) current.value(predictions.attribute("repeat"));
			} catch(NullPointerException e) {}
			try { 
				fold = (int) current.value(predictions.attribute("fold"));
			} catch(NullPointerException e) {}
			try { 
				sample = (int) current.value(predictions.attribute("sample"));
			} catch(NullPointerException e) {}
			
			Integer row_id = (int) current.value(predictions.attribute("row_id"));
			String prediction = current.stringValue(predictions.attribute("prediction"));
			
			if (results.containsKey(repeat) == false) {
				results.put(repeat, new HashMap<Integer, Map<Integer,Map<Integer,String>>>());
			}
			if (results.get(repeat).containsKey(fold) == false) {
				results.get(repeat).put(fold, new HashMap<Integer,Map<Integer,String>>());
			}
			if (results.get(repeat).get(fold).containsKey(sample) == false) {
				results.get(repeat).get(fold).put(sample, new HashMap<Integer,String>());
			}
			results.get(repeat).get(fold).get(sample).put(row_id, prediction);
		}
		
		return results;
	}
	
	private static Map<Integer,String> datasetToHashMap(Instances dataset, String target_attribute) {
		Map<Integer,String> correct = new HashMap<Integer, String>();
		
		for (int i = 0; i < dataset.numInstances(); ++i) {
			Instance current = dataset.get(i);
			
			correct.put(i, current.stringValue(dataset.attribute(target_attribute)));
		}
		
		return correct;
	}
	
}
