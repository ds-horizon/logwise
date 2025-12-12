package com.logwise.orchestrator.tests.integration;

import static org.testng.Assert.*;

import com.logwise.orchestrator.client.kafka.Ec2KafkaClient;
import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.dto.kafka.ScalingDecision;
import com.logwise.orchestrator.dto.kafka.TopicPartitionMetrics;
import com.logwise.orchestrator.enums.KafkaType;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.service.KafkaScalingService;
import com.logwise.orchestrator.service.SparkCheckpointService;
import com.logwise.orchestrator.setup.BaseTest;
import com.logwise.orchestrator.testconfig.ApplicationTestConfig;
import io.reactivex.Single;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Integration tests for Kafka partition scaling using Testcontainers. Tests end-to-end scaling
 * scenarios with a real Kafka instance.
 */
public class KafkaScalingIntegrationTest extends BaseTest {

  private static KafkaContainer kafka;
  private AdminClient adminClient;
  private KafkaProducer<String, String> producer;
  private ApplicationConfig.KafkaConfig kafkaConfig;
  private Ec2KafkaClient ec2KafkaClient;

  @BeforeClass
  public static void setUpKafka() {
    kafka =
        new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"))
            .withReuse(true);
    kafka.start();
  }

  @BeforeClass
  public void setUp() throws Exception {
    super.setUp();
    kafkaConfig = new ApplicationConfig.KafkaConfig();
    kafkaConfig.setKafkaType(KafkaType.EC2);
    kafkaConfig.setKafkaBrokersHost(kafka.getBootstrapServers().split(":")[0]);
    kafkaConfig.setKafkaBrokerPort(
        Integer.parseInt(kafka.getBootstrapServers().split(":")[1]));
    kafkaConfig.setMaxLagPerPartition(50_000L);
    kafkaConfig.setDefaultPartitions(3);

    // Create AdminClient
    Map<String, Object> adminConfig = new HashMap<>();
    adminConfig.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
    adminClient = AdminClient.create(adminConfig);

    // Create Producer
    Map<String, Object> producerConfig = new HashMap<>();
    producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
    producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    producer = new KafkaProducer<>(producerConfig);

    ec2KafkaClient = new Ec2KafkaClient(kafkaConfig);
  }

  @Test
  public void testListTopics_WithCreatedTopic_ReturnsTopic() throws Exception {
    String topicName = "test-topic-" + System.currentTimeMillis();
    NewTopic newTopic = new NewTopic(topicName, 3, (short) 1);
    adminClient.createTopics(Collections.singletonList(newTopic)).all().get(5, TimeUnit.SECONDS);

    Single<Set<String>> topics = ec2KafkaClient.listTopics("test-topic-.*");
    Set<String> result = topics.blockingGet();

    assertNotNull(result);
    assertTrue(result.contains(topicName));
  }

  @Test
  public void testGetPartitionMetrics_WithTopic_ReturnsMetrics() throws Exception {
    String topicName = "metrics-topic-" + System.currentTimeMillis();
    NewTopic newTopic = new NewTopic(topicName, 3, (short) 1);
    adminClient.createTopics(Collections.singletonList(newTopic)).all().get(5, TimeUnit.SECONDS);

    // Produce some messages
    for (int i = 0; i < 100; i++) {
      producer.send(new ProducerRecord<>(topicName, "key-" + i, "value-" + i));
    }
    producer.flush();

    Single<Map<String, TopicPartitionMetrics>> metrics =
        ec2KafkaClient.getPartitionMetrics(Collections.singletonList(topicName));
    Map<String, TopicPartitionMetrics> result = metrics.blockingGet();

    assertNotNull(result);
    assertTrue(result.containsKey(topicName));
    TopicPartitionMetrics topicMetrics = result.get(topicName);
    assertEquals(topicMetrics.getPartitionCount(), 3);
    assertTrue(topicMetrics.getTotalMessages() > 0);
  }

  @Test
  public void testIncreasePartitions_WithTopic_IncreasesPartitions() throws Exception {
    String topicName = "scale-topic-" + System.currentTimeMillis();
    NewTopic newTopic = new NewTopic(topicName, 3, (short) 1);
    adminClient.createTopics(Collections.singletonList(newTopic)).all().get(5, TimeUnit.SECONDS);

    Map<String, Integer> scalingMap = Map.of(topicName, 6);
    ec2KafkaClient.increasePartitions(scalingMap).blockingAwait();

    // Verify partitions increased
    var topicDescription =
        adminClient.describeTopics(Collections.singletonList(topicName)).all().get();
    assertEquals(topicDescription.get(topicName).partitions().size(), 6);
  }

  @Test
  public void testKafkaScalingService_WithHighLag_IdentifiesScalingNeeded() {
    KafkaScalingService scalingService = new KafkaScalingService();

    String topic = "lag-topic";
    TopicPartitionMetrics metrics =
        TopicPartitionMetrics.builder()
            .topic(topic)
            .partitionCount(3)
            .totalMessages(3_000_000L)
            .avgMessagesPerPartition(1_000_000L)
            .estimatedSizeBytes(3_000_000_000L)
            .partitionOffsets(Map.of(0, 1_000_000L, 1, 1_000_000L, 2, 1_000_000L))
            .build();

    Map<String, TopicPartitionMetrics> metricsMap = Map.of(topic, metrics);
    Map<org.apache.kafka.common.TopicPartition, Long> lagMap = new HashMap<>();
    lagMap.put(new org.apache.kafka.common.TopicPartition(topic, 0), 100_000L);
    lagMap.put(new org.apache.kafka.common.TopicPartition(topic, 1), 100_000L);
    lagMap.put(new org.apache.kafka.common.TopicPartition(topic, 2), 100_000L);

    List<ScalingDecision> decisions =
        scalingService.identifyTopicsNeedingScaling(metricsMap, lagMap, kafkaConfig);

    assertNotNull(decisions);
    assertEquals(decisions.size(), 1);
    ScalingDecision decision = decisions.get(0);
    assertEquals(decision.getTopic(), topic);
    assertTrue(decision.getNewPartitions() > decision.getCurrentPartitions());
  }

  @Test
  public void testEndToEndScaling_WithHighLag_ScalesPartitions() throws Exception {
    String topicName = "e2e-topic-" + System.currentTimeMillis();
    NewTopic newTopic = new NewTopic(topicName, 3, (short) 1);
    adminClient.createTopics(Collections.singletonList(newTopic)).all().get(5, TimeUnit.SECONDS);

    // Produce messages to create lag
    for (int i = 0; i < 200_000; i++) {
      producer.send(new ProducerRecord<>(topicName, "key-" + i, "value-" + i));
    }
    producer.flush();

    // Get metrics
    Single<Map<String, TopicPartitionMetrics>> metrics =
        ec2KafkaClient.getPartitionMetrics(Collections.singletonList(topicName));
    Map<String, TopicPartitionMetrics> metricsMap = metrics.blockingGet();

    // Simulate high lag
    Map<org.apache.kafka.common.TopicPartition, Long> lagMap = new HashMap<>();
    for (int i = 0; i < 3; i++) {
      lagMap.put(new org.apache.kafka.common.TopicPartition(topicName, i), 100_000L);
    }

    // Identify scaling needs
    KafkaScalingService scalingService = new KafkaScalingService();
    List<ScalingDecision> decisions =
        scalingService.identifyTopicsNeedingScaling(metricsMap, lagMap, kafkaConfig);

    if (!decisions.isEmpty()) {
      ScalingDecision decision = decisions.get(0);
      Map<String, Integer> scalingMap =
          Map.of(decision.getTopic(), decision.getNewPartitions());

      // Scale partitions
      ec2KafkaClient.increasePartitions(scalingMap).blockingAwait();

      // Verify scaling
      var topicDescription =
          adminClient.describeTopics(Collections.singletonList(topicName)).all().get();
      assertEquals(
          topicDescription.get(topicName).partitions().size(), decision.getNewPartitions());
    }
  }

  @AfterClass
  public static void tearDownKafka() {
    if (kafka != null) {
      kafka.stop();
    }
  }

  @AfterClass
  public void tearDown() {
    if (adminClient != null) {
      adminClient.close();
    }
    if (producer != null) {
      producer.close();
    }
    if (ec2KafkaClient != null) {
      ec2KafkaClient.close();
    }
  }
}

