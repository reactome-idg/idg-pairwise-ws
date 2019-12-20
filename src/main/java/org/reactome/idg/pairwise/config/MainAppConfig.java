package org.reactome.idg.pairwise.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Start up the configuration for a Main application, which is different from Spring Boot application.
 * @author wug
 *
 */
@Configuration
@PropertySource("classpath:application.properties")
@ComponentScans({@ComponentScan("org.reactome.idg.pairwise.config"), // Make sure Database is initialized
                 @ComponentScan("org.reactome.idg.pairwise.service")})
public class MainAppConfig {
}
