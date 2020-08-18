package org.reactome.idg.pairwise.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * 
 * @author brunsont
 *
 */
@JsonInclude(Include.NON_NULL)
public class Pathway {

	private String stId;
	private Integer index;
	private String name;
	private Double fdr;
	private Double pVal;
	private boolean bottomLevel;
	
	public Pathway(String stId, String name, Double fdr, Double pVal, boolean bottomLevel) {
		this.stId = stId;
		this.name = name;
		this.fdr = fdr;
		this.pVal = pVal;
		this.bottomLevel = bottomLevel;
	}

	public Pathway(String stId, String name, boolean bottomLevel) {
		this.stId = stId;
		this.name = name;
		this.bottomLevel = bottomLevel;
	}

	public Integer getIndex() {
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

	public Double getFdr() {
		return fdr;
	}

	public void setFdr(Double fdr) {
		this.fdr = fdr;
	}

	public Double getpVal() {
		return pVal;
	}

	public void setpVal(Double pVal) {
		this.pVal = pVal;
	}

	public boolean isBottomLevel() {
		return bottomLevel;
	}

	public void setBottomLevel(boolean bottomLevel) {
		this.bottomLevel = bottomLevel;
	}	
	
}
