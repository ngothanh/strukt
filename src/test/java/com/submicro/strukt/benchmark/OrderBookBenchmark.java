package com.submicro.strukt.benchmark;

import com.submicro.strukt.art.order.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Comprehensive JMH benchmarks for OrderBook.newOrder() method.
 * Tests various scenarios including pure inserts, matches, partial matches,
 * random mixes, hotspot scenarios, and cold book operations.
 * 
 * Measures both throughput (ops/sec) and latency distribution (p50, p90, p99).
 */
@BenchmarkMode({Mode.Throughput, Mode.SampleTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 10, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgs = {
    "-Xmx2G", "-Xms2G",
    "-XX:+UseG1GC",
    "-XX:+UnlockExperimentalVMOptions",
    "-XX:+UseStringDeduplication",
    "-Xlog:gc*:gc.log"
})
@Threads(1) // Start with single thread, can be parameterized later
public class OrderBookBenchmark {

    /**
     * Scenario 1: Pure Insert - No matches, all orders added to book
     * Tests insert path, tree growth, idMap usage
     */
    @Benchmark
    public void pureInsertScenario(OrderBookBenchmarkState state, Blackhole bh) {
        OrderBook book = state.createOrderBook();
        
        for (OrderCommand cmd : state.pureInsertOrders) {
            book.newOrder(cmd);
        }
        
        bh.consume(book);
    }

    /**
     * Scenario 2: Pure Match - 100% matched orders
     * Tests matching logic, getBestMatchingOrder(), removeOrder()
     */
    @Benchmark
    public void pureMatchScenario(OrderBookBenchmarkState state, Blackhole bh) {
        OrderBook book = state.prefilledOrderBook;
        
        for (OrderCommand cmd : state.pureMatchOrders) {
            book.newOrder(cmd);
        }
        
        bh.consume(book);
    }

    /**
     * Scenario 3: Partial Match - 50% matched, remainder inserted
     * Stresses both match and insert in same flow
     */
    @Benchmark
    public void partialMatchScenario(OrderBookBenchmarkState state, Blackhole bh) {
        OrderBook book = state.partialMatchOrderBook;
        
        for (OrderCommand cmd : state.partialMatchOrders) {
            book.newOrder(cmd);
        }
        
        bh.consume(book);
    }

    /**
     * Scenario 4: Random Mix - 70% unmatched, 30% matched
     * Random price ranges to simulate realistic L2 activity
     */
    @Benchmark
    public void randomMixScenario(OrderBookBenchmarkState state, Blackhole bh) {
        OrderBook book = state.randomMixOrderBook;
        
        for (OrderCommand cmd : state.randomMixOrders) {
            book.newOrder(cmd);
        }
        
        bh.consume(book);
    }

    /**
     * Scenario 5: Hotspot Match - All orders target same price level
     * High write pressure on one bucket
     */
    @Benchmark
    public void hotspotMatchScenario(OrderBookBenchmarkState state, Blackhole bh) {
        OrderBook book = state.createOrderBook();
        
        // Pre-fill with one large order to enable matching
        OrderCommand prefillOrder = new OrderCommand();
        prefillOrder.orderId = 999999999L;
        prefillOrder.action = OrderAction.ASK;
        prefillOrder.price = 100_000L;
        prefillOrder.size = state.datasetSize * 100L;
        prefillOrder.uid = 1L;
        prefillOrder.timestamp = System.currentTimeMillis();
        book.newOrder(prefillOrder);
        
        for (OrderCommand cmd : state.hotspotOrders) {
            book.newOrder(cmd);
        }
        
        bh.consume(book);
    }

    /**
     * Scenario 6: Cold Book - Empty every time
     * Insert → immediately matched → removed, book size always ~0
     */
    @Benchmark
    public void coldBookScenario(OrderBookBenchmarkState state, Blackhole bh) {
        OrderBook book = state.createOrderBook();
        
        for (int i = 0; i < state.coldBookOrders.length; i += 2) {
            // Add first order (will be placed in book)
            if (i < state.coldBookOrders.length) {
                book.newOrder(state.coldBookOrders[i]);
            }
            
            // Add second order (will match and remove first)
            if (i + 1 < state.coldBookOrders.length) {
                book.newOrder(state.coldBookOrders[i + 1]);
            }
        }
        
        bh.consume(book);
    }

    /**
     * Single order benchmark for latency measurement
     * Measures individual newOrder() call performance
     */
    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    public void singleOrderLatency(OrderBookBenchmarkState state, Blackhole bh) {
        OrderBook book = state.createOrderBook();
        
        // Use a random order from the pure insert scenario
        int index = (int) (System.nanoTime() % state.pureInsertOrders.length);
        OrderCommand cmd = state.pureInsertOrders[index];
        
        book.newOrder(cmd);
        bh.consume(book);
    }

    /**
     * Batch order processing benchmark
     * Measures throughput of processing multiple orders in sequence
     */
    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void batchOrderThroughput(OrderBookBenchmarkState state, Blackhole bh) {
        OrderBook book = state.createOrderBook();
        
        // Process a small batch of orders
        int batchSize = Math.min(1000, state.pureInsertOrders.length);
        for (int i = 0; i < batchSize; i++) {
            book.newOrder(state.pureInsertOrders[i]);
        }
        
        bh.consume(book);
    }

    /**
     * Memory pressure benchmark
     * Tests GC behavior under high order volume
     */
    @Benchmark
    public void memoryPressureScenario(OrderBookBenchmarkState state, Blackhole bh) {
        OrderBook book = state.createOrderBook();
        
        // Cycle through different order types to create memory pressure
        int quarter = state.datasetSize / 4;
        
        // Insert orders
        for (int i = 0; i < quarter && i < state.pureInsertOrders.length; i++) {
            book.newOrder(state.pureInsertOrders[i]);
        }
        
        // Match some orders
        for (int i = 0; i < quarter && i < state.pureMatchOrders.length; i++) {
            book.newOrder(state.pureMatchOrders[i]);
        }
        
        // Random mix
        for (int i = 0; i < quarter && i < state.randomMixOrders.length; i++) {
            book.newOrder(state.randomMixOrders[i]);
        }
        
        // Hotspot orders
        for (int i = 0; i < quarter && i < state.hotspotOrders.length; i++) {
            book.newOrder(state.hotspotOrders[i]);
        }
        
        bh.consume(book);
    }
}
