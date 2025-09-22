package com.qa.project.bdd.config;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Cucumber Spring configuration class.
 * This class provides the Spring context configuration for Cucumber BDD tests.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
public class CucumberSpringConfiguration {
    // This class serves as the Spring context configuration for Cucumber tests
    // No additional implementation needed - annotations provide the configuration
}