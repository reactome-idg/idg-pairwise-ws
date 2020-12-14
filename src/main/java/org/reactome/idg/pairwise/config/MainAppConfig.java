package org.reactome.idg.pairwise.config;

import java.util.Arrays;

import org.reactome.idg.pairwise.service.ServiceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;

/**
 * Start up the configuration for a Main application, which is different from Spring Boot application.
 * Basically it is used to set up a database connection.
 * @author wug
 *
 */
@Configuration
@PropertySource("classpath:application.properties")
@ComponentScans({@ComponentScan("org.reactome.idg.pairwise.service")})
public class MainAppConfig {
    
    @Value("${mongo.host}")
    private String host;

    @Value("${mongo.db}")
    private String dbName;

    @Value("${mongo.user}")
    private String userName;

    @Value("${mongo.pwd}")
    private String password;
    
    @Value("${mongo.authentication.db}")
    private String authenticaionDb;
    
    @Value("${core.ws.service}")
    private String coreWSUrl;
    
    @Value("${gene.to.pathway.name}")
    private String geneToPathwayNameFile;
    
    @Value("${uniprot.to.reactome}")
    private String uniProt2Reactome;
    
    @Value("${uniprot.to.reactome.all}")
    private String uniProt2ReactomeAllLevels;
    
    @Value("${event.hierarchy.url}")
    private String eventHierarchyUrl;

    @Bean
    public MongoClient mongoClient() {
        MongoCredential credential = MongoCredential.createCredential(userName, authenticaionDb, password.toCharArray());
        MongoClient client = new MongoClient(new ServerAddress(host), Arrays.asList(credential));
        return client;
    }

    @Bean
    public MongoDatabase database() {
        return mongoClient().getDatabase(dbName);
    }
    
    @Bean
    public ServiceConfig getPairwiseServiceConfig() {
    	ServiceConfig config = new ServiceConfig();
    	config.setCoreWSURL(coreWSUrl);
    	config.setEventHierarchyUrl(eventHierarchyUrl);
    	config.setGeneToPathwayStIdFile(geneToPathwayNameFile);
    	config.setUniProt2ReactomeFile(uniProt2Reactome);
    	config.setUniProt2ReactomeAllLevelsFile(uniProt2ReactomeAllLevels);
    	
    	return config;
    }
}
