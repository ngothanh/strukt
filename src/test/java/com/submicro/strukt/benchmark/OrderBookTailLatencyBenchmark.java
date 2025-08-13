package com.submicro.strukt.benchmark;

import com.submicro.strukt.art.order.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * OrderBook Tail-Latency Benchmark Suite
 * 
 * Dedicated to measuring tail latency distributions (p99, p99.9, p99.99) using SampleTime mode.
 * Critical for exchange-core validation where worst-case predictability matters more than averages.
 * 
 * Key Metrics:
 * - Individual operation latency distributions
 * - Tail latency percentiles (p99, p99.9, p99.99, max)
 * - HDRHistogram-compatible measurements for CDF analysis
 * - GC pause correlation with outliers
 * - Jitter analysis under various load conditions
 * 
 * Configuration: Extended measurement for stable tail analysis
 * Mode: SampleTime for detailed latency distribution capture
 * Focus: Worst-case predictability for HFT requirements
 */
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 15, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(value = 5, jvmArgs = {
    "-Xmx8G", "-Xms8G", 
    "-XX:+UseG1GC", 
    "-XX:+AlwaysPreTouch",
    "-XX:MaxGCPauseMillis=10", // Aggressive GC tuning for latency
    "-XX:G1HeapRegionSize=16m",
    "-XX:+UnlockExperimentalVMOptions",
    "-XX:G1NewSizePercent=40",
    "-XX:G1MaxNewSizePercent=50",
    "-Xlog:gc*:tail-latency-gc.log"
})
@Threads(1)
public class OrderBookTailLatencyBenchmark {

    /**
     * Tail Latency: Pure Insert - Individual insert operation latency distribution
     * Critical for understanding worst-case insert performance under various conditions
     */
    @Benchmark
    public long tailLatency01_PureInsert_Cold(OrderBookBenchmarkSuite state, Blackhole bh) {
        OrderBook book = state.createOrderBook();
        
        // Cold book - single insert operation
        OrderCommand order = state.pureInsertOrders.get(0);
        long startTime = System.nanoTime();
        book.newOrder(order);
        long endTime = System.nanoTime();
        
        // Comprehensive DCE prevention
        bh.consume(order.orderId);
        bh.consume(order.price);
        bh.consume(order.size);
        bh.consume(order.action);
        bh.consume(startTime);
        bh.consume(endTime);
        
        return endTime - startTime;
    }

    /**
     * Tail Latency: Pure Insert - Insert operation in pre-warmed book
     */
    @Benchmark
    public long tailLatency01_PureInsert_Warm(OrderBookBenchmarkSuite state, Blackhole bh) {
        OrderBook book = state.createOrderBook();
        
        // Pre-warm with 1000 orders
        for (int i = 0; i < 1000 && i < state.pureInsertOrders.size(); i++) {
            book.newOrder(state.pureInsertOrders.get(i));
        }
        
        // Measure single insert in warm book
        OrderCommand order = state.pureInsertOrders.get(1001 % state.pureInsertOrders.size());
        long startTime = System.nanoTime();
        book.newOrder(order);
        long endTime = System.nanoTime();
        
        bh.consume(order.orderId);
        bh.consume(order.price);
        bh.consume(order.size);
        bh.consume(startTime);
        bh.consume(endTime);
        
        return endTime - startTime;
    }

    /**
     * Tail Latency: Pure Match - Individual match operation latency distribution
     */
    @Benchmark
    public long tailLatency02_PureMatch_SingleLevel(OrderBookBenchmarkSuite state, Blackhole bh) {
        OrderBook book = state.createOrderBook();
        
        // Pre-fill with single opposing order
        OrderCommand preFill = new OrderCommand();
        preFill.orderId = 999L;
        preFill.action = OrderAction.BID;
        preFill.price = state.PRICE_MID;
        preFill.size = 10000L; // Large size to avoid depletion
        preFill.uid = 1000L;
        preFill.timestamp = System.nanoTime();
        preFill.symbol = 1;
        book.newOrder(preFill);
        
        // Measure single match operation
        OrderCommand order = new OrderCommand();
        order.orderId = 1000L;
        order.action = OrderAction.ASK;
        order.price = state.PRICE_MID;
        order.size = 100L;
        order.uid = 1000L;
        order.timestamp = System.nanoTime();
        order.symbol = 1;
        
        long startTime = System.nanoTime();
        book.newOrder(order);
        long endTime = System.nanoTime();
        
        bh.consume(order.orderId);
        bh.consume(order.price);
        bh.consume(order.size);
        bh.consume(startTime);
        bh.consume(endTime);
        
        return endTime - startTime;
    }

    /**
     * Tail Latency: Pure Match - Match operation against deep book
     */
    @Benchmark
    public long tailLatency02_PureMatch_DeepBook(OrderBookBenchmarkSuite state, Blackhole bh) {
        OrderBook book = state.createOrderBook();
        
        // Pre-fill with deep liquidity (100 levels)
        for (int i = 0; i < 100; i++) {
            OrderCommand bid = new OrderCommand();
            bid.orderId = i;
            bid.action = OrderAction.BID;
            bid.price = state.PRICE_MID - i;
            bid.size = 1000L;
            bid.uid = 1000L;
            bid.timestamp = System.nanoTime();
            bid.symbol = 1;
            book.newOrder(bid);
        }
        
        // Measure match against deep book
        OrderCommand order = new OrderCommand();
        order.orderId = 1000L;
        order.action = OrderAction.ASK;
        order.price = state.PRICE_MID - 50; // Match deep in book
        order.size = 100L;
        order.uid = 1000L;
        order.timestamp = System.nanoTime();
        order.symbol = 1;
        
        long startTime = System.nanoTime();
        book.newOrder(order);
        long endTime = System.nanoTime();
        
        bh.consume(order.orderId);
        bh.consume(order.price);
        bh.consume(order.size);
        bh.consume(startTime);
        bh.consume(endTime);
        
        return endTime - startTime;
    }

    /**
     * Tail Latency: Hotspot - Operation at concentrated price level
     */
    @Benchmark
    public long tailLatency05_Hotspot_Concentrated(OrderBookBenchmarkSuite state, Blackhole bh) {
        OrderBook book = state.createOrderBook();
        
        // Create hotspot with 500 orders at same price
        for (int i = 0; i < 500; i++) {
            OrderCommand hotspotOrder = new OrderCommand();
            hotspotOrder.orderId = i;
            hotspotOrder.action = OrderAction.BID;
            hotspotOrder.price = state.PRICE_MID;
            hotspotOrder.size = 100L;
            hotspotOrder.uid = 1000L;
            hotspotOrder.timestamp = System.nanoTime();
            hotspotOrder.symbol = 1;
            book.newOrder(hotspotOrder);
        }
        
        // Measure operation at hotspot price
        OrderCommand order = new OrderCommand();
        order.orderId = 1000L;
        order.action = OrderAction.ASK;
        order.price = state.PRICE_MID;
        order.size = 100L;
        order.uid = 1000L;
        order.timestamp = System.nanoTime();
        order.symbol = 1;
        
        long startTime = System.nanoTime();
        book.newOrder(order);
        long endTime = System.nanoTime();
        
        bh.consume(order.orderId);
        bh.consume(order.price);
        bh.consume(order.size);
        bh.consume(startTime);
        bh.consume(endTime);
        
        return endTime - startTime;
    }

    /**
     * Tail Latency: Memory Pressure - Operation under allocation stress
     */
    @Benchmark
    public long tailLatency12_MemoryPressure_HighAlloc(OrderBookBenchmarkSuite state, Blackhole bh) {
        OrderBook book = state.createOrderBook();
        
        // Create memory pressure with large allocations
        for (int i = 0; i < 1000; i++) {
            bh.consume(new byte[10000]); // 10KB allocations
        }
        
        // Measure operation under memory pressure
        OrderCommand order = new OrderCommand();
        order.orderId = 1000L;
        order.action = OrderAction.ASK;
        order.price = state.PRICE_MID;
        order.size = 5000L; // Large order for additional pressure
        order.uid = 1000L;
        order.timestamp = System.nanoTime();
        order.symbol = 1;
        
        long startTime = System.nanoTime();
        book.newOrder(order);
        long endTime = System.nanoTime();
        
        // Additional allocation pressure
        bh.consume(new byte[5000]);
        bh.consume(order.orderId);
        bh.consume(order.price);
        bh.consume(order.size);
        bh.consume(startTime);
        bh.consume(endTime);
        
        return endTime - startTime;
    }

    /**
     * Tail Latency: Burst Load - Operation during burst conditions
     */
    @Benchmark
    public long tailLatency11_BurstLoad_Spike(OrderBookBenchmarkSuite state, Blackhole bh) {
        OrderBook book = state.createOrderBook();
        
        // Simulate burst by processing 100 orders rapidly
        for (int i = 0; i < 100; i++) {
            OrderCommand burstOrder = state.randomMixOrders.get(i % state.randomMixOrders.size());
            book.newOrder(burstOrder);
            bh.consume(burstOrder.orderId);
        }
        
        // Measure single operation during burst aftermath
        OrderCommand order = state.randomMixOrders.get(101 % state.randomMixOrders.size());
        
        long startTime = System.nanoTime();
        book.newOrder(order);
        long endTime = System.nanoTime();
        
        bh.consume(order.orderId);
        bh.consume(order.price);
        bh.consume(order.size);
        bh.consume(startTime);
        bh.consume(endTime);
        
        return endTime - startTime;
    }
}
