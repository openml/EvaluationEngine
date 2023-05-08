package org.openml.webapplication.features;

import org.junit.Test;
import org.openml.apiconnector.xml.DataFeature;
import weka.core.Instances;

import java.io.FileReader;
import java.io.Reader;
import java.util.List;

import static org.junit.Assert.*;

public class TestExtractFeatures {
    private static final double DELTA = 1e-6;
    @Test
    public final void testFeatureExtraction() throws Exception {
        String targetAttribute = "play";
        //        Reader dataset = new FileReader("data/test/datasets/weather.arff");
        Instances dataset = new Instances(new FileReader("data/test/datasets/weather.arff"));
        List<DataFeature.Feature> features = ExtractFeatures.getFeatures(dataset, targetAttribute);

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
}
