package uk.ac.ed.ilp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration class following Lecture 5 patterns
 * Demonstrates profile-based configuration
 */
@Configuration
@ConfigurationProperties(prefix = "ilp.service")
public class IlpConfiguration {

    private boolean debug;
    private int timeout;
    private int maxRetries;
    private String url; // For fallback from application.yml

    // Getters and setters
    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Bean to provide ILP endpoint URL from environment variable
     * Falls back to application.yml if environment variable not set
     * Based on CW2 requirements: endpoint can change between container restarts
     */
    @Bean
    public String ilpEndpoint() {
        // First try environment variable 
        String endpoint = System.getenv("ILP_ENDPOINT");
        
        // Fallback to application.yml if env var not set
        if (endpoint == null || endpoint.trim().isEmpty()) {
            endpoint = url; // From application.yml
        }
        
        // Final fallback to default URL if nothing is configured
        if (endpoint == null || endpoint.trim().isEmpty()) {
            endpoint = "https://ilp-rest-2025-bvh6e9hschfagrgy.ukwest-01.azurewebsites.net/";
        }
        
        // Ensure URL ends with / for proper path construction
        if (endpoint != null && !endpoint.endsWith("/")) {
            endpoint = endpoint + "/";
        }
        
        return endpoint;
    }

    /**
     * Local development configuration
     */
    @Bean
    @Profile("local")
    public IlpConfiguration localConfiguration() {
        IlpConfiguration config = new IlpConfiguration();
        config.setDebug(true);
        config.setTimeout(30000);
        config.setMaxRetries(3);
        return config;
    }

    /**
     * Production configuration
     */
    @Bean
    @Profile("production")
    public IlpConfiguration productionConfiguration() {
        IlpConfiguration config = new IlpConfiguration();
        config.setDebug(false);
        config.setTimeout(10000);
        config.setMaxRetries(1);
        return config;
    }

    /**
     * Test configuration
     */
    @Bean
    @Profile("test")
    public IlpConfiguration testConfiguration() {
        IlpConfiguration config = new IlpConfiguration();
        config.setDebug(true);
        config.setTimeout(5000);
        config.setMaxRetries(1);
        return config;
    }
}
