---
title: Kafka Partition Scaling
---

# Kafka Partition Scaling

The Logwise orchestrator service supports automatic Kafka partition scaling across multiple Kafka implementations (EC2, AWS MSK, and Confluent Cloud).

## Overview

The partition scaling feature automatically increases the number of partitions for Kafka topics when they exceed configured thresholds for:
- **Consumer lag** - When Spark consumers fall behind
- **Partition size** - When partitions grow too large
- **Message count** - When partitions contain too many messages

## Architecture

The scaling system uses:
- **Kafka AdminClient** - Native Kafka API for partition management
- **Spark Checkpoint Offsets** - Reads Spark Structured Streaming checkpoints from S3 to calculate consumer lag
- **Partition Metrics** - Fetches partition-level metrics (offsets, sizes) directly from Kafka

## Supported Kafka Types

### EC2 (Self-Managed Kafka)
- Basic Kafka installations on EC2 or any self-managed infrastructure
- Supports hostname resolution to IP addresses
- Optional SSL/TLS configuration

### AWS MSK (Managed Streaming for Kafka)
- AWS Managed Streaming for Kafka
- IAM authentication
- Automatic bootstrap server discovery via MSK API

### Confluent Cloud/Platform
- Confluent Cloud and Confluent Platform
- API key/secret authentication
- SASL/PLAIN over SSL

## Configuration

### Basic Configuration

Add the following to your tenant configuration in `application-default.conf`:

```hocon
kafka = {
  # Kafka type: "ec2", "msk", or "confluent" (default: "ec2")
  kafkaType = "ec2"
  kafkaBrokersHost = ${KAFKA_BROKERS_HOST}
  kafkaBrokerPort = 9092
  
  # Scaling thresholds
  maxLagPerPartition = 50000        # Consumer lag threshold (default: 50,000)
  maxPartitionSizeBytes = 10000000000  # Partition size threshold (default: 10GB)
  maxMessagesPerPartition = 1000000    # Message count threshold (default: 1,000,000)
  defaultPartitions = 3                 # Rounding factor for partition counts (default: 3)
}
```

### EC2 Kafka Configuration

```hocon
kafka = {
  kafkaType = "ec2"
  kafkaBrokersHost = "kafka.example.com"  # Hostname (will be resolved to IPs)
  kafkaBrokerPort = 9092
  
  # Optional SSL/TLS
  sslTruststoreLocation = ${?SSL_TRUSTSTORE_LOCATION}
  sslTruststorePassword = ${?SSL_TRUSTSTORE_PASSWORD}
  
  # Scaling thresholds
  maxLagPerPartition = 50000
  maxPartitionSizeBytes = 10000000000
  maxMessagesPerPartition = 1000000
  defaultPartitions = 3
}
```

### AWS MSK Configuration

**Option 1: Provide Bootstrap Servers Directly**

```hocon
kafka = {
  kafkaType = "msk"
  kafkaBrokersHost = "b-1.mycluster.abc123.c1.kafka.us-east-1.amazonaws.com:9098,b-2.mycluster.abc123.c1.kafka.us-east-1.amazonaws.com:9098"
  
  # Scaling thresholds
  maxLagPerPartition = 50000
  maxPartitionSizeBytes = 10000000000
  defaultPartitions = 3
}
```

**Option 2: Use MSK Cluster ARN (Auto-Discovery)**

```hocon
kafka = {
  kafkaType = "msk"
  mskClusterArn = "arn:aws:kafka:us-east-1:123456789012:cluster/my-cluster/abcd-1234-5678-90ef-ghij-1234567890ab-1"
  mskRegion = "us-east-1"
  
  # Scaling thresholds
  maxLagPerPartition = 50000
  maxPartitionSizeBytes = 10000000000
  defaultPartitions = 3
}
```

**IAM Permissions Required:**
- `kafka-cluster:Connect`
- `kafka-cluster:DescribeCluster`
- `kafka-cluster:AlterCluster`
- `kafka-cluster:DescribeTopic`
- `kafka-cluster:AlterTopic`

### Confluent Cloud Configuration

```hocon
kafka = {
  kafkaType = "confluent"
  kafkaBrokersHost = "pkc-xxxxx.us-east-1.aws.confluent.cloud:9092"
  confluentApiKey = ${CONFLUENT_API_KEY}
  confluentApiSecret = ${CONFLUENT_API_SECRET}
  
  # Scaling thresholds
  maxLagPerPartition = 50000
  maxPartitionSizeBytes = 10000000000
  maxMessagesPerPartition = 1000000
  defaultPartitions = 3
}
```

## Scaling Thresholds

### maxLagPerPartition
- **Default**: 50,000 messages
- **Description**: Maximum average consumer lag per partition before scaling
- **Calculation**: `(endOffset - checkpointOffset) / partitionCount`
- **When to adjust**: 
  - Lower for faster scaling (more aggressive)
  - Higher for less frequent scaling (more conservative)

### maxPartitionSizeBytes
- **Default**: 10,000,000,000 bytes (10GB)
- **Description**: Maximum partition size before scaling
- **Calculation**: Estimated from message count (1KB per message)
- **When to adjust**: Based on your storage capacity and retention policies

### maxMessagesPerPartition
- **Default**: 1,000,000 messages
- **Description**: Maximum messages per partition before scaling
- **When to adjust**: Based on your message size and processing requirements

### defaultPartitions
- **Default**: 3
- **Description**: Rounding factor for new partition counts
- **Behavior**: New partition counts are rounded up to the nearest multiple of this value
- **Example**: If calculation requires 7 partitions and defaultPartitions=3, result is 9 partitions

## Scaling Algorithm

The system evaluates three factors and scales based on the maximum requirement:

1. **Lag-based scaling**: `requiredPartitions = (avgLagPerPartition / maxLagPerPartition) + 1`
2. **Size-based scaling**: `requiredPartitions = (avgSizePerPartition / maxPartitionSizeBytes) + 1`
3. **Message-based scaling**: `requiredPartitions = (avgMessagesPerPartition / maxMessagesPerPartition) + 1`

Final partition count = `max(lag, size, messages)` rounded up to nearest multiple of `defaultPartitions`.

## API Usage

### Trigger Scaling Manually

```bash
curl -X POST http://orchestrator:8080/kafka/scale-partitions \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Name: ABC" \
  -d '{}'
```

**Response:**
```json
{
  "tenant": "ABC",
  "message": "Kafka partition scaling initiated successfully.",
  "success": true
}
```

## Monitoring

### Logs

The orchestrator service logs scaling decisions:

```
INFO: Found 2 topics needing scaling for tenant: ABC: topic-1(3->6), topic-2(3->9)
INFO: Successfully scaled 2 topics for tenant: ABC in 1234ms. Total partitions added: 9
INFO: Scaled topic topic-1 from 3 to 6 partitions (factors: [lag, size], reason: lag: 75000, size: 15000000000 bytes)
```

### Metrics

Key metrics to monitor:
- Scaling operation duration
- Number of topics scaled
- Total partitions added
- Average lag per partition
- Partition sizes

## Troubleshooting

### Scaling Not Triggering

**Check:**
1. Verify Spark checkpoint offsets are available in S3
2. Check that consumer lag exceeds `maxLagPerPartition`
3. Verify partition metrics are being fetched correctly
4. Check orchestrator logs for errors

**Solution:**
- Lower thresholds if scaling is too conservative
- Verify Spark checkpoint path is correct in Spark configuration

### Scaling Fails

**Common Causes:**
1. **Insufficient permissions** - Check Kafka ACLs and IAM permissions
2. **Network connectivity** - Verify orchestrator can reach Kafka brokers
3. **Authentication errors** - Check credentials and SSL certificates
4. **Topic doesn't exist** - Ensure topics are created before scaling

**Solution:**
- Check orchestrator error logs
- Verify Kafka client configuration
- Test connectivity manually using Kafka CLI tools

### High Lag Despite Scaling

**Possible Causes:**
1. Consumer processing rate is slower than producer rate
2. Scaling thresholds are too high
3. Consumer is stuck or not processing

**Solution:**
- Investigate Spark job performance
- Lower `maxLagPerPartition` threshold
- Check Spark job logs for errors

### Docker-Based Kafka

For Kafka running in Docker:

```hocon
kafka = {
  kafkaType = "ec2"
  kafkaBrokersHost = "kafka"  # Docker service name (NOT "kafka:9092")
  kafkaBrokerPort = 9092
}
```

**Important:** Use the Docker service name, not IP address or hostname with port.

## Best Practices

1. **Start Conservative**: Begin with default thresholds and adjust based on your workload
2. **Monitor Metrics**: Track scaling frequency and partition counts over time
3. **Test First**: Test scaling on non-production environments
4. **Set Alerts**: Configure alerts for scaling failures or excessive scaling
5. **Review Regularly**: Periodically review and adjust thresholds based on actual usage patterns

## Limitations

- **Partition Decrease**: The system only increases partitions, never decreases them
- **Spark Checkpoint Dependency**: Accurate lag calculation requires Spark checkpoint offsets
- **Topic Creation**: Topics must exist before scaling can occur
- **Concurrent Scaling**: Only one scaling operation per tenant at a time

## Migration from Old System

If migrating from the previous Kafka Manager-based system:

1. The old `maxProducerRatePerPartition` field is deprecated but still supported
2. New system uses `maxLagPerPartition`, `maxPartitionSizeBytes`, and `maxMessagesPerPartition`
3. Update configuration to use new thresholds for better accuracy
4. Old producer rate metrics are no longer used

## See Also

- [Kafka Setup Guide](/setup-guides/self-host/kafka-setup)
- [Orchestrator Service Setup](/setup-guides/self-host/orchestrator-service-setup)
- [Spark Setup Guide](/setup-guides/self-host/spark-setup)

