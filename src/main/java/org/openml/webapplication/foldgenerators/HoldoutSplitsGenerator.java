package org.openml.webapplication.foldgenerators;

import java.util.Random;

import org.openml.apiconnector.xml.EstimationProcedure;

import weka.core.Instances;

public class HoldoutSplitsGenerator extends FoldGeneratorBase {
	
	public HoldoutSplitsGenerator(Instances dataset, EstimationProcedure evaluationMethod, Random random, String splitsName)  throws Exception  {
		super(dataset, evaluationMethod, random, splitsName);
	}
	
	public Instances generate() throws Exception {
		Instances splits = new Instances(splitsName, arffMapping.getArffHeader(), splitsSize);
		for (int r = 0; r < evaluationMethod.getRepeats(); ++r) {
			dataset.randomize(random);
			int testSetSize = Math.round(dataset.numInstances() * evaluationMethod.getPercentage() / 100);

			for (int i = 0; i < dataset.numInstances(); ++i) {
				int rowid = (int) dataset.instance(i).value(0);
				splits.add(arffMapping.createInstance(i >= testSetSize, rowid, r, 0));
			}
		}
		return splits;
	}
}
