package com.dream11.logcentralorchestrator.tests.unit.rest;

import com.dream11.logcentralorchestrator.common.app.AppContext;
import com.dream11.logcentralorchestrator.rest.RestUtil;
import com.dream11.logcentralorchestrator.rest.filter.LoggerFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.UriInfo;
import static org.mockito.ArgumentMatchers.eq;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for LoggerFilter. */
public class LoggerFilterTest {

  private LoggerFilter loggerFilter;
  private ContainerRequestContext mockRequestContext;
  private ContainerResponseContext mockResponseContext;
  private UriInfo mockUriInfo;
  private ObjectMapper mockObjectMapper;

  @BeforeMethod
  public void setUp() {
    loggerFilter = new LoggerFilter();
    mockRequestContext = Mockito.mock(ContainerRequestContext.class);
    mockResponseContext = Mockito.mock(ContainerResponseContext.class);
    mockUriInfo = Mockito.mock(UriInfo.class);
    mockObjectMapper = new ObjectMapper();

    Mockito.when(mockRequestContext.getUriInfo()).thenReturn(mockUriInfo);
    Mockito.when(mockUriInfo.getPath()).thenReturn("/api/v1/test");
    Mockito.when(mockRequestContext.getMethod()).thenReturn("GET");
    Mockito.when(mockRequestContext.getHeaders()).thenReturn(new MultivaluedHashMap<>());
    Mockito.when(mockUriInfo.getPathParameters()).thenReturn(new MultivaluedHashMap<>());
    Mockito.when(mockUriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
  }

  @Test
  public void testFilter_Request_WithEntity_SetsStartTime() throws IOException {
    // Arrange
    String body = "{\"test\":\"value\"}";
    ByteArrayInputStream inputStream = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
    Mockito.when(mockRequestContext.hasEntity()).thenReturn(true);
    Mockito.when(mockRequestContext.getEntityStream()).thenReturn(inputStream);

    // Act
    loggerFilter.filter(mockRequestContext);

    // Assert
    Mockito.verify(mockRequestContext).setProperty(eq("REQUEST_START_TIME"), Mockito.anyLong());
    Mockito.verify(mockRequestContext, Mockito.atLeastOnce()).getMethod();
    Mockito.verify(mockUriInfo, Mockito.atLeastOnce()).getPath();
  }

  @Test
  public void testFilter_Request_WithoutEntity_SetsStartTime() throws IOException {
    // Arrange
    Mockito.when(mockRequestContext.hasEntity()).thenReturn(false);

    // Act
    loggerFilter.filter(mockRequestContext);

    // Assert
    Mockito.verify(mockRequestContext).setProperty(eq("REQUEST_START_TIME"), Mockito.anyLong());
  }

  @Test
  public void testFilter_Request_WithNullEntityStream_HandlesGracefully() throws IOException {
    // Arrange
    Mockito.when(mockRequestContext.hasEntity()).thenReturn(true);
    Mockito.when(mockRequestContext.getEntityStream()).thenReturn(null);

    // Act
    loggerFilter.filter(mockRequestContext);

    // Assert
    Mockito.verify(mockRequestContext).setProperty(eq("REQUEST_START_TIME"), Mockito.anyLong());
  }

  @Test
  public void testFilter_Response_WithEntity_ConvertsToString() throws Exception {
    // Arrange
    Object entity = new TestEntity("test-value");
    Mockito.when(mockResponseContext.hasEntity()).thenReturn(true);
    Mockito.when(mockResponseContext.getEntity()).thenReturn(entity);
    Mockito.when(mockRequestContext.getProperty("REQUEST_START_TIME")).thenReturn(System.currentTimeMillis() - 100);

    try (MockedStatic<RestUtil> mockedRestUtil = Mockito.mockStatic(RestUtil.class)) {
      mockedRestUtil.when(() -> RestUtil.getString(entity)).thenReturn("{\"value\":\"test-value\"}");

      // Act
      loggerFilter.filter(mockRequestContext, mockResponseContext);

      // Assert
      Mockito.verify(mockResponseContext).setEntity(Mockito.anyString());
      Mockito.verify(mockRequestContext).removeProperty("REQUEST_START_TIME");
    }
  }

  @Test
  public void testFilter_Response_WithoutEntity_LogsResponseTime() throws Exception {
    // Arrange
    Mockito.when(mockResponseContext.hasEntity()).thenReturn(false);
    Mockito.when(mockResponseContext.getStatus()).thenReturn(200);
    Mockito.when(mockRequestContext.getProperty("REQUEST_START_TIME")).thenReturn(System.currentTimeMillis() - 100);

    // Act
    loggerFilter.filter(mockRequestContext, mockResponseContext);

    // Assert
    Mockito.verify(mockRequestContext).removeProperty("REQUEST_START_TIME");
  }

  @Test
  public void testFilter_Response_WithoutStartTime_DoesNotLogResponseTime() throws Exception {
    // Arrange
    Mockito.when(mockResponseContext.hasEntity()).thenReturn(false);
    Mockito.when(mockRequestContext.getProperty("REQUEST_START_TIME")).thenReturn(null);

    // Act
    loggerFilter.filter(mockRequestContext, mockResponseContext);

    // Assert
    Mockito.verify(mockRequestContext, Mockito.never()).removeProperty("REQUEST_START_TIME");
  }

  private static class TestEntity {
    private String value;

    public TestEntity(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }
}

