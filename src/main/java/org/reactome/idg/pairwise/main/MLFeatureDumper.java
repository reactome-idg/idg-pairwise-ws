package org.reactome.idg.pairwise.main;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.reactome.idg.annotations.FeatureDesc;
import org.reactome.idg.annotations.FeatureLoader;
import org.reactome.idg.fi.FeatureFileGenerator;
import org.reactome.idg.harmonizome.HarmonizomePairwiseLoader;
import org.reactome.idg.model.FeatureSource;
import org.reactome.idg.pairwise.config.MainAppConfig;
import org.reactome.idg.pairwise.model.DataDesc;
import org.reactome.idg.pairwise.service.PairwiseService;
import org.reactome.idg.util.ApplicationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * This class is used to dump features selected for ML into a mongodb database.
 * @author wug
 *
 */
public class MLFeatureDumper {
    private static Logger logger = LoggerFactory.getLogger(MLFeatureDumper.class);
    
    public MLFeatureDumper() {
        // Force to load the properties
        // This properties file should be copied from the idg-fi-network-ml project so that
        // we should have the same set of setting.
        ApplicationConfig.getConfig().loadProps("ml.application.properties");
    }

    public void dump() throws Exception {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MainAppConfig.class);
        PairwiseService service = context.getBean(PairwiseService.class);
        logger.info("Starting dump features...");
        FeatureFileGenerator featureGenerator = new FeatureFileGenerator();
        featureGenerator.setNeedNegative(true); // We need to split positive and negative features if any
        featureGenerator.setPrefixHarmonizomeInFeature(true);
        Map<String, FeatureDesc> featureNameToDesc = loadDataDesc(FeatureFileGenerator.class);
        logger.info("The size of featureNameToDesc: " + featureNameToDesc.size());
        featureNameToDesc.forEach((name, desc) -> {
            logger.info(name + ": " + desc.toString());
        });
        // Start to dump
        dumpPPIFeatures(featureGenerator, featureNameToDesc, service);
        dumpMiscFeatures(featureGenerator, featureNameToDesc, service);
        dumpHarmonizomeFeatures(featureGenerator, featureNameToDesc, service);
        dumpGTExFeatures(featureGenerator, featureNameToDesc, service);
        dumpTCGAFeatures(featureGenerator, featureNameToDesc, service);
        context.close();
    }
    
    private void dumpTCGAFeatures(FeatureFileGenerator featureGenerator, 
                                  Map<String, FeatureDesc> featureNameToDesc,
                                  PairwiseService service) throws Exception {
        String provenance = "TCGA";
        logger.info(String.format("Working on %s features...", provenance));
        Map<String, Set<String>> feature2pairs = new HashMap<>();
        long time1 = System.currentTimeMillis();
        featureGenerator.loadTCGACoExpressions(feature2pairs);
        long time2 = System.currentTimeMillis();
        logger.info("Loading is done: " + (time2 - time1) / (1000.0d * 60) +
                    " minutes for " + feature2pairs.size() + " feature(s).");
        dumpCoexpressionFeatures(feature2pairs,
                                 service, 
                                 provenance);
    }

    private void dumpGTExFeatures(FeatureFileGenerator featureGenerator, 
                                  Map<String, FeatureDesc> featureNameToDesc,
                                  PairwiseService service) throws Exception {
        String provenance = "GTEx";
        logger.info(String.format("Working on %s features...", provenance));
        Map<String, Set<String>> feature2pairs = new HashMap<>();
        long time1 = System.currentTimeMillis();
        featureGenerator.loadGTExCoExpressions(feature2pairs);
        long time2 = System.currentTimeMillis();
        logger.info("Loading is done: " + (time2 - time1) / (1000.0d * 60) + 
                    " minutes for " + feature2pairs.size() + " feature(s).");
        dumpCoexpressionFeatures(feature2pairs, 
                                 service,
                                 provenance);
    }

    private void dumpCoexpressionFeatures(Map<String, Set<String>> feature2pairs,
                                         PairwiseService service,
                                         String provenance) throws IOException, Exception {
        GTExDataProcessor processor = new GTExDataProcessor();
        long time1 = System.currentTimeMillis();
        for (String feature : feature2pairs.keySet()) {
            logger.info("Working on " + feature + "...");
            // Get the source with a little bit parsing
            // format: GTEx-Adipose-Subcutaneous
            // or: TCGA-BRCA
            int index = feature.indexOf("-");
            DataDesc dataDesc = processor.createDataDescForSource(feature.substring(index + 1),
                                                                  provenance);
            logger.info("Insert DataDesc " + dataDesc.getId() + "...");
            service.insertDataDesc(dataDesc);
            Set<String> pairs = feature2pairs.get(feature);
            logger.info("Total pairs: " + pairs.size());
            processor.processPairs(pairs, dataDesc, service);
        }
        long time2 = System.currentTimeMillis();
        logger.info("Feature loaded: " + (time2 - time1) / (1000.0d * 60)
                    + " minutes for " + feature2pairs.size() + ".");
    }

    private void dumpHarmonizomeFeatures(FeatureFileGenerator featureGenerator,
                                         Map<String, FeatureDesc> featureNameToDesc,
                                         PairwiseService service) throws Exception {
        logger.info("Working on Harmonizome features...");
        Map<String, Set<String>> feature2pairs = new HashMap<>();
        long time1 = System.currentTimeMillis();
        featureGenerator.loadHarmonizomeFeatures(feature2pairs);
        HarmonizomeDataProcessor processor = new HarmonizomeDataProcessor();
        for (String feature : feature2pairs.keySet()) {
            logger.info("Working on " + feature + "...");
            // Get the origin
            int index = feature.indexOf("-");
            String origin = feature.substring(index + 1);
            DataDesc dataDesc = processor.createDataDescFromOrigin(origin);
            logger.info("Insert DataDesc " + dataDesc.getId() + "...");
            service.insertDataDesc(dataDesc);
            Set<String> pairs = feature2pairs.get(feature);
            logger.info("Total pairs: " + pairs.size());
            processor.processPairs(pairs, dataDesc, service);
        }
        long time2 = System.currentTimeMillis();
        logger.info("Feature loaded: " + (time2 - time1) / (1000.0d * 60)
                    + " minutes for " + feature2pairs.size() + ".");
    }

    private void dumpMiscFeatures(FeatureFileGenerator featureGenerator,
                                  Map<String, FeatureDesc> featureNameToDesc,
                                  PairwiseService service) throws Exception {
        logger.info("Working on misc features...");
        Map<String, Set<String>> feature2pairs = new HashMap<>();
        long time1 = System.currentTimeMillis();
        featureGenerator.loadMiscFeatures(feature2pairs);
        long time2 = System.currentTimeMillis();
        logger.info("Feature loaded: " + (time2 - time1) / (1000.0d * 60) + " minutes.");
        dumpPPIs(featureNameToDesc,
                 service,
                 feature2pairs);
        logger.info("Done.");
    }

    private void dumpPPIFeatures(FeatureFileGenerator featureGenerator,
                                 Map<String, FeatureDesc> feature2desc,
                                 PairwiseService service) throws Exception {
        logger.info("Working on PPI features...");
        // Start to dump
        long time1 = System.currentTimeMillis();
        Map<String, Set<String>> feature2pairs = new HashMap<>();
        logger.info("Working on PPI features...");
        featureGenerator.loadPPIFeatures(feature2pairs);
        long time2 = System.currentTimeMillis();
        logger.info("PPI featurs loaded: " + (time2 - time1) / (1000.0d * 60) + " minutes.");
        logger.info("Starting dumping...");
        dumpPPIs(feature2desc,
                 service,
                 feature2pairs);
    }

    private void dumpPPIs(Map<String, FeatureDesc> feature2desc,
                          PairwiseService service,
                          Map<String, Set<String>> feature2pairs) throws Exception {
        long time1;
        long time2;
        time1 = System.currentTimeMillis();
        PPIDataProcessor helper = new PPIDataProcessor();
        for (String feature : feature2pairs.keySet()) {
            logger.info("Working on " + feature + "...");
            FeatureDesc featureDesc = feature2desc.get(feature);
            if (featureDesc == null)
                throw new IllegalStateException("Cannot find FeatureDesc annotation for " + feature);
            DataDesc dataDesc = convertFeatureDescToDataDesc(featureDesc);
            logger.info("Insert DataDesc " + dataDesc.getId() + "...");
            service.insertDataDesc(dataDesc);
            Set<String> pairs = feature2pairs.get(feature);
            logger.info("Total pairs: " + pairs.size());
            helper.processPairs(pairs, dataDesc, service);
        }
        time2 = System.currentTimeMillis();
        logger.info("Done: " + (time2 - time1) / (1000.0d * 60) + " minutes.");
    }

    private DataDesc convertFeatureDescToDataDesc(FeatureDesc featureDesc) {
        DataDesc dataDesc = new DataDesc();
        dataDesc.setBioSource(featureDesc.species().toString());
        dataDesc.setDataType(featureDesc.type());
        FeatureSource[] sources = featureDesc.sources();
        if (sources != null && sources.length > 0) {
            String text = Arrays.asList(sources)
                    .stream()
                    .map(src -> src.toString())
                    .sorted()
                    .collect(Collectors.joining());
            dataDesc.setProvenance(text);
        }
        return dataDesc;
    }
    
    private Map<String, FeatureDesc> loadDataDesc(Class<FeatureFileGenerator> cls) throws Exception {
        Map<String, FeatureDesc> featureNameToDesc = new HashMap<>();
        Method[] methods = cls.getDeclaredMethods();
        for (Method method : methods) {
            FeatureLoader featureLoader = method.getAnnotation(FeatureLoader.class);
            if (featureLoader == null)
                continue; // This is not a feature loader method. Nothing to do with it.
            String[] loaderMethods = featureLoader.methods();
            for (String methodName : loaderMethods) {
                String[] tokens = methodName.split(",");
                String loaderMethod = null;
                if (tokens.length == 2)
                    loaderMethod = tokens[1];
                else
                    loaderMethod = tokens[0];
                // Need to extra parameters
                int index = loaderMethod.lastIndexOf(".");
                Class<?> loaderCls = Class.forName(loaderMethod.substring(0, index));
                Method clsMethod = searchMethod(loaderMethod.substring(index + 1), loaderCls);
                // Expect a FeatureDesc annotation
                FeatureDesc desc = clsMethod.getAnnotation(FeatureDesc.class);
                if (tokens.length == 2)
                    featureNameToDesc.put(tokens[0], desc);
                else if (featureLoader.source() != null){
                    // We expected to get the source value from featureLoader
                    featureNameToDesc.put(featureLoader.source() + "", desc);
                }
                else
                    throw new IllegalStateException("Cannot find a name for method: " + methodName);
            }
        }
        return featureNameToDesc;
    }
    
    private <T> Method searchMethod(String methodName, Class<T> cls) {
        Method[] methods = cls.getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName) && method.getAnnotation(FeatureDesc.class) != null)
                return method;
        }
        return null;
    }

}
