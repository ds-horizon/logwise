package com.dream11.logcentralorchestrator.tests.unit.rest;

import com.dream11.logcentralorchestrator.rest.healthcheck.HealthCheckException;
import com.dream11.logcentralorchestrator.rest.healthcheck.HealthCheckResponse;
import com.dream11.logcentralorchestrator.rest.healthcheck.HealthCheckUtil;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests for HealthCheckUtil, HealthCheckResponse, and HealthCheckException. */
public class HealthCheckTest {

  // ========== HealthCheckUtil Tests ==========

  @Test
  public void testHandler_WithAllUp_ReturnsUpStatus() {
    // Arrange
    Map<String, Single<JsonObject>> healthChecks = new HashMap<>();
    healthChecks.put("db", Single.just(new JsonObject().put("status", "ok")));
    healthChecks.put("cache", Single.just(new JsonObject().put("status", "ok")));

    // Act
    HealthCheckResponse response = HealthCheckUtil.handler(healthChecks).blockingGet();

    // Assert
    Assert.assertNotNull(response);
    Assert.assertNotNull(response.getStatus());
    Assert.assertEquals(response.getChecks().size(), 2);
    // Verify status is UP by checking the status name through toJson
    JsonObject json = response.toJson();
    Assert.assertEquals(json.getJsonObject("data").getString("status"), "UP");
  }

  @Test
  public void testHandler_WithOneDown_ThrowsHealthCheckException() {
    // Arrange
    Map<String, Single<JsonObject>> healthChecks = new HashMap<>();
    healthChecks.put("db", Single.just(new JsonObject().put("status", "ok")));
    healthChecks.put("cache", Single.error(new RuntimeException("Cache error")));

    // Act & Assert - HealthCheckUtil throws HealthCheckException when status is DOWN
    try {
      HealthCheckUtil.handler(healthChecks).blockingGet();
      Assert.fail("Should have thrown HealthCheckException");
    } catch (HealthCheckException e) {
      Assert.assertEquals(e.getHttpStatusCode(), 503);
      Assert.assertEquals(e.getError().getCode(), "HEALTHCHECK_FAILED");
      // Verify the response contains DOWN status
      String message = e.getMessage();
      Assert.assertTrue(message.contains("DOWN"));
    }
  }

  @Test
  public void testHandler_WithAllDown_ThrowsHealthCheckException() {
    // Arrange
    Map<String, Single<JsonObject>> healthChecks = new HashMap<>();
    healthChecks.put("db", Single.error(new RuntimeException("DB error")));
    healthChecks.put("cache", Single.error(new RuntimeException("Cache error")));

    // Act & Assert
    try {
      HealthCheckUtil.handler(healthChecks).blockingGet();
      Assert.fail("Should have thrown HealthCheckException");
    } catch (HealthCheckException e) {
      Assert.assertEquals(e.getHttpStatusCode(), 503);
      Assert.assertEquals(e.getError().getCode(), "HEALTHCHECK_FAILED");
    }
  }

  // ========== HealthCheckResponse Tests ==========

  @Test
  public void testHealthCheckResponse_WithUpChecks_ReturnsUpStatus() {
    // Arrange - Use HealthCheckUtil to create response with checks
    Map<String, Single<JsonObject>> healthChecks = new HashMap<>();
    healthChecks.put("db", Single.just(new JsonObject().put("status", "ok")));
    healthChecks.put("cache", Single.just(new JsonObject().put("status", "ok")));

    // Act
    HealthCheckResponse response = HealthCheckUtil.handler(healthChecks).blockingGet();

    // Assert
    Assert.assertNotNull(response.getStatus());
    Assert.assertEquals(response.getChecks().size(), 2);
    JsonObject json = response.toJson();
    Assert.assertEquals(json.getJsonObject("data").getString("status"), "UP");
  }

  @Test
  public void testHealthCheckResponse_WithDownCheck_ThrowsHealthCheckException() {
    // Arrange - Use HealthCheckUtil to create response with checks
    Map<String, Single<JsonObject>> healthChecks = new HashMap<>();
    healthChecks.put("db", Single.just(new JsonObject().put("status", "ok")));
    healthChecks.put("cache", Single.error(new RuntimeException("Error")));

    // Act & Assert - HealthCheckUtil throws HealthCheckException when status is DOWN
    try {
      HealthCheckUtil.handler(healthChecks).blockingGet();
      Assert.fail("Should have thrown HealthCheckException");
    } catch (HealthCheckException e) {
      Assert.assertEquals(e.getHttpStatusCode(), 503);
      Assert.assertEquals(e.getError().getCode(), "HEALTHCHECK_FAILED");
      // Verify the response contains DOWN status
      String message = e.getMessage();
      Assert.assertTrue(message.contains("DOWN"));
    }
  }

  @Test
  public void testHealthCheckResponse_ToJson_ReturnsJsonObject() {
    // Arrange
    Map<String, Single<JsonObject>> healthChecks = new HashMap<>();
    healthChecks.put("db", Single.just(new JsonObject().put("status", "ok")));

    // Act
    HealthCheckResponse response = HealthCheckUtil.handler(healthChecks).blockingGet();
    JsonObject json = response.toJson();

    // Assert
    Assert.assertNotNull(json);
    Assert.assertTrue(json.containsKey("data"));
  }

  // ========== HealthCheckException Tests ==========

  @Test
  public void testHealthCheckException_Constructor_SetsErrorAndStatusCode() {
    // Act
    HealthCheckException exception = new HealthCheckException("Health check failed");

    // Assert
    Assert.assertEquals(exception.getHttpStatusCode(), 503);
    Assert.assertEquals(exception.getError().getCode(), "HEALTHCHECK_FAILED");
    Assert.assertEquals(exception.getError().getMessage(), "healthcheck failed");
    Assert.assertEquals(exception.getMessage(), "Health check failed");
  }

  @Test
  public void testHealthCheckException_ToString_ReturnsMessage() {
    // Arrange
    HealthCheckException exception = new HealthCheckException("Test message");

    // Act
    String result = exception.toString();

    // Assert
    Assert.assertEquals(result, "Test message");
  }
}
