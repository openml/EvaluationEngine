package org.openml.webapplication.generatefolds;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;

import org.openml.apiconnector.algorithms.Conversion;
import org.openml.apiconnector.algorithms.DateParser;
import org.openml.apiconnector.algorithms.TaskInformation;
import org.openml.apiconnector.xml.DataSetDescription;
import org.openml.apiconnector.xml.Task;
import org.openml.apiconnector.xml.Task.Input.Data_set;
import org.openml.apiconnector.xml.Task.Input.Stream_schedule;
import org.openml.webapplication.io.Output;
import org.openml.webapplication.settings.Settings;
import org.openml.weka.io.OpenmlWekaConnector;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

public class ChallengeSets {

	private final Instances dataset;
	private final Integer trainAvailable;
	private final Integer testAvailable;
	private final Attribute targetAttribute;
	
	public ChallengeSets(OpenmlWekaConnector apiconnector, Integer task_id) throws Exception {
		Task current = apiconnector.taskGet(task_id);
		
		if (current.getTask_type_id() != 9) {
			throw new Exception("Task not of type 'Data Stream Challenge': " + task_id);
		}
		
		Data_set ds = TaskInformation.getSourceData(current);
		int dataset_id = ds.getData_set_id();
		DataSetDescription dsd = apiconnector.dataGet(dataset_id);
		Map<String, Double> dq = apiconnector.dataQualities(dsd.getId(), Settings.EVALUATION_ENGINE_ID).getQualitiesMap();
		Integer numInstances = dq.get("NumberOfInstances").intValue();
		
		Stream_schedule stream_schedule = TaskInformation.getStreamSchedule(current);
		
		long secondsInProgres = DateParser.secondsSince(stream_schedule.getStart_time());
		long batchesAvailable = secondsInProgres / stream_schedule.getBatch_time();
		long trainAvailableEst = batchesAvailable * stream_schedule.getBatch_size() + stream_schedule.getInitial_batch_size();
		trainAvailable = secondsInProgres < 0 ? 0 : Math.min(numInstances, (int) trainAvailableEst);
		testAvailable = secondsInProgres < 0 ? 0 : Math.min(numInstances, trainAvailable + stream_schedule.getBatch_size());
		
		// System.out.println(secondsInProgres + "," +trainAvailable  + "," +  testAvailable);
		
		dataset = new Instances(apiconnector.getDataset(dsd));
		targetAttribute = dataset.attribute(ds.getTarget_feature());
	}
	
	public void train(Integer offset, Integer numInstances) throws IOException {
		if (offset == null) {
			offset = 0;
		}
		if (numInstances == null) {
			numInstances = trainAvailable;
		}
		int size = trainAvailable - offset;
		if (size <= 0) {
			throw new RuntimeException("No train instances found within search criteria. ");
		}
		
		Conversion.log("OK", "Challenge Train Set", "Range <" + offset + "-" + trainAvailable + "]");
		Instances trainingSet = new Instances(dataset, size);
		for (int i = offset; i < Math.min(offset + numInstances, trainAvailable); ++i) {
			trainingSet.add((Instance) dataset.get(i).copy());
		}
		Output.instances2file(trainingSet, new OutputStreamWriter(System.out), null);
	}
	
	public void test(Integer offset, Integer numInstances) throws IOException {
		if (offset == null) {
			offset = trainAvailable;
		}
		if (numInstances == null) {
			numInstances = testAvailable;
		}
		
		int size = testAvailable - offset;
		if (size <= 0) {
			throw new RuntimeException("No test instances found within search criteria. ");
		}
		
		Conversion.log("OK", "Challenge Test Set", "Range <" + offset + "-" + testAvailable + "]");
		
		Instances testSet = new Instances(dataset, size);
		for (int i = offset; i < Math.min(offset + numInstances, testAvailable); ++i) {
			Instance current = (Instance) dataset.get(i).copy();
			current.setValue(targetAttribute, Double.NaN);
			testSet.add(current);
		}
		Output.instances2file(testSet, new OutputStreamWriter(System.out), null);
	}
}
