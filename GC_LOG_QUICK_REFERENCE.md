# GC Log Quick Reference Guide

## 🔍 How to Read GC Logs Like a Pro

### Log Entry Format
```
[timestamp][level][component] Message
```

### Key Components to Watch

#### 1. **GC Event Start**
```
[0.392s][info][gc,start] GC(0) Pause Young (Normal) (G1 Evacuation Pause)
```
- **GC(0)**: Event number (sequential)
- **Pause Young**: Type of collection
- **Normal**: Not concurrent or mixed
- **G1 Evacuation Pause**: G1's copy-based collection

#### 2. **Phase Timing** (Most Important for Performance)
```
[0.404s][info][gc,phases] GC(0)   Evacuate Collection Set: 11.7ms
```
- **Evacuate Collection Set**: Main work phase
- **11.7ms**: Time spent copying live objects
- **This is your main pause time contributor**

#### 3. **Heap Region Changes**
```
[0.404s][info][gc,heap] GC(0) Eden regions: 102->0(89)
```
- **102**: Regions before GC
- **0**: Regions after GC (always 0 for Eden)
- **(89)**: Target size for next cycle

#### 4. **Memory Summary**
```
[0.404s][info][gc] GC(0) Pause Young (Normal) (G1 Evacuation Pause) 115M->33M(2048M) 12.486ms
```
- **115M->33M**: Heap usage before->after
- **(2048M)**: Total heap capacity
- **12.486ms**: Total pause time

---

## 🚨 What to Look For

### ✅ Good Signs
- **Pause times < 10ms**: Excellent for most applications
- **Consistent pause times**: Predictable performance
- **High collection efficiency**: >70% memory freed
- **Stable old generation**: No growth over time
- **No full GC events**: Young generation collections only

### ⚠️ Warning Signs
- **Pause times > 50ms**: May impact application performance
- **Increasing pause times**: GC getting less efficient
- **Growing old generation**: Memory leak potential
- **Frequent humongous allocations**: Large object pressure
- **Full GC events**: Heap pressure or tuning issues

### 🔥 Critical Issues
- **Pause times > 100ms**: Serious performance impact
- **OutOfMemoryError**: Heap exhaustion
- **Allocation failure**: Cannot allocate new objects
- **Concurrent mode failure**: G1 cannot keep up with allocation

---

## 📊 Quick Analysis Techniques

### Calculate Allocation Rate
```bash
# Between two GC events
Allocation Rate = (Heap_After_GC2 - Heap_After_GC1) / (Time_GC2 - Time_GC1)

# Example: GC(0) to GC(1)
# (136M - 33M) / (0.486s - 0.392s) = 103M / 0.094s ≈ 1.1GB/sec
```

### Measure GC Efficiency
```bash
# For each GC event
Efficiency = (Heap_Before - Heap_After) / Heap_Before * 100%

# Example: GC(0)
# (115M - 33M) / 115M * 100% = 71% efficiency
```

### Track Pause Time Trends
```bash
# Extract pause times
grep "Pause Young.*ms$" gc.log | awk '{print $(NF)}' | sed 's/ms//'

# Get statistics
grep "Pause Young.*ms$" gc.log | awk '{print $(NF)}' | sed 's/ms//' | sort -n | awk '
{
    sum += $1
    values[NR] = $1
}
END {
    print "Count:", NR
    print "Average:", sum/NR "ms"
    print "Min:", values[1] "ms"
    print "Max:", values[NR] "ms"
    print "p95:", values[int(NR*0.95)] "ms"
}'
```

---

## 🎯 Region Types Explained

### Eden Regions
- **Purpose**: Where new objects are allocated
- **Pattern**: Always goes to 0 after GC
- **Watch**: Size adaptation (number in parentheses)

### Survivor Regions
- **Purpose**: Objects that survived one GC
- **Pattern**: Usually small, stable count
- **Watch**: Excessive growth (indicates promotion issues)

### Old Regions
- **Purpose**: Long-lived objects
- **Pattern**: Gradual growth, then stable
- **Watch**: Continuous growth (memory leak)

### Humongous Regions
- **Purpose**: Objects > 50% of region size
- **Pattern**: Should be minimal
- **Watch**: High count (large object pressure)

---

## 🔧 Common GC Tuning Based on Log Analysis

### High Pause Times
```bash
# Reduce pause time target
-XX:MaxGCPauseMillis=5

# Increase parallelism
-XX:ParallelGCThreads=16

# Smaller regions for more granular collection
-XX:G1HeapRegionSize=512k
```

### High Allocation Rate
```bash
# Larger young generation
-XX:G1NewSizePercent=40

# More aggressive collection
-XX:G1MixedGCCountTarget=4
```

### Memory Pressure
```bash
# Larger heap
-Xmx8G -Xms8G

# Earlier concurrent marking
-XX:G1HeapWastePercent=5
```

### Humongous Object Issues
```bash
# Smaller region size
-XX:G1HeapRegionSize=8m

# Application-level: object pooling, smaller arrays
```

---

## 📈 Benchmark-Specific Patterns

### OrderBook Memory Behavior

**Expected Patterns**:
- High allocation rate (1-2 GB/sec)
- Frequent Eden collections
- Some humongous objects (large arrays)
- Stable old generation (long-lived OrderBook state)

**ART vs TreeSet Differences**:
- **ART**: Lower allocation rate, better pooling
- **TreeSet**: Higher allocation rate, more GC pressure

**Optimization Opportunities**:
- Object pooling for Order instances
- Reuse of temporary objects
- Batch processing to reduce allocation spikes

---

## 🛠️ Useful GC Log Analysis Commands

### Extract Key Metrics
```bash
# Total GC events
grep -c "Pause Young" gc.log

# Pause time statistics
grep "Pause Young.*ms$" gc.log | awk '{print $(NF)}' | sed 's/ms//' | sort -n

# Heap usage over time
grep "Pause Young.*ms$" gc.log | awk '{print $1, $(NF-1)}' | sed 's/\[//g' | sed 's/s\]//g'

# Allocation rate calculation
grep "Pause Young.*ms$" gc.log | awk '{print $1, $(NF-1)}' | sed 's/\[//g' | sed 's/s\]//g' | sed 's/M->.*M(2048M)//'
```

### Monitor GC Health
```bash
# Check for concerning patterns
grep -E "(Full GC|Allocation Failure|OutOfMemoryError)" gc.log

# Monitor old generation growth
grep "Old regions:" gc.log | awk '{print $4}' | sed 's/->/ /' | awk '{print $2}'

# Track humongous object pressure
grep "Humongous regions:" gc.log | awk '{print $4}' | sed 's/->/ /' | awk '{print $1}'
```

This quick reference helps you quickly identify performance issues and optimization opportunities from GC logs!
