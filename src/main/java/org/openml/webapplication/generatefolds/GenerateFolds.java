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
package org.openml.webapplication.generatefolds;

import java.io.FileReader;

import org.apache.commons.lang.ArrayUtils;
import org.openml.apiconnector.algorithms.TaskInformation;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.xml.DataSetDescription;
import org.openml.apiconnector.xml.EstimationProcedure;
import org.openml.apiconnector.xml.Task;
import org.openml.webapplication.algorithm.InstancesHelper;
import org.openml.webapplication.foldgenerators.CrossValidationSplitsGenerator;
import org.openml.webapplication.foldgenerators.FoldGeneratorInterface;
import org.openml.webapplication.foldgenerators.HoldoutOrderedSplitsGenerator;
import org.openml.webapplication.foldgenerators.HoldoutSplitsGenerator;
import org.openml.webapplication.foldgenerators.LeaveOneOutSplitsGenerator;
import org.openml.webapplication.foldgenerators.TrainOnTestSplitsGenerator;
import org.openml.webapplication.settings.Settings;

import weka.core.Instances;

public class GenerateFolds {
	
	private final FoldGeneratorInterface foldGenerator;
	private final int ttid;
	
	public GenerateFolds(OpenmlConnector ac, int taskId, int randomSeed) throws Exception {
		Task task = ac.taskGet(taskId);
		ttid = task.getTask_type_id();
		int epId = TaskInformation.getEstimationProcedure(task).getId();
		int did = TaskInformation.getSourceData(task).getData_set_id();
		DataSetDescription dsd = ac.dataGet(did);
		Instances dataset = new Instances(new FileReader(ac.datasetGet(dsd)));
		InstancesHelper.setTargetAttribute(dataset, TaskInformation.getSourceData(task).getTarget_feature());
		EstimationProcedure evaluationMethod = ac.estimationProcedureGet(epId);
		String splitsName = dsd.getName() + "_splits";
		
		switch (evaluationMethod.getType()) {
			case HOLDOUT:
				foldGenerator = new HoldoutSplitsGenerator(dataset, evaluationMethod, randomSeed, splitsName);
				break;
			case HOLDOUT_ORDERED:
				foldGenerator = new HoldoutOrderedSplitsGenerator(dataset, evaluationMethod, splitsName);
				break;
			case CROSSVALIDATION:
				foldGenerator = new CrossValidationSplitsGenerator(dataset, evaluationMethod, randomSeed, splitsName);
				break;
			case LEAVEONEOUT:
				foldGenerator = new LeaveOneOutSplitsGenerator(dataset, evaluationMethod, splitsName);
				break;
			case TESTONTRAININGDATA:
				foldGenerator = new TrainOnTestSplitsGenerator(dataset, evaluationMethod, splitsName);
				break;
			default:
				throw new RuntimeException("Illegal evaluationMethod (GenerateFolds::generateInstances)");
		}
	}
	
	public Instances getSplits() throws Exception {
		// note that the openml evaluation engine does not have a caching mechanism, this is done
		// on task level
		if (ArrayUtils.contains(Settings.LEARNING_CURVE_TASK_IDS, ttid)) {
			return foldGenerator.generate_learningcurve();
		} else {
			return foldGenerator.generate();
		}
	}
}
