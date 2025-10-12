package org.openml.webapplication.foldgenerators;

import org.openml.apiconnector.xml.EstimationProcedure;

import weka.core.Instances;

public class LeaveOneOutSplitsGenerator extends FoldGeneratorBase {
	
	public LeaveOneOutSplitsGenerator(Instances dataset, EstimationProcedure evaluationMethod, String splitsName) throws Exception {
		super(dataset, evaluationMethod, null, splitsName);
	}
	
	public Instances generate() throws Exception {
		Instances splits = new Instances(splitsName, arffMapping.getArffHeader(), splitsSize);
		for (int f = 0; f < dataset.numInstances(); ++f) {
			for (int i = 0; i < dataset.numInstances(); ++i) {
				int rowid = (int) dataset.instance(i).value(0);
				splits.add(arffMapping.createInstance(f != i, rowid, 0, f));
			}
		}
		return splits;
	}
	
	public Instances generate_learningcurve() throws Exception {
		throw new Exception("Learning Curve samples not defined for this SplitGenerator");
	}
}
