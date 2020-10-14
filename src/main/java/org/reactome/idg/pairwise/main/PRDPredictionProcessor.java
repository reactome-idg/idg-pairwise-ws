package org.reactome.idg.pairwise.main;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.reactome.idg.pairwise.model.GeneCombinedScore;
import org.reactome.idg.pairwise.service.PairwiseService;

public class PRDPredictionProcessor {

	public void processPRDPredictions(PairwiseService service, String folder, String prdFile, String predictionFile) {
		System.out.println("Clearing combined scores...");
		service.clearCombinedScores(); //helper method for testing adding combined scores
		
		System.out.println("Caching combined scores...");
		
		Map<String, String> underscoreToTabMap = loadUnderscoreToTabMap(folder, predictionFile);
				
		Map<String, GeneCombinedScore> geneToCombinedScore = new HashMap<>();
		
		try {
			Reader in = new FileReader(folder + prdFile);
			Iterable<CSVRecord> records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
			for(CSVRecord record : records) {
				String [] genes = underscoreToTabMap.get(record.get(0)).split("\t");
				Double prd = Double.parseDouble(record.get(1));

				if(!geneToCombinedScore.containsKey(genes[0]))
					geneToCombinedScore.put(genes[0], new GeneCombinedScore(genes[0]));
				geneToCombinedScore.get(genes[0]).addInteractorToScore(genes[1], prd);
				
				if(!geneToCombinedScore.containsKey(genes[1]))
					geneToCombinedScore.put(genes[1], new GeneCombinedScore(genes[1]));
				geneToCombinedScore.get(genes[1]).addInteractorToScore(genes[0], prd);
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Finished caching " + geneToCombinedScore.keySet().size() + " genes.");
		
		System.out.println("Ensuring all genes are in GENE_INDEX");
		service.ensureGeneIndex(geneToCombinedScore.keySet());
		
		System.out.println("Inserting combined_scores for each relationship document");
		service.insertCombinedScore(geneToCombinedScore.values());
		System.out.println("Finished adding combined scores.");
	}

	private Map<String, String> loadUnderscoreToTabMap(String folder, String file){
		Map<String, String> rtn = new HashMap<>();
		try {
			Reader in = new FileReader(folder+file);
			Iterable<CSVRecord> records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
			for(CSVRecord record : records) {
				String tabString = record.get(0);
				rtn.put(tabString.replace("\t", "_"), tabString);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return rtn;
	}
	
}
