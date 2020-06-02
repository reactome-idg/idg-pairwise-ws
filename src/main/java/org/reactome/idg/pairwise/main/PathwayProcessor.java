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
        	Set<String> hsa = new HashSet<>();
        	String line = null;
			while((line = br.readLine()) != null) {
				String[] tokens = line.split("\t");
				if(tokens[1].contains("R-HSA")) hsa.add(tokens[1]);
				if(uniprotToGene.containsKey(tokens[0].contains("-") ? tokens[0].substring(0, tokens[0].indexOf("-")) : tokens[0] )) {
					if(!pathwayStIdToGeneNameList.containsKey(tokens[1]))
						pathwayStIdToGeneNameList.put(tokens[1], new ArrayList<>());
					pathwayStIdToGeneNameList.get(tokens[1]).add(tokens[0]);
				}
			}			
			br.close();
			hsa.removeAll(pathwayStIdToGeneNameList.keySet());
			System.out.println(hsa);
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
    	
    	Map<String, Integer> pathwayToIndex = service.ensurePathwayIndex(pathwayStIdToGeneNameList.keySet());
    	processPathwayToGene(pathwayStIdToGeneNameList, service);
    	processGeneToFirstPathway(pathwayStIdToGeneNameList, service);
	}

	private void processPathwayToGene(Map<String,List<String>> pathwayStIdToGeneNameList,
									  PairwiseService service) {
		Map<String, Integer> geneToIndex = service.getIndexToGene().entrySet().stream().collect(Collectors.toMap(Entry::getValue, Entry::getKey));
		
		//want to persist Map of pathway to list of gene Index
		Map<String, List<Integer>> pathwayToGeneIndexList = new HashMap<>();
		
		//convert from gene name to gene index 
		pathwayStIdToGeneNameList.forEach((k,v) -> {
			List<Integer> geneIndexes = new ArrayList<>();
			v.forEach(gene -> {
				geneIndexes.add(geneToIndex.get(gene));
			});
			pathwayToGeneIndexList.put(k, geneIndexes);
		});
		
		//persist pathwayToGeneIndexList to mongodb 
		service.insertPathwayToGeneDoc(pathwayToGeneIndexList);
	}
	
	private void processGeneToFirstPathway(Map<String, List<String>> pathwayStIdToGeneNameList,
			   							   PairwiseService service) {

	}
}
