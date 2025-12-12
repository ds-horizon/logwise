---
title: Kafka Partition Scaling Migration Guide
---

# Kafka Partition Scaling Migration Guide

This guide helps you migrate from the old Kafka Manager-based partition scaling system to the new multi-Kafka partition scaling system.

## Overview

The new system provides:
- Support for EC2, MSK, and Confluent Kafka
- Metrics-based scaling using partition-level metrics instead of producer rates
- Spark checkpoint integration for accurate lag calculation
- Native Kafka AdminClient (no dependency on Kafka Manager)

## Migration Steps

### Phase 1: Deploy with Feature Flag (Week 1)

1. **Deploy the new code** alongside existing code
2. **Feature flag is enabled by default** (`enablePartitionScaling = true`)
3. **Disable for all tenants initially** to test in isolation:

```hocon
kafka = {
  enablePartitionScaling = false  # Disable new system initially
  # ... existing configuration ...
}
```

4. **Monitor** - Verify no impact on existing functionality

### Phase 2: Enable for One Test Tenant (Week 2)

1. **Select a test tenant** with low traffic
2. **Enable feature flag** for that tenant:

```hocon
tenants = [
  {
    name = "TEST_TENANT"
    kafka = {
      enablePartitionScaling = true  # Enable new system
      kafkaType = "ec2"  # or "msk" or "confluent"
      # ... new configuration ...
    }
  }
]
```

3. **Monitor for 2-3 days**:
   - Check scaling decisions in logs
   - Verify partitions are scaled correctly
   - Monitor Spark job performance
   - Check for any errors

4. **Compare behavior** with old system if still running

### Phase 3: Gradual Rollout (Week 3-4)

1. **Enable for more tenants** one at a time
2. **Monitor each tenant** for 1-2 days before enabling the next
3. **Prioritize tenants**:
   - Start with low-traffic tenants
   - Then medium-traffic tenants
   - Finally high-traffic tenants

### Phase 4: Full Migration (Week 5)

1. **Enable for all remaining tenants**
2. **Remove old Kafka Manager dependency** (if applicable)
3. **Update all configurations** to use new thresholds
4. **Remove deprecated fields** from configuration

### Phase 5: Cleanup (Week 6)

1. **Remove feature flag** (or keep for future use)
2. **Remove old code** if no longer needed
3. **Update documentation** to reflect new system only

## Configuration Migration

### Old Configuration

```hocon
kafka = {
  kafkaBrokersHost = "kafka.example.com"
  kafkaBrokerPort = 9092
  maxProducerRatePerPartition = 6000  # Old threshold
}
```

### New Configuration

```hocon
kafka = {
  enablePartitionScaling = true  # Enable new system
  kafkaType = "ec2"  # Required: "ec2", "msk", or "confluent"
  kafkaBrokersHost = "kafka.example.com"
  kafkaBrokerPort = 9092
  
  # New scaling thresholds (replace old maxProducerRatePerPartition)
  maxLagPerPartition = 50000
  maxPartitionSizeBytes = 10000000000
  maxMessagesPerPartition = 1000000
  defaultPartitions = 3
}
```

## Threshold Conversion

The old `maxProducerRatePerPartition` cannot be directly converted to new thresholds. Use these guidelines:

### If Old Threshold Was Conservative (High Value)
- Start with **default thresholds**
- Monitor for 1-2 weeks
- Adjust based on actual scaling behavior

### If Old Threshold Was Aggressive (Low Value)
- Lower `maxLagPerPartition` to 30,000-40,000
- Lower `maxMessagesPerPartition` to 500,000-750,000
- Monitor and adjust

### Recommended Starting Point

For most use cases, start with defaults and adjust:

```hocon
kafka = {
  maxLagPerPartition = 50000        # Default
  maxPartitionSizeBytes = 10000000000  # Default (10GB)
  maxMessagesPerPartition = 1000000    # Default
  defaultPartitions = 3                 # Default
}
```

## Rollback Plan

If issues occur during migration:

1. **Disable feature flag** for affected tenant:

```hocon
kafka = {
  enablePartitionScaling = false  # Disable new system
}
```

2. **Restart orchestrator service**
3. **Investigate issues** using logs and metrics
4. **Fix configuration** or code issues
5. **Re-enable** after fixes are verified

## Verification Checklist

Before enabling for a tenant, verify:

- [ ] Kafka connectivity works
- [ ] Spark checkpoint path is correct
- [ ] Scaling thresholds are appropriate
- [ ] Monitoring and alerting are configured
- [ ] Rollback plan is ready

After enabling, monitor:

- [ ] Scaling decisions are logged correctly
- [ ] Partitions are increased as expected
- [ ] No errors in orchestrator logs
- [ ] Spark jobs continue processing normally
- [ ] Consumer lag decreases after scaling

## Common Issues and Solutions

### Issue: Scaling Not Triggering

**Symptoms:**
- No scaling decisions in logs
- High lag but no partition increases

**Solutions:**
1. Check feature flag is enabled
2. Verify Spark checkpoint offsets are available
3. Check threshold values (may be too high)
4. Verify Kafka client can connect

### Issue: Too Frequent Scaling

**Symptoms:**
- Partitions scaled very frequently
- Rapid partition count increases

**Solutions:**
1. Increase thresholds (especially `maxLagPerPartition`)
2. Review scaling decisions in logs
3. Check if consumer is actually processing

### Issue: Scaling Fails

**Symptoms:**
- Error messages in logs
- Partitions not increased despite decision

**Solutions:**
1. Check Kafka permissions/ACLs
2. Verify network connectivity
3. Check authentication credentials
4. Review error messages in logs

## Support

For issues during migration:
1. Check logs: `orchestrator-service.log`
2. Review configuration
3. Test connectivity manually
4. Contact support with:
   - Tenant name
   - Error messages
   - Configuration (sanitized)
   - Log excerpts

## Timeline Summary

| Week | Phase | Action |
|------|-------|--------|
| 1 | Deploy | Deploy code, disable feature flag |
| 2 | Test | Enable for one test tenant |
| 3-4 | Rollout | Enable for tenants gradually |
| 5 | Full Migration | Enable for all tenants |
| 6 | Cleanup | Remove old code/dependencies |

**Total Migration Time**: 6 weeks (can be accelerated based on confidence)

## See Also

- [Kafka Partition Scaling Guide](/setup-guides/self-host/kafka-partition-scaling)
- [Kafka Setup Guide](/setup-guides/self-host/kafka-setup)

