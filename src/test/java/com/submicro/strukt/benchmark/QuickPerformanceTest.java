package com.submicro.strukt.benchmark;

import com.submicro.strukt.art.order.*;

/**
 * Comprehensive performance test focused on real matching engine use cases:
 * - Price level management (the core ART advantage)
 * - Best bid/ask lookup (most frequent operation)
 * - Sparse price distributions (real market conditions)
 * - Memory efficiency under stress
 *
 * Optimized for memory efficiency by reusing objects and careful allocation.
 */
public class QuickPerformanceTest {

    // Reusable order command to reduce allocations
    private static final ThreadLocal<OrderCommand> REUSABLE_ORDER = ThreadLocal.withInitial(() -> {
        OrderCommand cmd = new OrderCommand();
        cmd.reserveBidPrice = 0L;
        cmd.symbol = 1;
        return cmd;
    });

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("MATCHING ENGINE PERFORMANCE TEST: ART vs TreeSet OrderBook");
        System.out.println("Focus: Price Level Management & Best Price Discovery");
        System.out.println("=".repeat(80));

        // Test real matching engine scenarios
        testBestPriceLookup();
        testSparsePriceDistribution();
        testPriceLevelManagement();
        testMarketDataGeneration();
        testMemoryEfficiency();
        testLatencyDistribution();

        System.out.println("\n" + "=".repeat(80));
        System.out.println("COMPREHENSIVE PERFORMANCE ANALYSIS COMPLETED");
        System.out.println("=".repeat(80));
    }
    
    /**
     * Test 1: Best Bid/Ask Lookup Performance
     * This is the most critical operation in matching engines
     */
    private static void testBestPriceLookup() {
        System.out.println("\n--- Test 1: Best Bid/Ask Lookup Performance ---");
        System.out.println("Critical Path: Finding best prices for order matching");

        int[] priceLevels = {500, 2000, 5000}; // Reduced scales to avoid OOM
        int lookupsPerTest = 100_000; // Reduced from 1M to 100K

        for (int levels : priceLevels) {
            System.out.printf("\nPrice Levels: %,d | Lookups: %,d\n", levels, lookupsPerTest);

            // Force GC before each test
            System.gc();

            // Test ART
            long artTime = timeBestPriceLookups(new ArtOrderBook(), levels, lookupsPerTest);

            // Force GC between tests
            System.gc();

            // Test TreeSet
            long treeSetTime = timeBestPriceLookups(new TreeSetOrderBook(), levels, lookupsPerTest);

            double ratio = (double) treeSetTime / artTime;
            System.out.printf("ART:     %,d ms (%.1f ns/lookup)\n", artTime, (artTime * 1_000_000.0) / lookupsPerTest);
            System.out.printf("TreeSet: %,d ms (%.1f ns/lookup)\n", treeSetTime, (treeSetTime * 1_000_000.0) / lookupsPerTest);
            System.out.printf("Ratio:   %.2fx %s\n",
                Math.abs(ratio), ratio > 1 ? "(ART faster)" : "(TreeSet faster)");
        }
    }
    
    /**
     * Test 2: Sparse Price Distribution Performance
     * Real markets have sparse price distributions with large gaps
     */
    private static void testSparsePriceDistribution() {
        System.out.println("\n--- Test 2: Sparse Price Distribution Performance ---");
        System.out.println("Real Market Condition: Large gaps between price levels");

        int[] sparsityFactors = {100, 1000, 10000}; // Gap between prices
        int basePriceLevels = 10000;

        for (int sparsity : sparsityFactors) {
            System.out.printf("\nPrice Levels: %,d | Sparsity Factor: %,d (gaps of %,d)\n",
                basePriceLevels, sparsity, sparsity);

            // Test ART
            long artTime = timeSparsePriceDistribution(new ArtOrderBook(), basePriceLevels, sparsity);

            // Test TreeSet
            long treeSetTime = timeSparsePriceDistribution(new TreeSetOrderBook(), basePriceLevels, sparsity);

            double ratio = (double) treeSetTime / artTime;
            System.out.printf("ART:     %,d ms\n", artTime);
            System.out.printf("TreeSet: %,d ms\n", treeSetTime);
            System.out.printf("Ratio:   %.2fx %s\n",
                Math.abs(ratio), ratio > 1 ? "(ART faster)" : "(TreeSet faster)");
        }
    }
    
    /**
     * Test 3: Price Level Management Performance
     * Managing thousands of price levels efficiently
     */
    private static void testPriceLevelManagement() {
        System.out.println("\n--- Test 3: Price Level Management Performance ---");
        System.out.println("Core Operation: Managing thousands of active price levels");

        int[] priceLevelCounts = {5000, 25000, 100000}; // Reduced max from 500K to 100K
        int operationsPerLevel = 10; // Reduced from 100 to 10 operations per level

        for (int levels : priceLevelCounts) {
            System.out.printf("\nPrice Levels: %,d | Operations: %,d\n", levels, levels * operationsPerLevel);

            // Force GC before each test
            System.gc();

            // Test ART
            long artTime = timePriceLevelManagement(new ArtOrderBook(), levels, operationsPerLevel);

            // Force GC between tests
            System.gc();

            // Test TreeSet
            long treeSetTime = timePriceLevelManagement(new TreeSetOrderBook(), levels, operationsPerLevel);

            double ratio = (double) treeSetTime / artTime;
            System.out.printf("ART:     %,d ms\n", artTime);
            System.out.printf("TreeSet: %,d ms\n", treeSetTime);
            System.out.printf("Ratio:   %.2fx %s\n",
                Math.abs(ratio), ratio > 1 ? "(ART faster)" : "(TreeSet faster)");
        }
    }

    /**
     * Test 4: Market Data Generation Performance
     * Iterating through price levels for market data feeds
     */
    private static void testMarketDataGeneration() {
        System.out.println("\n--- Test 4: Market Data Generation Performance ---");
        System.out.println("Use Case: Generating L2 market data feeds");

        int priceLevels = 50000; // Reduced from 100K to 50K
        int iterations = 100; // Reduced from 1000 to 100 iterations

        System.out.printf("\nPrice Levels: %,d | Market Data Generations: %,d\n", priceLevels, iterations);

        // Force GC before test
        System.gc();

        // Test ART
        long artTime = timeMarketDataGeneration(new ArtOrderBook(), priceLevels, iterations);

        // Force GC between tests
        System.gc();

        // Test TreeSet
        long treeSetTime = timeMarketDataGeneration(new TreeSetOrderBook(), priceLevels, iterations);

        double ratio = (double) treeSetTime / artTime;
        System.out.printf("ART:     %,d ms (%.2f ms/generation)\n", artTime, artTime / (double) iterations);
        System.out.printf("TreeSet: %,d ms (%.2f ms/generation)\n", treeSetTime, treeSetTime / (double) iterations);
        System.out.printf("Ratio:   %.2fx %s\n",
            Math.abs(ratio), ratio > 1 ? "(ART faster)" : "(TreeSet faster)");
    }
    
    /**
     * Test 5: Memory Efficiency Under Stress
     */
    private static void testMemoryEfficiency() {
        System.out.println("\n--- Test 5: Memory Efficiency Analysis ---");
        System.out.println("Measuring memory usage and GC pressure");

        int priceLevels = 20000; // Further reduced to avoid OOM

        System.out.printf("\nPrice Levels: %,d\n", priceLevels);

        // Force GC before test
        System.gc();
        Thread.yield();
        System.gc();
        Runtime runtime = Runtime.getRuntime();

        // Test ART
        long artMemoryBefore = runtime.totalMemory() - runtime.freeMemory();
        OrderBook artBook = new ArtOrderBook();
        populateOrderBookEfficient(artBook, priceLevels);
        System.gc();
        Thread.yield();
        System.gc();
        long artMemoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long artMemoryUsed = artMemoryAfter - artMemoryBefore;

        // Clear ART book to free memory
        artBook = null;
        System.gc();
        Thread.yield();
        System.gc();

        // Test TreeSet
        long treeSetMemoryBefore = runtime.totalMemory() - runtime.freeMemory();
        OrderBook treeSetBook = new TreeSetOrderBook();
        populateOrderBookEfficient(treeSetBook, priceLevels);
        System.gc();
        Thread.yield();
        System.gc();
        long treeSetMemoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long treeSetMemoryUsed = treeSetMemoryAfter - treeSetMemoryBefore;

        double memoryRatio = (double) treeSetMemoryUsed / Math.max(artMemoryUsed, 1);
        System.out.printf("ART Memory:     %,d KB\n", artMemoryUsed / 1024);
        System.out.printf("TreeSet Memory: %,d KB\n", treeSetMemoryUsed / 1024);
        System.out.printf("Memory Ratio:   %.2fx %s\n",
            Math.abs(memoryRatio), memoryRatio > 1 ? "(ART more efficient)" : "(TreeSet more efficient)");
    }

    /**
     * Test 6: Latency Distribution Analysis
     */
    private static void testLatencyDistribution() {
        System.out.println("\n--- Test 6: Latency Distribution Analysis ---");
        System.out.println("Measuring p50, p95, p99 latencies for best price lookup");

        int priceLevels = 10000; // Reduced from 50K to 10K
        int measurements = 5000; // Reduced from 10K to 5K

        System.out.printf("\nPrice Levels: %,d | Measurements: %,d\n", priceLevels, measurements);

        // Force GC before test
        System.gc();

        // Test ART latencies
        long[] artLatencies = measureLatencyDistribution(new ArtOrderBook(), priceLevels, measurements);

        // Force GC between tests
        System.gc();

        // Test TreeSet latencies
        long[] treeSetLatencies = measureLatencyDistribution(new TreeSetOrderBook(), priceLevels, measurements);

        // Calculate percentiles
        java.util.Arrays.sort(artLatencies);
        java.util.Arrays.sort(treeSetLatencies);

        long artP50 = artLatencies[measurements / 2];
        long artP95 = artLatencies[(int) (measurements * 0.95)];
        long artP99 = artLatencies[(int) (measurements * 0.99)];

        long treeSetP50 = treeSetLatencies[measurements / 2];
        long treeSetP95 = treeSetLatencies[(int) (measurements * 0.95)];
        long treeSetP99 = treeSetLatencies[(int) (measurements * 0.99)];

        System.out.printf("ART     - p50: %,d ns | p95: %,d ns | p99: %,d ns\n", artP50, artP95, artP99);
        System.out.printf("TreeSet - p50: %,d ns | p95: %,d ns | p99: %,d ns\n", treeSetP50, treeSetP95, treeSetP99);
        System.out.printf("p99 Ratio: %.2fx %s\n",
            (double) treeSetP99 / Math.max(artP99, 1),
            treeSetP99 > artP99 ? "(ART better tail latency)" : "(TreeSet better tail latency)");
    }
    
    // ========== Benchmark Implementation Methods ==========

    private static long timeBestPriceLookups(OrderBook orderBook, int priceLevels, int lookups) {
        // First populate the order book with price levels efficiently
        populateOrderBookEfficient(orderBook, priceLevels);

        long startTime = System.currentTimeMillis();

        // Use reusable order to reduce allocations
        OrderCommand reusableOrder = REUSABLE_ORDER.get();

        // Perform rapid best price lookups by creating orders that will match
        long seed = 42;
        for (int i = 0; i < lookups; i++) {
            seed = (seed * 1103515245 + 12345) & 0x7fffffffL;
            long price = (seed % priceLevels) + 1000;

            // Reuse order object to trigger best price lookup
            updateReusableOrder(reusableOrder, OrderAction.BID, price, 1);
            orderBook.newOrder(reusableOrder);
        }

        return System.currentTimeMillis() - startTime;
    }

    private static long timeSparsePriceDistribution(OrderBook orderBook, int priceLevels, int sparsityFactor) {
        long startTime = System.currentTimeMillis();

        // Use reusable order to reduce allocations
        OrderCommand reusableOrder = REUSABLE_ORDER.get();

        // Create sparse price distribution with large gaps
        for (int i = 0; i < priceLevels; i++) {
            long price = 1000 + (i * sparsityFactor); // Large gaps between prices
            updateReusableOrder(reusableOrder, OrderAction.ASK, price, 10);
            orderBook.newOrder(reusableOrder);
        }

        // Perform lookups across the sparse range
        long seed = 42;
        for (int i = 0; i < priceLevels; i++) {
            seed = (seed * 1103515245 + 12345) & 0x7fffffffL;
            long price = 1000 + ((seed % priceLevels) * sparsityFactor);
            updateReusableOrder(reusableOrder, OrderAction.BID, price, 1);
            orderBook.newOrder(reusableOrder);
        }

        return System.currentTimeMillis() - startTime;
    }
    
    private static long timePriceLevelManagement(OrderBook orderBook, int priceLevels, int operationsPerLevel) {
        long startTime = System.currentTimeMillis();

        // Use reusable order to reduce allocations
        OrderCommand reusableOrder = REUSABLE_ORDER.get();

        // Create many price levels with memory-efficient approach
        for (int level = 0; level < priceLevels; level++) {
            long basePrice = 1000 + level;

            // Multiple operations per price level
            for (int op = 0; op < operationsPerLevel; op++) {
                if (op % 3 == 0) {
                    // Add order to price level
                    updateReusableOrder(reusableOrder, OrderAction.ASK, basePrice, 10);
                    orderBook.newOrder(reusableOrder);
                } else if (op % 3 == 1) {
                    // Match some orders at this price level
                    updateReusableOrder(reusableOrder, OrderAction.BID, basePrice, 5);
                    orderBook.newOrder(reusableOrder);
                } else {
                    // Add more depth to price level
                    updateReusableOrder(reusableOrder, OrderAction.ASK, basePrice, 20);
                    orderBook.newOrder(reusableOrder);
                }
            }

            // More frequent GC hints for memory management
            if (level % 5000 == 0 && level > 0) {
                System.gc();
                Thread.yield();
            }
        }

        return System.currentTimeMillis() - startTime;
    }

    private static long timeMarketDataGeneration(OrderBook orderBook, int priceLevels, int iterations) {
        // Populate order book with many price levels efficiently
        populateOrderBookEfficient(orderBook, priceLevels);

        long startTime = System.currentTimeMillis();

        // Use reusable order to reduce allocations
        OrderCommand reusableOrder = REUSABLE_ORDER.get();

        // Simulate market data generation by iterating through price levels
        for (int iter = 0; iter < iterations; iter++) {
            // Simulate generating L2 market data by accessing price levels
            long seed = 42 + iter;
            for (int i = 0; i < 100; i++) { // Sample 100 price levels per iteration
                seed = (seed * 1103515245 + 12345) & 0x7fffffffL;
                long price = (seed % priceLevels) + 1000;

                // Access price level (simulates market data generation)
                updateReusableOrder(reusableOrder, OrderAction.BID, price, 1);
                orderBook.newOrder(reusableOrder);
            }
        }

        return System.currentTimeMillis() - startTime;
    }
    
    private static void populateOrderBookEfficient(OrderBook orderBook, int priceLevels) {
        // Create realistic order book with multiple orders per price level using reusable objects
        OrderCommand reusableOrder = REUSABLE_ORDER.get();

        for (int i = 0; i < priceLevels; i++) {
            long price = 1000 + i;

            // Add multiple orders at each price level (realistic depth) using reusable order
            updateReusableOrder(reusableOrder, OrderAction.ASK, price, 10);
            orderBook.newOrder(reusableOrder);

            updateReusableOrder(reusableOrder, OrderAction.ASK, price, 20);
            orderBook.newOrder(reusableOrder);

            updateReusableOrder(reusableOrder, OrderAction.ASK, price, 15);
            orderBook.newOrder(reusableOrder);

            // More frequent GC hints for memory management
            if (i % 2000 == 0 && i > 0) {
                System.gc();
                Thread.yield();
            }
        }
    }

    private static void updateReusableOrder(OrderCommand order, OrderAction action, long price, long size) {
        order.id = System.nanoTime() + Thread.currentThread().getId();
        order.orderId = order.id;
        order.action = action;
        order.price = price;
        order.size = size;
        order.uid = order.id + 1000;
        order.timestamp = System.currentTimeMillis();
    }

    private static long[] measureLatencyDistribution(OrderBook orderBook, int priceLevels, int measurements) {
        // Populate order book efficiently
        populateOrderBookEfficient(orderBook, priceLevels);

        long[] latencies = new long[measurements];
        long seed = 42;
        OrderCommand reusableOrder = REUSABLE_ORDER.get();

        // Warm up
        for (int i = 0; i < 1000; i++) {
            seed = (seed * 1103515245 + 12345) & 0x7fffffffL;
            long price = (seed % priceLevels) + 1000;
            updateReusableOrder(reusableOrder, OrderAction.BID, price, 1);
            orderBook.newOrder(reusableOrder);
        }

        // Measure latencies
        for (int i = 0; i < measurements; i++) {
            seed = (seed * 1103515245 + 12345) & 0x7fffffffL;
            long price = (seed % priceLevels) + 1000;

            long startTime = System.nanoTime();
            updateReusableOrder(reusableOrder, OrderAction.BID, price, 1);
            orderBook.newOrder(reusableOrder);
            long endTime = System.nanoTime();

            latencies[i] = endTime - startTime;
        }

        return latencies;
    }

    private static OrderCommand createOrder(OrderAction action, long price, long size) {
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
}
