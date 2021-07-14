package org.reactome.idg.pairwise.model;

import java.util.List;

/**
 * 
 * @author brunsont
 * Wrapper to return list of diagram objects for flagging, and a list of interactors for the requested Data descriptions
 */
public class PEsForInteractorResponse {
	
	private List<Long> peIds;
	private List<String> interactors;
	private List<String> dataDescs;

	public PEsForInteractorResponse() {/*Nothing Here*/}
	
	public PEsForInteractorResponse(List<Long> peIds, List<String> interactors) {
		this.peIds = peIds;
		this.interactors = interactors;
	}

	public PEsForInteractorResponse(List<Long> peIds, List<String> interactors, List<String> dataDescs) {
		super();
		this.peIds = peIds;
		this.interactors = interactors;
		this.dataDescs = dataDescs;
	}

	public List<Long> getPeIds() {
		return peIds;
	}

	public void setPeIds(List<Long> peIds) {
		this.peIds = peIds;
	}

	public List<String> getInteractors() {
		return interactors;
	}

	public void setInteractors(List<String> interactors) {
		this.interactors = interactors;
	}

	public List<String> getDataDescs() {
		return dataDescs;
	}

	public void setDataDescs(List<String> dataDescs) {
		this.dataDescs = dataDescs;
	}
}
