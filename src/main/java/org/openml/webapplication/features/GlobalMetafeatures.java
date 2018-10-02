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

public class GlobalMetafeatures {
    private final String preprocessingPrefix = "-E \"weka.attributeSelection.CfsSubsetEval -P 1 -E 1\" -S \"weka.attributeSelection.BestFirst -D 1 -N 5\" -W ";
    private final String cp1NN = "weka.classifiers.lazy.IBk";
    private final String cpNB = "weka.classifiers.bayes.NaiveBayes";
    private final String cpASC = "weka.classifiers.meta.AttributeSelectedClassifier";
    private final String cpDS = "weka.classifiers.trees.DecisionStump";
    //private final StreamCharacterizer[] streamCharacterizers;

    private ArrayList<Characterizer> batchCharacterizers;
    private int expectedQualities;
    private ArrayList<String> expectedIds = new ArrayList<>();

    public GlobalMetafeatures(String characterizer, Integer window_size) throws Exception {
    	
    	ArrayList<Characterizer> batchCharacterizers = new ArrayList<Characterizer>();
    	batchCharacterizers.add(new SimpleMetaFeatures()); // always included because others depend on them

		// Add characterizer of choice
    	if (characterizer != null && !characterizer.equals("GenericLandmarker") 
    			&& !characterizer.equals("fast") && !characterizer.equals("SimpleMetaFeatures")) {
    		Characterizer ch = (Characterizer) Class.forName(characterizer).newInstance();
    		batchCharacterizers.add(ch);
    	} 
    	
		// Add all 'fast' characterizers
        if (characterizer == null || characterizer.equals("fast")) {
        	batchCharacterizers.add(new Statistical());
        	batchCharacterizers.add(new NominalAttDistinctValues());
        	batchCharacterizers.add(new AttributeEntropy());
        }
        
		// Add landmarkers
        if (characterizer == null || characterizer.equals("GenericLandmarker")) {
        	batchCharacterizers.add(new GenericLandmarker("kNN1N", cp1NN, 2, null));
        	batchCharacterizers.add(new GenericLandmarker("NaiveBayes", cpNB, 2, null));
        	batchCharacterizers.add(new GenericLandmarker("DecisionStump", cpDS, 2, null));
        	batchCharacterizers.add(new GenericLandmarker("CfsSubsetEval_kNN1N", cpASC, 2, Utils.splitOptions(preprocessingPrefix + cp1NN)));
        	batchCharacterizers.add(new GenericLandmarker("CfsSubsetEval_NaiveBayes", cpASC, 2, Utils.splitOptions(preprocessingPrefix + cpNB)));
        	batchCharacterizers.add(new GenericLandmarker("CfsSubsetEval_DecisionStump", cpASC, 2, Utils.splitOptions(preprocessingPrefix + cpDS)));
        
		    String zeros = "0";
		    for( int i = 1; i <= 3; ++i ) {
		        zeros += "0";
		        String[] j48Option = { "-C", "." + zeros + "1" };
		        batchCharacterizers.add(new GenericLandmarker("J48." + zeros + "1.", "weka.classifiers.trees.J48", 2, j48Option));
		
		        String[] repOption = { "-L", "" + i };
		        batchCharacterizers.add(new GenericLandmarker("REPTreeDepth" + i, "weka.classifiers.trees.REPTree", 2, repOption));
		
		        String[] randomtreeOption = { "-depth", "" + i };
		        batchCharacterizers.add(new GenericLandmarker("RandomTreeDepth" + i, "weka.classifiers.trees.RandomTree", 2, randomtreeOption));
		    }
        }
        
        for(Characterizer c : batchCharacterizers) {
            expectedQualities += c.getNumMetaFeatures();
            expectedIds.addAll(Arrays.asList(c.getIDs()));
        }

   /*     streamCharacterizers = new StreamCharacterizer[]{new ChangeDetectors(window_size)};

        for (StreamCharacterizer streamCharacterizer: streamCharacterizers) {
            expectedQualities += streamCharacterizer.getNumMetaFeatures();
            expectedIds.addAll(Arrays.asList(streamCharacterizer.getIDs()));
        } */
    }

    public int getExpectedQualities() {
        return expectedQualities;
    }

    public List<String> getExpectedIds() {
        return expectedIds;
    }

    public List<Characterizer> getCharacterizers(){
        return batchCharacterizers;
    }

    //public StreamCharacterizer[] getStreamCharacterizers() { return streamCharacterizers; }
}
