package org.reactome.idg.pairwise.model;

import java.util.List;

public class GeneToPathwayRelationship {

	private String gene;
	private List<Pathway> pathways;
	private List<Pathway> secondaryPathways;
	
	public GeneToPathwayRelationship() {
	}

	public String getGene() {
		return gene;
	}

	public void setGene(String gene) {
		this.gene = gene;
	}

	public List<Pathway> getPathways() {
		return pathways;
	}

	public void setPathways(List<Pathway> pathways) {
		this.pathways = pathways;
	}

	public List<Pathway> getSecondaryPathways() {
		return secondaryPathways;
	}

	public void setSecondaryPathways(List<Pathway> secondaryPathways) {
		this.secondaryPathways = secondaryPathways;
	}
}
