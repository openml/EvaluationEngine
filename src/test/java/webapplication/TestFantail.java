package webapplication;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.openml.webapplication.fantail.dc.statistical.SimpleMetaFeatures;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

public class TestFantail {

	private static final SimpleMetaFeatures simpleMetaFeatures = new SimpleMetaFeatures();

	
	@Test
	public void testNumberOfClasses() throws Exception {

		// Make XOR dataset
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		attributes.add(new Attribute("x"));
		attributes.add(new Attribute("y"));
		attributes.add(new Attribute("target"));
		Instances artificialData = new Instances("TestInstances", attributes , 0);
		artificialData.setClassIndex(artificialData.numAttributes() - 1);

		artificialData.add(new DenseInstance(1.0, new double[] {0.0, 1.0, 0.5}));
		artificialData.add(new DenseInstance(1.0, new double[] {1.0, 0.0, 0.9}));

		// Check the produced class count
		Map<String,Double> metafeatures = simpleMetaFeatures.characterize(artificialData);
		assertEquals(metafeatures.get("NumberOfClasses"),new Double(0.0));
	}
}
