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
	
	private final void processAndCheck(DataSetDescription dsd, Instances dataset) throws Exception {
		File datasetFile = Conversion.stringToTempFile(dataset.toString(), "testdataset", "arff");
		int did = client_admin_test.dataUpload(dsd, datasetFile);
		
		new ProcessDataset(client_admin_test, did, null);
		// this automatically processes the dataset
		
		DataFeature f = client_admin_test.dataFeatures(did);
		assertTrue(f.getFeatures().length == dataset.numAttributes());
		
		DataQuality q = client_admin_test.dataQualities(did, Settings.EVALUATION_ENGINE_ID);
		assertTrue(q.getQualities().length > 5);  // FIXME: 5 chosen arbitrarily
		
		DataSetDescription descr = client_admin_test.dataGet(did);
		assertEquals(descr.getStatus(), Constants.DATA_STATUS_ACTIVE);
	}
	
	@Test
	public final void testActivateNominalDataset() throws Exception {
		// first upload a dataset
		DataSetDescription dsd = new DataSetDescription("testXor", "testXor", "arff", "class");
		Instances dataset = DatasetFactory.getXORNominal();
		
		processAndCheck(dsd, dataset);
	}
	
	@Test
	public final void testActivateRegressionDataset() throws Exception {
		// first upload a dataset
		DataSetDescription dsd = new DataSetDescription("testXor", "testXor", "arff", "y");
		Instances dataset = DatasetFactory.getXORNumeric();
		
		processAndCheck(dsd, dataset);
	}
	
	@Test
	public final void testActivateDatasetNoClass() throws Exception {
		// first upload a dataset
		DataSetDescription dsd = new DataSetDescription("testXor", "testXor", "arff", null);
		Instances dataset = DatasetFactory.getXORNumericNoClass();
		
		processAndCheck(dsd, dataset);
	}
	
	@Test(expected = Exception.class)
	public final void testActivateDatasetIllegalClass() throws Exception {
		// first upload a dataset
		DataSetDescription dsd = new DataSetDescription("testXor", "testXor", "arff", "ClassNonExistant");
		Instances dataset = DatasetFactory.getXORNumericNoClass();
		
		processAndCheck(dsd, dataset);
	}

}
