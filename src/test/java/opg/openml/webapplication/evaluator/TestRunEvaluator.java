package opg.openml.webapplication.evaluator;

import static org.junit.Assert.*;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.openml.apiconnector.io.ApiException;
import org.openml.apiconnector.xml.Run;
import org.openml.apiconnector.xml.RunTrace;
import org.openml.webapplication.EvaluateRun;

import org.openml.webapplication.testutils.BaseTestFramework;
import org.openml.weka.algorithm.WekaConfig;
import org.openml.weka.experiment.RunOpenmlJob;

import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.meta.MultiSearch;
import weka.classifiers.meta.multisearch.RandomSearch;
import weka.classifiers.trees.J48;
import weka.core.setupgenerator.AbstractParameter;
import weka.core.setupgenerator.MathParameter;

public class TestRunEvaluator extends BaseTestFramework {
	
	private final static int testEvaluateRun(int taskId, Classifier classifier) throws Exception {
		final String configString = "avoid_duplicate_runs=false; skip_jvm_benchmark=true;";
		final WekaConfig config = new WekaConfig(configString);
		final Pair<Integer, Run> result = RunOpenmlJob.executeTask(client_write_test, config, taskId, classifier);
		
		try {
			// note that changing the evaluation engine id does not help us with
			// adding duplicate records
			new EvaluateRun(client_admin_test, result.getLeft(), null, null, null, null, null);
			// automatically processes run
		} catch (ApiException e) {
			// sometimes OpenML already processed the run ... 
		}
		
		Run runDownloaded = client_write_test.runGet(result.getLeft());
		assertTrue(runDownloaded.getOutputEvaluation().length > 5);
		return result.getLeft();
	}
	
	@Test
	public final void testEvaluateClassificationRun() throws Exception {
		int taskId = 115;
		Classifier classifier = new NaiveBayes();
		testEvaluateRun(taskId, classifier);
	}
	
	@Test
	public final void testEvaluateLeaveOneOutClassification() throws Exception {
		int taskId = 238;
		Classifier classifier = new NaiveBayes();
		testEvaluateRun(taskId, classifier);
	}
	
	@Test
	public final void testEvaluateClassificationRunWithTrace() throws Exception {
		int taskId = 115;
		int numFolds = 10;
		int numRepeats = 1;
		int numIterations = 10;
		int runId = testEvaluateRun(taskId, getRandomSearchClassifier(numIterations));
		RunTrace trace = client_read_test.runTrace(runId);
		int expectedResults = numFolds * numRepeats * numIterations;
		assertTrue(trace.getTrace_iterations().length == expectedResults);
	}
	
	private Classifier getRandomSearchClassifier(int numIterations) throws Exception {
		RandomSearch randomSearchAlgorithm = new RandomSearch();
		randomSearchAlgorithm.setNumIterations(numIterations);
		randomSearchAlgorithm.setSearchSpaceNumFolds(2);
		
		J48 baseclassifier = new J48();
		
		MathParameter numFeatures = new MathParameter();
		numFeatures.setProperty("classifier.minNumObj");
		numFeatures.setBase(1);
		numFeatures.setExpression("I");
		numFeatures.setMin(1);
		numFeatures.setMax(20);
		numFeatures.setStep(1);

		MathParameter maxDepth = new MathParameter();
		maxDepth.setProperty("classifier.confidenceFactor");
		maxDepth.setBase(10);
		maxDepth.setExpression("pow(BASE,I)");
		maxDepth.setMin(-4);
		maxDepth.setMax(-1);
		maxDepth.setStep(1);
		
		AbstractParameter[] searchParameters = {numFeatures, maxDepth};
		
		MultiSearch search = new MultiSearch();
		String[] evaluation = {"-E", "ACC"};
		search.setOptions(evaluation);
		search.setClassifier(baseclassifier);
		search.setAlgorithm(randomSearchAlgorithm);
		search.setSearchParameters(searchParameters);
		return search;
	}
}
