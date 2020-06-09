package org.reactome.idg.pairwise.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.reactome.idg.pairwise.model.DataDesc;
import org.reactome.idg.pairwise.model.PairwiseRelationship;
import org.reactome.idg.pairwise.service.PairwiseService;

public class PathwayProcessor {
    private final static Logger logger = LoggerFactory.getLogger(PathwayProcessor.class);
    private final String UNIPROT_2_REACTOME_URL = "https://reactome.org/download/current/UniProt2Reactome.txt";
	
	public PathwayProcessor() {}
	
	public void processPathways(PairwiseService service){
		Map<String, String> uniprotToGene = service.getUniProtToGene();
    	Map<String, List<String>> pathwayStIdToGeneNameList = new HashMap<>(); 
    	
    	try {
    		URL url = new URL(UNIPROT_2_REACTOME_URL);
        	BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
        	String line = null;
			while((line = br.readLine()) != null) {
				String[] tokens = line.split("\t");
				
				//need to remove "-#" in any uniprot to get parent uniprot
				//map.compute api to simplify (takes binary function)
				String correctedUniprot = tokens[0].split("-")[0];
				if(uniprotToGene.containsKey(correctedUniprot)) {
					if(!pathwayStIdToGeneNameList.containsKey(tokens[1]))
						pathwayStIdToGeneNameList.put(tokens[1], new ArrayList<>());
					pathwayStIdToGeneNameList.get(tokens[1]).add(uniprotToGene.get(correctedUniprot));
				}
			}			
			br.close();
			
			Map<String, Integer> pathwayToIndex = service.ensurePathwayIndex(pathwayStIdToGeneNameList.keySet());
	    	//regenerate pathway collection so that any no longer existing pathway-gene relationships are removed
	    	service.regeneratePathwayCollection();
	    	processGenePathwayRelationship(pathwayStIdToGeneNameList, pathwayToIndex, service);
			
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			return;
		}
	}

	/**
	 * Creates two maps
	 * Maps from pathway stId to list of genes by index
	 * Maps from gene name to list of pathways by index
	 * Directs service to insert these maps into the database
	 * @param pathwayStIdToGeneNameList
	 * @param pathwayToIndex
	 * @param service
	 */
	private void processGenePathwayRelationship(Map<String, List<String>> pathwayStIdToGeneNameList,
									  Map<String, Integer> pathwayToIndex,
									  PairwiseService service) {
		Map<String, Integer> geneToIndex = service.getIndexToGene().entrySet().stream().collect(Collectors.toMap(Entry::getValue, Entry::getKey));
		
		//Will be persisted into pathways table of mongoDb
		Map<String, List<Integer>> pathwayToGeneIndexList = new HashMap<>();
		
		//want to persist Map of gene name to list of pathway Indexes
		Map<String, Set<Integer>> geneToPathwayIndexList = new HashMap<>();
		
		//generates values for pathwayToGeneIndexList
		//also generates geneToPathwayIndexList to avoid having to loop again later
		pathwayStIdToGeneNameList.forEach((k,v) -> {
			List<Integer> geneIndexes = new ArrayList<>();
			v.forEach(gene -> {
				//add gene index to list for placement on pathwayToGeneIndexList
				geneIndexes.add(geneToIndex.get(gene));
				//next to lines for building geneToPathwayIndexList
				if(!geneToPathwayIndexList.containsKey(gene)) geneToPathwayIndexList.put(gene, new HashSet<>());
				if(!geneToPathwayIndexList.get(gene).contains(pathwayToIndex.get(k))) geneToPathwayIndexList.get(gene).add(pathwayToIndex.get(k));
			});
			pathwayToGeneIndexList.put(k, new ArrayList<>(geneIndexes));
		});
		
		service.insertPathwayRelationships(pathwayToGeneIndexList);
		service.insertGeneRelationships(geneToPathwayIndexList, getGeneToSecondPathwayIndexList(geneToPathwayIndexList, service));
	}

	/**
	 * Makes relationships for gene to pathway and secondary pathway
	 * Returns a list of these pathways
	 * @param geneToPathwayIndexList
	 * @param service
	 * @return
	 */
	private Map<String, Set<Integer>> getGeneToSecondPathwayIndexList(Map<String, Set<Integer>> geneToPathwayIndexList, PairwiseService service) {		
		//should be a list of all descIds
		List<String> dataDesc = service.listDataDesc().stream().map(DataDesc::getId).collect(Collectors.toList());
		long time1 = System.currentTimeMillis();
		
		Map<String, Set<Integer>> geneToSecondPathwayList = new HashMap<>();
		//loop over genes from geneToIndex to make sure all genes are checked for secondary pathways
		//even if they have no pathways in geneToPathwayIndexList
		Set<Integer> emptySet = new HashSet<>();
		service.getIndexToGene().values().forEach(gene -> {
			Set<Integer> pathwayIndexList = new HashSet<>();
			List<PairwiseRelationship> pairwise = service.queryRelsForGenes(Collections.singletonList(gene), dataDesc);
			pairwise.forEach(rel -> {
				if(rel.getPosGenes() != null) {
					pathwayIndexList.addAll(rel.getPosGenes().stream()
							.flatMap(key -> geneToPathwayIndexList.getOrDefault(key, emptySet).stream())
							.collect(Collectors.toSet()));
				}
				if(rel.getNegGenes() != null)
					pathwayIndexList.addAll(rel.getNegGenes().stream()
							.flatMap(key -> geneToPathwayIndexList.getOrDefault(key, emptySet).stream())
							.collect(Collectors.toSet()));
			});
			pathwayIndexList.removeAll(geneToPathwayIndexList.getOrDefault(gene, emptySet));
			geneToSecondPathwayList.put(gene, pathwayIndexList);
		});
		long time2 = System.currentTimeMillis();
		logger.info("Total time: " + (time2 - time1));
		
		return geneToSecondPathwayList;
	}
}