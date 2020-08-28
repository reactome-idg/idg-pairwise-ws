package org.reactome.idg.pairwise.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.reactome.idg.pairwise.model.DataDesc;
import org.reactome.idg.pairwise.model.GeneToPathwaysRequestWrapper;
import org.reactome.idg.pairwise.model.PEsForInteractorAndDataDescsWrapper;
import org.reactome.idg.pairwise.model.PEsForInteractorResponse;
import org.reactome.idg.pairwise.model.PairwiseRelRequest;
import org.reactome.idg.pairwise.model.PairwiseRelationship;
import org.reactome.idg.pairwise.model.Pathway;
import org.reactome.idg.pairwise.model.PathwayToGeneRelationship;
import org.reactome.idg.pairwise.service.PairwiseService;
import org.reactome.idg.pairwise.web.errors.InternalServerError;
import org.reactome.idg.pairwise.web.errors.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller class provides the RESTful API for idg-pairwise relationships.
 * @author wug
 *
 */
@CrossOrigin
@RestController
public class PairwiseController {
    
    @Autowired
    private PairwiseService service;
    
    private static final Logger logger = LoggerFactory.getLogger(PairwiseService.class);
    
    public PairwiseController() {
    }
    
    @GetMapping("uniprot2gene")
    public String getUniProtToGene() {
        Map<String, String> uniprotToGene = service.getUniProtToGene();
        StringBuilder builder = new StringBuilder();
        uniprotToGene.forEach((u, g) -> builder.append(u + "\t" + g + "\n"));
        return builder.toString();
    }
    
    @GetMapping("/datadesc")
    public List<DataDesc> listDataDesctiptions() {
        return service.listDataDesc();
    }
    
    /**
     * There should be two lines in the post body:
     * 1). Line 1: "," delimited DataDesc ids
     * 2). Line 2: "," delimited gene names.
     * @param wrap
     * @return
     */
    @PostMapping("/pairwise/genes/{numberOnly}")
    public List<PairwiseRelationship> queryRelationshipsForGenes(@PathVariable("numberOnly") Boolean numberOnly,
                                                                 @RequestBody PairwiseRelRequest wrap) {
        if (wrap.getDataDescs() == null || wrap.getGenes() == null)
            return new ArrayList<>(); // Nothing to return
        return service.queryRelsForGenes(wrap.getGenes(), wrap.getDataDescs(), numberOnly);
    }
    
    /**
     * There should be two lines in the post body:
     * Line 1: "," delimited DataDesc ids
     * List 2: "," delimited protein uniprot accession numbers
     * @param wrap
     * @return
     */
    @PostMapping("/pairwise/uniprots/{numberOnly}")
    public List<PairwiseRelationship> queryRelationshipsForProteins(@PathVariable("numberOnly") Boolean numberOnly,
                                                                    @RequestBody PairwiseRelRequest wrap) {
    	if (wrap.getDataDescs() == null || wrap.getGenes() == null)
            return new ArrayList<>(); // Nothing to return
        return service.queryRelsForProteins(wrap.getGenes(), wrap.getDataDescs(), numberOnly);
    }
    
    @CrossOrigin
    @GetMapping("/relationships/genesForPathway/{stId}")
    public PathwayToGeneRelationship queryPathwayToGeneRelationship(@PathVariable("stId") String stId) {
    	return service.queryPathwayToGeneRelationships(stId.toUpperCase());
    }
    
    @CrossOrigin
    @GetMapping("/relationships/uniprotsForPathway/{uniprot}")
    public PathwayToGeneRelationship queryPathwayToUniprotRelationship(@PathVariable("uniprot") String uniprot) {
    	return service.queryPathwayToUniprotRelationships(uniprot.toUpperCase());
    }
    
    
    @CrossOrigin
    @PostMapping("/relationships/PEsForGeneInteractors")
    public PEsForInteractorResponse queryPEsForInteractor(@RequestBody PEsForInteractorAndDataDescsWrapper request){
    	PEsForInteractorResponse rtn;
		try {
			rtn = service.queryPEsForInteractor(request.getDbId(), request.getGene(), request.getDataDescs());
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new InternalServerError(e.getMessage());
		}
    	if(rtn.getPeIds() == null) {
    		String msg = "Physical entities not found for " + request.getGene();
    		//TODO: add exception as a variable and pass into warn
    		logger.warn(msg);
    		throw new ResourceNotFoundException(msg);
    	}

    	return rtn;
    }
    
    @CrossOrigin
    @PostMapping("/relationships/PEsForUniprotInteractors")
    public PEsForInteractorResponse queryPEsForUniprotInteractor(@RequestBody PEsForInteractorAndDataDescsWrapper request){
    	PEsForInteractorResponse rtn;
    	try {
    		rtn = service.queryPEsForUniprotInteractor(request.getDbId(), request.getGene(), request.getDataDescs());
    	} catch(IOException e) {
    		logger.error(e.getMessage(), e);
    		throw new InternalServerError(e.getMessage());
    	}
    	if(rtn.getPeIds() == null ) {
    		String msg = "Physical entities not found for " + request.getGene();
    		ResourceNotFoundException e = new ResourceNotFoundException(msg);
    		logger.warn(msg, e);
    		throw e;
    	}
    	return rtn;
    }
    
    @CrossOrigin
    @GetMapping("relationships/primaryPathwaysForGene/{gene}")
    public List<Pathway> queryPrimaryPathwaysForGene(@PathVariable("gene") String gene){
    	List<Pathway> rtn = service.queryPrimaryPathwaysForGene(gene);
    	if(rtn == null) {
    		String msg = "No primary pathways found for " + gene;
    		ResourceNotFoundException e = new ResourceNotFoundException(msg);
    		logger.info(msg, e);
    		throw e;
    	}
    	return rtn;
    }
    
    @CrossOrigin
    @GetMapping("/relationships/primaryPathwaysForUniprot/{uniprot}")
    public List<Pathway> queryUniprotToPathwayRelationship(@PathVariable("uniprot")String uniprot) {
    	List<Pathway> rtn = service.queryPrimaryPathwaysForUniprot(uniprot.toUpperCase());
    	if(rtn == null) throw new ResourceNotFoundException(uniprot + " not found.");
    	else return rtn;
    }
    
    /**
     * Performs an enrichment analysis on interactors for a gene based on passed in data descriptions
     * Line 1: gene to get interactors for
     * Line 2: "," separated list of data descriptions
     * @param text
     * @return
     */
    @CrossOrigin
    @PostMapping(path="/relationships/enrichedSecondaryPathwaysForGene")
    public List<Pathway> enrichPathwaysForGene(@RequestBody GeneToPathwaysRequestWrapper request) {
    	if(request == null || request.getGene() == null || request.getDataDescs() == null)
    		return new ArrayList<>();
    	return service.queryGeneToSecondaryPathwaysWithEnrichment(request.getGene(), request.getDataDescs());
    }
    
    /**
     * Performs an enrichment analysis on interactors for a gene based on passed in data descriptions
     * Line 1: Uniprot to get interactors for
     * Line 2: "," separated list of data descriptions
     * @param text
     * @return
     */
    @CrossOrigin
    @PostMapping(path="/relationships/enrichedSecondaryPathwaysForUniprot")
    public List<Pathway> enrichPathwaysForUniprot(@RequestBody GeneToPathwaysRequestWrapper request){
    	if(request == null || request.getGene() == null || request.getDataDescs() == null)
    		return new ArrayList<>();
    	return service.queryUniprotToSecondaryPathwaysWithEnrichment(request.getGene(), request.getDataDescs());
    }
    
    //TODO: swagger document for ws API design
}
