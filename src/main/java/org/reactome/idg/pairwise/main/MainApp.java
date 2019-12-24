package org.reactome.idg.pairwise.main;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.reactome.idg.pairwise.config.MainAppConfig;
import org.reactome.idg.pairwise.model.DataDesc;
import org.reactome.idg.pairwise.model.DataType;
import org.reactome.idg.pairwise.model.PairwiseRelationship;
import org.reactome.idg.pairwise.service.PairwiseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import ch.qos.logback.classic.Level;

/**
 * This is for a traditional Java application used to load pairwise relationships into a couchbase database.
 * @author wug
 *
 */
public class MainApp {
    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

    public static void main(String[] args) {
        // Force to set the logging level
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MainAppConfig.class);
        PairwiseService service = context.getBean(PairwiseService.class);
//        service.testConnection();
//        String dirName = "examples";
//        String fileName = "Breast-MammaryTissue_Spearman_Adj.csv";
////        String fileName = "Ovary_Spearman_Adj.csv";
//        DataDesc desc = createGTExDataDesc(fileName);
//        service.insertDataDesc(desc);
//        processGTEGeneCoExpression(fileName, dirName, desc, service);
//        service.performIndex();
        List<DataDesc> descs = service.listDataDesc();
        descs.forEach(desc -> System.out.println(desc.getId()));
        context.close();
    }
    
    private static void processGTEGeneCoExpression(String fileName, 
                                                   String dirName,
                                                   DataDesc desc,
                                                   PairwiseService service) {
        try {
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
                for (int i = 1; i < tokens.length; i++) {
                    if (tokens[i].equals("NA") || tokens[i].equals("TRUE")) {
//                        System.err.println(line);
                        continue; // Just ignore NA
                    }
                    Integer geneIndex = geneToIndex.get(genes[i]);
                    if (geneIndex == null)
                        throw new IllegalStateException(tokens[i] + " is not in the database!");
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
        catch(IOException e) {
            logger.error(e.getMessage(), e);
        }
    }
    
    private static DataDesc createGTExDataDesc(String fileName) {
        DataDesc desc = new DataDesc();
        desc.setProvenance("GTEx");
        desc.setDataType(DataType.Gene_Coexpression);
        // Need to get the tissue from the file name
        int index = fileName.indexOf("_");
        String tissue = fileName.substring(0, index);
        desc.setBioSource(tissue);
        return desc;
    }
    
}
