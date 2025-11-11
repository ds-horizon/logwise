package com.dream11.logcentralorchestrator.tests.unit.rest;

import com.dream11.logcentralorchestrator.rest.handler.HttpLoggerHandlerImpl;
import io.vertx.ext.web.handler.LoggerFormat;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests for HttpLoggerHandlerImpl. */
public class HttpLoggerHandlerImplTest {

  @Test
  public void testConstructor_WithLoggerName_CreatesInstance() {
    // Act
    HttpLoggerHandlerImpl handler = new HttpLoggerHandlerImpl("TEST_LOGGER");

    // Assert
    Assert.assertNotNull(handler);
  }

  @Test
  public void testConstructor_WithFormatAndLoggerName_CreatesInstance() {
    // Act
    HttpLoggerHandlerImpl handler = new HttpLoggerHandlerImpl(LoggerFormat.DEFAULT, "TEST_LOGGER");

    // Assert
    Assert.assertNotNull(handler);
  }

  @Test
  public void testConstructor_WithImmediateFormatAndLoggerName_CreatesInstance() {
    // Act
    HttpLoggerHandlerImpl handler = new HttpLoggerHandlerImpl(false, LoggerFormat.DEFAULT, "TEST_LOGGER");

    // Assert
    Assert.assertNotNull(handler);
  }

  @Test
  public void testConstructor_WithImmediateTrue_CreatesInstance() {
    // Act
    HttpLoggerHandlerImpl handler = new HttpLoggerHandlerImpl(true, LoggerFormat.SHORT, "SHORT_LOGGER");

    // Assert
    Assert.assertNotNull(handler);
  }

  @Test
  public void testConstructor_WithDifferentFormats_CreatesInstances() {
    // Act & Assert
    HttpLoggerHandlerImpl handler1 = new HttpLoggerHandlerImpl(LoggerFormat.DEFAULT, "DEFAULT_LOGGER");
    HttpLoggerHandlerImpl handler2 = new HttpLoggerHandlerImpl(LoggerFormat.SHORT, "SHORT_LOGGER");
    HttpLoggerHandlerImpl handler3 = new HttpLoggerHandlerImpl(LoggerFormat.TINY, "TINY_LOGGER");

    Assert.assertNotNull(handler1);
    Assert.assertNotNull(handler2);
    Assert.assertNotNull(handler3);
  }
}

