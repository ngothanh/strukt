# OrderBook Benchmark Suite

Comprehensive JMH benchmarks for `OrderBook.newOrder(OrderCommand)` method testing various real-world scenarios.

## 🧪 Benchmark Scenarios

### 1. **Pure Insert Scenario** (`pureInsertScenario`)
- **Purpose**: Tests insert path, tree growth, idMap usage
- **Behavior**: No matches - ASK orders above best bid, BID orders below best ask
- **Measures**: Insert performance, memory allocation, tree balancing

### 2. **Pure Match Scenario** (`pureMatchScenario`)
- **Purpose**: Tests matching logic, `getBestMatchingOrder()`, `removeOrder()`
- **Behavior**: 100% matched orders against pre-filled opposing orders
- **Measures**: Matching speed, order removal efficiency

### 3. **Partial Match Scenario** (`partialMatchScenario`)
- **Purpose**: Stresses both match and insert in same flow
- **Behavior**: Each order partially matched, remainder inserted
- **Measures**: Combined match+insert performance

### 4. **Random Mix Scenario** (`randomMixScenario`)
- **Purpose**: Simulates realistic L2 order book activity
- **Behavior**: 70% unmatched → inserted, 30% matched → removed
- **Measures**: Real-world performance characteristics

### 5. **Hotspot Match Scenario** (`hotspotMatchScenario`)
- **Purpose**: Tests performance under concentrated price activity
- **Behavior**: All orders target same price level (best bid/ask)
- **Measures**: Bucket contention, matching tight price bands

### 6. **Cold Book Scenario** (`coldBookScenario`)
- **Purpose**: Tests GC behavior and matching speed
- **Behavior**: Insert → immediately matched → removed, book size always ~0
- **Measures**: Memory churn, GC pressure

## 📊 Additional Benchmarks

### **Single Order Latency** (`singleOrderLatency`)
- **Mode**: `SampleTime` for latency distribution (p50, p90, p99, max)
- **Purpose**: Measures individual `newOrder()` call performance

### **Batch Order Throughput** (`batchOrderThroughput`)
- **Mode**: `Throughput` for ops/sec measurement
- **Purpose**: Measures sustained order processing rate

### **Memory Pressure Scenario** (`memoryPressureScenario`)
- **Purpose**: Tests GC behavior under high order volume
- **Behavior**: Cycles through different order types to create memory pressure

## 🚀 Running Benchmarks

### Quick Start
```bash
# Compile the project
mvn clean compile test-compile

# Run all benchmarks
java -cp target/test-classes:target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q) \
  com.submicro.strukt.benchmark.OrderBookBenchmarkRunner

# Run specific scenario
java -cp target/test-classes:target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q) \
  com.submicro.strukt.benchmark.OrderBookBenchmarkRunner pureInsert
```

### Specialized Runs

#### Throughput Only
```java
OrderBookBenchmarkRunner.runThroughputOnly();
```

#### Latency Only
```java
OrderBookBenchmarkRunner.runLatencyOnly();
```

#### Multi-threaded (4 threads)
```java
OrderBookBenchmarkRunner.runMultiThreaded();
```

#### Implementation Comparison (ART vs TreeSet)
```java
OrderBookBenchmarkRunner.runImplementationComparison();
```

#### Capacity Density Tests (25%, 50%, 75%, 100%)
```java
OrderBookBenchmarkRunner.runCapacityDensityTests();
```

## 📈 Benchmark Configuration

### Dataset Sizes
- **10K orders**: Quick validation runs
- **100K orders**: Standard performance testing
- **1M orders**: Stress testing and scalability

### Order Book Implementations
- **ART**: Adaptive Radix Tree implementation
- **TreeSet**: Java TreeMap implementation

### JVM Settings
```
-Xmx4G -Xms4G
-XX:+UseG1GC
-XX:+UnlockExperimentalVMOptions
-XX:+UseStringDeduplication
-XX:MaxGCPauseMillis=200
```

### Measurement Parameters
- **Warmup**: 10 iterations × 10 seconds
- **Measurement**: 10 iterations × 10 seconds
- **Forks**: 1 (separate process for accuracy)
- **Threads**: 1 (configurable)

## 📋 Expected Output Metrics

| Metric | Description |
|--------|-------------|
| **ops/sec** | Raw throughput (operations per second) |
| **p50/p90/p99 latency** | Latency percentiles in microseconds |
| **avg heap used** | Memory pressure indicator |
| **GC count/pause** | Garbage collection stress |
| **avg book size** | Verification of matching logic |

## 🔍 Results Analysis

### Performance Indicators
- **High throughput** in pure insert scenarios indicates efficient tree operations
- **Low latency** in match scenarios shows effective best order tracking
- **Consistent performance** across dataset sizes demonstrates scalability
- **Low GC pressure** indicates memory-efficient implementation

### Comparison Points
- **ART vs TreeSet**: Compare tree implementation efficiency
- **Insert vs Match**: Understand operation cost differences
- **Single vs Batch**: Measure batching benefits
- **Cold vs Hot**: Assess memory locality impact

## 🛠️ Troubleshooting

### Common Issues
1. **OutOfMemoryError**: Increase heap size (`-Xmx8G`)
2. **Slow startup**: Reduce dataset size for quick tests
3. **Inconsistent results**: Ensure stable system load during benchmarking

### Validation
Run the unit tests to verify benchmark correctness:
```bash
mvn test -Dtest=OrderBookBenchmarkTest
```

## 📁 Output Files

Results are saved to `target/benchmark-results/`:
- `orderbook-benchmark-{timestamp}.json`: Complete results
- `gc-{timestamp}.log`: GC analysis logs
- `orderbook-{scenario}-{timestamp}.json`: Scenario-specific results

## 🎯 Benchmark Goals

1. **Identify bottlenecks** in order processing pipeline
2. **Compare implementations** (ART vs TreeSet) objectively
3. **Validate scalability** across different order volumes
4. **Measure memory efficiency** and GC impact
5. **Establish performance baselines** for optimization work
