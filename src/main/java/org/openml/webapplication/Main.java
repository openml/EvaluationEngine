/*
 *  Webapplication - Java library that runs on OpenML servers
 *  Copyright (C) 2014 
 *  @author Jan N. van Rijn (j.n.van.rijn@liacs.leidenuniv.nl)
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 */
package org.openml.webapplication;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.json.JSONArray;
import org.openml.apiconnector.algorithms.TaskInformation;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.settings.Config;
import org.openml.apiconnector.settings.Settings;
import org.openml.apiconnector.xml.DataSetDescription;
import org.openml.apiconnector.xml.Task;
import org.openml.apiconnector.xml.Task.Input.Estimation_procedure;
import org.openml.webapplication.features.FantailConnector;
import org.openml.webapplication.generatefolds.ChallengeSets;
import org.openml.webapplication.generatefolds.GenerateFolds;
import org.openml.webapplication.io.Output;

public class Main {
	
	public static final int FOLD_GENERATION_SEED = 0;
	
	public static final int[] TASK_TYPE_IDS = {1, 2, 3, 4, 5, 6, 7};
	
	public static void main( String[] args ) {
		OpenmlConnector apiconnector;
		CommandLineParser parser = new GnuParser();
		Options options = new Options();
		Integer id = null;
		Config config;
		
		options.addOption("id", true, "The id of the dataset/run used");
		options.addOption("u", true, "The user id");
		options.addOption("config", true, "The config string describing the settings for API interaction");
		options.addOption("f", true, "The function to invole");
		options.addOption("o", true, "The output file / offset");
		options.addOption("r", true, "The run id");
		options.addOption("reverse", false, "whether to start evaluating last runs");
		options.addOption("t", true, "The task id");
		options.addOption("x", false, "Flag determining whether we should pick a random id");
		options.addOption("m", false, "Flag determining whether the output of the splits file should be presented as a md5 hash");
		options.addOption("test", true, "A list of rowids for a holdout set (fold generation)" );
		options.addOption("tag", true, "A tag that will get priority in processing fantail features. " );
		options.addOption("mode", true, "{train,test}" );
		options.addOption("size", true, "Desired size of train/test set" );
		options.addOption("v", false, "Flag determining whether to have verbose output");
		
		try {
			CommandLine cli  = parser.parse(options, args);
			if( cli.hasOption("config") == false ) {
				config = new Config();
			} else {
				config = new Config(cli.getOptionValue("config"));
			}
			config.updateStaticSettings();
			Settings.CACHE_ALLOWED = false;
			
			if (config.getServer() != null) {
				apiconnector = new OpenmlConnector(config.getServer(),config.getApiKey());
			} else {
				apiconnector = new OpenmlConnector(config.getApiKey());
			}
			
			if (cli.hasOption("v")) {
				apiconnector.setVerboseLevel(1);
			}
			
			if( cli.hasOption("id") ) {
				id = Integer.parseInt(cli.getOptionValue("id"));
			}
			
			if( cli.hasOption("f") ) {
				
				String function = cli.getOptionValue("f");
				if( function.equals("evaluate_run") ) {
					int[] ttids = TASK_TYPE_IDS;
					if (cli.hasOption("mode")) {
						ttids = new int[1];
						ttids[0] = Integer.parseInt(cli.getOptionValue("mode"));
					}
					Integer uploaderId = cli.hasOption("u") ? Integer.parseInt(cli.getOptionValue("u")) : null;
					String evaluationMode = cli.hasOption("x") ? "random" : "normal";
					if (cli.hasOption("reverse")) {
						evaluationMode = "reverse";
					}
					String taskIds = cli.hasOption("t") ? cli.getOptionValue("t") : null;
					
					// bootstrap evaluate run
					new EvaluateRun(apiconnector, id, evaluationMode, ttids, taskIds, cli.getOptionValue("tag"), uploaderId);
					
				} else if( function.equals("process_dataset") ) {
					// bootstrap process dataset
					String processMode = cli.hasOption("x") ? "random" : "normal";
					new ProcessDataset(apiconnector, id, processMode);
				} else if( function.equals("extract_features_all") ) {
					
					String mode = cli.hasOption("x") ? "random" : "normal";
					FantailConnector fc = new FantailConnector( apiconnector, id, mode, "all", cli.getOptionValue("tag"), null);
					fc.toString();
					
				} else if( function.equals("extract_features") ) {
					// c can be a characterizer class name, 'fast' or 'all'
					String mode = cli.hasOption("x") ? "random" : "normal";
					String characterizer = cli.hasOption("c") ? cli.getOptionValue("c") : null;
					FantailConnector fc = new FantailConnector( apiconnector, id, mode, characterizer, cli.getOptionValue("tag"), null);
					fc.toString();
					
				} else if( function.equals("generate_folds") ) {
					
					URL datasetUrl;
					String target_feature;
					String estimation_procedure;
					String custum_testset = null; 
					String datasetName;
					
					
					if(cli.hasOption("id")) {
						id = Integer.parseInt(cli.getOptionValue("id"));
						Task current = apiconnector.taskGet(id);
						Map<String, String> inputs = apiconnector.taskInputs(id).getInputsAsMap();
						int dataset_id = TaskInformation.getSourceData(current).getData_set_id();
						DataSetDescription dsd = apiconnector.dataGet(dataset_id);
						Estimation_procedure ep = TaskInformation.getEstimationProcedure(current);
						Integer numberOfRepeats = null;
						Integer numberOfFolds = null;
						Integer percentage = null;
						try {numberOfRepeats = TaskInformation.getNumberOfRepeats(current);} catch(Exception e) {}
						try {numberOfFolds = TaskInformation.getNumberOfFolds(current);} catch(Exception e) {}
						try {percentage = TaskInformation.getPercentage(current);} catch(Exception e) {}
						datasetName = dsd.getName();
						datasetUrl = apiconnector.getOpenmlFileUrl(dsd.getFile_id(), dsd.getName());
						target_feature = TaskInformation.getSourceData(current).getTarget_feature();
						estimation_procedure = ep.getType();
						if (numberOfRepeats != null) {estimation_procedure += "_" + numberOfRepeats;}
						if (numberOfFolds != null) {estimation_procedure += "_" + numberOfFolds;}
						if (percentage != null) {estimation_procedure += "_" + percentage;}
						
						if (inputs.containsKey("custom_testset")) {
							custum_testset = inputs.get("custom_testset");
						}
					} else {
						System.out.println(Output.styleToJsonError("Missing arguments for function 'generate_folds'. Need id (task_id). "));
						return;
					}
					

					List<List<List<Integer>>> testset = new ArrayList<List<List<Integer>>>();
					if(custum_testset != null && custum_testset.length() > 0) {
						JSONArray rowidsJson;
						try {
							rowidsJson = new JSONArray(custum_testset);
						} catch(Exception e) {
							System.out.println(Output.styleToJsonError("Problem parsing custom splits.  "));
							return;
						}
						
						for(int i = 0; i < rowidsJson.length(); ++i) {
							while (testset.size() <= i) {
								testset.add(new ArrayList<List<Integer>>());
							}
							
							for( int j = 0; j < rowidsJson.getJSONArray(i).length(); ++j ) {
								while (testset.get(i).size() <= j) {
									testset.get(i).add(new ArrayList<Integer>());
								}
								
								for( int k = 0; k < rowidsJson.getJSONArray(i).getJSONArray(j).length(); ++k ) {
									
									testset.get(i).get(j).add(rowidsJson.getJSONArray(i).getJSONArray(j).getInt(k));
								}
								
							}
						}
					}
					
					GenerateFolds gf = new GenerateFolds(
							apiconnector, 
							datasetName,
							datasetUrl, 
							estimation_procedure, 
							target_feature, 
							testset, 
							FOLD_GENERATION_SEED );
					if(cli.hasOption("o") == true) {
						gf.toFile(cli.getOptionValue("o"));
					} else if(cli.hasOption("m") == true) {
						gf.toStdOutMd5();
					}else {
						gf.toStdout();
					}
					
				} else if(function.equals("all_wrong")) {
					
					if(cli.hasOption("r") && cli.hasOption("t")) {
						
						String[] run_ids_splitted = cli.getOptionValue("r").split(",");
						Integer task_id = Integer.parseInt(cli.getOptionValue("t"));
						List<Integer> run_ids = new ArrayList<Integer>();
						
						for (String s : run_ids_splitted) {
							run_ids.add(Integer.parseInt(s));
						}
						InstanceBased aw = new InstanceBased(apiconnector, run_ids, task_id);
						
						aw.calculateAllWrong();
						
						aw.toStdout(null);
					} else {
						System.out.println( Output.styleToJsonError("Missing arguments for function 'different_predictions'. Need r (run ids, comma separated) and t (task_id)") );
					}
					
				} else if(function.equals("different_predictions")) {
					
					if( cli.hasOption("r") && cli.hasOption("t") ) {
						
						String[] run_ids_splitted = cli.getOptionValue("r").split(",");
						Integer task_id = Integer.parseInt(cli.getOptionValue("t"));
						List<Integer> run_ids = new ArrayList<Integer>();
						
						for (String s : run_ids_splitted) {
							run_ids.add(Integer.parseInt(s));
						}
						InstanceBased aw = new InstanceBased(apiconnector, run_ids, task_id);
						
						int diff = aw.calculateDifference();
						String[] leadingComments = {"Classifier Output Difference: " + diff + "/" + aw.taskSplitSize()};
						
						aw.toStdout(leadingComments);
					} else {
						System.out.println( Output.styleToJsonError("Missing arguments for function 'all_wrong'. Need r (run ids, comma separated) and t (task_id)") );
					}
				} else if(function.equals("challenge")) {
					Integer task_id = Integer.parseInt(cli.getOptionValue("t"));
					boolean isTrain = cli.getOptionValue("mode").equals("train");
					Integer offset = null;
					Integer size = null;
					
					if (cli.hasOption("o")) {
						offset = Integer.parseInt(cli.getOptionValue("o"));
						
						if (cli.hasOption("size")) {
							size = Integer.parseInt(cli.getOptionValue("size"));
						}
					}
					
					ChallengeSets challenge = new ChallengeSets(apiconnector, task_id);
					if (isTrain) {
						challenge.train(offset, size);
					} else {
						challenge.test(offset, size);
					}
					
				} else {
					System.out.println( Output.styleToJsonError("call to unknown function: " + function) );
				}
			} else {
				System.out.println( Output.styleToJsonError("No function specified. ") );
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println( Output.styleToJsonError(e.getMessage() ));
			System.exit(1);
		}
		
		// makes sure the system halts, even when cortana is executed
		System.exit(0);
	}

	public static boolean isInteger(String s) {
		try {
			Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}
}
