---
title: Architecture Sizing Calculator
description: Interactive calculator to determine optimal Kafka and Spark configuration values based on your workload characteristics
---

# Architecture Sizing Calculator

Use this calculator to determine the optimal configuration values for your LogWise deployment based on your workload characteristics.

<ClientOnly>
  <ArchitectureCalculator />
</ClientOnly>

## How It Works

The calculator takes into account:

- **Log Generation Rate**: Total messages per second across all services
- **Message Size**: Average size of log messages (affects processing rate)
- **Peak Traffic**: Peak to average ratio for handling traffic spikes
- **Environment**: Production, staging, or development requirements
- **Availability**: High, medium, or low availability requirements

## What You'll Get

### Kafka Configuration
- Recommended partition count
- Broker instance type and count
- Replication factor
- Retention hours

### Spark Configuration
- `kafkaMaxRatePerPartition` value (calculated based on message size)
- Executor instance type, cores, and memory
- Driver configuration

### Scaling Thresholds
- `maxLagPerPartition` (message count threshold)
- `maxLagTimeSeconds` (time-based threshold)
- `defaultPartitions`

### Performance Metrics
- Total consumption capacity
- Headroom percentage
- Peak load handling capability

## Understanding the Results

### Max Rate Per Partition
The calculator determines the maximum messages per second that can be processed per partition based on your message size:
- **Small messages** (< 1KB): Up to 10,000 msgs/sec
- **Medium messages** (1-5KB): Up to 8,000 msgs/sec
- **Large messages** (> 10KB): 2,000-5,000 msgs/sec

### Partition Count
The recommended partition count ensures:
- Peak traffic can be handled (with headroom)
- Each partition processes at optimal rate
- Sufficient parallelism for Spark consumption

### Scaling Thresholds
The `maxLagPerPartition` threshold is calculated as:
```
maxLagPerPartition = maxRatePerPartition × 300 seconds (5 minutes)
```

This means the system will scale partitions when lag exceeds 5 minutes of messages at the maximum processing rate.

## Next Steps

1. **Copy the configuration** snippet and add it to your `application-default.conf`
2. **Review the recommendations** and adjust based on your specific requirements
3. **Monitor your system** after deployment and fine-tune if needed
4. **Scale partitions** automatically using the orchestrator service

## Need Help?

- See the [Production Setup Guide](/setup-guides/production-setup) for deployment instructions
- Check [Kafka Component Documentation](/components/kafka) for partition management details
- Review [Spark Component Documentation](/components/spark) for executor configuration

