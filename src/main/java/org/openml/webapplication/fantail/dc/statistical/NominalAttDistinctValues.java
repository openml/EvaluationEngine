package org.openml.webapplication.fantail.dc.statistical;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.openml.webapplication.fantail.dc.Characterizer;

import weka.core.Instances;

public class NominalAttDistinctValues extends Characterizer {

	protected final String[] ids = new String[] { 
		"MaxNominalAttDistinctValues", "MinNominalAttDistinctValues", 
		"MeanNominalAttDistinctValues", "StdvNominalAttDistinctValues"
	};

	@Override
	public String[] getIDs() {
		return ids;
	}

	@Override
	protected Map<String, Double> characterize(Instances dataset) {
		Map<String, Double> qualities = new HashMap<String, Double>();
		int nominalAtts = 0;
		
		DescriptiveStatistics distinctValuesStats = new DescriptiveStatistics();
		for (int attribute = 0; attribute < dataset.numAttributes(); attribute++) {
			if (dataset.attribute(attribute).isNominal()) {
				nominalAtts += 1;
				
				ArrayList<Double> valuesList = new ArrayList<Double>();
				for (int instances = 0; instances < dataset.numInstances(); instances++) {
					if (!dataset.get(instances).isMissing(attribute)) {
						double value = dataset.get(instances).value(attribute);
						if (!valuesList.contains(value)) {
							valuesList.add(value);
						}
					}
				}

				distinctValuesStats.addValue(valuesList.size());
			}
		}
		
		if (nominalAtts > 0) {
			qualities.put("MaxNominalAttDistinctValues", distinctValuesStats.getMax());
			qualities.put("MinNominalAttDistinctValues", distinctValuesStats.getMin());
			qualities.put("MeanNominalAttDistinctValues", distinctValuesStats.getMean());
			qualities.put("StdvNominalAttDistinctValues", distinctValuesStats.getStandardDeviation());
		} else {
			qualities.put("MaxNominalAttDistinctValues", null);
			qualities.put("MinNominalAttDistinctValues", null);
			qualities.put("MeanNominalAttDistinctValues", null);
			qualities.put("StdvNominalAttDistinctValues", null);
		}
		return qualities;
	}
}
