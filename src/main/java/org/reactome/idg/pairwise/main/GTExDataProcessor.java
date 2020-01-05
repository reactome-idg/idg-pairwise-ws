package org.reactome.idg.pairwise.main;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.reactome.idg.pairwise.model.DataDesc;
import org.reactome.idg.pairwise.model.DataType;
import org.reactome.idg.pairwise.model.PairwiseRelationship;
import org.reactome.idg.pairwise.service.PairwiseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to process GTEx data.
 * @author wug
 *
 */
public class GTExDataProcessor implements PairwiseDataProcessor {

    private static final Logger logger = LoggerFactory.getLogger(GTExDataProcessor.class);

    public GTExDataProcessor() {
    }



    @Override
    public boolean isCorrectFile(String fileName) {
        return fileName.endsWith("Spearman_Adj.csv");
    }

    @Override
    public DataDesc createDataDesc(String fileName) {
        DataDesc desc = new DataDesc();
        desc.setProvenance("GTEx");
        desc.setDataType(DataType.Gene_Coexpression);
        // Need to get the tissue from the file name
        int index = fileName.indexOf("_");
        String tissue = fileName.substring(0, index);
        desc.setBioSource(tissue);
        return desc;
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
                logger.info("No value in line: " + line);
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
