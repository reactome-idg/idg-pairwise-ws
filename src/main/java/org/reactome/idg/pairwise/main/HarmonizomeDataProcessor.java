package org.reactome.idg.pairwise.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.reactome.idg.pairwise.model.DataDesc;
import org.reactome.idg.pairwise.model.DataType;
import org.reactome.idg.pairwise.model.PairwiseRelationship;
import org.reactome.idg.pairwise.service.PairwiseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HarmonizomeDataProcessor implements PairwiseDataProcessor {
    private final static Logger logger = LoggerFactory.getLogger(HarmonizomeDataProcessor.class);

    public HarmonizomeDataProcessor() {
    }

    @Override
    public boolean isCorrectFile(String fileName) {
        return fileName.endsWith("_filtered.txt");
    }

    @Override
    public DataDesc createDataDesc(String fileName) {
        DataDesc desc = new DataDesc();
        desc.setProvenance("Harmonizome");
        desc.setDataType(DataType.Gene_Similarity);
        // Need to get the origin from the file name
        int index = fileName.indexOf("_");
        String origin = fileName.substring(0, index);
        desc.setOrigin(origin);
        // Use human for all data collected from harmonizome
        desc.setBioSource("human");
        return desc;
    }

    @Override
    public void processFile(String fileName, String dirName, DataDesc desc, PairwiseService service) throws Exception {
        // Load all genes on one shot first
        List<String> totalGenes = loadGenes(fileName, dirName);
        Map<String, Integer> geneToIndex = service.ensureGeneIndex(totalGenes);
        logger.info("Total indexed genes: " + geneToIndex.size());
        // Load relationships
        Map<String, List<Integer>> posRels = new HashMap<>();
        Map<String, List<Integer>> negRels = new HashMap<>();
        loadRelationships(fileName, dirName, posRels, negRels, geneToIndex);
        logger.info("Relationships have been loaded.");
        
        long time1 = System.currentTimeMillis();
        int c = 0;
        for (String gene : geneToIndex.keySet()) {
            PairwiseRelationship rel = new PairwiseRelationship();
            rel.setGene(gene);
            rel.setPos(posRels.get(gene));
            rel.setNeg(posRels.get(gene));
            if (rel.getNeg() == null && rel.getPos() == null) {
                continue;
            }
            rel.setDataDesc(desc);
            service.insertPairwise(rel);
            c ++;
        }
        long time2 = System.currentTimeMillis();
        logger.info("Total inserted genes: " + c);
        logger.info("Total time: " + (time2 - time1) / (1000.0 * 60.0d) + " minutes.");
    }

    private void loadRelationships(String fileName, 
                                   String dirName,
                                   Map<String, List<Integer>> posRels,
                                   Map<String, List<Integer>> negRels,
                                   Map<String, Integer> geneToIndex) throws IOException {
        FileReader fr = new FileReader(dirName + File.separator + fileName);
        BufferedReader br = new BufferedReader(fr);
        String line = null;
        while ((line = br.readLine()) != null) {
//            System.out.println(line);
            String[] tokens = line.split("\t");
            if (tokens[2].equals("+")) 
                pushRel(tokens, posRels, geneToIndex);
            else if (tokens[2].equals("-")) 
                pushRel(tokens, negRels, geneToIndex);
        }
        br.close();
        fr.close();
    }
    
    private void loadRelationships(String fileName, 
                                   String dirName) throws IOException {
        List<String> genes = loadGenes(fileName, dirName);
        logger.info("Total genes loaded: " + genes.size());
        Map<String, Integer> geneToIndex = new HashMap<>();
        for (int i = 0; i < genes.size(); i++)
            geneToIndex.put(genes.get(i), i);
        FileReader fr = new FileReader(dirName + File.separator + fileName);
        BufferedReader br = new BufferedReader(fr);
        String line = null;
        Map<String, List<Integer>> posRels = new HashMap<>();
        Map<String, List<Integer>> negRels = new HashMap<>();
        while ((line = br.readLine()) != null) {
            String[] tokens = line.split("\t");
            if (tokens[2].equals("+")) 
                pushRel(tokens, posRels, geneToIndex);
            else if (tokens[2].equals("-")) 
                pushRel(tokens, negRels, geneToIndex);
        }
        br.close();
        fr.close();
    }
    
    protected List<String> loadGenes(String fileName, String dirName) throws IOException {
        FileReader fr = new FileReader(dirName + File.separator + fileName);
        BufferedReader br = new BufferedReader(fr);
        String line = null;
        Set<String> genes = new HashSet<>();
        while ((line = br.readLine()) != null) {
            String[] tokens = line.split("\t");
            genes.add(tokens[0]);
            genes.add(tokens[1]);
        }
        br.close();
        fr.close();
        List<String> rtn = new ArrayList<>(genes);
        Collections.sort(rtn);
        return rtn;
    }
    
    @Test
    public void testLoadRelationships() throws IOException {
        String fileName = "locatepredicted_filtered.txt";
        String dirName = "/Users/wug/datasets/Harmonizome/download/gene_similarity_matrix_cosine";
        loadRelationships(fileName, dirName);
    }
    
    protected void pushRel(String[] tokens, Map<String, List<Integer>> map, Map<String, Integer> geneToIndex) {
        map.compute(tokens[0], (key, set) -> {
            if (set == null)
                set = new ArrayList<>();
            set.add(geneToIndex.get(tokens[1]));
            return set;
        });
        // Need to add another relationship
        map.compute(tokens[1], (key, set) -> {
            if (set == null)
                set = new ArrayList<>();
            set.add(geneToIndex.get(tokens[0]));
            return set;
        });
    }

}
