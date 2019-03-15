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

import org.apache.commons.lang.ArrayUtils;
import org.openml.apiconnector.algorithms.TaskInformation;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.xml.DataSetDescription;
import org.openml.apiconnector.xml.EstimationProcedure;
import org.openml.apiconnector.xml.Task;
import org.openml.webapplication.algorithm.InstancesHelper;
import org.openml.webapplication.settings.Settings;

import weka.core.Attribute;
import weka.core.Instances;

public class GenerateFolds {
	public static final int MAX_SPLITS_SIZE = 1000000;

	private final Instances dataset;
	private final Instances splits;
	private final String splits_name;
	private final Integer splits_size;

	private final EstimationProcedure evaluationMethod;

	private final ArffMapping am;
	private final Random rand;

	public GenerateFolds(OpenmlConnector ac, int taskId, int randomSeed) throws Exception {
		Task task = ac.taskGet(taskId);
		int epId = TaskInformation.getEstimationProcedure(task).getId();
		rand = new Random(randomSeed);
		int did = TaskInformation.getSourceData(task).getData_set_id();
		DataSetDescription dsd = ac.dataGet(did);
		dataset = new Instances(new FileReader(ac.datasetGet(dsd)));
		InstancesHelper.setTargetAttribute(dataset, TaskInformation.getSourceData(task).getTarget_feature());
		// adds row id. guarantees sorting
		// we are not allowed to use official row_id, even if it exist,
		// since there is no guarantee that this runs from 0 -> n - 1
		addRowId(dataset, "rowid");

		splits_name = dsd.getName() + "_splits";
		evaluationMethod = ac.estimationProcedureGet(epId);
		if (ArrayUtils.contains(Settings.LEARNING_CURVE_TASK_IDS, task.getTask_type_id())) {
			// learning curve task, take special care
			am = new ArffMapping(true);
			splits_size = getSplitsSizeLearningCurve(evaluationMethod, dataset.numInstances());
			splits = sample_splits_learningcurve(splits_name);
		} else {
			am = new ArffMapping(false);
			splits_size = getSplitsSizeVanilla(evaluationMethod, dataset.numInstances());
			splits = generateSplitsVanilla(splits_name);
		}
	}
	
	public Instances getSplits() {
		return splits;
	}

	private static Instances addRowId(Instances instances, String name) {
		instances.insertAttributeAt(new Attribute(name), 0);
		for (int i = 0; i < instances.numInstances(); ++i) {
			instances.instance(i).setValue(0, i);
		}
		return instances;
	}

	private Instances generateSplitsVanilla(String name) throws Exception {
		switch (evaluationMethod.getType()) {
		case HOLDOUT:
			return sample_splits_holdout(name);
		case CROSSVALIDATION:
			return sample_splits_crossvalidation(name);
		case LEAVEONEOUT:
			return sample_splits_leaveoneout(name);
		case TESTONTRAININGDATA:
			return sample_splits_train_on_test(name);
		default:
			throw new RuntimeException("Illegal evaluationMethod (GenerateFolds::generateInstances)");
		}
	}

	private Instances sample_splits_train_on_test(String name) {
		Instances splits = new Instances(name, am.getArffHeader(), splits_size);
		
		for (int i = 0; i < dataset.numInstances(); ++i) {
			int rowid = (int) dataset.instance(i).value(0);
			splits.add(am.createInstance(false, rowid, 0, 0));
			splits.add(am.createInstance(true, rowid, 0, 0));
		}
		
		return splits;
	}

	private Instances sample_splits_holdout(String name) {
		Instances splits = new Instances(name, am.getArffHeader(), splits_size);
		for (int r = 0; r < evaluationMethod.getRepeats(); ++r) {
			dataset.randomize(rand);
			int testSetSize = Math.round(dataset.numInstances() * evaluationMethod.getPercentage() / 100);

			for (int i = 0; i < dataset.numInstances(); ++i) {
				int rowid = (int) dataset.instance(i).value(0);
				splits.add(am.createInstance(i >= testSetSize, rowid, r, 0));
			}
		}
		return splits;
	}

	private Instances sample_splits_crossvalidation(String name) {
		Instances splits = new Instances(name, am.getArffHeader(), splits_size);
		for (int r = 0; r < evaluationMethod.getRepeats(); ++r) {
			dataset.randomize(rand);
			if (dataset.classAttribute().isNominal()) {
				dataset.stratify(evaluationMethod.getFolds());
			}

			for (int f = 0; f < evaluationMethod.getFolds(); ++f) {
				Instances train = dataset.trainCV(evaluationMethod.getFolds(), f);
				Instances test = dataset.testCV(evaluationMethod.getFolds(), f);

				for (int i = 0; i < train.numInstances(); ++i) {
					int rowid = (int) train.instance(i).value(0);
					splits.add(am.createInstance(true, rowid, r, f));
				}
				for (int i = 0; i < test.numInstances(); ++i) {
					int rowid = (int) test.instance(i).value(0);
					splits.add(am.createInstance(false, rowid, r, f));
				}
			}
		}
		return splits;
	}

	private Instances sample_splits_leaveoneout(String name) {
		Instances splits = new Instances(name, am.getArffHeader(), splits_size);
		for (int f = 0; f < dataset.numInstances(); ++f) {
			for (int i = 0; i < dataset.numInstances(); ++i) {
				int rowid = (int) dataset.instance(i).value(0);
				splits.add(am.createInstance(f != i, rowid, 0, f));
			}
		}
		return splits;
	}

	private Instances sample_splits_learningcurve(String name) {
		Instances splits = new Instances(name, am.getArffHeader(), splits_size);
		for (int r = 0; r < evaluationMethod.getRepeats(); ++r) {
			dataset.randomize(rand);
			if (dataset.classAttribute().isNominal()) {
				InstancesHelper.stratify(dataset); // do our own stratification
			}
			
			for (int f = 0; f < evaluationMethod.getFolds(); ++f) {
				Instances train = dataset.trainCV(evaluationMethod.getFolds(), f);
				Instances test = dataset.testCV(evaluationMethod.getFolds(), f);

				for (int s = 0; s < getNumberOfSamples(train.numInstances()); ++s) {
					for (int i = 0; i < sampleSize(s, train.numInstances()); ++i) {
						int rowid = (int) train.instance(i).value(0);
						splits.add(am.createInstance(true, rowid, r, f, s));
					}
					for (int i = 0; i < test.numInstances(); ++i) {
						int rowid = (int) test.instance(i).value(0);
						splits.add(am.createInstance(false, rowid, r, f, s));
					}
				}
			}
		}
		return splits;
	}
	
	/* private Instances sample_splits_bootstrap( String name ) throws Exception {
		Instances splits = new Instances(name,am.getArffHeader(),splits_size);
		for( int r = 0; r < evaluationMethod.getRepeats(); ++r) {
			Resample resample = new Resample();
			String[] resampleOptions = { "-B", "0.0", "-Z", "100.0", "-S", r + "" };
			resample.setOptions(resampleOptions);
			resample.setInputFormat(dataset);
			Instances trainingsset = Filter.useFilter(dataset, resample);
			
			// create training set, consisting of instances from 
			for( int i = 0; i < trainingsset.numInstances(); ++i ) {
				int rowid = (int) trainingsset.instance(i).value(0);
				splits.add(am.createInstance(true,rowid,r,0));
			}
			for( int i = 0; i < dataset.numInstances(); ++i ) {
				int rowid = (int) dataset.instance(i).value(0);
				splits.add(am.createInstance(false,rowid,r,0));
			}
		}
		return splits;
	} */

	/* private Instances sample_splits_holdout_unlabeled(String name) {
		Instances splits = new Instances(name, am.getArffHeader(), splits_size);

		// do not randomize data set, as this method is based on user defined splits
		for (int i = 0; i < dataset.size(); ++i) {
			if (dataset.get(i).classIsMissing()) {
				splits.add(am.createInstance(false, i, 0, 0));
			} else {
				splits.add(am.createInstance(true, i, 0, 0));
			}
		}

		return splits;
	} */
	
	/*private Instances sample_splits_holdout_userdefined( String name, List<List<List<Integer>>> testset ) {
		Instances splits = new Instances(name,am.getArffHeader(),splits_size);
		if( testset == null ) {
			throw new RuntimeException("Option -test not set correctly. ");
		}
		
		for( int r = 0; r < evaluationMethod.getRepeats(); ++r ) {
			for( int f = 0; f < evaluationMethod.getFolds(); ++f ) {
				Collections.sort(testset.get(r).get(f));
				// do not randomize data set, as this method is based on user defined splits
				for( int i = 0; i < dataset.size(); ++i ) {
					if( Collections.binarySearch(testset.get(r).get(f), i) >= 0 ) {
						splits.add(am.createInstance(false,i,r,f));
					} else {
						splits.add(am.createInstance(true,i,r,f));
					}
				}
			}
		}
		
		return splits;
	}*/

	private static int getSplitsSizeVanilla(EstimationProcedure ep, int numInstances) throws Exception {
		switch (ep.getType()) {
			case LEAVEONEOUT:
				return numInstances * numInstances; // repeats (== data set size) * data set size
			case HOLDOUT:
				return ep.getRepeats() * numInstances; // repeats * data set size (each instance is used once)
			case CROSSVALIDATION:
				return ep.getRepeats() * ep.getFolds() * numInstances; // repeats * folds * data set size
			case TESTONTRAININGDATA:
				if (ep.getRepeats() != null || ep.getFolds() != null) {
					throw new Exception("Weird fold/repeat combination for type: " + ep.getType());
				}
				return numInstances * 2;
			default:
				throw new Exception("Unsupported evaluationMethod: " + ep.getType());
		}
	}

	private static int getSplitsSizeLearningCurve(EstimationProcedure ep, int numInstances) {
		switch (ep.getType()) {
			case CROSSVALIDATION:
				int trainsetSize = (int) (numInstances * 1.0 / ep.getFolds() * (ep.getFolds() - 1));
				int splitRecordsPerFold = 0;
				for (int i = 0; i < getNumberOfSamples(trainsetSize); ++i) {
					splitRecordsPerFold += sampleSize(i, trainsetSize);
				}
				return ep.getRepeats() * ep.getFolds() * splitRecordsPerFold; // repeats * folds * perfold
			default:
				throw new RuntimeException("Unsupported evaluationMethod for learning curves: " + ep.getType());
		}
	}

	private static int sampleSize(int number, int trainingsetSize) {
		return (int) Math.min(trainingsetSize, Math.round(Math.pow(2, 6 + (number * 0.5))));
	}

	private static int getNumberOfSamples(int trainingsetSize) {
		int i = 0;
		for (; sampleSize(i, trainingsetSize) < trainingsetSize; ++i) {
		}
		return i + 1; // + 1 for considering the "full" training set
	}
}
