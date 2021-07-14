package org.reactome.idg.pairwise.model;

import java.util.List;

/**
 * 
 * @author brunsont
 *
 */
public class PairwiseRelRequest {

	private List<String> genes;
	private List<String> dataDescs;
	
	public PairwiseRelRequest() {/*Nothin Here*/}

	public PairwiseRelRequest(List<String> genes, List<String> dataDescs) {
		this.genes = genes;
		this.dataDescs = dataDescs;
	}

	public List<String> getGenes() {
		return genes;
	}

	public void setGenes(List<String> genes) {
		this.genes = genes;
	}

	public List<String> getDataDescs() {
		return dataDescs;
	}

	public void setDataDescs(List<String> dataDescs) {
		this.dataDescs = dataDescs;
	}
}
