package org.reactome.idg.pairwise.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.reactome.fi.util.InteractionUtilities;
import org.reactome.idg.model.FeatureType;
import org.reactome.idg.pairwise.model.DataDesc;
import org.reactome.idg.pairwise.model.PairwiseRelationship;
import org.reactome.idg.pairwise.service.PairwiseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PPIDataProcessor extends HarmonizomeDataProcessor {
    private static final Logger logger = LoggerFactory.getLogger(PPIDataProcessor.class);
    
    public PPIDataProcessor() {
    }

    @Override
    public boolean isCorrectFile(String fileName) {
        return fileName.endsWith("_PPI.txt");
    }

    @Override
    public DataDesc createDataDesc(String fileName) {
        DataDesc desc = new DataDesc();
        String[] tokens = fileName.split("_");
        desc.setBioSource(tokens[1].toLowerCase());
        desc.setProvenance(tokens[0]);
        desc.setDataType(FeatureType.Protein_Interaction);
        return desc;
    }
    
    @Override
    public void processPairs(Set<String> ppis,
                             DataDesc desc,
                             PairwiseService service) throws Exception {
        Set<String> genes = grepGenesFromPairs(ppis);
        Map<String, Integer> geneToIndex = service.ensureGeneIndex(genes);
        logger.info("Total indexed genes: " + geneToIndex.size());
        // Load relationships
        Map<String, List<Integer>> rels = new HashMap<>();
        loadRelationships(ppis, rels, geneToIndex);
        dumpPairs(rels, desc, service);
    }
    
    @Override
    public void processFile(String fileName, String dirName, DataDesc desc, PairwiseService service) throws Exception {
        // Load all genes on one shot first
        List<String> totalGenes = loadGenes(fileName, dirName);
        Map<String, Integer> geneToIndex = service.ensureGeneIndex(totalGenes);
        logger.info("Total indexed genes: " + geneToIndex.size());
        // Load relationships
        Map<String, List<Integer>> rels = new HashMap<>();
        loadRelationships(fileName, dirName, rels, geneToIndex);
        logger.info("Relationships have been loaded.");
        dumpPairs(rels, desc, service);
    }

    private void dumpPairs(Map<String, List<Integer>> geneToIndices,
                           DataDesc desc,
                           PairwiseService service) {
        long time1 = System.currentTimeMillis();
        int c = 0;
        for (String gene : geneToIndices.keySet()) {
            PairwiseRelationship rel = new PairwiseRelationship();
            rel.setGene(gene);
            // All PPIs are treated as positive relationships here
            rel.setPos(geneToIndices.get(gene));
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
                                   Map<String, List<Integer>> rels,
                                   Map<String, Integer> geneToIndex) throws IOException {
        FileReader fr = new FileReader(dirName + File.separator + fileName);
        BufferedReader br = new BufferedReader(fr);
        String line = null;
        while ((line = br.readLine()) != null) {
//            System.out.println(line);
            String[] tokens = line.split("\t");
            pushRel(tokens, rels, geneToIndex);
        }
        br.close();
        fr.close();
    }
    
    private void loadRelationships(Set<String> pairs,
                                     Map<String, List<Integer>> rels,
                                     Map<String, Integer> geneToIndex) throws IOException {
        for (String pair : pairs) {
            String[] tokens = pair.split("\t");
            pushRel(tokens, rels, geneToIndex);
        }
    }
    
    /**
     * Split Solomon's original merged human PPI files into individual data source files.
     */
    @Test
    public void splitFile() throws IOException {
        String dirName = "/Users/wug/datasets/idg_PPI/";
        String srcFileName = dirName + "overlaps-BioGrid-String.txt";
        String biogridFileName = dirName + "BioGrid_Human_PPI.txt";
        PrintWriter bgPw = new PrintWriter(biogridFileName);
        String stringdbFileName = dirName + "StringDB_Human_PPI.txt";
        PrintWriter sbPw = new PrintWriter(stringdbFileName);
        FileReader fr = new FileReader(srcFileName);
        BufferedReader br = new BufferedReader(fr);
        String line = null;
        // Want to map uniprot to genes
        Map<String, String> uniprotToGene = new PairwiseService().getUniProtToGene();
        int totalUnMappedLines = 0;
        while ((line = br.readLine()) != null) {
            String[] tokens = line.split("\t");
            if (tokens.length == 3) {
                String fi = getFI(tokens[1], tokens[2], uniprotToGene);
                if (fi == null) {
                    logger.warn("Cannot map to genes: " + line);
                    totalUnMappedLines ++;
                    continue;
                }
                if (tokens[0].equals("Biogrid"))
                    bgPw.println(fi);
                else if (tokens[0].equals("StringDB")) 
                    sbPw.println(fi);
            }
            else if (tokens.length == 4) {
                String fi = getFI(tokens[2], tokens[3], uniprotToGene);
                if (fi == null) {
                    logger.warn("Cannot map to genes: " + line);
                    totalUnMappedLines++;
                    continue;
                }
                bgPw.println(fi);
                sbPw.println(fi);
            }
        }
        br.close();
        fr.close();
        sbPw.close();
        bgPw.close();
        // Total number is 98,553. Based on some check, most of them should be related to
        // unreviewed uniprot accession numbers, which can be escaped for our analysis.
        System.out.println("Total un-mapped lines: " + totalUnMappedLines);
    }
    
    private String getFI(String protein1, String protein2, Map<String, String> proteinToGene) {
        String gene1 = proteinToGene.get(protein1);
        String gene2 = proteinToGene.get(protein2);
        if (gene1 == null || gene2 == null)
            return null;
        return gene1.compareTo(gene2) < 0 ? gene1 + "\t" + gene2 : gene2 + "\t" + gene1;
    }

}
