package org.openml.webapplication.evaluate;

import weka.classifiers.CostMatrix;
import weka.classifiers.Evaluation;
import weka.core.Instances;

public class OpenMLEvaluation extends Evaluation {
	
	private static final long serialVersionUID = 1L;
	
	private final Instances m_Instances;
	
	public OpenMLEvaluation(Instances data) throws Exception {
		super(data);
		m_Instances = data;
	}
	public OpenMLEvaluation(Instances data, CostMatrix costMatrix) throws Exception {
		super(data, costMatrix);
		m_Instances = data;
	}
	
	public double unweightedRecall() {
		double summedRecall = 0.0;
		for (int i = 0; i < m_Instances.numClasses(); ++i) {
			summedRecall += m_delegate.recall(i);
		}
		return summedRecall / m_Instances.numClasses();
	}
}
