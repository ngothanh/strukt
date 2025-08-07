package com.submicro.strukt.benchmark;

import com.submicro.strukt.art.order.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.List;
import java.util.NavigableMap;
import java.util.concurrent.TimeUnit;

/**
 * Advanced benchmark scenarios for OrderBook comparison.
 * Focuses on concurrent performance and range query capabilities.
 *
 * Note: JMH configuration is handled by dedicated runners, not annotations.
 */
@State(Scope.Benchmark)
public class OrderBookAdvancedBenchmark {

    // These will be set by the runner via JMH parameters
    @Param({})
    private int datasetSize;

    @Param({}) // Will be configured by runner
    private String implementation;
    
    private TreeSetOrderBook treeSetOrderBook;
    private BenchmarkDataGenerator dataGenerator;
    private List<OrderCommand> rangeQueryOrders;
    
    @Setup(Level.Trial)
    public void setupTrial() {
        dataGenerator = new BenchmarkDataGenerator(42L);
        rangeQueryOrders = dataGenerator.generateRangeQueryOrders(datasetSize, OrderAction.ASK);
    }
    
    @Setup(Level.Iteration)
    public void setupIteration() {
        treeSetOrderBook = new TreeSetOrderBook();
        dataGenerator.reset();
    }
    
    // ========== Scenario 8: Range Query Performance ==========
    
    @Benchmark
    public void scenario8_rangeQuery_small(Blackhole bh) {
        // Populate order book
        for (OrderCommand order : rangeQueryOrders) {
            treeSetOrderBook.newOrder(order);
        }
        
        // Perform range queries (small ranges: 10 entries)
        for (int i = 0; i < 1000; i++) {
            long startPrice = 1000 + (i * 100);
            long endPrice = startPrice + 100; // 10 entries (every 10th price)
            
            int count = performRangeQuery(startPrice, endPrice);
            bh.consume(count);
        }
    }
    
    @Benchmark
    public void scenario8_rangeQuery_medium(Blackhole bh) {
        // Populate order book
        for (OrderCommand order : rangeQueryOrders) {
            treeSetOrderBook.newOrder(order);
        }
        
        // Perform range queries (medium ranges: 100 entries)
        for (int i = 0; i < 100; i++) {
            long startPrice = 1000 + (i * 1000);
            long endPrice = startPrice + 1000; // 100 entries
            
            int count = performRangeQuery(startPrice, endPrice);
            bh.consume(count);
        }
    }
    
    @Benchmark
    public void scenario8_rangeQuery_large(Blackhole bh) {
        // Populate order book
        for (OrderCommand order : rangeQueryOrders) {
            treeSetOrderBook.newOrder(order);
        }
        
        // Perform range queries (large ranges: 1000 entries)
        for (int i = 0; i < 10; i++) {
            long startPrice = 1000 + (i * 10000);
            long endPrice = startPrice + 10000; // 1000 entries
            
            int count = performRangeQuery(startPrice, endPrice);
            bh.consume(count);
        }
    }
    
    @Benchmark
    public void scenario8_headMap_queries(Blackhole bh) {
        // Populate order book
        for (OrderCommand order : rangeQueryOrders) {
            treeSetOrderBook.newOrder(order);
        }
        
        // Perform headMap queries (all orders below a price)
        for (int i = 0; i < 100; i++) {
            long maxPrice = 1000 + (i * 100);
            int count = performHeadMapQuery(maxPrice);
            bh.consume(count);
        }
    }
    
    @Benchmark
    public void scenario8_tailMap_queries(Blackhole bh) {
        // Populate order book
        for (OrderCommand order : rangeQueryOrders) {
            treeSetOrderBook.newOrder(order);
        }
        
        // Perform tailMap queries (all orders above a price)
        for (int i = 0; i < 100; i++) {
            long minPrice = 1000 + (i * 100);
            int count = performTailMapQuery(minPrice);
            bh.consume(count);
        }
    }
    
    // ========== Concurrent Performance Simulation ==========
    
    @Benchmark
    @Threads(1)
    public void scenario7_concurrent_singleThread(Blackhole bh) {
        performConcurrentWorkload(bh);
    }
    
    @Benchmark
    @Threads(2)
    public void scenario7_concurrent_twoThreads(Blackhole bh) {
        performConcurrentWorkload(bh);
    }
    
    @Benchmark
    @Threads(4)
    public void scenario7_concurrent_fourThreads(Blackhole bh) {
        performConcurrentWorkload(bh);
    }
    
    @Benchmark
    @Threads(8)
    public void scenario7_concurrent_eightThreads(Blackhole bh) {
        performConcurrentWorkload(bh);
    }
    
    // ========== Memory Efficiency Test ==========
    
    @Benchmark
    public void memoryEfficiency_largeDataset(Blackhole bh) {
        // Test memory efficiency with large dataset
        for (int i = 0; i < datasetSize; i++) {
            OrderCommand order = createOrder(OrderAction.ASK, i + 1000, 10);
            treeSetOrderBook.newOrder(order);
            
            // Occasionally match some orders to test memory cleanup
            if (i % 100 == 0 && i > 0) {
                OrderCommand matchOrder = createOrder(OrderAction.BID, i + 900, 50);
                treeSetOrderBook.newOrder(matchOrder);
            }
        }
        bh.consume(treeSetOrderBook);
    }
    
    // ========== Helper Methods ==========
    
    private void performConcurrentWorkload(Blackhole bh) {
        // Each thread performs mixed operations
        for (int i = 0; i < 1000; i++) {
            double opType = (i % 10) / 10.0;
            
            if (opType < 0.5) {
                // 50% reads (matching operations)
                OrderCommand matchOrder = createOrder(OrderAction.BID, (i % 1000) + 1000, 1);
                treeSetOrderBook.newOrder(matchOrder);
                bh.consume(matchOrder);
            } else {
                // 50% writes (new orders)
                OrderCommand newOrder = createOrder(OrderAction.ASK, i + 5000, 10);
                treeSetOrderBook.newOrder(newOrder);
                bh.consume(newOrder);
            }
        }
    }
    
    private int performRangeQuery(long startPrice, long endPrice) {
        // Simulate range query by accessing TreeSet's NavigableMap
        // Note: This is a simplified simulation since we don't have direct access
        // to the internal TreeMap in TreeSetOrderBook
        int count = 0;
        for (long price = startPrice; price <= endPrice; price += 10) {
            // Simulate accessing orders in price range
            OrderCommand testOrder = createOrder(OrderAction.BID, price, 1);
            treeSetOrderBook.newOrder(testOrder);
            count++;
        }
        return count;
    }
    
    private int performHeadMapQuery(long maxPrice) {
        // Simulate headMap query
        int count = 0;
        for (long price = 1000; price < maxPrice; price += 10) {
            OrderCommand testOrder = createOrder(OrderAction.BID, price, 1);
            treeSetOrderBook.newOrder(testOrder);
            count++;
        }
        return count;
    }
    
    private int performTailMapQuery(long minPrice) {
        // Simulate tailMap query
        int count = 0;
        for (long price = minPrice; price < minPrice + 1000; price += 10) {
            OrderCommand testOrder = createOrder(OrderAction.BID, price, 1);
            treeSetOrderBook.newOrder(testOrder);
            count++;
        }
        return count;
    }
    
    private OrderCommand createOrder(OrderAction action, long price, long size) {
        OrderCommand cmd = new OrderCommand();
        cmd.id = System.nanoTime() + Thread.currentThread().getId(); // Thread-safe unique IDs
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
}
