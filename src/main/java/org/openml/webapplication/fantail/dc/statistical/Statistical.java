package org.openml.webapplication.fantail.dc.statistical;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.openml.webapplication.fantail.dc.Characterizer;

import weka.core.Instances;

public class Statistical extends Characterizer {

	protected final String[] ids = new String[] { "MeanMeansOfNumericAtts", "MeanStdDevOfNumericAtts",
			"MeanKurtosisOfNumericAtts", "MeanSkewnessOfNumericAtts", "MinMeansOfNumericAtts", "MinStdDevOfNumericAtts",
			"MinKurtosisOfNumericAtts", "MinSkewnessOfNumericAtts", "MaxMeansOfNumericAtts", "MaxStdDevOfNumericAtts",
			"MaxKurtosisOfNumericAtts", "MaxSkewnessOfNumericAtts", "Quartile1MeansOfNumericAtts",
			"Quartile1StdDevOfNumericAtts", "Quartile1KurtosisOfNumericAtts", "Quartile1SkewnessOfNumericAtts",
			"Quartile2MeansOfNumericAtts", "Quartile2StdDevOfNumericAtts", "Quartile2KurtosisOfNumericAtts",
			"Quartile2SkewnessOfNumericAtts", "Quartile3MeansOfNumericAtts", "Quartile3StdDevOfNumericAtts",
			"Quartile3KurtosisOfNumericAtts", "Quartile3SkewnessOfNumericAtts" };

	@Override
	public String[] getIDs() {
		return ids;
	}

	@Override
	protected Map<String, Double> characterize(Instances dataset) {

		Map<String, Double> qualities = new HashMap<String, Double>();
		int numericValues = 0;

		DescriptiveStatistics meanStats = new DescriptiveStatistics();
		DescriptiveStatistics stdDevStats = new DescriptiveStatistics();
		DescriptiveStatistics kurtosisStats = new DescriptiveStatistics();
		DescriptiveStatistics skewnessStats = new DescriptiveStatistics();

		for (int attribute = 0; attribute < dataset.numAttributes(); attribute++) {
			if (dataset.attribute(attribute).isNumeric()) {
				numericValues += 1;
				
				DescriptiveStatistics AttributeStats = new DescriptiveStatistics();
				for (int instance = 0; instance < dataset.numInstances(); instance++) {
					if (!dataset.get(instance).isMissing(attribute)) {
						double value = dataset.get(instance).value(attribute);
						AttributeStats.addValue(value);
					}
				}
				if (Double.isFinite(AttributeStats.getMean())) {
					meanStats.addValue(AttributeStats.getMean());
				}
				if (Double.isFinite(AttributeStats.getStandardDeviation())) {
					stdDevStats.addValue(AttributeStats.getStandardDeviation());
				}
				if (Double.isFinite(AttributeStats.getKurtosis())) {
					// TODO: this is probably a bug!
					kurtosisStats.addValue(AttributeStats.getKurtosis());
				}
				if (Double.isFinite(AttributeStats.getSkewness())) {
					skewnessStats.addValue(AttributeStats.getSkewness());
				}
			}
		}
		
		final String SUFFIX = "OfNumericAtts";
		Map<String, DescriptiveStatistics> categoryStats = new TreeMap<String, DescriptiveStatistics>();
		categoryStats.put("Means", meanStats);
		categoryStats.put("StdDev", stdDevStats);
		categoryStats.put("Kurtosis", kurtosisStats);
		categoryStats.put("Skewness", skewnessStats);
		
		for (String category : categoryStats.keySet()) {
			if (numericValues > 0) {
				DescriptiveStatistics stats = categoryStats.get(category);
				qualities.put("Mean" + category + SUFFIX, stats.getMean());
				qualities.put("Min" + category + SUFFIX, stats.getMin());
				qualities.put("Max" + category + SUFFIX, stats.getMax());
				qualities.put("Quartile1" + category + SUFFIX, stats.getPercentile(25));
				qualities.put("Quartile2" + category + SUFFIX, stats.getPercentile(50));
				qualities.put("Quartile3" + category + SUFFIX, stats.getPercentile(75));
			} else {
				qualities.put("Mean" + category + SUFFIX, null);
				qualities.put("Min" + category + SUFFIX, null);
				qualities.put("Max" + category + SUFFIX, null);
				qualities.put("Quartile1" + category + SUFFIX, null);
				qualities.put("Quartile2" + category + SUFFIX, null);
				qualities.put("Quartile3" + category + SUFFIX, null);
			}
		}
		
		return qualities;
	}

}
