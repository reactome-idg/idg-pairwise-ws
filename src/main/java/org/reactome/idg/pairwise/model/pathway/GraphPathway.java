package org.reactome.idg.pairwise.model.pathway;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * 
 * @author brunsont
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(value = { "parents", "diagram" })
public class GraphPathway {

	private String stId;
	private String name;
	private String species;
	private String type;
	private boolean diagram;
	private List<GraphPathway> children;
	private List<GraphPathway> parents;
	private String topPathway;
	
	public GraphPathway() {}
	
	public GraphPathway(String stId, String name, String species, String type, boolean diagram, List<GraphPathway> children) {
		this.stId = stId;
		this.name = name;
		this.species = species;
		this.type = type;
		this.diagram = diagram;
		this.children = children;
	}

	public GraphPathway(String stId, String name, String species, String type) {
		super();
		this.stId = stId;
		this.name = name;
		this.species = species;
		this.type = type;
	}

	public GraphPathway(String stId, String name, String species, String type, boolean diagram,
			List<GraphPathway> children, List<GraphPathway> parents) {
		super();
		this.stId = stId;
		this.name = name;
		this.species = species;
		this.type = type;
		this.diagram = diagram;
		this.children = children;
		this.parents = parents;
	}

	public String getStId() {
		return stId;
	}
	public void setStId(String stId) {
		this.stId = stId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getSpecies() {
		return species;
	}
	public void setSpecies(String species) {
		this.species = species;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public boolean isDiagram() {
		return diagram;
	}

	public void setDiagram(boolean diagram) {
		this.diagram = diagram;
	}

	public List<GraphPathway> getChildren() {
		return children;
	}
	public void setChildren(List<GraphPathway> children) {
		this.children = children;
	}
	
	public void addChild(GraphPathway child) {
		if(children == null)children = new ArrayList<>();
		this.children.add(child);
	}
	
	public List<GraphPathway> getParents() {
		return parents;
	}
	public void setParents(List<GraphPathway> parents) {
		this.parents = parents;
	}
	public boolean addParent(GraphPathway parent) {
		if(parents == null) parents = new ArrayList<>();
		if(!parents.contains(parent)) {
			parents.add(parent);
			return true;
		}
		return false;
	}
	public String getTopPathway() {
		return topPathway;
	}

	public void setTopPathway(String topPathway) {
		this.topPathway = topPathway;
	}
}
