package org.openml.webapplication.features;

import org.openml.webapplication.fantail.dc.Characterizer;
import org.openml.webapplication.fantail.dc.landmarking.GenericLandmarker;
import org.openml.webapplication.fantail.dc.statistical.AttributeEntropy;
import org.openml.webapplication.fantail.dc.statistical.NominalAttDistinctValues;
import org.openml.webapplication.fantail.dc.statistical.SimpleMetaFeatures;
import org.openml.webapplication.fantail.dc.statistical.Statistical;
import weka.core.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CharacterizerFactory {
	private static final String preprocessingPrefix = "-E \"weka.attributeSelection.CfsSubsetEval -P 1 -E 1\" -S \"weka.attributeSelection.BestFirst -D 1 -N 5\" -W ";
	private static final String cp1NN = "weka.classifiers.lazy.IBk";
	private static final String cpNB = "weka.classifiers.bayes.NaiveBayes";
	private static final String cpASC = "weka.classifiers.meta.AttributeSelectedClassifier";
	private static final String cpDS = "weka.classifiers.trees.DecisionStump";
	// private static final StreamCharacterizer[] streamCharacterizers;

	public static List<Characterizer> all(Integer window_size) throws Exception {
		Characterizer[] characterizers = { new SimpleMetaFeatures(), // potentially done before
				new Statistical(), new NominalAttDistinctValues(), new AttributeEntropy(),
				new GenericLandmarker("kNN1N", cp1NN, 2, null), new GenericLandmarker("NaiveBayes", cpNB, 2, null),
				new GenericLandmarker("DecisionStump", cpDS, 2, null),
				new GenericLandmarker("CfsSubsetEval_kNN1N", cpASC, 2, Utils.splitOptions(preprocessingPrefix + cp1NN)),
				new GenericLandmarker("CfsSubsetEval_NaiveBayes", cpASC, 2, Utils.splitOptions(preprocessingPrefix + cpNB)),
				new GenericLandmarker("CfsSubsetEval_DecisionStump", cpASC, 2, Utils.splitOptions(preprocessingPrefix + cpDS)) };
		List<Characterizer> batchCharacterizers = new ArrayList<>(Arrays.asList(characterizers));
		// additional parameterized batch landmarkers
		String zeros = "0";
		for (int i = 1; i <= 3; ++i) {
			zeros += "0";
			String[] j48Option = { "-C", "." + zeros + "1" };
			batchCharacterizers.add(new GenericLandmarker("J48." + zeros + "1.", "weka.classifiers.trees.J48", 2, j48Option));

			String[] repOption = { "-L", "" + i };
			batchCharacterizers.add(new GenericLandmarker("REPTreeDepth" + i, "weka.classifiers.trees.REPTree", 2, repOption));

			String[] randomtreeOption = { "-depth", "" + i };
			batchCharacterizers.add(new GenericLandmarker("RandomTreeDepth" + i, "weka.classifiers.trees.RandomTree", 2, randomtreeOption));
		}
		return batchCharacterizers;
	}
	
	public static List<Characterizer> simple() {
		Characterizer[] characterizers = { new SimpleMetaFeatures() }; // DO NOT ADD OTHERS, heapspace!
		List<Characterizer> batchCharacterizers = new ArrayList<>(Arrays.asList(characterizers));
		return batchCharacterizers;
	}
	
	public static List<String> getExpectedQualities(List<Characterizer> characterizers) {
		List<String> expectedQualities = new ArrayList<>();
		for (Characterizer c : characterizers) {
			expectedQualities.addAll(Arrays.asList(c.getIDs()));
		}
		return expectedQualities;
	}
}
