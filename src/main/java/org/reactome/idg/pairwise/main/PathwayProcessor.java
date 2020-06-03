package org.reactome.idg.pairwise.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
				String correctedUniprot = tokens[0].contains("-") ? tokens[0].substring(0, tokens[0].indexOf("-")) : tokens[0];
				if(uniprotToGene.containsKey(correctedUniprot)) {
					if(!pathwayStIdToGeneNameList.containsKey(tokens[1]))
						pathwayStIdToGeneNameList.put(tokens[1], new ArrayList<>());
					if(uniprotToGene.get(correctedUniprot) == null) System.out.println(tokens[0]);
					pathwayStIdToGeneNameList.get(tokens[1]).add(uniprotToGene.get(correctedUniprot));
				}
			}			
			br.close();
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
    	
    	Map<String, Integer> pathwayToIndex = service.ensurePathwayIndex(pathwayStIdToGeneNameList.keySet());
    	processGenePathwayRelationship(pathwayStIdToGeneNameList, pathwayToIndex, service);
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
	private void processGenePathwayRelationship(Map<String,List<String>> pathwayStIdToGeneNameList,
									  Map<String, Integer> pathwayToIndex,
									  PairwiseService service) {
		Map<String, Integer> geneToIndex = service.getIndexToGene().entrySet().stream().collect(Collectors.toMap(Entry::getValue, Entry::getKey));
		
		//want to persist Map of pathway to list of gene Index
		Map<String, List<Integer>> pathwayToGeneIndexList = new HashMap<>();
		
		//want to persist Map of gene name to list of pathway Indexes
		Map<String, List<Integer>> geneToPathwayIndexList = new HashMap<>();
		
		//convert from gene name to gene index 
		pathwayStIdToGeneNameList.forEach((k,v) -> {
			List<Integer> geneIndexes = new ArrayList<>();
			v.forEach(gene -> {
				//add gene index to list for placement on pathwayToGeneIndexList
				geneIndexes.add(geneToIndex.get(gene));
				//next to lines for building geneToPathwayIndexList
				if(!geneToPathwayIndexList.containsKey(gene)) geneToPathwayIndexList.put(gene, new ArrayList<>());
				if(!geneToPathwayIndexList.get(gene).contains(pathwayToIndex.get(k))) geneToPathwayIndexList.get(gene).add(pathwayToIndex.get(k));
			});
			pathwayToGeneIndexList.put(k, geneIndexes);
		});
		
		//persist pathwayToGeneIndexList to mongodb 
		service.insertPathwayToGeneDoc(pathwayToGeneIndexList);
		service.insertGeneToPathwayDoc(geneToPathwayIndexList);
	}
}
