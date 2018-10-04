/*
 *  Webapplication - Java library that runs on OpenML servers
 *  Copyright (C) 2014 
 *  @author Jan N. van Rijn (j.n.van.rijn@liacs.leidenuniv.nl)
 *  @author Quan Sun (quan.sun.nz@gmail.com)
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
package org.openml.webapplication.fantail.dc.statistical;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.stat.StatUtils;
import org.openml.webapplication.fantail.dc.Characterizer;
import org.openml.webapplication.fantail.dc.DCUntils;

import weka.core.Instances;

public class AttributeEntropy extends Characterizer {

	protected static final String[] ids = new String[] { 
		"ClassEntropy",
		"MeanAttributeEntropy", 
		"MeanMutualInformation",
		"EquivalentNumberOfAtts", 
		"MeanNoiseToSignalRatio",
		
		"MinAttributeEntropy",
		"MinMutualInformation",

		"MaxAttributeEntropy",
		"MaxMutualInformation",
		
		"Quartile1AttributeEntropy",
		"Quartile1MutualInformation",
		
		"Quartile2AttributeEntropy",
		"Quartile2MutualInformation",
		
		"Quartile3AttributeEntropy",
		"Quartile3MutualInformation",
	};

	@Override
	public String[] getIDs() {
		return ids;
	}

	@Override
	protected Map<String, Double> characterize(Instances data) {
		int nominal_count = 0;
		for (int i = 0; i < data.numAttributes(); ++i) {
			if (data.attribute(i).isNominal() && data.classIndex() != i) {
				nominal_count += 1;
			}
		}
		
		Map<String, Double> qualities = new HashMap<String, Double>();
		if (data.classIndex() > 0 && data.classAttribute().isNominal()) {
			double classEntropy = DCUntils.computeClassEntropy(data);
			double[] attEntropy = DCUntils.computeAttributeEntropy(data);
			double[] mutualInformation = DCUntils.computeMutualInformation(data);
			double meanMI = StatUtils.mean(mutualInformation);
	
			qualities.put("ClassEntropy", classEntropy);
			
			if (nominal_count > 0) {
				qualities.put("MeanAttributeEntropy", StatUtils.mean(attEntropy));
				
				if (meanMI > 0) {
					qualities.put("EquivalentNumberOfAtts", classEntropy / meanMI);
					qualities.put("MeanNoiseToSignalRatio", (StatUtils.mean(attEntropy) - meanMI) / meanMI);
				} else {
					qualities.put("EquivalentNumberOfAtts", null);
					qualities.put("MeanNoiseToSignalRatio", null);
				}
				qualities.put("MeanMutualInformation", meanMI);
				qualities.put("MinMutualInformation", StatUtils.min(mutualInformation));
				qualities.put("MaxMutualInformation", StatUtils.max(mutualInformation));
				qualities.put("Quartile1MutualInformation", StatUtils.percentile(mutualInformation,25));
				qualities.put("Quartile2MutualInformation", StatUtils.percentile(mutualInformation,50));
				qualities.put("Quartile3MutualInformation", StatUtils.percentile(mutualInformation,75));
				
				qualities.put("MinAttributeEntropy", StatUtils.min(attEntropy));
				qualities.put("MaxAttributeEntropy", StatUtils.max(attEntropy));
				qualities.put("Quartile1AttributeEntropy", StatUtils.percentile(attEntropy,25));
				qualities.put("Quartile2AttributeEntropy", StatUtils.percentile(attEntropy,50));
				qualities.put("Quartile3AttributeEntropy", StatUtils.percentile(attEntropy,75));
			} else {
				qualities.put("MeanAttributeEntropy", null);
				
				qualities.put("EquivalentNumberOfAtts", null);
				qualities.put("MeanNoiseToSignalRatio", null);
				
				qualities.put("MeanMutualInformation", null);
				qualities.put("MinMutualInformation", null);
				qualities.put("MaxMutualInformation", null);
				qualities.put("Quartile1MutualInformation", null);
				qualities.put("Quartile2MutualInformation", null);
				qualities.put("Quartile3MutualInformation", null);
				
				qualities.put("MinAttributeEntropy", null);
				qualities.put("MaxAttributeEntropy", null);
				qualities.put("Quartile1AttributeEntropy", null);
				qualities.put("Quartile2AttributeEntropy", null);
				qualities.put("Quartile3AttributeEntropy", null);
			}
			
		} else { // numeric target
			for (int i = 0; i < ids.length; ++i) {
				qualities.put(ids[i], null);
			}
		}
		return qualities;
	}
}
