package org.openml.webapplication.foldgenerators;

import java.util.Random;

import org.openml.apiconnector.xml.EstimationProcedure;
import org.openml.webapplication.algorithm.InstancesHelper;

import weka.core.Instances;

public class CrossValidationSplitsGenerator extends FoldGeneratorBase {
	
	public CrossValidationSplitsGenerator(Instances dataset, EstimationProcedure evaluationMethod, int randomSeed, String splitsName)  throws Exception  {
		super(dataset, evaluationMethod, randomSeed, splitsName);
	}

	public Instances generate() throws Exception {
		Random randomGenerator = new Random(randomSeed);
		Instances splits = new Instances(splitsName, arffMapping.getArffHeader(), splitsSize);
		
		for (int r = 0; r < evaluationMethod.getRepeats(); ++r) {
			dataset.randomize(randomGenerator);
			if (dataset.classAttribute().isNominal()) {
				dataset.stratify(evaluationMethod.getFolds());
			}

			for (int f = 0; f < evaluationMethod.getFolds(); ++f) {
				Instances train = dataset.trainCV(evaluationMethod.getFolds(), f);
				Instances test = dataset.testCV(evaluationMethod.getFolds(), f);

				for (int i = 0; i < train.numInstances(); ++i) {
					int rowid = (int) train.instance(i).value(0);
					splits.add(arffMapping.createInstance(true, rowid, r, f));
				}
				for (int i = 0; i < test.numInstances(); ++i) {
					int rowid = (int) test.instance(i).value(0);
					splits.add(arffMapping.createInstance(false, rowid, r, f));
				}
			}
		}
		return splits;
	}
	
	public Instances generate_learningcurve() throws Exception {
		Random randomGenerator = new Random(randomSeed);
		Instances splits = new Instances(splitsName, arffMapping.getArffHeader(), splitsSize);
		for (int r = 0; r < evaluationMethod.getRepeats(); ++r) {
			dataset.randomize(randomGenerator);
			if (dataset.classAttribute().isNominal()) {
				InstancesHelper.stratify(dataset); // do our own stratification
			}
			
			for (int f = 0; f < evaluationMethod.getFolds(); ++f) {
				Instances train = dataset.trainCV(evaluationMethod.getFolds(), f);
				Instances test = dataset.testCV(evaluationMethod.getFolds(), f);

				for (int s = 0; s < getNumberOfSamples(train.numInstances()); ++s) {
					for (int i = 0; i < sampleSize(s, train.numInstances()); ++i) {
						int rowid = (int) train.instance(i).value(0);
						splits.add(arffMapping.createInstance(true, rowid, r, f, s));
					}
					for (int i = 0; i < test.numInstances(); ++i) {
						int rowid = (int) test.instance(i).value(0);
						splits.add(arffMapping.createInstance(false, rowid, r, f, s));
					}
				}
			}
		}
		return splits;
	}
}
