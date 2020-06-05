package org.reactome.idg.pairwise.model;

import java.util.List;

public class GeneToPathwayRelationship {

	String gene;
	List<String> pathways;
	List<String> secondaryPathways;
	
	public GeneToPathwayRelationship() {
	}

	public String getGene() {
		return gene;
	}

	public void setGene(String gene) {
		this.gene = gene;
	}

	public List<String> getPathways() {
		return pathways;
	}

	public void setPathways(List<String> pathways) {
		this.pathways = pathways;
	}

	public List<String> getSecondaryPathways() {
		return secondaryPathways;
	}

	public void setSecondaryPathways(List<String> secondaryPathways) {
		this.secondaryPathways = secondaryPathways;
	}
}
