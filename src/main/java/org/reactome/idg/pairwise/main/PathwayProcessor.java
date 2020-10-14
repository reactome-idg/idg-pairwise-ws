package org.reactome.idg.pairwise.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
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

/**
 * 
 * @author brunsont
 *
 */
public class PathwayProcessor {
    private final static Logger logger = LoggerFactory.getLogger(PathwayProcessor.class);
    private final String UNIPROT_2_REACTOME_BASE_LEVEL_URL = "https://reactome.org/download/current/UniProt2Reactome.txt";
    private final String UNIPROT_2_REACTOME_ALL_LEVELS_URL = "https://reactome.org/download/current/UniProt2Reactome_All_Levels.txt";
    
	private Map<String, Integer> pathwayToIndex;
	private Map<String, Set<Integer>> geneToPathwayIndexList;
	
	public PathwayProcessor() {}
	
	/**
	 * Gets Uniprot2Reactome_All_Levels.txt file and directs creation of PATHWAY_INDEX and pathways collection
	 * @param service
	 */
	public void processPathways(PairwiseService service){
		Map<String, String> uniprotToGene = service.getUniProtToGene();
    	Map<String, List<String>> pathwayStIdToGeneNameList = new HashMap<>();
    	Map<String, String> pathwayStIdToPathwayName = new HashMap<>();
    	
    	try {
    		URL url = new URL(UNIPROT_2_REACTOME_ALL_LEVELS_URL);
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
					if(!pathwayStIdToPathwayName.containsKey(tokens[1]))
						pathwayStIdToPathwayName.put(tokens[1], tokens[3]);
				}
			}			
			br.close();
						
			//Need to get a list of the bottom level pathways for flaggin in PATHWAY_INDEX
			Set<String> basePathways = getBasePathways(uniprotToGene);
			
			//generate file to be used for on the fly analysis in ws
	    	generateProteinNameToPathwayFile(pathwayStIdToGeneNameList);
			
			//regenerate pathway collection and PATHWAY_INDEX so that any no longer existing pathway-gene relationships are removed
	    	service.regeneratePathwayCollections();
			
			pathwayToIndex = service.ensurePathwayIndex(pathwayStIdToPathwayName, basePathways);
	    	processGenePathwayRelationship(pathwayStIdToGeneNameList, service);
			
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			return;
		}
	}

	private Set<String> getBasePathways(Map<String, String> uniprotToGene) throws IOException {
		Set<String> rtn = new HashSet<>();
		URL url = new URL(UNIPROT_2_REACTOME_BASE_LEVEL_URL);
		BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
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
												PairwiseService service) {
		Map<String, Integer> geneToIndex = service.getIndexToGene().entrySet().stream().collect(Collectors.toMap(Entry::getValue, Entry::getKey));
		
		//Will be persisted into pathways table of mongoDb
		Map<String, List<Integer>> pathwayToGeneIndexList = new HashMap<>();
		
		//want to persist Map of gene name to list of pathway Indexes
		geneToPathwayIndexList = new HashMap<>();
		
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
		service.insertGeneRelationships(geneToPathwayIndexList);
	}
	
	private void generateProteinNameToPathwayFile(Map<String, List<String>> pathwayStIdToGeneNameList) throws IOException {
		String fileName = "src/main/resources/ProteinNameToPathwayStId.txt";
		File geneToPathwaysFile = new File(fileName);
		geneToPathwaysFile.createNewFile();
		FileWriter fos = new FileWriter(geneToPathwaysFile);
		PrintWriter dos = new PrintWriter(fos);
		pathwayStIdToGeneNameList.forEach((stId,genes) -> {
			genes.stream().distinct().forEach(gene -> {
				dos.println(gene + "\t" + stId);
			});
		});
		dos.close();
		fos.close();
		
		
	}
}