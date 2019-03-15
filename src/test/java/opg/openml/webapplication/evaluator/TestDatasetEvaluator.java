package opg.openml.webapplication.evaluator;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;
import org.openml.apiconnector.algorithms.Conversion;
import org.openml.apiconnector.settings.Constants;
import org.openml.apiconnector.xml.DataFeature;
import org.openml.apiconnector.xml.DataQuality;
import org.openml.apiconnector.xml.DataSetDescription;
import org.openml.webapplication.ProcessDataset;
import org.openml.webapplication.settings.Settings;
import org.openml.webapplication.testutils.DatasetFactory;

import org.openml.webapplication.testutils.BaseTestFramework;
import weka.core.Instances;

public class TestDatasetEvaluator extends BaseTestFramework {
	
	@Test
	public final void testActivateDataset() throws Exception {
		// first upload a dataset
		DataSetDescription dsd = new DataSetDescription("testXor", "testXor", "arff", "class");
		Instances dataset = DatasetFactory.getXORNominal();
		File datasetFile = Conversion.stringToTempFile(dataset.toString(), "xor", "arff");
		int did = client_admin_test.dataUpload(dsd, datasetFile);
		
		new ProcessDataset(client_admin_test, did, null);
		// this automatically processes the dataset
		
		DataFeature f = client_admin_test.dataFeatures(did);
		assertTrue(f.getFeatures().length == dataset.numAttributes());
		
		DataQuality q = client_admin_test.dataQualities(did, Settings.EVALUATION_ENGINE_ID);
		assertTrue(q.getQualities().length > 0);
		
		DataSetDescription descr = client_admin_test.dataGet(did);
		assertEquals(descr.getStatus(), Constants.DATA_STATUS_ACTIVE);
	}

}
