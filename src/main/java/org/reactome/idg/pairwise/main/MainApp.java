package org.reactome.idg.pairwise.main;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import org.reactome.idg.pairwise.config.MainAppConfig;
import org.reactome.idg.pairwise.model.DataDesc;
import org.reactome.idg.pairwise.model.PairwiseRelationship;
import org.reactome.idg.pairwise.service.PairwiseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * This is for a traditional Java application used to load pairwise relationships into a mongodb database.
 * @author wug
 */
public class MainApp {
    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

    public static void main(String[] args) {
//        pushDataIntoDB(args);
//        pushMLFeatureIntoDB();
    	  pushHomePageData();
    }
    
    private static void pushMLFeatureIntoDB() {
        try {
            new MLFeatureDumper().dump();
        }
        catch(Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unused")
    private static void pushDataIntoDB(String[] args) {
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
//      loadPathwayGeneDarkProteinRels(service);
        context.close();
    }
    
    
    /**
     * Used to insert pathway index data from Uniprot2Reactome.txt file
     * Also caches Gene/Pathway relationships into a pathways collection
     * @param: args[] should be [*directory of UniProt2Pathway file*, *fileName*]
     */
    private static void pushHomePageData() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MainAppConfig.class);
        PairwiseService service = context.getBean(PairwiseService.class);
    	PathwayProcessor processor = new PathwayProcessor();
    	processor.processPathways(service);
    	
    	context.close();
    }
    
    /**
     * For some data distribution test.
     * @param service
     */
    private static void loadPathwayGeneDarkProteinRels(PairwiseService service) {
        List<String> darkGenes = loadDarkGenes();
        List<String> queryGenes = Arrays.asList(new String[]{"RYR1", "RYR2", "RYR3"});
        List<String> descIds = Arrays.asList(new String[]{"GTEx|Breast-MammaryTissue|Gene_Coexpression",
                                                          "GTEx|Ovary|Gene_Coexpression"});
        List<PairwiseRelationship> rels = service.queryRelsForGenesInTargets(queryGenes, 
                                                                             darkGenes,
                                                                             descIds);
        System.out.println("Total rels: " + rels.size());
        for (PairwiseRelationship rel : rels) {
            System.out.println();
            System.out.println(rel.getGene() + " " + rel.getDataDesc().getId());
            List<String> posGenes = rel.getPosGenes();
            System.out.println("Positive: " + (posGenes == null ? "0" : posGenes.size()));
            if (posGenes != null && posGenes.size() > 0) {
                Collections.sort(posGenes);
                System.out.println(posGenes);
            }
            List<String> negGenes = rel.getNegGenes();
            System.out.println("Negative: " + (negGenes == null ? "0" : negGenes.size()));
            if (negGenes != null && negGenes.size() > 0) {
                Collections.sort(negGenes);
                System.out.println(negGenes);
            }
        }
    }
    
    private static List<String> loadDarkGenes() {
        String fileName = "TdarkProteins.txt";
        InputStream is = MainApp.class.getClassLoader().getResourceAsStream(fileName);
        Scanner scanner = new Scanner(is);
        String line = scanner.nextLine(); // Escape one line
        List<String> genes = new ArrayList<>();
        while ((line = scanner.nextLine()) != null) {
            String[] tokens = line.split("\t");
            genes.add(tokens[1]);
            if (!scanner.hasNextLine())
                break;
        }
        scanner.close();
        return genes;
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
