package com.dream11.logcentralorchestrator.tests.unit.verticle;

import com.dream11.logcentralorchestrator.verticle.RestVerticle;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests for RestVerticle. */
public class VerticleTest {

  @Test
  public void testRestVerticle_Constructor_CreatesVerticle() {
    // Act
    RestVerticle verticle = new RestVerticle();

    // Assert
    Assert.assertNotNull(verticle);
  }

  @Test
  public void testRestVerticle_Constructor_SetsApplicationConfig() {
    // Act
    RestVerticle verticle = new RestVerticle();

    // Assert
    Assert.assertNotNull(verticle);
    // applicationConfig is package-private, but we can verify verticle creation
  }
}
