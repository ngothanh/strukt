package com.submicro.strukt.benchmark;

import com.submicro.strukt.art.order.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * OrderBook GC Profiling Benchmark Suite
 * 
 * Dedicated to measuring memory allocation patterns and GC behavior.
 * Provides detailed analysis of:
 * - Bytes per operation (B/op)
 * - Young/Old GC pause distributions
 * - Allocation spikes and patterns
 * - Live set size tracking
 * - Memory efficiency comparison
 * 
 * Configuration: Extended measurement for GC analysis
 * Mode: Throughput with GC profiling enabled
 * Focus: Memory behavior under realistic trading loads
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(value = 3, jvmArgs = {
    "-Xmx8G", "-Xms8G", 
    "-XX:+UseG1GC", 
    "-XX:+AlwaysPreTouch",
    "-XX:MaxGCPauseMillis=200",
    "-Xlog:gc*:gc-profile.log",
    "-XX:+UnlockExperimentalVMOptions",
    "-XX:G1NewSizePercent=20",
    "-XX:G1MaxNewSizePercent=30"
})
@Threads(1)
public class OrderBookGCProfileBenchmark {

    /**
     * GC Profile: Pure Insert - Memory allocation patterns for insert operations
     * Focus: Tree growth allocation, bucket creation overhead
     */
    @Benchmark
    public long gcProfile01_PureInsert(OrderBookBenchmarkSuite state, Blackhole bh) {
        OrderBook book = state.createOrderBook();
        long totalProcessed = 0;
        
        // Track allocation patterns during pure insert
        for (OrderCommand order : state.pureInsertOrders) {
            book.newOrder(order);
            totalProcessed++;
            
            // Force allocation tracking
            bh.consume(order.orderId);
            bh.consume(order.price);
            bh.consume(order.size);
            bh.consume(order.action);
            bh.consume(order.timestamp);
        }
        
        // Force GC to measure live set
        System.gc();
        bh.consume(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        
        return totalProcessed;
    }

    /**
     * GC Profile: Pure Match - Memory allocation patterns for match operations
     * Focus: Object churn during matching, temporary allocation patterns
     */
    @Benchmark
    public long gcProfile02_PureMatch(OrderBookBenchmarkSuite state, Blackhole bh) {
        OrderBook book = state.createOrderBook();
        
        // Pre-fill with deep liquidity (measure pre-fill allocation)
        for (OrderCommand order : state.preFillDeepLiquidityBids) {
            book.newOrder(order);
            bh.consume(order);
        }
        for (OrderCommand order : state.preFillDeepLiquidityAsks) {
            book.newOrder(order);
            bh.consume(order);
        }
        
        long totalProcessed = 0;
        
        // Track allocation patterns during matching
        for (OrderCommand order : state.pureMatchOrders) {
            book.newOrder(order);
            totalProcessed++;
            
            // Comprehensive allocation tracking
            bh.consume(order.orderId);
            bh.consume(order.price);
            bh.consume(order.size);
            bh.consume(order.action);
            bh.consume(order.timestamp);
            bh.consume(order.uid);
        }
        
        // Measure live set after matching
        System.gc();
        bh.consume(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        
        return totalProcessed;
    }

    /**
     * GC Profile: Partial Match - Memory allocation patterns for partial matching
     * Focus: Combined allocation patterns, object lifecycle during partial fills
     */
    @Benchmark
    public long gcProfile03_PartialMatch(OrderBookBenchmarkSuite state, Blackhole bh) {
        OrderBook book = state.createOrderBook();
        
        // Pre-fill with small orders
        for (OrderCommand order : state.preFillSmallOrders) {
            book.newOrder(order);
            bh.consume(order);
        }
        
        long totalProcessed = 0;
        
        // Track allocation during partial matching
        for (OrderCommand order : state.partialMatchOrders) {
            book.newOrder(order);
            totalProcessed++;
            
            // Track all object references
            bh.consume(order.orderId);
            bh.consume(order.price);
            bh.consume(order.size);
            bh.consume(order.action);
            bh.consume(order.timestamp);
            bh.consume(order.uid);
            bh.consume(order.symbol);
        }
        
        // Force GC and measure
        System.gc();
        bh.consume(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        
        return totalProcessed;
    }

    /**
     * GC Profile: Random Mix - Memory allocation patterns for realistic workloads
     * Focus: Mixed allocation patterns, GC behavior under realistic load
     */
    @Benchmark
    public long gcProfile04_RandomMix(OrderBookBenchmarkSuite state, Blackhole bh) {
        OrderBook book = state.createOrderBook();
        
        // Pre-fill with mixed book
        for (OrderCommand order : state.preFillMixedBook) {
            book.newOrder(order);
            bh.consume(order);
        }
        
        long totalProcessed = 0;
        
        // Track allocation during realistic mixed workload
        for (OrderCommand order : state.randomMixOrders) {
            book.newOrder(order);
            totalProcessed++;
            
            // Comprehensive tracking for realistic patterns
            bh.consume(order.orderId);
            bh.consume(order.price);
            bh.consume(order.size);
            bh.consume(order.action);
            bh.consume(order.timestamp);
            bh.consume(order.uid);
            bh.consume(order.symbol);
            bh.consume(order.reserveBidPrice);
        }
        
        // Measure final live set
        System.gc();
        long liveSet = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        bh.consume(liveSet);
        
        return totalProcessed;
    }

    /**
     * GC Profile: Hotspot Single Price - Memory allocation under concentrated load
     * Focus: Allocation patterns when orders concentrate at single price
     */
    @Benchmark
    public long gcProfile05_HotspotSinglePrice(OrderBookBenchmarkSuite state, Blackhole bh) {
        OrderBook book = state.createOrderBook();
        long totalProcessed = 0;
        
        // Track allocation during hotspot concentration
        for (OrderCommand order : state.hotspotSinglePriceOrders) {
            book.newOrder(order);
            totalProcessed++;
            
            // Track hotspot-specific allocation patterns
            bh.consume(order.orderId);
            bh.consume(order.price);
            bh.consume(order.size);
            bh.consume(order.action);
        }
        
        // Measure hotspot memory footprint
        System.gc();
        bh.consume(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        
        return totalProcessed;
    }

    /**
     * GC Profile: Memory Pressure - Allocation behavior under high memory pressure
     * Focus: GC behavior, allocation spikes, pause time impact
     */
    @Benchmark
    public long gcProfile12_MemoryPressure(OrderBookBenchmarkSuite state, Blackhole bh) {
        OrderBook book = state.createOrderBook();
        long totalProcessed = 0;
        
        // Create intentional memory pressure
        for (int i = 0; i < state.datasetSize; i++) {
            OrderAction action = (i % 2 == 0) ? OrderAction.ASK : OrderAction.BID;
            long price = state.PRICE_MID + (i % 1001 - 500) * state.TICK_SIZE;
            long size = 1000 + (i % 9000); // Large sizes for allocation pressure
            
            OrderCommand order = new OrderCommand();
            order.orderId = i;
            order.action = action;
            order.price = price;
            order.size = size;
            order.uid = 1000L + i;
            order.timestamp = System.nanoTime();
            order.symbol = 1;
            
            book.newOrder(order);
            totalProcessed++;
            
            // Force additional allocations to stress GC
            bh.consume(order.orderId);
            bh.consume(order.price);
            bh.consume(order.size);
            bh.consume(order.action);
            bh.consume(order.timestamp);
            bh.consume(order.uid);
            bh.consume(order.symbol);
            bh.consume(new byte[200]); // Additional allocation pressure
            
            // Periodic GC measurement
            if (i % 10000 == 0) {
                System.gc();
                bh.consume(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
            }
        }
        
        // Final GC measurement
        System.gc();
        long finalLiveSet = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        bh.consume(finalLiveSet);
        
        return totalProcessed;
    }

    /**
     * GC Profile: Soak Test - Long-running memory behavior analysis
     * Focus: Memory leaks, drift, long-term GC behavior
     */
    @Benchmark
    @Measurement(iterations = 2, time = 30, timeUnit = TimeUnit.SECONDS)
    public long gcProfile13_SoakTest(OrderBookBenchmarkSuite state, Blackhole bh) {
        OrderBook book = state.createOrderBook();
        long totalProcessed = 0;
        long startTime = System.nanoTime();
        long endTime = startTime + TimeUnit.SECONDS.toNanos(25); // 25 second soak
        
        int orderIndex = 0;
        
        // Run for extended period to detect memory issues
        while (System.nanoTime() < endTime && orderIndex < state.randomMixOrders.size()) {
            OrderCommand order = state.randomMixOrders.get(orderIndex % state.randomMixOrders.size());
            book.newOrder(order);
            totalProcessed++;
            
            bh.consume(order.orderId);
            bh.consume(order.price);
            bh.consume(order.size);
            
            orderIndex++;
            
            // Periodic memory measurement
            if (totalProcessed % 50000 == 0) {
                System.gc();
                long currentLiveSet = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                bh.consume(currentLiveSet);
            }
        }
        
        // Final soak test measurement
        System.gc();
        bh.consume(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        
        return totalProcessed;
    }
}
