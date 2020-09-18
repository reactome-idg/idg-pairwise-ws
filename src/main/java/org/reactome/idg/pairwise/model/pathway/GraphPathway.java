package org.reactome.idg.pairwise.model.pathway;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(value = { "parents" })
//TODO: add idRef annotation for stId
public class GraphPathway {

	private String stId;
	private String name;
	private String species;
	private String type;
	private Set<GraphPathway> parents = new HashSet<>();
	private Set<GraphPathway> children = new HashSet<>();
	
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

	public Set<GraphPathway> getParents() {
		return parents;
	}

	public void addParents(Set<GraphPathway> parents) {
		this.parents.addAll(parents);
	}
	
	public void addParent(GraphPathway parent) {
		parents.add(parent);
	}
	
	public void setParents(Set<GraphPathway> parents) {
		this.parents = parents;
	}

	public Set<GraphPathway> getChildren() {
		return children;
	}

	public void addChildren(Set<GraphPathway> children) {
		this.children.addAll(children);
	}
	
	public void addChild(GraphPathway child) {
		children.add(child);
	}
	
	public void setChildren(Set<GraphPathway> children) {
		this.children = children;
	}
}
