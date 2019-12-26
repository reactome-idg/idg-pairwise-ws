package org.reactome.idg.pairwise.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.reactome.idg.pairwise.model.DataDesc;
import org.reactome.idg.pairwise.model.PairwiseRelationship;
import org.reactome.idg.pairwise.service.PairwiseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller class provides the RESTful API for idg-pairwise relationships.
 * @author wug
 *
 */
@RestController
public class PairwiseController {
    
    @Autowired
    private PairwiseService service;
    
    public PairwiseController() {
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
    @PostMapping("/pairwise/genes")
    public List<PairwiseRelationship> queryRelationshipsForGenes(@RequestBody String txt) {
        String[] lines = txt.split("\n");
        if (lines.length < 2)
            return new ArrayList<>(); // Nothing to return
        // The first line should be desc ids
        List<String> descIds = Arrays.asList(lines[0].split(","));
        List<String> genes = Arrays.asList(lines[1].split(","));
        return service.queryRelsForGenes(genes, descIds);
    }
    
    /**
     * There should be two lines in the post body:
     * Line 1: "," delimited DataDesc ids
     * List 2: "," delimited protein uniprot accession numbers
     * @param txt
     * @return
     */
    @PostMapping("/pairwise/uniprots")
    public List<PairwiseRelationship> queryRelationshipsForProteins(@RequestBody String txt) {
        String[] lines = txt.split("\n");
        if (lines.length < 2)
            return new ArrayList<>(); // Nothing to return
        // The first line should be desc ids
        List<String> descIds = Arrays.asList(lines[0].split(","));
        List<String> genes = Arrays.asList(lines[1].split(","));
        return service.queryRelsForProteins(genes, descIds);
    }

}
