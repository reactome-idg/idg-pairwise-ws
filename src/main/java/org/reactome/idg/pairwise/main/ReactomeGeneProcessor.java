package org.reactome.idg.pairwise.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.reactome.idg.pairwise.service.PairwiseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author brunsont
 * Helper class to insert all Reactome annotated genes into their own collection inside MongoDB
 */
public class ReactomeGeneProcessor {
    private final static Logger logger = LoggerFactory.getLogger(PathwayProcessor.class);
    private final String UNIPROT_2_REACTOME_ALL_LEVELS_URL = "https://reactome.org/download/current/UniProt2Reactome_All_Levels.txt";

	public ReactomeGeneProcessor() {}
	
	public void processReactomeGenes(PairwiseService service) {
		service.regenrateReactomeAnnotatedGenesCollection();
		Map<String, String> uniprotToGene = service.getUniProtToGene();
		Set<String> annotatedGenes = new HashSet<>();
		try {
			URL url = new URL(this.UNIPROT_2_REACTOME_ALL_LEVELS_URL);
			BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
			String line = null;
			while((line = br.readLine()) != null) {
				String correctedUniprot = line.split("\t")[0].split("-")[0];
				String gene = uniprotToGene.get(correctedUniprot);
				if(gene != null) annotatedGenes.add(gene);
			}
			service.insertReactomeAnnotatedGenes(annotatedGenes);
		}
		catch(IOException e) {
			logger.error(e.getMessage(), e);
			return;
		}
	}
	
}
