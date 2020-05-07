package org.reactome.idg.pairwise.main;

import java.util.Set;

import org.reactome.idg.pairwise.model.DataDesc;
import org.reactome.idg.pairwise.service.PairwiseService;

public interface PairwiseDataProcessor {
    
    /**
     * Make sure the correct file is processed. This is basically based on some 
     * consistent file pattern.
     * @param fileName
     * @return
     */
    public boolean isCorrectFile(String fileName);
    
    /**
     * Create a correct DataDesc object for the passed file.
     * @param fileName
     * @return
     */
    public DataDesc createDataDesc(String fileName);
    
    /**
     * Load the pairwise relationships into the database.
     * @param fileName
     * @param dirName
     * @param desc
     * @param service
     */
    public void processFile(String fileName, 
                            String dirName, 
                            DataDesc desc, 
                            PairwiseService service) throws Exception;
    
    /**
     * Load a set of pairwise relationships directly into the database.
     * @param pairs
     * @param desc
     * @param service
     * @throws Exception
     */
    public void processPairs(Set<String> pairs,
                             DataDesc desc,
                             PairwiseService service) throws Exception;

}
