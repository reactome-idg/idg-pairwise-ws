package org.reactome.idg.pairwise.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.reactome.idg.pairwise.model.Pathway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * @author brunsont
 *
 */
@Service
public class PathwayService {
    private static final Logger logger = LoggerFactory.getLogger(PairwiseService.class);
    
    @Autowired
    private ServiceConfig config;
    
    //attempt at removing need for pathways and PATHWAY_INDEX collections
    private Map<String, Set<Pathway>> geneToPathwayList;
    private Map<String, Pathway> pathwayStIdToPathway;
    
    public PathwayService() {/*Nothing Here*/}
    
    /**
     * Caches pathways into geneToPathwayList and paathwayStIdToPathway list. Used for multiple service methods
     * Requires access to UniProt2Reactome.txt and UniProt2Reactome_All_Levels.txt from downloads page of reactome.org
     */
    private void cachePathways(Map<String, String> uniprotToGene) {
    	geneToPathwayList = new HashMap<>();
    	pathwayStIdToPathway = new HashMap<>();

		try {
			Set<String> basePathways = loadBasePathways(uniprotToGene);
			BufferedReader br = new BufferedReader(new FileReader(config.getUniProt2ReactomeAllLevelsFile()));
			String line = null;
			while((line = br.readLine()) != null) {
				
				//split by column
				String[] tokens = line.split("\t");
				
				String gene = uniprotToGene.get(tokens[0].split("-")[0]);
				if(gene == null) continue; //ensure row exists as a human pathway using list of human uniprots

				//if pathway isn't made yet, create pathway and add to pathwayStIdToPathway
				if(!pathwayStIdToPathway.containsKey(tokens[1])) {
					Pathway pw = new Pathway(tokens[1], tokens[3], (basePathways.contains(tokens[1]) ? true:false));
					pathwayStIdToPathway.put(tokens[1], pw);
				}
				
				//add gene to pathway object
				pathwayStIdToPathway.get(tokens[1]).addGene(gene);
				
				//if gene hasn't been seen before, add gene/hashset relationship to geneToPathwayList
				if(!geneToPathwayList.containsKey(gene)) geneToPathwayList.put(gene, new HashSet<>());
				
				//add pathway to set of pathways for gene
				geneToPathwayList.get(gene).add(pathwayStIdToPathway.get(tokens[1]));
			}
			br.close();
		} catch (IOException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
	}
    
	private Set<String> loadBasePathways(Map<String, String> uniprotToGene) throws IOException {
		Set<String> rtn = new HashSet<>();
		BufferedReader br = new BufferedReader(new FileReader(config.getUniProt2ReactomeFile()));
		String line = null;
		while((line = br.readLine()) != null) {
			String[] tokens = line.split("\t");
			String correctedUniprot = tokens[0].split("-")[0]; //fix for uniprots with isophorms
			if(uniprotToGene.containsKey(correctedUniprot))
				rtn.add(tokens[1]);
		}
		br.close();
		return rtn;
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
}
