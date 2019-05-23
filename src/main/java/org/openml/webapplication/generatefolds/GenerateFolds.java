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
import java.util.Random;

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

import weka.core.Instances;

public class GenerateFolds {
	
	private final FoldGeneratorInterface foldGenerator;
	
	public GenerateFolds(OpenmlConnector ac, int taskId, int randomSeed) throws Exception {
		Task task = ac.taskGet(taskId);
		int epId = TaskInformation.getEstimationProcedure(task).getId();
		int did = TaskInformation.getSourceData(task).getData_set_id();
		DataSetDescription dsd = ac.dataGet(did);
		Instances dataset = new Instances(new FileReader(ac.datasetGet(dsd)));
		InstancesHelper.setTargetAttribute(dataset, TaskInformation.getSourceData(task).getTarget_feature());
		EstimationProcedure evaluationMethod = ac.estimationProcedureGet(epId);
		Random random = new Random(randomSeed);
		String splitsName = dsd.getName() + "_splits";
		
		switch (evaluationMethod.getType()) {
			case HOLDOUT:
				foldGenerator = new HoldoutSplitsGenerator(dataset, evaluationMethod, random, splitsName);
				break;
			case HOLDOUT_ORDERED:
				foldGenerator = new HoldoutOrderedSplitsGenerator(dataset, evaluationMethod, random, splitsName);
				break;
			case CROSSVALIDATION:
				foldGenerator = new CrossValidationSplitsGenerator(dataset, evaluationMethod, random, splitsName);
				break;
			case LEAVEONEOUT:
				foldGenerator = new LeaveOneOutSplitsGenerator(dataset, evaluationMethod, random, splitsName);
				break;
			case TESTONTRAININGDATA:
				foldGenerator = new TrainOnTestSplitsGenerator(dataset, evaluationMethod, random, splitsName);
				break;
			default:
				throw new RuntimeException("Illegal evaluationMethod (GenerateFolds::generateInstances)");
		}
	}
	
	public Instances getSplits() throws Exception {
		return foldGenerator.generate();
	}
}
