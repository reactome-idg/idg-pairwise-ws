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
			//This is where top level pathways get added to stIdToPathway map
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
		//Everything that isnt a Pathway needs to be removed
		//Want hierarchy of only pathways
		//Dont need to check on TopLevelPathways because they never see this method
		List<GraphPathway> toRemove = new ArrayList<>();
		for(GraphPathway child : children) {
			//Continue if child is not a Pathway
			if(!child.getType().equals("Pathway")) {
				toRemove.add(child);
				continue;
			}
			//Place each Pathway on map of stId to the GraphPathway Object
			if(!stIdToPathway.containsKey(child.getStId()))
				stIdToPathway.put(child.getStId(), child);
			//add passed in parent to each child
			child.addParent(parent);
			
			//If children exist, recursively call this method to the bottom of the hierarchy
			if(child.getChildren() != null)
				fillParents(child, child.getChildren());
		}
		//remove all non-Pathway GraphPathwayObjects from parent
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
		
		
		//traverse and make parent-to-child relationships for each stId from top down
		stIds.forEach(stId -> {
			traverse(visited, stIdToPathway.get(stId), null);
		});
		
		//values of visited are the newly created GraphPathway objects
		//filtering for TopLevelPathway returns the minimal number of GraphPathways with children
		//leading down to the passed in stIds above
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
		//try to get parent off map to avoid generating multiple instances
		GraphPathway newParent = oldToNew.get(oldParent);
		if(newParent == null) { //create if not on map
			existed = false;
			newParent = new GraphPathway(oldParent.getStId(), oldParent.getName(), oldParent.getSpecies(), oldParent.getType());
			oldToNew.put(oldParent, newParent);
		}
		
		//add passed in new child to the parent created above
		//add new parent as a parent of new child passed in
		if(newChild != null) {
			newParent.addChild(newChild);
			newChild.addParent(newParent);
		}
		
		//if already existed, or doesnt have parents, dont need to continue to work up parents
		if(existed || oldParent.getParents() == null) {
			return;
		}
		
		//loop over oldParents' parents moving up hierarchy
		for(GraphPathway p : oldParent.getParents()) {
			traverse(oldToNew, p, newParent);
		}
	}
}
