package opg.openml.webapplication.evaluator;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;
import org.openml.apiconnector.algorithms.Conversion;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.settings.Constants;
import org.openml.apiconnector.xml.DataFeature;
import org.openml.apiconnector.xml.DataQuality;
import org.openml.apiconnector.xml.DataSetDescription;
import org.openml.webapplication.ProcessDataset;
import org.openml.webapplication.settings.Settings;
import org.openml.webapplication.testutils.DatasetFactory;

import weka.core.Instances;

public class TestDatasetEvaluator {
	

	private static final String testUrl = "https://test.openml.org/";
	private static final String testKey = "d488d8afd93b32331cf6ea9d7003d4c3"; // admin key
	private static final OpenmlConnector clientAdmin = new OpenmlConnector(testUrl, testKey);
	
	@Test
	public final void testActivateDataset() throws Exception {
		org.openml.apiconnector.settings.Settings.CACHE_ALLOWED = false;
		// first upload a dataset
		DataSetDescription dsd = new DataSetDescription("testXor", "testXor", "arff", "class");
		Instances dataset = DatasetFactory.getXORNominal();
		File datasetFile = Conversion.stringToTempFile(dataset.toString(), "xor", "arff");
		int did = clientAdmin.dataUpload(dsd, datasetFile);
		
		new ProcessDataset(clientAdmin, did, null);
		// this automatically processes the dataset
		
		DataFeature f = clientAdmin.dataFeatures(did);
		assertTrue(f.getFeatures().length == dataset.numAttributes());
		
		DataQuality q = clientAdmin.dataQualities(did, Settings.EVALUATION_ENGINE_ID);
		assertTrue(q.getQualities().length > 0);
		
		DataSetDescription descr = clientAdmin.dataGet(did);
		assertEquals(descr.getStatus(), Constants.DATA_STATUS_ACTIVE);
	}

}
