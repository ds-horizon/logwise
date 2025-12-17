/**
 * LogWise Architecture Sizing Calculator - Calculation Logic
 * Phase 1: Kafka + Spark configuration recommendations
 */

/**
 * Calculate maximum rate per partition based on message size
 * @param {number} messageSizeBytes - Average message size in bytes
 * @returns {number} Maximum messages per second per partition
 */
export function calculateMaxRatePerPartition(messageSizeBytes) {
  if (messageSizeBytes < 1024) return 10000;      // Small: 10k/sec
  if (messageSizeBytes < 5120) return 8000;      // Medium-small: 8k/sec
  if (messageSizeBytes < 10240) return 5000;     // Medium: 5k/sec
  if (messageSizeBytes < 20480) return 3000;     // Medium-large: 3k/sec
  return 2000;                                    // Large: 2k/sec
}

/**
 * Calculate recommended partition count
 * @param {number} logRate - Total log generation rate (messages/second)
 * @param {number} peakRatio - Peak to average ratio
 * @param {number} maxRatePerPartition - Maximum rate per partition
 * @param {number} headroomPercent - Headroom percentage (default: 20)
 * @returns {number} Recommended partition count
 */
export function calculateRecommendedPartitions(logRate, peakRatio, maxRatePerPartition, headroomPercent = 20) {
  const peakRate = logRate * peakRatio;
  const requiredPartitions = Math.ceil(peakRate / maxRatePerPartition);
  const recommendedPartitions = Math.max(3, Math.ceil(requiredPartitions * (1 + headroomPercent / 100)));
  return recommendedPartitions;
}

/**
 * Calculate executor configuration based on max rate per partition
 * @param {number} maxRatePerPartition - Maximum rate per partition
 * @param {string} preferredInstanceType - Optional preferred instance type override
 * @returns {Object} Executor configuration
 */
export function calculateExecutorConfig(maxRatePerPartition, preferredInstanceType = null) {
  let instanceType, cores, memory;
  
  if (preferredInstanceType) {
    // Use preferred instance type and determine cores/memory based on it
    if (preferredInstanceType.includes('xlarge')) {
      instanceType = preferredInstanceType;
      cores = "4";
      memory = "8G";
    } else if (preferredInstanceType.includes('large')) {
      instanceType = preferredInstanceType;
      cores = "2";
      memory = "4G";
    } else {
      instanceType = preferredInstanceType;
      cores = "1";
      memory = "2G";
    }
  } else {
    // Auto-determine based on rate
    if (maxRatePerPartition >= 8000) {
      instanceType = "m5.xlarge";
      cores = "4";
      memory = "8G";
    } else if (maxRatePerPartition >= 5000) {
      instanceType = "m5.large";
      cores = "2";
      memory = "4G";
    } else {
      instanceType = "m5.medium";
      cores = "1";
      memory = "2G";
    }
  }
  
  return {
    instanceType,
    cores,
    memory,
    driverCores: "1",
    driverMemory: "2G"
  };
}

/**
 * Calculate scaling thresholds
 * @param {number} maxRatePerPartition - Maximum rate per partition
 * @returns {Object} Scaling threshold configuration
 */
export function calculateScalingThresholds(maxRatePerPartition) {
  return {
    maxLagPerPartition: maxRatePerPartition * 300,  // 5 minutes at max rate
    maxLagTimeSeconds: 300,  // 5 minutes
    defaultPartitions: 3
  };
}

/**
 * Calculate Kafka broker configuration
 * @param {number} partitions - Number of partitions
 * @param {string} availability - Availability requirement (high/medium/low)
 * @param {string} environment - Environment type (production/staging/development)
 * @returns {Object} Kafka broker configuration
 */
export function calculateKafkaConfig(partitions, availability, environment) {
  let replicationFactor, brokerCount;
  
  if (availability === "high") {
    replicationFactor = 3;
    brokerCount = 3;
  } else if (availability === "medium") {
    replicationFactor = 2;
    brokerCount = 2;
  } else {
    replicationFactor = 1;
    brokerCount = 1;
  }
  
  let brokerInstanceType;
  if (partitions > 20) {
    brokerInstanceType = "m5.2xlarge";
  } else if (partitions > 10) {
    brokerInstanceType = "m5.xlarge";
  } else {
    brokerInstanceType = "m5.large";
  }
  
  let retentionHours;
  if (environment === "production") {
    retentionHours = 24;
  } else if (environment === "staging") {
    retentionHours = 12;
  } else {
    retentionHours = 1;
  }
  
  return {
    brokerInstanceType,
    brokerCount,
    replicationFactor,
    retentionHours
  };
}

/**
 * Main function to calculate all recommendations
 * @param {Object} inputs - User input values
 * @returns {Object} Complete recommendations
 */
export function calculateRecommendations(inputs) {
  const {
    logRate,
    messageSize,
    peakRatio,
    environment,
    availability,
    headroomPercent = 20,
    preferredExecutorInstanceType = null
  } = inputs;
  
  // Calculate max rate per partition
  const maxRatePerPartition = calculateMaxRatePerPartition(messageSize);
  
  // Calculate recommended partitions
  const recommendedPartitions = calculateRecommendedPartitions(
    logRate,
    peakRatio,
    maxRatePerPartition,
    headroomPercent
  );
  
  // Calculate executor configuration
  const executorConfig = calculateExecutorConfig(maxRatePerPartition, preferredExecutorInstanceType);
  
  // Calculate scaling thresholds
  const scalingThresholds = calculateScalingThresholds(maxRatePerPartition);
  
  // Calculate Kafka configuration
  const kafkaConfig = calculateKafkaConfig(recommendedPartitions, availability, environment);
  
  // Calculate performance metrics
  const peakRate = logRate * peakRatio;
  const totalConsumptionCapacity = recommendedPartitions * maxRatePerPartition;
  const canHandlePeak = totalConsumptionCapacity >= peakRate;
  const headroom = ((totalConsumptionCapacity - peakRate) / peakRate) * 100;
  
  // Estimate lag time at current generation rate
  const estimatedLagTime = logRate > 0 
    ? Math.max(0, (peakRate - totalConsumptionCapacity) / logRate * 60) // in minutes
    : 0;
  
  return {
    kafka: {
      recommendedPartitions: recommendedPartitions,
      ...kafkaConfig
    },
    spark: {
      kafkaMaxRatePerPartition: maxRatePerPartition.toString(),
      ...executorConfig
    },
    scaling: scalingThresholds,
    performance: {
      totalConsumptionCapacity: totalConsumptionCapacity,
      headroom: Math.round(headroom * 10) / 10, // Round to 1 decimal
      canHandlePeak: canHandlePeak,
      estimatedLagTimeMinutes: Math.round(estimatedLagTime * 10) / 10
    }
  };
}

