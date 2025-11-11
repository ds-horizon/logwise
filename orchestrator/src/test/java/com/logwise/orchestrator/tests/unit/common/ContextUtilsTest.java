package com.dream11.logcentralorchestrator.tests.unit.common;

import com.dream11.logcentralorchestrator.common.util.ContextUtils;
import com.dream11.logcentralorchestrator.common.util.SharedDataUtils;
import com.dream11.logcentralorchestrator.setup.BaseTest;
import io.vertx.reactivex.core.Context;
import io.vertx.reactivex.core.Vertx;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for ContextUtils and SharedDataUtils. */
public class ContextUtilsTest extends BaseTest {

  private Vertx vertx;
  private Context context;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    vertx = BaseTest.getReactiveVertx();
    context = vertx.getOrCreateContext();
  }

  // ========== ContextUtils Tests ==========

  @Test
  public void testSetInstance_WithContextAndKey_SetsInstance() {
    // Arrange
    String testValue = "test-value";
    String key = "test-key";

    // Act - Use explicit context parameter to avoid ambiguity
    ContextUtils.setInstance(context, testValue, key);
    String retrieved = ContextUtils.getInstance(context, String.class, key);

    // Assert
    Assert.assertEquals(retrieved, testValue);
  }

  @Test
  public void testSetInstance_WithContext_WithoutKey_UsesDefaultKey() {
    // Arrange
    String testValue = "test-value-default";

    // Act - Explicitly cast to avoid ambiguity with setInstance(T) overload
    ContextUtils.<String>setInstance(context, testValue);
    String retrieved = ContextUtils.getInstance(context, String.class);

    // Assert
    Assert.assertEquals(retrieved, testValue);
  }

  @Test
  public void testGetInstance_WithKey_RetrievesInstance() {
    // Arrange
    Integer testValue = 42;
    String key = "number-key";
    ContextUtils.setInstance(context, testValue, key);

    // Act
    Integer retrieved = ContextUtils.getInstance(context, Integer.class, key);

    // Assert
    Assert.assertEquals(retrieved, testValue);
  }

  @Test
  public void testGetInstances_WithMultipleInstances_ReturnsMap() throws Exception {
    // Arrange
    String value1 = "value1";
    String value2 = "value2";

    // Act - getInstances requires Vertx.currentContext() which must be set
    // Set instances and retrieve them within the same context
    java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
    final Map<String, String>[] resultHolder = new Map[1];
    final Exception[] exceptionHolder = new Exception[1];

    // Use the same context for both setting and getting instances
    context.runOnContext(
        v -> {
          try {
            // Set instances within this context
            ContextUtils.setInstance(context, value1, "key1");
            ContextUtils.setInstance(context, value2, "key2");

            // Get instances from the same context
            Map<String, String> instances = ContextUtils.getInstances(String.class);
            resultHolder[0] = instances;
          } catch (Exception e) {
            exceptionHolder[0] = e;
          } finally {
            latch.countDown();
          }
        });

    // Wait for async execution
    latch.await();

    // Assert
    if (exceptionHolder[0] != null) {
      throw exceptionHolder[0];
    }
    Map<String, String> instances = resultHolder[0];
    Assert.assertNotNull(instances);
    Assert.assertTrue(instances.size() >= 2);
    Assert.assertEquals(instances.get("key1"), value1);
    Assert.assertEquals(instances.get("key2"), value2);
  }

  // ========== SharedDataUtils Tests ==========

  @Test
  public void testGetOrCreate_WithSupplier_CreatesInstance() {
    // Arrange
    String name = "test-instance";
    String expectedValue = "created-value";

    // Act
    String result = SharedDataUtils.getOrCreate(vertx, name, () -> expectedValue);

    // Assert
    Assert.assertEquals(result, expectedValue);
  }

  @Test
  public void testGetOrCreate_WithExistingName_ReturnsExistingInstance() {
    // Arrange
    String name = "shared-instance";
    String firstValue = "first";
    String secondValue = "second";

    // Act
    String first = SharedDataUtils.getOrCreate(vertx, name, () -> firstValue);
    String second = SharedDataUtils.getOrCreate(vertx, name, () -> secondValue);

    // Assert
    Assert.assertEquals(first, firstValue);
    Assert.assertEquals(second, firstValue); // Should return existing, not create new
  }

  @Test
  public void testSetInstance_WithVertx_SetsInstance() {
    // Arrange
    String instance = "test-instance";

    // Act
    String result = SharedDataUtils.setInstance(vertx, instance);
    String retrieved = SharedDataUtils.getInstance(vertx, String.class);

    // Assert
    Assert.assertEquals(result, instance);
    Assert.assertEquals(retrieved, instance);
  }

  @Test
  public void testGetInstance_WithExistingInstance_ReturnsInstance() {
    // Arrange
    Integer instance = 100;
    SharedDataUtils.setInstance(vertx, instance);

    // Act
    Integer retrieved = SharedDataUtils.getInstance(vertx, Integer.class);

    // Assert
    Assert.assertEquals(retrieved, instance);
  }

  @Test
  public void testGetInstance_WithoutExistingInstance_ThrowsException() {
    // Act & Assert
    try {
      SharedDataUtils.getInstance(vertx, Double.class);
      Assert.fail("Should have thrown RuntimeException");
    } catch (RuntimeException e) {
      Assert.assertTrue(e.getMessage().contains("Cannot find default instance"));
    }
  }
}
