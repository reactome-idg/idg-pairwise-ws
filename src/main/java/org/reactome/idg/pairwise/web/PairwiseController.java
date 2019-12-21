package org.reactome.idg.pairwise.web;

import java.util.List;

import org.reactome.idg.pairwise.model.DataDesc;
import org.reactome.idg.pairwise.service.PairwiseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
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

}
