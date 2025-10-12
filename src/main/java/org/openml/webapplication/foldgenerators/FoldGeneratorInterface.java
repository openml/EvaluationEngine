package org.openml.webapplication.foldgenerators;

import weka.core.Instances;

public interface FoldGeneratorInterface {
	public Instances generate_learningcurve() throws Exception;
	public Instances generate() throws Exception;
}
