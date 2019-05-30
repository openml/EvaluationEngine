package org.openml.webapplication.foldgenerators;

import org.openml.apiconnector.xml.EstimationProcedure;

import weka.core.Instances;

public class TrainOnTestSplitsGenerator extends FoldGeneratorBase {
	
	public TrainOnTestSplitsGenerator(Instances dataset, EstimationProcedure evaluationMethod, String splitsName) throws Exception {
		super(dataset, evaluationMethod, null, splitsName);
	}
	
	public Instances generate() throws Exception {
		Instances splits = new Instances(splitsName, arffMapping.getArffHeader(), splitsSize);
		for (int i = 0; i < dataset.numInstances(); ++i) {
			int rowid = (int) dataset.instance(i).value(0);
			splits.add(arffMapping.createInstance(false, rowid, 0, 0));
			splits.add(arffMapping.createInstance(true, rowid, 0, 0));
		}
		
		return splits;
	}
}
