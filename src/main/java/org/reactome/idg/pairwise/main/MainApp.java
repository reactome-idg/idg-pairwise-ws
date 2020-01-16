package org.reactome.idg.pairwise.main;

import java.io.File;

import org.reactome.idg.pairwise.config.MainAppConfig;
import org.reactome.idg.pairwise.model.DataDesc;
import org.reactome.idg.pairwise.service.PairwiseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * This is for a traditional Java application used to load pairwise relationships into a couchbase database.
 * @author wug
 *
 */
public class MainApp {
    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Provide two parameters: data_source data_dir.");
            return;
        }
        // The configuration is controlled by logback.xml in the resource folder.
        // There is no need to hard-code this here.
        // Force to set the logging level
//        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
//        root.setLevel(Level.WARN); // Force to use the warn level
        PairwiseDataProcessor processor = getPairwiseDataProcessor(args[0]);
        if (processor == null) {
            logger.error("Cannot find a data processor for " + args[0] + ".");
            return;
        }
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MainAppConfig.class);
        PairwiseService service = context.getBean(PairwiseService.class);
        File dir = new File(args[1]);
        try {
            for (File file : dir.listFiles()) {
                String fileName = file.getName();
                if (!processor.isCorrectFile(fileName))
                    continue;
                logger.info("Processing file " + fileName + "...");
                DataDesc desc = processor.createDataDesc(fileName);
                service.insertDataDesc(desc);
                processor.processFile(fileName, args[1], desc, service);
            }
        }
        catch(Exception e) {
            logger.error(e.getMessage(), e);
        }
        //        service.testConnection();
        //        String dirName = "examples";
        //        String fileName = "Breast-MammaryTissue_Spearman_Adj.csv";
        ////        String fileName = "Ovary_Spearman_Adj.csv";
        //        DataDesc desc = createGTExDataDesc(fileName);
        //        service.insertDataDesc(desc);
        //        processGTEGeneCoExpression(fileName, dirName, desc, service);
        //        service.performIndex();
        //        List<DataDesc> descs = service.listDataDesc();
        //        descs.forEach(desc -> System.out.println(desc.getId()));
        context.close();
    }
    
    private static PairwiseDataProcessor getPairwiseDataProcessor(String dataSource) {
        if (dataSource.equalsIgnoreCase("GTEx"))
            return new GTExDataProcessor();
        if (dataSource.equalsIgnoreCase("TCGA"))
            return new TCGADataProcessor();
        if (dataSource.equalsIgnoreCase("Harmonizome"))
            return new HarmonizomeDataProcessor();
        if (dataSource.equals("PPI"))
            return new PPIDataProcessor();
        return null; // Don't know
    }
}
