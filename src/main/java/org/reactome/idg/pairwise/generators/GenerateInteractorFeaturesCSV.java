package org.reactome.idg.pairwise.generators;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.Document;
import org.reactome.idg.pairwise.main.PathwayProcessor;
import org.reactome.idg.pairwise.service.PairwiseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author brunsont
 *
 */
public class GenerateInteractorFeaturesCSV {
    private final static Logger logger = LoggerFactory.getLogger(PathwayProcessor.class);
	private final String CSV = ",";
	private final String TSV = "\t";
	
	public GenerateInteractorFeaturesCSV() {}//nothing here
	
	public void generateCSV(PairwiseService service) {
		generateFile(service, CSV);
	}
	
	public void generateTSV(PairwiseService service) {
		generateFile(service, TSV);
	}

	@SuppressWarnings(value = { "unchecked" })
	private void generateFile(PairwiseService service, String deliminator) {
		File fOut = new File("idg_reactome_pairwise_features.csv");
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(fOut);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));
			
			//get list of Data Description ids
	    	List<String> dataDescriptions = service.getDataDescriptions().stream().sorted().collect(Collectors.toList());
	
	    	//build header of file
	    	StringBuilder header = new StringBuilder();
	    	header.append("gene_name,interactor_name,included_in_reactome,functional_interaction_score,")
	    	   	   .append(String.join(",",dataDescriptions));
	    	writer.write(header.toString());
	    	writer.newLine();
	    	
	    	//reverse index to gene name
	    	Map<Integer,String> indexToGene = service.getIndexToGene();
	    	Set<String> reactomeAnnotatedGenes = service.getReactomeAnnotatedGenes();
	    	
	    	//get list of all gene names sorted and iterate over them
			service.getIndexToGene().values().stream().sorted().collect(Collectors.toList()).forEach(gene -> {
		    	
				Document geneDoc = service.getRelationshipDocForGene(gene);
				if(geneDoc == null) return;
				
				//assumes combined_score includes every interactor of gene
				for(Map.Entry<String, Object> entry: ((Document)geneDoc.get("combined_score")).entrySet()){
					if(entry.getKey().equals("null")) continue;
					StringBuilder toWrite = new StringBuilder();
					int index = Integer.parseInt(entry.getKey());
					String interactor = indexToGene.get(index);
					toWrite.append(gene + "," + interactor + ",")
						   .append(reactomeAnnotatedGenes.contains(interactor) ? "1":"0")
						   .append((Double)entry.getValue());
					
					dataDescriptions.forEach(desc -> {
						toWrite.append(",");
						Document doc = (Document)geneDoc.get(desc);
						if(doc == null) {
							toWrite.append("0");
							return;
						}
						if(doc.containsKey("pos") && ((List<Integer>)doc.get("pos")).contains(index)) {
							toWrite.append("1");
							return;
						}
						if(doc.containsKey("neg") && ((List<Integer>)doc.get("neg")).contains(index)) {
							toWrite.append("-1");
							return;
						}
						toWrite.append("0");
					});
					try {
						writer.write(toWrite.toString());
						writer.newLine();
					} catch (IOException e) {
						e.printStackTrace();
						logger.error(e.getMessage(), e);
					}
				}
	 		});
			writer.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
			logger.error(e.getMessage(), e);
		}
	}
	
}
