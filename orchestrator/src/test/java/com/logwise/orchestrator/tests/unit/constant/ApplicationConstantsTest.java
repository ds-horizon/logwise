package com.dream11.logcentralorchestrator.tests.unit.constant;

import com.dream11.logcentralorchestrator.constant.ApplicationConstants;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests for ApplicationConstants. */
public class ApplicationConstantsTest {

  @Test
  public void testServiceName() {
    Assert.assertEquals(ApplicationConstants.SERVICE_NAME, "log-central-orchestrator");
  }

  @Test
  public void testApiVersionV1() {
    Assert.assertEquals(ApplicationConstants.API_VERSION_V1, "/api/v1");
  }

  @Test
  public void testTempDir() {
    Assert.assertNotNull(ApplicationConstants.TEMP_DIR);
    Assert.assertEquals(ApplicationConstants.TEMP_DIR, System.getProperty("java.io.tmpdir"));
  }

  @Test
  public void testAwsSdkRetries() {
    Assert.assertEquals(ApplicationConstants.AWS_SDK_RETRIES, 10);
  }

  @Test
  public void testAwsSdkMaxConcurrency() {
    Assert.assertEquals(ApplicationConstants.AWS_SDK_MAX_CONCURRENCY, 1024);
  }

  @Test
  public void testAwsSdkBaseRetryDelaySeconds() {
    Assert.assertEquals(ApplicationConstants.AWS_SDK_BASE_RETRY_DELAY_SECONDS, 3);
  }

  @Test
  public void testAwsSdkMaxBackOffTimeSeconds() {
    Assert.assertEquals(ApplicationConstants.AWS_SDK_MAX_BACK_OFF_TIME_SECONDS, 8);
  }

  @Test
  public void testObjectStoreInjectorName() {
    String tenantName = "test-tenant";
    String result = ApplicationConstants.OBJECT_STORE_INJECTOR_NAME.apply(tenantName);
    Assert.assertEquals(result, "objectStore-test-tenant");
  }

  @Test
  public void testDefaultRetryDelaySeconds() {
    Assert.assertEquals(ApplicationConstants.DEFAULT_RETRY_DELAY_SECONDS, 2);
  }

  @Test
  public void testDefaultMaxRetries() {
    Assert.assertEquals(ApplicationConstants.DEFAULT_MAX_RETRIES, 3);
  }

  @Test
  public void testHeaderTenantName() {
    Assert.assertEquals(ApplicationConstants.HEADER_TENANT_NAME, "X-Tenant-Name");
  }

  @Test
  public void testHeaderTenantNames() {
    Assert.assertEquals(ApplicationConstants.HEADER_TENANT_NAMES, "X-Tenant-Names");
  }

  @Test
  public void testGetServiceDetailsCache() {
    Assert.assertEquals(ApplicationConstants.GET_SERVICE_DETAILS_CACHE, "get-service-details-cache");
  }

  @Test
  public void testKafkaMaxProducerRatePerPartition() {
    Assert.assertEquals(ApplicationConstants.KAFKA_MAX_PRODUCER_RATE_PER_PARTITION, 5500);
  }

  @Test
  public void testKafkaBrokerPort() {
    Assert.assertEquals(ApplicationConstants.KAFKA_BROKER_PORT, 9092);
  }

  @Test
  public void testSparkMetadataFileName() {
    Assert.assertEquals(ApplicationConstants.SPARK_METADATA_FILE_NAME, "_spark_metadata");
  }

  @Test
  public void testSparkMonitorPollIntervalSecs() {
    Assert.assertEquals(ApplicationConstants.SPARK_MONITOR_POLL_INTERVAL_SECS, 15);
  }

  @Test
  public void testSparkMonitorTimeInSecs() {
    Assert.assertEquals(ApplicationConstants.SPARK_MONITOR_TIME_IN_SECS, 60);
  }

  @Test
  public void testSparkMinDownscale() {
    Assert.assertEquals(ApplicationConstants.SPARK_MIN_DOWNSCALE, 2);
  }

  @Test
  public void testSparkMaxDownscale() {
    Assert.assertEquals(ApplicationConstants.SPARK_MAX_DOWNSCALE, 50);
  }

  @Test
  public void testSparkDownscaleProportion() {
    Assert.assertEquals(ApplicationConstants.SPARK_DOWNSCALE_PROPORTION, 0.25);
  }

  @Test
  public void testSparkMinUpscale() {
    Assert.assertEquals(ApplicationConstants.SPARK_MIN_UPSCALE, 1);
  }

  @Test
  public void testSparkMaxUpscale() {
    Assert.assertEquals(ApplicationConstants.SPARK_MAX_UPSCALE, 200);
  }

  @Test
  public void testSparkGcJavaOptions() {
    Assert.assertEquals(
        ApplicationConstants.SPARK_GC_JAVA_OPTIONS,
        "-XX:+UnlockExperimentalVMOptions -XX:+UseG1GC");
  }

  @Test
  public void testMaxLogsSyncDelayHours() {
    Assert.assertEquals(ApplicationConstants.MAX_LOGS_SYNC_DELAY_HOURS, 3);
  }
}

