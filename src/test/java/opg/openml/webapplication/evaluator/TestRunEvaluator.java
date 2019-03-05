package opg.openml.webapplication.evaluator;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;
import org.openml.apiconnector.io.ApiException;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.settings.Settings;
import org.openml.apiconnector.xml.Run;
import org.openml.apiconnector.xstream.XstreamXmlMapping;
import org.openml.webapplication.EvaluateRun;


public class TestRunEvaluator {
	
	private static final String testUrl = "https://test.openml.org/";
	private static final String testKeyWrite = "8baa83ecddfe44b561fd3d92442e3319"; // write key
	private static final String testKeyAdmin = "d488d8afd93b32331cf6ea9d7003d4c3"; // admin key
	private static final OpenmlConnector clientAdmin = new OpenmlConnector(testUrl, testKeyAdmin);
	private static final OpenmlConnector clientWrite = new OpenmlConnector(testUrl, testKeyWrite);
	
	@Test
	public final void testEvaluateRun() throws Exception {
		// TODO: add test trace function
		Settings.CACHE_ALLOWED = false;
		
		File description = new File("data/test/run_1/description.xml");
		Run run = (Run) XstreamXmlMapping.getInstance().fromXML(description);
		File predictions = new File("data/test/run_1/predictions.arff");
		Map<String, File> outputFiles = new TreeMap<>();
		outputFiles.put("predictions", predictions);
		int rid = clientWrite.runUpload(run, outputFiles);
		
		try {
			new EvaluateRun(clientAdmin, rid, null, null, null, null, null);
			// automatically processes run
		} catch (ApiException e) {
			// sometimes OpenML already processed the run ... 
		}
		
		Run runDownloaded = clientWrite.runGet(rid);
		assertTrue(runDownloaded.getOutputEvaluation().length > 5);
	}
}
