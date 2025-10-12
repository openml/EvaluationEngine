package org.openml.webapplication.mergedatasets;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;
import org.openml.webapplication.MergeDataset;
import org.openml.webapplication.testutils.BaseTestFramework;
import org.openml.webapplication.testutils.DatasetFactory;

import weka.core.Instances;

public class TestMergeDatasets extends BaseTestFramework {
	
	@Test
	public final void testMergeSameDataset() throws Exception {
		Map<String, Instances> datasets = new TreeMap<String, Instances>();
		datasets.put("1", DatasetFactory.getXORMixedNoClass());
		datasets.put("2", DatasetFactory.getXORMixedNoClass());
		
		MergeDataset md = new MergeDataset();
		md.setDatasets(datasets);
		
		Instances result = md.merge();
		assertEquals(DatasetFactory.getXORMixedNoClass().numInstances() * 2, result.numInstances());
		assertEquals(2, result.attribute(0).numValues());
	}

	@Test
	public final void testMergeSameOnlineDataset() throws Exception {
		List<Integer> dids = new ArrayList<Integer>();
		dids.add(41510); 
		dids.add(41511); 
		
		MergeDataset md = new MergeDataset();
		md.downloadDatasets(client_read_live, dids);
		
		Instances result = md.merge();
		assertEquals(300, result.numInstances());
		assertEquals(2, result.attribute(0).numValues());
	}

	@Test(expected = Exception.class)
	public final void testMergeDifferentDataset() throws Exception {
		Map<String, Instances> datasets = new TreeMap<String, Instances>();
		datasets.put("1", DatasetFactory.getXORMixedNoClass());
		datasets.put("2", DatasetFactory.getXORNominalNoClass());
		
		MergeDataset md = new MergeDataset();
		md.setDatasets(datasets);
		
		Instances result = md.merge();
		assertEquals(DatasetFactory.getXORMixedNoClass().numInstances() * 2, result.numInstances());
		assertEquals(2, result.attribute(0).numValues());
	}
}
