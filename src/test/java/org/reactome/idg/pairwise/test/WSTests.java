package org.reactome.idg.pairwise.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.junit.Test;
import org.reactome.idg.pairwise.model.FeatureForTermInteractorsWrapper;
import org.reactome.idg.pairwise.model.GeneToPathwaysRequestWrapper;
import org.reactome.idg.pairwise.model.PEsForInteractorAndDataDescsWrapper;
import org.reactome.idg.pairwise.model.PairwiseRelRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WSTests {
    protected final String HOST_URL = "http://localhost:8043/idgpairwise";
    protected final String HTTP_POST = "Post";
    protected final String HTTP_GET = "Get";

    
    @Test
    public void testUniProtToGene() throws Exception {
        String url = HOST_URL + "/uniprot2gene";
        System.out.println(url);
        String rtn = callHttp(url, HTTP_GET, null);
        System.out.println(rtn);
    }
    
    @Test
    public void testListDataDescs() throws Exception {
        String url = HOST_URL + "/datadesc";
        System.out.println(url);
        String rtn = callHttp(url, HTTP_GET, null);
        outputJSON(rtn);
    }
    
    @Test
    public void testListDataDescsForTerm() throws Exception {
    	String url = HOST_URL + "/datadesc/NTN1";
    	System.out.println(url);
    	String rtn = callHttp(url, HTTP_GET,null);
    	outputJSON(rtn);
    }
    
    @Test
    public void testQueryRelsForGenes() throws Exception {
    	ObjectMapper mapper = new ObjectMapper();
        String url = HOST_URL + "/pairwise/genes/false";
        List<String> genes = Stream.of("EGF","EGFR","TP53","NOTCH1").collect(Collectors.toList());
        List<String> descIds = Stream.of("GTEx|Ovary|Gene_Coexpression","GTEx|Breast-MammaryTissue|Gene_Coexpression","Harmonizome|human|Gene_Similarity|ctddisease").collect(Collectors.toList());
        PairwiseRelRequest query = new PairwiseRelRequest(genes, descIds);
        System.out.println(url + ": " + descIds);
        String rtn = callHttp(url, HTTP_POST, mapper.writeValueAsString(query));
        outputJSON(rtn);
    }
    
    @Test
    public void testQueryRelsForProteins() throws Exception {
    	ObjectMapper mapper = new ObjectMapper();
        String url = HOST_URL + "/pairwise/uniprots/false";
        List<String> genes = Stream.of("P01133","P00533","P04637","P46531").collect(Collectors.toList());
        List<String> descIds = Stream.of("GTEx|Ovary|Gene_Coexpression","GTEx|Breast-MammaryTissue|Gene_Coexpression","Harmonizome|human|Gene_Similarity|ctddisease").collect(Collectors.toList());
        PairwiseRelRequest query = new PairwiseRelRequest(genes, descIds);
        System.out.println(url + ": " + descIds);
        String rtn = callHttp(url, HTTP_POST, mapper.writeValueAsString(query));
        outputJSON(rtn);
    }
    
    @Test
    public void testQueryHierarchyForTerm() throws Exception {
    	String url = HOST_URL + "/relationships/hierarchyForTerm/NTN1";
    	System.out.println(url);
    	String rtn = callHttp(url, HTTP_GET, null);
    	outputJSON(rtn);
    }
    
    @Test
    public void testQueryGenesForPathway() throws Exception {
    	String url = HOST_URL + "/relationships/genesForPathway/R-HSA-198753";
    	System.out.println(url);
    	String rtn = callHttp(url, HTTP_GET, null);
    	outputJSON(rtn);
    }
    
    @Test
    public void testQueryUniprotsForPathway() throws Exception { 
    	String url = HOST_URL + "/relationships/uniprotsForPathway/R-HSA-198753";
    	System.out.println(url);
    	String rtn = callHttp(url, HTTP_GET, null);
    	outputJSON(rtn);
    }
    
    @Test
    public void testQueryPEsForTermInteractor() throws Exception {
    	ObjectMapper mapper = new ObjectMapper();
    	String url = HOST_URL + "/relationships/PEsForTermInteractors";
    	System.out.println(url);
    	PEsForInteractorAndDataDescsWrapper postData = new PEsForInteractorAndDataDescsWrapper();
    	postData.setDbId(373760L);
    	postData.setTerm("PRKY");
    	postData.setPrd(0.9);
    	String json = mapper.writeValueAsString(postData);
    	System.out.println(json);
    	String rtn = callHttp(url, HTTP_POST, json);
    	outputJSON(rtn);
    }
    
    @Test
    public void testGetDataDescriptionsForKeys() throws Exception {
    	ObjectMapper mapper = new ObjectMapper();
    	String url = HOST_URL + "/relationships/dataDescsForKeys";
    	System.out.println(url);
    	List<Integer> descKeys = Arrays.asList(0);
    	String json = mapper.writeValueAsString(descKeys);
    	System.out.println(json);
    	String rtn = callHttp(url, HTTP_POST, json);
    	outputJSON(rtn);
    }
    
    @Test
    public void testQueryPrimaryPathwaysForGene() throws Exception {
    	String url = HOST_URL + "/relationships/primaryPathwaysForGene/NTN1";
    	System.out.println(url);
    	String rtn = callHttp(url, HTTP_GET, null);
    	outputJSON(rtn);
    }
    
    @Test
    public void testQuesryPrimaryPathwaysForUniprot() throws Exception {
    	String url = HOST_URL + "/relationships/primaryPathwaysForUniprot/O95631";
    	System.out.println(url);
    	String rtn = callHttp(url, HTTP_GET, null);
    	outputJSON(rtn);
    }
    
    @Test
    public void testQueryCombinedScoreGenesForGene() throws Exception {
    	String url = HOST_URL + "/relationships/combinedScoreGenesForTerm/NTN1";
    	System.out.println(url);
    	String rtn = callHttp(url, HTTP_GET, null);
    	outputJSON(rtn);
    }
    
    @Test
    public void testQueryCombinedScoreGenesForUniprot() throws Exception {
    	String url = HOST_URL + "/relationships/combinedScoreGenesForTerm/O95631";
    	System.out.println(url);
    	String rtn = callHttp(url, HTTP_GET, null);
    	outputJSON(rtn);
    }
    
    @Test
    public void testEnrichInteractorsForGene() throws Exception {
    	ObjectMapper mapper = new ObjectMapper();
    	String url = HOST_URL + "/relationships/enrichedSecondaryPathwaysForTerm";
    	System.out.println(url);
    	GeneToPathwaysRequestWrapper postData = new GeneToPathwaysRequestWrapper();
    	postData.setTerm("NTN1");
    	postData.setDataDescKeys(Collections.singletonList(1));
    	String json = mapper.writeValueAsString(postData);
    	System.out.println(json);
    	Long time1 = System.currentTimeMillis();
    	String rtn = callHttp(url, HTTP_POST, json);
    	Long time2 = System.currentTimeMillis() - time1;
    	outputJSON(rtn);
    	System.out.println(time2);
    }
    
    @Test
    public void testCombinedScoreEnrichedInteractorsForTerm() throws Exception {
    	ObjectMapper mapper = new ObjectMapper();
    	String url = HOST_URL + "/relationships/network/enrichedSecondaryPathaysForTerm";
    	System.out.println(url);
    	GeneToPathwaysRequestWrapper postData = new GeneToPathwaysRequestWrapper();
    	postData.setTerm("NTN1");
    	postData.setDataDescKeys(Collections.singletonList(0));
    	postData.setPrd(0.9d);
    	String json = mapper.writeValueAsString(postData);
    	System.out.println(json);
    	Long time1 = System.currentTimeMillis();
    	String rtn = callHttp(url, HTTP_POST, json);
    	Long time2 = System.currentTimeMillis();
    	outputJSON(rtn);
    	System.out.println(time2-time1);
    }
    
    @Test
    public void testDownloadEnrichInteractorsForGene() throws Exception {
    	ObjectMapper mapper = new ObjectMapper();
    	String url = HOST_URL + "/relationships/enrichedSecondaryPathwaysForTerm/download";
    	System.out.println(url);
    	GeneToPathwaysRequestWrapper postData = new GeneToPathwaysRequestWrapper();
    	postData.setTerm("NTN1");
    	postData.setDataDescKeys(Collections.singletonList(1));
    	String json = mapper.writeValueAsString(postData);
    	System.out.println(json);
    	String rtn = callHttp(url, HTTP_POST, json);
    	System.out.println(rtn);
    	
    }
    
    @Test
    public void testQuantityOfPathwaysAndStIdsForGene() throws Exception {
    	ObjectMapper mapper = new ObjectMapper();
    	String url = HOST_URL + "/relationships/primaryPathwaysForGene/EGFR";
    	String rtn = callHttp(url, HTTP_GET, null);
        List<?> obj = mapper.readValue(rtn, ArrayList.class);
        System.out.println("Number of Pathways: " + obj.size());
        url = HOST_URL + "/realtionships/pathwayStIdsForTerm/EGFR";
        rtn = callHttp(url, HTTP_GET, null);
        List<?> obj2 = mapper.readValue(rtn, ArrayList.class);
        System.out.println("Number of Pathway stIds: " + obj2.size());
        assert(obj.size() == obj2.size());
    }
    
    @Test
    public void testCombinedScoreForTerm() throws Exception {
    	ObjectMapper mapper = new ObjectMapper();
    	String url = HOST_URL + "/relationships/combinedScoreForTerm";
    	System.out.println(url);
    	GeneToPathwaysRequestWrapper postData = new GeneToPathwaysRequestWrapper();
    	postData.setTerm("NTN1");
    	postData.setPrd(0.5d);
    	String json = mapper.writeValueAsString(postData);
    	System.out.println(json);
    	String rtn = callHttp(url, HTTP_POST, json);
    	outputJSON(rtn);
    }
    
    @Test
    public void testGetHierarchyForTerm() throws Exception {
    	String url = HOST_URL + "/relationships/hierarchyForTerm/MAPK3";
    	System.out.println(url);
    	String rtn = callHttp(url,HTTP_GET, null);
    	outputJSON(rtn);
    }
    
    @Test
    public void testGetHierarchicalOrderedPathways() throws Exception {
    	String url = HOST_URL + "/realtionships/getHierarchicalOrderedPathways";
    	System.out.println(url);
    	String rtn = callHttp(url,HTTP_GET, null);
    	outputJSON(rtn);
    }
    
    @Test
    public void testDownloadFeaturesForInteractors() throws Exception {
    	ObjectMapper mapper = new ObjectMapper();
    	String url = HOST_URL + "/download/FeaturesForTermAndInteractors";
    	System.out.println(url);
    	List<String> interactors = new ArrayList<>();
    	interactors.add("UNC5A");
    	interactors.add("UNC5B");
    	interactors.add("UNC5C");
    	FeatureForTermInteractorsWrapper postData = new FeatureForTermInteractorsWrapper("NTN1", interactors);
    	String json = mapper.writeValueAsString(postData);
    	System.out.println(json);
    	String rtn = callHttp(url, HTTP_POST, json);
    	System.out.println(rtn);
    }
    
    private void outputJSON(String json) throws JsonProcessingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        Object obj = mapper.readValue(json, Object.class);
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj));
    }

    protected String callHttp(String url,
                              String type,
                              String query) throws IOException {
        HttpMethod method = null;
        HttpClient client = null;
        if (type.equals(HTTP_POST)) {
            method = new PostMethod(url);
            method.addRequestHeader("content-type", "application/json");
            client = initializeHTTPClient((PostMethod) method, query);
        } else {
            method = new GetMethod(url); // Default
            client = new HttpClient();
        }
        method.setRequestHeader("Accept", "application/json");
        int responseCode = client.executeMethod(method);
        if (responseCode == HttpStatus.SC_OK) {
            InputStream is = method.getResponseBodyAsStream();
            return readMethodReturn(is);
        } else {
            System.err.println("Error from server: " + method.getResponseBodyAsString());
            System.out.println("Response code: " + responseCode);
            throw new IllegalStateException(method.getResponseBodyAsString());
        }
    }

    protected String readMethodReturn(InputStream is) throws IOException {
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader reader = new BufferedReader(isr);
        StringBuilder builder = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null)
            builder.append(line).append("\n");
        reader.close();
        isr.close();
        is.close();
        // Remove the last new line
        String rtn = builder.toString();
        // Just in case an empty string is returned
        if (rtn.length() == 0)
            return rtn;
        return rtn.substring(0, rtn.length() - 1);
    }

    private HttpClient initializeHTTPClient(PostMethod post, String query) throws UnsupportedEncodingException {
        RequestEntity entity = new StringRequestEntity(query, "text/plain", "UTF-8");
        //        RequestEntity entity = new StringRequestEntity(query, "application/XML", "UTF-8");
        post.setRequestEntity(entity);
        //        post.setRequestHeader("Accept", "application/JSON, application/XML, text/plain");
        post.setRequestHeader("Accept", "application/json");
        HttpClient client = new HttpClient();
        return client;
    }

}
