package org.openml.webapplication.features;

import org.junit.Test;
import org.openml.apiconnector.xml.DataFeature;
import org.openml.apiconnector.xml.DataQuality;
import weka.core.converters.ArffLoader;

import java.io.FileReader;
import java.io.Reader;
import java.util.List;

import static org.junit.Assert.*;

public class TestExtractFeatures {
    private static final double DELTA = 1e-9;
    @Test
    public final void testFeatureExtraction() throws Exception {
        String targetAttribute = "play";
        Reader reader = new FileReader("data/test/datasets/weather.arff");
        ArffLoader.ArffReader dataset = new ArffLoader.ArffReader(reader, 1000, false);
        List<DataFeature.Feature> features = FeatureExtractor.getFeatures(dataset, targetAttribute);

        assertEquals(5, features.size());

        DataFeature.Feature feature = features.get(0);
        assertEquals("outlook", feature.getName());
        assertEquals("nominal", feature.getDataType());
        assertArrayEquals(new String[]{"overcast","rainy", "sunny"}, feature.getNominalValues());
        assertEquals("[[\"overcast\",\"rainy\",\"sunny\"],[[0, 4],[2, 3],[3, 2]]]", feature.getClassDistribution());
        assertEquals(14, feature.getNumberOfIntegerValues().intValue());

        assertEquals(14, feature.getNumberOfValues().intValue());
        assertEquals(3, feature.getNumberOfNominalValues().intValue());
        assertEquals(3, feature.getNumberOfDistinctValues().intValue());
        assertEquals(0, feature.getNumberOfMissingValues().intValue());
        assertEquals(14, feature.getNumberOfIntegerValues().intValue());
        assertEquals(0, feature.getNumberOfRealValues().intValue());
        assertEquals(0, feature.getNumberOfUniqueValues().intValue());

        assertNull(feature.getMinimumValue());
        assertNull(feature.getMaximumValue());
        assertNull(feature.getMeanValue());
        assertNull(feature.getStandardDeviation());

        feature = features.get(2);
        assertEquals("humidity", feature.getName());
        assertEquals("numeric", feature.getDataType());
        assertArrayEquals(new String[]{}, feature.getNominalValues());
        assertEquals("[]", feature.getClassDistribution());
        assertEquals(14, feature.getNumberOfIntegerValues().intValue());

        assertEquals(14, feature.getNumberOfValues().intValue());
        assertNull(feature.getNumberOfNominalValues());
        assertEquals(10, feature.getNumberOfDistinctValues().intValue());
        assertEquals(0, feature.getNumberOfMissingValues().intValue());
        assertEquals(14, feature.getNumberOfIntegerValues().intValue());
        assertEquals(0, feature.getNumberOfRealValues().intValue());
        assertEquals(7, feature.getNumberOfUniqueValues().intValue());

        assertEquals(65.0, feature.getMinimumValue(), DELTA);
        assertEquals(96.0, feature.getMaximumValue(), DELTA);
        assertEquals(81.64285714285714, feature.getMeanValue(), DELTA);
        assertEquals(10.285218242007035, feature.getStandardDeviation(), DELTA);
    }

    /**
     * The target is real, so no ClassDistribution should be summarized for any attribute.
     */
    @Test
    public final void testFeatureExtractionWithRealTarget() throws Exception {
        String targetAttribute = "num";
        Reader reader = new FileReader("data/test/datasets/cleveland.arff");
        ArffLoader.ArffReader dataset = new ArffLoader.ArffReader(reader, 1000, false);
        List<DataFeature.Feature> features = FeatureExtractor.getFeatures(dataset, targetAttribute);
        for (DataFeature.Feature feature : features) {
            assertEquals("[]", feature.getClassDistribution());
        }
    }

    @Test
    public final void testFeatureExtractionWithCompletelyNaNFeatures() throws Exception {
        String targetAttribute = "Class";
        Reader reader = new FileReader("data/test/datasets/sick.arff");
        ArffLoader.ArffReader dataset = new ArffLoader.ArffReader(reader, 1000, false);
        List<DataFeature.Feature> features = FeatureExtractor.getFeatures(dataset, targetAttribute);

        DataFeature.Feature tbg = features.stream().filter(feature -> feature.getName().equals("TBG")).findFirst().orElseThrow();
        assertNull(tbg.getMeanValue());  // This raised an error before, because it divided by zero
    }
}
