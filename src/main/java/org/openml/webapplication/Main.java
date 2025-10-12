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

import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.openml.apiconnector.algorithms.Conversion;
import org.openml.apiconnector.settings.Config;
import org.openml.apiconnector.settings.Settings;
import org.openml.webapplication.exceptions.LegacyWarning;
import org.openml.webapplication.features.CharacterizerFactory;
import org.openml.webapplication.features.FantailConnector;
import org.openml.webapplication.generatefolds.ChallengeSets;
import org.openml.webapplication.generatefolds.GenerateFolds;
import org.openml.webapplication.io.Output;
import org.openml.weka.io.OpenmlWekaConnector;

import weka.core.Instances;

public class Main {

	public static final int FOLD_GENERATION_SEED = 0;

	public static void main(String[] args) {
		OpenmlWekaConnector apiconnector;
		CommandLineParser parser = new GnuParser();
		Options options = new Options();
		Integer id = null;
		Config config;

		options.addOption("id", true, "The id of the dataset/run used");
		options.addOption("u", true, "The user id");
		options.addOption("config", true, "The config string describing the settings for API interaction");
		options.addOption("f", true, "The function to invoke");
		options.addOption("o", true, "The output file / offset");
		options.addOption("r", true, "The run id");
		options.addOption("reverse", false, "whether to start evaluating last runs");
		options.addOption("t", true, "The task id");
		options.addOption("x", false, "Flag determining whether we should pick a random id");
		options.addOption("m", false, "Flag determining whether the output of the splits file should be presented as a md5 hash");
		options.addOption("test", true, "A list of rowids for a holdout set (fold generation)");
		options.addOption("tag", true, "A tag that will get priority in processing fantail features. ");
		options.addOption("mode", true, "{train,test}");
		options.addOption("size", true, "Desired size of train/test set");
		options.addOption("v", false, "Flag determining whether to have verbose output");

		try {
			CommandLine cli = parser.parse(options, args);
			if (cli.hasOption("config") == false) {
				config = new Config();
			} else {
				config = new Config(cli.getOptionValue("config"));
			}
			config.updateStaticSettings();
			Settings.CACHE_ALLOWED = false;

			if (config.getServer() != null) {
				apiconnector = new OpenmlWekaConnector(config.getServer(), config.getApiKey());
			} else {
				apiconnector = new OpenmlWekaConnector(Settings.BASE_URL, config.getApiKey());
			}

			if (cli.hasOption("v")) {
				apiconnector.setVerboseLevel(1);
			}

			if (cli.hasOption("id")) {
				id = Integer.parseInt(cli.getOptionValue("id"));
			}

			if (cli.hasOption("f")) {

				String function = cli.getOptionValue("f");
				if (function.equals("evaluate_run")) {
					int[] ttids = org.openml.webapplication.settings.Settings.SUPPORTED_TASK_TYPES_EVALUATION;
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

				} else if (function.equals("process_dataset")) {
					// bootstrap process dataset
					String processMode = cli.hasOption("x") ? "random" : "normal";
					new ProcessDataset(apiconnector, id, processMode);
				} else if (function.equals("extract_features_all")) {
					String mode = cli.hasOption("x") ? "random" : "normal";
					FantailConnector fc = new FantailConnector(apiconnector, CharacterizerFactory.all(null));
					fc.start(id, mode, cli.getOptionValue("tag"), null);
					fc.toString();
				} else if (function.equals("extract_features_simple")) {
					String mode = cli.hasOption("x") ? "random" : "normal";
					FantailConnector fc = new FantailConnector(apiconnector, CharacterizerFactory.simple());
					fc.start(id, mode, cli.getOptionValue("tag"), null);
					fc.toString();
				} else if (function.equals("merge_datasets")) {
					MergeDataset md = new MergeDataset(Integer.parseInt(cli.getOptionValue("id")), apiconnector);
					
					Instances merged = md.merge();
					if (cli.hasOption("o") == true) {
						FileWriter f = new FileWriter(cli.getOptionValue("o"));
						Output.instances2file(merged, f, null);
					} else {
						Output.instances2file(merged, new OutputStreamWriter(System.out), null);
					}
				} else if (function.equals("generate_folds")) {
					GenerateFolds gf = new GenerateFolds(apiconnector, Integer.parseInt(cli.getOptionValue("id")), FOLD_GENERATION_SEED);
					Instances splits = gf.getSplits();
					if (cli.hasOption("o") == true) {
						FileWriter f = new FileWriter(cli.getOptionValue("o"));
						Output.instances2file(splits, f, null);
					} else {
						Output.instances2file(splits, new OutputStreamWriter(System.out), null);
					}

				} else if (function.equals("all_wrong")) {

					if (cli.hasOption("r") && cli.hasOption("t")) {

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
						Conversion.log("Warning", "all_wrong", Output.styleToJsonError("Missing arguments for function 'all_wrong'. Need r (run ids, comma separated) and t (task_id)") );
					}

				} else if (function.equals("different_predictions")) {

					if (cli.hasOption("r") && cli.hasOption("t")) {

						String[] run_ids_splitted = cli.getOptionValue("r").split(",");
						Integer task_id = Integer.parseInt(cli.getOptionValue("t"));
						List<Integer> run_ids = new ArrayList<Integer>();

						for (String s : run_ids_splitted) {
							run_ids.add(Integer.parseInt(s));
						}
						InstanceBased aw = new InstanceBased(apiconnector, run_ids, task_id);

						int diff = aw.calculateDifference();
						String[] leadingComments = {"Classifier Output Difference: " + diff + "/" + aw.taskSplitSize() };

						aw.toStdout(leadingComments);
					} else {
						Conversion.log("Warning", "different_predictions", Output.styleToJsonError("Missing arguments for function 'different_predictions'. Need r (run ids, comma separated) and t (task_id)") );
					}
				} else if (function.equals("challenge")) {
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
					Conversion.log("Error", "Main", Output.styleToJsonError("call to unknown function: " + function) );
				}
			} else {
				Conversion.log("Error", "Main", Output.styleToJsonError("No function specified. ") );
			}
		} catch (LegacyWarning e) {
			// exit status 0 to prevent mail
			System.out.println(Output.styleToJsonError(e.getMessage()));
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
			Conversion.log("Error", "Main", Output.styleToJsonError(e.getMessage() ));
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
