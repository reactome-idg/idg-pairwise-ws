package org.reactome.idg.pairwise.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	private List<String> genes;
	private Double weightedTDL;
	private Map<String, PathwayOverlap> stIdToHypergeometricScoreMap;
	
	public Pathway() {
	}
	public Pathway(String stId) {
		this.stId = stId;
	}
	
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
	
	public List<String> getGenes(){
		return this.genes;
	}
	public void addGene(String gene) {
		if(this.genes == null) genes = new ArrayList<>();
		if(!this.genes.contains(gene)) this.genes.add(gene);
	}
	public void setGenes(List<String> genes) {
		this.genes = genes;
	}

	public Double getWeightedTDL() {
		return weightedTDL;
	}

	public void setWeightedTDL(Double weightedTDL) {
		this.weightedTDL = weightedTDL;
	}

	public Collection<PathwayOverlap> getStIdToHypergeometricScoreMap() {
		return stIdToHypergeometricScoreMap.values();
	}

	public void setStIdToHypergeometricScoreMap(Map<String, PathwayOverlap> stIdTohypergeometricScoreMap) {
		this.stIdToHypergeometricScoreMap = stIdTohypergeometricScoreMap;
	}
	
	public void addStIdToHypergeometricScoreRelationship(String relStId, PathwayOverlap p) {
		if(this.stIdToHypergeometricScoreMap == null) this.stIdToHypergeometricScoreMap = new HashMap<>();
		if(!this.stIdToHypergeometricScoreMap.containsKey(relStId))
			this.stIdToHypergeometricScoreMap.put(relStId, p);
	}
}
