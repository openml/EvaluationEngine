# EvaluationEngine
Sources of the Java Evaluation Engine. The evaluation engine performs backend tasks for OpenML:

- `process_datasets`: download a dataset, describe information per feature (e.g., the datatype and number of values per feature) and upload the results. 
- `evaluate_run`: download a dataset and its splits, run the run's predictor on this dataset, and upload the evaluation (e.g., mean absolute error).
- `extract_features_all`: download a dataset, describe some features such as maximum number of distinct values of nominal features, and upload the results.
- `extract_features_simple`: same as `extract_features_all`, but with a subset of features.

## Usage

See [docker](./docker/README.md) directory for how to run this using docker.

### Prerequisites

- Java. It works with 11 LTS, compatibility with other versions is unknown.
- Maven

### Compile / package

    mvn clean compile                 # only compile
    mvn clean package -DskipTests     # package (generate jar)

### Run
Generate the jar (see "Compile / package"). Then

    java -jar target/EvaluationEngine-*-SNAPSHOT-jar-with-dependencies.jar -f process_dataset -id 1
    java -jar target/EvaluationEngine-*-SNAPSHOT-jar-with-dependencies.jar -f evaluate_run -id 1