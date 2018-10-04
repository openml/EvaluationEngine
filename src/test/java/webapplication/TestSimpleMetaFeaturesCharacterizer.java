package webapplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;
import org.openml.webapplication.fantail.dc.Characterizer;
import org.openml.webapplication.fantail.dc.statistical.SimpleMetaFeatures;

import weka.core.Instances;

public class TestSimpleMetaFeaturesCharacterizer {

	private static final Characterizer characterizer = new SimpleMetaFeatures();
	
	private static final Map<String, Double> getXORNumericExpectedResults() {
		Map<String, Double> results = new TreeMap<String, Double>();
		results.put("NumberOfInstances", 4.0);
		results.put("NumberOfFeatures", 3.0);
		results.put("NumberOfClasses", 0.0);
		results.put("Dimensionality", 0.75);
		results.put("NumberOfInstancesWithMissingValues", 0.0); 
		results.put("NumberOfMissingValues", 0.0);
		results.put("PercentageOfInstancesWithMissingValues", 0.0);
		results.put("PercentageOfMissingValues", 0.0);
		results.put("NumberOfNumericFeatures", 3.0);
		results.put("NumberOfSymbolicFeatures", 0.0);
		results.put("NumberOfBinaryFeatures", 0.0); 
		results.put("PercentageOfNumericFeatures", 100.0);
		results.put("PercentageOfSymbolicFeatures", 0.0);
		results.put("PercentageOfBinaryFeatures", 0.0);
		results.put("MajorityClassSize", null);
		results.put("MinorityClassSize", null);
		results.put("MajorityClassPercentage", null);
		results.put("MinorityClassPercentage", null);
		results.put("AutoCorrelation", 1.0 / 3.0);
		return results;
	}
	
	private static final Map<String, Double> getXORNumericNoClassExpectedResults() {
		Map<String, Double> results = new TreeMap<String, Double>();
		results.put("NumberOfInstances", 4.0);
		results.put("NumberOfFeatures", 3.0);
		results.put("NumberOfClasses", null);
		results.put("Dimensionality", 0.75);
		results.put("NumberOfInstancesWithMissingValues", 0.0); 
		results.put("NumberOfMissingValues", 0.0);
		results.put("PercentageOfInstancesWithMissingValues", 0.0);
		results.put("PercentageOfMissingValues", 0.0);
		results.put("NumberOfNumericFeatures", 3.0);
		results.put("NumberOfSymbolicFeatures", 0.0);
		results.put("NumberOfBinaryFeatures", 0.0); 
		results.put("PercentageOfNumericFeatures", 100.0);
		results.put("PercentageOfSymbolicFeatures", 0.0);
		results.put("PercentageOfBinaryFeatures", 0.0);
		results.put("MajorityClassSize", null);
		results.put("MinorityClassSize", null);
		results.put("MajorityClassPercentage", null);
		results.put("MinorityClassPercentage", null);
		results.put("AutoCorrelation", null);
		return results;
	}
	
	private static final Map<String, Double> getXORNominalExpectedResults() {
		Map<String, Double> results = new TreeMap<String, Double>();
		results.put("NumberOfInstances", 4.0);
		results.put("NumberOfFeatures", 3.0);
		results.put("NumberOfClasses", 2.0);
		results.put("Dimensionality", 0.75);
		results.put("NumberOfInstancesWithMissingValues", 0.0); 
		results.put("NumberOfMissingValues", 0.0);
		results.put("PercentageOfInstancesWithMissingValues", 0.0);
		results.put("PercentageOfMissingValues", 0.0);
		results.put("NumberOfNumericFeatures", 0.0);
		results.put("NumberOfSymbolicFeatures", 3.0);
		results.put("NumberOfBinaryFeatures", 3.0);
		results.put("PercentageOfNumericFeatures", 0.0);
		results.put("PercentageOfSymbolicFeatures", 100.0);
		results.put("PercentageOfBinaryFeatures", 100.0);
		results.put("MajorityClassSize", 2.0);
		results.put("MinorityClassSize", 2.0);
		results.put("MajorityClassPercentage", 50.0);
		results.put("MinorityClassPercentage", 50.0);
		results.put("AutoCorrelation", 1.0 / 3.0);
		return results;
	}
	
	private static final Map<String, Double> getXORNominalObfuscatedExpectedResults() {
		Map<String, Double> results = new TreeMap<String, Double>();
		results.put("NumberOfInstances", 4.0);
		results.put("NumberOfFeatures", 4.0);
		results.put("NumberOfClasses", 2.0);
		results.put("Dimensionality", 1.0);
		results.put("NumberOfInstancesWithMissingValues", 4.0); 
		results.put("NumberOfMissingValues", 4.0);
		results.put("PercentageOfInstancesWithMissingValues", 100.0);
		results.put("PercentageOfMissingValues", 25.0);
		results.put("NumberOfNumericFeatures", 0.0);
		results.put("NumberOfSymbolicFeatures", 4.0);
		results.put("NumberOfBinaryFeatures", 1.0);
		results.put("PercentageOfNumericFeatures", 0.0);
		results.put("PercentageOfSymbolicFeatures", 100.0);
		results.put("PercentageOfBinaryFeatures", 25.0);
		results.put("MajorityClassSize", 2.0);
		results.put("MinorityClassSize", 2.0);
		results.put("MajorityClassPercentage", 50.0);
		results.put("MinorityClassPercentage", 50.0);
		results.put("AutoCorrelation", 1.0 / 3.0);
		return results;
	}
	
	@Test
	public void testSimpleFeaturesXorNumeric() throws Exception {
		Instances xor = DatasetFactory.getXORNumeric();
		Map<String, Double> expectedResults = getXORNumericExpectedResults();
		
		// Check the produced class count
		Map<String,Double> metafeatures = characterizer.characterizeAll(xor);
		List<String> mismatches = DatasetFactory.differences(expectedResults, metafeatures);
		if (mismatches.size() != 0) {
			fail("Mismatches (" + mismatches.size() + "): " + mismatches.toString());
		}
	}
	
	@Test
	public void testSimpleFeaturesXorNumericNoClass() throws Exception {
		Instances xor = DatasetFactory.getXORNumericNoClass();
		Map<String, Double> expectedResults = getXORNumericNoClassExpectedResults();
		
		// Check the produced class count
		Map<String,Double> metafeatures = characterizer.characterizeAll(xor);
		List<String> mismatches = DatasetFactory.differences(expectedResults, metafeatures);
		if (mismatches.size() != 0) {
			fail("Mismatches (" + mismatches.size() + "): " + mismatches.toString());
		}
	}
	
	@Test
	public void testSimpleFeaturesXorNominal() throws Exception {
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
	public void testSimpleFeaturesXorNominalObfuscated() throws Exception {
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
