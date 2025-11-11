package com.dream11.logcentralorchestrator.tests.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.dream11.logcentralorchestrator.rest.exception.GenericExceptionMapper;
import com.dream11.logcentralorchestrator.rest.exception.RestException;
import com.dream11.logcentralorchestrator.rest.exception.ValidationExceptionMapper;
import com.dream11.logcentralorchestrator.rest.exception.WebApplicationExceptionMapper;
import com.dream11.logcentralorchestrator.rest.io.Error;
import com.dream11.logcentralorchestrator.rest.io.RestResponse;
import javax.validation.ValidationException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for exception mapper package (GenericExceptionMapper, ValidationExceptionMapper, WebApplicationExceptionMapper). */
public class ExceptionMapperTest {

  private GenericExceptionMapper genericMapper;
  private ValidationExceptionMapper validationMapper;
  private WebApplicationExceptionMapper webApplicationMapper;

  @BeforeMethod
  public void setUp() {
    genericMapper = new GenericExceptionMapper();
    validationMapper = new ValidationExceptionMapper();
    webApplicationMapper = new WebApplicationExceptionMapper();
  }

  // ========== GenericExceptionMapper Tests ==========

  @Test
  public void testGenericExceptionMapper_ToResponse_WithRestException_ReturnsResponseWithStatusCode() {
    // Arrange
    RestException restException =
        new RestException("Test error", Error.of("CODE", "Message"), 400);

    try (MockedStatic<RestResponse> mockedRestResponse =
        org.mockito.Mockito.mockStatic(RestResponse.class)) {
      mockedRestResponse
          .when(() -> RestResponse.parseThrowable(any(Throwable.class)))
          .thenReturn(restException);

      // Act
      Response response = genericMapper.toResponse(restException);

      // Assert
      Assert.assertNotNull(response);
      Assert.assertEquals(response.getStatus(), 400);
      Assert.assertNotNull(response.getEntity());
    }
  }

  @Test
  public void testGenericExceptionMapper_ToResponse_WithNonRestException_ReturnsResponseWith500() {
    // Arrange
    RuntimeException runtimeException = new RuntimeException("Test error");
    RestException restException =
        new RestException(
            runtimeException, Error.of("UNKNOWN-EXCEPTION", "Test error"), 500);

    try (MockedStatic<RestResponse> mockedRestResponse =
        org.mockito.Mockito.mockStatic(RestResponse.class)) {
      mockedRestResponse
          .when(() -> RestResponse.parseThrowable(any(Throwable.class)))
          .thenReturn(restException);

      // Act
      Response response = genericMapper.toResponse(runtimeException);

      // Assert
      Assert.assertNotNull(response);
      Assert.assertEquals(response.getStatus(), 500);
      Assert.assertNotNull(response.getEntity());
    }
  }

  @Test
  public void testGenericExceptionMapper_ToResponse_WithNullThrowable_HandlesGracefully() {
    // Arrange
    RestException restException =
        new RestException((Throwable) null, Error.of("UNKNOWN-EXCEPTION", "null"), 500);

    try (MockedStatic<RestResponse> mockedRestResponse =
        org.mockito.Mockito.mockStatic(RestResponse.class)) {
      mockedRestResponse
          .when(() -> RestResponse.parseThrowable(org.mockito.ArgumentMatchers.isNull()))
          .thenReturn(restException);

      mockedRestResponse
          .when(() -> RestResponse.parseThrowable(any(Throwable.class)))
          .thenAnswer(
              invocation -> {
                Throwable t = invocation.getArgument(0);
                if (t == null) {
                  return restException;
                }
                return RestResponse.parseThrowable(t);
              });

      // Act
      Response response = genericMapper.toResponse((Throwable) null);

      // Assert
      Assert.assertNotNull(response);
      Assert.assertEquals(response.getStatus(), 500);
    }
  }

  // ========== ValidationExceptionMapper Tests ==========

  @Test
  public void testValidationExceptionMapper_ToResponse_WithValidationException_ReturnsBadRequest() {
    // Arrange
    ValidationException validationException = new ValidationException("Constraint violation");

    // Act
    Response response = validationMapper.toResponse(validationException);

    // Assert
    Assert.assertNotNull(response);
    Assert.assertEquals(
        response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
    Assert.assertNotNull(response.getEntity());

    String entity = (String) response.getEntity();
    Assert.assertTrue(entity.contains("INVALID_REQUEST"));
    Assert.assertTrue(entity.contains("Constraint Violation"));
  }

  @Test
  public void testValidationExceptionMapper_ToResponse_WithNullMessage_HandlesGracefully() {
    // Arrange
    ValidationException validationException = new ValidationException((String) null);

    // Act
    Response response = validationMapper.toResponse(validationException);

    // Assert
    Assert.assertNotNull(response);
    Assert.assertEquals(
        response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
    Assert.assertNotNull(response.getEntity());
  }

  @Test
  public void testValidationExceptionMapper_ToResponse_WithEmptyMessage_HandlesGracefully() {
    // Arrange
    ValidationException validationException = new ValidationException("");

    // Act
    Response response = validationMapper.toResponse(validationException);

    // Assert
    Assert.assertNotNull(response);
    Assert.assertEquals(
        response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
    Assert.assertNotNull(response.getEntity());
  }

  // ========== WebApplicationExceptionMapper Tests ==========

  @Test
  public void testWebApplicationExceptionMapper_ToResponse_WithRestExceptionAsCause_ReturnsRestExceptionResponse() {
    // Arrange
    RestException restException =
        new RestException("Test error", Error.of("CODE", "Message"), 400);
    WebApplicationException webException = new WebApplicationException(restException);

    // Act
    Response response = webApplicationMapper.toResponse(webException);

    // Assert
    Assert.assertNotNull(response);
    Assert.assertEquals(response.getStatus(), 400);
    Assert.assertNotNull(response.getEntity());
  }

  @Test
  public void testWebApplicationExceptionMapper_ToResponse_WithNonRestExceptionCause_ReturnsGenericErrorResponse() {
    // Arrange
    RuntimeException runtimeException = new RuntimeException("Test error");
    WebApplicationException webException =
        new WebApplicationException(runtimeException, Response.Status.INTERNAL_SERVER_ERROR);

    // Act
    Response response = webApplicationMapper.toResponse(webException);

    // Assert
    Assert.assertNotNull(response);
    Assert.assertEquals(
        response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    Assert.assertNotNull(response.getEntity());

    String entity = (String) response.getEntity();
    Assert.assertTrue(entity.contains("UNKNOWN_EXCEPTION"));
  }

  @Test
  public void testWebApplicationExceptionMapper_ToResponse_WithNoCause_ReturnsGenericErrorResponse() {
    // Arrange
    WebApplicationException webException =
        new WebApplicationException(Response.Status.BAD_REQUEST);

    // Act
    Response response = webApplicationMapper.toResponse(webException);

    // Assert
    Assert.assertNotNull(response);
    Assert.assertEquals(
        response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
    Assert.assertNotNull(response.getEntity());
  }

  @Test
  public void testWebApplicationExceptionMapper_ToResponse_WithNullCause_HandlesGracefully() {
    // Arrange
    WebApplicationException webException =
        new WebApplicationException((Throwable) null, Response.Status.INTERNAL_SERVER_ERROR);

    // Act
    Response response = webApplicationMapper.toResponse(webException);

    // Assert
    Assert.assertNotNull(response);
    Assert.assertEquals(
        response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    Assert.assertNotNull(response.getEntity());
  }
}

