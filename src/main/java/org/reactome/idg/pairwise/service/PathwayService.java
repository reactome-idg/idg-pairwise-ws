package org.reactome.idg.pairwise.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.net.URL;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.util.GKApplicationUtilities;
import org.reactome.data.ReactomeAnalyzer;
import org.reactome.idg.bn.PathwayImpactAnalyzer;
import org.reactome.idg.pairwise.model.Pathway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.reactome.idg.pairwise.model.pathway.GraphPathway;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * 
 * The new implementation uses the relational database to build the mapping between pathways and genes so
 * that there is no need to handle static files for each release. The actual implementation is ported from
 * class PathawyGeneSetGenerator in the FI network build project: https://github.com/reactome/fi_network_build/blob/master/src/org/reactome/fi/PathwayGeneSetGenerator.java.
 * @author brunsont, guanming
 *
 */
@Service
public class PathwayService {
	private static final Logger logger = LoggerFactory.getLogger(PathwayService.class);

	@Autowired
	private MySQLAdaptor mysqlDBA;

	//attempt at removing need for pathways and PATHWAY_INDEX collections
	private Map<String, Set<Pathway>> geneToPathwayList;
	private Map<String, Pathway> pathwayStIdToPathway;
	private Map<String, Set<String>> geneToPathwayStId;
	
    @Autowired
    private ServiceConfig config;
	
    // Want to cache the pathway list assuming it will not change
    private List<GraphPathway> pathways;

	public PathwayService() {/*Nothing Here*/}

	/**
	 * This implementation collects the mapping between genes and pathways directly from a released
	 * MySQL database.
	 */
	@SuppressWarnings("unchecked")
	private void cachePathways(Map<String, String> uniprotToGene) {
		geneToPathwayList = new HashMap<>();
		pathwayStIdToPathway = new HashMap<>();

		try {
			// Use pathways having laid-out ELV as the base pathways for easy comparison and visualization
			List<GKInstance> basePathways = new PathwayImpactAnalyzer().loadPathwaysForAnalysis(mysqlDBA);
			// Pull out all human pathways
			GKInstance human = mysqlDBA.fetchInstance(GKApplicationUtilities.HOMO_SAPIENS_DB_ID);
			Collection<GKInstance> humanPathways = mysqlDBA.fetchInstanceByAttribute(ReactomeJavaConstants.Pathway,
					ReactomeJavaConstants.species, 
					"=",
					human);
			ReactomeAnalyzer reactomeAnalyzer = new ReactomeAnalyzer();
			reactomeAnalyzer.setMySQLAdaptor(mysqlDBA);
			// In this context we want to include candidate members so that the numbers are more consistent
			// with the web application
			reactomeAnalyzer.getTopicHelper().setNeedCandidateRepeatedUnit(true);
			Map<GKInstance, Set<String>> pathway2uniprotIds = reactomeAnalyzer.grepIDsFromTopics(humanPathways);
			// Convert to the required data structures
			for (GKInstance pathway : pathway2uniprotIds.keySet()) {
				Set<String> uniprotIds = pathway2uniprotIds.get(pathway);
				GKInstance stableIdInst = (GKInstance) pathway.getAttributeValue(ReactomeJavaConstants.stableIdentifier);
				String stableId = (String) stableIdInst.getAttributeValue(ReactomeJavaConstants.identifier);
				Pathway pathwayObj = new Pathway(stableId,
												 pathway.getDisplayName(),
												 basePathways.contains(pathway));
				pathwayStIdToPathway.put(stableId, pathwayObj);
				for (String uniprotId : uniprotIds) {
					// Just in case isoform is used. But most unlikely.
					String gene = uniprotToGene.get(uniprotId.split("-")[0]);
					if (gene == null)
						continue;
					pathwayObj.addGene(gene);
					geneToPathwayList.compute(gene, (k, set) -> {
						if (set == null)
							set = new HashSet<>();
						set.add(pathwayObj);
						return set;
					});
				}
			}
			// Just close MySQLAdaptor. There is no need in other place
			mysqlDBA.cleanUp();
		} 
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public Map<String, Pathway> getPathwayStIdToPathway(Map<String, String> uniprotToGene){
		if(this.pathwayStIdToPathway == null)
			this.cachePathways(uniprotToGene);
		return this.pathwayStIdToPathway;
	}

	public Map<String, Set<Pathway>> getGeneToPathwayList(Map<String, String> uniprotToGene){
		if(this.geneToPathwayList == null)
			this.cachePathways(uniprotToGene);
		return this.geneToPathwayList;
	}  

	public Map<String, Set<String>> getGeneToPathwayStId(Map<String, String> uniprotToGene){
		if(this.geneToPathwayStId == null) {
			this.geneToPathwayStId =  getGeneToPathwayList(uniprotToGene).entrySet().stream()
					.collect(Collectors.toMap(
							Map.Entry::getKey, 
							e -> e.getValue().stream().map(Pathway::getStId).collect(Collectors.toSet())));
		}
		return this.geneToPathwayStId;
	}

	/**
	 * Generate a list of pathways that are ordered based on their locations in the pathway
	 * hierarchy so that similar pathways are grouped together. This method uses a width-first-search
	 * algorithm to get the list.
	 * @return
	 * @throws Exception
	 */
    @SuppressWarnings("unchecked")
    public synchronized List<GraphPathway> getHierarchicalOrderedPathways() {
    	if (pathways != null)
    		return pathways;
    	try {
    		URL url = new URL(config.getEventHierarchyUrl());
    		ObjectMapper mapper = new ObjectMapper();
    		List<GraphPathway> topPathways = mapper.readValue(url,
    				new TypeReference<List<GraphPathway>>() {
    		});
    		List<GraphPathway> pathways = new ArrayList<>();
    		// Some pathways may be listed under multiple topics. For this application,
    		// we need to list them once only by choosing whatever they occur first.
    		// Use this set to keep tracking pathways based on names.
    		Set<String> listedPathways = new HashSet<>();
    		for (GraphPathway topPathway : topPathways) {
    			traversePathway(topPathway, topPathway, pathways, listedPathways);
    		}
    		// Get rid of some values that don't need at the front-end
    		pathways.forEach(p -> {
    			p.setChildren(null);
    			p.setSpecies(null);
    			p.setType(null);
    			p.setDiagram(false);
    		});
    		this.pathways = pathways;
    		return pathways;
    	}
    	catch(Exception e) {
    		logger.error(e.getMessage(), e);
    	}
    	return Collections.EMPTY_LIST; // Just return an empty list.
    }

	private void traversePathway(GraphPathway topPathway,
								GraphPathway currentPathway,
					            List<GraphPathway> list,
					            Set<String> listedPathways) {
		if (!currentPathway.getType().equals("Pathway") &&
				!currentPathway.getType().equals("TopLevelPathway"))
				return; // Check pathways only
		if (listedPathways.contains(currentPathway.getName()))
				return; // Added to the list already
		currentPathway.setTopPathway(topPathway.getName());
		list.add(currentPathway);
		listedPathways.add(currentPathway.getName());
		if (currentPathway.getChildren() == null)
				return; // No need to go down.
		for (GraphPathway child : currentPathway.getChildren())
				traversePathway(topPathway, child, list, listedPathways);
	}
}
