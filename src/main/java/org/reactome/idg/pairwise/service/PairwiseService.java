package org.reactome.idg.pairwise.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.reactome.idg.pairwise.model.DataDesc;
import org.reactome.idg.pairwise.model.DataType;
import org.reactome.idg.pairwise.model.PairwiseRelationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

@Service
public class PairwiseService {
    private final String DATA_DESCRIPTIONS_COL_ID = "datadescriptions";
    private final String GENE_INDEX_COL_ID = "GENE_INDEX";
    private final String RELATIONSHUP_COL_ID = "relationships";
    
    private static final Logger logger = LoggerFactory.getLogger(PairwiseService.class);
    
    @Autowired
    private MongoDatabase database;
    
    public PairwiseService() {
    }
    
    public void testConnection() {
        FindIterable<Document> documents = database.getCollection("datadescriptions").find();
        for (Document doc : documents)
            System.out.println(doc.toJson());
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
                desc.setDataType(DataType.valueOf((String)value));
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
    
    /**
     * Call this method to make ensure all genes in the list have been persited in the database.
     * @param genes
     * @return
     */
    public Map<String, Integer> ensureGeneIndex(List<String> genes) {
        MongoCollection<Document> collection = database.getCollection(GENE_INDEX_COL_ID);
        Document document = collection.find().first();
        // Only one document is expected in this collection
        if (document == null) {
            document = new Document();
            collection.insertOne(document); 
        }
        // Existing content
        Map<String, Object> originalMap = new HashMap<>();
        // It may be an ObjectId
        document.forEach((gene, index) -> originalMap.put(gene, index));
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
                                 Updates.set(gene, nextIndex));
            rtn.put(gene, nextIndex);
            nextIndex ++;
        }
        return rtn;
    }
    
    public void insertPairwise(PairwiseRelationship rel) {
        if (rel.isEmpty()) {
            logger.info("Nothing to be inserted for " + rel.getGene() + " in " + rel.getDataDesc().getId());
            return; 
        }
        MongoCollection<Document> collection = database.getCollection(RELATIONSHUP_COL_ID);
        ensureGeneDoc(collection, rel.getGene());
        // Need to push the values via a Document
        Document relDoc = new Document();
        if (rel.getPos() != null && rel.getPos().size() > 0) 
            relDoc.append("pos", rel.getPos());
        if (rel.getNeg() != null && rel.getNeg().size() > 0)
            relDoc.append("neg", rel.getNeg());
        collection.updateOne(Filters.eq("_id", rel.getGene()),
                             Updates.set(rel.getDataDesc().getId(), relDoc));
        logger.info("Insert: " + rel.getDataDesc().getId() + " for " + rel.getGene() + ": " + relDoc.toJson());
    }
    
    private void ensureGeneDoc(MongoCollection<Document> collection,
                               String gene) {
        // There shoul dbe only one document
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
        collection.insertOne(document);
        logger.info("Inserted DataDesc: " + desc.getId());
    }

}
