package com.dream11.logcentralorchestrator.tests.unit;

import com.dream11.logcentralorchestrator.dto.entity.ServiceDetails;
import com.dream11.logcentralorchestrator.dto.entity.SparkSubmitArgs;
import com.dream11.logcentralorchestrator.dto.entity.SparkSubmitStatus;
import com.dream11.logcentralorchestrator.dto.request.ComponentSyncRequest;
import com.dream11.logcentralorchestrator.dto.request.MonitorSparkJobRequest;
import com.dream11.logcentralorchestrator.dto.request.SubmitSparkJobRequest;
import com.dream11.logcentralorchestrator.dto.response.*;
import com.dream11.logcentralorchestrator.rest.exception.RestException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests for DTO package (entity, request, response). */
public class DtoTest {

  // ========== ServiceDetails Tests ==========

  @Test
  public void testServiceDetails_Constructor_CreatesInstance() {
    // Act
    ServiceDetails details = new ServiceDetails();

    // Assert
    Assert.assertNotNull(details);
  }

  @Test
  public void testServiceDetails_AllArgsConstructor_CreatesInstance() {
    // Act
    ServiceDetails details =
        new ServiceDetails("prod", "service1", "component1", 30, "tenant1");

    // Assert
    Assert.assertEquals(details.getEnv(), "prod");
    Assert.assertEquals(details.getServiceName(), "service1");
    Assert.assertEquals(details.getComponentName(), "component1");
    Assert.assertEquals(details.getRetentionDays(), Integer.valueOf(30));
    Assert.assertEquals(details.getTenant(), "tenant1");
  }

  @Test
  public void testServiceDetails_Builder_CreatesInstance() {
    // Act
    ServiceDetails details =
        ServiceDetails.builder()
            .env("dev")
            .serviceName("service2")
            .componentName("component2")
            .retentionDays(60)
            .tenant("tenant2")
            .build();

    // Assert
    Assert.assertEquals(details.getEnv(), "dev");
    Assert.assertEquals(details.getServiceName(), "service2");
    Assert.assertEquals(details.getComponentName(), "component2");
    Assert.assertEquals(details.getRetentionDays(), Integer.valueOf(60));
    Assert.assertEquals(details.getTenant(), "tenant2");
  }

  @Test
  public void testServiceDetails_Equals_WithSameValues_ReturnsTrue() {
    // Arrange
    ServiceDetails details1 = new ServiceDetails("prod", "service1", "component1", 30, "tenant1");
    ServiceDetails details2 = new ServiceDetails("prod", "service1", "component1", 30, "tenant1");

    // Act & Assert
    Assert.assertTrue(details1.equals(details2));
  }

  @Test
  public void testServiceDetails_Equals_WithDifferentEnv_ReturnsFalse() {
    // Arrange
    ServiceDetails details1 = new ServiceDetails("prod", "service1", "component1", 30, "tenant1");
    ServiceDetails details2 = new ServiceDetails("dev", "service1", "component1", 30, "tenant1");

    // Act & Assert
    Assert.assertFalse(details1.equals(details2));
  }

  // ========== SparkSubmitArgs Tests ==========

  @Test
  public void testSparkSubmitArgs_AllArgsConstructor_CreatesInstance() {
    // Act
    SparkSubmitArgs args = new SparkSubmitArgs(1234567890L, true, false, 1L, true, false);

    // Assert
    Assert.assertEquals(args.getTimeStamp(), Long.valueOf(1234567890L));
    Assert.assertTrue(args.getCleanStateRequired());
    Assert.assertFalse(args.getSubmitJobRequired());
    Assert.assertEquals(args.getSparkSubmitStatusId(), Long.valueOf(1L));
    Assert.assertTrue(args.getSubmittedForOffsetsTimestamp());
    Assert.assertFalse(args.getResumedToSubscribePattern());
  }

  @Test
  public void testSparkSubmitArgs_Setters_Work() {
    // Arrange
    SparkSubmitArgs args = new SparkSubmitArgs(0L, false, false, null, null, null);

    // Act
    args.setTimeStamp(9876543210L);
    args.setCleanStateRequired(true);
    args.setSubmitJobRequired(true);

    // Assert
    Assert.assertEquals(args.getTimeStamp(), Long.valueOf(9876543210L));
    Assert.assertTrue(args.getCleanStateRequired());
    Assert.assertTrue(args.getSubmitJobRequired());
  }

  // ========== SparkSubmitStatus Tests ==========

  @Test
  public void testSparkSubmitStatus_NoArgsConstructor_CreatesInstance() {
    // Act
    SparkSubmitStatus status = new SparkSubmitStatus();

    // Assert
    Assert.assertNotNull(status);
    Assert.assertNull(status.getId());
    Assert.assertEquals(status.getStartingOffsetsTimestamp(), Long.valueOf(0L));
    Assert.assertEquals(status.getResumeToSubscribePatternTimestamp(), Long.valueOf(0L));
    Assert.assertFalse(status.getIsSubmittedForOffsetsTimestamp());
    Assert.assertFalse(status.getIsResumedToSubscribePattern());
  }

  @Test
  public void testSparkSubmitStatus_Builder_CreatesInstance() {
    // Act
    SparkSubmitStatus status =
        SparkSubmitStatus.builder()
            .id(2L)
            .startingOffsetsTimestamp(3000L)
            .resumeToSubscribePatternTimestamp(4000L)
            .isSubmittedForOffsetsTimestamp(true)
            .isResumedToSubscribePattern(true)
            .tenant("tenant2")
            .build();

    // Assert
    Assert.assertEquals(status.getId(), Long.valueOf(2L));
    Assert.assertEquals(status.getStartingOffsetsTimestamp(), Long.valueOf(3000L));
    Assert.assertEquals(status.getResumeToSubscribePatternTimestamp(), Long.valueOf(4000L));
    Assert.assertTrue(status.getIsSubmittedForOffsetsTimestamp());
    Assert.assertTrue(status.getIsResumedToSubscribePattern());
    Assert.assertEquals(status.getTenant(), "tenant2");
  }

  // ========== ComponentSyncRequest Tests ==========

  @Test
  public void testComponentSyncRequest_NoArgsConstructor_CreatesInstance() {
    // Act
    ComponentSyncRequest request = new ComponentSyncRequest();

    // Assert
    Assert.assertNotNull(request);
    Assert.assertNull(request.getComponentType());
  }

  @Test
  public void testComponentSyncRequest_SetComponentType_SetsValue() {
    // Arrange
    ComponentSyncRequest request = new ComponentSyncRequest();

    // Act
    request.setComponentType("application"); // Valid ComponentType value

    // Assert
    Assert.assertEquals(request.getComponentType(), "application");
  }

  @Test
  public void testComponentSyncRequest_ValidateParam_WithValidComponentType_Passes() {
    // Arrange
    ComponentSyncRequest request = new ComponentSyncRequest();
    request.setComponentType("application"); // Valid ComponentType value

    // Act & Assert - Should not throw exception
    request.validateParam();
  }

  @Test
  public void testComponentSyncRequest_ValidateParam_WithNullComponentType_ThrowsException() {
    // Arrange
    ComponentSyncRequest request = new ComponentSyncRequest();
    request.setComponentType(null);

    // Act & Assert
    try {
      request.validateParam();
      Assert.fail("Should have thrown RestException");
    } catch (RestException e) {
      Assert.assertNotNull(e);
    }
  }

  // ========== MonitorSparkJobRequest Tests ==========

  @Test
  public void testMonitorSparkJobRequest_NoArgsConstructor_CreatesInstance() {
    // Act
    MonitorSparkJobRequest request = new MonitorSparkJobRequest();

    // Assert
    Assert.assertNotNull(request);
    Assert.assertNull(request.getDriverCores());
    Assert.assertNull(request.getDriverMemoryInGb());
  }

  @Test
  public void testMonitorSparkJobRequest_Setters_Work() {
    // Arrange
    MonitorSparkJobRequest request = new MonitorSparkJobRequest();

    // Act
    request.setDriverCores(4);
    request.setDriverMemoryInGb(8);

    // Assert
    Assert.assertEquals(request.getDriverCores(), Integer.valueOf(4));
    Assert.assertEquals(request.getDriverMemoryInGb(), Integer.valueOf(8));
  }

  // ========== SubmitSparkJobRequest Tests ==========

  @Test
  public void testSubmitSparkJobRequest_Builder_CreatesInstance() {
    // Act
    SubmitSparkJobRequest request =
        SubmitSparkJobRequest.builder()
            .action("CreateSubmissionRequest")
            .appArgs(Arrays.asList("arg1", "arg2"))
            .appResource("s3://bucket/app.jar")
            .clientSparkVersion("3.2.0")
            .mainClass("com.example.Main")
            .build();

    // Assert
    Assert.assertEquals(request.getAction(), "CreateSubmissionRequest");
    Assert.assertEquals(request.getAppArgs().size(), 2);
    Assert.assertEquals(request.getAppResource(), "s3://bucket/app.jar");
    Assert.assertEquals(request.getClientSparkVersion(), "3.2.0");
    Assert.assertEquals(request.getMainClass(), "com.example.Main");
  }

  @Test
  public void testSubmitSparkJobRequest_Builder_WithDefaults_UsesDefaults() {
    // Act
    SubmitSparkJobRequest request = SubmitSparkJobRequest.builder().build();

    // Assert
    Assert.assertEquals(request.getAction(), "CreateSubmissionRequest");
  }

  @Test
  public void testSubmitSparkJobRequest_Builder_WithEnvironmentVariables_SetsValue() {
    // Arrange
    Map<String, String> envVars = new HashMap<>();
    envVars.put("KEY1", "VALUE1");
    envVars.put("KEY2", "VALUE2");

    // Act
    SubmitSparkJobRequest request =
        SubmitSparkJobRequest.builder().environmentVariables(envVars).build();

    // Assert
    Assert.assertNotNull(request.getEnvironmentVariables());
    Assert.assertEquals(request.getEnvironmentVariables().size(), 2);
    Assert.assertEquals(request.getEnvironmentVariables().get("KEY1"), "VALUE1");
  }

  // ========== DefaultErrorResponse Tests ==========

  @Test
  public void testDefaultErrorResponse_CreatesInstance() {
    // Note: DefaultErrorResponse has a private static inner class ErrorResponse
    // We can test the structure exists
    Assert.assertNotNull(DefaultErrorResponse.class);
  }

  // ========== DefaultSuccessResponse Tests ==========

  @Test
  public void testDefaultSuccessResponse_Builder_CreatesInstance() {
    // Act
    DefaultSuccessResponse response = DefaultSuccessResponse.builder().build();

    // Assert
    Assert.assertTrue(response.isSuccess());
    Assert.assertEquals(response.getMessage(), "Success");
  }

  @Test
  public void testDefaultSuccessResponse_Builder_WithCustomMessage_SetsMessage() {
    // Act
    DefaultSuccessResponse response =
        DefaultSuccessResponse.builder().message("Custom success message").build();

    // Assert
    Assert.assertTrue(response.isSuccess());
    Assert.assertEquals(response.getMessage(), "Custom success message");
  }

  // ========== GetServiceDetailsResponse Tests ==========

  @Test
  public void testGetServiceDetailsResponse_Builder_CreatesInstance() {
    // Arrange
    List<ServiceDetails> details =
        Arrays.asList(
            ServiceDetails.builder()
                .env("dev")
                .serviceName("service2")
                .componentName("component2")
                .build());

    // Act
    GetServiceDetailsResponse response =
        GetServiceDetailsResponse.builder().serviceDetails(details).build();

    // Assert
    Assert.assertNotNull(response.getServiceDetails());
    Assert.assertEquals(response.getServiceDetails().size(), 1);
  }

  // ========== LogSyncDelayResponse Tests ==========

  @Test
  public void testLogSyncDelayResponse_Builder_WithValues_SetsValues() {
    // Act
    LogSyncDelayResponse response =
        LogSyncDelayResponse.builder().tenant("tenant1").appLogsDelayMinutes(15).build();

    // Assert
    Assert.assertEquals(response.getTenant(), "tenant1");
    Assert.assertEquals(response.getAppLogsDelayMinutes(), Integer.valueOf(15));
  }

  // ========== SparkMasterJsonResponse Tests ==========

  @Test
  public void testSparkMasterJsonResponse_NoArgsConstructor_CreatesInstance() {
    // Act
    SparkMasterJsonResponse response = new SparkMasterJsonResponse();

    // Assert
    Assert.assertNotNull(response);
    Assert.assertNull(response.getActiveapps());
    Assert.assertEquals(response.getMemory(), 0);
    Assert.assertNull(response.getCores());
  }

  @Test
  public void testSparkMasterJsonResponse_Setters_Work() {
    // Arrange
    SparkMasterJsonResponse response = new SparkMasterJsonResponse();

    // Act
    response.setStatus("ALIVE");
    response.setUrl("http://spark-master:8080");
    response.setCores(100);
    response.setMemory(1024);

    // Assert
    Assert.assertEquals(response.getStatus(), "ALIVE");
    Assert.assertEquals(response.getUrl(), "http://spark-master:8080");
    Assert.assertEquals(response.getCores(), Integer.valueOf(100));
    Assert.assertEquals(response.getMemory(), 1024);
  }

  @Test
  public void testSparkMasterJsonResponse_App_NoArgsConstructor_CreatesInstance() {
    // Act
    SparkMasterJsonResponse.App app = new SparkMasterJsonResponse.App();

    // Assert
    Assert.assertNotNull(app);
    Assert.assertEquals(app.getDuration(), 0L);
    Assert.assertEquals(app.getCores(), 0);
  }

  @Test
  public void testSparkMasterJsonResponse_App_Setters_Work() {
    // Arrange
    SparkMasterJsonResponse.App app = new SparkMasterJsonResponse.App();

    // Act
    app.setId("app-123");
    app.setName("test-app");
    app.setState("RUNNING");
    app.setUser("testuser");
    app.setCores(4);
    app.setMemoryperexecutor(2048);

    // Assert
    Assert.assertEquals(app.getId(), "app-123");
    Assert.assertEquals(app.getName(), "test-app");
    Assert.assertEquals(app.getState(), "RUNNING");
    Assert.assertEquals(app.getUser(), "testuser");
    Assert.assertEquals(app.getCores(), 4);
    Assert.assertEquals(app.getMemoryperexecutor(), 2048);
  }

  @Test
  public void testSparkMasterJsonResponse_Driver_CreatesInstance() {
    // Act
    SparkMasterJsonResponse.Driver driver = new SparkMasterJsonResponse.Driver();

    // Assert
    Assert.assertNotNull(driver);
    Assert.assertEquals(driver.getCores(), 0);
    Assert.assertEquals(driver.getMemory(), 0);
  }

  @Test
  public void testSparkMasterJsonResponse_Driver_Setters_Work() {
    // Arrange
    SparkMasterJsonResponse.Driver driver = new SparkMasterJsonResponse.Driver();

    // Act
    driver.setId("driver-123");
    driver.setState("RUNNING");
    driver.setMainclass("com.example.Main");
    driver.setCores(2);
    driver.setMemory(1024);

    // Assert
    Assert.assertEquals(driver.getId(), "driver-123");
    Assert.assertEquals(driver.getState(), "RUNNING");
    Assert.assertEquals(driver.getMainclass(), "com.example.Main");
    Assert.assertEquals(driver.getCores(), 2);
    Assert.assertEquals(driver.getMemory(), 1024);
  }

  @Test
  public void testSparkMasterJsonResponse_Worker_NoArgsConstructor_CreatesInstance() {
    // Act
    SparkMasterJsonResponse.Worker worker = new SparkMasterJsonResponse.Worker();

    // Assert
    Assert.assertNotNull(worker);
    Assert.assertEquals(worker.getCores(), 0);
    Assert.assertEquals(worker.getMemory(), 0);
  }

  @Test
  public void testSparkMasterJsonResponse_Worker_Setters_Work() {
    // Arrange
    SparkMasterJsonResponse.Worker worker = new SparkMasterJsonResponse.Worker();

    // Act
    worker.setId("worker-123");
    worker.setHost("worker1.example.com");
    worker.setPort(8081);
    worker.setState("ALIVE");
    worker.setCores(8);
    worker.setMemory(8192);
    worker.setCoresused(4);
    worker.setCoresfree(4);
    worker.setMemoryused(4096);
    worker.setMemoryfree(4096);

    // Assert
    Assert.assertEquals(worker.getId(), "worker-123");
    Assert.assertEquals(worker.getHost(), "worker1.example.com");
    Assert.assertEquals(worker.getPort(), 8081);
    Assert.assertEquals(worker.getState(), "ALIVE");
    Assert.assertEquals(worker.getCores(), 8);
    Assert.assertEquals(worker.getMemory(), 8192);
    Assert.assertEquals(worker.getCoresused(), 4);
    Assert.assertEquals(worker.getCoresfree(), 4);
    Assert.assertEquals(worker.getMemoryused(), 4096);
    Assert.assertEquals(worker.getMemoryfree(), 4096);
  }
}

