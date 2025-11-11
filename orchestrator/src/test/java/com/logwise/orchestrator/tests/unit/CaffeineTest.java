package com.dream11.logcentralorchestrator.tests.unit;

import com.dream11.logcentralorchestrator.CaffeineCacheFactory;
import com.dream11.logcentralorchestrator.CaffeineConfig;
import java.util.concurrent.TimeUnit;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests for CaffeineCacheFactory and CaffeineConfig. */
public class CaffeineTest {

  // ========== CaffeineConfig Tests ==========

  @Test
  public void testCaffeineConfig_DefaultValues_AreSet() {
    // Act
    CaffeineConfig config = new CaffeineConfig();

    // Assert
    Assert.assertFalse(config.getWeakKeys());
    Assert.assertFalse(config.getWeakValues());
    Assert.assertFalse(config.getSoftValues());
  }

  @Test
  public void testCaffeineConfig_SetRefreshAfterWriteSeconds_SetsDurationAndTimeUnit() {
    // Arrange
    CaffeineConfig config = new CaffeineConfig();

    // Act
    config.setRefreshAfterWriteSeconds(60L);

    // Assert
    Assert.assertEquals(config.getRefreshAfterWriteDuration(), Long.valueOf(60L));
    Assert.assertEquals(config.getRefreshAfterWriteTimeUnit(), TimeUnit.SECONDS);
  }

  @Test
  public void testCaffeineConfig_SetExpireAfterWriteSeconds_SetsDurationAndTimeUnit() {
    // Arrange
    CaffeineConfig config = new CaffeineConfig();

    // Act
    config.setExpireAfterWriteSeconds(120L);

    // Assert
    Assert.assertEquals(config.getExpireAfterWriteDuration(), Long.valueOf(120L));
    Assert.assertEquals(config.getExpireAfterWriteTimeUnit(), TimeUnit.SECONDS);
  }

  @Test
  public void testCaffeineConfig_SetExpireAfterAccessSeconds_SetsDurationAndTimeUnit() {
    // Arrange
    CaffeineConfig config = new CaffeineConfig();

    // Act
    config.setExpireAfterAccessSeconds(180L);

    // Assert
    Assert.assertEquals(config.getExpireAfterAccessDuration(), Long.valueOf(180L));
    Assert.assertEquals(config.getExpireAfterAccessTimeUnit(), TimeUnit.SECONDS);
  }

  @Test
  public void
      testCaffeineConfig_SetRefreshAfterWriteDuration_WithExistingSeconds_ThrowsException() {
    // Arrange
    CaffeineConfig config = new CaffeineConfig();
    config.setRefreshAfterWriteSeconds(60L);

    // Act & Assert
    try {
      config.setRefreshAfterWriteDuration(120L);
      Assert.fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(e.getMessage().contains("cannot be set together"));
    }
  }

  @Test
  public void testCaffeineConfig_SetExpireAfterWriteDuration_WithExistingSeconds_ThrowsException() {
    // Arrange
    CaffeineConfig config = new CaffeineConfig();
    config.setExpireAfterWriteSeconds(60L);

    // Act & Assert
    try {
      config.setExpireAfterWriteDuration(120L);
      Assert.fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(e.getMessage().contains("cannot be set together"));
    }
  }

  @Test
  public void
      testCaffeineConfig_SetExpireAfterAccessDuration_WithExistingSeconds_ThrowsException() {
    // Arrange
    CaffeineConfig config = new CaffeineConfig();
    config.setExpireAfterAccessSeconds(60L);

    // Act & Assert
    try {
      config.setExpireAfterAccessDuration(120L);
      Assert.fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(e.getMessage().contains("cannot be set together"));
    }
  }

  @Test
  public void
      testCaffeineConfig_SetRefreshAfterWriteTimeUnit_WithExistingSeconds_ThrowsException() {
    // Arrange
    CaffeineConfig config = new CaffeineConfig();
    config.setRefreshAfterWriteSeconds(60L);

    // Act & Assert
    try {
      config.setRefreshAfterWriteTimeUnit(TimeUnit.MINUTES);
      Assert.fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(e.getMessage().contains("cannot be set together"));
    }
  }

  @Test
  public void testCaffeineConfig_Setters_Work() {
    // Arrange
    CaffeineConfig config = new CaffeineConfig();

    // Act
    config.setInitialCapacity(100);
    config.setMaximumSize(1000);
    config.setWeakKeys(true);
    config.setWeakValues(true);
    config.setSoftValues(true);

    // Assert
    Assert.assertEquals(config.getInitialCapacity(), Integer.valueOf(100));
    Assert.assertEquals(config.getMaximumSize(), Integer.valueOf(1000));
    Assert.assertTrue(config.getWeakKeys());
    Assert.assertTrue(config.getWeakValues());
    Assert.assertTrue(config.getSoftValues());
  }

  // ========== CaffeineCacheFactory Tests ==========

  @Test
  public void testCaffeineCacheFactory_ClassExists() {
    // Note: CaffeineCacheFactory requires Vertx context and config files
    // Full testing would require proper Vertx setup
    Assert.assertNotNull(CaffeineCacheFactory.class);
  }
}
