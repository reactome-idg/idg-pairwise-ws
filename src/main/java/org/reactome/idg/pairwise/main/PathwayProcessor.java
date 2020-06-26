package org.reactome.idg.pairwise.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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
import org.reactome.annotate.AnnotationType;
import org.reactome.annotate.GeneSetAnnotation;
import org.reactome.annotate.PathwayBasedAnnotator;
import org.reactome.idg.pairwise.model.DataDesc;
import org.reactome.idg.pairwise.model.PairwiseRelationship;
import org.reactome.idg.pairwise.model.Pathway;
import org.reactome.idg.pairwise.service.PairwiseService;

public class PathwayProcessor {
    private final static Logger logger = LoggerFactory.getLogger(PathwayProcessor.class);
    private final String UNIPROT_2_REACTOME_URL = "https://reactome.org/download/current/UniProt2Reactome.txt";
    
    
    private PathwayBasedAnnotator analyzer;
	private Map<String, Integer> pathwayToIndex;
	
	public PathwayProcessor() {}
	
	public void processPathways(PairwiseService service){
		Map<String, String> uniprotToGene = service.getUniProtToGene();
    	Map<String, List<String>> pathwayStIdToGeneNameList = new HashMap<>();
    	Map<String, String> pathwayStIdToPathwayName = new HashMap<>();
    	
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
					if(!pathwayStIdToPathwayName.containsKey(tokens[1]))
						pathwayStIdToPathwayName.put(tokens[1], tokens[3]);
				}
			}			
			br.close();
			
			//want tos set this up before regenerating any of the database
			analyzer = new PathwayBasedAnnotator();
			setAnalysisFile(analyzer, pathwayStIdToGeneNameList);
			
			//regenerate pathway collection and PATHWAY_INDEX so that any no longer existing pathway-gene relationships are removed
	    	service.regeneratePathwayCollections();
			
			pathwayToIndex = service.ensurePathwayIndex(pathwayStIdToPathwayName);
	    	processGenePathwayRelationship(pathwayStIdToGeneNameList, service);
			
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
		service.insertGeneRelationships(geneToPathwayIndexList, getGeneToSecondPathwayList(geneToPathwayIndexList, pathwayStIdToGeneNameList, service));
	}

	/**
	 * Makes relationships for gene to pathway and secondary pathway
	 * Returns a list of these pathways
	 * @param geneToPathwayIndexList
	 * @param service
	 * @return
	 */
	private Map<String, Set<Pathway>> getGeneToSecondPathwayList(Map<String, Set<Integer>> geneToPathwayIndexList, Map<String, List<String>> pathwayStIdToGeneNameList, PairwiseService service) {		
		//should be a list of all descIds
		List<String> dataDesc = service.listDataDesc().stream().map(DataDesc::getId).collect(Collectors.toList());
		long time1 = System.currentTimeMillis();
		logger.info("Beginning creation of GeneToSecondaryPathwayList");
		
		Map<String, Set<Pathway>> geneToSecondPathwayList = new HashMap<>();
		//loop over genes from geneToIndex to make sure all genes are checked for secondary pathways
		//even if they have no pathways in geneToPathwayIndexList
		Set<Integer> emptySet = new HashSet<>();
		service.getIndexToGene().values().forEach(gene -> {
			Set<String> interactorGenes = new HashSet<>();
			List<PairwiseRelationship> pairwise = service.queryRelsForGenes(Collections.singletonList(gene), dataDesc);
			pairwise.forEach(rel -> {
				if(rel.getPosGenes() != null) {
					interactorGenes.addAll(rel.getPosGenes());
				}
				if(rel.getNegGenes() != null)
					interactorGenes.addAll(rel.getNegGenes());
			});
			
			geneToSecondPathwayList.put(gene, analyzeGeneSet(interactorGenes, geneToPathwayIndexList.getOrDefault(gene, emptySet)));
		});
		long time2 = System.currentTimeMillis();
		logger.info("Total time: " + (time2 - time1));
		
		return geneToSecondPathwayList;
	}

	private Set<Pathway> analyzeGeneSet(Set<String> interactorGenes, Set<Integer> primaryPathways) {
		
		if(interactorGenes.isEmpty()) return new HashSet<>();
		
		Set<Pathway> pathways = new HashSet<>();
		try {
			List<GeneSetAnnotation> annotations = analyzer.annotateGenesWithFDR(interactorGenes, AnnotationType.Pathway);
			annotations.forEach(x -> {
				Integer index = pathwayToIndex.get(x.getTopic());
				if(!primaryPathways.contains(index))
					pathways.add(new Pathway(pathwayToIndex.get(x.getTopic()), x.getFdr(), x.getPValue()));
			});
		} catch (Exception e) {
			logger.info("Error analyzing genes: " + e.getMessage());
		}

		
		return pathways;
	}

	/**
	 * Sets protein name to pathway File for gene expression analysis
	 * @param analyzer
	 * @param pathwayStIdToGeneNameList
	 */
	private void setAnalysisFile(PathwayBasedAnnotator analyzer, Map<String, List<String>> pathwayStIdToGeneNameList) throws IOException{
		String geneToPathwayFileName = "temp/geneToPathway.txt";
			File geneToPathwayFile = new File(geneToPathwayFileName);
			geneToPathwayFile.getParentFile().mkdirs();
			geneToPathwayFile.createNewFile();
			geneToPathwayFile.deleteOnExit();
			FileWriter fos = new FileWriter(geneToPathwayFileName);
			PrintWriter dos = new PrintWriter(fos);
			pathwayStIdToGeneNameList.forEach((stId,genes) -> {
				genes.stream().distinct().forEach(gene -> {
					dos.println(gene + "\t" + stId);
				});
			});
			dos.close();
			fos.close();
			analyzer.getAnnotationHelper().setProteinNameToPathwayFile(geneToPathwayFileName);
	}
}