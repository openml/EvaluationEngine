package org.openml.webapplication.foldgenerators;

import java.util.Random;

import org.openml.apiconnector.xml.EstimationProcedure;

import weka.core.Instances;

public class CrossValidationSplitsGenerator extends FoldGeneratorBase {
	
	public CrossValidationSplitsGenerator(Instances dataset, EstimationProcedure evaluationMethod, Random random, String splitsName)  throws Exception  {
		super(dataset, evaluationMethod, random, splitsName);
	}
	

	public Instances generate() throws Exception {
		Instances splits = new Instances(splitsName, arffMapping.getArffHeader(), splitsSize);
		
		for (int r = 0; r < evaluationMethod.getRepeats(); ++r) {
			dataset.randomize(random);
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
}
