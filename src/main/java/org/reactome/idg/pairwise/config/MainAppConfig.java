package org.reactome.idg.pairwise.config;

import org.gk.persistence.MySQLAdaptor;
import org.reactome.idg.pairwise.service.ServiceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
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
    
    @Value("${event.hierarchy.url}")
    private String eventHierarchyUrl;
    @Value("${mysql.host}")
    private String mysqlHost;
    @Value("${mysql.db}")
    private String mysqlDb;
    @Value("${mysql.user}")
    private String mysqlUser;
    @Value("${mysql.pwd}")
    private String mysqlPwd;

    @Bean
    public MongoClient mongoClient() {
        String uri = String.format(
                "mongodb://%s:%s@%s/%s",
                userName,
                password,
                host,
                authenticaionDb
            );
            return MongoClients.create(uri);
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
    	
    	return config;
    }
    
    @Bean
    public MySQLAdaptor getMySQLDBA() throws Exception {
    	MySQLAdaptor dba = new MySQLAdaptor(mysqlHost, 
    			mysqlDb,
    			mysqlUser,
    			mysqlPwd);
    	dba.initDumbThreadForConnection(); // Make sure it is running always
    	return dba;
    }
}
