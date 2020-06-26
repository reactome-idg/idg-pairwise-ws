package org.reactome.idg.pairwise.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class Pathway {

	private String stId;
	private Integer index;
	private String name;
	private String fdr;
	private Double pVal;
	
	public Pathway(String stId, String name, String fdr, Double pVal) {
		this.stId = stId;
		this.name = name;
		this.fdr = fdr;
		this.pVal = pVal;
	}
	
	public Pathway(Integer index, String fdr, Double pVal) {
		this.index = index;
		this.fdr = fdr;
		this.pVal = pVal;
	}

	public Pathway(String stId, String name) {
		this.stId = stId;
		this.name = name;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(Integer index) {
		this.index = index;
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

	public String getFdr() {
		return fdr;
	}

	public void setFdr(String fdr) {
		this.fdr = fdr;
	}

	public Double getpVal() {
		return pVal;
	}

	public void setpVal(Double pVal) {
		this.pVal = pVal;
	}	
}
