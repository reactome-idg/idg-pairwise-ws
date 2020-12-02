package org.reactome.idg.pairwise.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * 
 * @author brunsont
 *
 */
@JsonInclude(Include.NON_NULL)
public class GeneToPathwaysRequestWrapper {

	private String term;
	private List<String> dataDescs;
	private Double prd;
	public GeneToPathwaysRequestWrapper() {
		
	}
	public GeneToPathwaysRequestWrapper(String term, List<String> dataDescs) {
		super();
		this.term = term;
		this.dataDescs = dataDescs;
	}
	
	public GeneToPathwaysRequestWrapper(String term, List<String> dataDescs, Double prd) {
		super();
		this.term = term;
		this.dataDescs = dataDescs;
		this.prd = prd;
	}
	public GeneToPathwaysRequestWrapper(String term, Double prd) {
		super();
		this.term = term;
		this.prd = prd;
	}
	public String getTerm() {
		return term;
	}
	public void setTerm(String term) {
		this.term = term;
	}
	public List<String> getDataDescs() {
		return dataDescs;
	}
	public void setDataDescs(List<String> dataDescs) {
		this.dataDescs = dataDescs;
	}
	public Double getPrd() {
		return prd;
	}
	public void setPrd(Double prd) {
		this.prd = prd;
	}
}
