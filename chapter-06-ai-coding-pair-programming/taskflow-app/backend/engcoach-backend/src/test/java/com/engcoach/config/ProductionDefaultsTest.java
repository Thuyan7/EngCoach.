package com.engcoach.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.UrlResource;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P7.3 / finding R3: with no environment variables set, the shipped
 * configuration must be safe: no seeding, and no built-in database password or
 * JWT secret (startup fails instead). Local-development conveniences live in
 * the "dev" profile. The test reads the real src/main configuration files,
 * because src/test/resources/application.yml shadows them in other tests.
 */
class ProductionDefaultsTest {

    private static final List<String> SEED_FLAGS = List.of(
            "engcoach.question-bank.seed-samples",
            "engcoach.question-bank.import-generated",
            "engcoach.question-bank.refresh-generated");

    /** Finds a configuration file in target/classes (main), not in target/test-classes. */
    private static URL mainResource(String name) throws Exception {
        Enumeration<URL> urls = ProductionDefaultsTest.class.getClassLoader().getResources(name);
        for (URL url : Collections.list(urls)) {
            if (!url.toString().contains("test-classes")) {
                return url;
            }
        }
        return null;
    }

    /** Resolver over the given main files (first wins) and an optional fake environment. */
    private static PropertySourcesPropertyResolver resolver(Map<String, Object> environment,
                                                             String... files) throws Exception {
        MutablePropertySources sources = new MutablePropertySources();
        sources.addLast(new MapPropertySource("environment", environment));
        for (String file : files) {
            URL url = mainResource(file);
            assertNotNull(url, file + " must exist in src/main/resources");
            List<PropertySource<?>> loaded = new YamlPropertySourceLoader().load(file, new UrlResource(url));
            for (PropertySource<?> source : loaded) {
                sources.addLast(source);
            }
        }
        return new PropertySourcesPropertyResolver(sources);
    }

    @Test
    void seedingFlags_defaultToFalse() throws Exception {
        PropertySourcesPropertyResolver resolver = resolver(Map.of(), "application.yml");

        for (String flag : SEED_FLAGS) {
            assertEquals("false", resolver.getProperty(flag), flag);
        }
    }

    @Test
    void jwtSecret_hasNoDefault_andIsReadFromTheEnvironment() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> resolver(Map.of(), "application.yml").getProperty("engcoach.jwt.secret"));

        assertEquals("from-env", resolver(Map.of("JWT_SECRET", "from-env"), "application.yml")
                .getProperty("engcoach.jwt.secret"));
    }

    @Test
    void databasePassword_hasNoDefault_andIsReadFromTheEnvironment() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> resolver(Map.of(), "application.yml").getProperty("spring.datasource.password"));

        assertEquals("from-env", resolver(Map.of("DB_PASSWORD", "from-env"), "application.yml")
                .getProperty("spring.datasource.password"));
    }

    @Test
    void devProfile_keepsTheLocalDevelopmentConveniences() throws Exception {
        List<String> files = new ArrayList<>(List.of("application-dev.yml", "application.yml"));
        PropertySourcesPropertyResolver resolver = resolver(Map.of(), files.toArray(new String[0]));

        for (String flag : SEED_FLAGS) {
            assertEquals("true", resolver.getProperty(flag), flag);
        }
        assertEquals("1234", resolver.getProperty("spring.datasource.password"));
        String secret = resolver.getProperty("engcoach.jwt.secret");
        assertNotNull(secret);
        assertTrue(secret.length() >= 32, "the dev JWT secret must satisfy the 32-character minimum");
    }
}
