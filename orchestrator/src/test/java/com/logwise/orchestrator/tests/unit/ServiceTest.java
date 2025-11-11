package com.dream11.logcentralorchestrator.tests.unit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.dream11.logcentralorchestrator.CaffeineCacheFactory;
import com.dream11.logcentralorchestrator.client.ObjectStoreClient;
import com.dream11.logcentralorchestrator.common.util.CompletableFutureUtils;
import com.dream11.logcentralorchestrator.config.ApplicationConfig;
import com.dream11.logcentralorchestrator.constant.ApplicationConstants;
import com.dream11.logcentralorchestrator.dao.ServicesDao;
import com.dream11.logcentralorchestrator.dao.SparkSubmitStatusDao;
import com.dream11.logcentralorchestrator.dto.entity.ServiceDetails;
import com.dream11.logcentralorchestrator.dto.request.SubmitSparkJobRequest;
import com.dream11.logcentralorchestrator.dto.response.GetServiceDetailsResponse;
import com.dream11.logcentralorchestrator.dto.response.LogSyncDelayResponse;
import com.dream11.logcentralorchestrator.dto.response.SparkMasterJsonResponse;
import com.dream11.logcentralorchestrator.enums.Tenant;
import com.dream11.logcentralorchestrator.factory.ObjectStoreFactory;
import com.dream11.logcentralorchestrator.service.MetricsService;
import com.dream11.logcentralorchestrator.service.ObjectStoreService;
import com.dream11.logcentralorchestrator.service.ServiceManagerService;
import com.dream11.logcentralorchestrator.service.SparkService;
import com.dream11.logcentralorchestrator.setup.BaseTest;
import com.dream11.logcentralorchestrator.testconfig.ApplicationTestConfig;
import com.dream11.logcentralorchestrator.util.ApplicationConfigUtil;
import com.dream11.logcentralorchestrator.util.ApplicationUtils;
import com.dream11.logcentralorchestrator.webclient.reactivex.client.WebClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for service package (ObjectStoreService, SparkService, ServiceManagerService, MetricsService). */
public class ServiceTest extends BaseTest {

  // ========== ObjectStoreService Test Fields ==========
  private ObjectStoreService objectStoreService;
  private ServicesDao mockServicesDao;
  private ObjectStoreClient mockObjectStoreClient;
  @Mock private ApplicationConfig.TenantConfig mockTenantConfig;
  private ApplicationConfig.SparkConfig mockSparkConfig;

  // ========== SparkService Test Fields ==========
  private SparkService sparkService;
  private WebClient mockWebClient;
  private ObjectMapper mockObjectMapper;
  private SparkSubmitStatusDao mockSparkSubmitStatusDao;
  private io.vertx.reactivex.ext.web.client.WebClient mockVertxWebClient;
  private ApplicationConfig.KafkaConfig mockKafkaConfig;

  // ========== ServiceManagerService Test Fields ==========
  @Mock private ServicesDao mockServicesDaoForManager;
  @Mock private ObjectStoreService mockObjectStoreServiceForManager;
  @Mock private AsyncLoadingCache<Tenant, GetServiceDetailsResponse> mockCache;
  private ServiceManagerService serviceManagerService;
  private Vertx vertx;

  // ========== MetricsService Test Fields ==========
  private MetricsService metricsService;
  private ApplicationConfig.DelayMetricsConfig mockDelayMetricsConfig;
  private ApplicationConfig.ApplicationDelayMetricsConfig mockAppDelayMetricsConfig;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    MockitoAnnotations.openMocks(this);

    // Setup ObjectStoreService
    mockServicesDao = mock(ServicesDao.class);
    objectStoreService = new ObjectStoreService();
    Field servicesDaoField = ObjectStoreService.class.getDeclaredField("servicesDao");
    servicesDaoField.setAccessible(true);
    servicesDaoField.set(objectStoreService, mockServicesDao);

    // Create mockObjectStoreClient manually to ensure proper initialization
    mockObjectStoreClient = mock(ObjectStoreClient.class);
    // mockTenantConfig is initialized via @Mock annotation and MockitoAnnotations.openMocks(this)
    mockSparkConfig = mock(ApplicationConfig.SparkConfig.class);
    when(mockTenantConfig.getName()).thenReturn("test-tenant");
    when(mockTenantConfig.getSpark()).thenReturn(mockSparkConfig);
    when(mockSparkConfig.getLogsDir()).thenReturn("logs"); // Required for getAllDistinctServicesInAws
    when(mockTenantConfig.getObjectStore()).thenReturn(ApplicationTestConfig.createMockObjectStoreConfig());

    // Setup SparkService
    mockWebClient = mock(WebClient.class);
    mockObjectMapper = new ObjectMapper();
    mockSparkSubmitStatusDao = mock(SparkSubmitStatusDao.class);
    mockVertxWebClient = mock(io.vertx.reactivex.ext.web.client.WebClient.class);
    mockKafkaConfig = mock(ApplicationConfig.KafkaConfig.class);
    sparkService = new SparkService(mockWebClient, mockObjectMapper, mockSparkSubmitStatusDao);
    doReturn(mockVertxWebClient).when(mockWebClient).getWebClient();
    when(mockTenantConfig.getKafka()).thenReturn(mockKafkaConfig);

    // Setup ServiceManagerService
    if (BaseTest.vertx != null) {
      vertx = new io.vertx.reactivex.core.Vertx(BaseTest.vertx);
    } else {
      vertx = new io.vertx.reactivex.core.Vertx(io.vertx.core.Vertx.vertx());
    }

    try (MockedStatic<CaffeineCacheFactory> mockedFactory =
        Mockito.mockStatic(CaffeineCacheFactory.class)) {
      mockedFactory
          .when(
              () ->
                  CaffeineCacheFactory.createAsyncLoadingCache(
                      any(Vertx.class), anyString(), any(), anyString()))
          .thenReturn(mockCache);
      serviceManagerService =
          new ServiceManagerService(vertx, mockServicesDaoForManager, mockObjectStoreServiceForManager);
    }

    // Setup MetricsService
    metricsService = new MetricsService();
    mockDelayMetricsConfig = mock(ApplicationConfig.DelayMetricsConfig.class);
    mockAppDelayMetricsConfig = mock(ApplicationConfig.ApplicationDelayMetricsConfig.class);
    when(mockTenantConfig.getDelayMetrics()).thenReturn(mockDelayMetricsConfig);
    when(mockDelayMetricsConfig.getApp()).thenReturn(mockAppDelayMetricsConfig);
    when(mockAppDelayMetricsConfig.getSampleEnv()).thenReturn("prod");
    when(mockAppDelayMetricsConfig.getSampleServiceName()).thenReturn("test-service");
    when(mockAppDelayMetricsConfig.getSampleComponentName()).thenReturn("test-component");
  }

  @AfterClass
  public static void tearDownClass() {
    BaseTest.cleanup();
  }

  // ========== ObjectStoreService Tests ==========

  @Test
  public void testObjectStoreService_GetAllDistinctServicesInAws_WithValidServices_ReturnsServiceDetailsList()
      throws Exception {
    // Arrange
    Tenant tenant = Tenant.D11_Prod_AWS;
    List<String> envPrefixes = Arrays.asList("logs/env=prod/", "logs/env=staging/");
    List<String> servicePrefixes1 = Arrays.asList("logs/env=prod/service_name=service1/");
    List<String> servicePrefixes2 = Arrays.asList("logs/env=staging/service_name=service2/");
    List<String> componentPrefixes =
        Arrays.asList(
            "logs/env=prod/service_name=service1/component_name=component1/",
            "logs/env=staging/service_name=service2/component_name=component2/");

    ServiceDetails service1 =
        ServiceDetails.builder()
            .env("prod")
            .serviceName("service1")
            .componentName("component1")
            .build();
    ServiceDetails service2 =
        ServiceDetails.builder()
            .env("staging")
            .serviceName("service2")
            .componentName("component2")
            .build();

    try (MockedStatic<ObjectStoreFactory> mockedFactory =
            Mockito.mockStatic(ObjectStoreFactory.class);
        MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            Mockito.mockStatic(ApplicationConfigUtil.class);
        MockedStatic<ApplicationUtils> mockedUtils =
            Mockito.mockStatic(ApplicationUtils.class)) {

      mockedFactory.when(() -> ObjectStoreFactory.getClient(tenant)).thenReturn(mockObjectStoreClient);
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);

      // Stub exact string matches as expected - use lenient() to avoid strict stubbing issues
      lenient().when(mockObjectStoreClient.listCommonPrefix("logs/env=", "/"))
          .thenReturn(Single.just(envPrefixes));
      lenient().when(mockObjectStoreClient.listCommonPrefix("logs/env=prod/service_name=", "/"))
          .thenReturn(Single.just(servicePrefixes1));
      lenient().when(mockObjectStoreClient.listCommonPrefix("logs/env=staging/service_name=", "/"))
          .thenReturn(Single.just(servicePrefixes2));
      lenient().when(mockObjectStoreClient.listCommonPrefix(
              "logs/env=prod/service_name=service1/component_name=", "/"))
          .thenReturn(Single.just(Collections.singletonList(componentPrefixes.get(0))));
      lenient().when(mockObjectStoreClient.listCommonPrefix(
              "logs/env=staging/service_name=service2/component_name=", "/"))
          .thenReturn(Single.just(Collections.singletonList(componentPrefixes.get(1))));

      mockedUtils
          .when(() -> ApplicationUtils.getServiceFromObjectKey(componentPrefixes.get(0)))
          .thenReturn(service1);
      mockedUtils
          .when(() -> ApplicationUtils.getServiceFromObjectKey(componentPrefixes.get(1)))
          .thenReturn(service2);

      ApplicationConfig.EnvLogsRetentionDaysConfig retentionConfig =
          new ApplicationConfig.EnvLogsRetentionDaysConfig();
      retentionConfig.setEnvs(Arrays.asList("prod", "staging"));
      retentionConfig.setRetentionDays(30);
      when(mockTenantConfig.getEnvLogsRetentionDays())
          .thenReturn(Collections.singletonList(retentionConfig));
      when(mockTenantConfig.getDefaultLogsRetentionDays()).thenReturn(3);

      // Act
      Single<List<ServiceDetails>> result = objectStoreService.getAllDistinctServicesInAws(tenant);
      List<ServiceDetails> services = result.blockingGet();

      // Assert
      Assert.assertNotNull(services);
      Assert.assertEquals(services.size(), 2);
      Assert.assertEquals(services.get(0).getEnv(), "prod");
      Assert.assertEquals(services.get(0).getServiceName(), "service1");
      Assert.assertEquals(services.get(0).getComponentName(), "component1");
      Assert.assertEquals(services.get(0).getRetentionDays(), Integer.valueOf(30));
      Assert.assertEquals(services.get(0).getTenant(), tenant.getValue());
    }
  }

  @Test
  public void testObjectStoreService_GetAllDistinctServicesInAws_WithNoServices_ReturnsEmptyList()
      throws Exception {
    // Arrange
    Tenant tenant = Tenant.D11_Prod_AWS;

    try (MockedStatic<ObjectStoreFactory> mockedFactory =
            Mockito.mockStatic(ObjectStoreFactory.class);
        MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            Mockito.mockStatic(ApplicationConfigUtil.class)) {

      mockedFactory.when(() -> ObjectStoreFactory.getClient(tenant)).thenReturn(mockObjectStoreClient);
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);

      lenient().when(mockObjectStoreClient.listCommonPrefix("logs/env=", "/"))
          .thenReturn(Single.just(Collections.emptyList()));

      // Act
      Single<List<ServiceDetails>> result = objectStoreService.getAllDistinctServicesInAws(tenant);
      List<ServiceDetails> services = result.blockingGet();

      // Assert
      Assert.assertNotNull(services);
      Assert.assertTrue(services.isEmpty());
    }
  }

  @Test
  public void testObjectStoreService_GetAllDistinctServicesInAws_WhenClientFails_PropagatesError() {
    // Arrange
    Tenant tenant = Tenant.D11_Prod_AWS;
    RuntimeException error = new RuntimeException("S3 client error");

    try (MockedStatic<ObjectStoreFactory> mockedFactory =
            Mockito.mockStatic(ObjectStoreFactory.class);
        MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            Mockito.mockStatic(ApplicationConfigUtil.class)) {

      mockedFactory.when(() -> ObjectStoreFactory.getClient(tenant)).thenReturn(mockObjectStoreClient);
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);

      lenient().when(mockObjectStoreClient.listCommonPrefix("logs/env=", "/"))
          .thenReturn(Single.error(error));

      // Act
      Single<List<ServiceDetails>> result = objectStoreService.getAllDistinctServicesInAws(tenant);

      // Assert
      try {
        result.blockingGet();
        Assert.fail("Should have thrown exception");
      } catch (RuntimeException e) {
        Assert.assertNotNull(e);
      }
    }
  }

  // ========== SparkService Tests ==========

  @Test
  public void testSparkService_GetSparkSubmitRequestBody_WithValidConfig_ReturnsRequest() {
    // Arrange
    when(mockSparkConfig.getSparkJarPath()).thenReturn("s3://bucket/app.jar");
    when(mockSparkConfig.getClientSparkVersion()).thenReturn("3.2.0");
    when(mockSparkConfig.getMainClass()).thenReturn("com.example.Main");
    when(mockSparkConfig.getDriverCores()).thenReturn("2");
    when(mockSparkConfig.getDriverMemory()).thenReturn("4G");
    when(mockSparkConfig.getDriverCoresMax()).thenReturn("10");
    when(mockSparkConfig.getDriverMaxResultSize()).thenReturn("2g");
    when(mockSparkConfig.getExecutorCores()).thenReturn("2");
    when(mockSparkConfig.getExecutorMemory()).thenReturn("4G");
    when(mockSparkConfig.getLog4jPropertiesFilePath()).thenReturn("log4j.properties");
    when(mockSparkConfig.getAppName()).thenReturn("test-app");
    when(mockSparkConfig.getKafkaMaxRatePerPartition()).thenReturn("1000");
    when(mockSparkConfig.getKafkaStartingOffsets()).thenReturn("latest");
    when(mockSparkConfig.getSubscribePattern()).thenReturn("topic.*");
    when(mockSparkConfig.getSparkMasterHost()).thenReturn("spark-master");
    when(mockKafkaConfig.getKafkaBrokersHost()).thenReturn("kafka-broker:9092");
    when(mockKafkaConfig.getKafkaManagerClusterName()).thenReturn("test-cluster");
    when(mockKafkaConfig.getKafkaManagerUrl()).thenReturn("http://kafka-manager:9000");
    when(mockSparkConfig.getS3aAccessKey()).thenReturn("access-key");
    when(mockSparkConfig.getS3aSecretKey()).thenReturn("secret-key");

    // Act - Use reflection to call private static method
    try {
      Method method =
          SparkService.class.getDeclaredMethod(
              "getSparkSubmitRequestBody",
              ApplicationConfig.TenantConfig.class,
              Long.class,
              Integer.class,
              Integer.class);
      method.setAccessible(true);
      SubmitSparkJobRequest request =
          (SubmitSparkJobRequest) method.invoke(sparkService, mockTenantConfig, 1234567890L, 4, 8);

      // Assert
      Assert.assertNotNull(request);
      Assert.assertEquals(request.getAction(), "CreateSubmissionRequest");
      Assert.assertNotNull(request.getAppArgs());
      Assert.assertTrue(request.getAppArgs().size() > 0);
      Assert.assertEquals(request.getAppResource(), "s3://bucket/app.jar");
      Assert.assertEquals(request.getClientSparkVersion(), "3.2.0");
      Assert.assertEquals(request.getMainClass(), "com.example.Main");
      Assert.assertNotNull(request.getEnvironmentVariables());
      Assert.assertNotNull(request.getSparkProperties());
      Assert.assertEquals(request.getSparkProperties().get("spark.driver.cores"), "4");
      Assert.assertEquals(request.getSparkProperties().get("spark.driver.memory"), "8G");
    } catch (Exception e) {
      Assert.fail("Should not throw exception", e);
    }
  }

  @Test
  public void testSparkService_IsDriverNotRunning_WithEmptyDrivers_ReturnsTrue() {
    // Arrange
    SparkMasterJsonResponse response = new SparkMasterJsonResponse();
    response.setActivedrivers(new ArrayList<>());

    // Act - Use reflection to call private static method
    try {
      Method method =
          SparkService.class.getDeclaredMethod("isDriverNotRunning", SparkMasterJsonResponse.class);
      method.setAccessible(true);
      boolean result = (Boolean) method.invoke(sparkService, response);

      // Assert
      Assert.assertTrue(result);
    } catch (Exception e) {
      Assert.fail("Should not throw exception", e);
    }
  }

  @Test
  public void testSparkService_IsDriverNotRunning_WithRunningDriver_ReturnsFalse() {
    // Arrange
    SparkMasterJsonResponse response = new SparkMasterJsonResponse();
    SparkMasterJsonResponse.Driver driver = new SparkMasterJsonResponse.Driver();
    driver.setState("RUNNING");
    response.setActivedrivers(Arrays.asList(driver));

    // Act
    try {
      Method method =
          SparkService.class.getDeclaredMethod("isDriverNotRunning", SparkMasterJsonResponse.class);
      method.setAccessible(true);
      boolean result = (Boolean) method.invoke(sparkService, response);

      // Assert
      Assert.assertFalse(result);
    } catch (Exception e) {
      Assert.fail("Should not throw exception", e);
    }
  }

  @Test
  public void testSparkService_CleanSparkState_WithValidTenant_DeletesFiles() {
    // Arrange
    Tenant tenant = Tenant.D11_Prod_AWS;
    ObjectStoreClient mockObjectStoreClientForSpark = mock(ObjectStoreClient.class);
    List<String> checkpointFiles = Arrays.asList("checkpoint/file1", "checkpoint/file2");
    List<String> metadataFiles = Arrays.asList("metadata/file1");

    when(mockSparkConfig.getCheckPointDir()).thenReturn("checkpoint");
    when(mockSparkConfig.getLogsDir()).thenReturn("logs");

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            Mockito.mockStatic(ApplicationConfigUtil.class);
        MockedStatic<ObjectStoreFactory> mockedFactory =
            Mockito.mockStatic(ObjectStoreFactory.class)) {
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);
      mockedFactory.when(() -> ObjectStoreFactory.getClient(tenant)).thenReturn(mockObjectStoreClientForSpark);

      when(mockObjectStoreClientForSpark.listObjects("checkpoint/"))
          .thenReturn(Single.just(checkpointFiles));
      when(mockObjectStoreClientForSpark.listObjects("logs/_spark_metadata/"))
          .thenReturn(Single.just(metadataFiles));
      when(mockObjectStoreClientForSpark.deleteFile(anyString())).thenReturn(Completable.complete());

      // Act
      Completable result = sparkService.cleanSparkState(tenant);
      result.blockingAwait();

      // Assert
      verify(mockObjectStoreClientForSpark, times(3)).deleteFile(anyString());
    }
  }

  // ========== ServiceManagerService Tests ==========

  @Test
  public void testServiceManagerService_GetServiceDetailsFromCache_WithValidTenant_ReturnsCachedResponse() {
    // Arrange
    Tenant tenant = Tenant.D11_Prod_AWS;
    GetServiceDetailsResponse cachedResponse =
        GetServiceDetailsResponse.builder().serviceDetails(Collections.emptyList()).build();

    CompletableFuture<GetServiceDetailsResponse> future =
        CompletableFuture.completedFuture(cachedResponse);
    when(mockCache.get(tenant)).thenReturn(future);

    // Mock CompletableFutureUtils to avoid Vertx context requirement
    try (MockedStatic<CompletableFutureUtils> mockedUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedUtils
          .when(() -> CompletableFutureUtils.toSingle(any(CompletableFuture.class)))
          .thenAnswer(
              invocation -> {
                CompletableFuture<GetServiceDetailsResponse> cf = invocation.getArgument(0);
                return Single.fromFuture(cf);
              });

      // Act
      Single<GetServiceDetailsResponse> result =
          serviceManagerService.getServiceDetailsFromCache(tenant);
      GetServiceDetailsResponse response = result.blockingGet();

      // Assert
      Assert.assertNotNull(response);
      Assert.assertEquals(response, cachedResponse);
    }
  }

  @Test
  public void testServiceManagerService_GetServiceDetailsFromDB_WithValidTenant_ReturnsServiceDetails() {
    // Arrange
    Tenant tenant = Tenant.D11_Prod_AWS;
    List<ServiceDetails> serviceDetailsList = new ArrayList<>();
    serviceDetailsList.add(
        ServiceDetails.builder()
            .env("prod")
            .serviceName("test-service")
            .componentName("test-component")
            .build());

    when(mockServicesDaoForManager.getAllServiceDetails(tenant))
        .thenReturn(Single.just(serviceDetailsList));

    // Act
    Single<GetServiceDetailsResponse> result = serviceManagerService.getServiceDetailsFromDB(tenant);
    GetServiceDetailsResponse response = result.blockingGet();

    // Assert
    Assert.assertNotNull(response);
    Assert.assertNotNull(response.getServiceDetails());
    Assert.assertEquals(response.getServiceDetails().size(), 1);
    Assert.assertEquals(response.getServiceDetails().get(0).getServiceName(), "test-service");
  }

  @Test
  public void testServiceManagerService_SyncServices_WithServicesNotInDb_OnBoardsNewServices() {
    // Arrange
    Tenant tenant = Tenant.D11_Prod_AWS;

    // DB has no services
    List<ServiceDetails> dbServices = new ArrayList<>();

    // Object store has one service
    List<ServiceDetails> objectStoreServices = new ArrayList<>();
    ServiceDetails newService =
        ServiceDetails.builder()
            .env("prod")
            .serviceName("new-service")
            .componentName("new-component")
            .build();
    objectStoreServices.add(newService);

    when(mockServicesDaoForManager.getAllServiceDetails(tenant))
        .thenReturn(Single.just(dbServices));
    when(mockObjectStoreServiceForManager.getAllDistinctServicesInAws(tenant))
        .thenReturn(Single.just(objectStoreServices));
    when(mockServicesDaoForManager.insertServiceDetails(anyList()))
        .thenReturn(Completable.complete());

    // Act
    Completable result = serviceManagerService.syncServices(tenant);
    result.blockingAwait();

    // Assert
    verify(mockServicesDaoForManager, times(1)).insertServiceDetails(anyList());
    verify(mockServicesDaoForManager, never()).deleteServiceDetails(anyList());
  }

  @Test
  public void testServiceManagerService_SyncServices_WithNoChanges_CompletesWithoutModifications() {
    // Arrange
    Tenant tenant = Tenant.D11_Prod_AWS;

    // Same service in both DB and object store
    ServiceDetails service =
        ServiceDetails.builder()
            .env("prod")
            .serviceName("test-service")
            .componentName("test-component")
            .build();

    List<ServiceDetails> services = Collections.singletonList(service);

    when(mockServicesDaoForManager.getAllServiceDetails(tenant))
        .thenReturn(Single.just(services));
    when(mockObjectStoreServiceForManager.getAllDistinctServicesInAws(tenant))
        .thenReturn(Single.just(services));

    // Act
    Completable result = serviceManagerService.syncServices(tenant);
    result.blockingAwait();

    // Assert
    verify(mockServicesDaoForManager, never()).insertServiceDetails(anyList());
    verify(mockServicesDaoForManager, never()).deleteServiceDetails(anyList());
  }

  // ========== MetricsService Tests ==========

  @Test
  public void testMetricsService_ComputeLogSyncDelay_WithValidTenant_ReturnsLogSyncDelayResponse() {
    // Arrange
    Tenant tenant = Tenant.D11_Prod_AWS;
    List<String> objectNames = new ArrayList<>();
    objectNames.add(
        "logs/env=prod/service_name=test-service/component_name=test-component/year=2024/month=01/day=01/hour=10/minute=30/file.log");

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            Mockito.mockStatic(ApplicationConfigUtil.class);
        MockedStatic<ObjectStoreFactory> mockedFactory =
            Mockito.mockStatic(ObjectStoreFactory.class);
        MockedStatic<ApplicationUtils> mockedUtils =
            Mockito.mockStatic(ApplicationUtils.class)) {

      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);

      mockedFactory.when(() -> ObjectStoreFactory.getClient(tenant)).thenReturn(mockObjectStoreClient);

      Single<List<String>> listObjectsSingle = Single.just(objectNames);
      when(mockObjectStoreClient.listObjects(anyString())).thenReturn(listObjectsSingle);

      Maybe<Integer> maybeDelay = Maybe.just(30);
      mockedUtils
          .when(() -> ApplicationUtils.executeBlockingCallable(any()))
          .thenReturn(maybeDelay);

      // Act
      Single<LogSyncDelayResponse> result = metricsService.computeLogSyncDelay(tenant);
      LogSyncDelayResponse response = result.blockingGet();

      // Assert
      Assert.assertNotNull(response);
      Assert.assertEquals(response.getTenant(), tenant.getValue());
      Assert.assertNotNull(response.getAppLogsDelayMinutes());
      Assert.assertTrue(response.getAppLogsDelayMinutes() >= 1);
    }
  }

  @Test
  public void testMetricsService_ComputeLogSyncDelay_WithNoObjectsFound_ReturnsMaxDelay() {
    // Arrange
    Tenant tenant = Tenant.D11_Prod_AWS;
    List<String> emptyList = Collections.emptyList();

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            Mockito.mockStatic(ApplicationConfigUtil.class);
        MockedStatic<ObjectStoreFactory> mockedFactory =
            Mockito.mockStatic(ObjectStoreFactory.class);
        MockedStatic<ApplicationUtils> mockedUtils =
            Mockito.mockStatic(ApplicationUtils.class)) {

      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);

      mockedFactory.when(() -> ObjectStoreFactory.getClient(tenant)).thenReturn(mockObjectStoreClient);

      Single<List<String>> listObjectsSingle = Single.just(emptyList);
      when(mockObjectStoreClient.listObjects(anyString())).thenReturn(listObjectsSingle);

      Maybe<Integer> maybeDelay =
          Maybe.just(ApplicationConstants.MAX_LOGS_SYNC_DELAY_HOURS * 60);
      mockedUtils
          .when(() -> ApplicationUtils.executeBlockingCallable(any()))
          .thenReturn(maybeDelay);

      // Act
      Single<LogSyncDelayResponse> result = metricsService.computeLogSyncDelay(tenant);
      LogSyncDelayResponse response = result.blockingGet();

      // Assert
      Assert.assertNotNull(response);
      Assert.assertEquals(response.getTenant(), tenant.getValue());
      Assert.assertEquals(
          response.getAppLogsDelayMinutes(),
          Integer.valueOf(ApplicationConstants.MAX_LOGS_SYNC_DELAY_HOURS * 60));
    }
  }
}

