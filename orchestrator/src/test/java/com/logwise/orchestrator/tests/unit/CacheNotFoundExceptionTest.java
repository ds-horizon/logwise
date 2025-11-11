package com.dream11.logcentralorchestrator.tests.unit;

import com.dream11.logcentralorchestrator.CacheNotFoundException;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests for CacheNotFoundException. */
public class CacheNotFoundExceptionTest {

  @Test
  public void testConstructor_WithMessage_CreatesException() {
    // Arrange
    String message = "Cache not found for key: test-key";

    // Act
    CacheNotFoundException exception = new CacheNotFoundException(message);

    // Assert
    Assert.assertNotNull(exception);
    Assert.assertEquals(exception.getMessage(), message);
    Assert.assertNull(exception.getCause());
  }

  @Test
  public void testConstructor_WithEmptyMessage_CreatesException() {
    // Arrange
    String message = "";

    // Act
    CacheNotFoundException exception = new CacheNotFoundException(message);

    // Assert
    Assert.assertNotNull(exception);
    Assert.assertEquals(exception.getMessage(), message);
  }

  @Test
  public void testConstructor_WithNullMessage_CreatesException() {
    // Arrange
    String message = null;

    // Act
    CacheNotFoundException exception = new CacheNotFoundException(message);

    // Assert
    Assert.assertNotNull(exception);
    Assert.assertNull(exception.getMessage());
  }
}

