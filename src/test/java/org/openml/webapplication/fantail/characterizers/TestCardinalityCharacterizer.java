package org.openml.webapplication.fantail.characterizers;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;
import org.openml.webapplication.fantail.dc.Characterizer;
import org.openml.webapplication.fantail.dc.statistical.Cardinality;
import org.openml.webapplication.testutils.DatasetFactory;

import weka.core.Instances;

public class TestCardinalityCharacterizer {

	private static final Characterizer characterizer = new Cardinality();
	
	private static final Map<String, Double> getXORNumericExpectedResults() {
		Map<String, Double> results = new TreeMap<String, Double>();
		results.put("MeanCardinalityOfNumericAttributes", 2.0);
		results.put("StdevCardinalityOfNumericAttributes", 0.0);
		results.put("MinCardinalityOfNumericAttributes", 2.0);
		results.put("MaxCardinalityOfNumericAttributes", 2.0);
		results.put("MeanCardinalityOfNominalAttributes", null);
		results.put("StdevCardinalityOfNominalAttributes", null);
		results.put("MinCardinalityOfNominalAttributes", null);
		results.put("MaxCardinalityOfNominalAttributes", null);
		results.put("CardinalityAtTwo", 4.0);
		results.put("CardinalityAtThree", 8.0);
		results.put("CardinalityAtFour", null);
		return results;
	}
	
	private static final Map<String, Double> getXORNominalExpectedResults() {
		Map<String, Double> results = new TreeMap<String, Double>();
		results.put("MeanCardinalityOfNumericAttributes", null);
		results.put("StdevCardinalityOfNumericAttributes", null);
		results.put("MinCardinalityOfNumericAttributes", null);
		results.put("MaxCardinalityOfNumericAttributes", null);
		results.put("MeanCardinalityOfNominalAttributes", 2.0);
		results.put("StdevCardinalityOfNominalAttributes", 0.0);
		results.put("MinCardinalityOfNominalAttributes", 2.0);
		results.put("MaxCardinalityOfNominalAttributes", 2.0);
		results.put("CardinalityAtTwo", 4.0);
		results.put("CardinalityAtThree", 8.0);
		results.put("CardinalityAtFour", null);
		return results;
	}
	
	private static final Map<String, Double> getXORNominalObfuscatedExpectedResults() {
		Map<String, Double> results = new TreeMap<String, Double>();
		results.put("MeanCardinalityOfNumericAttributes", null);
		results.put("StdevCardinalityOfNumericAttributes", null);
		results.put("MinCardinalityOfNumericAttributes", null);
		results.put("MaxCardinalityOfNumericAttributes", null);
		results.put("MeanCardinalityOfNominalAttributes", 1.5);
		results.put("StdevCardinalityOfNominalAttributes", 1.0);
		results.put("MinCardinalityOfNominalAttributes", 0.0);
		results.put("MaxCardinalityOfNominalAttributes", 2.0);
		results.put("CardinalityAtTwo", 4.0);
		results.put("CardinalityAtThree", 8.0);
		results.put("CardinalityAtFour", 8.0);
		return results;
	}
	
	@Test
	public void testCardinalityXorNumeric() throws Exception {
		Instances xor = DatasetFactory.getXORNumeric();
		Map<String, Double> expectedResults = getXORNumericExpectedResults();
		
		// Check the produced class count
		Map<String,Double> metafeatures = characterizer.characterizeAll(xor);
		List<String> mismatches = DatasetFactory.differences(expectedResults, metafeatures);
		if (mismatches.size() != 0) {
			fail("Mismatches (" + mismatches.size() + "): " + mismatches.toString());
		}
		
		assertEquals(0, mismatches.size());
	}
	
	@Test
	public void testCardinalityXorNumericNoClass() throws Exception {
		Instances xor = DatasetFactory.getXORNumericNoClass();
		// results currently the same as vanilla numeric
		Map<String, Double> expectedResults = getXORNumericExpectedResults();
		
		// Check the produced class count
		Map<String,Double> metafeatures = characterizer.characterizeAll(xor);
		List<String> mismatches = DatasetFactory.differences(expectedResults, metafeatures);
		if (mismatches.size() != 0) {
			fail("Mismatches (" + mismatches.size() + "): " + mismatches.toString());
		}
		
		assertEquals(0, mismatches.size());
	}
	
	@Test
	public void testCardinalityXorNominal() throws Exception {
		Instances xor = DatasetFactory.getXORNominal();
		Map<String, Double> expectedResults = getXORNominalExpectedResults();
		
		// Check the produced class count
		Map<String,Double> metafeatures = characterizer.characterizeAll(xor);
		List<String> mismatches = DatasetFactory.differences(expectedResults, metafeatures);
		if (mismatches.size() != 0) {
			fail("Mismatches (" + mismatches.size() + "): " + mismatches.toString());
		}
		
		assertEquals(0, mismatches.size());
	}

	@Test
	public void testCardinalityXorNominalObfuscated() throws Exception {
		Instances xor = DatasetFactory.getXORNominalObfuscated();
		Map<String, Double> expectedResults = getXORNominalObfuscatedExpectedResults();
		
		// Check the produced class count
		Map<String,Double> metafeatures = characterizer.characterizeAll(xor);
		List<String> mismatches = DatasetFactory.differences(expectedResults, metafeatures);
		if (mismatches.size() != 0) {
			fail("Mismatches (" + mismatches.size() + "): " + mismatches.toString());
		}
		
		assertEquals(0, mismatches.size());
	}
}
