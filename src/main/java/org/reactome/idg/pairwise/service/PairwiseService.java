package org.reactome.idg.pairwise.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.bson.Document;
import org.junit.Test;
import org.reactome.annotate.AnnotationType;
import org.reactome.annotate.GeneSetAnnotation;
import org.reactome.idg.model.*;
import org.reactome.idg.pairwise.model.DataDesc;
import org.reactome.idg.pairwise.model.PEsForInteractorResponse;
import org.reactome.idg.pairwise.model.PairwiseRelationship;
import org.reactome.idg.pairwise.model.Pathway;
import org.reactome.idg.pairwise.model.PathwayToGeneRelationship;
import org.reactome.idg.pairwise.model.pathway.GraphHierarchy;
import org.reactome.idg.pairwise.model.pathway.GraphPathway;
import org.reactome.idg.pairwise.model.pathway.HierarchyResponseWrapper;
import org.reactome.idg.pairwise.web.errors.InternalServerError;
import org.reactome.idg.pairwise.web.errors.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final String DATA_DESCRIPTIONS_COL_ID = "datadescriptions";
    private final String GENE_INDEX_COL_ID = "GENE_INDEX";
    private final String RELATIONSHIP_COL_ID = "relationships";
    private final String UNIPROT_TO_GENE_FILE_NAME = "GeneToUniProt.txt";
    private final String PATHWAY_INDEX_COL_ID = "PATHWAY_INDEX";
    private final String PATHWAYS_COL_ID = "pathways";

    private static final Logger logger = LoggerFactory.getLogger(PairwiseService.class);
    
    @Autowired
    private MongoDatabase database;
    
    @Autowired
    private PairwiseServiceConfig config;
    
    // Cached index to gene for performance
    private Map<Integer, String> indexToGene;
    //Cached index to pathway for performance
    private Map<Integer,Pathway> indexToPathway;
    //TODO: One-to-one mapping between UniProt and gene symbols are most likely not right.
    // This should be improved in the future.
    // Cached uniprot to gene mapping
    private Map<String, String> uniprotToGene;
    private Map<String, String> geneToUniprot;
    private Map<String, String> pathwayStIdToName;
    
    //catched EventHierarchy
    private GraphHierarchy graphHierarchy;

    public PairwiseService() {
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
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
			throw new InternalServerError("Internal Server Error");
		}
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
    
    public PEsForInteractorResponse queryPEsForUniprotInteractor(Long dbId, String gene, List<String> dataDescs) throws IOException{
    	String term = this.getUniProtToGene().get(gene);
    	
    	return queryPEsForInteractor(dbId, term, dataDescs);
    }
    
    public PEsForInteractorResponse queryPEsForInteractor(Long dbId, String gene, List<String> dataDescs) throws IOException {
		
    	//get pairwise doc for gene and throw exception if no doc found.
    	Document interactorsDoc = getRelationshipDocForGene(gene);
    	
    	Map<String, List<Long>> geneToPEMap = callGeneToIdsInPathwayDiagram(dbId);
    	if(geneToPEMap == null) {
    		String errormsg = "Physical Entity Map could not be created.";
    		logger.error(errormsg);
    		throw new InternalServerError("Physical Entity Map could not be created.");
    	}
    	    	
    	Set<String> interactorGenes = new HashSet<>();
    	for(String key : interactorsDoc.keySet()) {
    		if(!dataDescs.contains(key)) continue;
    		Document dataDoc = (Document) interactorsDoc.get(key);
    		interactorGenes.addAll(getGenesFromRelDoc(dataDoc));
    	}
    	
    	Set<Long> peIds = new HashSet<>();
    	interactorGenes.forEach(geneName -> {
    		if(geneToPEMap.containsKey(geneName))
    			peIds.addAll(geneToPEMap.get(geneName));
    	});
    	
    	PEsForInteractorResponse rtn = new PEsForInteractorResponse(new ArrayList<>(peIds), new ArrayList<>(interactorGenes));
    	
		return rtn;
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
 		return structureGeneToPEMap(method.getResponseBodyAsString());
 	}
     
    private Map<String, List<Long>> structureGeneToPEMap(String responseBodyAsString)  throws IOException{
		ObjectMapper mapper = new ObjectMapper();
		
		JsonNode response = mapper.readTree(responseBodyAsString);
		if(!response.get("geneToPEIds").isArray()) return null;
		
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
            rel.setPosGenes(geneList);
        }
        indexList = (List<Integer>) relDoc.get("neg");
        if (indexList != null) {
            List<String> geneList = indexList.stream()
                    .map(i -> indexToGene.get(i))
                    .collect(Collectors.toList());
            rel.setNegGenes(geneList);
        }
    }
    
    /**
     * Returns document from relationships doc for gene
     * @param gene
     * @return
     */
    private Document getRelationshipDocForGene(String gene) {
    	Document doc = database.getCollection(RELATIONSHIP_COL_ID)
    			.find(Filters.eq("_id", gene)).first();
    	if(doc == null) {
    		String infomsg = "No interactors found for gene: " + gene;
    		logger.info(infomsg);
    		throw new ResourceNotFoundException(infomsg);
    	}
    	return doc;
    }
    
    private Set<String> getGenesFromRelDoc(Document relDoc){
    	Set<String> rtn = new HashSet<>();
    	
    	Map<Integer, String> indexToGene = getIndexToGene();
    	    	
		if(relDoc.containsKey("pos"))
			rtn.addAll(((List<Integer>)relDoc.get("pos")).stream().map(i -> indexToGene.get(i)).collect(Collectors.toSet()));
		if(relDoc.containsKey("neg"))
			rtn.addAll(((List<Integer>)relDoc.get("neg")).stream().map(i -> indexToGene.get(i)).collect(Collectors.toSet()));	
    	
    	return rtn;
    }
    
    public HierarchyResponseWrapper queryHierarchyForGene(String gene) {
    	if(graphHierarchy == null) loadGraphHierarchy();
    	
    	List<String> stIds = queryPrimaryPathwaysForGene(gene).stream().map(Pathway::getStId).collect(Collectors.toList());
    	return new HierarchyResponseWrapper(gene, stIds, graphHierarchy.getBranches(stIds));
    }

	public List<Pathway> queryPrimaryPathwaysForGene(String gene) {
		Map<Integer, Pathway> indexToPathway = getIndexToPathway();
		Document doc = database.getCollection(PATHWAYS_COL_ID).find(Filters.eq("_id", gene)).first();
		if(doc == null) return new ArrayList<>();
		
		List<Integer> indexList=(List<Integer>) doc.get("pathways");
		if(indexList == null) return new ArrayList<>();
		
		List<Pathway> pathways = indexList.stream().map(i -> indexToPathway.get(i)).collect(Collectors.toList());
		
		return pathways;
	}
    
    public List<Pathway> queryPrimaryPathwaysForUniprot(String uniprot) {
    	String gene = this.getUniProtToGene().get(uniprot);
    	if(gene == null) return null;
    	List<Pathway> rtn = this.queryPrimaryPathwaysForGene(gene);
    	
    	//null check
    	if(rtn ==  null) return null;
    			
    	return rtn;
    }
    
    public List<Pathway> queryUniprotToSecondaryPathwaysWithEnrichment(String uniprot, List<String> dataDescs) {
    	String gene = this.getUniProtToGene().get(uniprot);
    	if(gene == null) return null;
    	List<Pathway> rtn = this.queryGeneToSecondaryPathwaysWithEnrichment(gene, dataDescs);
    	
    	//null check
    	if(rtn ==  null || rtn.size() == 0) return null;
    			
    	return rtn;
	}
    
    public List<Pathway> queryGeneToSecondaryPathwaysWithEnrichment(String gene, List<String> descIds) {
    	Document relDoc = getRelationshipDocForGene(gene);
    	    	
    	Set<String> interactors = new HashSet<>();
    	for(String key : relDoc.keySet()) {
    		if(!descIds.contains(key)) continue;
    		Document doc = (Document) relDoc.get(key);
    		interactors.addAll(getGenesFromRelDoc(doc));
    	}
    	//if there are no interactors
    	if(interactors.size() < 1) {
    		return new ArrayList<>();
    	}
    	
    	//get enrichment analysis results
    	List<GeneSetAnnotation> annotations = performEnrichment(interactors, gene);
    	
    	//convert enrichment analysis results into a list of Pathway objects
    	List<Pathway> rtnPathways = new ArrayList<>();
    	Map<String, String> stIdToPathwayName = this.getPathwayNameToStId().entrySet().stream().collect(Collectors.toMap(Entry::getValue, Entry::getKey));
    	annotations.forEach(annotation -> {
    		String stId = annotation.getTopic();
    		rtnPathways.add(new Pathway(stId,
    									stIdToPathwayName.get(stId),
    									Double.parseDouble(annotation.getFdr()),
    									annotation.getPValue(),
    									isBottomLevel(stId)));
    	});
    	
    	return rtnPathways;
    }
    
    private boolean isBottomLevel(String stId) {
		Document doc = database.getCollection(PATHWAY_INDEX_COL_ID).find(Filters.eq("_id", stId)).first();
		return doc.getBoolean("bottomLevel", false);
	}

	private List<GeneSetAnnotation> performEnrichment(Set<String> interactors, String gene){
    	List<GeneSetAnnotation> annotations;
    	try {
    		annotations = config.getAnnotator().annotateGenesWithFDR(interactors, AnnotationType.Pathway);
    	} catch(Exception e) {
    		logger.error(e.getMessage());
    		e.printStackTrace();
    		throw new InternalServerError("Could not annotate interactors for " + gene);
    	}
    	return annotations;
    }
    
    public PathwayToGeneRelationship queryPathwayToGeneRelationships(String stId) {
    	Map<Integer, String> indexToGene = getIndexToGene();
    	PathwayToGeneRelationship rtn = new PathwayToGeneRelationship();
    	rtn.setPathwayStId(stId);
    	
    	Document doc = database.getCollection(PATHWAYS_COL_ID).find(Filters.eq("_id", stId)).first();
    	//make sure doc exists
    	if(doc == null) return null;
    	
    	List<Integer> indexList = (List<Integer>)doc.get("genes");
    	if(indexList != null) {
    		List<String> geneList = indexList.stream().map(i -> indexToGene.get(i)).collect(Collectors.toList());
    		rtn.setGenes(geneList);
    	}

    	return rtn;
    }
    
    public PathwayToGeneRelationship queryPathwayToUniprotRelationships(String stId) {
		Map<String, String> geneToUniprot = getGeneToUniProt();
		PathwayToGeneRelationship rtn = queryPathwayToGeneRelationships(stId);
		if(rtn == null) return null;
		List<String> uniprotList = rtn.getGenes().stream().map(i -> geneToUniprot.get(i)).collect(Collectors.toList());
		rtn.setGenes(uniprotList);
		return rtn;
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
            DataDesc desc = new DataDesc();
            Object value = doc.get("_id");
            desc.setId((String)value);
            value = doc.get("bioSource");
            if (value != null)
                desc.setBioSource((String)value);
            value = doc.get("provenance");
            if (value != null)
                desc.setProvenance((String)value);
            value = doc.get("dataType");
            if (value != null)
                desc.setDataType(FeatureType.valueOf((String)value));
            value = doc.get("origin");
            if (value != null)
                desc.setOrigin((String)value);
            rtn.add(desc);
        }
        return rtn;
    }

    /**
     * Do nothing for the time being since we are using the primary index. If needed,
     * indexing will be handled manually via the shell.
     */
    public void performIndex() {
    }
    
    public Map<String, Integer> ensurePathwayIndex(Map<String, String> pathwayStIdToPathwayName, Set<String> bottomPathways){
    	Map<String, Integer> rtn = new HashMap<>();
    	MongoCollection<Document> collection = database.getCollection(PATHWAY_INDEX_COL_ID);
    	int nextIndex = 0;
    	for(Entry<String,String> entry : pathwayStIdToPathwayName.entrySet()){
    		ensureCollectionDoc(collection, entry.getKey());
    		collection.updateOne(Filters.eq("_id", entry.getKey()), Updates.set("index", nextIndex));
    		collection.updateOne(Filters.eq("_id", entry.getKey()), Updates.set("name", entry.getValue()));
    		
    		boolean bottomLevel = bottomPathways.contains(entry.getKey()) ? true:false; //make boolean that is true if entry pathway is a bottom level pathway
    		collection.updateOne(Filters.eq("_id", entry.getKey()), Updates.set("bottomLevel", bottomLevel));
    		
    		rtn.put(entry.getKey(), nextIndex);
    		nextIndex++;
    	}
    	
    	return rtn;
    }
    
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

    public void insertPathwayRelationships(Map<String, List<Integer>> pathwayRelationships) {
    	logger.info("Inserting pathway relationships for " + pathwayRelationships.keySet().size() + " pathways.");
    	MongoCollection<Document> collection = database.getCollection(PATHWAYS_COL_ID);
    	pathwayRelationships.forEach((pathway, geneList) -> {
    		ensureCollectionDoc(collection, pathway);
    		collection.updateOne(Filters.eq("_id", pathway), Updates.set("genes", geneList));
    	});
    	logger.info("Inserting patwhay relationships complete");
    }
    
    /**
     * Collect keyset of each argument for loop to ensure nothing is missed
     * @param geneToPathwayList
     * @param geneToSecondPathway
     */
    public void insertGeneRelationships(Map<String, Set<Integer>> geneToPathwayList) {
    	logger.info("Inserting gene relationships");
    	
    	MongoCollection<Document> collection = database.getCollection(PATHWAYS_COL_ID);
    	
    	Set<String> genes = new HashSet<>(geneToPathwayList.keySet());
    	genes.forEach((gene) -> {
    		ensureCollectionDoc(collection, gene);
    		if(geneToPathwayList.get(gene) != null && geneToPathwayList.get(gene).size() > 0) 
    			collection.updateOne(Filters.eq("_id", gene), Updates.set("pathways", geneToPathwayList.get(gene)));
    	});
    	
    	logger.info(collection.count() + " documents in pathways collection");
    	
    }
    
    public Map<Integer, String> getIndexToGene() {
        if (indexToGene != null)
            return indexToGene;
        indexToGene = makeValKeyMapFromDoc(database.getCollection(GENE_INDEX_COL_ID).find().first());
        return indexToGene;
    }
    
    public Map<Integer, Pathway> getIndexToPathway() {
    	if(indexToPathway != null)
    		return indexToPathway;
    	MongoCursor<Document> cursor = database.getCollection(PATHWAY_INDEX_COL_ID).find().iterator();
    	indexToPathway = new HashMap<>();
    	
    	while(cursor.hasNext()) {
    		Document nextDoc = cursor.next();
    		indexToPathway.put(nextDoc.getInteger("index"), new Pathway(nextDoc.getString("_id"), nextDoc.getString("name"), nextDoc.getBoolean("bottomLevel")));
    	}
    	return indexToPathway;
    }
    
    public Map<String, String> getPathwayNameToStId(){
    	if(pathwayStIdToName != null)
    		return pathwayStIdToName;
    	
    	MongoCursor<Document> cursor = database.getCollection(PATHWAY_INDEX_COL_ID).find().iterator();
    	pathwayStIdToName = new HashMap<>();
    	
    	while(cursor.hasNext()) {
    		Document nextDoc = cursor.next();
    		pathwayStIdToName.put(nextDoc.getString("name"), nextDoc.getString("_id"));
    	}
    	
    	return pathwayStIdToName;
    }


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
    
    private void ensureCollectionDoc(MongoCollection<Document> collection,
                               String gene) {
        // There should be only one document
        Document geneDoc = collection.find(Filters.eq("_id", gene)).first();
        if (geneDoc != null)
            return;
        // Need to create one document
        geneDoc = new Document().append("_id", gene);
        collection.insertOne(geneDoc);
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
                .append("provenance", desc.getProvenance());
        if (desc.getOrigin() != null)
            document.append("origin", desc.getOrigin());
        collection.insertOne(document);
        logger.info("Inserted DataDesc: " + desc.getId());
    }

	public void regeneratePathwayCollections() {
		database.getCollection(this.PATHWAY_INDEX_COL_ID).drop();
		database.getCollection(this.PATHWAYS_COL_ID).drop();
		database.createCollection(this.PATHWAY_INDEX_COL_ID);
		database.createCollection(this.PATHWAYS_COL_ID);
	}
}
