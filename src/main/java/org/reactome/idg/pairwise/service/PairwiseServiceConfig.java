package org.reactome.idg.pairwise.service;

/**
 * 
 * @author brunsont
 *
 */
public class PairwiseServiceConfig {

	private String coreWSURL;
	private String eventHierarchyUrl;
	private String geneToPathwayStIdFile;
	
	public PairwiseServiceConfig() {
		
	}

	public String getCoreWSURL() {
		return coreWSURL;
	}

	public void setCoreWSURL(String coreWSURL) {
		this.coreWSURL = coreWSURL;
	}

	public String getEventHierarchyUrl() {
		return eventHierarchyUrl;
	}

	public void setEventHierarchyUrl(String eventHierarchyUrl) {
		this.eventHierarchyUrl = eventHierarchyUrl;
	}

	public String getGeneToPathwayStIdFile() {
		return this.geneToPathwayStIdFile;
	}

	public void setGeneToPathwayStIdFile(String geneToPathwayStIdFile) {
		this.geneToPathwayStIdFile = geneToPathwayStIdFile;
	}
	
}
