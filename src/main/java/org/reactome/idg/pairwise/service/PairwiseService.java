package org.reactome.idg.pairwise.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.reactome.idg.pairwise.model.DataDesc;
import org.reactome.idg.pairwise.model.DataType;
import org.reactome.idg.pairwise.model.PairwiseRelationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryRow;

@Service
public class PairwiseService {
    private final String GENE_INDEX_DOC_ID = "GENE_INDEX";
    
    private static final Logger logger = LoggerFactory.getLogger(PairwiseService.class);
    
    @Autowired
    private Bucket bucket;
    
    public PairwiseService() {
    }
    
    public List<DataDesc> listDataDesc() {
        N1qlQuery query = N1qlQuery.simple("SELECT meta().id, provenance, dataType, bioSource "
                + "from `" + bucket.name() + "` WHERE _class = '" + DataDesc.class.getSimpleName() + "'");
        List<N1qlQueryRow> rows = bucket.query(query).allRows();
        List<DataDesc> rtn = new ArrayList<>();
        for (N1qlQueryRow row : rows) {
            Map<String, Object> map = row.value().toMap();
            DataDesc desc = new DataDesc();
            desc.setId((String)map.get("id"));
            desc.setBioSource((String)map.get("bioSource"));
            desc.setDataType(DataType.valueOf((String)map.get("dataType")));
            desc.setProvenance((String)map.get("provenance"));
            rtn.add(desc);
        }
        return rtn;
    }
    
    public void performIndex() {
        bucket.bucketManager().createN1qlPrimaryIndex(true, false);
    }
    
    /**
     * Call this method to make ensure all genes in the list have been persited in the database.
     * @param genes
     * @return
     */
    public Map<String, Integer> ensureGeneIndex(List<String> genes) {
        // Check if the index document is there
        JsonDocument indexDoc = bucket.get(GENE_INDEX_DOC_ID);
        if (indexDoc == null)
            bucket.insert(JsonDocument.create(GENE_INDEX_DOC_ID, JsonObject.empty()));
        Map<String, Object> originalMap = null;
        if (indexDoc.content().isEmpty())
            originalMap = new HashMap<>();
        else
            originalMap = indexDoc.content().toMap();
        List<String> toBePersisted = new ArrayList<>();
        Map<String, Integer> rtn = new HashMap<>();
        for (String gene : genes) {
            Object obj = originalMap.get(gene);
            if (obj == null) {
                toBePersisted.add(gene);
            }
            else {
                rtn.put(gene, (Integer)obj);
            }
        }
        int nextIndex = originalMap.size();
        for (String gene : toBePersisted) {
            bucket.mapAdd(GENE_INDEX_DOC_ID, gene, nextIndex);
            rtn.put(gene, nextIndex);
            nextIndex ++;
        }
        return rtn;
    }
    
    public void insertPairwise(PairwiseRelationship rel) {
        JsonDocument doc = ensureGeneDoc(rel);
        if (doc == null) {
            logger.error("No document in the database for " + rel.getGene());
            throw new IllegalStateException("No document in the database for " + rel.getGene());
        }
        JsonObject obj = JsonObject.create();
        if (rel.getNeg() != null && rel.getNeg().size() > 0)
            obj.put("neg", rel.getNeg());
        if (rel.getPos() != null && rel.getPos().size() > 0)
            obj.put("pos", rel.getPos());
        if (obj.isEmpty()) {
            logger.info("Nothing to be inserted for " + rel.getGene() + " in " + rel.getDataDesc().getId());
            return;
        }
        bucket.mapAdd(rel.getGene(), rel.getDataDesc().getId(), obj);
        logger.info("Inserted PairwiseRelationship: " + rel.getGene());
        doc = bucket.get(rel.getGene());
        logger.info(doc.toString());
    }
    
    private JsonDocument ensureGeneDoc(PairwiseRelationship rel) {
        JsonDocument doc = bucket.get(rel.getGene());
        if (doc != null)
            return doc; // This is fine
        // Otherwise create one
        JsonObject obj = JsonObject.create()
                .put("_class", rel.getClass().getSimpleName())
                .put("gene", rel.getGene());
        doc = JsonDocument.create(rel.getGene(), obj);
        doc = bucket.insert(doc);
        return doc;
    }
    
    public void insertDataDesc(DataDesc desc) {
        JsonObject obj = JsonObject.create()
                .put("_class", desc.getClass().getSimpleName())
                .put("bioSource", desc.getBioSource())
                .put("dataType", desc.getDataType().toString())
                .put("provenance", desc.getProvenance());
        JsonDocument doc = JsonDocument.create(desc.getId(), obj);
        bucket.upsert(doc);
        logger.info("Inserted DataDesc: " + desc.getId());
    }

}
