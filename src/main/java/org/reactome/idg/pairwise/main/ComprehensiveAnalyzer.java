package org.reactome.idg.pairwise.main;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.fluent.Content;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.fluent.Response;
import org.apache.hc.core5.http.ContentType;
import org.junit.Test;
import org.reactome.idg.pairwise.model.GeneToPathwaysRequestWrapper;
import org.reactome.idg.pairwise.model.Pathway;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to do a comprehensive analysis of dark proteins in the context of Reactome pathways. To run methods
 * in this class, this web service app should be up and running. The RESTful API is called to get all information needed.
 */
public class ComprehensiveAnalyzer {
    // The local WS API started from mvn tomcat7:run
    private final String TCRD_WS_URL = "http://localhost:8060/tcrdws";
    private final String PAIRWISE_UR_URL = "http://localhost:8043/idgpairwise";

    public ComprehensiveAnalyzer() {
    }

    /**
     * Get a list of dark proteins. To run this method, make sure the tcrdws is running.
     * @return
     * @throws Exception
     */
    public List<String> getDarkProteins() throws Exception {
        String url = TCRD_WS_URL + "/tdark/uniprots";
        ObjectMapper mapper = new ObjectMapper();
        URL urlAddress = new URL(url);
        // For get-based URL, we may use this simple API
        List<String> list = mapper.readValue(urlAddress, new TypeReference<List<String>>(){});
        return list;
    }

    /**
     * Get a list of proteins for a UniProt id.
     *
     * @param term
     * @return
     * @throws Exception
     */
    public List<Pathway> queryInteractingPathways(String term) throws Exception {
        String url = PAIRWISE_UR_URL + "/relationships/combinedScoreForTerm";
        GeneToPathwaysRequestWrapper wrapper = new GeneToPathwaysRequestWrapper();
        wrapper.setTerm(term);
        wrapper.setPrd(0.0d); // Pick up all pathways
        ObjectMapper mapper = new ObjectMapper();
        Content content = Request.post(url)
                .bodyString(mapper.writeValueAsString(wrapper), ContentType.APPLICATION_JSON)
                .execute()
                .returnContent();
        List<Pathway> pathways = mapper.readValue(content.toString(), new TypeReference<List<Pathway>>(){});
        return pathways;
    }

    public List<String> getPathwayGenes() throws Exception {
    	return new ArrayList<>();
    }

    @Test
    public void testQueryInteractingPathways() throws Exception {
        String term = "Q8N5C1"; // CALHM5
        List<Pathway> pathways = queryInteractingPathways(term);
        System.out.println("Total pathways: " + pathways.size());
        Pathway pathway = pathways.stream().findAny().get();
        System.out.println("Pathway: " + pathway);
    }


}
