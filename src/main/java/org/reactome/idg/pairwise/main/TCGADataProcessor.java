package org.reactome.idg.pairwise.main;

import org.junit.Test;
import org.reactome.idg.model.FeatureType;
import org.reactome.idg.pairwise.model.DataDesc;

public class TCGADataProcessor extends GTExDataProcessor {
    
    public TCGADataProcessor() {
    }
    
    @Override
    public DataDesc createDataDesc(String fileName) {
        DataDesc desc = new DataDesc();
        desc.setProvenance("TCGA");
        desc.setDataType(FeatureType.Gene_Coexpression);
        // Need to get the cancer from the file name
        // The format is like this: TCGA-SKCM_Spearman_Adj.csv
        int index = fileName.indexOf("_");
        String cancer = fileName.substring(0, index);
        index = cancer.indexOf("-");
        // Remove TCGA
        cancer = cancer.substring(index + 1);
        desc.setBioSource(cancer);
        return desc;
    }
    
    @Test
    public void testCreateDataDesc() {
        String fileName = "TCGA-SKCM_Spearman_Adj.csv";
        DataDesc desc = createDataDesc(fileName);
        System.out.println("Data desc: " + desc.getId());
    }

}
