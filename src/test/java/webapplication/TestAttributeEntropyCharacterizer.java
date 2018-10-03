package webapplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;
import org.openml.webapplication.fantail.dc.Characterizer;
import org.openml.webapplication.fantail.dc.statistical.AttributeEntropy;

import weka.core.Instances;

public class TestAttributeEntropyCharacterizer {
	
	private static final Characterizer characterizer = new AttributeEntropy();
	
	private static final Map<String, Double> getXORNumericExpectedResults() {
		Map<String, Double> results = new TreeMap<String, Double>();
		results.put("ClassEntropy", null);
		results.put("MeanAttributeEntropy", null);
		results.put("MeanMutualInformation", null);
		results.put("EquivalentNumberOfAtts", null);
		results.put("MeanNoiseToSignalRatio", null);
		results.put("MinAttributeEntropy", null);
		results.put("MinMutualInformation", null);
		results.put("MaxAttributeEntropy", null);
		results.put("MaxMutualInformation", null);
		results.put("Quartile1AttributeEntropy", null);
		results.put("Quartile1MutualInformation", null);
		results.put("Quartile2AttributeEntropy", null);
		results.put("Quartile2MutualInformation", null);
		results.put("Quartile3AttributeEntropy", null);
		results.put("Quartile3MutualInformation", null);
		
		return results;
	}

	private static final Map<String, Double> getXORNominalExpectedResults() {
		Map<String, Double> results = new TreeMap<String, Double>();
		results.put("ClassEntropy", 1.0);
		results.put("MeanAttributeEntropy", 1.0);
		results.put("MeanMutualInformation", 0.0);
		results.put("EquivalentNumberOfAtts", null);
		results.put("MeanNoiseToSignalRatio", null);
		results.put("MinAttributeEntropy", 1.0);
		results.put("MinMutualInformation", 0.0);
		results.put("MaxAttributeEntropy", 1.0);
		results.put("MaxMutualInformation", 0.0);
		results.put("Quartile1AttributeEntropy", 1.0);
		results.put("Quartile1MutualInformation", 0.0);
		results.put("Quartile2AttributeEntropy", 1.0);
		results.put("Quartile2MutualInformation", 0.0);
		results.put("Quartile3AttributeEntropy", 1.0);
		results.put("Quartile3MutualInformation", 0.0);
		
		return results;
	}

	private static final Map<String, Double> getXORNominalObfuscatedExpectedResults() {
		Map<String, Double> results = new TreeMap<String, Double>();
		results.put("ClassEntropy", 1.0);
		results.put("MeanAttributeEntropy", 0.6666666666666666);
		results.put("MeanMutualInformation", 0.0);
		results.put("EquivalentNumberOfAtts", null);
		results.put("MeanNoiseToSignalRatio", null);
		results.put("MinAttributeEntropy", -0.0);
		results.put("MinMutualInformation", 0.0);
		results.put("MaxAttributeEntropy", 1.0);
		results.put("MaxMutualInformation", 0.0);
		results.put("Quartile1AttributeEntropy", 0.0);
		results.put("Quartile1MutualInformation", 0.0);
		results.put("Quartile2AttributeEntropy", 1.0);
		results.put("Quartile2MutualInformation", 0.0);
		results.put("Quartile3AttributeEntropy", 1.0);
		results.put("Quartile3MutualInformation", 0.0);
		
		return results;
	}
	
	@Test
	public void testAttributeEntropyXorNumeric() throws Exception {
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
	public void testAttributeEntropyXorNominal() throws Exception {
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
	public void testAttributeEntropyXorNominalObfuscated() throws Exception {
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
