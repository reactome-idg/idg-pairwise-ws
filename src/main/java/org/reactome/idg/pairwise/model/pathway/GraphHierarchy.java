package org.reactome.idg.pairwise.model.pathway;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;
import org.reactome.idg.pairwise.web.errors.InternalServerError;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GraphHierarchy {

	private Map<String, GraphPathway> stIdToPathway;
	private Collection<GraphPathway> topLevelPathways;
	
	public GraphHierarchy(String jsonString) throws JsonProcessingException, IOException {
		initGraph(jsonString);
	}
	
	private void initGraph(String jsonString) throws JsonProcessingException, IOException {
		stIdToPathway = new HashMap<>();
		topLevelPathways = new ArrayList<>();
		ObjectMapper mapper = new ObjectMapper();
		
		JsonNode response = mapper.readTree(jsonString);
		if(!response.isArray()) throw new InternalServerError("Error Building/Traversing Event Hierarchy");
		buildGraph(response, null);
	}

	private Set<GraphPathway> buildGraph(JsonNode nodes, GraphPathway parent) {
		Set<GraphPathway> pathways = new HashSet<>();
		for(JsonNode node : nodes) {
			//want to ignore anything that isnt a pathway or top level pathway
			String type = node.get("type").asText();
			if(!type.equals("Pathway") && !type.equals("TopLevelPathway")) continue;
			
			//get pathway if it already exists from another part of the hierarchy
			GraphPathway pathway = stIdToPathway.get(node.get("stId").asText());
			
			//make pathway if doesnt already exist
			if(pathway == null) {
				stIdToPathway.put(node.get("stId").asText(), 
						pathway = new GraphPathway(node.get("stId").asText(),
										   		   node.get("name").asText(),
										   		   node.get("species").asText(),
										   		   node.get("type").asText()));
				if(pathway.getType().equals("TopLevelPathway")) topLevelPathways.add(pathway);
			}
			
			//parent will only be null for top level set of pathways
			if(parent != null)
				pathway.addParent(parent);
			//recursion down the hierarchy
			if(node.get("children") != null)
				pathway.addChildren(buildGraph(node.get("children"), pathway));
			
			pathways.add(pathway);
		}
		
		return pathways;
	}

	/**
	 * Return GraphPathway for passed in stId
	 * @param stId
	 */
	public GraphPathway getPathway(String stId) {
		return stIdToPathway.get(stId);
	}
	
	public List<GraphPathway> getBranches(List<String> stIds){
		Map<GraphPathway, GraphPathway> visited = new HashMap<>();
		
		
		stIds.forEach(stId -> {
			traverse(visited, stIdToPathway.get(stId), null);
		});
		
		List<GraphPathway> rtn = visited.values().stream().filter(pw -> pw.getType().equals("TopLevelPathway")).collect(Collectors.toList());
		rtn.sort((x, y) -> x.getName().compareTo(y.getName()));
		return rtn;
	}
	
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
		
		if(existed) {
			return;
		}
		
		for(GraphPathway p : oldParent.getParents()) {
			traverse(oldToNew, p, newParent);
		}
	}
}
