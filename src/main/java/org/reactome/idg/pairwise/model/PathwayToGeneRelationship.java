package org.reactome.idg.pairwise.model;

import java.util.List;

public class PathwayToGeneRelationship {

	private String pathwayStId;
	private List<String> genes;
	
	public PathwayToGeneRelationship() {
	}

	public String getPathwayStId() {
		return pathwayStId;
	}

	public void setPathwayStId(String pathwayStId) {
		this.pathwayStId = pathwayStId;
	}

	public List<String> getGenes() {
		return genes;
	}

	public void setGenes(List<String> genes) {
		this.genes = genes;
	}
}
