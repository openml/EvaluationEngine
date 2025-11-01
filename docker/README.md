# Evaluation Engine

This docker container contains the OpenML Evaluation Engine.
Instructions only tested on a Linux host machine.

First pull the docker image:

    docker pull openml/evaluation-engine


## Usage

    docker run -it openml/evaluation-engine 


## Building

    docker build -f docker/Dockerfile --tag openml/evaluation-engine:0.0.1 .

    docker --push openml/evaluation-engine:0.0.1