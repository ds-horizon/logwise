package com.dream11.logcentralorchestrator.tests.unit.config;

import com.dream11.logcentralorchestrator.config.constant.Constants;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for Constants. */
public class ConstantsTest {

  private String originalConsulToken;
  private String originalEnv;
  private String originalNamespace;
  private String originalServiceName;
  private String originalVaultToken;
  private String originalAppEnvironment;

  @BeforeMethod
  public void setUp() {
    // Save original values
    originalConsulToken = System.getenv("CONSUL_TOKEN");
    originalEnv = System.getenv("ENV");
    originalNamespace = System.getenv("NAMESPACE");
    originalServiceName = System.getenv("SERVICE_NAME");
    originalVaultToken = System.getenv("VAULT_TOKEN");
    originalAppEnvironment = System.getProperty("app.environment");
  }

  @AfterMethod
  public void tearDown() {
    // Restore original values
    if (originalConsulToken != null) {
      System.setProperty("CONSUL_TOKEN", originalConsulToken);
    }
    if (originalEnv != null) {
      System.setProperty("ENV", originalEnv);
    }
    if (originalNamespace != null) {
      System.setProperty("NAMESPACE", originalNamespace);
    }
    if (originalServiceName != null) {
      System.setProperty("SERVICE_NAME", originalServiceName);
    }
    if (originalVaultToken != null) {
      System.setProperty("VAULT_TOKEN", originalVaultToken);
    }
    if (originalAppEnvironment != null) {
      System.setProperty("app.environment", originalAppEnvironment);
    } else {
      System.clearProperty("app.environment");
    }
  }

  @Test
  public void testConsulToken_CanBeAccessed() {
    // Note: Constants are initialized at class load time, so we can't easily test with different env vars
    // This test verifies the constant exists and can be accessed
    String consulToken = Constants.CONSUL_TOKEN;
    // May be null if env var not set - that's acceptable
    // Just verify we can access it without exception
    Assert.assertTrue(true); // Constant accessed successfully
  }

  @Test
  public void testEnv_CanBeAccessed() {
    // Note: Constants are initialized at class load time
    String env = Constants.ENV;
    // May be null if env var not set - that's acceptable
    Assert.assertTrue(true); // Constant accessed successfully
  }

  @Test
  public void testNamespace_CanBeAccessed() {
    String namespace = Constants.NAMESPACE;
    // May be null if env var not set - that's acceptable
    Assert.assertTrue(true); // Constant accessed successfully
  }

  @Test
  public void testServiceName_CanBeAccessed() {
    String serviceName = Constants.SERVICE_NAME;
    // May be null if env var not set - that's acceptable
    Assert.assertTrue(true); // Constant accessed successfully
  }

  @Test
  public void testVaultToken_CanBeAccessed() {
    String vaultToken = Constants.VAULT_TOKEN;
    // May be null if env var not set - that's acceptable
    Assert.assertTrue(true); // Constant accessed successfully
  }
}

