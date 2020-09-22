package org.reactome.idg.pairwise.model.pathway;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Test;
import org.reactome.idg.pairwise.web.errors.InternalServerError;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GraphHierarchy {

	private Map<String, GraphPathway> stIdToPathway;
	
	public GraphHierarchy(String jsonString) throws JsonProcessingException, IOException {
		initGraph(jsonString);
	}
	
	private void initGraph(String jsonString) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		
		List<GraphPathway> hierarchy = Arrays.asList(mapper.readValue(jsonString, GraphPathway[].class));
		stIdToPathway = new HashMap<>();
		for(GraphPathway pw : hierarchy) {
			stIdToPathway.put(pw.getStId(), pw);
			fillParents(pw, pw.getChildren());
		}
	}

	/**
	 * Used to give GraphPathways a parent relationship and populate stIdToPathway map.
	 * @param parent
	 * @param children
	 */
	private void fillParents(GraphPathway parent, List<GraphPathway> children) {
		List<GraphPathway> toRemove = new ArrayList<>();
		for(GraphPathway child : children) {
			if(!child.getType().equals("Pathway")) {
				toRemove.add(child);
				continue;
			}
			if(!stIdToPathway.containsKey(child.getStId()))
				stIdToPathway.put(child.getStId(), child);
			child.addParent(parent);
			
			if(child.getChildren() != null)
				fillParents(child, child.getChildren());
		}
		parent.getChildren().removeAll(toRemove);
	}

	/**
	 * Return GraphPathway for passed in stId
	 * @param stId
	 */
	public GraphPathway getPathway(String stId) {
		return stIdToPathway.get(stId);
	}
	
	/**
	 * For a list of stIds corresponding to pathways, slice branches for return to controller
	 * @param stIds
	 * @return
	 */
	public List<GraphPathway> getBranches(List<String> stIds){
		Map<GraphPathway, GraphPathway> visited = new HashMap<>();
		
		
		stIds.forEach(stId -> {
			traverse(visited, stIdToPathway.get(stId), null);
		});
		
		List<GraphPathway> rtn = visited.values().stream().filter(pw -> pw.getType().equals("TopLevelPathway")).collect(Collectors.toList());
		rtn.sort((x, y) -> x.getName().compareTo(y.getName()));
		return rtn;
	}
	
	/**
	 * Recursive method to fill children of original passed in GraphPathway in a hierarchical manner
	 * oldToNew maps parents to their children for lookup and to avoid duplication
	 * If a protein existed already on the map, dont want to itterate over parents again
	 * Results in values of oldToNew being the return value wanted by the controller
	 * @param oldToNew
	 * @param oldParent
	 * @param newChild
	 */
	private void traverse(Map<GraphPathway, GraphPathway> oldToNew, GraphPathway oldParent, GraphPathway newChild) {
		boolean existed = true;
		GraphPathway newParent = oldToNew.get(oldParent);
		if(newParent == null) {
			existed = false;
			newParent = new GraphPathway(oldParent.getStId(), oldParent.getName(), oldParent.getSpecies(), oldParent.getType());
			oldToNew.put(oldParent, newParent);
		}
				
		if(newChild != null) {
			newParent.addChild(newChild);
			newChild.addParent(newParent);
		}
		
		if(existed || oldParent.getParents() == null) {
			return;
		}
		
		for(GraphPathway p : oldParent.getParents()) {
			traverse(oldToNew, p, newParent);
		}
	}
}
