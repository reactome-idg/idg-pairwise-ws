package org.reactome.idg.pairwise.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.bson.Document;
import org.reactome.annotate.GeneSetAnnotation;
import org.reactome.annotate.PathwayBasedAnnotator;
import org.reactome.idg.model.FeatureType;
import org.reactome.idg.pairwise.model.DataDesc;
import org.reactome.idg.pairwise.model.GeneCombinedScore;
import org.reactome.idg.pairwise.model.PEsForInteractorResponse;
import org.reactome.idg.pairwise.model.PairwiseRelationship;
import org.reactome.idg.pairwise.model.Pathway;
import org.reactome.idg.pairwise.model.PathwayOverlap;
import org.reactome.idg.pairwise.model.network.EdgeData;
import org.reactome.idg.pairwise.model.network.Element;
import org.reactome.idg.pairwise.model.network.NodeData;
import org.reactome.idg.pairwise.model.pathway.GraphHierarchy;
import org.reactome.idg.pairwise.model.pathway.GraphPathway;
import org.reactome.idg.pairwise.model.pathway.HierarchyResponseWrapper;
import org.reactome.idg.pairwise.util.FourColorGradient;
import org.reactome.idg.pairwise.web.errors.InternalServerError;
import org.reactome.idg.pairwise.web.errors.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;

@Service
@SuppressWarnings("unchecked")
public class PairwiseService {
    //mongoDb collections
	private final String DATA_DESCRIPTIONS_COL_ID = "datadescriptions";
    private final String GENE_INDEX_COL_ID = "GENE_INDEX";
    private final String RELATIONSHIP_COL_ID = "relationships";
    private final String REACTOME_ANNOTATED_GENES_COL_ID = "REACTOME_ANNOTATED_GENES";
    private final String REACTOME_PATHWAYS_CACHE_COL_ID = "reactome_pathways";
    
    private final String UNIPROT_TO_GENE_FILE_NAME = "GeneToUniProt.txt";
    
    //to reference documents on collections
    private final String COMBINED_SCORE = "combined_score";

    private static final Logger logger = LoggerFactory.getLogger(PairwiseService.class);
    
    @Autowired
    private MongoDatabase database;
    
    @Autowired
    private ServiceConfig config;
    
    @Autowired
    private PathwayService pathwayService;
    
    // Want to cache the pathway list assuming it will not change
    private List<GraphPathway> pathways;

    
    FourColorGradient fourColorGradient;
    
    // Cached index to gene for performance
    private Map<Integer, String> indexToGene;
    //TODO: One-to-one mapping between UniProt and gene symbols are most likely not right.
    // This should be improved in the future.
    // Cached uniprot to gene mapping
    private Map<String, String> uniprotToGene;
    private Map<String, String> geneToUniprot;
    private Set<String> reactomeAnnotatedGenes;
    private int totalReactomeGenes;
    
    //cached EventHierarchy
    private GraphHierarchy graphHierarchy;
    private PathwayBasedAnnotator annotator;

    public PairwiseService() {
    	fourColorGradient = new FourColorGradient();
    }
    
    public ServiceConfig getServiceConfig() {
    	return config;
    }
	
	/**
     * Actually fills uniprotToGene and geneToUniprot map
     */
    private void loadUniProtToGene() {
        uniprotToGene = new HashMap<>();
        InputStream is = getClass().getClassLoader().getResourceAsStream(UNIPROT_TO_GENE_FILE_NAME);
        Scanner scanner = new Scanner(is);
        String line = scanner.nextLine(); // Escape one line
        while ((line = scanner.nextLine()) != null) {
            String[] tokens = line.split("\t");
            uniprotToGene.put(tokens[0], tokens[1]);
            if (!scanner.hasNextLine())
                break;
        }
        scanner.close();
        geneToUniprot = new HashMap<>();
        uniprotToGene.forEach((u, g) -> geneToUniprot.put(g, u));
    }


	public Map<String, String> getUniProtToGene() {
        if (uniprotToGene == null) {
            loadUniProtToGene();
        }
        return uniprotToGene;
    }
    
    public Map<String, String> getGeneToUniProt(){
    	if(geneToUniprot == null) loadUniProtToGene();
    	return geneToUniprot;
    }
    
    public int getTotalReactomeGenes() {
    	if(totalReactomeGenes == 0) loadTotalReactomeGenes(); 
    	return totalReactomeGenes;
    }

    private void loadTotalReactomeGenes() {
		Document doc = database.getCollection(REACTOME_ANNOTATED_GENES_COL_ID).find().first();
		totalReactomeGenes = ((List<String>)doc.get("reactomeAnnotatedGenes")).size();
	}

	public Set<String> getReactomeAnnotatedGenes(){
    	if(this.reactomeAnnotatedGenes != null) return this.reactomeAnnotatedGenes;
    	
    	Document doc = database.getCollection(REACTOME_ANNOTATED_GENES_COL_ID).find().first();
    	reactomeAnnotatedGenes = new HashSet<>((List<String>)doc.get("reactomeAnnotatedGenes"));
    	
    	return reactomeAnnotatedGenes;
    }
    
    /**
     * Loads Event hierarchy from server and initializes GraphHierarchy object
     */
    private void loadGraphHierarchy() {
		GetMethod method = new GetMethod(config.getEventHierarchyUrl());
		method.setRequestHeader("Accept", "application/json");
		HttpClient client = new HttpClient();
		int responseCode;
		try {
			responseCode = client.executeMethod(method);
			if(responseCode != HttpStatus.SC_OK) {
				logger.error(method.getStatusText());
			}
			graphHierarchy = new GraphHierarchy(method.getResponseBodyAsString());
		} catch (HttpException e) {
			logger.error(e.getMessage());
			throw new InternalServerError("Internal Server Error");
		} catch (IOException e) {
			logger.error(e.getMessage());
			throw new InternalServerError("Internal Server Error");
		}
	}
  
    public void testConnection() {
        FindIterable<Document> documents = database.getCollection("datadescriptions").find();
        for (Document doc : documents)
            System.out.println(doc.toJson());
    }
    
    public List<PairwiseRelationship> queryRelsForProteins(List<String> proteins,
                                                           List<String> descIds,
                                                           Boolean numberOnly) {
         Map<String, String> uniprotToGenes = getUniProtToGene();
         List<String> genes = proteins.stream()
                                      .map(p -> uniprotToGenes.get(p))
                                      .distinct() // Remove duplication
                                      .collect(Collectors.toList());
         // Since in rare cases, more than one gene may be mapped to the same UniProt id.
         // Therefore, we may get inconsistent numbers using genes. So always query 
         // full genes first. 
         List<PairwiseRelationship> rels = queryRelsForGenes(genes, descIds, false);
         // Need to add UniProt ids back to relationships
         for (PairwiseRelationship rel : rels) {
             rel.setGene(geneToUniprot.get(rel.getGene()));
             // Need to add UniProt ids back to relationships
             List<String> posGenes = rel.getPosGenes();
             if (posGenes != null) {
                 List<String> posProteins = posGenes.stream()
                         .map(g -> geneToUniprot.get(g))
                         .distinct()
                         .filter(x-> x!=null)
                         .collect(Collectors.toList());
                 if (numberOnly) {
                     rel.setPosNum(posProteins.size());
                     rel.setPosGenes(null); // Reset it so that they will not be returned
                 }
                 else
                     rel.setPosGenes(posProteins);
                 
             }
             List<String> negGenes = rel.getNegGenes();
             if (negGenes != null) {
                 List<String> negProteins = negGenes.stream()
                         .map(g -> geneToUniprot.get(g))
                         .distinct()
                         .filter(x-> x!=null)
                         .collect(Collectors.toList());
                 if (numberOnly) {
                     rel.setNegNum(negProteins.size());
                     rel.setNegGenes(null);
                 }
                 else
                     rel.setNegGenes(negProteins);
             }
         }
         return rels;
     }
    
    public List<PairwiseRelationship> queryRelsForProteins(List<String> proteins,
                                                          List<String> descIds) {
        return queryRelsForProteins(proteins, descIds, false);
    }
    
    /**
     * List PairwiseRelationships between queryGenes and targetGenes. For example, query a list of
     * genes in a pathway for TDark proteins.
     * @param queryGenes
     * @param targetGenes
     * @return
     */
    public List<PairwiseRelationship> queryRelsForGenesInTargets(List<String> queryGenes,
                                                                 List<String> targetGenes,
                                                                 List<String> descIds) {
        List<PairwiseRelationship> rtn = queryRelsForGenes(queryGenes, descIds);
        // Perform filtering
        for (Iterator<PairwiseRelationship> it = rtn.iterator(); it.hasNext();) {
            PairwiseRelationship rel = it.next();
            if (rel.getNegGenes() != null)
                rel.getNegGenes().retainAll(targetGenes);
            if (rel.getPosGenes() != null)
                rel.getPosGenes().retainAll(targetGenes);
            if ((rel.getNegGenes() == null || rel.getNegGenes().size() == 0) && 
                (rel.getPosGenes() == null || rel.getPosGenes().size() == 0))
                it.remove();
        }
        return rtn;
    }

    public List<PairwiseRelationship> queryRelsForGenes(List<String> genes,
                                                        List<String> descIds,
                                                        Boolean numberOnly) {
        Map<Integer, String> indexToGene = null;
        if (!numberOnly) // Need to load this map
            indexToGene = getIndexToGene();
        List<PairwiseRelationship> rtn = new ArrayList<>();
        FindIterable<Document> results = database.getCollection(RELATIONSHIP_COL_ID)
                .find(Filters.in("_id", genes))
                .projection(Projections.include(descIds));
        Map<String, DataDesc> idToDesc = createIdToDesc(descIds);
        for (Document result : results) {
            String gene = result.getString("_id");
            for (String key : result.keySet()) {
                if (descIds.contains(key)) {
                    // This should be a relationship
                    PairwiseRelationship rel = new PairwiseRelationship();
                    rtn.add(rel);
                    rel.setGene(gene);
                    rel.setDataDesc(idToDesc.get(key));
                    Document relDoc = (Document) result.get(key);
                    if (numberOnly)
                        fillGeneNumbersForRel(rel, relDoc);
                    else
                        fillGenesForRel(indexToGene, rel, relDoc);
                }
            }
        }
        return rtn;
    }
    
    public PEsForInteractorResponse queryPEsForTermInteractor(Long dbId, String term, List<Integer> dataDescKeys, Double prd) throws IOException {
		
		term = getGeneForTerm(term);
		if(term == null) throw new ResourceNotFoundException("Could not find term: " + term);
		
		//get pairwise doc for gene and throw exception if no doc found.
    	Document interactorsDoc = getRelationshipDocForGene(term);
    	if(interactorsDoc == null) throwDocumentNotFound(term);
    	
    	Map<String, List<Long>> geneToPEMap = callGeneToIdsInPathwayDiagram(dbId);
    	if(geneToPEMap == null) {
    		String errormsg = "Physical Entity Map could not be created.";
    		logger.error(errormsg);
    		throw new InternalServerError("Physical Entity Map could not be created.");
    	}
    	
    	PEsForInteractorResponse rtn = new PEsForInteractorResponse();
    	    	
    	Set<String> interactorGenes = new HashSet<>();
    	if(dataDescKeys == null || dataDescKeys.size() == 0 || dataDescKeys.contains(0)) { //want to get combined score if no data descs passed in or key is 0 for combined score
    		interactorGenes.addAll(this.getCombinedScoresWithCutoff((Document)interactorsDoc.get(COMBINED_SCORE), prd));
    		rtn.setDataDescs(Collections.singletonList("combined_score")); //set Combined score if no dataDesc keys passed in
    	}
    	else {
    		List<String> dataDescs = this.getDataDescIdsForDigitalKeys(dataDescKeys);
    		rtn.setDataDescs(dataDescs); //set data descs on return object
	    	for(String key : interactorsDoc.keySet()) {
	    		if(!dataDescs.contains(key)) continue;
	    		Document dataDoc = (Document) interactorsDoc.get(key);
	    		interactorGenes.addAll(getGenesFromRelDoc(dataDoc));
	    	}
    	}
    	
    	Set<Long> peIds = new HashSet<>();
    	interactorGenes.forEach(geneName -> {
    		if(geneToPEMap.containsKey(geneName))
    			peIds.addAll(geneToPEMap.get(geneName));
    	});
    	
    	rtn.setPeIds(new ArrayList<>(peIds));
    	rtn.setInteractors(new ArrayList<>(interactorGenes));
    	
		return rtn;

	}
 
 	private void throwDocumentNotFound(String term) {
		throw new ResourceNotFoundException("Could not find relationship document" + (term != null ? (" for: " + term + "."):"."));
	}

	private Map<String, List<Long>> callGeneToIdsInPathwayDiagram(Long pathwayDbId) throws IOException{
 		String url = config.getCoreWSURL() + "/getGeneToIdsInPathway/"+pathwayDbId;
 		GetMethod method = new GetMethod(url);
 		method.setRequestHeader("Accept", "application/json");
 		
 		HttpClient client = new HttpClient();
 		int responseCode = client.executeMethod(method);
 		if(responseCode != HttpStatus.SC_OK) {
 			logger.error(method.getStatusText());
 			if(responseCode == HttpStatus.SC_NOT_FOUND)
 				throw new ResourceNotFoundException("Pathway does not exist");
 			else
 				throw new InternalServerError("Unable to retrieve physical entities for pathway " + pathwayDbId);		
 		}
 		return structureGeneToPEMap(method.getResponseBodyAsString(), pathwayDbId);
 	}
     
    private Map<String, List<Long>> structureGeneToPEMap(String responseBodyAsString, Long pathwayDbId)  throws IOException{
		ObjectMapper mapper = new ObjectMapper();
		
		JsonNode response = mapper.readTree(responseBodyAsString);
		//throw if geneToPEIds doesn't exist in response. Also ensure it is an array.
		if(response.get("geneToPEIds") == null || !response.get("geneToPEIds").isArray()) {
			ResourceNotFoundException ex = new ResourceNotFoundException("Could not find physical entities for pathway: " + pathwayDbId);
			logger.error(ex.getMessage());
			return new HashMap<>();
		}
		
		Map<String, List<Long>> rtn = new HashMap<>();
		
		for(final JsonNode node : response.get("geneToPEIds")) {
			List<Long> pes = new ArrayList<>();
			String gene = node.get("gene").asText();
			//node.get("peDBIds") will either be a List of String or a String
			//either case add all options to pes
			if(node.get("peDbIds").isArray())
				for(JsonNode value : node.get("peDbIds"))
					pes.add(value.asLong());
			else
				pes.add(node.get("peDbIds").asLong());
				
			if(!rtn.keySet().contains(gene))
				rtn.put(gene, new ArrayList<>());
			rtn.get(gene).addAll(pes);
		}
		
		return rtn;
	}

	public List<PairwiseRelationship> queryRelsForGenes(List<String> genes,
                                                        List<String> descIds) {
        return queryRelsForGenes(genes, descIds, false);
    }
	
    private void fillGeneNumbersForRel(PairwiseRelationship rel, Document relDoc) {
        List<Integer> indexList = (List<Integer>) relDoc.get("pos");
        if (indexList != null)
            rel.setPosNum(indexList.size());
        indexList = (List<Integer>) relDoc.get("neg");
        if (indexList != null) {
            rel.setNegNum(indexList.size());
        }
    }

    private void fillGenesForRel(Map<Integer, String> indexToGene, PairwiseRelationship rel, Document relDoc) {
        List<Integer> indexList = (List<Integer>) relDoc.get("pos");
        if (indexList != null) {
            List<String> geneList = indexList.stream()
                    .map(i -> indexToGene.get(i))
                    .collect(Collectors.toList());
          //remove null that results from gene name mapping between species
            geneList.removeAll(Collections.singletonList("null"));
            rel.setPosGenes(geneList);
        }
        indexList = (List<Integer>) relDoc.get("neg");
        if (indexList != null) {
            List<String> geneList = indexList.stream()
                    .map(i -> indexToGene.get(i))
                    .collect(Collectors.toList());
            //remove null that results from gene name mapping between species
            geneList.removeAll(Collections.singletonList("null"));
            rel.setNegGenes(geneList);
        }
    }
    
    /**
     * Returns document from relationships doc for gene
     * @param gene
     * @return
     */
    public Document getRelationshipDocForGene(String gene) {
    	return database.getCollection(RELATIONSHIP_COL_ID)
    			.find(Filters.eq("_id", gene)).first();
    }
    
    public Set<String> getGenesFromRelDoc(Document relDoc){
    	Set<String> rtn = new HashSet<>();
    	    	    	
		rtn.addAll(getPosGenesFromRelDoc(relDoc));
		rtn.addAll(getNegGenesFromRelDoc(relDoc));
    	return rtn;
    }
    
    public Set<String> getPosGenesFromRelDoc(Document relDoc){
    	Set<String> rtn = new HashSet<>();
    	Map<Integer, String> indexToGene = getIndexToGene();
    	
		if(relDoc.containsKey("pos"))
			rtn.addAll(((List<Integer>)relDoc.get("pos")).stream().map(i -> indexToGene.get(i)).collect(Collectors.toSet()));
		
		return rtn;
    }
    
    public Set<String> getNegGenesFromRelDoc(Document relDoc){
    	Set<String> rtn = new HashSet<>();
    	Map<Integer,String> indexToGene = getIndexToGene();
    	
    	if(relDoc.containsKey("neg"))
			rtn.addAll(((List<Integer>)relDoc.get("neg")).stream().map(i -> indexToGene.get(i)).collect(Collectors.toSet()));	
    	
    	return rtn;
    }

    /**
     * Gets stIds for pathways containing term. Gets hierarchy and return.
     * Term can be either uniprot or gene symbol
     * @param term
     * @return
     */
    public HierarchyResponseWrapper queryHierarchhyForTerm(String term) {
    	if(graphHierarchy == null) loadGraphHierarchy();
    	
    	term = getGeneForTerm(term);
    	if(term == null) return new HierarchyResponseWrapper(term, new ArrayList<>(), new ArrayList<>());
    	
    	
    	List<String> stIds = queryPrimaryPathwaysForGene(term).stream().map(Pathway::getStId).collect(Collectors.toList());
		
		if(stIds == null || stIds.size() == 0)
			return new HierarchyResponseWrapper(term, new ArrayList<>(), new ArrayList<>());
		
		return new HierarchyResponseWrapper(term, stIds, graphHierarchy.getBranches(stIds));
	}

	public List<Pathway> queryPrimaryPathwaysForGene(String gene) {
		Set<Pathway> rtn = pathwayService.getGeneToPathwayList(this.getUniProtToGene()).get(gene);
		if(rtn == null) return new ArrayList<>();
		return rtn.size() > 0 ? new ArrayList<>(rtn) : new ArrayList<>();
	}
    
    public List<Pathway> queryPrimaryPathwaysForUniprot(String uniprot) {
    	String gene = this.getUniProtToGene().get(uniprot);
    	if(gene == null) return null;
    	List<Pathway> rtn = this.queryPrimaryPathwaysForGene(gene);
    	
    	//null check
    	if(rtn ==  null) return null;
    			
    	return rtn;
    }
    
    public String downloadTermToSecondaryPathwaysWithEnrichment(String term, List<Integer> dataDescKeys, double prd) {
		List<Pathway> pathways = queryTermToSecondaryPathwaysWithEnrichment(term, dataDescKeys, prd);
		
		StringBuilder pathwayString = new StringBuilder();
		
		pathwayString.append("Pathway Id,Pathway,Gene Number,pValue,FDR\n");
		
		pathways.forEach((pathway) -> {
		
			pathwayString.append(pathway.getStId() + ",")
						 .append(pathway.getName() + ",")
						 .append(pathway.getNumGenes() + ",")
						 .append(pathway.getpVal() + ",")
						 .append(pathway.getFdr() + "\n");
			
		});
		
		return pathwayString.toString();
	}
    
    public List<Pathway> queryTermToSecondaryPathwaysWithEnrichment(String term, List<Integer> dataDescKeys, Double prd) {
    	//if no data descs passed in, return pathways for combined score with a prd of 0.5d;
    	if(dataDescKeys == null || dataDescKeys.size() == 0 || dataDescKeys.contains(0)) return queryEnrichedPathwaysForCombinedScore(term, prd);
    	
    	//convert term to gene name and return new list if not available
    	term = getGeneForTerm(term);
    	if(term == null) return new ArrayList<>();

    	Document relDoc = getRelationshipDocForGene(term);
    	if(relDoc == null) this.throwDocumentNotFound(term);
    	
    	List<String> descIds = this.getDataDescIdsForDigitalKeys(dataDescKeys);
    	
    	Collection<String> interactors = new ArrayList<>();
    	for(String key : relDoc.keySet()) {
    		if(!descIds.contains(key)) continue;
    		Document doc = (Document) relDoc.get(key);
    		interactors.addAll(getGenesFromRelDoc(doc));
    	}
    	//if there are no interactors
    	if(interactors.size() < 1) {
    		return null;
    	}
    	
    	return getEnrichedPathways(interactors, term);
    	
	}
    
    public List<Element> queryTermToSecondaryPathwaysNetworkWithEnrichment(String term, 
                                                                           List<Integer> dataDescKeys,
                                                                           double prd) {
//    	Map<Integer, String> indexToGene = this.getIndexToGene();
    	List<Pathway> pathways;
    	//check if should get combined score pathways or based on dataDescs
    	if(dataDescKeys == null || dataDescKeys.size() == 0 || dataDescKeys.contains(0)) 
    		pathways = queryEnrichedPathwaysForCombinedScore(term, prd);
    	else 
    		pathways = queryTermToSecondaryPathwaysWithEnrichment(term, dataDescKeys, prd);
    	
    	// Filter to bottom level pathways only
    	pathways = pathways.stream()
    			           .filter(pathway -> pathway.isBottomLevel())
    			           .sorted((p1, p2) -> p1.getStId().compareTo(p2.getStId()))
    			           .collect(Collectors.toList());
    	
    	List<Element> nodes = new ArrayList<>();
    	List<Element> edges = new ArrayList<>();
    	
    	//double for loop to compare every pathway to every other pathway for overlapping
    	//genes
    	//assumes there are no repeat pathways
    	for(int i = 0; i < pathways.size(); i++) {
			Pathway from = pathways.get(i);

    		//get pathway overlap doc
			Document pathwayDoc = database.getCollection(REACTOME_PATHWAYS_CACHE_COL_ID).find(Filters.eq("_id", from.getStId())).first();
			if(pathwayDoc == null) continue;
			from.setWeightedTDL(pathwayDoc.getDouble("weighted_tdl_average"));
			    		//add from pathways to nodes
			NodeData nodeData = new NodeData(from.getStId(),
					   from.getName(),
					   from.getWeightedTDL(),
					   from.getFdr(),
					   from.getpVal(),
					   fourColorGradient.getColor(from.getWeightedTDL()));
			List<Integer> geneList = (List<Integer>)pathwayDoc.get("gene_list");
			nodeData.setGeneNumber(geneList == null? 0: geneList.size());
			
			nodes.add(new Element(Element.Group.NODES, nodeData));
			// Nothing to do for the last pathway. We just want to add it into the network
			if (i == pathways.size() - 1)
				break;
			//build map of overlapping pathway stId to overlap information
			Map<String, PathwayOverlap> stIdToOverlappingPathway = getStIdToOverlappingPathways(((List<Document>)pathwayDoc.get("overlapping_pathways")));
    		for(int j = i+1; j<pathways.size(); j++) {
    			Pathway to = pathways.get(j);
    			if(!stIdToOverlappingPathway.containsKey(to.getStId()) || 
    			   stIdToOverlappingPathway.get(to.getStId()).getNumberOfSharedGenes() == 0) // No shared genes, no edges
    				continue;
    			EdgeData data = new EdgeData(from.getStId() + ":" +to.getStId(),
			  							     stIdToOverlappingPathway.get(to.getStId()).getNumberOfSharedGenes(),
			  							     stIdToOverlappingPathway.get(to.getStId()).getHypergeometricScore(),
			  							     from.getStId(),
			  							     to.getStId());
    			Element edge = new Element(Element.Group.EDGES, data);
    			edges.add(edge);
    		}
    	}
    	// Two sections: first nodes and then edges. nodes is just a convenient variable here. Actually it
    	// is the whole network.
    	nodes.addAll(edges);
		return nodes;
	}
    
    private Map<String, PathwayOverlap> getStIdToOverlappingPathways(List<Document> docs) {
		Map<String, PathwayOverlap> rtn = new HashMap<>();
		
		docs.forEach(doc -> {
			String stId = doc.getString("stId");
			rtn.put(stId, new PathwayOverlap(stId, doc.getDouble("hypergeometricScore"), doc.getInteger("numberOfSharedGenes")));
		});
		
    	return rtn;
	}

	/**
     * Modifies Pathways in place to add list of genes and weighted TDL
     * @param pathways
     */
    private void addGenesAndTDLToPathways(List<Pathway> pathways) {
    	Map<Integer, String> indexToGene = this.getIndexToGene();
    	Map<String, Pathway> stIdToPathway = pathways.stream().collect(Collectors.toMap(Pathway::getStId, Function.identity()));
		MongoCollection<Document> collection = database.getCollection(REACTOME_PATHWAYS_CACHE_COL_ID);
		MongoCursor<Document> cursor = collection.find(Filters.in("_id", 
																  pathways.stream().map(pw -> pw.getStId())
																  .collect(Collectors.toList())))
																  .iterator();
		while(cursor.hasNext()) {
			Document curr = cursor.next();
			stIdToPathway.get(curr.get("_id")).setGenes(((List<Integer>)curr.get("gene_list"))
																			.stream().map(index -> indexToGene.get(index))
																			.collect(Collectors.toList()));
			stIdToPathway.get(curr.get("_id")).setWeightedTDL(((Double)curr.get("weighted_tdl_average")));
		}
	}

	public Map<String, Double> queryCombinedScoreGenesForTerm(String term) {
    	term = getGeneForTerm(term);
    	if(term == null) return new HashMap<>();
		
		Document relDoc = getRelationshipDocForGene(term);
		if(relDoc == null || relDoc.get(COMBINED_SCORE) == null)
			return new HashMap<>(); //return empty map if no document exists
		
		return getCombinedScoresWithoutCutoff((Document)relDoc.get(COMBINED_SCORE));
	}
    
    public List<Pathway> queryEnrichedPathwaysForCombinedScore(String term, Double prdCutoff) {
    	
    	//if term is uniprot, convert to gene name.
		//Return empty list if gene not available.
		term = getGeneForTerm(term);
    	if(term == null) return new ArrayList<>();
		
		Document relDoc = getRelationshipDocForGene(term);
		if(relDoc == null || relDoc.get(COMBINED_SCORE) == null) return null; //return null if no relationship doc for term or no combined score
		
		//Get combined scores from document. Loop over each row and add index (as gene)  to interactors if prd value less than passed in cutoff
		Collection<String> interactors = getCombinedScoresWithCutoff((Document)relDoc.get(COMBINED_SCORE), prdCutoff);
		
		if(interactors.size() == 0) //want to return empty array instead of enrich if no interactors under given prd.
			return new ArrayList<>();
    	
		return getEnrichedPathways(interactors, term);
	}
    
    private Map<String, Double> getCombinedScoresWithoutCutoff(Document combinedScores) {
    	if(combinedScores == null || combinedScores.size() == 0) return new HashMap<>();
    	Map<Integer, String> indexToGene = this.getIndexToGene();
    	Map<String, Double> rtn = new HashMap<>();
    	
    	combinedScores.forEach((index, prd) -> {
    		rtn.put(indexToGene.get(Integer.parseInt(index)), (Double)prd);
    	});
    	
    	return rtn;
    }
    
    /**
     * Returns list of interactors where score is higher than passed in prdCutoff
     * @param combinedScores
     * @param prdCutoff
     * @return
     */
    private Collection<String> getCombinedScoresWithCutoff(Document combinedScores, Double prdCutoff){
    	if(combinedScores == null || combinedScores.size() == 0) return new ArrayList<>();
		Map<Integer, String> indexToGene = this.getIndexToGene();
    	Collection<String> rtn = new ArrayList<>();
		combinedScores.forEach((index, prd) -> {
			if((Double)prd == null) return; //just in case prd isn't parse-able to avoid error on next line
			if((Double)prd > prdCutoff)
				rtn.add(indexToGene.get(Integer.parseInt(index)));
		});
		return rtn;
    }
    
    public List<Pathway> getEnrichedPathways(Collection<String> interactors, String gene){
    	Map<String, Pathway> pathwayStIdToPathway = pathwayService.getPathwayStIdToPathway(this.getUniProtToGene());
    	
    	//get enrichment analysis results
    	List<GeneSetAnnotation> annotations = performEnrichment(interactors, gene);
    	
    	//convert enrichment analysis results into a list of Pathway objects
    	List<Pathway> rtnPathways = new ArrayList<>();
    	annotations.forEach(annotation -> {
    		String stId = annotation.getTopic();
    		Pathway pathway = new Pathway(stId,
										  pathwayStIdToPathway.get(stId).getName(),
										  Double.parseDouble(annotation.getFdr()),
										  annotation.getPValue(),
										  pathwayStIdToPathway.get(stId).isBottomLevel());
    		pathway.setNumGenes(annotation.getNumberInTopic());
    		rtnPathways.add(pathway);
    	});
    	
    	return rtnPathways;
    }

	private List<GeneSetAnnotation> performEnrichment(Collection<String> interactors, String gene){
		if(annotator == null) loadPathwayBasedAnnotator();
    	List<GeneSetAnnotation> annotations;
    	try {
    		annotations = this.annotator.annotateGeneSet(interactors, 
    													 pathwayService.getGeneToPathwayStId(uniprotToGene));
    	} catch(Exception e) {
    		logger.error(e.getMessage(), e);
    		throw new InternalServerError("Could not annotate interactors for " + gene);
    	}
    	return annotations;
    }
	
	private void loadPathwayBasedAnnotator() {
		this.annotator = new PathwayBasedAnnotator();
	}
    
    public Pathway queryPathwayToGeneRelationships(String stId) {
    	return pathwayService.getPathwayStIdToPathway(this.getUniProtToGene()).get(stId);
    }
    
    public Pathway queryPathwayToUniprotRelationships(String stId) {
		Map<String, String> geneToUniprot = getGeneToUniProt();
		Pathway pw = pathwayService.getPathwayStIdToPathway(this.getUniProtToGene()).get(stId);
		if(pw == null) return null;
		
		//make new instance of pathway so as not to mutate the cached list 
		Pathway rtn = new Pathway();
		rtn.setStId(stId);
		rtn.setName(pw.getName());
		rtn.setBottomLevel(pw.isBottomLevel());
		
		//replace gene list on Pathway with list of uniprots
		List<String> uniprotList = pw.getGenes().stream().map(i -> geneToUniprot.get(i)).collect(Collectors.toList());
		rtn.setGenes(uniprotList);
		return rtn;
	}
    
    /**
     * For term and a map of interactors to their Functional Interaction value, 
     * return a CSV containing rows of term, interactor, reactome inclusion, FI value, 
     * and feature inclusion for the interaction.
     * @param term
     * @param interactors
     * @return
     */
    public String queryFeaturesForTermAndInteractors(String term, List<String>interactors) {
    	
    	//ensure term is or can be a gene name. 404 if not found
    	boolean isGene = this.getGeneToUniProt().containsKey(term);
    	String gene = isGene ? term : this.getUniProtToGene().get(term);
    	if(gene == null) throw new ResourceNotFoundException("Could not find term: " + term);
    	
    	Document geneDoc = this.getRelationshipDocForGene(gene);
    	if(geneDoc == null) this.throwDocumentNotFound(term);
    	
    	//get list of Data Description ids
    	List<String> dataDescriptions = this.getDataDescriptions().stream().sorted().collect(Collectors.toList());
    	
    	Map<String,Integer> geneToIndex = this.getIndexToGene().entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    	Document combinedScoreDoc = (Document)geneDoc.get(COMBINED_SCORE);
    	
    	//Make return String
    	StringBuilder rtn = new StringBuilder();
    	
    	//build header of file
    	rtn.append("gene_name,interactor_name,included_in_reactome,functional_interaction_score,")
    	   .append(String.join(",",dataDescriptions))
    	   .append("\n");
    	
    	//append row for each interactor
    	interactors.forEach(interactor -> {
    		int index = geneToIndex.get(interactor);
    		rtn.append(term+","+interactor+",")
    		   .append(this.getReactomeAnnotatedGenes().contains(interactor) ? "1,":"0,")
    		   .append(combinedScoreDoc.getDouble(index+""));
    		dataDescriptions.forEach(desc -> {
    			rtn.append(",");
    			Document doc = (Document) geneDoc.get(desc);
    			if(doc == null) {
    				rtn.append("0");
    				return;
    			}
    			if(doc.containsKey("pos") && ((List<Integer>)doc.get("pos")).contains(index)) {
    				rtn.append("1");
    				return;
    			}
    			if(doc.containsKey("neg") && ((List<Integer>)doc.get("neg")).contains(index)) {
    				rtn.append("-1");
    				return;
    			}
    			rtn.append("0");    			
    		});
    		rtn.append("\n");
    	});
    	
    	return rtn.toString();
	}

	private Map<String, DataDesc> createIdToDesc(List<String> descIds) {
        Map<String, DataDesc> idToDesc = new HashMap<>();
        for (String id : descIds) {
            // Just a very simple desc
            DataDesc desc = new DataDesc();
            desc.setId(id);
            idToDesc.put(id, desc);
        }
        return idToDesc;
    }

    public List<DataDesc> listDataDesc() {
        MongoCollection<Document> collection = database.getCollection(DATA_DESCRIPTIONS_COL_ID);
        FindIterable<Document> descDocs = collection.find();
        List<DataDesc> rtn = new ArrayList<>();
        for (Document doc : descDocs) {
            rtn.add(this.getDataDescObjectForDoc(doc));
        }
        return rtn;
    }
    
    /**
     * return list of data descriptions for a given term.
     * term can be either gene name or uniprot
     * @param term
     * @return
     */
    public List<DataDesc> listDataDesc(String term) {
		String gene = getGeneForTerm(term);
		if(gene == null) return new ArrayList<>();
		
		List<DataDesc> rtn = new ArrayList<>();
		
		MongoCollection<Document> collection = database.getCollection(DATA_DESCRIPTIONS_COL_ID);
		Document relDoc = this.getRelationshipDocForGene(gene);
		if(relDoc == null) this.throwDocumentNotFound(term);
		
		relDoc.keySet().forEach(key -> {
			if(!key.contains("|"))
				return;
			Document dataDescDoc = collection.find(Filters.eq("_id", key)).first();
			rtn.add(getDataDescObjectForDoc(dataDescDoc));
		});
		
		return rtn;
	}
    
    /**
     * Create Data Description object for a Document
     * @param doc
     * @return
     */
    private DataDesc getDataDescObjectForDoc(Document doc) {
    	DataDesc rtn = new DataDesc();
        Object value = doc.get("_id");
        rtn.setId((String)value);
        value = doc.get("digitalKey");
        if(value != null)
        	rtn.setDigitalKey((Integer)value);
        value = doc.get("bioSource");
        if (value != null)
            rtn.setBioSource((String)value);
        value = doc.get("provenance");
        if (value != null)
            rtn.setProvenance((String)value);
        value = doc.get("dataType");
        if (value != null)
            rtn.setDataType(FeatureType.valueOf((String)value));
        value = doc.get("origin");
        if (value != null)
            rtn.setOrigin((String)value);
       return rtn;
	}

	/**
     * ensure passed in term is a gene name, if uniprot, convert to gene name
     * if not known gene name or uniprot, return null
     * @param term
     * @return
     */
    public String getGeneForTerm(String term) {
		Map<String, String> uniprotToGene = this.getUniProtToGene();
		if(uniprotToGene.values().contains(term))
			return term;
		else if(uniprotToGene.containsKey(term))
			return uniprotToGene.get(term);
		else return null;
	}

	/**
     * For list of digital keys, return list of data description _id mapped from digitalkeys
     * @param digitalKeys
     * @return
     */
    public List<String> getDataDescIdsForDigitalKeys(List<Integer> digitalKeys){
    	List<String>rtn =  new ArrayList<>();
    	
    	MongoCollection<Document> collection = database.getCollection(DATA_DESCRIPTIONS_COL_ID);
    	digitalKeys.forEach(key -> {
    		//Combined score isnt on datadescriptions collection so add to rtn and return if found.
    		if(key==0) {
    			rtn.add(COMBINED_SCORE);
    			return;
    		}
    		rtn.add((String)collection.find(Filters.eq("digitalKey",key)).first().get("_id"));
    	});
    	
    	return rtn;
    }
    
    /**
     * Returns a list of all the data description Ids
     * representative of each feature of the mongodb relationships collection.
     * @return
     */
    public List<String> getDataDescriptions(){
    	List<String> rtn = new ArrayList<>();
    	MongoCursor<Document> cursor = database.getCollection(DATA_DESCRIPTIONS_COL_ID).find().iterator();
    	while(cursor.hasNext()) {
    		rtn.add(cursor.next().get("_id").toString());
    	}
    	return rtn;
    }
    /**
     * Do nothing for the time being since we are using the primary index. If needed,
     * indexing will be handled manually via the shell.
     */
    public void performIndex() {
    }
    
//    public Map<String, Integer> ensurePathwayIndex(Map<String, String> pathwayStIdToPathwayName, Set<String> bottomPathways){
//    	Map<String, Integer> rtn = new HashMap<>();
//    	MongoCollection<Document> collection = database.getCollection(PATHWAY_INDEX_COL_ID);
//    	int nextIndex = 0;
//    	for(Entry<String,String> entry : pathwayStIdToPathwayName.entrySet()){
//    		ensureCollectionDoc(collection, entry.getKey());
//    		collection.updateOne(Filters.eq("_id", entry.getKey()), Updates.set("index", nextIndex));
//    		collection.updateOne(Filters.eq("_id", entry.getKey()), Updates.set("name", entry.getValue()));
//    		
//    		boolean bottomLevel = bottomPathways.contains(entry.getKey()) ? true:false; //make boolean that is true if entry pathway is a bottom level pathway
//    		collection.updateOne(Filters.eq("_id", entry.getKey()), Updates.set("bottomLevel", bottomLevel));
//    		
//    		rtn.put(entry.getKey(), nextIndex);
//    		nextIndex++;
//    	}
//    	
//    	return rtn;
//    }
    
    public Map<String, Integer> ensureGeneIndex(Collection<String> genes){
    	return ensureCollectionIndex(genes, GENE_INDEX_COL_ID);
    }
    
    /**
     * Call this method to make ensure all keys in the list have been persisted in the passed in collection.
     * Collection should be a single document of key:value pairss
     * NOTE: Document maximum size is 16Mb
     * @param genes
     * @return
     */
    private Map<String, Integer> ensureCollectionIndex(Collection<String> genes, String collectionName) {
        MongoCollection<Document> collection = database.getCollection(collectionName);
        Document document = collection.find().first();
        // Only one document is expected in this collection
        if (document == null) {
            document = new Document();
            collection.insertOne(document); 
        }
        // Existing content
        Map<String, Object> originalMap = new HashMap<>();
        // It may be an ObjectId
        document.forEach((gene, index) -> originalMap.put(recoverDotInGene(gene), index));
        List<String> toBePersisted = new ArrayList<>();
        Map<String, Integer> rtn = new HashMap<>();
        for (String gene : genes) {
            Object obj = originalMap.get(gene);
            if (obj == null) {
                toBePersisted.add(gene);
            }
            else {
                rtn.put(gene, (Integer)obj); // If this is a gene, the value should be an integer
            }
        }
        int nextIndex = originalMap.size();
        for (String gene : toBePersisted) {
            collection.updateOne(Filters.eq("_id", document.get("_id")), 
                                 Updates.set(escapeDotInGene(gene), nextIndex));
            rtn.put(gene, nextIndex);
            nextIndex ++;
        }
        return rtn;
    }
    
    /**
     * Adds a digital key for use in fetching data desc ids. digital keys are used to provide shorter ids in queries. 
     * !!IMPORTANT: Counter starts at 1 because 0 is reserved for combined_score, which is not a listed data description. 
     */
    public void addDataDescDigitalKeys() {
    	MongoCollection<Document> collection = database.getCollection(DATA_DESCRIPTIONS_COL_ID);
    	MongoCursor<Document> cursor = collection.find().iterator();
    	
    	int counter = 1;
    	

    	try {
    		while(cursor.hasNext()) {
    			//use put in case docs already contain a key so that no key is duplicated on re-run. 
    			//Put ensures old values are overwritten.
    			collection.updateOne(Filters.eq(cursor.next().get("_id")), Updates.set("digitalKey", counter));
    			counter++;
    		}
    	} finally {
    		cursor.close();
    	}
    }
    
    /**
     * Dot cannot be used in a key name. Use its escape to escape it.
     * See https://stackoverflow.com/questions/37987299/insert-field-name-with-dot-in-mongo-document.
     * @param gene
     * @return
     */
    private String escapeDotInGene(String gene) {
        if (!gene.contains("."))
            return gene;
        gene = gene.replaceAll("\\.", "\\u002E");
        return gene;
    }
    
    private String recoverDotInGene(String gene) {
        gene = gene.replaceAll("\\u002E", ".");
        return gene;
    }

    public void insertPairwise(PairwiseRelationship rel) {
        if (rel.isEmpty()) {
            logger.info("Nothing to be inserted for " + rel.getGene() + " in " + rel.getDataDesc().getId());
            return; 
        }
        MongoCollection<Document> collection = database.getCollection(RELATIONSHIP_COL_ID);
        ensureCollectionDoc(collection, rel.getGene());
        // Need to push the values via a Document
        Document relDoc = new Document();
        if (rel.getPos() != null && rel.getPos().size() > 0) 
            relDoc.append("pos", rel.getPos());
        if (rel.getNeg() != null && rel.getNeg().size() > 0)
            relDoc.append("neg", rel.getNeg());
        collection.updateOne(Filters.eq("_id", rel.getGene()),
                             Updates.set(rel.getDataDesc().getId(), relDoc));
        logger.debug("Insert: " + rel.getDataDesc().getId() + " for " + rel.getGene() + ".");
    }

//    public void insertPathwayRelationships(Map<String, List<Integer>> pathwayRelationships) {
//    	logger.info("Inserting pathway relationships for " + pathwayRelationships.keySet().size() + " pathways.");
//    	MongoCollection<Document> collection = database.getCollection(PATHWAYS_COL_ID);
//    	pathwayRelationships.forEach((pathway, geneList) -> {
//    		ensureCollectionDoc(collection, pathway);
//    		collection.updateOne(Filters.eq("_id", pathway), Updates.set("genes", geneList));
//    	});
//    	logger.info("Inserting patwhay relationships complete");
//    }
    
    public void clearCombinedScores() {
    	MongoCollection<Document> collection =  database.getCollection(RELATIONSHIP_COL_ID);
    	collection.updateMany(new BasicDBObject(), Updates.unset(COMBINED_SCORE));
    }
    
    /**
     * Insert combined score for gene pair from gene1 to gene2 and reverse.
     */
    public void insertCombinedScore(Collection<GeneCombinedScore> geneCombinedScores) {
    	
    	
		Map<String, Integer> geneToIndex = getIndexToGene().entrySet().stream().collect(Collectors.toMap(Entry::getValue, Entry::getKey));
    	MongoCollection<Document> collection = database.getCollection(RELATIONSHIP_COL_ID);
		
    	geneCombinedScores.forEach(obj -> {
    		ensureCollectionDoc(collection, obj.getGene());
    		Document combinedScoresDoc = new Document();
    		obj.getInteractorToScore().forEach((gene,prd) -> {
    			combinedScoresDoc.append(geneToIndex.get(gene)+"", prd);
    		});
    		collection.updateOne(Filters.eq("_id", obj.getGene()), Updates.set(COMBINED_SCORE, combinedScoresDoc));
    	});
    }
    
//    /**
//     * Collect keyset of each argument for loop to ensure nothing is missed
//     * @param geneToPathwayList
//     * @param geneToSecondPathway
//     */
//    public void insertGeneRelationships(Map<String, Set<Integer>> geneToPathwayList) {
//    	logger.info("Inserting gene relationships");
//    	
//    	MongoCollection<Document> collection = database.getCollection(PATHWAYS_COL_ID);
//    	
//    	Set<String> genes = new HashSet<>(geneToPathwayList.keySet());
//    	genes.forEach((gene) -> {
//    		ensureCollectionDoc(collection, gene);
//    		if(geneToPathwayList.get(gene) != null && geneToPathwayList.get(gene).size() > 0) 
//    			collection.updateOne(Filters.eq("_id", gene), Updates.set("pathways", geneToPathwayList.get(gene)));
//    	});
//    	
//    	logger.info(collection.count() + " documents in pathways collection");
//    	
//    }
    
    public Map<Integer, String> getIndexToGene() {
        if (indexToGene != null)
            return indexToGene;
        indexToGene = makeValKeyMapFromDoc(database.getCollection(GENE_INDEX_COL_ID).find().first());
        return indexToGene;
    }
    
    public Set<String> getPathwayStIdsForTerm(String term) {
		term = this.getGeneForTerm(term);
		return pathwayService.getGeneToPathwayStId(this.getUniProtToGene()).get(term);
		
	}
    
//    public Map<Integer, Pathway> getIndexToPathway() {
//    	if(indexToPathway != null)
//    		return indexToPathway;
//    	MongoCursor<Document> cursor = database.getCollection(PATHWAY_INDEX_COL_ID).find().iterator();
//    	indexToPathway = new HashMap<>();
//    	
//    	while(cursor.hasNext()) {
//    		Document nextDoc = cursor.next();
//    		indexToPathway.put(nextDoc.getInteger("index"), new Pathway(nextDoc.getString("_id"), nextDoc.getString("name"), nextDoc.getBoolean("bottomLevel")));
//    	}
//    	return indexToPathway;
//    }
//    
//    public Map<String, String> getPathwayNameToStId(){
//    	if(pathwayStIdToName != null)
//    		return pathwayStIdToName;
//    	
//    	MongoCursor<Document> cursor = database.getCollection(PATHWAY_INDEX_COL_ID).find().iterator();
//    	pathwayStIdToName = new HashMap<>();
//    	
//    	while(cursor.hasNext()) {
//    		Document nextDoc = cursor.next();
//    		pathwayStIdToName.put(nextDoc.getString("name"), nextDoc.getString("_id"));
//    	}
//    	
//    	return pathwayStIdToName;
//    }

    /**
     * Consumes a document of key:value pairs.
     * Returns a Map<Integer, String> of value to key
     * @param doc
     * @return
     */
    private Map<Integer, String> makeValKeyMapFromDoc(Document doc){
    	Map<Integer, String> rtn = new HashMap<>();
    	for(String key : doc.keySet()) {
    		Object value = doc.get(key);
    		if(value instanceof Integer)
    			rtn.put((Integer)value, key);
    	}
    	return rtn;
    }
    
    private Document ensureCollectionDoc(MongoCollection<Document> collection,
                               String gene) {
        // There should be only one document
        Document geneDoc = collection.find(Filters.eq("_id", gene)).first();
        if (geneDoc != null)
            return geneDoc;
        // Need to create one document
        geneDoc = new Document().append("_id", gene);
        collection.insertOne(geneDoc);
        return geneDoc;
    }

    public void insertDataDesc(DataDesc desc) {
        MongoCollection<Document> collection = database.getCollection(DATA_DESCRIPTIONS_COL_ID);
        // Check if this document has been inserted already
        Document document = collection.find(Filters.eq("_id", desc.getId())).first();
        if (document != null) {
            logger.info("Document has been in the database: " + desc.getId());
            return;
        }
        document = new Document();
        document.append("_id", desc.getId())
                .append("bioSource", desc.getBioSource())
                .append("dataType", desc.getDataType().toString())
                .append("provenance", desc.getProvenance())
        		.append("digitalKey", collection.count()+1); //digitalKey is for accession with shorter filter than _id
        if (desc.getOrigin() != null)
            document.append("origin", desc.getOrigin());
        collection.insertOne(document);
        logger.info("Inserted DataDesc: " + desc.getId());
    }
    
    public void insertReactomeAnnotatedGenes(Set<String> annotatedGenes) {
		MongoCollection<Document> collection = database.getCollection(this.REACTOME_ANNOTATED_GENES_COL_ID);
		Document doc = new Document();
		doc.append("reactomeAnnotatedGenes", annotatedGenes);
		collection.insertOne(doc);
	}
    
    /*
     * inserts data into the reactome_pathways collection.
     * Each pathway document contains a set of gene indexes for genes contained in the pathway
     * and a Target Development Level score based on the weighted average
     * of the Target Development Levels of each gene in the pathway.
     */
    public void insertReactomePathwayCache(Collection<Pathway> pathways) {
    	Map<String, Integer> geneToIndex = this.getIndexToGene().entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    	
    	List<Document> docs = new ArrayList<>();
    	pathways.forEach(pw->{
    		List<Integer> indexList = pw.getGenes().stream().map(gene -> geneToIndex.get(gene)).collect(Collectors.toList());
    		List<Document> pathwayOverlapDocs = new ArrayList<>();
    		pw.getStIdToHypergeometricScoreMap().values().forEach(overlap ->{
    			Document doc = new Document();
    			doc.append("stId", overlap.getStId());
    			doc.append("hypergeometricScore", overlap.getHypergeometricScore());
    			doc.append("numberOfSharedGenes", overlap.getNumberOfSharedGenes());
    			pathwayOverlapDocs.add(doc);
    		});
    		//build document and add to docs list
    		Document doc = new Document();
    		doc.append("_id", pw.getStId());
    		doc.append("weighted_tdl_average", pw.getWeightedTDL());
    		doc.append("overlapping_pathways", pathwayOverlapDocs);
    		doc.append("gene_list", indexList);
    		docs.add(doc);
    	});
    	//add collection of documents to database all at once
    	database.getCollection(REACTOME_PATHWAYS_CACHE_COL_ID).insertMany(docs);
    }
    
    public void regenrateReactomeAnnotatedGenesCollection() {
    	database.getCollection(REACTOME_ANNOTATED_GENES_COL_ID).drop();
    	database.createCollection(REACTOME_ANNOTATED_GENES_COL_ID);
    }
    
    public void regenerateReactomePathwaysCacheCollection() {
    	database.getCollection(REACTOME_PATHWAYS_CACHE_COL_ID).drop();
    	database.createCollection(REACTOME_PATHWAYS_CACHE_COL_ID);
    }

//	public void regeneratePathwayCollections() {
//		database.getCollection(this.PATHWAY_INDEX_COL_ID).drop();
//		database.getCollection(this.PATHWAYS_COL_ID).drop();
//		database.createCollection(this.PATHWAY_INDEX_COL_ID);
//		database.createCollection(this.PATHWAYS_COL_ID);
//	}
    
	/**
	 * Generate a list of pathways that are ordered based on their locations in the pathway
	 * hierarchy so that similar pathways are grouped together. This method uses a width-first-search
	 * algorithm to get the list.
	 * @return
	 * @throws Exception
	 */
    @SuppressWarnings("unchecked")
    public synchronized List<GraphPathway> getHierarchicalOrderedPathways() {
    	if (pathways != null)
    		return pathways;
    	try {
    		URL url = new URL(config.getEventHierarchyUrl());
    		ObjectMapper mapper = new ObjectMapper();
    		List<GraphPathway> topPathways = mapper.readValue(url,
    				new TypeReference<List<GraphPathway>>() {
    		});
    		List<GraphPathway> pathways = new ArrayList<>();
    		// Some pathways may be listed under multiple topics. For this application,
    		// we need to list them once only by choosing whatever they occur first.
    		// Use this set to keep tracking pathways based on names.
    		Set<String> listedPathways = new HashSet<>();
    		for (GraphPathway topPathway : topPathways) {
    			traversePathway(topPathway, topPathway, pathways, listedPathways);
    		}
    		// Get rid of some values that don't need at the front-end
    		pathways.forEach(p -> {
    			p.setChildren(null);
    			p.setSpecies(null);
    			p.setType(null);
    			p.setDiagram(false);
    		});
    		this.pathways = pathways;
    		return pathways;
    	}
    	catch(Exception e) {
    		logger.error(e.getMessage(), e);
    	}
    	return Collections.EMPTY_LIST; // Just return an empty list.
    }
    
	private void traversePathway(GraphPathway topPathway,
								GraphPathway currentPathway,
					            List<GraphPathway> list,
					            Set<String> listedPathways) {
		if (!currentPathway.getType().equals("Pathway") &&
				!currentPathway.getType().equals("TopLevelPathway"))
				return; // Check pathways only
		if (listedPathways.contains(currentPathway.getName()))
				return; // Added to the list already
		currentPathway.setTopPathway(topPathway.getName());
		list.add(currentPathway);
		listedPathways.add(currentPathway.getName());
		if (currentPathway.getChildren() == null)
				return; // No need to go down.
		for (GraphPathway child : currentPathway.getChildren())
				traversePathway(topPathway, child, list, listedPathways);
	}
}
