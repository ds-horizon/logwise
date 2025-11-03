# Vector Setup for LogWise

Vector configuration guide for the LogWise log-central system. Vector receives logs from OpenTelemetry collectors, transforms them, and forwards them to Kafka for downstream processing.

## Architecture

```
OpenTelemetry Collector → Vector → Kafka → Log Storage/Processing
```

Vector handles:
- **Ingestion**: Receives OTLP logs via gRPC (4317) and HTTP (4318)
- **Transformation**: Validates, normalizes, and enriches log data
- **Routing**: Publishes logs to Kafka topics organized by type, environment, and service

## Prerequisites

Install required dependencies:

```bash
# Vector (choose your platform)
# See: https://vector.dev/docs/setup/installation/

# Protocol Buffers compiler
# See: https://protobuf.dev/installation/
```

## Setup

### 1. Compile Protobuf Schema

```bash
protoc --include_imports --descriptor_set_out=logwise-vector.desc logwise-vector.proto
```

### 2. Configure Vector Pipeline

Create `vector.toml` with the following components:

#### Source: OpenTelemetry Logs

```toml
[sources.otlp_logs]
type = "opentelemetry"

[sources.otlp_logs.grpc]
address = "0.0.0.0:4317"

[sources.otlp_logs.http]
address = "0.0.0.0:4318"
```

[Component Reference](https://vector.dev/docs/reference/configuration/sources/opentelemetry/)

#### Transform: Validate and Enrich

```toml
[transforms.enrich_otlp]
type = "remap"
inputs = ["otlp_logs.logs"]
drop_on_abort = true
source = '''
# Validate required fields
if !is_string(.resources.service_name) { abort }
if !is_string(.resources.environment) { abort }
if !is_string(.resources.type) { abort }

# Extract and normalize resource attributes
.service_name = to_string(.resources.service_name) ?? "unknown"
.environment_name = to_string(.resources.environment) ?? "unknown"
.type = to_string(.resources.type) ?? "unknown"

# Sanitize for Kafka topic names (alphanumeric, dot, dash, underscore only)
.service_name = replace(.service_name, r'[^a-zA-Z0-9._-]', "_")
.environment_name = replace(.environment_name, r'[^a-zA-Z0-9._-]', "_")
.type = replace(.type, r'[^a-zA-Z0-9._-]', "_")

# Extract log attributes
.message = to_string(.attributes.message) ?? "no_message"
.log_level = to_string(.attributes.level) ?? "unknown"
.timestamp = to_string(.attributes.time) ?? "unknown"
'''
```

[Component Reference](https://vector.dev/docs/reference/configuration/transforms/remap/)

#### Sink: Kafka Output

```toml
[sinks.app-logs-kafka]
type = "kafka"
inputs = ["enrich_otlp"]
bootstrap_servers = "kafka:29092"
topic = "logs.{{.type}}.{{.environment_name}}_{{.service_name}}"
compression = "zstd"

encoding.codec = "protobuf"
encoding.protobuf.message_type = "logwise.vector.logs.VectorLogs"
encoding.protobuf.desc_file = "/etc/vector/logwise-vector.desc"

[sinks.app-logs-kafka.librdkafka_options]
"linger.ms" = "100"           # Batch window for throughput
"batch.size" = "2500000"      # Max batch size (bytes)
```

**Topic naming**: Logs route to `logs.<type>_<env>_<service>` (e.g., `logs.application_prod_api-server`)

[Component Reference](https://vector.dev/docs/reference/configuration/sinks/kafka/)

### 3. Deploy Configuration

```bash
sudo mkdir -p /etc/vector
sudo cp vector.toml /etc/vector/vector.toml
sudo cp logwise-vector.desc /etc/vector/logwise-vector.desc
```

## Running Vector

Start Vector as a service or run manually:

```bash
# Production: Run as systemd service (Linux)
sudo systemctl start vector
sudo systemctl enable vector

# Development: Run manually
vector --config /etc/vector/vector.toml
```

### Verify Operation

Check Vector is processing logs:

```bash
# Check service status
sudo systemctl status vector

# View live logs
sudo journalctl -u vector -f

# Check Kafka topics are being created/populated
kafka-topics --bootstrap-server kafka:29092 --list | grep "^logs\."
```

## Troubleshooting

### Protobuf Codec Not Recognized

**Error:**
```
ERROR vector::sinks::kafka: Unknown encoding codec: protobuf
```

**Solution:** Vector v0.45.0+ required for Protobuf support.

```bash
# Check version
vector --version

# Upgrade if needed
# Docker: timberio/vector:0.45.0-distroless-libc or later
# Homebrew: brew upgrade vector
# Apt: sudo apt update && sudo apt upgrade vector

# Workaround for older versions: use JSON codec
# encoding.codec = "json"
```

### Kafka Connection Failures

**Error:**
```
ERROR vector::sinks::kafka: Failed to produce message: BrokerTransportFailure
```

**Common cause:** Kafka's `advertised.listeners` misconfigured.

```bash
# Kafka must advertise its reachable hostname/IP, not localhost
# ❌ WRONG: advertised.listeners=PLAINTEXT://localhost:9092
# ✅ CORRECT: advertised.listeners=PLAINTEXT://kafka.example.com:9092

# Verify Kafka's advertised address
kcat -b kafka:29092 -L

# Look for broker metadata:
# broker 1 at localhost:9092         ← Problem: clients outside container can't connect
# broker 1 at kafka.example.com:9092 ← Good: resolvable hostname
```

**Other checks:**
- Verify `bootstrap_servers` in `vector.toml` matches Kafka's network address
- Ensure firewall allows connections to Kafka ports
- Test connectivity: `telnet kafka 29092`

## Reference Files

- **[`vector.toml`](vector.toml)** - Complete Vector pipeline configuration
- **[`logwise-vector.proto`](logwise-vector.proto)** - Protobuf schema defining log message structure