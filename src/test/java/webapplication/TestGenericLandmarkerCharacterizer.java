package webapplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;
import org.openml.webapplication.fantail.dc.Characterizer;
import org.openml.webapplication.fantail.dc.landmarking.GenericLandmarker;

import weka.core.Instances;

public class TestGenericLandmarkerCharacterizer {
	
	private static final Characterizer characterizer = new GenericLandmarker("DecisionStump", "weka.classifiers.trees.DecisionStump", 2, null);
	
	private static final Map<String, Double> getXORNumericExpectedResults() {
		Map<String, Double> results = new TreeMap<String, Double>();
		results.put("DecisionStumpAUC", null);
		results.put("DecisionStumpErrRate", null);
		results.put("DecisionStumpKappa", null);
		return results;
	}
	
	private static final Map<String, Double> getXORNominalExpectedResults() {
		Map<String, Double> results = new TreeMap<String, Double>();
		// If the decision stump tries to learn on a stratified fold, it will always fail. 
		results.put("DecisionStumpAUC", 0.0);
		results.put("DecisionStumpErrRate", 1.0);
		results.put("DecisionStumpKappa", -1.0);
		return results;
	}
	
	private static final Map<String, Double> getXORNominalObfuscatedExpectedResults() {
		Map<String, Double> results = new TreeMap<String, Double>();
		// If the decision stump tries to learn on a stratified fold, it will always fail. 
		results.put("DecisionStumpAUC", 0.0);
		results.put("DecisionStumpErrRate", 1.0);
		results.put("DecisionStumpKappa", -1.0);
		return results;
	}
	
	@Test
	public void testLandmarkerXorNumeric() throws Exception {
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
	public void testLandmarkerXorNominal() throws Exception {
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
	public void testLandmarkerXorNominalObfuscated() throws Exception {
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
