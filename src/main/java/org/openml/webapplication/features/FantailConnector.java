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

import org.openml.apiconnector.algorithms.Conversion;
import org.openml.apiconnector.io.ApiException;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.settings.Constants;
import org.openml.apiconnector.xml.DataFeature;
import org.openml.apiconnector.xml.DataFeature.Feature;
import org.openml.apiconnector.xml.DataQuality;
import org.openml.apiconnector.xml.DataQuality.Quality;
import org.openml.apiconnector.xml.DataSetDescription;
import org.openml.apiconnector.xml.DataUnprocessed;
import org.openml.webapplication.fantail.dc.Characterizer;
import org.openml.webapplication.settings.Settings;

import weka.core.Instances;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToNominal;

import java.io.FileReader;
import java.util.*;

public class FantailConnector {
	private final Integer window_size = null; // TODO: make it work again
	private OpenmlConnector apiconnector;
	private final List<Characterizer> CHARACTERIZERS;

	public FantailConnector(OpenmlConnector ac, List<Characterizer> characterizers) throws Exception {
		apiconnector = ac;
		CHARACTERIZERS = characterizers;
	}
	
	public void start(Integer dataset_id, String mode, String priorityTag, Integer interval_size) throws Exception {
		if (dataset_id != null) {
			Conversion.log("OK", "Process Dataset", "Processing dataset " + dataset_id + " on special request. ");
			computeMetafeatures(dataset_id);
		} else {
			List<String> expectedQualities = CharacterizerFactory.getExpectedQualities(CHARACTERIZERS);
			Optional<DataUnprocessed> du = fetchUnprocessedQualities(mode, priorityTag, expectedQualities);

			while (du.isPresent()) {
				Conversion.log("OK", "Process Dataset", "Processing dataset " + dataset_id + " as obtained from database. ");
				computeMetafeatures(du.get().getDatasets()[0].getDid());
				du = fetchUnprocessedQualities(mode, priorityTag, expectedQualities);
			}
			Conversion.log("OK", "Process Dataset", "No more datasets to process. ");
		}
	}

	private Optional<DataUnprocessed> fetchUnprocessedQualities(String mode, String priorityTag, List<String> expectedQualities) throws Exception {
		try {
			return Optional.of(apiconnector.dataqualitiesUnprocessed(Settings.EVALUATION_ENGINE_ID, mode, false, expectedQualities, priorityTag));
		} catch (ApiException e){
			// No unprocessed qualities is perfectly normal behaviour, so ignoring these exceptions.
			if(!e.getMessage().contains("No unprocessed")){
				throw e;
			}
		}
		return Optional.empty();
	}

	public void computeMetafeatures(int datasetId) throws Exception {
		Conversion.log("OK", "Download", "Start downloading dataset: " + datasetId);
		DataSetDescription dsd = apiconnector.dataGet(datasetId);
		DataFeature dataFeatures = apiconnector.dataFeatures(datasetId);
		List<String> targetFeatures = new ArrayList<String>();
		List<String> ignoreFeatures = new ArrayList<String>();
		for (Feature f : dataFeatures.getFeatures()) {
			if (f.getIs_ignore()) {
				ignoreFeatures.add(f.getName());
			}
			if (f.getIs_target()) {
				targetFeatures.add(f.getName());
			}
		}
		
		Instances dataset = new Instances(new FileReader(apiconnector.datasetGet(dsd)));
		List<String> qualitiesAvailable = new ArrayList<String>();
		try {
			qualitiesAvailable = Arrays.asList(apiconnector.dataQualities(datasetId, Settings.EVALUATION_ENGINE_ID).getQualityNames());
		} catch(ApiException ae) {
			if (ae.getCode() != 362) {
				throw ae;
			} else {
				// this is fine. (just no qualities available)
			}
		}
		Set<Quality> qualities = extractFeatures(dsd.getId(), dataset, targetFeatures, ignoreFeatures, dsd.getRow_id_attribute(), qualitiesAvailable);
		
		// now upload the qualitues
		Conversion.log("OK", "Extract Features", "Done generating features, start wrapping up");
		if (qualities.size() > 0) {
			DataQuality dq = new DataQuality(datasetId, Settings.EVALUATION_ENGINE_ID, qualities.toArray(new Quality[qualities.size()]));
			int uploadedId = apiconnector.dataQualitiesUpload(dq);
			Conversion.log("OK", "Extract Features", "DONE: " + uploadedId);
		} else {
			Conversion.log("OK", "Extract Features", "DONE: Nothing to upload");
		}
	}

	private Set<Quality> extractFeatures(int did, Instances dataset, List<String> targetAttributes, List<String> ignoreAttributes, String rowIdAttribute, List<String> qualitiesAvailable) throws Exception {
		Conversion.log("OK", "Extract Features", "Start extracting features for dataset: " + did);
		
		if (targetAttributes != null && targetAttributes.size() == 1) {
			String targetAttribute = targetAttributes.get(0);
			if (dataset.attribute(targetAttribute) == null) {
				throw new Exception("Could not find target attribute: " + targetAttribute);
			}
			dataset.setClass(dataset.attribute(targetAttribute));
		}
		
		// keeping the full dataset for attribute identification purposes
		Instances fullDataset = new Instances(dataset);

		if (rowIdAttribute != null) {
			if (dataset.attribute(rowIdAttribute) != null) {
				dataset.deleteAttributeAt(dataset.attribute(rowIdAttribute).index());
			}
		}
		if (ignoreAttributes != null) {
			for (String att : ignoreAttributes) {
				if (dataset.attribute(att) != null) {
					dataset.deleteAttributeAt(dataset.attribute(att).index());
				}
			}
		}

		// first run stream characterizers
		/*for (StreamCharacterizer sc : globalMetafeatures.getStreamCharacterizers()) {

			if (qualitiesAvailable.containsAll(Arrays.asList(sc.getIDs())) == false) {
				Conversion.log("OK", "Extract Features", "Running Stream Characterizers (full data)");
				// This just precomputes everything, result will be used later depending on the windows size
				sc.characterize(dataset);
			} else {
				Conversion.log("OK", "Extract Features", "Skipping Stream Characterizers (full data) - already in database");
			}
		}*/

		Set<Quality> qualities = new TreeSet<Quality>();
		if (window_size != null) {
			Conversion.log("OK", "Extract Features", "Running Batch Characterizers (partial data)");

			for (int i = 0; i < dataset.numInstances(); i += window_size) {
				if (apiconnector.getVerboselevel() >= Constants.VERBOSE_LEVEL_ARFF) {
					Conversion.log("OK", "FantailConnector",
							"Starting window [" + i + "," + (i + window_size) + "> (did = " + did + ",total size = " + dataset.numInstances() + ")");
				}
				qualities.addAll(datasetCharacteristics(dataset, CHARACTERIZERS, i, window_size, null, fullDataset));

				/*for (StreamCharacterizer sc : globalMetafeatures.getStreamCharacterizers()) {
					// preventing nullpointer exception (if stream characterizer was already run)
					if (qualitiesAvailable.containsAll(Arrays.asList(sc.getIDs())) == false) {
						qualities.addAll(hashMaptoList(sc.interval(i), i, window_size));
					}
				}*/
			}

		} else {
			Conversion.log("OK", "Extract Features", "Running Batch Characterizers (full data, might take a while)");
			qualities.addAll(datasetCharacteristics(dataset, CHARACTERIZERS, null, null, qualitiesAvailable, fullDataset));
			/*for (StreamCharacterizer sc : globalMetafeatures.getStreamCharacterizers()) {
				Map<String, Double> streamqualities = sc.global();
				if (streamqualities != null) {
					qualities.addAll(hashMaptoList(streamqualities, null, null));
				}
			}*/
		}
		return qualities;
	}

	/**
	 * Visible for testing
	 */
	public static Set<Quality> datasetCharacteristics(Instances dataset, List<Characterizer> characterizers, Integer start, Integer interval_size, List<String> qualitiesAvailable, Instances fullDataset) throws Exception {
		Set<Quality> result = new TreeSet<>();
		Instances intervalData;

		// Be careful changing this!
		if (interval_size != null) {
			intervalData = new Instances(dataset, start, Math.min(interval_size, dataset.numInstances() - start));
			intervalData = applyFilter(intervalData, new StringToNominal(), "-R first-last");
			intervalData.setClassIndex(dataset.classIndex());
		} else {
			intervalData = dataset;
			// todo: use StringToNominal filter? might be too expensive
		}

		for (Characterizer dc : characterizers) {
			if (qualitiesAvailable.containsAll(Arrays.asList(dc.getIDs())) == false) {
				Conversion.log("OK", "Extract Batch Features", dc.getClass().getName() + ": " + Arrays.toString(dc.getIDs()));
				Map<String, Double> qualities = dc.characterizeAll(intervalData);
				result.addAll(hashMaptoSet(qualities, start, interval_size));
			} else {
				Conversion.log("OK", "Extract Batch Features", dc.getClass().getName() + " - already in database");
			}
		}
		
		return result;
	}

	private static Set<Quality> hashMaptoSet(Map<String, Double> map, Integer start, Integer size) {
		Set<Quality> result = new TreeSet<>();
		for (String quality : map.keySet()) {
			Integer end = start != null ? start + size : null;
			result.add(new Quality(quality, map.get(quality), start, end, null));
		}
		return result;
	}

	private static Instances applyFilter(Instances dataset, Filter filter, String options) throws Exception {
		filter.setOptions(Utils.splitOptions(options));
		filter.setInputFormat(dataset);
		return Filter.useFilter(dataset, filter);
	}
}
