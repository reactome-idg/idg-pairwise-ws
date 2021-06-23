package org.reactome.idg.pairwise.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
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
    	Map<String, Set<String>> pathwayToGeneSet = new HashMap<>();
    	Map<String, Double> pathwayToWeightedTDL = new HashMap<>();
    	
    	//build map of pathay stId to set of genes in pathway
    	try {
    		URL url = new URL(UNIPROT_2_REACTOME_ALL_LEVELS_URL);
    		BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
    		String line = null;
    		while((line = br.readLine()) != null){
    			String[] tokens = line.split("\t");
    			
    			String geneName = uniprotToGene.get(tokens[0].split("-")[0]);
    			if(geneName == null) continue;
    			String pw = tokens[1];
    			
    			if(!pathwayToGeneSet.containsKey(pw)) pathwayToGeneSet.put(pw, new HashSet<>());
    			pathwayToGeneSet.get(pw).add(geneName);
    		}
    		br.close();
    	} catch(IOException e) {
    		logger.error(e.getMessage(), e);
    		return;
    	}
    	
    	//build map of pathway to weighted target development level score
    	logger.info("Processing weighted average target development levels");
    	Map<String, String> genes = getGeneTargetDevLevels();
    	
    	pathwayToGeneSet.forEach((pw, geneList) ->{
    		//list of TDLs for pw based on the pw's geneList
    		Collection<String> tdls = geneList.stream().filter(genes::containsKey).collect(Collectors.toMap(Function.identity(), genes::get)).values();
    		//calculate weighted average
    		double tdark = Collections.frequency(tdls, "Tdark")*1;
    		double tbio = Collections.frequency(tdls, "Tbio")*2;
    		double tchem = Collections.frequency(tdls, "Tchem")*3;
    		double tclin = Collections.frequency(tdls, "Tclin")*4;
    		double avg = (tdark+tbio+tclin+tchem)/tdls.size();
    		//put weighted TDL on map for inserting into database
    		pathwayToWeightedTDL.put(pw, avg);
    	});
    	
    	logger.info("Inserting pathways into database");
    	service.insertReactomePathwayCache(pathwayToGeneSet, pathwayToWeightedTDL);
    	logger.info("Done inserting pathways into database");
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
