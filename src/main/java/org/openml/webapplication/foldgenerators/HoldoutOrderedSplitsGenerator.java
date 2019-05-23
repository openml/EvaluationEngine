package org.openml.webapplication.foldgenerators;

import org.openml.apiconnector.xml.EstimationProcedure;

import weka.core.Instances;

public class HoldoutOrderedSplitsGenerator extends FoldGeneratorBase {
	
	public HoldoutOrderedSplitsGenerator(Instances dataset, EstimationProcedure evaluationMethod, String splitsName)  throws Exception  {
		super(dataset, evaluationMethod, null, splitsName);
	}
	
	public Instances generate() throws Exception {
		Instances splits = new Instances(splitsName, arffMapping.getArffHeader(), splitsSize);
		
		for (int f = 0; f < dataset.numInstances(); ++f) {
			double testSetSize = dataset.numInstances() * evaluationMethod.getPercentage() / 100.0;
			double threshold = dataset.numInstances() - testSetSize;
			splits.add(arffMapping.createInstance(f <= threshold, f, 0, 0));
		}
		return splits;
	}
}
