package com.logwise.orchestrator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.logwise.orchestrator.config.ApplicationConfig.S3Config;
import com.logwise.orchestrator.config.ApplicationConfig.SparkConfig;
import com.logwise.orchestrator.dto.kafka.SparkCheckpointOffsets;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.util.ApplicationConfigUtil;
import com.logwise.orchestrator.util.AwsClientUtils;
import com.logwise.orchestrator.util.S3Utils;
import io.reactivex.Single;
import java.net.URI;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;

/**
 * Service for reading and parsing Spark Structured Streaming checkpoint offsets from S3. Spark
 * stores checkpoint offsets in JSON format at: checkpoint/sources/0/offsets/
 */
@Slf4j
public class SparkCheckpointService {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Inject
  public SparkCheckpointService() {}

  /**
   * Get Spark checkpoint offsets for a tenant. Parses the latest offset file from Spark checkpoint
   * directory.
   *
   * @param tenant Tenant
   * @return SparkCheckpointOffsets with partition offsets
   */
  public Single<SparkCheckpointOffsets> getSparkCheckpointOffsets(Tenant tenant) {
    try {
      var tenantConfig = ApplicationConfigUtil.getTenantConfig(tenant);
      SparkConfig sparkConfig = tenantConfig.getSpark();
      S3Config s3Config = tenantConfig.getObjectStore().getAws();

      // Build checkpoint path: checkpoint/application
      // checkpointDir is just "checkpoint" (relative to bucket root)
      String checkpointDir = sparkConfig.getCheckPointDir();
      String checkpointPath = checkpointDir + "/application";

      // Create S3 client
      S3AsyncClient s3Client = createS3Client(s3Config);

      return parseCheckpointOffsets(s3Client, s3Config, checkpointPath)
          .doFinally(
              () -> {
                if (s3Client != null) {
                  s3Client.close();
                }
              });
    } catch (Exception e) {
      log.error("Error getting Spark checkpoint offsets for tenant: {}", tenant, e);
      return Single.just(
          SparkCheckpointOffsets.builder()
              .checkpointPath("")
              .offsets(Collections.emptyMap())
              .available(false)
              .lastUpdatedTimestamp(System.currentTimeMillis())
              .build());
    }
  }

  private S3AsyncClient createS3Client(S3Config s3Config) {
    S3AsyncClientBuilder builder = S3AsyncClient.builder();
    builder
        .region(Region.of(s3Config.getRegion()))
        .httpClient(AwsClientUtils.createHttpClient())
        .credentialsProvider(AwsClientUtils.getDefaultCredentialsProvider())
        .overrideConfiguration(
            overrideConfig -> overrideConfig.retryPolicy(AwsClientUtils.createRetryPolicy()));

    if (s3Config.getEndpointOverride() != null) {
      builder.endpointOverride(URI.create(s3Config.getEndpointOverride()));
    }

    return builder.build();
  }

  private Single<SparkCheckpointOffsets> parseCheckpointOffsets(
      S3AsyncClient s3Client, S3Config s3Config, String checkpointPath) {
    // Spark stores offsets at: checkpoint/application/sources/0/offsets/
    String offsetsPath = checkpointPath + "/sources/0/offsets";

    return S3Utils.listObjects(s3Client, s3Config, offsetsPath)
        .flatMap(
            offsetFiles -> {
              if (offsetFiles.isEmpty()) {
                log.warn("No offset files found in checkpoint path: {}", offsetsPath);
                return Single.just(createEmptyCheckpointOffsets(checkpointPath));
              }

              // Get the latest offset file (sorted by name, highest number is latest)
              String latestOffsetFile =
                  offsetFiles.stream()
                      .filter(file -> file.endsWith(".json"))
                      .sorted(Collections.reverseOrder())
                      .findFirst()
                      .orElse(null);

              if (latestOffsetFile == null) {
                log.warn("No JSON offset files found in checkpoint path: {}", offsetsPath);
                return Single.just(createEmptyCheckpointOffsets(checkpointPath));
              }

              // Read and parse the latest offset file
              return S3Utils.readFileContent(s3Client, s3Config, latestOffsetFile)
                  .map(content -> parseOffsetFile(content, checkpointPath))
                  .onErrorReturn(
                      throwable -> {
                        log.error("Error parsing offset file: {}", latestOffsetFile, throwable);
                        return createEmptyCheckpointOffsets(checkpointPath);
                      });
            })
        .onErrorReturn(
            throwable -> {
              log.error(
                  "Error listing offset files in checkpoint path: {}", offsetsPath, throwable);
              return createEmptyCheckpointOffsets(checkpointPath);
            });
  }

  private SparkCheckpointOffsets parseOffsetFile(String content, String checkpointPath) {
    try {
      JsonNode rootNode = objectMapper.readTree(content);
      Map<TopicPartition, Long> offsets = new HashMap<>();

      // Spark checkpoint format: {"batchId": 123, "partitions": {"topic-0": {"0": 100, "1": 200}}}
      if (rootNode.has("partitions")) {
        JsonNode partitionsNode = rootNode.get("partitions");
        partitionsNode
            .fields()
            .forEachRemaining(
                topicEntry -> {
                  String topic = topicEntry.getKey();
                  JsonNode partitionOffsets = topicEntry.getValue();
                  partitionOffsets
                      .fields()
                      .forEachRemaining(
                          partitionEntry -> {
                            int partition = Integer.parseInt(partitionEntry.getKey());
                            long offset = partitionEntry.getValue().asLong();
                            offsets.put(new TopicPartition(topic, partition), offset);
                          });
                });
      }

      log.info("Parsed {} partition offsets from checkpoint", offsets.size());
      return SparkCheckpointOffsets.builder()
          .checkpointPath(checkpointPath)
          .offsets(offsets)
          .available(true)
          .lastUpdatedTimestamp(System.currentTimeMillis())
          .build();
    } catch (Exception e) {
      log.error("Error parsing checkpoint offset file content", e);
      return createEmptyCheckpointOffsets(checkpointPath);
    }
  }

  private SparkCheckpointOffsets createEmptyCheckpointOffsets(String checkpointPath) {
    return SparkCheckpointOffsets.builder()
        .checkpointPath(checkpointPath)
        .offsets(Collections.emptyMap())
        .available(false)
        .lastUpdatedTimestamp(System.currentTimeMillis())
        .build();
  }
}
