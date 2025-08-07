package com.submicro.strukt.benchmark;

import com.submicro.strukt.art.order.ArtOrderBook;
import com.submicro.strukt.art.order.OrderAction;
import com.submicro.strukt.art.order.OrderCommand;
import com.submicro.strukt.art.order.TreeSetOrderBook;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Memory efficiency benchmark specifically designed to test ART's memory advantages.
 * Focuses on scenarios with high memory pressure and prefix-heavy data.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 1, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 2, time = 15, timeUnit = TimeUnit.SECONDS)
public class MemoryEfficiencyBenchmark {

    @Param({"500000", "1000000", "1500000"})
    private int datasetSize;

    @Param({"ART", "TreeSet"})
    private String implementation;

    private Object orderBook;
    private List<Long> prefixKeys;
    private List<Long> sparseKeys;
    private Random random;
    private MemoryMXBean memoryBean;

    @Setup(Level.Trial)
    public void setupTrial() {
        memoryBean = ManagementFactory.getMemoryMXBean();
        random = new Random(42);
        
        // Create implementation
        if ("ART".equals(implementation)) {
            orderBook = new ArtOrderBook();
        } else {
            orderBook = new TreeSetOrderBook();
        }

        generateMemoryTestKeys();
        
        // Measure memory before population
        System.gc();
        long memoryBefore = getUsedMemory();
        System.out.println("Memory before population: " + formatMemory(memoryBefore));
        
        populateOrderBook();
        
        // Measure memory after population
        System.gc();
        long memoryAfter = getUsedMemory();
        System.out.println("Memory after population: " + formatMemory(memoryAfter));
        System.out.println("Memory used by " + implementation + " with " + datasetSize + " orders: " + 
                          formatMemory(memoryAfter - memoryBefore));
    }

    private void generateMemoryTestKeys() {
        prefixKeys = new ArrayList<>(datasetSize);
        sparseKeys = new ArrayList<>(datasetSize);

        // Generate prefix-heavy keys (should favor ART)
        String[] symbols = {"AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "META", "NVDA", "NFLX"};
        for (int i = 0; i < datasetSize; i++) {
            String symbol = symbols[i % symbols.length];
            // Create keys with common prefixes but different suffixes
            long key = hashString(symbol + String.format("%010d", i));
            prefixKeys.add(key);
        }

        // Generate sparse keys (wide distribution)
        Set<Long> uniqueKeys = new HashSet<>();
        while (uniqueKeys.size() < datasetSize) {
            uniqueKeys.add(random.nextLong() & 0x7FFFFFFFFFFFFFFFL);
        }
        sparseKeys.addAll(uniqueKeys);
        Collections.shuffle(sparseKeys, random);
    }

    private long hashString(String s) {
        return s.hashCode() & 0x7FFFFFFFFFFFFFFFL;
    }

    private void populateOrderBook() {
        System.out.println("Populating " + implementation + " with " + datasetSize + " prefix-heavy orders...");
        
        for (int i = 0; i < datasetSize; i++) {
            long price = prefixKeys.get(i);
            OrderCommand cmd = new OrderCommand();
            cmd.orderId = i + 1;
            cmd.price = price;
            cmd.size = 100;
            cmd.action = (i % 2 == 0) ? OrderAction.BID : OrderAction.ASK;
            cmd.uid = i;
            cmd.timestamp = System.nanoTime();

            if (orderBook instanceof ArtOrderBook) {
                ((ArtOrderBook) orderBook).newOrder(cmd);
            } else {
                ((TreeSetOrderBook) orderBook).newOrder(cmd);
            }

            if (i % 100000 == 0 && i > 0) {
                System.gc();
                System.out.println("Populated " + i + " orders, memory: " + formatMemory(getUsedMemory()));
            }
        }
    }

    private long getUsedMemory() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        return heapUsage.getUsed();
    }

    private String formatMemory(long bytes) {
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }

    /**
     * Test prefix-heavy lookups under memory pressure
     */
    @Benchmark
    public void prefixHeavyLookups(Blackhole bh) {
        for (int i = 0; i < 1000; i++) {
            long price = prefixKeys.get(i % prefixKeys.size());
            
            OrderCommand cmd = new OrderCommand();
            cmd.orderId = datasetSize + i + 1;
            cmd.price = price;
            cmd.size = 50;
            cmd.action = OrderAction.BID;
            cmd.uid = datasetSize + i;
            cmd.timestamp = System.nanoTime();

            if (orderBook instanceof ArtOrderBook) {
                ((ArtOrderBook) orderBook).newOrder(cmd);
            } else {
                ((TreeSetOrderBook) orderBook).newOrder(cmd);
            }
            
            bh.consume(cmd);
        }
    }

    /**
     * Test sparse lookups under memory pressure
     */
    @Benchmark
    public void sparseLookups(Blackhole bh) {
        for (int i = 0; i < 1000; i++) {
            long price = sparseKeys.get(i % sparseKeys.size());
            
            OrderCommand cmd = new OrderCommand();
            cmd.orderId = datasetSize + 10000 + i + 1;
            cmd.price = price;
            cmd.size = 75;
            cmd.action = OrderAction.ASK;
            cmd.uid = datasetSize + 10000 + i;
            cmd.timestamp = System.nanoTime();

            if (orderBook instanceof ArtOrderBook) {
                ((ArtOrderBook) orderBook).newOrder(cmd);
            } else {
                ((TreeSetOrderBook) orderBook).newOrder(cmd);
            }
            
            bh.consume(cmd);
        }
    }

    /**
     * Test memory churn with frequent allocations/deallocations
     */
    @Benchmark
    public void memoryChurnOperations(Blackhole bh) {
        for (int i = 0; i < 500; i++) {
            // Insert operation
            long price = prefixKeys.get(random.nextInt(prefixKeys.size()));
            OrderCommand insertCmd = new OrderCommand();
            insertCmd.orderId = datasetSize + 20000 + i * 2 + 1;
            insertCmd.price = price;
            insertCmd.size = 100;
            insertCmd.action = OrderAction.BID;
            insertCmd.uid = datasetSize + 20000 + i * 2;
            insertCmd.timestamp = System.nanoTime();

            if (orderBook instanceof ArtOrderBook) {
                ((ArtOrderBook) orderBook).newOrder(insertCmd);
            } else {
                ((TreeSetOrderBook) orderBook).newOrder(insertCmd);
            }

            // Matching operation (causes removal)
            OrderCommand matchCmd = new OrderCommand();
            matchCmd.orderId = datasetSize + 20000 + i * 2 + 2;
            matchCmd.price = price;
            matchCmd.size = 150; // Larger size to trigger matching
            matchCmd.action = OrderAction.ASK;
            matchCmd.uid = datasetSize + 20000 + i * 2 + 1;
            matchCmd.timestamp = System.nanoTime();

            if (orderBook instanceof ArtOrderBook) {
                ((ArtOrderBook) orderBook).newOrder(matchCmd);
            } else {
                ((TreeSetOrderBook) orderBook).newOrder(matchCmd);
            }
            
            bh.consume(insertCmd);
            bh.consume(matchCmd);
        }
    }

    /**
     * Test high-frequency operations with prefix patterns
     */
    @Benchmark
    public void highFrequencyPrefixOperations(Blackhole bh) {
        // Simulate high-frequency trading with symbol-based clustering
        String[] hotSymbols = {"AAPL", "MSFT", "GOOGL"};
        
        for (int i = 0; i < 1000; i++) {
            String symbol = hotSymbols[i % hotSymbols.length];
            long basePrice = hashString(symbol);
            long price = basePrice + (i % 1000); // Small price variations
            
            OrderCommand cmd = new OrderCommand();
            cmd.orderId = datasetSize + 30000 + i + 1;
            cmd.price = price;
            cmd.size = 25;
            cmd.action = (i % 2 == 0) ? OrderAction.BID : OrderAction.ASK;
            cmd.uid = datasetSize + 30000 + i;
            cmd.timestamp = System.nanoTime();

            if (orderBook instanceof ArtOrderBook) {
                ((ArtOrderBook) orderBook).newOrder(cmd);
            } else {
                ((TreeSetOrderBook) orderBook).newOrder(cmd);
            }
            
            bh.consume(cmd);
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        System.gc();
        long finalMemory = getUsedMemory();
        System.out.println("Final memory usage for " + implementation + ": " + formatMemory(finalMemory));
    }
}
