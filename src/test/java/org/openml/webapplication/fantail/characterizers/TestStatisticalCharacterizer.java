package org.openml.webapplication.fantail.characterizers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;
import org.openml.webapplication.fantail.dc.Characterizer;
import org.openml.webapplication.fantail.dc.statistical.Statistical;
import org.openml.webapplication.testutils.DatasetFactory;

import weka.core.Instances;

public class TestStatisticalCharacterizer {
	
	private static final Characterizer characterizer = new Statistical();

	private static final Map<String, Double> getXORNumericExpectedResults() {
		double mean = 0.5;
		double stdev = 0.5773502691896257; 
		double skewness = 0.0;
		double kurtosis = -5.999999999999998; // TODO: this is probably a bug!
		
		Map<String, Double> results = new TreeMap<String, Double>();
		results.put("MeanMeansOfNumericAtts", mean);
		results.put("MeanStdDevOfNumericAtts", stdev);
		results.put("MeanKurtosisOfNumericAtts", kurtosis);
		results.put("MeanSkewnessOfNumericAtts", skewness);

		results.put("MinMeansOfNumericAtts", mean);
		results.put("MinStdDevOfNumericAtts", stdev);
		results.put("MinKurtosisOfNumericAtts", kurtosis);
		results.put("MinSkewnessOfNumericAtts", skewness);

		results.put("MaxMeansOfNumericAtts", mean);
		results.put("MaxStdDevOfNumericAtts", stdev);
		results.put("MaxKurtosisOfNumericAtts", kurtosis);
		results.put("MaxSkewnessOfNumericAtts", skewness);

		results.put("Quartile1MeansOfNumericAtts", mean);
		results.put("Quartile1StdDevOfNumericAtts", stdev);
		results.put("Quartile1KurtosisOfNumericAtts", kurtosis);
		results.put("Quartile1SkewnessOfNumericAtts", skewness);

		results.put("Quartile2MeansOfNumericAtts", mean);
		results.put("Quartile2StdDevOfNumericAtts", stdev);
		results.put("Quartile2KurtosisOfNumericAtts", kurtosis);
		results.put("Quartile2SkewnessOfNumericAtts", skewness);

		results.put("Quartile3MeansOfNumericAtts", mean);
		results.put("Quartile3StdDevOfNumericAtts", stdev);
		results.put("Quartile3KurtosisOfNumericAtts", kurtosis);
		results.put("Quartile3SkewnessOfNumericAtts", skewness);
		
		return results;
	}
	
	private static final Map<String, Double> getXORMixedExpectedResults() {
		double mean = 0.5;
		double stdev = 0.5773502691896257; 
		double skewness = 0.0;
		double kurtosis = -5.999999999999998; // TODO: this is probably a bug!
		
		Map<String, Double> results = new TreeMap<String, Double>();
		results.put("MeanMeansOfNumericAtts", mean);
		results.put("MeanStdDevOfNumericAtts", stdev);
		results.put("MeanKurtosisOfNumericAtts", kurtosis);
		results.put("MeanSkewnessOfNumericAtts", skewness);

		results.put("MinMeansOfNumericAtts", mean);
		results.put("MinStdDevOfNumericAtts", stdev);
		results.put("MinKurtosisOfNumericAtts", kurtosis);
		results.put("MinSkewnessOfNumericAtts", skewness);

		results.put("MaxMeansOfNumericAtts", mean);
		results.put("MaxStdDevOfNumericAtts", stdev);
		results.put("MaxKurtosisOfNumericAtts", kurtosis);
		results.put("MaxSkewnessOfNumericAtts", skewness);

		results.put("Quartile1MeansOfNumericAtts", mean);
		results.put("Quartile1StdDevOfNumericAtts", stdev);
		results.put("Quartile1KurtosisOfNumericAtts", kurtosis);
		results.put("Quartile1SkewnessOfNumericAtts", skewness);

		results.put("Quartile2MeansOfNumericAtts", mean);
		results.put("Quartile2StdDevOfNumericAtts", stdev);
		results.put("Quartile2KurtosisOfNumericAtts", kurtosis);
		results.put("Quartile2SkewnessOfNumericAtts", skewness);

		results.put("Quartile3MeansOfNumericAtts", mean);
		results.put("Quartile3StdDevOfNumericAtts", stdev);
		results.put("Quartile3KurtosisOfNumericAtts", kurtosis);
		results.put("Quartile3SkewnessOfNumericAtts", skewness);
		
		return results;
	}
	
	private static final Map<String, Double> getXORNominalExpectedResults() {
		Map<String, Double> results = new TreeMap<String, Double>();
		results.put("MeanMeansOfNumericAtts", null);
		results.put("MeanStdDevOfNumericAtts", null);
		results.put("MeanKurtosisOfNumericAtts", null);
		results.put("MeanSkewnessOfNumericAtts", null);

		results.put("MinMeansOfNumericAtts", null);
		results.put("MinStdDevOfNumericAtts", null);
		results.put("MinKurtosisOfNumericAtts", null);
		results.put("MinSkewnessOfNumericAtts", null);

		results.put("MaxMeansOfNumericAtts", null);
		results.put("MaxStdDevOfNumericAtts", null);
		results.put("MaxKurtosisOfNumericAtts", null);
		results.put("MaxSkewnessOfNumericAtts", null);

		results.put("Quartile1MeansOfNumericAtts", null);
		results.put("Quartile1StdDevOfNumericAtts", null);
		results.put("Quartile1KurtosisOfNumericAtts", null);
		results.put("Quartile1SkewnessOfNumericAtts", null);

		results.put("Quartile2MeansOfNumericAtts", null);
		results.put("Quartile2StdDevOfNumericAtts", null);
		results.put("Quartile2KurtosisOfNumericAtts", null);
		results.put("Quartile2SkewnessOfNumericAtts", null);

		results.put("Quartile3MeansOfNumericAtts", null);
		results.put("Quartile3StdDevOfNumericAtts", null);
		results.put("Quartile3KurtosisOfNumericAtts", null);
		results.put("Quartile3SkewnessOfNumericAtts", null);
		
		return results;
	}
	
	@Test
	public void testStatisticalFeaturesXorNumeric() throws Exception {
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
	public void testStatisticalFeaturesXorNumericNoClass() throws Exception {
		Instances xor = DatasetFactory.getXORNumericNoClass();
		// no class results are exactly equal to the with class results
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
	public void testStatisticalFeaturesXorMixed() throws Exception {
		Instances xor = DatasetFactory.getXORMixed();
		Map<String, Double> expectedResults = getXORMixedExpectedResults();
		
		// Check the produced class count
		Map<String,Double> metafeatures = characterizer.characterizeAll(xor);
		List<String> mismatches = DatasetFactory.differences(expectedResults, metafeatures);
		if (mismatches.size() != 0) {
			fail("Mismatches (" + mismatches.size() + "): " + mismatches.toString());
		}
		
		assertEquals(0, mismatches.size());
	}
	
	@Test
	public void testStatisticalFeaturesXorNominal() throws Exception {
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
	public void testStatisticalFeaturesXorNominalObfuscated() throws Exception {
		Instances xor = DatasetFactory.getXORNominalObfuscated();
		// obfuscated results are exactly equal to the non-obfuscated results
		Map<String, Double> expectedResults = getXORNominalExpectedResults(); 
		
		// Check the produced class count
		Map<String,Double> metafeatures = characterizer.characterizeAll(xor);
		List<String> mismatches = DatasetFactory.differences(expectedResults, metafeatures);
		if (mismatches.size() != 0) {
			fail("Mismatches (" + mismatches.size() + "): " + mismatches.toString());
		}
		
		assertEquals(0, mismatches.size());
	}
}
