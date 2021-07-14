package org.reactome.idg.pairwise.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.math.distribution.HypergeometricDistributionImpl;
import org.reactome.idg.pairwise.model.Pathway;
import org.reactome.idg.pairwise.model.PathwayOverlap;
import org.reactome.idg.pairwise.model.pathway.gene.ProteinTargetDevLevel;
import org.reactome.idg.pairwise.service.PairwiseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/*
 * @author brunsont
 */
public class GeneToPathwayProcessor {
    private final static Logger logger = LoggerFactory.getLogger(GeneToPathwayProcessor.class);

	private PairwiseService service;
    private final String UNIPROT_2_REACTOME_ALL_LEVELS_URL = "https://reactome.org/download/current/UniProt2Reactome_All_Levels.txt";
    private final String TCRD_WS_ALL_UNIPROTS_TDL_URL = "http://localhost:8060/tcrdws/targetLevel/all-uniprots";

    public GeneToPathwayProcessor(PairwiseService service) {
    	this.service = service;
    }
    
    public void processGeneToPathways(){
    	service.regenerateReactomePathwaysCacheCollection();
    	logger.info("Processing gene to Pathways");
		Map<String, String> uniprotToGene = service.getUniProtToGene();
    	Map<String, Pathway> pathwayStIdToPathway = new HashMap<>();    	
    	//build map of pathway stId to Pathway object
    	try {
    		URL url = new URL(UNIPROT_2_REACTOME_ALL_LEVELS_URL);
    		BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
    		String line = null;
    		while((line = br.readLine()) != null){
    			String[] tokens = line.split("\t");
    			
    			String geneName = uniprotToGene.get(tokens[0].split("-")[0]);
    			if(geneName == null) continue;
    			String pw = tokens[1];
    			
    			if(!pathwayStIdToPathway.containsKey(pw)) pathwayStIdToPathway.put(pw, new Pathway(pw));
    			pathwayStIdToPathway.get(pw).addGene(geneName);
    		}
    		br.close();
    	} catch(IOException e) {
    		logger.error(e.getMessage(), e);
    		return;
    	}
    	//build map of pathway to weighted target development level score
    	logger.info("Processing weighted average target development levels");
    	Map<String, String> geneToTDL = getGeneTargetDevLevels();
    	List<String> pathwayStIds = new ArrayList<>(pathwayStIdToPathway.keySet());
    	int totalGenesInReactome = service.getTotalReactomeGenes();
    	
    	for(int i = 0; i<pathwayStIds.size()-1; i++) {
    		
    		Pathway from = pathwayStIdToPathway.get(pathwayStIds.get(i));
    		if(from.getGenes() == null || from.getGenes().size() == 0) continue;
    		from.setWeightedTDL(getWeightedTDLScore(geneToTDL, from.getGenes()));
    		
    		for(int j = i+1; j<pathwayStIds.size(); j++) {
    			
    			Pathway to = pathwayStIdToPathway.get(pathwayStIds.get(j));
    			if(to == null || to.getGenes().size() == 0) continue;
    			
    			//get common genes between two pathways
    			Set<String> commonGenes = new HashSet<>(from.getGenes());
    			commonGenes.retainAll(to.getGenes());
    			
    			
      			HypergeometricDistributionImpl hyper = new HypergeometricDistributionImpl(totalGenesInReactome,
      																					  from.getGenes().size(),
      																					  to.getGenes().size());
      			
      			Double p = hyper.upperCumulativeProbability(commonGenes.size());
      			
      			from.addStIdToHypergeometricScoreRelationship(to.getStId(), new PathwayOverlap(to.getStId(), p, commonGenes.size()));
      			to.addStIdToHypergeometricScoreRelationship(from.getStId(), new PathwayOverlap(from.getStId(), p, commonGenes.size()));
    		}
    	}
    	
    	//add weightedTDL for last pathway
    	Pathway lastPW = pathwayStIdToPathway.get(pathwayStIds.get(pathwayStIds.size() - 1));
    	lastPW.setWeightedTDL(getWeightedTDLScore(geneToTDL, lastPW.getGenes()));
    	
    	logger.info("Inserting pathways into database");
    	service.insertReactomePathwayCache(pathwayStIdToPathway.values());
    	logger.info("Done inserting pathways into database");
    }
    
    private Double getWeightedTDLScore(Map<String, String> geneToTDL, List<String> genes) {
    	Collection<String> tdls = genes.stream().filter(geneToTDL::containsKey)
				   .collect(Collectors.toMap(Function.identity(), geneToTDL::get))
				   .values();
		//calculate weighted average
		double tdark = Collections.frequency(tdls, "Tdark")*1;
		double tbio = Collections.frequency(tdls, "Tbio")*2;
		double tchem = Collections.frequency(tdls, "Tchem")*3;
		double tclin = Collections.frequency(tdls, "Tclin")*4;
		return (tdark+tbio+tclin+tchem)/tdls.size();
    }

	private Map<String, String> getGeneTargetDevLevels() {
		ObjectMapper mapper = new ObjectMapper();
		HttpClient client = new HttpClient();
		GetMethod method = new GetMethod(TCRD_WS_ALL_UNIPROTS_TDL_URL);
		List<ProteinTargetDevLevel> genes = null;
		try {
			client.executeMethod(method);
			genes = mapper.readValue(method.getResponseBodyAsString(), mapper.getTypeFactory().constructCollectionType(List.class, ProteinTargetDevLevel.class));
		} catch (JsonParseException e) {
			e.printStackTrace();
			throw new RuntimeException("Target Development Levels could not be parsed");
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException("Target Development Levels could not be mapped to Objects");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException("Target Development Levels could not be fetched");
		}
		Map<String, String> geneNameToTDL = new HashMap<>();
		genes.forEach(gene -> {
			geneNameToTDL.put(gene.getSym(), gene.getTargetDevLevel());
		});
		return geneNameToTDL;
	}
    
}
