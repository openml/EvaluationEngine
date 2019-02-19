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
import org.openml.apiconnector.xml.RunTrace;
import org.openml.webapplication.EvaluateRun;


public class TestRunEvaluator {
	
	private static final String testUrl = "https://test.openml.org/";
	private static final String testKeyWrite = "8baa83ecddfe44b561fd3d92442e3319"; // write key
	private static final String testKeyAdmin = "d488d8afd93b32331cf6ea9d7003d4c3"; // admin key
	private static final OpenmlConnector clientAdmin = new OpenmlConnector(testUrl, testKeyAdmin);
	private static final OpenmlConnector clientWrite = new OpenmlConnector(testUrl, testKeyWrite);
	
	@Test
	public final void testEvaluateRun() throws Exception {
		Settings.CACHE_ALLOWED = false;
		
		File description = new File("data/test/run_1/description.xml");
		File predictions = new File("data/test/run_1/predictions.arff");
		File trace = new File("data/test/run_1/trace.arff");
		Map<String, File> outputFiles = new TreeMap<>();
		outputFiles.put("predictions", predictions);
		outputFiles.put("trace", trace);
		int rid = clientWrite.runUpload(description, outputFiles).getRun_id();
		
		try {
			new EvaluateRun(clientAdmin, rid, null, null, null, null, null);
			// automatically processes run
		} catch (ApiException e) {
			// sometimes OpenML already processed the run ... 
		}
		
		Run run = clientWrite.runGet(rid);
		assertTrue(run.getOutputEvaluation().length > 5);
		RunTrace runTrace = clientWrite.runTrace(rid);
		assertTrue(runTrace.getTrace_iterations().length > 10);
	}
}
