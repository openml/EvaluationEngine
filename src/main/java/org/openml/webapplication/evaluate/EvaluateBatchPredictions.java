/*
 *  Webapplication - Java library that runs on OpenML servers
 *  Copyright (C) 2014 
 *  @author Jan N. van Rijn (j.n.van.rijn@liacs.leidenuniv.nl)
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 */
package org.openml.webapplication.evaluate;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.json.JSONArray;
import org.openml.apiconnector.algorithms.Conversion;
import org.openml.apiconnector.algorithms.TaskInformation;
import org.openml.apiconnector.models.MetricScore;
import org.openml.apiconnector.settings.Constants;
import org.openml.apiconnector.xml.DataSetDescription;
import org.openml.apiconnector.xml.EstimationProcedure;
import org.openml.apiconnector.xml.EstimationProcedureType;
import org.openml.apiconnector.xml.EvaluationScore;
import org.openml.apiconnector.xml.Task;
import org.openml.apiconnector.xml.Task.Input.Estimation_procedure;
import org.openml.webapplication.algorithm.InstancesHelper;
import org.openml.webapplication.io.Output;
import org.openml.webapplication.predictionCounter.FoldsPredictionCounter;
import org.openml.webapplication.predictionCounter.PredictionCounter;
import org.openml.weka.io.OpenmlWekaConnector;

import weka.classifiers.Evaluation;
import weka.core.Instance;
import weka.core.Instances;

public class EvaluateBatchPredictions implements PredictionEvaluator {

	private final int nrOfClasses;

	private final int ATT_PREDICTION_ROWID;
	private final int ATT_PREDICTION_FOLD;
	private final int ATT_PREDICTION_REPEAT;
	private final int ATT_PREDICTION_PREDICTION;
	private final int ATT_PREDICTION_SAMPLE;
	private final int[] ATT_PREDICTION_CONFIDENCE;

	private final Instances dataset;
	private final Instances splits;
	private final Instances predictions;

	private final PredictionCounter predictionCounter;
	private final String[] classes;
	private final EstimationProcedure estimationProcedure;
	private final TaskType taskType;
	private final JSONArray cost_matrix;
	private final Evaluation[][][][] sampleEvaluation;
	private final boolean bootstrap;

	private EvaluationScore[] evaluationScores;

	public EvaluateBatchPredictions(OpenmlWekaConnector openml, Task task, TaskType taskType, int predictionsFileId) throws Exception {
		final int datasetId = TaskInformation.getSourceData(task).getData_set_id();
		int epId = TaskInformation.getEstimationProcedure(task).getId();
		estimationProcedure = openml.estimationProcedureGet(epId);
		DataSetDescription dsd = openml.dataGet(datasetId);
		this.taskType = taskType;
		
		// set all arff files needed for this operation.
		dataset = new Instances(openml.getDataset(dsd));
		splits = new Instances(openml.getSplitsFromTask(task));
		predictions = new Instances(openml.getArffFromUrl(predictionsFileId));
		Conversion.log("OK", "EvaluateBatchPredictions", "predictions: " + predictionsFileId);
		
		Estimation_procedure estimationprocedure = TaskInformation.getEstimationProcedure(task);
		this.bootstrap = estimationprocedure.getType() == EstimationProcedureType.BOOTSTRAPPING;
		String classAttribute = TaskInformation.getSourceData(task).getTarget_feature();
		cost_matrix = TaskInformation.getCostMatrix(task);

		// Set class attribute to dataset ...
		if (dataset.attribute(classAttribute) != null) {
			dataset.setClass(dataset.attribute(classAttribute));
		} else {
			throw new RuntimeException("Class attribute (" + classAttribute + ") not found");
		}

		// initiate a class that will help us with checking the prediction
		// count.
		predictionCounter = new FoldsPredictionCounter(splits);
		sampleEvaluation = new Evaluation[predictionCounter.getRepeats()][predictionCounter
				.getFolds()][predictionCounter.getSamples()][bootstrap ? 2 : 1];

		// *** A sample is considered to be a subset of a fold. In a normal
		// n-times n-fold crossvalidation
		// setting, each fold consists of 1 sample. In a leaning curve example,
		// each fold could consist
		// of more samples.

		// register row indexes.
		ATT_PREDICTION_ROWID = InstancesHelper.getRowIndex("row_id", predictions);
		ATT_PREDICTION_REPEAT = InstancesHelper.getRowIndex(new String[] { "repeat", "repeat_nr" }, predictions);
		ATT_PREDICTION_FOLD = InstancesHelper.getRowIndex(new String[] { "fold", "fold_nr" }, predictions);
		ATT_PREDICTION_PREDICTION = InstancesHelper.getRowIndex(new String[] { "prediction" }, predictions);
		if (taskType == TaskType.LEARNINGCURVE) {
			ATT_PREDICTION_SAMPLE = InstancesHelper.getRowIndex(new String[] { "sample", "sample_nr" }, predictions);
		} else {
			ATT_PREDICTION_SAMPLE = -1;
		}
		// do the same for the confidence fields. This number is dependent on
		// the number
		// of classes in the data set, hence the for-loop.
		nrOfClasses = dataset.classAttribute().numValues(); // returns 0 if
															// numeric, that's
															// good.
		classes = new String[nrOfClasses];
		ATT_PREDICTION_CONFIDENCE = new int[nrOfClasses];
		for (int i = 0; i < classes.length; i++) {
			classes[i] = dataset.classAttribute().value(i);
			String attribute = "confidence." + classes[i];
			if (predictions.attribute(attribute) != null) {
				ATT_PREDICTION_CONFIDENCE[i] = predictions.attribute(attribute).index();
			} else {
				throw new Exception("Attribute " + attribute + " not found among predictions. ");
			}
		}

		// and do the actual evaluation.
		doEvaluation();
	}

	private void doEvaluation() throws Exception {
		// set global evaluation
		Evaluation[] globalEvaluator = new Evaluation[bootstrap ? 2 : 1];

		for (int i = 0; i < globalEvaluator.length; ++i) {
			globalEvaluator[i] = new Evaluation(dataset);
			if (cost_matrix != null) {
				// TODO test
				globalEvaluator[i] = new Evaluation(dataset, InstancesHelper.doubleToCostMatrix(cost_matrix));
			} else {
				globalEvaluator[i] = new Evaluation(dataset);
			}
		}

		// set local evaluations
		for (int i = 0; i < sampleEvaluation.length; ++i) {
			for (int j = 0; j < sampleEvaluation[i].length; ++j) {
				for (int k = 0; k < sampleEvaluation[i][j].length; ++k) {
					for (int m = 0; m < (bootstrap ? 2 : 1); ++m) {
						if (cost_matrix != null) {
							// TODO test
							sampleEvaluation[i][j][k][m] = new Evaluation(dataset, InstancesHelper.doubleToCostMatrix(cost_matrix));
						} else {
							sampleEvaluation[i][j][k][m] = new Evaluation(dataset);
						}
					}
				}
			}
		}

		for (int i = 0; i < predictions.numInstances(); i++) {
			Instance prediction = predictions.instance(i);
			int repeat = ATT_PREDICTION_REPEAT < 0 ? 0 : (int) prediction.value(ATT_PREDICTION_REPEAT);
			int fold = ATT_PREDICTION_FOLD < 0 ? 0 : (int) prediction.value(ATT_PREDICTION_FOLD);
			int sample = ATT_PREDICTION_SAMPLE < 0 ? 0 : (int) prediction.value(ATT_PREDICTION_SAMPLE);
			int rowid = (int) prediction.value(ATT_PREDICTION_ROWID);

			predictionCounter.addPrediction(repeat, fold, sample, rowid);
			if (dataset.numInstances() <= rowid) {
				throw new RuntimeException("Making a prediction for row_id" + rowid
						+ " (0-based) while dataset has only " + dataset.numInstances() + " instances. ");
			}

			int bootstrap = 0;
			boolean measureGlobalScore = true;
			if (taskType == TaskType.LEARNINGCURVE && sample != predictionCounter.getSamples() - 1) {
				// for learning curves, we want the score of the last sample at
				// global score
				measureGlobalScore = false;
			}

			if (taskType == TaskType.REGRESSION) {
				if (measureGlobalScore) {
					globalEvaluator[bootstrap].evaluateModelOnce(prediction.value(ATT_PREDICTION_PREDICTION),
							dataset.instance(rowid));
				}
				sampleEvaluation[repeat][fold][sample][bootstrap].evaluateModelOnce(prediction.value(ATT_PREDICTION_PREDICTION), dataset.instance(rowid));
			} else {
				// TODO: catch error when no prob distribution is provided
				double[] confidences = InstancesHelper.predictionToConfidences(dataset, prediction, ATT_PREDICTION_CONFIDENCE, ATT_PREDICTION_PREDICTION);

				if (measureGlobalScore) {
					globalEvaluator[bootstrap].evaluateModelOnceAndRecordPrediction(confidences, dataset.instance(rowid));
				}
				sampleEvaluation[repeat][fold][sample][bootstrap].evaluateModelOnceAndRecordPrediction(confidences, dataset.instance(rowid));
			}
		}

		if (predictionCounter.check() == false) {
			throw new RuntimeException("Prediction count does not match: " + predictionCounter.getErrorMessage());
		}
		
		// evaluationMeasuresList is an array that holds all evaluations that will be attached to a run
		List<EvaluationScore> evaluationMeasuresList = new ArrayList<EvaluationScore>();
		// tmpFoldEvaluations contains all results obtained per fold/repeat (not per sample) to obtain
		// standard deviation values for global result. 
		Map<String, List<Double>> tmpFoldEvaluations = new HashMap<String, List<Double>>();
		for (int i = 0; i < sampleEvaluation.length; ++i) {
			for (int j = 0; j < sampleEvaluation[i].length; ++j) {
				for (int k = 0; k < sampleEvaluation[i][j].length; ++k) {
					Map<String, MetricScore> currentMeasures = Output.evaluatorToMap(sampleEvaluation[i][j][k],
							nrOfClasses, taskType, bootstrap);
					for (String math_function : currentMeasures.keySet()) {
						MetricScore score = currentMeasures.get(math_function);
						// preventing divisions by zero and infinite scores (given by Weka)
						if (score.getScore() != null && score.getScore().isNaN() == false && score.getScore().isInfinite() == false) { 
							DecimalFormat dm = Constants.defaultDecimalFormat;
							EvaluationScore currentMeasure;
							
							Double currentScore = score.getScore() == null ? null : score.getScore();
							if (taskType == TaskType.LEARNINGCURVE) {
								currentMeasure = new EvaluationScore(math_function,
									currentScore,
									score.getArrayAsString(dm), i, j, k,
									predictionCounter.getShadowTypeSize(i, j, k));
							} else {
								currentMeasure = new EvaluationScore(math_function,
									currentScore,
									score.getArrayAsString(dm), i, j);
							}
							// do not add individual scores for LOO
							if (estimationProcedure.getType() != EstimationProcedureType.LEAVEONEOUT && estimationProcedure.getType() != EstimationProcedureType.TESTONTRAININGDATA) {
								evaluationMeasuresList.add(currentMeasure);
							}
							if (currentScore != null && k == sampleEvaluation[i][j].length - 1) {
								if (!tmpFoldEvaluations.containsKey(math_function)) {
									tmpFoldEvaluations.put(math_function, new ArrayList<>());
								}
								tmpFoldEvaluations.get(math_function).add(currentScore);
							}
						}
					}
				}
			}
		}
		
		StandardDeviation stdevCalculator = new StandardDeviation();
		Map<String, MetricScore> globalMeasures = Output.evaluatorToMap(globalEvaluator, nrOfClasses, taskType, bootstrap);
		for (String math_function : globalMeasures.keySet()) {
			MetricScore score = globalMeasures.get(math_function);
			// preventing divisions by zero and infinite scores (given by Weka)
			if (score.getScore() != null && score.getScore().isNaN() == false && score.getScore().isInfinite() == false) { 
				DecimalFormat dm = Constants.defaultDecimalFormat;
				Double calculated_score = score.getScore() == null ? null : score.getScore();
				
				Double stdev = null;
				if (tmpFoldEvaluations.containsKey(math_function)) {
					Double[] individualVals = tmpFoldEvaluations.get(math_function).toArray(new Double[tmpFoldEvaluations.get(math_function).size()]);
					stdev = stdevCalculator.evaluate(ArrayUtils.toPrimitive(individualVals));
				}
				
				EvaluationScore em = new EvaluationScore(math_function,
						calculated_score, 
						stdev,
						score.getArrayAsString(dm));
				evaluationMeasuresList.add(em);
			}
		}
		evaluationScores = evaluationMeasuresList.toArray(new EvaluationScore[evaluationMeasuresList.size()]);
	}

	public EvaluationScore[] getEvaluationScores() {
		return evaluationScores;
	}

	@Override
	public PredictionCounter getPredictionCounter() {
		return predictionCounter;
	}

}