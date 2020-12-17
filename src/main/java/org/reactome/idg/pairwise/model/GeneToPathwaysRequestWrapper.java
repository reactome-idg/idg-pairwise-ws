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
	private List<Integer> dataDescKeys;
	private Double prd;
	public GeneToPathwaysRequestWrapper() {
		
	}
	public GeneToPathwaysRequestWrapper(String term, List<Integer> dataDescKeys) {
		super();
		this.term = term;
		this.dataDescKeys = dataDescKeys;
	}
	
	public GeneToPathwaysRequestWrapper(String term, List<Integer> dataDescKeys, Double prd) {
		super();
		this.term = term;
		this.dataDescKeys = dataDescKeys;
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
	public List<Integer> getDataDescKeys() {
		return dataDescKeys;
	}
	public void setDataDescKeys(List<Integer> dataDescs) {
		this.dataDescKeys = dataDescs;
	}
	public Double getPrd() {
		return prd;
	}
	public void setPrd(Double prd) {
		this.prd = prd;
	}
}
