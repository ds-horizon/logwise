package com.logwise.orchestrator.tests.unit.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.dto.kafka.SparkCheckpointOffsets;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.service.SparkCheckpointService;
import com.logwise.orchestrator.setup.BaseTest;
import com.logwise.orchestrator.testconfig.ApplicationTestConfig;
import com.logwise.orchestrator.util.ApplicationConfigUtil;
import com.logwise.orchestrator.util.S3Utils;
import io.reactivex.Single;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.common.TopicPartition;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/** Unit tests for SparkCheckpointService. */
public class SparkCheckpointServiceTest extends BaseTest {

  private SparkCheckpointService sparkCheckpointService;
  private S3AsyncClient mockS3Client;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    sparkCheckpointService = new SparkCheckpointService();
    mockS3Client = mock(S3AsyncClient.class);
  }

  @Test
  public void testGetSparkCheckpointOffsets_WithValidCheckpoint_ReturnsOffsets() {
    Tenant tenant = Tenant.ABC;
    String checkpointJson =
        "{\"batchId\":123,\"partitions\":{\"topic-0\":{\"0\":100,\"1\":200},\"topic-1\":{\"0\":50}}}";

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            Mockito.mockStatic(ApplicationConfigUtil.class);
        MockedStatic<S3Utils> mockedS3Utils = Mockito.mockStatic(S3Utils.class)) {
      ApplicationConfig.TenantConfig tenantConfig =
          ApplicationTestConfig.createMockTenantConfig("ABC");
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(tenantConfig);

      // Mock listObjects to return offset files
      mockedS3Utils
          .when(() -> S3Utils.listObjects(any(), any(), anyString()))
          .thenReturn(Single.just(Arrays.asList("checkpoint/application/sources/0/offsets/1.json")));

      // Mock readFileContent to return checkpoint JSON
      mockedS3Utils
          .when(() -> S3Utils.readFileContent(any(), any(), anyString()))
          .thenReturn(Single.just(checkpointJson));

      SparkCheckpointOffsets result =
          sparkCheckpointService.getSparkCheckpointOffsets(tenant).blockingGet();

      assertNotNull(result);
      assertTrue(result.isAvailable());
      Map<TopicPartition, Long> offsets = result.getOffsets();
      assertEquals(offsets.size(), 3);
      assertEquals(offsets.get(new TopicPartition("topic-0", 0)), Long.valueOf(100));
      assertEquals(offsets.get(new TopicPartition("topic-0", 1)), Long.valueOf(200));
      assertEquals(offsets.get(new TopicPartition("topic-1", 0)), Long.valueOf(50));
    }
  }

  @Test
  public void testGetSparkCheckpointOffsets_WithNoOffsetFiles_ReturnsEmptyOffsets() {
    Tenant tenant = Tenant.ABC;

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            Mockito.mockStatic(ApplicationConfigUtil.class);
        MockedStatic<S3Utils> mockedS3Utils = Mockito.mockStatic(S3Utils.class)) {
      ApplicationConfig.TenantConfig tenantConfig =
          ApplicationTestConfig.createMockTenantConfig("ABC");
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(tenantConfig);

      // Mock listObjects to return empty list
      mockedS3Utils
          .when(() -> S3Utils.listObjects(any(), any(), anyString()))
          .thenReturn(Single.just(Collections.emptyList()));

      SparkCheckpointOffsets result =
          sparkCheckpointService.getSparkCheckpointOffsets(tenant).blockingGet();

      assertNotNull(result);
      assertFalse(result.isAvailable());
      assertTrue(result.getOffsets().isEmpty());
    }
  }

  @Test
  public void testGetSparkCheckpointOffsets_WithInvalidJson_ReturnsEmptyOffsets() {
    Tenant tenant = Tenant.ABC;
    String invalidJson = "invalid json";

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            Mockito.mockStatic(ApplicationConfigUtil.class);
        MockedStatic<S3Utils> mockedS3Utils = Mockito.mockStatic(S3Utils.class)) {
      ApplicationConfig.TenantConfig tenantConfig =
          ApplicationTestConfig.createMockTenantConfig("ABC");
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(tenantConfig);

      mockedS3Utils
          .when(() -> S3Utils.listObjects(any(), any(), anyString()))
          .thenReturn(Single.just(Arrays.asList("checkpoint/application/sources/0/offsets/1.json")));

      mockedS3Utils
          .when(() -> S3Utils.readFileContent(any(), any(), anyString()))
          .thenReturn(Single.just(invalidJson));

      SparkCheckpointOffsets result =
          sparkCheckpointService.getSparkCheckpointOffsets(tenant).blockingGet();

      assertNotNull(result);
      assertFalse(result.isAvailable());
      assertTrue(result.getOffsets().isEmpty());
    }
  }

  @Test
  public void testGetSparkCheckpointOffsets_WithS3Error_ReturnsEmptyOffsets() {
    Tenant tenant = Tenant.ABC;

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            Mockito.mockStatic(ApplicationConfigUtil.class);
        MockedStatic<S3Utils> mockedS3Utils = Mockito.mockStatic(S3Utils.class)) {
      ApplicationConfig.TenantConfig tenantConfig =
          ApplicationTestConfig.createMockTenantConfig("ABC");
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(tenantConfig);

      // Mock listObjects to throw error
      mockedS3Utils
          .when(() -> S3Utils.listObjects(any(), any(), anyString()))
          .thenReturn(Single.error(new RuntimeException("S3 error")));

      SparkCheckpointOffsets result =
          sparkCheckpointService.getSparkCheckpointOffsets(tenant).blockingGet();

      assertNotNull(result);
      assertFalse(result.isAvailable());
      assertTrue(result.getOffsets().isEmpty());
    }
  }

  @Test
  public void testGetSparkCheckpointOffsets_WithNoPartitionsNode_ReturnsEmptyOffsets() {
    Tenant tenant = Tenant.ABC;
    String jsonWithoutPartitions = "{\"batchId\":123}";

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            Mockito.mockStatic(ApplicationConfigUtil.class);
        MockedStatic<S3Utils> mockedS3Utils = Mockito.mockStatic(S3Utils.class)) {
      ApplicationConfig.TenantConfig tenantConfig =
          ApplicationTestConfig.createMockTenantConfig("ABC");
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(tenantConfig);

      mockedS3Utils
          .when(() -> S3Utils.listObjects(any(), any(), anyString()))
          .thenReturn(Single.just(Arrays.asList("checkpoint/application/sources/0/offsets/1.json")));

      mockedS3Utils
          .when(() -> S3Utils.readFileContent(any(), any(), anyString()))
          .thenReturn(Single.just(jsonWithoutPartitions));

      SparkCheckpointOffsets result =
          sparkCheckpointService.getSparkCheckpointOffsets(tenant).blockingGet();

      assertNotNull(result);
      assertFalse(result.isAvailable());
      assertTrue(result.getOffsets().isEmpty());
    }
  }
}

