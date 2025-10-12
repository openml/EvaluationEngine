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
package org.openml.webapplication.generatefolds;

import java.util.ArrayList;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class ArffMapping {
	
	private final ArrayList<Attribute> attributes;
	
	private final Instances dataset;
	private final boolean useSamples;
	private final boolean useTaskId;

	public ArffMapping(boolean useSamples, Instances dataset, Integer taskIdIdx) {
		attributes = new ArrayList<Attribute>();
		this.useSamples = useSamples;
		this.useTaskId = taskIdIdx != null;
		this.dataset = dataset;

		ArrayList<String> att_type_values = new ArrayList<String>();
		att_type_values.add("TRAIN");
		att_type_values.add("TEST");

		Attribute type = new Attribute("type", att_type_values);
		Attribute rowid = new Attribute("rowid");
		Attribute fold = new Attribute("fold");
		Attribute repeat = new Attribute("repeat");

		attributes.add(type);
		attributes.add(rowid);
		attributes.add(repeat);
		attributes.add(fold);

		if (useSamples) {
			Attribute sample = new Attribute("sample");
			attributes.add(sample);
		}
		
		if (taskIdIdx != null) {
			attributes.add(dataset.attribute(taskIdIdx));
		}
	}

	public ArrayList<Attribute> getArffHeader() {
		return attributes;
	}

	public Instance createInstance(boolean train, int rowid, int repeat, int fold) throws Exception {
		if (useSamples) {
			throw new Exception("can not use this create instance fn");
		}
		int size = this.useTaskId ? 5 : 4;
		Instance instance = new DenseInstance(size);
		instance.setValue(attributes.get(0), train ? 0.0 : 1.0);
		instance.setValue(attributes.get(1), rowid);
		instance.setValue(attributes.get(2), repeat);
		instance.setValue(attributes.get(3), fold);
		if (this.useTaskId) {
			// note that class value is task id
			instance.setValue(attributes.get(4), this.dataset.get(rowid).classValue());
		}

		return instance;
	}

	public Instance createInstanceWithSample(boolean train, int rowid, int repeat, int fold, int sample) throws Exception  {
		if (!useSamples || useTaskId) {
			throw new Exception("can not use this create instance fn");
		}
		Instance instance = new DenseInstance(5);
		instance.setValue(attributes.get(0), train ? 0.0 : 1.0);
		instance.setValue(attributes.get(1), rowid);
		instance.setValue(attributes.get(2), repeat);
		instance.setValue(attributes.get(3), fold);
		instance.setValue(attributes.get(4), sample);

		return instance;
	}
}
