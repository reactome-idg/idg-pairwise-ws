package org.reactome.idg.pairwise.model.pathway;

import java.util.ArrayList;
import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

//TODO: annotation to remove children from what goes to front end
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(value = { "parents" })
public class GraphPathway {

	private String stId;
	private String name;
	private String species;
	private String type;
	private Collection<GraphPathway> parents = new ArrayList<>();
	private Collection<GraphPathway> children = new ArrayList<>();
	
	public GraphPathway(String stId, String name, String species, String type) {
		this.stId = stId;
		this.name = name;
		this.species = species;
		this.type = type;
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

	public Collection<GraphPathway> getParents() {
		return parents;
	}

	public void addParents(Collection<GraphPathway> parents) {
		this.parents.addAll(parents);
	}
	
	public void addParent(GraphPathway parent) {
		parents.add(parent);
	}
	
	public void setParents(Collection<GraphPathway> parents) {
		this.parents = parents;
	}

	public Collection<GraphPathway> getChildren() {
		return children;
	}

	public void addChildren(Collection<GraphPathway> children) {
		this.children.addAll(children);
	}
	
	public void addChild(GraphPathway child) {
		children.add(child);
	}
	
	public void setChildren(Collection<GraphPathway> children) {
		this.children = children;
	}
	
	public boolean containsChild(String stId) {
		for(GraphPathway child : children) {
			if(child.getStId().equals(stId))
				return true;
		}
		return false;
	}
}
