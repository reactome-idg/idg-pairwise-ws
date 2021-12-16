package org.reactome.idg.pairwise.model.network;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * 
 * @author brunsont
 *
 */
@JsonInclude(Include.NON_NULL)
public class NodeData extends Data {

	private Double weightedTDL;
	private Double fdr;
	private Double pVal;
	private String weightedTDLColorHex;
	private Integer geneNumber;
	
	public NodeData() {/*Nothing Here*/}
	
	

	public NodeData(String id, String name, Double weightedTDL, Double fdr, Double pVal, String weightedTDLColorHex) {
		super(id, name);
		this.weightedTDL = weightedTDL;
		this.fdr = fdr;
		this.pVal = pVal;
		this.weightedTDLColorHex = weightedTDLColorHex;
	}

	public Integer getGeneNumber() {
		return geneNumber;
	}

	public void setGeneNumber(Integer geneNumber) {
		this.geneNumber = geneNumber;
	}

	public Double getWeightedTDL() {
		return weightedTDL;
	}

	public void setWeightedTDL(Double weightedTDL) {
		this.weightedTDL = weightedTDL;
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

	public String getWeightedTDLColorHex() {
		return weightedTDLColorHex;
	}

	public void setWeightedTDLColorHex(String weightedTDLColorHex) {
		this.weightedTDLColorHex = weightedTDLColorHex;
	}
}
