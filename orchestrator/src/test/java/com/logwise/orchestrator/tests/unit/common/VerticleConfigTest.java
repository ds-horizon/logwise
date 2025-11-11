package com.dream11.logcentralorchestrator.tests.unit.common;

import com.dream11.logcentralorchestrator.common.app.VerticleConfig;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests for VerticleConfig. */
public class VerticleConfigTest {

  @Test
  public void testDefaultConfig_HasCorrectValues() {
    // Act
    VerticleConfig config = VerticleConfig.DEFAULT_CONFIG;

    // Assert
    Assert.assertNotNull(config);
    Assert.assertEquals(config.getInstances(), 1);
    Assert.assertEquals(config.getThreadPoolSize(), 0);
    Assert.assertEquals(config.getVerticleType(), 0);
  }

  @Test
  public void testBuilder_CreatesConfig() {
    // Act
    VerticleConfig config = VerticleConfig.builder()
        .instances(3)
        .threadPoolSize(10)
        .verticleType(1)
        .build();

    // Assert
    Assert.assertNotNull(config);
    Assert.assertEquals(config.getInstances(), 3);
    Assert.assertEquals(config.getThreadPoolSize(), 10);
    Assert.assertEquals(config.getVerticleType(), 1);
  }

  @Test
  public void testNoArgsConstructor_CreatesConfig() {
    // Act
    VerticleConfig config = new VerticleConfig();

    // Assert
    Assert.assertNotNull(config);
    Assert.assertEquals(config.getInstances(), 0);
    Assert.assertEquals(config.getThreadPoolSize(), 0);
    Assert.assertEquals(config.getVerticleType(), 0);
  }

  @Test
  public void testAllArgsConstructor_CreatesConfig() {
    // Act
    VerticleConfig config = new VerticleConfig(5, 20, 2);

    // Assert
    Assert.assertNotNull(config);
    Assert.assertEquals(config.getInstances(), 5);
    Assert.assertEquals(config.getThreadPoolSize(), 20);
    Assert.assertEquals(config.getVerticleType(), 2);
  }

  @Test
  public void testSetters_WorkCorrectly() {
    // Arrange
    VerticleConfig config = new VerticleConfig();

    // Act
    config.setInstances(4);
    config.setThreadPoolSize(15);
    config.setVerticleType(1);

    // Assert
    Assert.assertEquals(config.getInstances(), 4);
    Assert.assertEquals(config.getThreadPoolSize(), 15);
    Assert.assertEquals(config.getVerticleType(), 1);
  }

  @Test
  public void testToString_ReturnsString() {
    // Arrange
    VerticleConfig config = VerticleConfig.builder()
        .instances(2)
        .threadPoolSize(5)
        .verticleType(1)
        .build();

    // Act
    String result = config.toString();

    // Assert
    Assert.assertNotNull(result);
    Assert.assertTrue(result.contains("instances=2"));
    Assert.assertTrue(result.contains("threadPoolSize=5"));
    Assert.assertTrue(result.contains("verticleType=1"));
  }
}

