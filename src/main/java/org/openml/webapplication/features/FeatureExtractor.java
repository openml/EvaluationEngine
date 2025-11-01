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

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.openml.apiconnector.xml.DataFeature.Feature;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;


/**
 * Describe the features of an ARFF file: for each attribute (i.e. column) the number of unique values, missing values,
 * etc.
 */
public class FeatureExtractor {


	/**
	 * Describe the features of an ARFF file. The file will not be loaded completely into memory, but will be read row
	 * by row.
	 *
	 * @param arffReader a Weka Arff reader. The features will be generated in a single loop.
	 * @param targetVariables a comma separated string with the name(s) of the target variable(s)
	 * @return a list with a feature for each attribute (column) in this ARFF file.
	 */
	public static List<Feature> getFeatures(ArffLoader.ArffReader arffReader, String targetVariables) throws IOException {
		final Instances structure = arffReader.getStructure();
		final Set<String> targetAttributeNames = getTargetClasses(structure, targetVariables);
		final boolean singleTarget = targetAttributeNames.size() == 1;
		if (singleTarget) {
			structure.setClass(structure.attribute(targetVariables));
		}
		final boolean singleNominalTarget = singleTarget && structure.classAttribute().isNominal();
		final Integer numClasses = singleTarget ? structure.numClasses() : null;

		final List<AttributeSummarizer> statistics = IntStream.range(0, structure.numAttributes())
				.mapToObj(structure::attribute)
				.map(attribute -> {
					boolean isTarget = targetAttributeNames.contains(attribute.name());
					return new AttributeSummarizer(attribute, isTarget, singleNominalTarget, numClasses);
				})
				.collect(Collectors.toList());

		Instance instance;
		while((instance = arffReader.readInstance(structure)) != null) {
			double classValue = instance.classValue();
			for (int attrIdx = 0; attrIdx < statistics.size(); attrIdx++) {
				AttributeSummarizer summarizer = statistics.get(attrIdx);
				summarizer.addValue(instance.value(attrIdx), classValue);
			}
		}
		return statistics.stream()
				.map(AttributeSummarizer::summarize)
				.collect(Collectors.toList());
	}

	private static Set<String> getTargetClasses(Instances structure, String defaultClass)  {
		if (defaultClass == null) {
			return Collections.emptySet();
		}
		String[] targetAttributes = defaultClass.split(",");
		List<String> targetAttributesNotFound = Arrays.stream(targetAttributes)
				.filter(dataClass -> structure.attribute(dataClass) == null)
				.collect(Collectors.toList());
		if (targetAttributesNotFound.size() > 0) {
			throw new IllegalArgumentException("Default target attribute(s) could not be found: " + targetAttributesNotFound);
		}
		return new HashSet<>(Arrays.asList(targetAttributes));
	}
}
