package org.reactome.idg.pairwise.service;

/**
 * 
 * @author brunsont
 *
 */
public class ServiceConfig {

	private String coreWSURL;
	private String eventHierarchyUrl;
	private String geneToPathwayStIdFile;
	private String uniProt2ReactomeFile;
	private String uniProt2ReactomeAllLevelsFile;
	
	public ServiceConfig() {
		
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

	public String getUniProt2ReactomeFile() {
		return uniProt2ReactomeFile;
	}

	public void setUniProt2ReactomeFile(String uniProt2ReactomeFile) {
		this.uniProt2ReactomeFile = uniProt2ReactomeFile;
	}

	public String getUniProt2ReactomeAllLevelsFile() {
		return uniProt2ReactomeAllLevelsFile;
	}

	public void setUniProt2ReactomeAllLevelsFile(String uniProt2ReactomeAllLevelsFile) {
		this.uniProt2ReactomeAllLevelsFile = uniProt2ReactomeAllLevelsFile;
	}
	
}
