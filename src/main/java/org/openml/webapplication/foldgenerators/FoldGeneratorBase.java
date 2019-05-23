package org.openml.webapplication.foldgenerators;

import java.util.Random;

import org.apache.commons.lang.ArrayUtils;
import org.openml.apiconnector.xml.EstimationProcedure;
import org.openml.apiconnector.xml.EstimationProcedureType;
import org.openml.webapplication.generatefolds.ArffMapping;
import org.openml.webapplication.settings.Settings;

import weka.core.Attribute;
import weka.core.Instances;

public abstract class FoldGeneratorBase implements FoldGeneratorInterface {
	
	protected final Instances dataset;
	protected final EstimationProcedure evaluationMethod;
	protected final Random random;
	protected final int splitsSize;
	protected final ArffMapping arffMapping;
	protected final String splitsName;
	
	public FoldGeneratorBase(Instances dataset, EstimationProcedure evaluationMethod, Random random, String splitsName) throws Exception {
		// adds row id. guarantees sorting, we are not allowed to use official row_id, even if it exist,
		// since there is no guarantee that this runs from 0 -> n - 1
		this.dataset = addRowId(dataset, "temp_row_id");
		this.random = random;
		this.evaluationMethod = evaluationMethod;
		this.splitsName = splitsName;
		
		if (ArrayUtils.contains(Settings.LEARNING_CURVE_TASK_IDS, evaluationMethod.getTtid())) {
			splitsSize = getSplitsSizeLearningCurve(evaluationMethod, dataset.numInstances());
			arffMapping = new ArffMapping(true);
			// TODO: check whether evaluation method has sample based
		} else {
			splitsSize = getSplitsSizeVanilla(evaluationMethod, dataset.numInstances());
			arffMapping = new ArffMapping(false);
			// TODO: check whether evaluation method has sample based
		}
		
		if (evaluationMethod.getType() == EstimationProcedureType.HOLDOUT_ORDERED) {
			if (evaluationMethod.getFolds() > 1 || evaluationMethod.getRepeats() > 1) {
				throw new Exception("Illegal combination: HOLDOUT_ORDERED should have exactly one repeat and one fold. ");
			}
		}
	}

	private static Instances addRowId(Instances instances, String attributeName) {
		instances.insertAttributeAt(new Attribute(attributeName), 0);
		for (int i = 0; i < instances.numInstances(); ++i) {
			instances.instance(i).setValue(0, i);
		}
		return instances;
	}

	protected static int getSplitsSizeVanilla(EstimationProcedure ep, int numInstances) throws Exception {
		switch (ep.getType()) {
			case LEAVEONEOUT:
				return numInstances * numInstances; // repeats (== data set size) * data set size
			case HOLDOUT:
			case HOLDOUT_ORDERED:
				return ep.getRepeats() * numInstances; // repeats * data set size (each instance is used once)
			case CROSSVALIDATION:
				return ep.getRepeats() * ep.getFolds() * numInstances; // repeats * folds * data set size
			case TESTONTRAININGDATA:
				if (ep.getRepeats() != null || ep.getFolds() != null) {
					throw new Exception("Illegal fold/repeat combination for type: " + ep.getType());
				}
				return numInstances * 2;
			default:
				throw new Exception("Unsupported evaluationMethod: " + ep.getType());
		}
	}
	
	protected static int getSplitsSizeLearningCurve(EstimationProcedure ep, int numInstances) {
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

	protected static int sampleSize(int number, int trainingsetSize) {
		return (int) Math.min(trainingsetSize, Math.round(Math.pow(2, 6 + (number * 0.5))));
	}

	protected static int getNumberOfSamples(int trainingsetSize) {
		int i = 0;
		for (; sampleSize(i, trainingsetSize) < trainingsetSize; ++i) {
		}
		return i + 1; // + 1 for considering the "full" training set
	}
}
