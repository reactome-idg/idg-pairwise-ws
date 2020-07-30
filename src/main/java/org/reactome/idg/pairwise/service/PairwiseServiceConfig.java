package org.reactome.idg.pairwise.service;

import org.reactome.annotate.PathwayBasedAnnotator;

/**
 * 
 * @author brunsont
 *
 */
public class PairwiseServiceConfig {

	private String coreWSURL;
	private PathwayBasedAnnotator annotator;
	
	public PairwiseServiceConfig() {
		
	}

	public String getCoreWSURL() {
		return coreWSURL;
	}

	public void setCoreWSURL(String coreWSURL) {
		this.coreWSURL = coreWSURL;
	}

	public PathwayBasedAnnotator getAnnotator() {
		return annotator;
	}

	public void setAnnotator(PathwayBasedAnnotator annotator) {
		this.annotator = annotator;
	}
}
