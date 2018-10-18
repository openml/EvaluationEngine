package org.openml.webapplication.testutils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

public class DatasetFactory {
	
	public static final Instances getXORNumericNoClass() {
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		attributes.add(new Attribute("x1"));
		attributes.add(new Attribute("x2"));
		attributes.add(new Attribute("y"));
		Instances xorDataset = new Instances("XorNumeric", attributes , 0);

		xorDataset.add(new DenseInstance(1.0, new double[] {0.0, 0.0, 0.0}));
		xorDataset.add(new DenseInstance(1.0, new double[] {0.0, 1.0, 1.0}));
		xorDataset.add(new DenseInstance(1.0, new double[] {1.0, 0.0, 1.0}));
		xorDataset.add(new DenseInstance(1.0, new double[] {1.0, 1.0, 0.0}));
		
		return xorDataset;
	}
	
	public static final Instances getXORNumeric() {
		Instances xorDataset = getXORNumericNoClass();
		xorDataset.setClassIndex(xorDataset.numAttributes() - 1);
		return xorDataset;
	}
	
	public static final Instances getXORMixedNoClass() {
		List<String> values = new ArrayList<String>();
		values.add("False");
		values.add("True");
		
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		attributes.add(new Attribute("x1"));
		attributes.add(new Attribute("x2"));
		attributes.add(new Attribute("y", values));
		Instances xorDataset = new Instances("XorMixed", attributes , 0);

		xorDataset.add(new DenseInstance(1.0, new double[] {0.0, 0.0, 0.0}));
		xorDataset.add(new DenseInstance(1.0, new double[] {0.0, 1.0, 1.0}));
		xorDataset.add(new DenseInstance(1.0, new double[] {1.0, 0.0, 1.0}));
		xorDataset.add(new DenseInstance(1.0, new double[] {1.0, 1.0, 0.0}));
		
		return xorDataset;
	}
	
	public static final Instances getXORMixed() {
		Instances xorDataset = getXORMixedNoClass();
		xorDataset.setClassIndex(xorDataset.numAttributes() - 1);
		return xorDataset;
	}
	
	public static final Instances getXORNominalNoClass() {
		List<String> values = new ArrayList<String>();
		values.add("False");
		values.add("True");
		
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		attributes.add(new Attribute("x1", values));
		attributes.add(new Attribute("x2", values));
		attributes.add(new Attribute("class", values));
		Instances xorDataset = new Instances("XorNominal", attributes , 4);

		xorDataset.add(new DenseInstance(1.0, new double[] {0.0, 0.0, 0.0}));
		xorDataset.add(new DenseInstance(1.0, new double[] {0.0, 1.0, 1.0}));
		xorDataset.add(new DenseInstance(1.0, new double[] {1.0, 0.0, 1.0}));
		xorDataset.add(new DenseInstance(1.0, new double[] {1.0, 1.0, 0.0}));
		
		return xorDataset;
	}
	
	public static final Instances getXORNominal() {
		Instances xorDataset = getXORNominalNoClass();
		xorDataset.setClassIndex(xorDataset.numAttributes() - 1);
		return xorDataset;
	}
	
	public static final Instances getXORNominalObfuscatedNoClass() {
		// obfuscated version of xor: x1 xor x2 = class. Includes column with missing values
		List<String> values = new ArrayList<String>();
		values.add("False");
		values.add("True");
		List<String> valuesObfuscated = new ArrayList<String>();
		valuesObfuscated.add("False");
		valuesObfuscated.add("True");
		valuesObfuscated.add("a");
		valuesObfuscated.add("b");
		valuesObfuscated.add("c");
		
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		attributes.add(new Attribute("x1", values));
		attributes.add(new Attribute("x2", valuesObfuscated));
		attributes.add(new Attribute("x3", valuesObfuscated));
		attributes.add(new Attribute("class", valuesObfuscated));
		Instances xorDataset = new Instances("XorNominalObfuscated", attributes , 4);
		xorDataset.setClassIndex(xorDataset.numAttributes() - 1);

		xorDataset.add(new DenseInstance(1.0, new double[] {0.0, 0.0, Double.NaN, 0.0}));
		xorDataset.add(new DenseInstance(1.0, new double[] {0.0, 1.0, Double.NaN, 1.0}));
		xorDataset.add(new DenseInstance(1.0, new double[] {1.0, 0.0, Double.NaN, 1.0}));
		xorDataset.add(new DenseInstance(1.0, new double[] {1.0, 1.0, Double.NaN, 0.0}));
		
		return xorDataset;
	}
	
	public static final Instances getXORNominalObfuscated() {
		Instances xorDataset = getXORNominalObfuscatedNoClass();
		xorDataset.setClassIndex(xorDataset.numAttributes() - 1);
		return xorDataset;
	}
	
	public static final List<String> differences(
			Map<String, Double> expected, Map<String, Double> result) throws Exception {
		if (!expected.keySet().equals(result.keySet())) {
			throw new Exception("KeySets not equal.");
		}
		List<String> differences = new ArrayList<>();
		for (String feature : expected.keySet()) {
			if (result.get(feature) == null) {
				// necessary to avoid nullpointer exception
				if (expected.get(feature) != null) {
					differences.add(feature);
				}
			} else if (!result.get(feature).equals(expected.get(feature))) {
				differences.add(feature);
			}
		}
		return differences;
	}
}
