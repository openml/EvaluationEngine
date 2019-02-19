package opg.openml.webapplication.evaluator;

import java.io.File;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openml.apiconnector.algorithms.Conversion;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.settings.Constants;
import org.openml.apiconnector.settings.Settings;
import org.openml.apiconnector.xml.DataFeature;
import org.openml.apiconnector.xml.DataQuality;
import org.openml.apiconnector.xml.DataSetDescription;
import org.openml.apiconnector.xstream.XstreamXmlMapping;
import org.openml.webapplication.ProcessDataset;
import org.openml.webapplication.testutils.DatasetFactory;

import com.thoughtworks.xstream.XStream;

import weka.core.Instances;

public class TestDatasetEvaluator {
	

	private static final String testUrl = "https://test.openml.org/";
	private static final String testKey = "d488d8afd93b32331cf6ea9d7003d4c3"; // admin key
	private static final OpenmlConnector clientAdmin = new OpenmlConnector(testUrl, testKey);
	private static final XStream xstream = XstreamXmlMapping.getInstance();
	
	@Test
	public final void testActivateDataset() throws Exception {
		Settings.CACHE_ALLOWED = false;
		// first upload a dataset
		DataSetDescription dsd = new DataSetDescription("testXor", "testXor", "arff", "class");
		Instances dataset = DatasetFactory.getXORNominal();
		File description = Conversion.stringToTempFile(xstream.toXML(dsd), "description", "xml");
		File datasetFile = Conversion.stringToTempFile(dataset.toString(), "xor", "arff");
		int did = clientAdmin.dataUpload(description, datasetFile).getId();
		
		new ProcessDataset(clientAdmin, did, null);
		// this automatically processes the dataset
		
		DataFeature f = clientAdmin.dataFeatures(did);
		assertTrue(f.getFeatures().length == dataset.numAttributes());
		
		DataQuality q = clientAdmin.dataQualities(did);
		assertTrue(q.getQualities().length > 0);
		
		DataSetDescription descr = clientAdmin.dataGet(did);
		assertEquals(descr.getStatus(), Constants.DATA_STATUS_ACTIVE);
	}

}
