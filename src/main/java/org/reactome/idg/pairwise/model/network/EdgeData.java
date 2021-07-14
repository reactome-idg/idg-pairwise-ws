package org.reactome.idg.pairwise.model.network;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * 
 * @author brunsont
 *
 */
@JsonInclude(Include.NON_NULL)
public class EdgeData extends Data{

	private Integer numSharedGenes;
	private Double hypergeometricScore;
	private String source;
	private String target;
	
	public EdgeData() {/*Nothing Here*/}

	public EdgeData(String id, Integer numSharedGenes, Double hypergeometricScore, String source, String target) {
		super(id, null);
		this.numSharedGenes = numSharedGenes;
		this.hypergeometricScore = hypergeometricScore;
		this.source = source;
		this.target = target;
	}

	public Integer getNumSharedGenes() {
		return numSharedGenes;
	}

	public void setNumSharedGenes(Integer numSharedGenes) {
		this.numSharedGenes = numSharedGenes;
	}

	public Double getHypergeometricScore() {
		return hypergeometricScore;
	}

	public void setHypergeometricScore(Double hypergeometricScore) {
		this.hypergeometricScore = hypergeometricScore;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}
	
	
	
}
