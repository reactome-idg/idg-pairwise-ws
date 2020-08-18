package org.reactome.idg.pairwise.model;

import java.util.List;

/**
 * 
 * @author brunsont
 *
 */
public class PEsForInteractorAndDataDescsWrapper {

	private String gene;
	private Long dbId;
	private List<String> dataDescs;
	
	public PEsForInteractorAndDataDescsWrapper() {
		
	}
	
	public PEsForInteractorAndDataDescsWrapper(String gene, Long dbId, List<String> dataDescs) {
		super();
		this.gene = gene;
		this.dbId = dbId;
		this.dataDescs = dataDescs;
	}

	public String getGene() {
		return gene;
	}

	public void setGene(String gene) {
		this.gene = gene;
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
