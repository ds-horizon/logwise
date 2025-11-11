package com.dream11.logcentralorchestrator.tests.unit.rest;

import com.dream11.logcentralorchestrator.rest.converter.DoubleParamConverter;
import com.dream11.logcentralorchestrator.rest.converter.FloatParamConverter;
import com.dream11.logcentralorchestrator.rest.converter.IntegerParamConverter;
import com.dream11.logcentralorchestrator.rest.converter.LongParamConverter;
import com.dream11.logcentralorchestrator.rest.provider.D11ParamConverterProvider;
import javax.ws.rs.ext.ParamConverter;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests for D11ParamConverterProvider. */
public class D11ParamConverterProviderTest {

  private D11ParamConverterProvider provider;

  @Test
  public void setUp() {
    provider = new D11ParamConverterProvider();
  }

  @Test
  public void testGetConverter_WithLong_ReturnsLongParamConverter() {
    // Arrange
    setUp();

    // Act
    ParamConverter<Long> converter =
        provider.getConverter(Long.class, null, new java.lang.annotation.Annotation[0]);

    // Assert
    Assert.assertNotNull(converter);
    Assert.assertTrue(converter instanceof LongParamConverter);
  }

  @Test
  public void testGetConverter_WithInteger_ReturnsIntegerParamConverter() {
    // Arrange
    setUp();

    // Act
    ParamConverter<Integer> converter =
        provider.getConverter(Integer.class, null, new java.lang.annotation.Annotation[0]);

    // Assert
    Assert.assertNotNull(converter);
    Assert.assertTrue(converter instanceof IntegerParamConverter);
  }

  @Test
  public void testGetConverter_WithDouble_ReturnsDoubleParamConverter() {
    // Arrange
    setUp();

    // Act
    ParamConverter<Double> converter =
        provider.getConverter(Double.class, null, new java.lang.annotation.Annotation[0]);

    // Assert
    Assert.assertNotNull(converter);
    Assert.assertTrue(converter instanceof DoubleParamConverter);
  }

  @Test
  public void testGetConverter_WithFloat_ReturnsFloatParamConverter() {
    // Arrange
    setUp();

    // Act
    ParamConverter<Float> converter =
        provider.getConverter(Float.class, null, new java.lang.annotation.Annotation[0]);

    // Assert
    Assert.assertNotNull(converter);
    Assert.assertTrue(converter instanceof FloatParamConverter);
  }

  @Test
  public void testGetConverter_WithUnsupportedType_ReturnsNull() {
    // Arrange
    setUp();

    // Act
    ParamConverter<String> converter =
        provider.getConverter(String.class, null, new java.lang.annotation.Annotation[0]);

    // Assert
    Assert.assertNull(converter);
  }

  @Test
  public void testGetConverter_WithAnnotations_PassesAnnotations() {
    // Arrange
    setUp();
    java.lang.annotation.Annotation[] annotations = new java.lang.annotation.Annotation[0];

    // Act
    ParamConverter<Long> converter = provider.getConverter(Long.class, null, annotations);

    // Assert
    Assert.assertNotNull(converter);
    Assert.assertTrue(converter instanceof LongParamConverter);
  }
}
