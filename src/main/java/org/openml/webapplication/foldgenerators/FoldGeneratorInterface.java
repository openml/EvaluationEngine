package org.openml.webapplication.foldgenerators;

import weka.core.Instances;

public interface FoldGeneratorInterface {
	public Instances generate() throws Exception;
}
