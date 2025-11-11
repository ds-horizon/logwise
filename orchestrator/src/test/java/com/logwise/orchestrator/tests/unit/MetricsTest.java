package com.dream11.logcentralorchestrator.tests.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.dream11.logcentralorchestrator.constants.TestConstants;
import com.dream11.logcentralorchestrator.dto.response.LogSyncDelayResponse;
import com.dream11.logcentralorchestrator.enums.Tenant;
import com.dream11.logcentralorchestrator.rest.io.Response;
import com.dream11.logcentralorchestrator.rest.v1.Metrics;
import com.dream11.logcentralorchestrator.service.MetricsService;
import com.dream11.logcentralorchestrator.setup.BaseTest;
import com.dream11.logcentralorchestrator.util.ResponseWrapper;
import com.dream11.logcentralorchestrator.util.TestResponseWrapper;
import io.reactivex.Single;
import java.util.concurrent.CompletionStage;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/** Unit tests for Metrics API - getObjectStoreSyncDelay endpoint. */
@Listeners(com.dream11.logcentralorchestrator.listeners.ExtentReportListener.class)
public class MetricsTest extends BaseTest {

  // Test Constants
  private static final int MAX_DELAY_MINUTES = 1440; // 24 hours * 60 minutes

  @Mock private MetricsService metricsService;

  private Metrics metrics;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    MockitoAnnotations.openMocks(this);
    // Since Metrics uses @RequiredArgsConstructor with final fields,
    // we need to manually inject using reflection
    metrics = new Metrics(metricsService);
  }

  @AfterClass
  public static void tearDownClass() {
    BaseTest.cleanup();
  }

  @Test
  public void testGetObjectStoreSyncDelay_WithValidTenant_ReturnsLogSyncDelayResponse()
      throws Exception {
    // Arrange
    LogSyncDelayResponse expectedResponse =
        LogSyncDelayResponse.builder()
            .tenant(TestConstants.VALID_TENANT_NAME)
            .appLogsDelayMinutes(TestConstants.DELAY_MINUTES)
            .build();

    when(metricsService.computeLogSyncDelay(any(Tenant.class)))
        .thenReturn(Single.just(expectedResponse));

    // Mock ResponseWrapper to use TestResponseWrapper instead
    try (MockedStatic<ResponseWrapper> mockedResponseWrapper =
        Mockito.mockStatic(ResponseWrapper.class)) {
      mockedResponseWrapper
          .when(() -> ResponseWrapper.fromSingle(any(Single.class), eq(200)))
          .thenAnswer(
              invocation -> {
                Single<LogSyncDelayResponse> single = invocation.getArgument(0);
                int statusCode = invocation.getArgument(1);
                return TestResponseWrapper.fromSingle(single, statusCode);
              });

      // Act
      CompletionStage<Response<LogSyncDelayResponse>> result =
          metrics.getObjectStoreSyncDelay(TestConstants.VALID_TENANT_NAME);

      // Assert
      Response<LogSyncDelayResponse> response = result.toCompletableFuture().get();
      Assert.assertNotNull(response, "Response should not be null");
      Assert.assertEquals(response.getHttpStatusCode(), 200, "HTTP status code should be 200");
      Assert.assertNull(response.getError(), "Error should be null for successful response");

      LogSyncDelayResponse responseData = response.getData();
      Assert.assertNotNull(responseData, "Response data should not be null");

      // Verify all fields in LogSyncDelayResponse
      // Verify tenant field
      Assert.assertNotNull(responseData.getTenant(), "Tenant should not be null");
      Assert.assertEquals(
          responseData.getTenant(),
          TestConstants.VALID_TENANT_NAME,
          "Tenant should match expected value");
      Assert.assertFalse(responseData.getTenant().isEmpty(), "Tenant should not be empty");

      // Verify appLogsDelayMinutes field
      Assert.assertNotNull(
          responseData.getAppLogsDelayMinutes(), "App logs delay minutes should not be null");
      Assert.assertEquals(
          responseData.getAppLogsDelayMinutes(),
          TestConstants.DELAY_MINUTES,
          "App logs delay minutes should match expected value");
      Assert.assertTrue(
          responseData.getAppLogsDelayMinutes() >= 0,
          "App logs delay minutes should be non-negative");

      // Verify service was called with correct tenant
      verify(metricsService, times(1)).computeLogSyncDelay(eq(Tenant.D11_Prod_AWS));

      // Verify ResponseWrapper was called with correct status code
      mockedResponseWrapper.verify(
          () -> ResponseWrapper.fromSingle(any(Single.class), eq(200)), times(1));
    }
  }

  @Test(expectedExceptions = Exception.class)
  public void testGetObjectStoreSyncDelay_WhenMetricsComputationFails_PropagatesException()
      throws Exception {
    // Arrange
    RuntimeException error = new RuntimeException("Metrics computation failed");

    when(metricsService.computeLogSyncDelay(any(Tenant.class))).thenReturn(Single.error(error));

    // Mock ResponseWrapper to use TestResponseWrapper instead
    try (MockedStatic<ResponseWrapper> mockedResponseWrapper =
        Mockito.mockStatic(ResponseWrapper.class)) {
      mockedResponseWrapper
          .when(() -> ResponseWrapper.fromSingle(any(Single.class), anyInt()))
          .thenAnswer(
              invocation -> {
                Single<LogSyncDelayResponse> single = invocation.getArgument(0);
                int statusCode = invocation.getArgument(1);
                return TestResponseWrapper.fromSingle(single, statusCode);
              });

      // Act - should throw Exception
      CompletionStage<Response<LogSyncDelayResponse>> result =
          metrics.getObjectStoreSyncDelay(TestConstants.VALID_TENANT_NAME);

      // Assert
      result.toCompletableFuture().get();

      // Verify service was called with correct tenant
      verify(metricsService, times(1)).computeLogSyncDelay(eq(Tenant.D11_Prod_AWS));
    }
  }

  @Test
  public void testGetObjectStoreSyncDelay_WithNoLogsFound_ReturnsMaxDelay() throws Exception {
    // Arrange
    LogSyncDelayResponse expectedResponse =
        LogSyncDelayResponse.builder()
            .tenant(TestConstants.VALID_TENANT_NAME)
            .appLogsDelayMinutes(MAX_DELAY_MINUTES)
            .build();

    when(metricsService.computeLogSyncDelay(any(Tenant.class)))
        .thenReturn(Single.just(expectedResponse));

    // Mock ResponseWrapper to use TestResponseWrapper instead
    try (MockedStatic<ResponseWrapper> mockedResponseWrapper =
        Mockito.mockStatic(ResponseWrapper.class)) {
      mockedResponseWrapper
          .when(() -> ResponseWrapper.fromSingle(any(Single.class), anyInt()))
          .thenAnswer(
              invocation -> {
                Single<LogSyncDelayResponse> single = invocation.getArgument(0);
                int statusCode = invocation.getArgument(1);
                return TestResponseWrapper.fromSingle(single, statusCode);
              });

      // Act
      CompletionStage<Response<LogSyncDelayResponse>> result =
          metrics.getObjectStoreSyncDelay(TestConstants.VALID_TENANT_NAME);

      // Assert
      Response<LogSyncDelayResponse> response = result.toCompletableFuture().get();
      Assert.assertNotNull(response, "Response should not be null");
      Assert.assertEquals(response.getHttpStatusCode(), 200, "HTTP status code should be 200");
      Assert.assertNull(response.getError(), "Error should be null for successful response");

      LogSyncDelayResponse responseData = response.getData();
      Assert.assertNotNull(responseData, "Response data should not be null");

      // Verify all fields in LogSyncDelayResponse
      // Verify tenant field
      Assert.assertNotNull(responseData.getTenant(), "Tenant should not be null");
      Assert.assertEquals(
          responseData.getTenant(),
          TestConstants.VALID_TENANT_NAME,
          "Tenant should match expected value");
      Assert.assertFalse(responseData.getTenant().isEmpty(), "Tenant should not be empty");

      // Verify appLogsDelayMinutes is set to max delay when no logs found
      Assert.assertNotNull(
          responseData.getAppLogsDelayMinutes(), "App logs delay minutes should not be null");
      Assert.assertEquals(
          responseData.getAppLogsDelayMinutes(),
          MAX_DELAY_MINUTES,
          "App logs delay minutes should be max delay (1440 minutes) when no logs found");
      Assert.assertTrue(
          responseData.getAppLogsDelayMinutes() > 0, "App logs delay minutes should be positive");

      // Verify service was called with correct tenant
      verify(metricsService, times(1)).computeLogSyncDelay(eq(Tenant.D11_Prod_AWS));
    }
  }
}
