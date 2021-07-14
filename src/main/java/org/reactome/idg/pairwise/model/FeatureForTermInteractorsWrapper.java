package org.reactome.idg.pairwise.model;

import java.util.List;

/**
 * 
 * @author brunsont
 *
 */
public class FeatureForTermInteractorsWrapper {

	private String term;
	private List<String> interactors;
	
	public FeatureForTermInteractorsWrapper() {
		super();
	}
	
	public FeatureForTermInteractorsWrapper(String term, List<String> interactors) {
		super();
		this.term = term;
		this.interactors = interactors;
	}
	
	public String getTerm() {
		return term;
	}
	
	public void setTerm(String term) {
		this.term = term;
	}
	
	public List<String> getInteractors() {
		return interactors;
	}
	
	public void setInteractors(List<String> interactors) {
		this.interactors = interactors;
	}
	
}
