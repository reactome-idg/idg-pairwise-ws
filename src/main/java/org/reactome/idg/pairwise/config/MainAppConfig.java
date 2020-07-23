package org.reactome.idg.pairwise.config;

import java.util.Arrays;

import org.reactome.idg.pairwise.service.PairwiseServiceConfig;
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
    public PairwiseServiceConfig getPairwiseServiceConfig() {
    	PairwiseServiceConfig config = new PairwiseServiceConfig();
    	config.setCoreWSURL(coreWSUrl);
    	return config;
    }
    
}
