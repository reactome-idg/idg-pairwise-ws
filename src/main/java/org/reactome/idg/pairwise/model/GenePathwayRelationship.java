package org.reactome.idg.pairwise.model;

import java.util.List;

public class GenePathwayRelationship {

	private String key;
	private List<Integer> pathways;
	private List<Integer> secondaryPathways;
	private List<Integer> genes;
	
	public GenePathwayRelationship() {}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public List<Integer> getPathways() {
		return pathways;
	}

	public void setPathways(List<Integer> pathways) {
		this.pathways = pathways;
	}

	public List<Integer> getSecondaryPathways() {
		return secondaryPathways;
	}

	public void setSecondaryPathways(List<Integer> secondaryPathways) {
		this.secondaryPathways = secondaryPathways;
	}

	public List<Integer> getGenes() {
		return genes;
	}

	public void setGenes(List<Integer> genes) {
		this.genes = genes;
	}	
}
