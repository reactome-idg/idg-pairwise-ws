package org.reactome.idg.pairwise.model.pathway;

import java.util.List;

public class HierarchyResponseWrapper {

	private String gene;
	private List<String> stIds;
	private List<GraphPathway> hierarchy;
	
	public HierarchyResponseWrapper(String gene, List<String> stIds, List<GraphPathway> hierarchy) {
		this.gene = gene;
		this.stIds = stIds;
		this.hierarchy = hierarchy;
	}

	public String getGene() {
		return gene;
	}

	public void setGene(String gene) {
		this.gene = gene;
	}

	public List<String> getStIds() {
		return stIds;
	}

	public void setStIds(List<String> stIds) {
		this.stIds = stIds;
	}

	public List<GraphPathway> getHierarchy() {
		return hierarchy;
	}

	public void setHierarchy(List<GraphPathway> hierarchy) {
		this.hierarchy = hierarchy;
	}
}
