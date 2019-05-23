package org.openml.webapplication.foldgenerators;

import java.util.Random;

import org.openml.apiconnector.xml.EstimationProcedure;

import weka.core.Instances;

public class HoldoutOrderedSplitsGenerator extends FoldGeneratorBase {
	
	public HoldoutOrderedSplitsGenerator(Instances dataset, EstimationProcedure evaluationMethod, Random random, String splitsName)  throws Exception  {
		super(dataset, evaluationMethod, random, splitsName);
	}
	
	public Instances generate() throws Exception {
		Instances splits = new Instances(splitsName, arffMapping.getArffHeader(), splitsSize);
		
		for (int f = 0; f < dataset.numInstances(); ++f) {
			int testSetSize = Math.round(dataset.numInstances() * evaluationMethod.getPercentage() / 100);
			splits.add(arffMapping.createInstance(f >= testSetSize, 0, 0, f));
		}
		return splits;
	}
}
