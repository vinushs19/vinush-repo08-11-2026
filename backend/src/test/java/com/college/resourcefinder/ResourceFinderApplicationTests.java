package com.college.resourcefinder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spring Boot Application Context Verification Test.
 * Confirms that all core configurations, JPA entity mappings, and beans initialize successfully.
 */
@SpringBootTest
@ActiveProfiles("test")
class ResourceFinderApplicationTests {

    @Test
    @DisplayName("Spring Application Context should load without errors")
    void contextLoads() {
        // Verification succeeds if the Spring ApplicationContext starts cleanly.
    }
}
