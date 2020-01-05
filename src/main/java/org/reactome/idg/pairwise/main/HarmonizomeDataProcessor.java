package org.reactome.idg.pairwise.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        Map<String, Set<String>> posRels = new HashMap<>();
        Map<String, Set<String>> negRels = new HashMap<>();
        loadRelationships(fileName, dirName, posRels, negRels);
        logger.info("Relationships have been loaded.");
        Set<String> totalGenes = new HashSet<>(posRels.keySet());
        totalGenes.addAll(negRels.keySet());
        Map<String, Integer> geneToIndex = service.ensureGeneIndex(totalGenes.stream().collect(Collectors.toList()));
        logger.info("Total indexed genes: " + geneToIndex.size());
        long time1 = System.currentTimeMillis();
        int c = 0;
        for (String gene : geneToIndex.keySet()) {
            PairwiseRelationship rel = new PairwiseRelationship();
            rel.setGene(gene);
            Set<String> posGenes = posRels.get(gene);
            if (posGenes != null && posGenes.size() > 0) {
                for (String posGene : posGenes) {
                    Integer posIndex = geneToIndex.get(posGene);
                    if (posIndex == null)
                        throw new IllegalStateException(posGene + " is not in the database!");
                    rel.addPos(posIndex);
                }
            }
            Set<String> negGenes = negRels.get(gene);
            if (negGenes != null && negGenes.size() > 0) {
                for (String negGene : negGenes) {
                    Integer negIndex = geneToIndex.get(negGene);
                    if (negIndex == null)
                        throw new IllegalStateException(negGene + " is not in the database!");
                    rel.addNeg(negIndex);
                }
            }
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
                                   Map<String, Set<String>> posRels,
                                   Map<String, Set<String>> negRels) throws IOException {
        FileReader fr = new FileReader(dirName + File.separator + fileName);
        BufferedReader br = new BufferedReader(fr);
        String line = null;
        while ((line = br.readLine()) != null) {
            String[] tokens = line.split("\t");
            if (tokens[2].equals("+")) 
                pushRel(tokens, posRels);
            else if (tokens[2].equals("-")) 
                pushRel(tokens, negRels);
        }
        br.close();
        fr.close();
    }

    private void pushRel(String[] tokens, Map<String, Set<String>> map) {
        map.compute(tokens[0], (key, set) -> {
            if (set == null)
                set = new HashSet<>();
            set.add(tokens[1]);
            return set;
        });
        // Need to add another relationship
        map.compute(tokens[1], (key, set) -> {
            if (set == null)
                set = new HashSet<>();
            set.add(tokens[0]);
            return set;
        });
    }

}
