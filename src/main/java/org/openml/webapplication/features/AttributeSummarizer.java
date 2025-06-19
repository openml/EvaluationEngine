/*
 *  Webapplication - Java library that runs on OpenML servers
 *  Copyright (C) 2014 
 *  @author Jan N. van Rijn (j.n.van.rijn@liacs.leidenuniv.nl)
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 */
package org.openml.webapplication.features;

import org.openml.apiconnector.xml.DataFeature;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.Utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Summarizes the statistics of an attribute into DataFeature.Feature.
 * This functionality is similar to weka's Instances.attributeStats and weka's AttributeStats, with three main
 * differences:
 * - This code does not rely on weka's Instances, and thus does not need to keep the file in memory.
 * - The code is slightly rearranged, so that there is a single class responsible for storing the attribute's while
 *      running through a file, instead of spreading it over Instances and AttributeStats.
 * - It keeps track of more statistics, such as the min, max, mean and standard deviation.
 */
public class AttributeSummarizer {
	private static final int PRECISION = 16;
	private static final int MAX_SIZE_CLASS_DISTR = 16384;
	private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
	private final boolean isTarget;
	private int missingCount;
	private final HashMap<Double, AtomicInteger> valueCounter;
	private final boolean trackClassDistribution;
	private final int[][] classDistribution;
	private final Attribute attribute;

	/**
	 * For each attribute (column) of a .arff file, an AttributeSummarizer should be created. You can then add values to
	 * the summarizer, and get a Feature by running summarize() on the summarizer.
	 *
	 * @param attribute The Weka Attribute of which data should be summarized
	 * @param isTarget Is this attribute a target variable
	 * @param singleNominalTarget Is there only one target variable, which is nominal?
	 * @param numTargetClasses Null if there is not a single target, otherwise the attribute.numClasses() of the target
	 *                         attribute
	 */
	public AttributeSummarizer(Attribute attribute, boolean isTarget, boolean singleNominalTarget, Integer numTargetClasses) {
		this.attribute = attribute;
		this.isTarget = isTarget;
		this.missingCount = 0;
		this.trackClassDistribution = singleNominalTarget && attribute.isNominal();
		if (trackClassDistribution) {
			classDistribution = new int[attribute.numValues()][numTargetClasses];
		} else {
			classDistribution = new int[0][0];
		}
		this.valueCounter = new HashMap<>(attribute.numValues());
	}

	/**
	 * Call this function for each row in the dataset.
	 *
	 * @param value The attribute value corresponding to this row
	 * @param classValue The class corresponding to this row, or NaN
	 */
	public void addValue(double value, double classValue) {
		if (Utils.isMissingValue(value)) { 
			missingCount += 1;
			return;
		}
		if (trackClassDistribution) {
			classDistribution[(int) value][(int) classValue] += 1;
		}
		valueCounter.computeIfAbsent(value, v -> new AtomicInteger())
				.getAndIncrement();
	}

	/**
	 * Run this after the values have been added for all rows.
	 *
	 * @return A DataFeature.Feature containing all statistics of this attribute.
	 */
	public DataFeature.Feature summarize(){
		final int nObservations = valueCounter.values().stream().map(AtomicInteger::get).reduce(0, Integer::sum);
		final int nDistinct = valueCounter.size();
		final int nUnique = Math.toIntExact(valueCounter.values().stream()
				.map(AtomicInteger::get)
				.filter(valueCount -> valueCount == 1)
				.count());
		int nInteger = 0, nReal = 0;
		Integer nNominal = null;
		Double mean = null, stdev = null, min = null, max = null;
		BigDecimal totalSum = null, totalSumSquared = null;
		String[] nominalValues;

		final boolean doCalculateStatistics = attribute.isNumeric();
		if(doCalculateStatistics) {
			min = Double.MAX_VALUE;
			max = Double.MIN_VALUE;
			totalSum = new BigDecimal(0);
			totalSumSquared = new BigDecimal(0);
		}
		for (Map.Entry<Double, AtomicInteger> entries : valueCounter.entrySet()) {
			double value = entries.getKey();
			int count = entries.getValue().get();
			if (value == (double)((int)value)) {
				nInteger += count;
			} else {
				nReal += count;
			}
			if(doCalculateStatistics) {
				if (value < min) min = value;
				if (value > max) max = value;
				totalSum = totalSum.add(BigDecimal.valueOf(value).multiply(BigDecimal.valueOf(count)));
				totalSumSquared = totalSumSquared.add(new BigDecimal(value * value).multiply(BigDecimal.valueOf(count)));
			}
		}
		if(doCalculateStatistics){
			mean = totalSum.divide(new BigDecimal(nObservations), PRECISION, ROUNDING_MODE ).doubleValue();
			BigDecimal obs = new BigDecimal(nObservations);
			stdev = Math.sqrt(
					totalSumSquared.multiply(obs)
							.subtract(totalSum.multiply(totalSum))
							.divide(
									obs.multiply(
											obs.subtract(new BigDecimal(1))),
									PRECISION,
									ROUNDING_MODE
							).doubleValue());
		}
		if (attribute.isNominal()){
			nNominal = attribute.numValues();
			nominalValues = IntStream.range(0, attribute.numValues())
					.mapToObj(attribute::value)
					.toArray(String[]::new);
		} else {
			nominalValues = new String[]{};
		}

		String dataType = Attribute.typeToString(attribute);
		String classDistr = getClassDistribution();
		if (classDistr.length() > MAX_SIZE_CLASS_DISTR) {
			classDistr = null;
		}
		return new DataFeature.Feature(attribute.index(), attribute.name(),
				dataType, nominalValues,
				isTarget,
				nDistinct,
				nUnique, missingCount,
				nInteger, nReal,
				nNominal, nObservations,
				max, min, mean, stdev, classDistr);
	}

	public String getClassDistribution() {
		if (!trackClassDistribution) {
			return "[]";
		}
		StringBuilder sb = new StringBuilder();
		StringBuilder headline = new StringBuilder();
		for (int i = 0; i < attribute.numValues(); ++i) {
			headline.append(",\"").append(attribute.value(i)).append("\"");
		}
		sb.append("[[").append(headline.substring(1)).append("],[");
		for (int i = 0; i < classDistribution.length; ++i) {
			if (i > 0) sb.append(",");
			sb.append(Arrays.toString(classDistribution[i]));
		}
		sb.append("]]");
		return sb.toString();
	}
}
