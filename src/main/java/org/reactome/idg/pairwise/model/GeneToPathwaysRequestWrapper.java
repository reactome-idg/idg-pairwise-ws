package org.reactome.idg.pairwise.model;

import java.util.List;

/**
 * 
 * @author brunsont
 *
 */
public class GeneToPathwaysRequestWrapper {

	private String gene;
	private List<String> dataDescs;
	
	public GeneToPathwaysRequestWrapper() {
		
	}
	public GeneToPathwaysRequestWrapper(String gene, List<String> dataDescs) {
		super();
		this.gene = gene;
		this.dataDescs = dataDescs;
	}
	public String getGene() {
		return gene;
	}
	public void setGene(String gene) {
		this.gene = gene;
	}
	public List<String> getDataDescs() {
		return dataDescs;
	}
	public void setDataDescs(List<String> dataDescs) {
		this.dataDescs = dataDescs;
	}
}
