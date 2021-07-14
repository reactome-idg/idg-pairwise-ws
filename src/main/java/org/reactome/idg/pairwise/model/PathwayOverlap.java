package org.reactome.idg.pairwise.model;

public class PathwayOverlap {

	private String stId;
	private Double hypergeometricScore;
	private Integer numberOfSharedGenes;
	
	public PathwayOverlap() {/*Nothing Here*/}
	
	public PathwayOverlap(String stId, Double hypergeometricScore, Integer numberOfSharedGenes) {
		super();
		this.stId = stId;
		this.hypergeometricScore = hypergeometricScore;
		this.numberOfSharedGenes = numberOfSharedGenes;
	}

	public String getStId() {
		return stId;
	}

	public void setStId(String stId) {
		this.stId = stId;
	}

	public Double getHypergeometricScore() {
		return hypergeometricScore;
	}

	public void setHypergeometricScore(Double hypergeometricScore) {
		this.hypergeometricScore = hypergeometricScore;
	}

	public Integer getNumberOfSharedGenes() {
		return numberOfSharedGenes;
	}

	public void setNumberOfSharedGenes(Integer numberOfCommonGenes) {
		this.numberOfSharedGenes = numberOfCommonGenes;
	}
}
