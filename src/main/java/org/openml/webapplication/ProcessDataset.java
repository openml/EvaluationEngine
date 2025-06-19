package org.openml.webapplication;

import org.openml.apiconnector.algorithms.Conversion;
import org.openml.apiconnector.io.ApiException;
import org.openml.apiconnector.settings.Constants;
import org.openml.apiconnector.xml.DataFeature;
import org.openml.apiconnector.xml.DataFeature.Feature;
import org.openml.apiconnector.xml.DataSetDescription;
import org.openml.apiconnector.xml.DataUnprocessed;
import org.openml.webapplication.features.CharacterizerFactory;
import org.openml.webapplication.features.FeatureExtractor;
import org.openml.webapplication.features.FantailConnector;
import org.openml.webapplication.settings.Settings;
import org.openml.weka.io.OpenmlWekaConnector;
import weka.core.converters.ArffLoader;

import java.io.Reader;
import java.util.List;
import java.util.Optional;

public class ProcessDataset {

	private final OpenmlWekaConnector apiconnector;
	
	public ProcessDataset(OpenmlWekaConnector ac, String mode) throws Exception {
		this(ac, null, mode);
	}
	
	public ProcessDataset(OpenmlWekaConnector connector, Integer dataset_id, String mode) throws Exception {
		apiconnector = connector;
		if(dataset_id != null) {
			Conversion.log( "OK", "Process Dataset", "Processing dataset " + dataset_id + " on special request. ");
			process(dataset_id);
		} else {
			Optional<DataUnprocessed> du = fetchUnprocessed(mode);
			while(du.isPresent()) {
				dataset_id = du.get().getDatasets()[0].getDid();
				Conversion.log("OK", "Process Dataset", "Processing dataset " + dataset_id + " as obtained from database. ");
				process( dataset_id );
				du = fetchUnprocessed(mode);
			}
			Conversion.log("OK", "Process Dataset", "No more datasets to process. ");
		}
	}

	private Optional<DataUnprocessed> fetchUnprocessed(String mode) throws Exception {
		try {
			return Optional.of(apiconnector.dataUnprocessed(Settings.EVALUATION_ENGINE_ID, mode));
		} catch (ApiException e){
			// No unprocessed datasets is perfectly normal behaviour, so ignoring these exceptions.
			if(!e.getMessage().contains("No unprocessed")){
				throw e;
			}
		}
		return Optional.empty();
	}


	public void process(Integer did) throws Exception {

		DataSetDescription dsd = apiconnector.dataGet(did);
		String defaultTarget = dsd.getDefault_target_attribute();
		
		try {
			FantailConnector fantail = new FantailConnector(apiconnector, CharacterizerFactory.simple());
			Reader reader = apiconnector.getDataset(dsd);
			ArffLoader.ArffReader dataset = new ArffLoader.ArffReader(reader, 1000, false);
			Conversion.log("OK", "Process Dataset", "Processing dataset " + did + " - obtaining features. ");
			List<Feature> features = FeatureExtractor.getFeatures(dataset, defaultTarget);
			DataFeature datafeature = new DataFeature(did, Settings.EVALUATION_ENGINE_ID, features.toArray(new Feature[0]));
			int receivedId = apiconnector.dataFeaturesUpload(datafeature);
			
			if (dsd.getStatus().equals(Constants.DATA_STATUS_PREP)) {
				apiconnector.dataStatusUpdate(did, Constants.DATA_STATUS_ACTIVE);
			}
			
			Conversion.log( "OK", "Process Dataset", "Processing dataset " + receivedId + " - obtaining basic qualities. " );
			fantail.computeMetafeatures(did);
			Conversion.log("OK", "Process Dataset", "Dataset " + did + " - Processed successfully. ");
		} catch(ApiException e) {
			if (e.getCode() == 431) {
				// dataset already processed
				Conversion.log("Notice", "Process Dataset", e.getMessage());
			} else {
				e.printStackTrace();
				processDatasetWithError(did, e.getMessage());
			}
		} catch(Exception | OutOfMemoryError e) {
			e.printStackTrace();
			processDatasetWithError(did, e.getMessage());
		}
	}
	
	private void processDatasetWithError(int did, String errorMessage) throws Exception {
		Conversion.log("Error", "Process Dataset", "Error while processing dataset. Marking this in database.");
		DataFeature datafeature = new DataFeature(did, Settings.EVALUATION_ENGINE_ID, errorMessage);
		int receivedId = apiconnector.dataFeaturesUpload(datafeature);
		Conversion.log("Error", "Process Dataset", "Dataset " + receivedId + " - Error: " + errorMessage);
	}
}
