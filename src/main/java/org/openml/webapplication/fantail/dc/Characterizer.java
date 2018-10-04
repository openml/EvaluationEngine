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
package org.openml.webapplication.fantail.dc;

import java.util.Map;

import weka.core.Instances;

public abstract class Characterizer {

	public abstract String[] getIDs();

	protected abstract Map<String, Double> characterize(Instances instances);
	
	public Map<String, Double> characterizeAll(Instances instances) throws Exception {
		Map<String, Double> qualities = characterize(instances);
		
		// enforce finite double or null for all qualities
		for (String key : qualities.keySet()) {
			if (qualities.get(key) != null && !Double.isFinite(qualities.get(key))) {
				throw new Exception("Quality illegal value: " + key + ", value: " + qualities.get(key));
			}
		}

		for (String key : getIDs()) {
			if (!qualities.containsKey(key)) {
				throw new Exception("Quality missing: " + key);
			}
		}
		
		return qualities;
	}

	public int getNumMetaFeatures() {
		return getIDs().length;
	}
}
