<template>
  <div class="calculator-wrapper">
    <h1>LogWise Architecture Sizing Calculator</h1>
    <p class="intro">Determine optimal Kafka and Spark configuration based on your workload characteristics.</p>
    
    <form @submit.prevent="calculate" class="calculator-form">
      <div class="form-section">
        <h2>Workload Characteristics</h2>
        
        <div class="form-group">
          <label for="logRate">
            Total Log Generation Rate (messages/second) <span class="required">*</span>
          </label>
          <input
            id="logRate"
            v-model.number="inputs.logRate"
            type="number"
            min="1"
            step="1"
            required
            placeholder="e.g., 50000"
          />
          <small>Total messages per second across all services</small>
        </div>
        
        <div class="form-group">
          <label for="messageSize">
            Average Message Size (bytes) <span class="required">*</span>
          </label>
          <input
            id="messageSize"
            v-model.number="inputs.messageSize"
            type="number"
            min="1"
            step="1"
            required
            placeholder="e.g., 2048"
          />
          <small>Average size of a single log message in bytes</small>
        </div>
        
        <div class="form-group">
          <label for="peakRatio">
            Peak to Average Ratio <span class="required">*</span>
          </label>
          <input
            id="peakRatio"
            v-model.number="inputs.peakRatio"
            type="number"
            min="1"
            step="0.1"
            required
            placeholder="2.0"
          />
          <small>How much higher can peak traffic be vs average (e.g., 2.0 = 2x)</small>
        </div>
      </div>
      
      <div class="form-section">
        <h2>Environment Settings</h2>
        
        <div class="form-group">
          <label for="environment">
            Environment Type <span class="required">*</span>
          </label>
          <select id="environment" v-model="inputs.environment" required>
            <option value="production">Production</option>
            <option value="staging">Staging</option>
            <option value="development">Development</option>
          </select>
        </div>
        
        <div class="form-group">
          <label for="availability">
            Availability Requirement <span class="required">*</span>
          </label>
          <select id="availability" v-model="inputs.availability" required>
            <option value="high">High (99.9%+)</option>
            <option value="medium">Medium (99.5%)</option>
            <option value="low">Low (99%)</option>
          </select>
        </div>
      </div>
      
      <div class="form-section">
        <div class="advanced-toggle" @click="showAdvanced = !showAdvanced">
          <h2>Advanced Settings</h2>
          <span class="toggle-icon">{{ showAdvanced ? '▼' : '▶' }}</span>
        </div>
        
        <div v-show="showAdvanced" class="advanced-settings">
          <div class="form-group">
            <label for="headroomPercent">
              Headroom Percentage
            </label>
            <input
              id="headroomPercent"
              v-model.number="inputs.headroomPercent"
              type="number"
              min="0"
              max="100"
              step="1"
              placeholder="20"
            />
            <small>Extra capacity buffer (default: 20%)</small>
          </div>
          
          <div class="form-group">
            <label for="preferredExecutorInstanceType">
              Preferred Executor Instance Type (Optional)
            </label>
            <select id="preferredExecutorInstanceType" v-model="inputs.preferredExecutorInstanceType">
              <option value="">Auto (Recommended)</option>
              <option value="m5.medium">m5.medium</option>
              <option value="m5.large">m5.large</option>
              <option value="m5.xlarge">m5.xlarge</option>
              <option value="m5.2xlarge">m5.2xlarge</option>
            </select>
            <small>Override automatic executor instance type selection</small>
          </div>
        </div>
      </div>
      
      <button type="submit" class="calculate-btn">Calculate Recommendations</button>
    </form>
    
    <div v-if="results" class="results-container">
      <h2>Recommended Configuration</h2>
      
      <div class="config-section">
        <h3>Kafka Configuration</h3>
        <div class="config-item">
          <span>Recommended Partitions:</span>
          <strong>{{ results.kafka.recommendedPartitions }}</strong>
        </div>
        <div class="config-item">
          <span>Broker Instance Type:</span>
          <strong>{{ results.kafka.brokerInstanceType }}</strong>
        </div>
        <div class="config-item">
          <span>Number of Brokers:</span>
          <strong>{{ results.kafka.brokerCount }}</strong>
        </div>
        <div class="config-item">
          <span>Replication Factor:</span>
          <strong>{{ results.kafka.replicationFactor }}</strong>
        </div>
        <div class="config-item">
          <span>Retention Hours:</span>
          <strong>{{ results.kafka.retentionHours }}</strong>
        </div>
      </div>
      
      <div class="config-section">
        <h3>Spark Configuration</h3>
        <div class="config-item">
          <span>kafkaMaxRatePerPartition:</span>
          <strong>{{ results.spark.kafkaMaxRatePerPartition }}</strong>
        </div>
        <div class="config-item">
          <span>Executor Instance Type:</span>
          <strong>{{ results.spark.instanceType }}</strong>
        </div>
        <div class="config-item">
          <span>Executor Cores:</span>
          <strong>{{ results.spark.cores }}</strong>
        </div>
        <div class="config-item">
          <span>Executor Memory:</span>
          <strong>{{ results.spark.memory }}</strong>
        </div>
        <div class="config-item">
          <span>Driver Cores:</span>
          <strong>{{ results.spark.driverCores }}</strong>
        </div>
        <div class="config-item">
          <span>Driver Memory:</span>
          <strong>{{ results.spark.driverMemory }}</strong>
        </div>
      </div>
      
      <div class="config-section">
        <h3>Scaling Thresholds</h3>
        <div class="config-item">
          <span>maxLagPerPartition:</span>
          <strong>{{ results.scaling.maxLagPerPartition.toLocaleString() }}</strong>
        </div>
        <div class="config-item">
          <span>maxLagTimeSeconds:</span>
          <strong>{{ results.scaling.maxLagTimeSeconds }}</strong>
        </div>
        <div class="config-item">
          <span>defaultPartitions:</span>
          <strong>{{ results.scaling.defaultPartitions }}</strong>
        </div>
      </div>
      
      <div class="config-section">
        <h3>Performance Metrics</h3>
        <div class="config-item">
          <span>Total Consumption Capacity:</span>
          <strong>{{ results.performance.totalConsumptionCapacity.toLocaleString() }} msgs/sec</strong>
        </div>
        <div class="config-item">
          <span>Headroom:</span>
          <strong>{{ results.performance.headroom }}%</strong>
        </div>
        <div class="config-item">
          <span>Can Handle Peak Load:</span>
          <strong :class="results.performance.canHandlePeak ? 'success' : 'warning'">
            {{ results.performance.canHandlePeak ? '✅ Yes' : '❌ No - Consider Increasing Partitions' }}
          </strong>
        </div>
        <div v-if="results.performance.estimatedLagTimeMinutes > 0" class="config-item">
          <span>Estimated Lag Time:</span>
          <strong class="warning">{{ results.performance.estimatedLagTimeMinutes }} minutes</strong>
        </div>
      </div>
      
      <div class="config-snippet">
        <h3>Configuration Snippet (HOCON)</h3>
        <pre><code>{{ configSnippet }}</code></pre>
        <button @click="copyConfig" class="copy-btn">Copy Configuration</button>
        <span v-if="copied" class="copied-message">✓ Copied!</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { calculateRecommendations } from '../utils/calculator.js'

const inputs = ref({
  logRate: 50000,
  messageSize: 2048,
  peakRatio: 2.0,
  environment: 'production',
  availability: 'high',
  headroomPercent: 20,
  preferredExecutorInstanceType: ''
})

const results = ref(null)
const showAdvanced = ref(false)
const copied = ref(false)

function calculate() {
  const calculationInputs = {
    ...inputs.value,
    preferredExecutorInstanceType: inputs.value.preferredExecutorInstanceType || null
  }
  results.value = calculateRecommendations(calculationInputs)
  copied.value = false
}

const configSnippet = computed(() => {
  if (!results.value) return ''
  
  return `kafka = {
  # Scaling thresholds
  maxLagPerPartition = ${results.value.scaling.maxLagPerPartition}
  defaultPartitions = ${results.value.kafka.recommendedPartitions}
  
  # Broker configuration
  # Recommended: ${results.value.kafka.brokerCount} brokers of type ${results.value.kafka.brokerInstanceType}
  # Replication factor: ${results.value.kafka.replicationFactor}
  # Retention: ${results.value.kafka.retentionHours} hours
}

spark = {
  kafkaMaxRatePerPartition = "${results.value.spark.kafkaMaxRatePerPartition}"
  executorCores = "${results.value.spark.cores}"
  executorMemory = "${results.value.spark.memory}"
  driverCores = "${results.value.spark.driverCores}"
  driverMemory = "${results.value.spark.driverMemory}"
  
  # Recommended executor instance type: ${results.value.spark.instanceType}
}`
})

async function copyConfig() {
  try {
    await navigator.clipboard.writeText(configSnippet.value)
    copied.value = true
    setTimeout(() => {
      copied.value = false
    }, 2000)
  } catch (err) {
    console.error('Failed to copy:', err)
  }
}
</script>

<style scoped>
.calculator-wrapper {
  max-width: 1000px;
  margin: 0 auto;
  padding: 2rem 1rem;
}

h1 {
  font-size: 2.5rem;
  margin-bottom: 0.5rem;
  color: var(--vp-c-brand-1);
}

.intro {
  font-size: 1.1rem;
  color: var(--vp-c-text-2);
  margin-bottom: 2rem;
}

.calculator-form {
  background: var(--vp-c-bg-soft);
  border: 1px solid var(--vp-c-divider);
  border-radius: 8px;
  padding: 2rem;
  margin-bottom: 2rem;
}

.form-section {
  margin-bottom: 2rem;
}

.form-section h2 {
  font-size: 1.5rem;
  margin-bottom: 1rem;
  color: var(--vp-c-text-1);
  border-bottom: 2px solid var(--vp-c-brand-1);
  padding-bottom: 0.5rem;
}

.advanced-toggle {
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  user-select: none;
}

.advanced-toggle h2 {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.toggle-icon {
  color: var(--vp-c-brand-1);
  font-size: 0.9rem;
}

.advanced-settings {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid var(--vp-c-divider);
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: var(--vp-c-text-1);
}

.required {
  color: var(--vp-c-brand-1);
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 0.75rem;
  background: var(--vp-c-bg);
  border: 1px solid var(--vp-c-divider);
  border-radius: 4px;
  color: var(--vp-c-text-1);
  font-size: 1rem;
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: var(--vp-c-brand-1);
  box-shadow: 0 0 0 2px rgba(95, 211, 224, 0.2);
}

.form-group small {
  display: block;
  margin-top: 0.25rem;
  color: var(--vp-c-text-2);
  font-size: 0.875rem;
}

.calculate-btn {
  width: 100%;
  padding: 1rem;
  background: var(--vp-c-brand-1);
  color: var(--vp-c-bg);
  border: none;
  border-radius: 6px;
  font-size: 1.1rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.calculate-btn:hover {
  background: var(--vp-c-brand-2);
}

.results-container {
  background: var(--vp-c-bg-soft);
  border: 1px solid var(--vp-c-divider);
  border-radius: 8px;
  padding: 2rem;
}

.results-container h2 {
  font-size: 2rem;
  margin-bottom: 1.5rem;
  color: var(--vp-c-brand-1);
}

.config-section {
  background: var(--vp-c-bg);
  border: 1px solid var(--vp-c-divider);
  border-radius: 6px;
  padding: 1.5rem;
  margin-bottom: 1.5rem;
}

.config-section h3 {
  font-size: 1.3rem;
  margin-bottom: 1rem;
  color: var(--vp-c-text-1);
  border-bottom: 1px solid var(--vp-c-divider);
  padding-bottom: 0.5rem;
}

.config-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 0;
  border-bottom: 1px solid var(--vp-c-divider);
}

.config-item:last-child {
  border-bottom: none;
}

.config-item span {
  color: var(--vp-c-text-2);
}

.config-item strong {
  color: var(--vp-c-text-1);
  font-weight: 600;
}

.config-item .success {
  color: #4ade80;
}

.config-item .warning {
  color: #fbbf24;
}

.config-snippet {
  margin-top: 2rem;
  background: var(--vp-c-bg);
  border: 1px solid var(--vp-c-divider);
  border-radius: 6px;
  padding: 1.5rem;
}

.config-snippet h3 {
  font-size: 1.3rem;
  margin-bottom: 1rem;
  color: var(--vp-c-text-1);
}

.config-snippet pre {
  background: #1a1a1a;
  border: 1px solid var(--vp-c-divider);
  border-radius: 4px;
  padding: 1rem;
  overflow-x: auto;
  margin-bottom: 1rem;
}

.config-snippet code {
  color: #e5e7eb;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.9rem;
  line-height: 1.6;
}

.copy-btn {
  padding: 0.75rem 1.5rem;
  background: var(--vp-c-brand-1);
  color: var(--vp-c-bg);
  border: none;
  border-radius: 4px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.copy-btn:hover {
  background: var(--vp-c-brand-2);
}

.copied-message {
  margin-left: 1rem;
  color: var(--vp-c-brand-1);
  font-weight: 600;
}

@media (max-width: 768px) {
  .calculator-wrapper {
    padding: 1rem 0.5rem;
  }
  
  .config-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }
  
  .config-item strong {
    align-self: flex-end;
  }
}
</style>

