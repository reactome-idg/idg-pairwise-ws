package org.reactome.idg.pairwise.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;

/**
 * This class is copied from the Couchbase Java SDK sample application 
 * (https://github.com/couchbaselabs/try-cb-java/tree/5.0-updates). It is used to provide
 * the database connection stuff.
 * @author wug
 */
@Configuration
public class Database {
    
    @Value("${storage.host}")
    private String host;

    @Value("${storage.bucket}")
    private String bucket;

    @Value("${storage.username}")
    private String username;

    @Value("${storage.password}")
    private String password;

    @Bean
    public Cluster couchbaseCluster() {
        CouchbaseCluster cluster = CouchbaseCluster.create(host);
        cluster.authenticate(username, password);
        return cluster;
    }

    @Bean
    public Bucket loginBucket() {
        return couchbaseCluster().openBucket(bucket);
    }

}
