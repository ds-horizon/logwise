# Kubernetes OpenTelemetry Collector Configuration Guide

This document explains a complete Kubernetes deployment of OpenTelemetry Collector for log shipping, including RBAC permissions, DaemonSet deployment, and Kubernetes metadata enrichment.

## Overview

This configuration deploys an OpenTelemetry Collector as a DaemonSet on every Kubernetes node to collect container logs, enrich them with Kubernetes metadata, and ship them to a remote log backend. The setup includes proper RBAC permissions, persistent storage, and production-ready configurations.

## Architecture


┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Application   │    │  Kubernetes      │    │  OTEL Collector │    │   Log Backend   │
│   Containers    │    │  Container Logs  │    │   (DaemonSet)   │    │   (Remote)      │
└─────────────────┘    └──────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │                       │
         │                       │                       │                       │
         ▼                       ▼                       ▼                       ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ 1. App writes   │    │ 2. K8s writes    │    │ 3. Collector    │    │ 4. Processed    │
│    JSON logs    │    │    to files      │    │    processes    │    │    logs sent    │
│    to stdout    │    │ /var/log/containers/│ │    & enriches   │    │    via OTLP     │
└─────────────────┘    └──────────────────┘    └─────────────────┘    └─────────────────┘


┌─────────────────────────────────────────────────────────────────────────────────────┐
│                           OTEL COLLECTOR PROCESSING PIPELINE                        │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                     │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐           │
│  │  RECEIVERS  │    │ PROCESSORS  │    │  EXPORTERS  │    │   OUTPUTS   │           │
│  │             │    │             │    │             │    │             │           │
│  │ filelog/    │───▶│ k8sattributes│──▶│ otlphttp    │───▶│ Remote API  │           │
│  │ containers  │    │             │    │             │    │             │           │
│  │             │    │ resource    │    │ file        │───▶│ Local File  │           │
│  │             │    │             │    │             │    │             │           │
│  │             │    │ memory_     │    │ debug       │───▶│ Debug Logs  │           │
│  │             │    │ limiter     │    │             │    │             │           │
│  │             │    │             │    │             │    │             │           │
│  │             │    │ batch       │    │             │    │             │           │
│  └─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘           │
└─────────────────────────────────────────────────────────────────────────────────────┘

## Key Features

- **Container Log Collection**: Reads logs from `/var/log/containers/*.log`
- **JSON Log Parsing**: Automatically parses structured JSON logs from applications
- **Kubernetes Metadata Enrichment**: Adds pod, namespace, node, and label information
- **Multiple Export Destinations**: File output, OTLP HTTP, and debug logging
- **Production Ready**: Memory limits, health checks, and persistent storage

## Component Breakdown

### 1. Namespace Creation

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: observability
```

**Purpose**: Creates a dedicated namespace for all observability components, providing:
- **Isolation**: Separates observability tools from application workloads
- **Resource Management**: Easier to apply resource quotas and policies
- **Security**: Can apply specific security policies to this namespace

### 2. Secret Management

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: logs-api-key
  namespace: observability
type: Opaque
stringData:
  MY_LOGS_API_KEY: ${env:MY_LOGS_API_KEY}
```

**Purpose**: Securely stores the API key for log backend authentication
- **Security**: Prevents API keys from being stored in plain text
- **Environment Variable**: Injected into the collector pod as `MY_LOGS_API_KEY`
- **Template Variable**: Uses `${env:MY_LOGS_API_KEY}` for dynamic configuration
- **Rotation**: Easy to update without changing the deployment

**Note**: The `${env:MY_LOGS_API_KEY}` template variable allows the secret to be populated from environment variables during deployment, making it more flexible for different environments.

### 3. RBAC Configuration

#### ServiceAccount
```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: otel-collector
  namespace: observability
```

#### ClusterRole
```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: otel-collector-k8s-read
rules:
  - apiGroups: [""]
    resources: ["pods", "namespaces", "nodes"]
    verbs: ["get", "list", "watch"]
```

#### ClusterRoleBinding
```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: otel-collector-k8s-read-binding
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: otel-collector-k8s-read
subjects:
  - kind: ServiceAccount
    name: otel-collector
    namespace: observability
```

**Purpose**: Grants the collector permission to read Kubernetes metadata
- **Principle of Least Privilege**: Only grants read access to necessary resources
- **Metadata Enrichment**: Allows collector to add pod, namespace, and node information to logs
- **Cluster-wide Access**: Required because pods can be on any node

### 4. Collector Configuration

#### Extensions
```yaml
extensions:
  file_storage:
    directory: /var/lib/otelcol/storage
  health_check:
    endpoint: 0.0.0.0:13133
  pprof:
    endpoint: 127.0.0.1:1777
  bearertokenauth:
    token: ${env:MY_LOGS_API_KEY}
```

**Key Features**:
- **File Storage**: Persistent storage for checkpoints and queues
- **Health Check**: Kubernetes liveness/readiness probe endpoint
- **Profiling**: Local pprof endpoint for performance analysis
- **Authentication**: Bearer token from environment variable

#### Receivers
```yaml
receivers:
  filelog/containers:
    include: [ /var/log/containers/*.log ]
    exclude: [ /var/log/containers/*_kube-system_*.log ]
    start_at: end
    storage: file_storage
    operators:
      - type: regex_parser
        regex: '^((?P<time>\S+)\s+(?P<stream>stdout|stderr)\s+(?P<logtag>[^ ]*)\s+(?P<log>.*))$'
        timestamp:
          parse_from: attributes.time
          layout: "%Y-%m-%dT%H:%M:%S.%9fZ"
      - type: json_parser
        parse_from: attributes.log
        parse_to: body
      - type: move
        from: body."log.level"
        to: attributes.app.level
      - type: move
        from: body.message
        to: body
      - type: severity_parser
        parse_from: attributes.stream
        mapping:
          stderr: ERROR
          stdout: INFO
```

**Log Collection Strategy**:
- **Container Logs**: Reads from `/var/log/containers/*.log` (Kubernetes symlinks)
- **System Log Exclusion**: Skips kube-system logs to reduce noise
- **Start at End**: Only processes new logs (not historical)
- **Regex Parser**: Extracts timestamp, stream, and log content from container format
- **JSON Parser**: Parses structured JSON logs from applications
- **Field Extraction**: Moves JSON fields to appropriate OpenTelemetry attributes
- **Severity Mapping**: Maps stderr/stdout to ERROR/INFO levels

#### Processors

##### Kubernetes Attributes Processor
```yaml
k8sattributes:
  auth_type: serviceAccount
  passthrough: false
  extract:
    metadata:
      - k8s.namespace.name
      - k8s.pod.name
      - k8s.container.name
      - k8s.node.name
    labels:
      - key: app.kubernetes.io/name
        from: pod
        tag_name: service.name
      - key: app.kubernetes.io/version
        from: pod
        tag_name: service.version
  filter:
    pass:
      - key: k8s.namespace.name
        values: [ "observability", "default" ]
```

**Metadata Enrichment**:
- **Pod Information**: Adds pod name, namespace, container name
- **Node Information**: Adds node name for infrastructure context
- **Label Promotion**: Converts Kubernetes labels to OpenTelemetry semantic conventions
- **Filtering**: Only processes logs from specified namespaces

##### Resource Processor
```yaml
resource:
  attributes:
    - { key: service.name, action: upsert, value: ${env:SERVICE_NAME} }
    - { key: environment, action: upsert, value: ${env:ENVIRONMENT} }
```

**Fallback Attributes**: Provides default values when Kubernetes metadata is unavailable

##### Memory Limiter
```yaml
memory_limiter:
  limit_mib: 1500
  spike_limit_mib: 500
  check_interval: 5s
```

**Memory Protection**: Prevents OOM kills by limiting memory usage

##### Batch Processor
```yaml
batch:
  timeout: 10s 
  send_batch_size: 1024
  send_batch_max_size: 2048
```

**Performance Optimization**: Batches logs for efficient transmission

#### Exporters

##### OTLP HTTP Exporter
```yaml
otlphttp:
  endpoint: "https://log-endpoint.example.com"
  compression: gzip
  auth:
    authenticator: bearertokenauth
  sending_queue: 
    enabled: false
```

**Configuration**:
- **Endpoint**: Remote log backend URL (update with your actual endpoint)
- **Compression**: Reduces network bandwidth with gzip
- **Authentication**: Uses bearer token from secret
- **Queue**: Disabled for immediate delivery (can be enabled for reliability)

##### File Exporter
```yaml
file:
  path: /tmp/otel-logs-output.jsonl
  format: json
```

**Local Storage**: Saves logs to local file for debugging and backup

##### Debug Exporter
```yaml
debug:
  verbosity: detailed
```

**Debugging**: Provides detailed logging information for troubleshooting

#### Service Pipeline
```yaml
service:
  extensions: [file_storage, health_check, pprof, bearertokenauth]
  pipelines:
    logs:
      receivers: [filelog/containers]
      processors: [k8sattributes, resource, memory_limiter, batch]
      exporters: [otlphttp, file, debug]
```

**Pipeline Flow**:
1. **Receivers**: Collect logs from container files
2. **Processors**: Enrich with K8s metadata, add resource attributes, manage memory, and batch
3. **Exporters**: Send to multiple destinations (remote, file, debug)

### JSON Log Parsing Capabilities

This configuration includes advanced JSON log parsing for structured application logs:

#### Supported Log Formats
- **Container Runtime Format**: `timestamp stream logtag json_content`
- **Structured JSON**: Application logs in JSON format
- **Field Extraction**: Automatic extraction of log levels, messages, and custom fields

#### Parsing Pipeline
1. **Regex Parser**: Extracts timestamp, stream (stdout/stderr), and log content
2. **JSON Parser**: Parses the log content as structured JSON
3. **Field Mapping**: Moves JSON fields to OpenTelemetry attributes
4. **Severity Mapping**: Maps stderr/stdout to ERROR/INFO levels

#### Example Log Processing
**Input** (container log):
```
2024-01-01T10:00:00.123456789Z stdout F {"log.level":"INFO","message":"User login successful","user_id":"12345"}
```

**Output** (OpenTelemetry log):
```json
{
  "timestamp": "2024-01-01T10:00:00.123456789Z",
  "severity": "INFO",
  "body": "User login successful",
  "attributes": {
    "app.level": "INFO",
    "k8s.namespace.name": "default",
    "k8s.pod.name": "my-app-123",
    "k8s.container.name": "my-app"
  }
}
```

### 5. DaemonSet Deployment

#### Volume Mounts
```yaml
volumes:
  - name: varlog
    hostPath: { path: /var/log, type: Directory }
  - name: varlibdockercontainers
    hostPath: { path: /var/lib/docker/containers, type: DirectoryOrCreate }
  - name: varlibkubeletpods
    hostPath: { path: /var/lib/kubelet/pods, type: Directory }
  - name: otel-storage
    hostPath: { path: /var/lib/otelcol, type: DirectoryOrCreate }
```

**Host Access Requirements**:
- **Container Logs**: Access to `/var/log/containers/` (Kubernetes symlinks)
- **Docker Metadata**: Access to `/var/lib/docker/containers/` for container info
- **Kubelet Metadata**: Access to `/var/lib/kubelet/pods/` for pod info
- **Persistent Storage**: Local storage for checkpoints and queues

#### Environment Variables
```yaml
env:
  - name: MY_LOGS_API_KEY
    valueFrom:
      secretKeyRef:
        name: logs-api-key
        key: MY_LOGS_API_KEY
  - { name: SERVICE_NAME, value: "k8s-workload" }
  - { name: ENVIRONMENT, value: "production" }
```

**Configuration Injection**:
- **API Key**: Securely injected from Kubernetes secret
- **Service Identity**: Default service name and environment

#### Health Checks
```yaml
livenessProbe:
  httpGet: { path: /healthz, port: healthz }
  initialDelaySeconds: 10
  periodSeconds: 10
readinessProbe:
  httpGet: { path: /healthz, port: healthz }
  initialDelaySeconds: 5
  periodSeconds: 5
```

**Kubernetes Integration**:
- **Liveness Probe**: Restarts pod if collector becomes unhealthy
- **Readiness Probe**: Ensures pod is ready before receiving traffic

#### Resource Constraints
```yaml
resources:
  requests: { cpu: 100m, memory: 256Mi }
  limits: { cpu: 1000m, memory: 1Gi }
```

**Resource Management**:
- **Requests**: Guaranteed resources for scheduling
- **Limits**: Maximum resources to prevent resource exhaustion

#### Security Context
```yaml
securityContext:
  runAsUser: 0
  runAsGroup: 0
  readOnlyRootFilesystem: true
  allowPrivilegeEscalation: false
  capabilities: { drop: ["ALL"] }
```

**Security Considerations**:
- **Root User**: Required to read host log files
- **Read-only Root**: Prevents writes to container filesystem
- **No Privilege Escalation**: Prevents privilege escalation attacks
- **No Capabilities**: Drops all Linux capabilities

## Deployment Process

### Step 1: Create Namespace and Secret
```bash
# Create namespace
kubectl apply -f - <<EOF
apiVersion: v1
kind: Namespace
metadata:
  name: observability
EOF

# Create secret with your API key
kubectl create secret generic logs-api-key \
  --from-literal=MY_LOGS_API_KEY="your-actual-api-key" \
  --namespace=observability
```

### Step 2: Deploy RBAC Resources
```bash
kubectl apply -f kubernets.txt
```

### Step 3: Verify Deployment
```bash
# Check DaemonSet status
kubectl get daemonset -n observability

# Check pod status
kubectl get pods -n observability

# Check logs
kubectl logs -n observability -l app.kubernetes.io/name=otel-collector
```

## Monitoring and Troubleshooting

### Health Check Endpoint
```bash
# Check collector health
kubectl port-forward -n observability svc/otel-collector 13133:13133
curl http://localhost:13133/healthz
```

### Debug Logs
```bash
# View collector logs
kubectl logs -n observability -l app.kubernetes.io/name=otel-collector -f

# Check specific pod logs
kubectl logs -n observability <pod-name> -f
```

### Resource Usage
```bash
# Check resource usage
kubectl top pods -n observability

# Check node resource usage
kubectl top nodes
```

## Deployment and Testing

### Quick Start

1. **Set environment variable**:
   ```bash
   export MY_LOGS_API_KEY="your-secret-api-key-here"
   ```

2. **Deploy to cluster**:
   ```bash
   kubectl apply -f otel-testing.yaml
   ```

3. **Verify deployment**:
   ```bash
   kubectl get pods -n observability
   kubectl get daemonset -n observability
   ```

4. **Check collector logs**:
   ```bash
   kubectl logs -n observability -l app.kubernetes.io/name=otel-collector
   ```

### Testing Log Collection

#### 1. Generate Test Logs
Create a test pod that generates structured JSON logs:
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: log-generator
  namespace: default
spec:
  containers:
  - name: log-generator
    image: busybox
    command: 
    - /bin/sh
    - -c
    - |
      while true; do
        echo '{"log.level":"INFO","message":"Test log message","timestamp":"'$(date -Iseconds)'","user_id":"12345"}'
        sleep 5
      done
```

#### 2. Verify Log Processing
Check if logs are being processed:
```bash
# Check collector logs for processing activity
kubectl logs -n observability -l app.kubernetes.io/name=otel-collector | grep "filelog/containers"

# Check file output (if enabled)
kubectl exec -n observability <collector-pod> -- cat /tmp/otel-logs-output.jsonl
```

#### 3. Test Remote Endpoint
Update the endpoint URL and test connectivity:
```bash
# Test endpoint connectivity
kubectl exec -n observability <collector-pod> -- curl -v https://your-endpoint.com/v1/logs
```

#### Development Environment
```yaml
env:
  - { name: SERVICE_NAME, value: "k8s-workload-dev" }
  - { name: ENVIRONMENT, value: "development" }
```

#### Production Environment
```yaml
env:
  - { name: SERVICE_NAME, value: "k8s-workload-prod" }
  - { name: ENVIRONMENT, value: "production" }
```

### Log Filtering
```yaml
# Include only specific namespaces
k8sattributes:
  filter:
    pass:
      - key: k8s.namespace.name
        values: [ "production", "staging" ]

# Exclude system namespaces
filelog/containers:
  exclude: [ 
    /var/log/containers/*_kube-system_*.log,
    /var/log/containers/*_kube-public_*.log
  ]
```

### Performance Tuning
```yaml
# Increase batch size for high-volume environments
batch:
  send_batch_size: 2048
  send_batch_max_size: 4096

# Increase queue size for burst handling
sending_queue:
  queue_size: 2000
  num_consumers: 20
```

## Troubleshooting

### Common Issues

#### 1. Permission Denied Errors
```bash
# Check if the collector has proper permissions
kubectl describe pod -n observability <pod-name>

# Check security context
kubectl get pod -n observability <pod-name> -o yaml | grep -A 10 securityContext
```

#### 2. No Logs Being Collected
```bash
# Check if logs are being read
kubectl logs -n observability <pod-name> | grep "filelog/containers"

# Check file permissions
kubectl exec -n observability <pod-name> -- ls -la /var/log/containers/
```

#### 3. Export Failures
```bash
# Check exporter status
kubectl logs -n observability <pod-name> | grep "exporter"

# Check network connectivity
kubectl exec -n observability <pod-name> -- curl -v <endpoint-url>
```

#### 4. JSON Parsing Issues
```bash
# Check if JSON parsing is working
kubectl logs -n observability <pod-name> | grep "json_parser"

# Verify log format matches expected structure
kubectl exec -n observability <pod-name> -- head -5 /var/log/containers/*.log
```

#### 5. Memory Issues
```bash
# Check memory usage
kubectl top pods -n observability

# Check for OOM kills
kubectl describe pod -n observability <pod-name> | grep -i "oom"
```

### Debug Mode

Enable detailed logging:
```yaml
exporters:
  debug:
    verbosity: detailed
```

### Log Format Validation

Verify your application logs match the expected JSON format:
```json
{
  "log.level": "INFO",
  "message": "Your log message",
  "timestamp": "2024-01-01T10:00:00.123Z",
  "user_id": "12345"
}
```

## Security Considerations

### Network Security
- **TLS**: Use HTTPS endpoints for log transmission
- **Network Policies**: Restrict network access to necessary endpoints
- **Service Mesh**: Consider using Istio/Linkerd for additional security

### Access Control
- **RBAC**: Minimal permissions as shown in the configuration
- **Pod Security Standards**: Enforce pod security policies
- **Secret Management**: Use external secret management systems

### Data Privacy
- **Log Sanitization**: Implement log sanitization processors
- **PII Detection**: Add processors to detect and mask PII
- **Retention Policies**: Implement log retention policies

## Best Practices

### Resource Management
1. **Monitor Resource Usage**: Set up alerts for high CPU/memory usage
2. **Scale Appropriately**: Adjust resource limits based on log volume
3. **Node Affinity**: Consider node affinity for collector pods

### Reliability
1. **Persistent Storage**: Ensure persistent storage is available
2. **Backup Strategy**: Implement backup for collector state
3. **Disaster Recovery**: Plan for collector failure scenarios

### Performance
1. **Batch Optimization**: Tune batch sizes for your log volume
2. **Queue Management**: Monitor queue sizes and adjust accordingly
3. **Network Optimization**: Use compression and efficient protocols

## Related Documentation

- [OpenTelemetry Collector Documentation](https://opentelemetry.io/docs/collector/)
- [Kubernetes RBAC Documentation](https://kubernetes.io/docs/reference/access-authn-authz/rbac/)
- [DaemonSet Documentation](https://kubernetes.io/docs/concepts/workloads/controllers/daemonset/)
