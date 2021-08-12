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
package org.openml.webapplication.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openml.apiconnector.models.MetricScore;
import org.openml.webapplication.evaluate.OpenMLEvaluation;
import org.openml.webapplication.evaluate.TaskType;
import org.openml.webapplication.models.JsonItem;

import weka.core.Instances;
import weka.core.Utils;

public class Output {

	/*
	 * Transforms a Weka Evaluation object into a HashMap with scores.
	 */
	public static Map<String, MetricScore> evaluatorToMap(OpenMLEvaluation evaluator, int classes, TaskType task) throws Exception {
		Map<String, MetricScore> m = new HashMap<String, MetricScore>();

		if (task == TaskType.REGRESSION) {

			// here all measures for regression tasks
			m.put("mean_absolute_error",
					new MetricScore(evaluator.meanAbsoluteError(), (int) evaluator.numInstances()));
			m.put("mean_prior_absolute_error",
					new MetricScore(evaluator.meanPriorAbsoluteError(), (int) evaluator.numInstances()));
			m.put("number_of_instances", new MetricScore(evaluator.numInstances(), (int) evaluator.numInstances()));
			m.put("root_mean_squared_error",
					new MetricScore(evaluator.rootMeanSquaredError(), (int) evaluator.numInstances()));
			m.put("root_mean_prior_squared_error",
					new MetricScore(evaluator.rootMeanPriorSquaredError(), (int) evaluator.numInstances()));
			m.put("relative_absolute_error",
					new MetricScore(evaluator.relativeAbsoluteError() / 100, (int) evaluator.numInstances()));
			m.put("root_relative_squared_error",
					new MetricScore(evaluator.rootRelativeSquaredError() / 100, (int) evaluator.numInstances()));

		} else if (task == TaskType.CLASSIFICATION || task == TaskType.LEARNINGCURVE
				|| task == TaskType.TESTTHENTRAIN) {

			m.put("average_cost", new MetricScore(evaluator.avgCost(), (int) evaluator.numInstances()));
			m.put("total_cost", new MetricScore(evaluator.totalCost(), (int) evaluator.numInstances()));

			m.put("mean_absolute_error",
					new MetricScore(evaluator.meanAbsoluteError(), (int) evaluator.numInstances()));
			m.put("mean_prior_absolute_error",
					new MetricScore(evaluator.meanPriorAbsoluteError(), (int) evaluator.numInstances()));
			m.put("root_mean_squared_error",
					new MetricScore(evaluator.rootMeanSquaredError(), (int) evaluator.numInstances()));
			m.put("root_mean_prior_squared_error",
					new MetricScore(evaluator.rootMeanPriorSquaredError(), (int) evaluator.numInstances()));
			m.put("relative_absolute_error",
					new MetricScore(evaluator.relativeAbsoluteError() / 100, (int) evaluator.numInstances()));
			m.put("root_relative_squared_error",
					new MetricScore(evaluator.rootRelativeSquaredError() / 100, (int) evaluator.numInstances()));

			m.put("prior_entropy", new MetricScore(evaluator.priorEntropy(), (int) evaluator.numInstances()));
			m.put("kb_relative_information_score",
					new MetricScore(evaluator.KBRelativeInformation() / 100, (int) evaluator.numInstances()));

			Double[] precision = new Double[classes];
			Double[] recall = new Double[classes];
			Double[] auroc = new Double[classes];
			Double[] fMeasure = new Double[classes];
			Double[] instancesPerClass = new Double[classes];
			double[][] confussion_matrix = evaluator.confusionMatrix();
			for (int i = 0; i < classes; ++i) {
				precision[i] = evaluator.precision(i);
				recall[i] = evaluator.recall(i);
				auroc[i] = evaluator.areaUnderROC(i);
				fMeasure[i] = evaluator.fMeasure(i);
				instancesPerClass[i] = 0.0;
				for (int j = 0; j < classes; ++j) {
					instancesPerClass[i] += confussion_matrix[i][j];
				}
			}

			m.put("predictive_accuracy", new MetricScore(evaluator.pctCorrect() / 100, (int) evaluator.numInstances()));
			m.put("kappa", new MetricScore(evaluator.kappa(), (int) evaluator.numInstances()));

			m.put("number_of_instances",
					new MetricScore(evaluator.numInstances(), instancesPerClass, (int) evaluator.numInstances()));

			m.put("precision",
					new MetricScore(evaluator.weightedPrecision(), precision, (int) evaluator.numInstances()));
			m.put("weighted_recall", new MetricScore(evaluator.weightedRecall(), recall, (int) evaluator.numInstances()));
			m.put("unweighted_recall", new MetricScore(evaluator.unweightedRecall(), recall, (int) evaluator.numInstances()));
			m.put("f_measure", new MetricScore(evaluator.weightedFMeasure(), fMeasure, (int) evaluator.numInstances()));
			if (Utils.isMissingValue(evaluator.weightedAreaUnderROC()) == false) {
				m.put("area_under_roc_curve",
						new MetricScore(evaluator.weightedAreaUnderROC(), auroc, (int) evaluator.numInstances()));
			}
			m.put("confusion_matrix", new MetricScore(confussion_matrix));
		}
		return m;
	}

	/*
	 * Transforms a Weka Evaluation object into a HashMap with scores. In this
	 * function, we assume that the evaluator consists of either 2 (bootstrap) or 1
	 * (otherwise) elements. evaluator[0] contains all unseen instances,
	 * evaluator[1] contains all instances that occured in the trainings set.
	 */
	public static Map<String, MetricScore> evaluatorToMap(OpenMLEvaluation[] evaluator, int classes, TaskType task, boolean bootstrap) throws Exception {
		if (bootstrap && evaluator.length != 2) {
			throw new Exception("Output->evaluatorToMap problem: Can not perform bootstrap scores, evaluation array is of wrong length. ");
		} else if (bootstrap == false && evaluator.length != 1) {
			throw new Exception("Output->evaluatorToMap exception: Choosen for normal evaluation, but two evaluators provided in array. ");
		}

		if (bootstrap == false) {
			return evaluatorToMap(evaluator[0], classes, task);
		} else {
			throw new Exception("bootstrap not supported yet");
		}
	}

	public static String styleToJsonError(Integer errorNo, String value) {
		String errorNoStr = "";
		if (errorNo != null) {
			errorNoStr = ", \"errorNo\": \"" + errorNo + "\"}";
		}
		return "{\"error\":\"" + value + "\"" + errorNoStr + "\n";
	}

	public static String statusMessage(String status, String message) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		sb.append("\t\"status\":\"" + status + "\",\n");
		sb.append("\t\"message\":\"" + message + "\"\n");
		sb.append("}");

		return sb.toString();
	}

	public static String dataFeatureToJson(List<JsonItem> jsonItems) {
		StringBuilder sb = new StringBuilder();
		for (JsonItem item : jsonItems) {
			if (item.useQuotes()) {
				sb.append(", \"" + item.getKey() + "\":\"" + item.getValue() + "\"");
			} else {
				sb.append(", \"" + item.getKey() + "\":" + item.getValue());
			}
		}
		return "{" + sb.toString().substring(2) + "}";
	}

	public static void instances2file(Instances instances, Writer out, String[] leadingComments) throws IOException {
		BufferedWriter bw = new BufferedWriter(out);

		if (leadingComments != null) {
			for (int i = 0; i < leadingComments.length; ++i) {
				bw.write("% " + leadingComments[i] + "\n");
			}
		}

		// Important: We can not use a std Instances.toString() approach, as instance
		// files can grow
		bw.write("@relation " + instances.relationName() + "\n\n");
		for (int i = 0; i < instances.numAttributes(); ++i) {
			bw.write(instances.attribute(i) + "\n");
		}
		bw.write("\n@data\n");
		for (int i = 0; i < instances.numInstances(); ++i) {
			if (i + 1 == instances.numInstances()) {
				bw.write(instances.instance(i) + ""); // fix for last instance
			} else {
				bw.write(instances.instance(i) + "\n");
			}
		}
		bw.close();
	}
}
