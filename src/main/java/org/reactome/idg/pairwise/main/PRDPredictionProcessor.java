package org.reactome.idg.pairwise.main;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.reactome.idg.pairwise.model.GeneCombinedScore;
import org.reactome.idg.pairwise.service.PairwiseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PRDPredictionProcessor {
    private final static Logger logger = LoggerFactory.getLogger(PathwayProcessor.class);

    /**
     * prdPrediction gene pairs is checked for consistency agains the feature file. After checking, 
     * a map is build of gene to Gene to combined score maps. After the mapping is complete, insert into database through
     * service method
     * @param service
     * @param folder
     * @param prdPredictionFile
     *     first column should be gene pair separated by underscore. Score in column 5 (index 4) will be cached as the prd for 
     *     each gene pair
     * @param featureFile
     *     Only concerned with first column which must be gene pairs separated by \t.
     */
	public void processPRDPredictions(PairwiseService service, String folder, String prdPredictionFile, String featureFile) {
		logger.info("Clearing combined scores...");
		service.clearCombinedScores(); //helper method for testing adding combined scores
		
		logger.info("Caching combined scores...");
		
		Map<String, String> underscoreToTabMap = loadUnderscoreToTabMap(folder, featureFile);
				
		Map<String, GeneCombinedScore> geneToCombinedScore = new HashMap<>();
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(folder+prdPredictionFile));
			String line = reader.readLine();
			while((line = reader.readLine()) != null) {
				String[] tokens = line.split(",");
				
				//check against underscore map to ensure features file matches prd gene pairs
				String genesString = underscoreToTabMap.get(tokens[0]);
				if(genesString == null) {
					throw new IOException("Gene pair in prediction file not found in feature file: " + tokens[0]);					
				};
				
				String [] genes = genesString.split("\t");
				if(genes[0] == "null" || genes[1] == "null") continue; //make sure no nulls get cached
				Double prd = Double.parseDouble(tokens[4]);

				if(!geneToCombinedScore.containsKey(genes[0]))
					geneToCombinedScore.put(genes[0], new GeneCombinedScore(genes[0]));
				geneToCombinedScore.get(genes[0]).addInteractorToScore(genes[1], prd);
				
				if(!geneToCombinedScore.containsKey(genes[1]))
					geneToCombinedScore.put(genes[1], new GeneCombinedScore(genes[1]));
				geneToCombinedScore.get(genes[1]).addInteractorToScore(genes[0], prd);
			}
			reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
			e.printStackTrace();
		}
				
//		logger.info("Ensuring all genes are in GENE_INDEX");
		service.ensureGeneIndex(geneToCombinedScore.keySet());
		
		logger.info("Inserting combined_scores for each relationship document");
		service.insertCombinedScore(geneToCombinedScore.values());
		logger.info("Finished adding combined scores.");
	}

	private Map<String, String> loadUnderscoreToTabMap(String folder, String file){
		Map<String, String> rtn = new HashMap<>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(folder+file));
			String line = reader.readLine(); //skip header line;
			while((line = reader.readLine()) != null) {
				String tabString = line.split(",")[0];
				rtn.put(tabString.replace("\t", "_"), tabString);
			}
			reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		
		return rtn;
	}
	
}
