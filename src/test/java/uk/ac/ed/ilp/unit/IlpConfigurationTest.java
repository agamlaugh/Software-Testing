package uk.ac.ed.ilp.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import uk.ac.ed.ilp.config.IlpConfiguration;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IlpConfigurationTest {

    @Test
    @DisplayName("Each profile exposes the expected configuration bean")
    void profilesExposeExpectedBeans() {
        try (AnnotationConfigApplicationContext context = createContextWithProfile("local")) {
            IlpConfiguration config = context.getBean("localConfiguration", IlpConfiguration.class);
            assertThat(config.isDebug()).isTrue();
            assertThat(config.getTimeout()).isEqualTo(30_000);
            assertThat(config.getMaxRetries()).isEqualTo(3);
            assertThrows(NoSuchBeanDefinitionException.class,
                    () -> context.getBean("productionConfiguration", IlpConfiguration.class));
        }
        try (AnnotationConfigApplicationContext context = createContextWithProfile("production")) {
            IlpConfiguration config = context.getBean("productionConfiguration", IlpConfiguration.class);
            assertThat(config.isDebug()).isFalse();
            assertThat(config.getTimeout()).isEqualTo(10_000);
            assertThat(config.getMaxRetries()).isEqualTo(1);
            assertThrows(NoSuchBeanDefinitionException.class,
                    () -> context.getBean("localConfiguration", IlpConfiguration.class));
            assertThrows(NoSuchBeanDefinitionException.class,
                    () -> context.getBean("testConfiguration", IlpConfiguration.class));
        }
        try (AnnotationConfigApplicationContext context = createContextWithProfile("test")) {
            IlpConfiguration config = context.getBean("testConfiguration", IlpConfiguration.class);
            assertThat(config.isDebug()).isTrue();
            assertThat(config.getTimeout()).isEqualTo(5_000);
            assertThat(config.getMaxRetries()).isEqualTo(1);
            assertThrows(NoSuchBeanDefinitionException.class,
                    () -> context.getBean("localConfiguration", IlpConfiguration.class));
        }
    }

    private AnnotationConfigApplicationContext createContextWithProfile(String profile) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles(profile);
        context.register(IlpConfiguration.class);
        context.refresh();
        return context;
    }
}
