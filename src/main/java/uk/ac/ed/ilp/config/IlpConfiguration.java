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
