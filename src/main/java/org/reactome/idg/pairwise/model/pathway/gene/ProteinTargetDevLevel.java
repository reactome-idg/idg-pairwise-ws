package org.reactome.idg.pairwise.model.pathway.gene;
/**
 * 
 * @author brunsont
 *
 */
public class ProteinTargetDevLevel {

	private String uniprot;
	private String sym;
	private String targetDevLevel;
	public ProteinTargetDevLevel() {
	}
	public ProteinTargetDevLevel(String uniprot, String sym, String targetDevLevel) {
		this.uniprot = uniprot;
		this.sym = sym;
		this.targetDevLevel = targetDevLevel;
	}
	public String getUniprot() {
		return uniprot;
	}
	public void setUniprot(String uniprot) {
		this.uniprot = uniprot;
	}
	public String getSym() {
		return sym;
	}
	public void setSym(String sym) {
		this.sym = sym;
	}
	public String getTargetDevLevel() {
		return targetDevLevel;
	}
	public void setTargetDevLevel(String targetDevLevel) {
		this.targetDevLevel = targetDevLevel;
	}
	
	
	
}
