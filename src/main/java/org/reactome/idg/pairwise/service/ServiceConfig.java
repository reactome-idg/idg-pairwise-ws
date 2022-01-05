package org.reactome.idg.pairwise.service;

/**
 * 
 * @author brunsont
 *
 */
public class ServiceConfig {

	private String coreWSURL;
	private String eventHierarchyUrl;
	
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
	
}
