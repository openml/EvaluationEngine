package opg.openml.webapplication.evaluator;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;
import org.openml.apiconnector.io.ApiException;
import org.openml.apiconnector.xml.Run;
import org.openml.apiconnector.xstream.XstreamXmlMapping;
import org.openml.webapplication.EvaluateRun;

import org.openml.webapplication.testutils.BaseTestFramework;

public class TestRunEvaluator extends BaseTestFramework {
	
	@Test
	public final void testEvaluateRun() throws Exception {
		File description = new File("data/test/run_1/description.xml");
		Run run = (Run) XstreamXmlMapping.getInstance().fromXML(description);
		File predictions = new File("data/test/run_1/predictions.arff");
		Map<String, File> outputFiles = new TreeMap<>();
		outputFiles.put("predictions", predictions);
		int rid = client_write_test.runUpload(run, outputFiles);
		
		try {
			new EvaluateRun(client_admin_test, rid, null, null, null, null, null);
			// automatically processes run
		} catch (ApiException e) {
			// sometimes OpenML already processed the run ... 
		}
		
		Run runDownloaded = client_write_test.runGet(rid);
		assertTrue(runDownloaded.getOutputEvaluation().length > 5);
	}
}
