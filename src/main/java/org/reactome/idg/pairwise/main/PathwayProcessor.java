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
    	Map<String, String> geneToPathwayStId = new HashMap<>(); 
    	
    	try {
    		URL url = new URL(UNIPROT_2_REACTOME_URL);
        	BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
        	
        	String line = null;
			while((line = br.readLine()) != null) {
				String[] tokens = line.split("\t");
				if(!uniprotToGene.containsKey(tokens[0]))continue;
				geneToPathwayStId.put(uniprotToGene.get(tokens[0]), tokens[1]);
				
			}
			br.close();
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
    	Map<String, Integer> pathwayToIndex = service.ensurePathwayIndex(new HashSet<>(geneToPathwayStId.values()));
    	processPathwayToGene(geneToPathwayStId, service);
	}
	
	private void processPathwayToGene(Map<String,String> geneToPathwayStId,
									  PairwiseService service) {
		Map<String, Integer> geneToIndex = service.getIndexToGene().entrySet().stream().collect(Collectors.toMap(Entry::getValue, Entry::getKey));
		
		//want to persist Map of pathway to list of gene Index
		Map<String, List<Integer>> pathwayToGeneIndexList = new HashMap<>();
		
		//iterate over (k,v) of unniprotToPathwayStId, 
		//check if pathwayStId in key list, add if not,
		//use gene to get index from geneToIndex map and add to list of integers in value
		geneToPathwayStId.forEach((k,v) -> {
			if(!pathwayToGeneIndexList.containsKey(v)) pathwayToGeneIndexList.put(v, new ArrayList<>());
			pathwayToGeneIndexList.get(v).add(geneToIndex.get(k));
		});
		
		//persist pathwayToGeneIndexList to mongodb 
		service.insertPathwayToGeneDoc(pathwayToGeneIndexList);
	}
	
}
