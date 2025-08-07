package com.submicro.strukt.benchmark;

import com.submicro.strukt.art.order.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.TimeUnit;

/**
 * Memory pressure benchmark to test OrderBook implementations under constrained heap conditions.
 * Tests GC behavior, allocation rates, and memory efficiency.
 *
 * Note: JMH configuration including JVM args is handled by dedicated runners.
 */
@State(Scope.Benchmark)
public class MemoryPressureBenchmark {

    // These will be set by the runner via JMH parameters
    @Param({})
    private String implementation;

    @Param({})
    private int datasetSize;
    
    private OrderBook orderBook;
    private BenchmarkDataGenerator dataGenerator;
    private MemoryMXBean memoryBean;
    private long initialGcCount;
    private long initialGcTime;
    
    @Setup(Level.Trial)
    public void setupTrial() {
        dataGenerator = new BenchmarkDataGenerator(42L);
        memoryBean = ManagementFactory.getMemoryMXBean();
        
        // Record initial GC stats
        initialGcCount = getTotalGcCount();
        initialGcTime = getTotalGcTime();
    }
    
    @Setup(Level.Iteration)
    public void setupIteration() {
        // Force GC before each iteration for consistent starting conditions
        System.gc();
        
        if ("ART".equals(implementation)) {
            orderBook = new ArtOrderBook();
        } else {
            orderBook = new TreeSetOrderBook();
        }
        dataGenerator.reset();
    }
    
    @TearDown(Level.Iteration)
    public void tearDownIteration() {
        // Report memory usage after each iteration
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        long gcCount = getTotalGcCount() - initialGcCount;
        long gcTime = getTotalGcTime() - initialGcTime;
        
        System.out.printf("%s [%d]: Heap Used: %.2f MB, GC Count: %d, GC Time: %d ms%n",
            implementation, datasetSize,
            heapUsage.getUsed() / (1024.0 * 1024.0),
            gcCount, gcTime);
    }
    
    @Benchmark
    public void memoryPressure_continuousChurn(Blackhole bh) {
        int churnCycles = 5;
        int ordersPerCycle = Math.min(datasetSize / churnCycles, 20000);
        
        for (int cycle = 0; cycle < churnCycles; cycle++) {
            // Phase 1: Add orders
            for (int i = 0; i < ordersPerCycle; i++) {
                long price = (cycle * 10000) + i + 1000;
                OrderCommand order = createOrder(OrderAction.ASK, price, 10);
                orderBook.newOrder(order);
            }
            
            // Phase 2: Match/remove half the orders
            for (int i = 0; i < ordersPerCycle / 2; i++) {
                long price = (cycle * 10000) + i + 1000;
                OrderCommand matchOrder = createOrder(OrderAction.BID, price, 15);
                orderBook.newOrder(matchOrder);
            }
            
            // Consume to prevent dead code elimination
            bh.consume(orderBook);
        }
    }
    
    @Benchmark
    public void memoryPressure_rapidAllocation(Blackhole bh) {
        // Test rapid allocation and deallocation patterns
        for (int batch = 0; batch < 10; batch++) {
            // Allocate a batch of orders
            for (int i = 0; i < datasetSize / 10; i++) {
                long price = (batch * 1000) + i + 1000;
                OrderCommand order = createOrder(OrderAction.ASK, price, 5);
                orderBook.newOrder(order);
            }
            
            // Immediately match them all
            for (int i = 0; i < datasetSize / 10; i++) {
                long price = (batch * 1000) + i + 1000;
                OrderCommand matchOrder = createOrder(OrderAction.BID, price, 10);
                orderBook.newOrder(matchOrder);
            }
            
            bh.consume(batch);
        }
    }
    
    @Benchmark
    public void memoryPressure_fragmentedAccess(Blackhole bh) {
        // Create fragmented memory access patterns
        
        // Phase 1: Create sparse orders
        for (int i = 0; i < datasetSize; i += 10) {
            long price = i + 1000;
            OrderCommand order = createOrder(OrderAction.ASK, price, 10);
            orderBook.newOrder(order);
        }
        
        // Phase 2: Fill in gaps with different order types
        for (int i = 1; i < datasetSize; i += 10) {
            long price = i + 1000;
            OrderCommand order = createOrder(OrderAction.BID, price, 5);
            orderBook.newOrder(order);
        }
        
        // Phase 3: Random access pattern
        for (int i = 0; i < 1000; i++) {
            long price = (i * 7) % datasetSize + 1000; // Pseudo-random access
            OrderCommand order = createOrder(OrderAction.ASK, price, 1);
            orderBook.newOrder(order);
            bh.consume(order);
        }
    }
    
    @Benchmark
    public void memoryPressure_sustainedLoad(Blackhole bh) {
        // Sustained load with gradual memory pressure increase
        int baseOrders = datasetSize / 4;
        
        // Build base load
        for (int i = 0; i < baseOrders; i++) {
            OrderCommand order = createOrder(OrderAction.ASK, i + 1000, 10);
            orderBook.newOrder(order);
        }
        
        // Sustained operations with increasing memory pressure
        for (int round = 0; round < 4; round++) {
            int opsPerRound = baseOrders / 2;
            
            for (int i = 0; i < opsPerRound; i++) {
                // 60% new orders, 40% matching orders
                if (i % 5 < 3) {
                    // New order
                    long price = (round * 10000) + i + 5000;
                    OrderCommand order = createOrder(OrderAction.ASK, price, 10);
                    orderBook.newOrder(order);
                } else {
                    // Matching order
                    long price = (i % baseOrders) + 1000;
                    OrderCommand matchOrder = createOrder(OrderAction.BID, price, 5);
                    orderBook.newOrder(matchOrder);
                }
            }
            
            bh.consume(round);
        }
    }
    
    @Benchmark
    public void memoryPressure_largeOrderSizes(Blackhole bh) {
        // Test with larger order sizes to increase memory pressure
        for (int i = 0; i < datasetSize / 2; i++) {
            long price = i + 1000;
            long size = 1000 + (i % 5000); // Large, variable sizes
            
            OrderCommand order = createOrder(OrderAction.ASK, price, size);
            orderBook.newOrder(order);
            
            // Occasionally match with smaller orders to create partial fills
            if (i % 10 == 0) {
                OrderCommand matchOrder = createOrder(OrderAction.BID, price, size / 3);
                orderBook.newOrder(matchOrder);
            }
            
            bh.consume(order);
        }
    }
    
    // Helper methods
    private OrderCommand createOrder(OrderAction action, long price, long size) {
        OrderCommand cmd = new OrderCommand();
        cmd.id = System.nanoTime() + Thread.currentThread().getId();
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
    
    private long getTotalGcCount() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(GarbageCollectorMXBean::getCollectionCount)
                .sum();
    }
    
    private long getTotalGcTime() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(GarbageCollectorMXBean::getCollectionTime)
                .sum();
    }
}
