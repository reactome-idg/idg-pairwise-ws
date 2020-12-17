package org.reactome.idg.pairwise.model;

import java.util.List;

/**
 * 
 * @author brunsont
 *
 */
public class PEsForInteractorAndDataDescsWrapper {

	private String term;
	private Long dbId;
	private List<Integer> dataDescKeys;
	private Double prd;
	
	public PEsForInteractorAndDataDescsWrapper() {
		
	}
	
	public PEsForInteractorAndDataDescsWrapper(String term, Long dbId, List<Integer> dataDescs) {
		super();
		this.term = term;
		this.dbId = dbId;
		this.dataDescKeys = dataDescs;
	}
	
	public PEsForInteractorAndDataDescsWrapper(String term, Long dbId, Double prd) {
		super();
		this.term = term;
		this.dbId = dbId;
		this.prd = prd;
	}

	public String getTerm() {
		return term;
	}

	public void setTerm(String term) {
		this.term = term;
	}

	public Long getDbId() {
		return dbId;
	}

	public void setDbId(Long dbId) {
		this.dbId = dbId;
	}

	public List<Integer> getDataDescKeys() {
		return dataDescKeys;
	}

	public void setDataDescs(List<Integer> dataDescKeys) {
		this.dataDescKeys = dataDescKeys;
	}

	public Double getPrd() {
		return prd;
	}

	public void setPrd(Double prd) {
		this.prd = prd;
	}
}
