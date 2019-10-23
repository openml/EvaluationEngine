package org.openml.webapplication;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.openml.apiconnector.xml.DataSetDescription;
import org.openml.weka.io.OpenmlWekaConnector;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToNominal;

public class MergeDataset {

	private Map<String, Instances> datasets = null;
	
	public MergeDataset() {
		this.datasets = new TreeMap<String, Instances>();
	}
	
	public void downloadDatasets(OpenmlWekaConnector openml, List<Integer> datasetIds) throws Exception {
		for (Integer datasetId : datasetIds) {
			DataSetDescription dsd = openml.dataGet(datasetId);
			Instances dataset = openml.getArffFromUrl(dsd.getFile_id());
			List<String> taskIdValues = new ArrayList<String>();
			taskIdValues.add(datasetId + "");
			Attribute taskId = new Attribute("openml-dataset-id", taskIdValues);
			dataset.insertAttributeAt(taskId, 0);
			for (Instance i : dataset) {
				i.setValue(0, datasetId + "");
			}
			this.datasets.put("" + datasetId, dataset);
		}
	}
	
	public void setDatasets(Map<String, Instances> datasets) {
		this.datasets = new TreeMap<String, Instances>();
		for (String datasetId : datasets.keySet()) {
			Instances dataset = datasets.get(datasetId);
			List<String> taskIdValues = new ArrayList<String>();
			taskIdValues.add(datasetId + "");
			Attribute taskId = new Attribute("openml-dataset-id", taskIdValues);
			dataset.insertAttributeAt(taskId, 0);
			for (Instance i : dataset) {
				i.setValue(0, datasetId);
			}
			this.datasets.put(datasetId, dataset);
		}
	}
	
	public boolean verifyAttributeSet() {
		final Set<String> attributes = getAttributeNames(this.datasets.values().iterator().next());
		for (Instances dataset : this.datasets.values()) {
			Set<String> attributesPrime = getAttributeNames(dataset);
			if (!attributes.equals(attributesPrime)) {
				return false;
			}
		}
		return true;
	}
	
	public Instances merge() throws Exception {
		if (!verifyAttributeSet()) {
			throw new Exception("Attribute Set does not agree!");
		}
		Instances first = this.datasets.values().iterator().next();
		String name = "merged" + datasets.keySet();
		// go through string representation
		StringBuilder sb = new StringBuilder();
		sb.append("@relation '" + name + "'\n\n");
		ArrayList<Attribute> atts = getAttributesAsString(first);
		for (Attribute a : atts) {
			sb.append(a.toString() + "\n");
		}
		sb.append("\n@data\n");
		for (String datasetId : datasets.keySet()) {
			Instances dataset = datasets.get(datasetId);
			for (int iInst = 0; iInst < dataset.numInstances(); ++iInst) {
				sb.append((DenseInstance) dataset.get(iInst) + "\n");
			}
		}
		
		Instances masterSet = new Instances(new StringReader(sb.toString()));
		
		Filter stringToNominal = new StringToNominal();
		stringToNominal.setInputFormat(masterSet);
		String value = getNominalIndices(first, 1).toString();
		String[] options = {"-R", value.substring(1, value.length() - 1).replace(" ", "")};
		stringToNominal.setOptions(options);
		masterSet = Filter.useFilter(masterSet, stringToNominal);
		masterSet.setRelationName(name);
		
		return masterSet;
	}
	
	private List<Integer> getNominalIndices(Instances dataset, int delta) {
		List<Integer> result = new ArrayList<>();
		for (int i = 0; i < dataset.numAttributes(); ++i) {
			if (dataset.attribute(i).isNominal()) {
				result.add(i+delta);
			}
		}
		return result;
	}
	
	private ArrayList<Attribute> getAttributesAsString(Instances dataset) {
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		Enumeration<Attribute> enumeration = dataset.enumerateAttributes();
		while (enumeration.hasMoreElements()) {
			Attribute att = enumeration.nextElement();
			if (att.isNominal()) {
				attributes.add(new Attribute(att.name(), true));
			} else {
				attributes.add(att);
			}
		}
		return attributes;
	}
	
	private Set<String> getAttributeNames(Instances dataset) {
		Set<String> attributeNames = new TreeSet<String>();
		Enumeration<Attribute> enumeration = dataset.enumerateAttributes();
		while (enumeration.hasMoreElements()) {
			attributeNames.add(enumeration.nextElement().name());
		}
		return attributeNames;
	}
}
