package opg.openml.webapplication.evaluator;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.openml.apiconnector.algorithms.Conversion;
import org.openml.apiconnector.settings.Constants;
import org.openml.apiconnector.xml.DataFeature;
import org.openml.apiconnector.xml.DataQuality;
import org.openml.apiconnector.xml.DataSetDescription;
import org.openml.apiconnector.xml.DataFeature.Feature;
import org.openml.webapplication.ProcessDataset;
import org.openml.webapplication.features.ExtractFeatures;
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

	@Test
	public final void testCaseSensitiveFeaturesArffTextual() throws Exception {
		Instances data = client_read_live.getArffFromUrl(1910507);
		assertEquals("high", data.attribute(1).value(1));
		assertEquals("High", data.attribute(1).value(2));
		assertEquals("low", data.attribute(1).value(3));
		assertEquals("Low", data.attribute(1).value(4));
		
		assertEquals("Low", data.instance(0).stringValue(1));
		assertEquals("low", data.instance(154).stringValue(1));
	}
	
	@Test
	public final void testCaseSensitiveExtractorTextual() throws Exception {
		Instances data = client_read_live.getArffFromUrl(1910507);
		List<Feature> features = ExtractFeatures.getFeatures(data, "Class");
		List<String> featureNames = new ArrayList<String>(Arrays.asList(features.get(1).getNominalValues()));
		assertTrue(featureNames.contains("low"));
		assertTrue(featureNames.contains("Low"));
		assertTrue(featureNames.contains("high"));
		assertTrue(featureNames.contains("High"));
		
		assertTrue(xstream.toXML(features).indexOf("<oml:nominal_value>low</oml:nominal_value>") > 0);
		assertTrue(xstream.toXML(features).indexOf("<oml:nominal_value>Low</oml:nominal_value>") > 0);
	}
	

	@Test
	public final void testCaseSensitiveExtractorOnTest() throws Exception {
		DataSetDescription dsd = new DataSetDescription("test-case-sensitive", "test", "arff", "Class");
		int did = client_write_test.dataUpload(dsd, new File("data/test/datasets/casesensitive.arff"));
		DataSetDescription downloaded = client_read_test.dataGet(did);
		Instances data = client_read_test.getArffFromUrl(downloaded.getFile_id());
		List<Feature> features = ExtractFeatures.getFeatures(data, "Class");
		DataFeature datafeature = new DataFeature(did, Settings.EVALUATION_ENGINE_ID, features.toArray(new Feature[features.size()]));
		client_admin_test.dataFeaturesUpload(datafeature);
		System.err.println(did);
		DataFeature df = client_read_test.dataFeatures(did);
		List<String> featureNames = new ArrayList<String>(Arrays.asList(df.getFeatureMap().get("V3").getNominalValues()));
		assertTrue(featureNames.contains("low"));
		assertTrue(featureNames.contains("Low"));
		assertTrue(featureNames.contains("high"));
		assertTrue(featureNames.contains("High"));
	}

}
