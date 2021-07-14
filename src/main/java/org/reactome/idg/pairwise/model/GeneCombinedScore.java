package org.reactome.idg.pairwise.model;

import java.util.HashMap;
import java.util.Map;

public class GeneCombinedScore {

	String gene;
	Map<String, Double> interactorToScore = new HashMap<>();
	
	public GeneCombinedScore(String gene) {
		this.gene = gene;
	}

	public String getGene() {
		return gene;
	}

	public void setGene(String gene) {
		this.gene = gene;
	}

	public Map<String, Double> getInteractorToScore() {
		return interactorToScore;
	}

	public void setInteractorToScore(Map<String, Double> interactorToScore) {
		this.interactorToScore = interactorToScore;
	}
	
	public void addInteractorToScore(String geneIndex, Double prd) {
		this.interactorToScore.put(geneIndex, prd);
	}
}
