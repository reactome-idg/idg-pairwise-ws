package org.reactome.idg.pairwise.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.reactome.idg.pairwise.model.DataDesc;
import org.reactome.idg.pairwise.model.FeatureForTermInteractorsWrapper;
import org.reactome.idg.pairwise.model.GeneToPathwaysRequestWrapper;
import org.reactome.idg.pairwise.model.PEsForInteractorAndDataDescsWrapper;
import org.reactome.idg.pairwise.model.PEsForInteractorResponse;
import org.reactome.idg.pairwise.model.PairwiseRelRequest;
import org.reactome.idg.pairwise.model.PairwiseRelationship;
import org.reactome.idg.pairwise.model.Pathway;
import org.reactome.idg.pairwise.model.network.Element;
import org.reactome.idg.pairwise.model.pathway.GraphPathway;
import org.reactome.idg.pairwise.model.pathway.HierarchyResponseWrapper;
import org.reactome.idg.pairwise.service.PairwiseService;
import org.reactome.idg.pairwise.web.errors.InternalServerError;
import org.reactome.idg.pairwise.web.errors.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller class provides the RESTful API for idg-pairwise relationships.
 * @author wug
 * @author brunsont
 */
@RestController
@CrossOrigin()
public class PairwiseController {
    
    @Autowired
    private PairwiseService pairwiseService;
    
    private static final Logger logger = LoggerFactory.getLogger(PairwiseService.class);
    
    public PairwiseController() {
    }
    
    @GetMapping("uniprot2gene")
    public String getUniProtToGene() {
        Map<String, String> uniprotToGene = pairwiseService.getUniProtToGene();
        StringBuilder builder = new StringBuilder();
        uniprotToGene.forEach((u, g) -> builder.append(u + "\t" + g + "\n"));
        return builder.toString();
    }
    
    @GetMapping("/datadesc")
    public List<DataDesc> listDataDesctiptions() {
        return pairwiseService.listDataDesc();
    }
    
    @GetMapping("/datadesc/{term}")
    public List<DataDesc> listDataDescriptionsForTerm(@PathVariable("term")String term){
    	return pairwiseService.listDataDesc(term);
    }
    
    @GetMapping("/checkTerm/{term}")
    public boolean checkIfTermExists(@PathVariable("term")String term) {
    	return pairwiseService.getGeneForTerm(term) == null ? false:true;
    }
    
    /**
     * @param wrap
     * @return
     */
    @PostMapping("/pairwise/genes/{numberOnly}")
    public List<PairwiseRelationship> queryRelationshipsForGenes(@PathVariable("numberOnly") Boolean numberOnly,
                                                                 @RequestBody PairwiseRelRequest wrap) {
        if (wrap.getDataDescs() == null || wrap.getGenes() == null)
            return new ArrayList<>(); // Nothing to return
        return pairwiseService.queryRelsForGenes(wrap.getGenes(), wrap.getDataDescs(), numberOnly);
    }
    
    /**
     * @param wrap
     * @return
     */
    @PostMapping("/pairwise/uniprots/{numberOnly}")
    public List<PairwiseRelationship> queryRelationshipsForProteins(@PathVariable("numberOnly") Boolean numberOnly,
                                                                    @RequestBody PairwiseRelRequest wrap) {
    	if (wrap.getDataDescs() == null || wrap.getGenes() == null)
            return new ArrayList<>(); // Nothing to return
        return pairwiseService.queryRelsForProteins(wrap.getGenes(), wrap.getDataDescs(), numberOnly);
    }
    
    /**
     * Only one term (a gene or UniProt) should be used as an input to genes 
     * @param wrap
     * @return
     */
    @PostMapping("/pairwise/term/{numberOnly}")
    public List<PairwiseRelationship> queryRelationshipsForTerm(@PathVariable("numberOnly") Boolean numberOnly,
                                                                    @RequestBody PairwiseRelRequest wrap) {
    	if (wrap.getDataDescs() == null || wrap.getGenes() == null)
            return new ArrayList<>(); // Nothing to return
    	String term = wrap.getGenes().get(0); // Only one term used
        return pairwiseService.queryRelsForTerm(term, wrap.getDataDescs(), numberOnly);
    }
    
    @CrossOrigin
    @GetMapping("/relationships/genesForPathway/{stId}")
    public Pathway queryPathwayToGeneRelationship(@PathVariable("stId") String stId) {
    	return pairwiseService.queryPathwayToGeneRelationships(stId.toUpperCase());
    }
    
    @CrossOrigin
    @GetMapping("/relationships/uniprotsForPathway/{stId}")
    public Pathway queryPathwayToUniprotRelationship(@PathVariable("stId") String stId) {
    	return pairwiseService.queryPathwayToUniprotRelationships(stId.toUpperCase());
    }
    
    @CrossOrigin
    @GetMapping("/realtionships/pathwayStIdsForTerm/{term}")
    public Set<String> getPathwayStIdsForTerm(@PathVariable("term")String term){
    	return pairwiseService.getPathwayStIdsForTerm(term);
    }
    
    @CrossOrigin
    @GetMapping("/realtionships/getHierarchicalOrderedPathways")
    public List<GraphPathway> getHierarchicalOrderedPathways(){
    	return pairwiseService.getHierarchicalOrderedPathways();
    }
    
    /**
     * Gets hit PEs in a diagram for passed in term's interactors based on data descriptions.
     * If no data descriptions are passed in, prd will be used for combined scores.
     * @param request
     * @return
     */
    @CrossOrigin
    @PostMapping("/relationships/PEsForTermInteractors")
    public PEsForInteractorResponse queryPEsForTermInteractor(@RequestBody PEsForInteractorAndDataDescsWrapper request) {
    	PEsForInteractorResponse rtn;
    	try {
    		rtn = pairwiseService.queryPEsForTermInteractor(request.getDbId(), 
    												request.getTerm(),
    												request.getDataDescKeys(), 
    												request.getPrd() != null ? request.getPrd(): 0.9d);
    	} catch(IOException e) {
    		logger.error(e.getMessage(), e);
    		throw new InternalServerError(e.getMessage());
    	}
    	if(rtn.getPeIds() == null ) {
    		String msg = "Physical entities not found for " + request.getTerm();
    		ResourceNotFoundException e = new ResourceNotFoundException(msg);
    		logger.warn(msg, e);
    		throw e;
    	}
    	return rtn;
    }
    
    @CrossOrigin
    @GetMapping("relationships/primaryPathwaysForGene/{gene}")
    public List<Pathway> queryPrimaryPathwaysForGene(@PathVariable("gene") String gene){
    	List<Pathway> rtn = pairwiseService.queryPrimaryPathwaysForGene(gene.toUpperCase());
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
    public List<Pathway> queryPrimaryPathwaysForUniprot(@PathVariable("uniprot")String uniprot) {
    	List<Pathway> rtn = pairwiseService.queryPrimaryPathwaysForUniprot(uniprot.toUpperCase());
    	if(rtn == null) throw new ResourceNotFoundException(uniprot + " not found.");
    	else return rtn;
    } 
    
    /**
     * Performs enrichment analysis on interactors for term (gene symbol or uniprot) based on passed in data descriptions.
     * If no data descriptions or passed in, passed in PRD cutoff will be used to query combined score pathways.
     * @param request
     * @return
     */
    @CrossOrigin
    @PostMapping(path="/relationships/enrichedSecondaryPathwaysForTerm")
    public List<Pathway> enrichedPathwaysForTerm(@RequestBody GeneToPathwaysRequestWrapper request){
    	if(request == null || request.getTerm() == null)
    		return new ArrayList<>();
    	// Add a delay to workaround a bug in the idg pathwaybrowser: the fireworks cannot be highlighted
    	// because of an error that needs quite some time to fix in GWT projects.
    	try {
    	    Thread.sleep(1000);
    	}
    	catch(InterruptedException e) {}
    	return pairwiseService.queryTermToSecondaryPathwaysWithEnrichment(request.getTerm(), 
    															  request.getDataDescKeys(), 
    															  request.getPrd() != null ? request.getPrd() : 0.9d);
    }
    
    /**
     * Performs enrichment analysis on interactors for term (gene symbol or uniprot) based on passed in data descriptions.
     * If no data descriptions or passed in, passed in PRD cutoff will be used to query combined score pathways.
     * Note: This version has not added any delay and should be used for the vuejs based homepage app.
     * @param request
     * @return
     */
    @CrossOrigin
    @PostMapping(path="/relationships/enrichedSecondaryPathwaysForTerm1")
    public List<Pathway> enrichedPathwaysForTermNoDelay(@RequestBody GeneToPathwaysRequestWrapper request){
        if(request == null || request.getTerm() == null)
            return new ArrayList<>();
        return pairwiseService.queryTermToSecondaryPathwaysWithEnrichment(request.getTerm(), 
                                                                  request.getDataDescKeys(), 
                                                                  request.getPrd() != null ? request.getPrd() : 0.9d);
    }
    
    @CrossOrigin
    @PostMapping(path="/relationships/network/enrichedSecondaryPathaysForTerm")
    public List<Element> enrichedNetworkPathwaysForTerm(@RequestBody GeneToPathwaysRequestWrapper request){
    	if(request == null || request.getTerm() == null) return Collections.emptyList();
    	return pairwiseService.queryTermToSecondaryPathwaysNetworkWithEnrichment(request.getTerm(),
    																			 request.getDataDescKeys(),
    																			 request.getPrd() != null ? request.getPrd() : 0.9d);
    }
    
    @CrossOrigin
    @PostMapping(path="/relationships/enrichedSecondaryPathwaysForTerm/download")
    public void downloadEnrichedSecondaryPathwaysForTerm(@RequestBody GeneToPathwaysRequestWrapper request, HttpServletResponse response) {
    	String csv = pairwiseService.downloadTermToSecondaryPathwaysWithEnrichment(request.getTerm(), 
    																	request.getDataDescKeys(), 
    																	request.getPrd() != null ? request.getPrd(): 0.9d);
    	response.setContentType("blob");
    	ServletOutputStream writer;
		try {
			writer = response.getOutputStream();
			writer.print(csv);
			writer.flush();
	    	writer.close();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new InternalServerError("We experienced an error and are working to fix it!");
		}
    	
    }
    
    /**
     * Supports uniprot and standard gene symbol
     * @param term
     * @return
     */
    @CrossOrigin
    @GetMapping("/relationships/hierarchyForTerm/{term}")
    public HierarchyResponseWrapper queryHierarchyForTerm(@PathVariable("term")String term) {
    	return pairwiseService.queryHierarchhyForTerm(term);
    }
    
    /**
     * Gets combined score pathways for Term and PRD
     * If no prd passed in, throw 404 error.
     * @param request
     * @return
     */
    @CrossOrigin
    @PostMapping("/relationships/combinedScoreForTerm")
    public List<Pathway> queryEnrichedPathwaysForCombinedScore(@RequestBody GeneToPathwaysRequestWrapper request){
    	if(request.getPrd() == null)
    		throw new ResourceNotFoundException("request must include a cutoff value");
    	
    	return pairwiseService.queryEnrichedPathwaysForCombinedScore(request.getTerm(), request.getPrd());
    }
    
    @CrossOrigin
    @PostMapping("/relationships/dataDescsForKeys")
    public List<String> queryDataDescsForKeys(@RequestBody List<Integer> request){
    	return pairwiseService.getDataDescIdsForDigitalKeys(request);
    }
    
    /**
     * For a passed in term, return a map of gene name to expression value for all combined_score interactors.
     * @param term
     * @return
     */
    @CrossOrigin
    @GetMapping("/relationships/combinedScoreGenesForTerm/{term}")
	public Map<String, Double> queryCombinedScoreGenesForTerm(@PathVariable("term") String term) {
		return pairwiseService.queryCombinedScoreGenesForTerm(term);
	}
    
    @CrossOrigin
    @PostMapping("/download/FeaturesForTermAndInteractors")
    public void queryFeaturesForTermAndInteractors(@RequestBody FeatureForTermInteractorsWrapper request, HttpServletResponse response) {
    	String csv = pairwiseService.queryFeaturesForTermAndInteractors(request.getTerm(), request.getInteractors());
    	response.setContentType("blob");
    	ServletOutputStream writer;
		try {
			writer = response.getOutputStream();
			writer.print(csv);
			writer.flush();
	    	writer.close();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new InternalServerError("We experienced an error and are working to fix it!");
		}
    }
    
    //TODO: swagger document for ws API design
}
