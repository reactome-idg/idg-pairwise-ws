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
	private List<String> dataDescs;
	
	public PEsForInteractorAndDataDescsWrapper() {
		
	}
	
	public PEsForInteractorAndDataDescsWrapper(String term, Long dbId, List<String> dataDescs) {
		super();
		this.term = term;
		this.dbId = dbId;
		this.dataDescs = dataDescs;
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

	public List<String> getDataDescs() {
		return dataDescs;
	}

	public void setDataDescs(List<String> dataDescs) {
		this.dataDescs = dataDescs;
	}
	
	
}
