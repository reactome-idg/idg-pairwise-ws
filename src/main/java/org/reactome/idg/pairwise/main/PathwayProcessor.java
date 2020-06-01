package org.reactome.idg.pairwise.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.reactome.idg.pairwise.service.PairwiseService;

public class PathwayProcessor {
    private final static Logger logger = LoggerFactory.getLogger(PathwayProcessor.class);
    private final String UNIPROT_2_REACTOME_URL = "https://reactome.org/download/current/UniProt2Reactome.txt";
	
	public PathwayProcessor() {}
	
	public void processPathwayIndex(PairwiseService service){
		Map<String, String> uniprotToGene = service.getUniProtToGene();
    	List<String> pathwayStIds = new ArrayList<>(); 
    	
    	try {
    		URL url = new URL(UNIPROT_2_REACTOME_URL);
        	BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
        	
        	String line = null;
			while((line = br.readLine()) != null) {
				String[] tokens = line.split("\t");
				if(!uniprotToGene.containsKey(tokens[0]))continue;
				pathwayStIds.add(tokens[1]);
				
			}
			br.close();
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
    	service.ensurePathwayIndex(pathwayStIds);
	}
	
}
