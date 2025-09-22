package com.qa.project.bdd.runners;

import io.cucumber.junit.platform.engine.Constants;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * BDD Test Runner for User Authentication Features
 * 
 * Executes Cucumber BDD tests for User Registration and Login scenarios.
 * This runner uses JUnit Platform Suite to discover and execute all .feature files
 * in the classpath and their corresponding step definitions.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "com.qa.project.bdd.stepdefinitions,com.qa.project.bdd.config")
@ConfigurationParameter(key = Constants.PLUGIN_PROPERTY_NAME, value = "pretty")
@ConfigurationParameter(key = Constants.EXECUTION_DRY_RUN_PROPERTY_NAME, value = "false")
@ConfigurationParameter(key = Constants.FEATURES_PROPERTY_NAME, value = "classpath:features")
public class BDDTestRunner {
    
    /**
     * This class serves as the main entry point for running BDD tests.
     * 
     * Configuration Details:
     * - @SelectClasspathResource("features"): Looks for .feature files in src/test/resources/features
     * - GLUE_PROPERTY_NAME: Points to step definitions package
     * - PLUGIN_PROPERTY_NAME: Generates multiple report formats (console, HTML, JSON, XML)
     * - Reports are generated in target/cucumber-reports directory
     * 
     * Supported Features:
     * - user_registration.feature: User registration scenarios
     * - user_login.feature: User authentication scenarios
     * 
     * To run BDD tests:
     * - From IDE: Run this class as JUnit test
     * - From Maven: mvn test -Dtest="BDDTestRunner"
     * - All BDD tests: mvn test -Dtest="com.qa.project.bdd.**"
     */
}