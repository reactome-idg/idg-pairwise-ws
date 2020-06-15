package org.reactome.idg.pairwise.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.reactome.idg.pairwise.model.DataDesc;
import org.reactome.idg.pairwise.model.GeneToPathwayRelationship;
import org.reactome.idg.pairwise.model.PairwiseRelationship;
import org.reactome.idg.pairwise.model.PathwayToGeneRelationship;
import org.reactome.idg.pairwise.service.PairwiseService;
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
     * @param txt
     * @return
     */
    @PostMapping("/pairwise/genes/{numberOnly}")
    public List<PairwiseRelationship> queryRelationshipsForGenes(@PathVariable("numberOnly") Boolean numberOnly,
                                                                 @RequestBody String txt) {
        String[] lines = txt.split("\n");
        if (lines.length < 2)
            return new ArrayList<>(); // Nothing to return
        // The first line should be desc ids
        List<String> descIds = Arrays.asList(lines[0].split(","));
        List<String> genes = Arrays.asList(lines[1].split(","));
        return service.queryRelsForGenes(genes, descIds, numberOnly);
    }
    
    /**
     * There should be two lines in the post body:
     * Line 1: "," delimited DataDesc ids
     * List 2: "," delimited protein uniprot accession numbers
     * @param txt
     * @return
     */
    @PostMapping("/pairwise/uniprots/{numberOnly}")
    public List<PairwiseRelationship> queryRelationshipsForProteins(@PathVariable("numberOnly") Boolean numberOnly,
                                                                    @RequestBody String txt) {
        String[] lines = txt.split("\n");
        if (lines.length < 2)
            return new ArrayList<>(); // Nothing to return
        // The first line should be desc ids
        List<String> descIds = Arrays.asList(lines[0].split(","));
        List<String> genes = Arrays.asList(lines[1].split(","));
        return service.queryRelsForProteins(genes, descIds, numberOnly);
    }

    @CrossOrigin
    @GetMapping("/relationships/pathwaysForGene/{gene}")
    public GeneToPathwayRelationship queryGeneToPathwayRelationship(@PathVariable("gene") String gene) {
    	return service.queryGeneToPathwayRelathinships(gene.toUpperCase());
    }
    
    @CrossOrigin
    @GetMapping("/realationships/pathwaysForUniprot/{uniprot}")
    public GeneToPathwayRelationship queryUniprotToPathwayRelationship(@PathVariable("uniprot")String uniprot) {
    	return service.queryUniprotToPathwayRelationships(uniprot.toUpperCase());
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
    
    //swagger document for ws API design
}
