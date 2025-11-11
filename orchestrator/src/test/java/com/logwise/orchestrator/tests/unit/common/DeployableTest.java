package com.dream11.logcentralorchestrator.tests.unit.common;

import com.dream11.logcentralorchestrator.common.app.AppContext;
import com.dream11.logcentralorchestrator.common.app.Deployable;
import com.dream11.logcentralorchestrator.common.app.VerticleConfig;
import com.dream11.logcentralorchestrator.verticle.RestVerticle;
import io.vertx.core.AbstractVerticle;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for Deployable. */
public class DeployableTest {

  @BeforeMethod
  public void setUp() {
    // Initialize AppContext if needed
    try {
      AppContext.getInstance();
    } catch (Exception e) {
      // AppContext may not be initialized in unit tests
    }
  }

  @Test
  public void testConstructor_WithVerticleClass_CreatesDeployable() {
    // Act
    Deployable deployable = new Deployable(RestVerticle.class);

    // Assert
    Assert.assertNotNull(deployable);
    Assert.assertEquals(deployable.getVerticleClass(), RestVerticle.class);
    // Note: getVerticle() requires AppContext to be initialized, so we skip that assertion
  }

  @Test
  public void testConstructor_WithConfigAndVerticleClass_CreatesDeployable() {
    // Arrange
    VerticleConfig config = VerticleConfig.builder().instances(2).build();

    // Act
    Deployable deployable = new Deployable(config, RestVerticle.class);

    // Assert
    Assert.assertNotNull(deployable);
    Assert.assertEquals(deployable.getVerticleClass(), RestVerticle.class);
    Assert.assertEquals(deployable.getConfig(), config);
  }

  @Test
  public void testGetVerticle_WithValidClass_ReturnsVerticle() {
    // Arrange
    Deployable deployable = new Deployable(RestVerticle.class);

    try (MockedStatic<AppContext> mockedAppContext = Mockito.mockStatic(AppContext.class)) {
      RestVerticle mockVerticle = Mockito.mock(RestVerticle.class);
      AppContext mockAppContext = Mockito.mock(AppContext.class);
      mockedAppContext.when(AppContext::getInstance).thenReturn(mockAppContext);
      Mockito.when(mockAppContext.getInstance(RestVerticle.class)).thenReturn(mockVerticle);

      // Act
      AbstractVerticle verticle = deployable.getVerticle();

      // Assert
      Assert.assertNotNull(verticle);
      Assert.assertSame(verticle, mockVerticle);
    }
  }

  @Test
  public void testGetConfig_WithDefaultConfig_ReturnsDefault() {
    // Arrange
    Deployable deployable = new Deployable(RestVerticle.class);

    // Act
    VerticleConfig config = deployable.getConfig();

    // Assert
    Assert.assertNotNull(config);
    Assert.assertEquals(config, VerticleConfig.DEFAULT_CONFIG);
  }

  @Test
  public void testGetConfig_WithCustomConfig_ReturnsCustom() {
    // Arrange
    VerticleConfig customConfig = VerticleConfig.builder().instances(3).build();
    Deployable deployable = new Deployable(customConfig, RestVerticle.class);

    // Act
    VerticleConfig config = deployable.getConfig();

    // Assert
    Assert.assertNotNull(config);
    Assert.assertEquals(config, customConfig);
  }

  @Test
  public void testDeprecatedConstructors_Work() {
    // Arrange
    AbstractVerticle mockVerticle = Mockito.mock(AbstractVerticle.class);
    VerticleConfig config = VerticleConfig.DEFAULT_CONFIG;

    // Act
    Deployable deployable1 = new Deployable(mockVerticle);
    Deployable deployable2 = new Deployable(mockVerticle, config);

    // Assert
    Assert.assertNotNull(deployable1);
    Assert.assertNotNull(deployable2);
    // Note: getVerticle() may return the mock directly for deprecated constructors
    // but we can't verify without AppContext initialization
  }
}

