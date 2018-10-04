package webapplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;
import org.openml.webapplication.fantail.dc.Characterizer;
import org.openml.webapplication.fantail.dc.statistical.NominalAttDistinctValues;

import weka.core.Instances;

public class TestNominalAttDistinctValuesCharacterizer {
	
	private static final Characterizer characterizer = new NominalAttDistinctValues();
	
	private static final Map<String, Double> getXORNumericExpectedResults() {
		Map<String, Double> results = new TreeMap<String, Double>();
		results.put("MaxNominalAttDistinctValues", null);
		results.put("MinNominalAttDistinctValues", null);
		results.put("MeanNominalAttDistinctValues", null);
		results.put("StdvNominalAttDistinctValues", null);
		return results;
	}
	
	private static final Map<String, Double> getXORNominalExpectedResults() {
		Map<String, Double> results = new TreeMap<String, Double>();
		results.put("MaxNominalAttDistinctValues", 2.0);
		results.put("MinNominalAttDistinctValues", 2.0);
		results.put("MeanNominalAttDistinctValues", 2.0);
		results.put("StdvNominalAttDistinctValues", 0.0);
		return results;
	}
	
	private static final Map<String, Double> getXORNominalObfuscatedExpectedResults() {
		Map<String, Double> results = new TreeMap<String, Double>();
		results.put("MaxNominalAttDistinctValues", 2.0);
		results.put("MinNominalAttDistinctValues", 0.0);
		results.put("MeanNominalAttDistinctValues", 1.5);
		results.put("StdvNominalAttDistinctValues", 1.0);
		return results;
	}
	
	@Test
	public void testNominalAttDistinctValuesFeaturesXorNumeric() throws Exception {
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
	public void testNominalAttDistinctValuesXorNominal() throws Exception {
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
	public void testNominalAttDistinctValuesXorNominalObfuscated() throws Exception {
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
