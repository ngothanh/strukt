package com.submicro.strukt.benchmark;

import com.submicro.strukt.art.order.ArtOrderBook;
import com.submicro.strukt.art.order.OrderAction;
import com.submicro.strukt.art.order.OrderCommand;
import com.submicro.strukt.art.order.TreeSetOrderBook;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Large-scale benchmark suite designed to test ART's potential advantages with 1M+ keys.
 * Focuses on scenarios where ART's memory efficiency and prefix compression might shine.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 15, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 20, timeUnit = TimeUnit.SECONDS)
public class LargeScaleBenchmark {

    @Param({"1000000", "2000000", "5000000"})
    private int datasetSize;

    @Param({"ART", "TreeSet"})
    private String implementation;

    private Object orderBook;
    private List<Long> testKeys;
    private List<Long> sparseKeys;
    private List<Long> clusteredKeys;
    private List<Long> prefixKeys;
    private Random random;

    @Setup(Level.Trial)
    public void setupTrial() {
        random = new Random(42); // Fixed seed for reproducibility
        
        // Create implementation
        if ("ART".equals(implementation)) {
            orderBook = new ArtOrderBook();
        } else {
            orderBook = new TreeSetOrderBook();
        }

        // Generate different key patterns for testing ART's strengths
        generateTestKeys();
        
        // Pre-populate with base dataset
        populateOrderBook();
        
        System.gc(); // Clean up before benchmarks
    }

    private void generateTestKeys() {
        testKeys = new ArrayList<>(datasetSize);
        sparseKeys = new ArrayList<>(datasetSize / 10);
        clusteredKeys = new ArrayList<>(datasetSize / 10);
        prefixKeys = new ArrayList<>(datasetSize / 10);

        // Generate sparse keys (wide distribution)
        Set<Long> uniqueKeys = new HashSet<>();
        while (uniqueKeys.size() < datasetSize) {
            uniqueKeys.add(random.nextLong() & 0x7FFFFFFFFFFFFFFFL); // Positive longs
        }
        testKeys.addAll(uniqueKeys);
        Collections.shuffle(testKeys, random);

        // Generate sparse lookup keys (10% of dataset)
        for (int i = 0; i < datasetSize / 10; i++) {
            sparseKeys.add(testKeys.get(random.nextInt(testKeys.size())));
        }

        // Generate clustered keys (keys with similar prefixes)
        long basePrefix = 0x1000000000000000L;
        for (int i = 0; i < datasetSize / 10; i++) {
            clusteredKeys.add(basePrefix + random.nextInt(1000000));
        }

        // Generate prefix-heavy keys (ART's potential strength)
        String[] prefixes = {"AAPL", "MSFT", "GOOGL", "AMZN", "TSLA"};
        for (int i = 0; i < datasetSize / 10; i++) {
            String prefix = prefixes[i % prefixes.length];
            long key = hashString(prefix + String.format("%08d", i));
            prefixKeys.add(key);
        }
    }

    private long hashString(String s) {
        return s.hashCode() & 0x7FFFFFFFFFFFFFFFL;
    }

    private void populateOrderBook() {
        System.out.println("Populating " + implementation + " with " + datasetSize + " orders...");
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < datasetSize; i++) {
            long price = testKeys.get(i);
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

            if (i % 100000 == 0) {
                System.out.println("Populated " + i + " orders...");
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("Population completed in " + duration + "ms");
    }

    /**
     * Test random lookups in very large dataset - ART might have advantage due to memory efficiency
     */
    @Benchmark
    public void scenario1_massiveRandomLookup(Blackhole bh) {
        for (int i = 0; i < 1000; i++) {
            long price = sparseKeys.get(i % sparseKeys.size());
            
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
     * Test prefix-heavy operations - ART's potential strength
     */
    @Benchmark
    public void scenario2_prefixHeavyOperations(Blackhole bh) {
        for (int i = 0; i < 1000; i++) {
            long price = prefixKeys.get(i % prefixKeys.size());
            
            OrderCommand cmd = new OrderCommand();
            cmd.orderId = datasetSize + 10000 + i + 1;
            cmd.price = price;
            cmd.size = 75;
            cmd.action = (i % 2 == 0) ? OrderAction.ASK : OrderAction.BID;
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
     * Test clustered key operations - potential ART advantage
     */
    @Benchmark
    public void scenario3_clusteredKeyOperations(Blackhole bh) {
        for (int i = 0; i < 1000; i++) {
            long price = clusteredKeys.get(i % clusteredKeys.size());
            
            OrderCommand cmd = new OrderCommand();
            cmd.orderId = datasetSize + 20000 + i + 1;
            cmd.price = price;
            cmd.size = 125;
            cmd.action = OrderAction.BID;
            cmd.uid = datasetSize + 20000 + i;
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
     * Test memory-intensive operations with large dataset
     */
    @Benchmark
    public void scenario4_memoryIntensiveOperations(Blackhole bh) {
        // Perform operations that stress memory usage
        for (int i = 0; i < 500; i++) {
            long price = testKeys.get(random.nextInt(testKeys.size()));
            
            // Insert
            OrderCommand insertCmd = new OrderCommand();
            insertCmd.orderId = datasetSize + 30000 + i * 2 + 1;
            insertCmd.price = price;
            insertCmd.size = 200;
            insertCmd.action = OrderAction.ASK;
            insertCmd.uid = datasetSize + 30000 + i * 2;
            insertCmd.timestamp = System.nanoTime();

            if (orderBook instanceof ArtOrderBook) {
                ((ArtOrderBook) orderBook).newOrder(insertCmd);
            } else {
                ((TreeSetOrderBook) orderBook).newOrder(insertCmd);
            }

            // Lookup
            long lookupPrice = testKeys.get(random.nextInt(testKeys.size()));
            OrderCommand lookupCmd = new OrderCommand();
            lookupCmd.orderId = datasetSize + 30000 + i * 2 + 2;
            lookupCmd.price = lookupPrice;
            lookupCmd.size = 50;
            lookupCmd.action = OrderAction.BID;
            lookupCmd.uid = datasetSize + 30000 + i * 2 + 1;
            lookupCmd.timestamp = System.nanoTime();

            if (orderBook instanceof ArtOrderBook) {
                ((ArtOrderBook) orderBook).newOrder(lookupCmd);
            } else {
                ((TreeSetOrderBook) orderBook).newOrder(lookupCmd);
            }
            
            bh.consume(insertCmd);
            bh.consume(lookupCmd);
        }
    }
}
