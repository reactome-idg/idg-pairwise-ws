package org.reactome.idg.pairwise.model;

public class Pathway {

	private String stId;
	private String name;
	
	public Pathway(String stId, String name) {
		this.stId = stId;
		this.name = name;
	}

	public String getStId() {
		return stId;
	}

	public void setStId(String stId) {
		this.stId = stId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}	
}
