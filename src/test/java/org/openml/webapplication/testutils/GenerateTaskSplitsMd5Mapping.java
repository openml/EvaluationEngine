package org.openml.webapplication.testutils;

import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.openml.apiconnector.algorithms.TaskInformation;
import org.openml.apiconnector.xml.DataSetDescription;
import org.openml.apiconnector.xml.Task;
import org.openml.webapplication.generatefolds.GenerateFolds;

/**
 * Utility class that calculates the MD5 hash of the split files within 
 * a given range. Can be used by "testSplitGenerationConstant" to ensure
 * that the procedure for generating splits remains constant.  
 *  
 * @author janvanrijn
 */

public class GenerateTaskSplitsMd5Mapping extends BaseTestFramework {
	
	private static final Map<Integer, String> result = new TreeMap<>();
	
	public static void main(String[] args) throws Exception {
		// TODO: ensure that <userhome>/.openml/openml.conf has same settings as client_read_live
		int startIdx = 132;
		int endIdx = 1000;
		for (int i = startIdx; i <= endIdx; ++i) {
			Task t;
			DataSetDescription dsd;
			try {
				t = client_read_live.taskGet(i);
				dsd = client_read_live.dataGet(TaskInformation.getSourceData(t).getData_set_id());
			} catch(Exception e) {
				continue;
			}
			if (t.getTask_type_id() > 3) {
				continue;
			}
			
			if (!dsd.getStatus().equals("active")) {
				System.out.println("Dataset not active.");
				continue;
			}
            System.out.println("starting at " + i);
			GenerateFolds fg = new GenerateFolds(client_read_live, i, 0);
			String resultCurrentMd5;
			try {
				String resultCurrent = fg.getSplits().toString();
				resultCurrentMd5 = DigestUtils.md5Hex(resultCurrent);
			} catch(Error e) {
				e.printStackTrace();
				continue;
			}
			
        	String java_cmd = "java -jar /home/janvanrijn/projects/OpenML/openml_OS/third_party/OpenML/Java/evaluate.jar -f generate_folds -id " + i;
            Process p = Runtime.getRuntime().exec(java_cmd);
            String resultRepo = IOUtils.toString(p.getInputStream()).trim();
            String resultRepoMd5 = DigestUtils.md5Hex(resultRepo);
            
            if (!resultCurrentMd5.equals(resultRepoMd5) && t.getTask_type_id() != 3) {
            	System.out.println("Error at " + i);
                System.out.println(resultCurrentMd5);
                System.out.println(resultRepo);
            } else {
            	result.put(i, resultCurrentMd5);
            }
		}
		
		for (int index : result.keySet()) {
			System.out.println(index + "," + result.get(index));
		}
	}
}
