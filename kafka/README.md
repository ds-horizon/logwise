# Kafka Setup and Configuration Guide

This guide explains how to set up and configure Kafka for the Logwise log-central system. In our architecture, Vector automatically collects logs from various sources (via OTEL collectors) and sends them to Kafka. Vector automatically creates topics in Kafka using our standardized naming convention.

## Overview

Kafka serves as the message streaming platform in our log-central architecture, handling high-throughput log data (15+ Gbps). The complete system includes:
- **OTEL Collectors**: Collect logs from applications
- **Vector**: Routes logs from OTEL collectors to Kafka
- **Kafka**: Message streaming platform (this component)
- **Spark**: Processes logs from Kafka
- **Grafana**: Visualizes log data

This guide focuses on setting up Kafka independently. For the complete end-to-end Docker setup, see the main project documentation.

## Important: Zookeeper-Based Kafka Required

We require **Zookeeper-based Kafka** (not KRaft mode) because we use Kafka Manager APIs to retrieve metrics. Kafka Manager requires Zookeeper-based Kafka clusters.

**Do not use Kafka in KRaft mode** - ensure you're running Kafka with Zookeeper.

## Table of Contents

- [Kafka Setup](#kafka-setup)
- [Required Configuration: Auto Topic Creation](#required-configuration-auto-topic-creation)
- [Required Configuration: Base Partitions](#required-configuration-base-partitions)
- [Topic Naming Convention](#topic-naming-convention)
- [Message Retention Configuration](#message-retention-configuration)
- [Zookeeper Configuration](#zookeeper-configuration)
- [Verification and Testing](#verification-and-testing)

## Kafka Setup

### Prerequisites

- Java 8 or higher
- Zookeeper (required - not KRaft mode)
- Sufficient disk space for log storage
- Network access between Vector and Kafka

### Installation Methods

#### Option 1: Docker (Recommended for Testing/MVP)

```bash
# Start Zookeeper
docker run -d \
  --name zookeeper \
  -p 2181:2181 \
  -e ZOOKEEPER_CLIENT_PORT=2181 \
  -e ZOOKEEPER_TICK_TIME=2000 \
  confluentinc/cp-zookeeper:latest

# Start Kafka (Zookeeper-based)
docker run -d \
  --name kafka \
  -p 9092:9092 \
  --link zookeeper:zookeeper \
  -e KAFKA_BROKER_ID=1 \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  -e KAFKA_AUTO_CREATE_TOPICS_ENABLE=true \
  -e KAFKA_NUM_PARTITIONS=3 \
  -e KAFKA_DEFAULT_REPLICATION_FACTOR=3 \
  -e KAFKA_LOG_RETENTION_HOURS=1 \
  confluentinc/cp-kafka:latest
```

#### Option 2: Manual Installation

1. Download Kafka (ensure it's a Zookeeper-compatible version)
2. Extract and configure
3. Start Zookeeper first, then Kafka

### Basic Kafka Configuration

Create or edit `config/server.properties`:

```properties
# Basic broker configuration
broker.id=1
listeners=PLAINTEXT://0.0.0.0:9092
log.dirs=/var/kafka-logs

# Zookeeper connection (required - NOT KRaft)
zookeeper.connect=localhost:2181
zookeeper.session.timeout.ms=18000
```

## Required Configuration: Auto Topic Creation

**Auto topic creation MUST be enabled** because Vector automatically creates Kafka topics when it sends logs. Without this setting, Vector will fail to send logs when it encounters a topic that doesn't exist.

### Configuration

Add this to your `config/server.properties`:

```properties
# REQUIRED: Enable auto topic creation for Vector's automatic topic creation
auto.create.topics.enable=true
```

### Why This Is Required

Vector automatically discovers log sources and creates corresponding Kafka topics on-demand. Vector creates topics using the naming convention `{type}_{env}_{service_name}` as it encounters new log sources. For this to work, Kafka must have auto topic creation enabled.

### Behavior

When Vector sends logs to a topic that doesn't exist:
1. Kafka automatically creates the topic
2. The topic is created with default settings (partitions, replication, retention)
3. Vector continues sending logs without manual intervention

## Required Configuration: Base Partitions

We configure a **base number of partitions** that all auto-created topics will use. This ensures consistent performance and allows for predictable scaling.

### Configuration

Set the base partition count in `config/server.properties`:

```properties
# Base number of partitions for topics Vector creates automatically
num.partitions=3
```

### Base Partition Count: 3

We use **3 partitions** as the base for all topics that Vector creates. This provides:
- Basic parallelism for producers and consumers
- Reasonable load distribution
- Flexibility for future scaling (partitions can be increased, never decreased)

### When Vector Creates Topics

When Vector automatically creates a topic:
- It uses the configured `num.partitions` value (default: 3)
- All partitions are created immediately
- The topic is ready to receive logs immediately

### Partition Management by Orchestrator Service

Partition management is handled automatically by our orchestrator service. The orchestrator:
- Monitors message/sec rates for each topic
- Automatically increases partitions when throughput requires it
- Makes decisions based on defined rate thresholds
- Handles partition scaling dynamically based on topic performance

You don't need to manually adjust partitions - the orchestrator service manages this automatically.

### Partition Guidelines (Reference)

The orchestrator uses these guidelines for partition management:

| Message Rate | Partitions |
|--------------|-----------|
| < 30K msg/sec | 3 (base) |
| 30K - 100K msg/sec | 6-9 |
| 100K - 500K msg/sec | 12-18 |
| > 500K msg/sec | 24+ |

Topics start with the base partition count (3) and are automatically scaled by the orchestrator service as needed.

## Topic Naming Convention

Vector automatically creates topics using the following naming convention:

```
{type}_{env}_{service_name}
```

### Format Breakdown

- **`type`**: The type of data/message (e.g., `logs`, `events`, `metrics`, `traces`)
- **`env`**: Environment identifier (e.g., `prod`, `staging`, `dev`, `test`)
- **`service_name`**: Name of the service or application generating the logs

### Examples

When Vector sends logs, it creates topics like:
- `logs_prod_nginx` - nginx logs from production
- `logs_prod_apache` - apache logs from production
- `logs_staging_user-service` - user service logs from staging
- `events_prod_audit-service` - audit events from production
- `metrics_dev_payment-service` - metrics from payment service in dev

### Naming Rules

Vector follows these rules when creating topic names:
1. All lowercase letters
2. Components separated by underscores (`_`)
3. No special characters or spaces
4. Service names are concise but descriptive

## Message Retention Configuration

### Default Retention: 1 Hour

By default, topics that Vector creates have a **1 hour retention period** (3,600,000 milliseconds). This means:

#### What Retention Means

1. Messages are kept for exactly 1 hour from when they are written
2. After 1 hour, messages are automatically deleted by Kafka
3. Deletion happens regardless of whether messages have been consumed
 intended4. Old log segments exceeding retention are removed

#### Why 1 Hour Default?

For high-throughput log streams (15+ Gbps):
- **Storage Efficiency**: Prevents unbounded growth of log storage
- **Cost Management**: Limits disk space usage
- **Performance**: Keeps log files manageable for faster operations
- **Typical Use Case**: Logs are usually processed within 1 hour in our pipeline

### Configuration

Set default retention for Vector-created topics in `config/server.properties`:

```properties
# Default retention for topics Vector creates automatically
log.retention.hours=1
# OR using milliseconds
log.retention.ms=3600000

# Retention check interval (how often Kafka checks for expired logs)
log.retention.check.interval.ms=300000  # 5 minutes
```

### When to Increase Retention

Increase retention beyond 1 hour when:

1. **Delayed Processing**
   - Batch processing that runs less frequently than hourly
   - Weekend/holiday processing schedules
   - Recovery scenarios requiring historical data

2. **Compliance and Audit Requirements**
   - Regulatory retention requirements (e.g., 7 days, 30 days, 1 year)
   - Legal retention requirements
   - Forensic analysis needs

3. **Analytics and Debugging**
   - Historical trend analysis over days/weeks
   - Debugging intermittent issues
   - Performance analysis requiring historical data

4. **Consumer Failures**
   - Consumer downtime exceeding 1 hour
   - Need to reprocess messages from the past
   - Disaster recovery scenarios

5. **High-Value Logs**
   - Critical business events
   - Security events
   - Financial transaction logs

### How to Increase Retention

Update retention on topics that Vector has already created:

```bash
# Update retention to 24 hours
kafka-configs.sh --alter \
  --topic logs_prod_nginx \
  --bootstrap-server localhost:9092 \
  --add-config retention.ms=86400000

# Update retention to 7 days
kafka-configs.sh --alter \
  --topic logs_prod_nginx \
  --bootstrap-server localhost:9092 \
  --add-config retention.ms=604800000
```

Note: Partition management is handled automatically by the orchestrator service - you only need to update retention manually if needed.

### Common Retention Values

| Duration | Milliseconds | Use Case |
|----------|--------------|----------|
| 1 hour | 3,600,000 | Default for high-volume log streams |
| 6 hours | 21,600,000 | Short batch processing windows |
| 24 hours | 86,400,000 | Daily processing windows |
| 7 days | 604,800,000 | Weekly analytics or compliance |
| 30 days | 2,592,000,000 | Monthly retention or compliance |
| 90 days | 7,776,000勺,000 | Quarterly retention |
| 1 year | 31,536,000,000 | Long-term compliance |

### Storage Impact

**Formula:**
```
Estimated Storage = (Message Size × Messages/Second × Retention Seconds)
```

**Example:**
```
Message Size: 1 KB
Message Rate: 10,000/second
Retention: 24 hours (86,400 seconds)

Storage = 1 KB × 10,000 × 86,400 = 864 GB
```

> **Warning**: Increasing retention significantly impacts disk storage requirements. With 15+ Gbps throughput, monitor disk usage carefully when increasing retention.

## Zookeeper Configuration

Since we require Zookeeper-based Kafka (for Kafka Manager), ensure Zookeeper is properly configured.

### Zookeeper Setup

#### Using Docker

```bash
docker run -d \
  --name zookeeper \
  -p 2181:2181 \
  -e ZOOKEEPER_CLIENT_PORT=2181 \
  -e ZOOKEEPER_TICK_TIME=2000 \
  confluentinc/cp-zookeeper:latest
```

#### Manual Setup

1. Download Zookeeper
2. Create configuration file `conf/zoo.cfg`:

```properties
dataDir=/var/lib/zookeeper
clientPort=2181
initLimit=5
syncLimit=2
server.1=localhost:2888:3888
```

3. Start Zookeeper:
```bash
bin/zkServer.sh start
```

### Kafka-Zookeeper Connection

In Kafka's `config/server.properties`:

```properties
# Zookeeper connection (REQUIRED - do not use KRaft)
zookeeper.connect=localhost:2181

# Zookeeper session timeout
zookeeper.session.timeout.ms=18000

# Zookeeper connection timeout
zookeeper.connection.timeout.ms=18000
```

### Multi-Broker Setup (Production)

For production with multiple Zookeeper instances:

```properties
# Connect to Zookeeper ensemble
zookeeper.connect=zk1:2181,zk2:2181,zk3:2181
zookeeper.session.timeout.ms=18000
```

## Complete Server Configuration Example

Here's a complete `config/server.properties` example for our log-central setup:

```properties
# Broker identity
broker.id=1
listeners=PLAINTEXT://0.0.0.0:9092
log.dirs=/var/kafka-logs

# REQUIRED: Auto topic creation for Vector
auto.create.topics.enable=true

# REQUIRED: Base partitions for Vector-created topics
num.partitions=3

# Replication and high availability
default.replication.factor=3
min.insync.replicas=2
unclean.leader.election.enable=false

# Default retention for Vector-created topics
log.retention.hours=1
log.retention.check.interval.ms=300000
log.segment.bytes=1073741824

# REQUIRED: Zookeeper connection (for Kafka Manager)
zookeeper.connect=localhost:2181
zookeeper.session.timeout.ms=18000
zookeeper.connection.timeout.ms=18000

# Performance tuning
num.network.threads=8
num.io.threads=16
socket.send.buffer.bytes=102400
socket.receive.buffer.bytes=102400
socket.request.max.bytes=104857600

# Log flushing
log.flush.interval.messages=10000
log.flush.interval.ms=1000
num.replica.fetchers=4
```

## Verification and Testing

### Verify Kafka is Running

```bash
# Check if Kafka is running
kafka-broker-api-versions.sh --bootstrap-server localhost:9092

# List topics
kafka-topics.sh --list --bootstrap-server localhost:9092
```

### Verify Auto Topic Creation

```bash
# Try to produce to a non-existent topic (should auto-create)
echo "test message" | kafka-console-producer.sh \
  --topic test_auto_creation \
  --bootstrap-server localhost:9092

# Verify topic was created automatically
kafka-topics.sh --describe \
  --topic test_auto_creation \
  --bootstrap-server localhost:9092

# Should show: Topic: test_auto_creation, PartitionCount: 3, ...
```

### Verify Zookeeper Connection

```bash
# Check Zookeeper connection
zkCli.sh -server localhost:2181 ls /brokers/ids

# Should list broker IDs if Kafka is connected
```

### Test Vector Topic Creation

Once Vector is configured and sending logs:
1. Start Vector with your log sources
2. Vector will automatically create topics as it discovers log sources
3. Verify topics are created with correct naming:

```bash
# List all topics Vector created
kafka-topics.sh --list --bootstrap-server localhost:9092

# Describe a Vector-created topic
kafka-topics.sh --describe \
  --topic logs_prod_nginx \
  --bootstrap-server localhost:9092
```

## Configuration Summary

### Required Settings

These configurations are **required** for Vector's automatic topic creation:

```properties
# MUST be enabled for Vector
auto.create.topics.enable=true

# Base partitions for Vector-created topics
num.partitions=3

# Zookeeper connection (required for Kafka Manager)
zookeeper.connect=localhost:2181
```

### Recommended Defaults

```properties
# Default replication factor
default.replication.factor=3

# Minimum in-sync replicas
min.insync.replicas=2

# Default retention (1 hour)
log.retention.hours=1
```

## Integration with Vector

Once Kafka is configured with the required settings:

1. **Vector automatically creates topics** when it sends logs
2. **Topics follow naming convention**: `{type}_{env}_{service_name}`
3. **Topics start with base partitions**: 3 partitions by default
4. **Topics use default retention**: 1 hour by default
5. **You can update topics** after Vector creates them if needed

No manual topic creation required - Vector handles everything automatically.

## Monitoring with Kafka Manager

Since we use Kafka Manager APIs for metrics, ensure:
- Kafka is running in Zookeeper mode (not KRaft)
- Zookeeper is accessible
- Kafka Manager can connect to both Kafka and Zookeeper

## Troubleshooting

### Vector Cannot Send Logs

**Issue**: Vector fails to send logs with topic creation errors

**Solution**: Ensure `auto.create.topics.enable=true` is set in Kafka configuration

### Topics Created with Wrong Partition Count

**Issue**: Vector-created topics don't have the expected partition count

**Solution**: Check `num.partitions` setting in `config/server.properties` and restart Kafka

### Kafka Manager Cannot Connect

**Issue**: Kafka Manager cannot retrieve metrics

**Solution**: 
- Verify Kafka is in Zookeeper mode (not KRaft)
- Check Zookeeper connection string in Kafka configuration
- Ensure Zookeeper is running and accessible

## Related Documentation

- [Main Logwise README](../README.md) - Complete end-to-end system documentation
- [Vector Configuration](../vector/README.md) - Vector setup and configuration
- [Spark Configuration](../spark/README.md) - Spark processing setup
- [Grafana Configuration](../grafana/README.md) - Grafana dashboard setup

## References

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Kafka Configuration Reference](https://kafka.apache.org/documentation/#configuration)
- [Vector Kafka Sink Documentation](https://vector.dev/docs/reference/configuration/sinks/kafka/)
