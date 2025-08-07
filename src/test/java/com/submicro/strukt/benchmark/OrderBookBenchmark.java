package com.submicro.strukt.benchmark;

import com.submicro.strukt.art.order.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive benchmark suite for ArtOrderBook vs TreeSetOrderBook.
 * Implements the 8 decision matrix scenarios to determine optimal usage patterns.
 *
 * Note: JMH configuration is handled by dedicated runners, not annotations.
 */
@State(Scope.Benchmark)
public class OrderBookBenchmark {

    // These will be set by the runner via JMH parameters
    @Param({})
    private int datasetSize;

    @Param({})
    private String implementation;
    
    private OrderBook orderBook;
    private BenchmarkDataGenerator dataGenerator;
    
    // Test data for different scenarios
    private List<OrderCommand> sequentialOrders;
    private List<OrderCommand> randomSparseOrders;
    private List<OrderCommand> clusteredOrders;
    private List<OrderCommand> prefixSharedOrders;
    private List<OrderCommand> mixedWorkloadReadHeavy;
    private List<OrderCommand> mixedWorkloadBalanced;
    private List<OrderCommand> mixedWorkloadWriteHeavy;
    private List<OrderCommand> hotspotOrders;
    private List<OrderCommand> rangeQueryOrders;
    
    @Setup(Level.Trial)
    public void setupTrial() {
        dataGenerator = new BenchmarkDataGenerator(42L);
        
        // Pre-generate all test data
        sequentialOrders = dataGenerator.generateSequentialOrders(datasetSize, OrderAction.ASK);
        randomSparseOrders = dataGenerator.generateRandomSparseOrders(datasetSize, OrderAction.ASK);
        clusteredOrders = dataGenerator.generateClusteredOrders(datasetSize, OrderAction.ASK);
        prefixSharedOrders = dataGenerator.generatePrefixSharedOrders(datasetSize, OrderAction.ASK);
        
        // Mixed workloads: read%, write%, remove%
        mixedWorkloadReadHeavy = dataGenerator.generateMixedWorkload(datasetSize, 0.95, 0.05, 0.0);
        mixedWorkloadBalanced = dataGenerator.generateMixedWorkload(datasetSize, 0.50, 0.30, 0.20);
        mixedWorkloadWriteHeavy = dataGenerator.generateMixedWorkload(datasetSize, 0.20, 0.80, 0.0);
        
        hotspotOrders = dataGenerator.generateHotspotOrders(datasetSize, OrderAction.ASK);
        rangeQueryOrders = dataGenerator.generateRangeQueryOrders(datasetSize, OrderAction.ASK);
    }
    
    @Setup(Level.Iteration)
    public void setupIteration() {
        // Create fresh order book for each iteration
        if ("ART".equals(implementation)) {
            orderBook = new ArtOrderBook();
        } else {
            orderBook = new TreeSetOrderBook();
        }
        dataGenerator.reset();
    }
    
    // ========== Scenario 1: Scale Threshold Analysis ==========
    
    @Benchmark
    public void scenario1_scaleThreshold_sequentialInsert(Blackhole bh) {
        for (OrderCommand order : sequentialOrders) {
            orderBook.newOrder(order);
        }
        bh.consume(orderBook);
    }
    
    @Benchmark
    public void scenario1_scaleThreshold_randomLookup(Blackhole bh) {
        // First populate the order book
        for (OrderCommand order : sequentialOrders) {
            orderBook.newOrder(order);
        }
        
        // Then perform random lookups by creating matching orders
        for (int i = 0; i < Math.min(10000, datasetSize); i++) {
            long price = (i % datasetSize) + 1;
            OrderCommand matchingOrder = createMatchingOrder(price);
            orderBook.newOrder(matchingOrder);
            bh.consume(matchingOrder);
        }
    }
    
    // ========== Scenario 2: Key Distribution Impact ==========
    
    @Benchmark
    public void scenario2_keyDistribution_sequential(Blackhole bh) {
        for (OrderCommand order : sequentialOrders) {
            orderBook.newOrder(order);
        }
        bh.consume(orderBook);
    }
    
    @Benchmark
    public void scenario2_keyDistribution_randomSparse(Blackhole bh) {
        for (OrderCommand order : randomSparseOrders) {
            orderBook.newOrder(order);
        }
        bh.consume(orderBook);
    }
    
    @Benchmark
    public void scenario2_keyDistribution_clustered(Blackhole bh) {
        for (OrderCommand order : clusteredOrders) {
            orderBook.newOrder(order);
        }
        bh.consume(orderBook);
    }
    
    @Benchmark
    public void scenario2_keyDistribution_prefixShared(Blackhole bh) {
        for (OrderCommand order : prefixSharedOrders) {
            orderBook.newOrder(order);
        }
        bh.consume(orderBook);
    }
    
    // ========== Scenario 3: Operation Mix Sensitivity ==========
    
    @Benchmark
    public void scenario3_operationMix_readHeavy(Blackhole bh) {
        for (OrderCommand order : mixedWorkloadReadHeavy) {
            orderBook.newOrder(order);
        }
        bh.consume(orderBook);
    }
    
    @Benchmark
    public void scenario3_operationMix_balanced(Blackhole bh) {
        for (OrderCommand order : mixedWorkloadBalanced) {
            orderBook.newOrder(order);
        }
        bh.consume(orderBook);
    }
    
    @Benchmark
    public void scenario3_operationMix_writeHeavy(Blackhole bh) {
        for (OrderCommand order : mixedWorkloadWriteHeavy) {
            orderBook.newOrder(order);
        }
        bh.consume(orderBook);
    }
    
    // ========== Scenario 5: Access Pattern Locality ==========
    
    @Benchmark
    public void scenario5_accessPattern_sequential(Blackhole bh) {
        // Populate first
        for (OrderCommand order : sequentialOrders) {
            orderBook.newOrder(order);
        }
        
        // Sequential access pattern
        for (int i = 1; i <= Math.min(1000, datasetSize); i++) {
            OrderCommand matchingOrder = createMatchingOrder(i);
            orderBook.newOrder(matchingOrder);
            bh.consume(matchingOrder);
        }
    }
    
    @Benchmark
    public void scenario5_accessPattern_hotspot(Blackhole bh) {
        for (OrderCommand order : hotspotOrders) {
            orderBook.newOrder(order);
        }
        bh.consume(orderBook);
    }
    
    // ========== Scenario 4: Memory Pressure Test ==========

    @Benchmark
    public void scenario4_memoryPressure_churn(Blackhole bh) {
        // Simulate memory pressure with continuous churn
        int churnSize = Math.min(datasetSize, 10000);

        // Initial population
        for (int i = 0; i < churnSize; i++) {
            OrderCommand order = createOrder(OrderAction.ASK, i + 1000, 10);
            orderBook.newOrder(order);
        }

        // Churn: remove half, add half (repeat pattern)
        for (int cycle = 0; cycle < 3; cycle++) {
            // Remove orders by matching them
            for (int i = 0; i < churnSize / 2; i++) {
                OrderCommand matchingOrder = createOrder(OrderAction.BID, i + 1000, 10);
                orderBook.newOrder(matchingOrder);
            }

            // Add new orders
            for (int i = 0; i < churnSize / 2; i++) {
                OrderCommand order = createOrder(OrderAction.ASK, i + 2000 + (cycle * 1000), 10);
                orderBook.newOrder(order);
            }
        }
        bh.consume(orderBook);
    }

    // ========== Scenario 6: Latency Distribution Analysis ==========

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void scenario6_latencyDistribution_sustainedLoad(Blackhole bh) {
        // Pre-populate for sustained load test
        for (int i = 0; i < Math.min(datasetSize, 5000); i++) {
            OrderCommand order = createOrder(OrderAction.ASK, i + 1000, 10);
            orderBook.newOrder(order);
        }

        // Mixed operations under sustained load
        for (int i = 0; i < 1000; i++) {
            double opType = (i % 10) / 10.0;
            if (opType < 0.7) {
                // 70% reads (matching operations)
                OrderCommand matchOrder = createOrder(OrderAction.BID, (i % 1000) + 1000, 1);
                orderBook.newOrder(matchOrder);
                bh.consume(matchOrder);
            } else if (opType < 0.9) {
                // 20% writes (new orders)
                OrderCommand newOrder = createOrder(OrderAction.ASK, i + 5000, 10);
                orderBook.newOrder(newOrder);
                bh.consume(newOrder);
            } else {
                // 10% removes (via matching)
                OrderCommand removeOrder = createOrder(OrderAction.BID, (i % 1000) + 1000, 20);
                orderBook.newOrder(removeOrder);
                bh.consume(removeOrder);
            }
        }
    }

    // ========== Helper Methods ==========

    private OrderCommand createOrder(OrderAction action, long price, long size) {
        OrderCommand cmd = new OrderCommand();
        cmd.id = System.nanoTime(); // Use timestamp for unique IDs
        cmd.orderId = cmd.id;
        cmd.action = action;
        cmd.price = price;
        cmd.size = size;
        cmd.uid = cmd.id + 1000;
        cmd.timestamp = System.currentTimeMillis();
        cmd.reserveBidPrice = 0L;
        cmd.symbol = 1;
        return cmd;
    }

    private OrderCommand createMatchingOrder(long price) {
        return createOrder(OrderAction.BID, price, 1);
    }
}
