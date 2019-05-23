package org.openml.webapplication.splits;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;
import org.openml.apiconnector.xml.EstimationProcedure;
import org.openml.apiconnector.xml.EstimationProcedureType;
import org.openml.webapplication.foldgenerators.HoldoutOrderedSplitsGenerator;
import org.openml.webapplication.testutils.BaseTestFramework;

import weka.core.Attribute;
import weka.core.Instances;

public class TestSplitGeneration  extends BaseTestFramework {
	
	@Test
	public void testHoldoutOrdered() throws Exception {
		Instances weather = new Instances(new FileReader(new File("data/test/datasets/weather.arff")));
		EstimationProcedure ep = new EstimationProcedure(-1, 1, "nameless", EstimationProcedureType.HOLDOUT_ORDERED, 1, 1, 20, "false");
		
		HoldoutOrderedSplitsGenerator hosg = new HoldoutOrderedSplitsGenerator(weather, ep, "weather-splits");
		Instances splits = hosg.generate();
		Attribute typeTrainTest = splits.attribute("type");
		Attribute rowIdAtt = splits.attribute("rowid");
		
		Set<Integer> train = new TreeSet<Integer>();
		Set<Integer> test = new TreeSet<Integer>();
		Set<Integer> reference = new TreeSet<Integer>();
		
		for (int i = 0; i < splits.size(); ++i) {
			reference.add(i);
			String typeValue = splits.get(i).stringValue(typeTrainTest);
			Integer rowId = (int) splits.get(i).value(rowIdAtt);
			if (typeValue.equals("TRAIN")) {
				train.add(rowId);
			} else if (typeValue.equals("TEST")) {
				test.add(rowId);
			} else {
				throw new Exception("Attribute type does not contain legal value: " + typeValue);
			}
		}
		Set<Integer> trainAndTest = new TreeSet<Integer>();
		trainAndTest.addAll(train);
		trainAndTest.addAll(test);
		
		// checks whether there are no duplicate row id entities
		assertEquals(trainAndTest.size(), splits.size());
		assertEquals(trainAndTest.size(), weather.size());
		
		// checks whether row ids go up subsequently
		assertTrue(trainAndTest.equals(reference));
		
		// checks whether test instances start after train instances
		assertTrue(Collections.max(train) < Collections.min(test));
		
		// checks right percentage. Plus 2 because rounding makes things hard
		double percentageReal = (test.size() * 1.0) / weather.size() * 100;
		double percentagePlus = (test.size() + 2.0) / weather.size() * 100;
		double percentageMinus = (test.size() - 2.0) / weather.size() * 100;
		double diffPercentageReal = Math.abs(percentageReal - ep.getPercentage());
		double diffPercentagePlus = Math.abs(percentagePlus - ep.getPercentage());
		double diffPercentageMinus = Math.abs(percentageMinus - ep.getPercentage());
		
		// might be too harsh
		assertTrue(diffPercentageReal < diffPercentageMinus);
		assertTrue(diffPercentageReal < diffPercentagePlus);
	}
}
