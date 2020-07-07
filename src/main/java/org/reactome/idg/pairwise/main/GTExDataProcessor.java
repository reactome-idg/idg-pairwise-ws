package org.reactome.idg.pairwise.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.reactome.idg.model.FeatureType;
import org.reactome.idg.pairwise.model.DataDesc;
import org.reactome.idg.pairwise.model.PairwiseRelationship;
import org.reactome.idg.pairwise.service.PairwiseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to process GTEx data.
 * @author wug
 *
 */
public class GTExDataProcessor extends HarmonizomeDataProcessor {

    private static final Logger logger = LoggerFactory.getLogger(GTExDataProcessor.class);

    public GTExDataProcessor() {
    }

    public static void main(String[] args) throws Exception {
        GTExDataProcessor processor = new GTExDataProcessor();
        Set<String> genes1 = Arrays.asList("RYR1", "RYR2", "RYR3").stream().collect(Collectors.toSet());
        Set<String> genes2 = Arrays.asList("SMIM3", "FNDC11").stream().collect(Collectors.toSet());
        processor.queryCorrelations(genes1, genes2, args[0], args[1]);
    }

    @Test
    public void mergeFiles() throws IOException {
        Map<String, String> sourceToFile = new HashMap<>();
        sourceToFile.put("GTEx", "GTEx_Corr_Query_0202.txt");
        sourceToFile.put("TCGA", "TCGA_Corr_Query_0202.txt");
        String dir = "examples/";
        PrintWriter pr = new PrintWriter(dir + "Corr_Query_0202_Merged.txt");
        pr.println("Pair\tBioSource\tSpearmCorr\tDataSource");
        for (String key : sourceToFile.keySet()) {
            String fileName = sourceToFile.get(key);
            try (Stream<String> lines = Files.lines(Paths.get(dir, fileName))) {
                lines.skip(1)
                     .forEach(line -> pr.println(line + "\t" + key));
            }
        }
        pr.close();
    }

    /**
     * Query correlations for genes.
     * @param genes1
     * @param genes2
     * @param outFileName
     * @param dirName
     * @throws IOException
     */
    public void queryCorrelations(Set<String> genes1,
                                  Set<String> genes2,
                                  String outFileName,
                                  String dirName) throws IOException {
        PrintWriter pr = new PrintWriter(outFileName);
        pr.println("Pair\tBioSource\tSpearmCorr");
        for (File file : new File(dirName).listFiles()) {
            if (!isCorrectFile(file.getName()))
                continue;
            logger.info("Processing file " + file.getName() + "...");
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line = br.readLine();
            String[] genes = line.split(",");
            // Get the index for genes2
            Map<String, Integer> geneToIndex = new HashMap<String, Integer>();
            for (int i = 1; i < genes.length; i++) {
                if (genes2.contains(genes[i])) {
                    geneToIndex.put(genes[i], i);
                }
            }
            // In case some genes are not there
            genes2 = new HashSet<>(genes2);
            genes2.retainAll(geneToIndex.keySet());
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",");
                if (!genes1.contains(tokens[0]))
                    continue;
                for (String gene2 : genes2) {
                    Double value = new Double(tokens[geneToIndex.get(gene2)]);
                    pr.println(tokens[0] + " " + gene2 + "\t" + parseBioSource(file.getName()) + "\t" + value);
                }
            }
            br.close();
            fr.close();
        }
        pr.close();
    }

    @Override
    public boolean isCorrectFile(String fileName) {
        return fileName.endsWith("Spearman_Adj.csv");
    }

    @Override
    public DataDesc createDataDesc(String fileName) {
        // Need to get the tissue from the file name
        String tissue = parseBioSource(fileName);
        return createDataDescForSource(tissue, "GTEx");
    }

    public DataDesc createDataDescForSource(String tissue,
                                            String provenance) {
        DataDesc desc = new DataDesc();
        desc.setBioSource(tissue);
        desc.setProvenance(provenance);
        desc.setDataType(FeatureType.Gene_Coexpression);
        return desc;
    }

    protected String parseBioSource(String fileName) {
        int index = fileName.indexOf("_");
        String tissue = fileName.substring(0, index);
        return tissue;
    }

    @Override
    public void processFile(String fileName, String dirName, DataDesc desc, PairwiseService service) throws Exception {
        FileReader reader = new FileReader(dirName + "/" + fileName);
        BufferedReader br = new BufferedReader(reader);
        String line = br.readLine();
        String[] genes = line.split(",");
        List<String> list = Arrays.asList(genes);
        Map<String, Integer> geneToIndex = service.ensureGeneIndex(list.subList(1, list.size()));
        logger.info("Total indexed genes: " + geneToIndex.size());
        int c = 0;
        long time1 = System.currentTimeMillis();
        while ((line = br.readLine()) != null) {
            String[] tokens = line.split(",");
            PairwiseRelationship rel = new PairwiseRelationship();
            rel.setGene(tokens[0]);
            Integer currentGeneIndex = geneToIndex.get(tokens[0]);
            for (int i = 1; i < tokens.length; i++) {
                if (tokens[i].equals("NA") || tokens[i].equals("TRUE")) {
                    //                        System.err.println(line);
                    continue; // Just ignore NA
                }
                Integer geneIndex = geneToIndex.get(genes[i]);
                if (geneIndex == null)
                    throw new IllegalStateException(tokens[i] + " is not in the database!");
                if (currentGeneIndex.equals(geneIndex))
                    continue; // Escape itself
                Double value = new Double(tokens[i]);
                if (value > 0.5d)
                    rel.addPos(geneIndex);
                else if (value < -0.5d)
                    rel.addNeg(geneIndex);
            }
            if (rel.getNeg() == null && rel.getPos() == null) {
                logger.info("No value in line: " + c);
                continue;
            }
            rel.setDataDesc(desc);
            service.insertPairwise(rel);
            c ++;
            //                if (c == 1000)
            //                    break;
        }
        br.close();
        reader.close();
        long time2 = System.currentTimeMillis();
        logger.info("Total inserted genes: " + c);
        logger.info("Total time: " + (time2 - time1) / (1000.0 * 60.0d) + " minutes.");
    }
}
