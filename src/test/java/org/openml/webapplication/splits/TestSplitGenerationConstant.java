package org.openml.webapplication.splits;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.Test;
import org.openml.webapplication.generatefolds.GenerateFolds;
import org.openml.webapplication.testutils.BaseTestFramework;

/**
 * Tests whether the split generation has remained equal since previous versions 
 * (to ensure immutability of tasks)
 */

public class TestSplitGenerationConstant extends BaseTestFramework {
	
	private static final File TASK_SPLIT_HASHES = new File("data/test/splits/task_splits_hashes.csv"); 
	
	@Test
	public void testFromCsv() throws FileNotFoundException, IOException {
        Iterable <CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(new FileReader(TASK_SPLIT_HASHES));
        Set<Integer> missmatches = new TreeSet<>();
        Set<Integer> generationErros = new TreeSet<>();
        for (CSVRecord record : records) {
        	int taskId = Integer.parseInt(record.get("task_id"));
        	String md5Hash = record.get("md5_hash");
        	try { 
        		GenerateFolds gf = new GenerateFolds(client_read_live, taskId, 0);
        		String generatedMd5Hash = DigestUtils.md5Hex(gf.getSplits().toString());
        		if (!generatedMd5Hash.equals(md5Hash)) {
        			missmatches.add(taskId);
        		}
        	} catch(Exception e) {
        		generationErros.add(taskId);
        	}
        }
        assertEquals(generationErros.size(), 0);
        assertEquals(missmatches.size(), 0);
	}
}
