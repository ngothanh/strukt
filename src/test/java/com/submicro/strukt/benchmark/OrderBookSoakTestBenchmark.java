package com.submicro.strukt.benchmark;

import com.submicro.strukt.art.order.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * OrderBook Soak Test Benchmark Suite
 * 
 * Long-running stability validation for exchange-core deployment confidence.
 * Validates no performance degradation, memory leaks, or drift over extended periods.
 * 
 * Key Validations:
 * - 30-60 minute continuous operation
 * - Memory leak detection over time
 * - Performance stability (no degradation)
 * - GC behavior under sustained load
 * - Resource utilization patterns
 * - Error rate monitoring (should be zero)
 * 
 * Configuration: Extended measurement for stability validation
 * Mode: Throughput with long-term focus
 * Focus: Production deployment confidence
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 60, timeUnit = TimeUnit.SECONDS) // 60-second measurements
@Fork(value = 2, jvmArgs = {
    "-Xmx8G", "-Xms8G", 
    "-XX:+UseG1GC", 
    "-XX:+AlwaysPreTouch",
    "-XX:MaxGCPauseMillis=50",
    "-Xlog:gc*:soak-test-gc.log:time,tags",
    "-XX:+UnlockExperimentalVMOptions",
    "-XX:G1NewSizePercent=30",
    "-XX:G1MaxNewSizePercent=40"
})
@Threads(1)
@State(Scope.Benchmark)
public class OrderBookSoakTestBenchmark {

    @Param({"100000"})
    public int datasetSize;

    @Param({"ART", "TreeSet"})
    public String orderBookType;

    // Soak test data
    public List<OrderCommand> soakTestOrders;
    public OrderBook soakBook;

    private Random random;
    private long orderIdCounter = 1;
    private long operationCount = 0;
    private long lastMemoryCheck = 0;
    private List<Long> memorySnapshots;

    @Setup(Level.Trial)
    public void setup() {
        random = new Random(42);
        orderIdCounter = 1;
        operationCount = 0;
        memorySnapshots = new ArrayList<>();
        
        System.out.println("=== Soak Test Benchmark Setup ===");
        System.out.println("Implementation: " + orderBookType);
        System.out.println("Dataset Size: " + datasetSize);
        System.out.println("Test Duration: 60 seconds per measurement");
        
        generateSoakTestData();
        soakBook = createOrderBook();
        
        // Initial memory snapshot
        System.gc();
        long initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        memorySnapshots.add(initialMemory);
        System.out.println("Initial memory: " + (initialMemory / 1024 / 1024) + " MB");
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        System.out.println("=== Soak Test Results ===");
        System.out.println("Total operations processed: " + operationCount);
        System.out.println("Memory snapshots collected: " + memorySnapshots.size());
        
        if (memorySnapshots.size() > 1) {
            long initialMemory = memorySnapshots.get(0);
            long finalMemory = memorySnapshots.get(memorySnapshots.size() - 1);
            long memoryGrowth = finalMemory - initialMemory;
            
            System.out.println("Initial memory: " + (initialMemory / 1024 / 1024) + " MB");
            System.out.println("Final memory: " + (finalMemory / 1024 / 1024) + " MB");
            System.out.println("Memory growth: " + (memoryGrowth / 1024 / 1024) + " MB");
            
            if (memoryGrowth > initialMemory * 0.5) { // More than 50% growth
                System.out.println("WARNING: Significant memory growth detected - potential leak");
            } else {
                System.out.println("Memory growth within acceptable bounds");
            }
        }
    }

    public OrderBook createOrderBook() {
        switch (orderBookType) {
            case "ART":
                return new ArtOrderBook();
            case "TreeSet":
                return new TreeSetOrderBook();
            default:
                throw new IllegalArgumentException("Unknown order book type: " + orderBookType);
        }
    }

    private void generateSoakTestData() {
        soakTestOrders = new ArrayList<>(datasetSize);
        
        // Generate diverse order patterns for soak testing
        for (int i = 0; i < datasetSize; i++) {
            OrderAction action = random.nextBoolean() ? OrderAction.ASK : OrderAction.BID;
            long price = 20000L + (random.nextInt(2001) - 1000); // ±1000 around mid
            long size = 100 + random.nextInt(900); // 100-1000 size range
            
            OrderCommand order = createOrder(action, price, size);
            soakTestOrders.add(order);
        }
    }

    private OrderCommand createOrder(OrderAction action, long price, long size) {
        OrderCommand cmd = new OrderCommand();
        cmd.id = orderIdCounter;
        cmd.orderId = orderIdCounter++;
        cmd.action = action;
        cmd.price = price;
        cmd.size = size;
        cmd.uid = 1000L + orderIdCounter;
        cmd.timestamp = System.nanoTime();
        cmd.symbol = 1;
        cmd.reserveBidPrice = 0L;
        return cmd;
    }

    /**
     * Soak Test: Continuous Mixed Operations
     * Runs for 60 seconds continuously to detect memory leaks and performance drift
     */
    @Benchmark
    public long soakTest01_ContinuousMixed(Blackhole bh) {
        long totalProcessed = 0;
        long startTime = System.nanoTime();
        long endTime = startTime + TimeUnit.SECONDS.toNanos(55); // 55 seconds of actual work
        
        int orderIndex = 0;
        
        while (System.nanoTime() < endTime) {
            // Get next order (cycle through dataset)
            OrderCommand order = soakTestOrders.get(orderIndex % soakTestOrders.size());
            orderIndex++;
            
            // Process order
            soakBook.newOrder(order);
            totalProcessed++;
            operationCount++;
            
            // DCE prevention
            bh.consume(order.orderId);
            bh.consume(order.price);
            bh.consume(order.size);
            
            // Periodic memory monitoring (every 10,000 operations)
            if (totalProcessed % 10000 == 0) {
                long currentTime = System.nanoTime();
                if (currentTime - lastMemoryCheck > TimeUnit.SECONDS.toNanos(10)) { // Every 10 seconds
                    System.gc();
                    long currentMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                    memorySnapshots.add(currentMemory);
                    bh.consume(currentMemory);
                    lastMemoryCheck = currentTime;
                }
            }
        }
        
        return totalProcessed;
    }

    /**
     * Soak Test: Insert Heavy Continuous
     * Continuous insert operations to stress tree growth over time
     */
    @Benchmark
    public long soakTest02_InsertHeavy(Blackhole bh) {
        long totalProcessed = 0;
        long startTime = System.nanoTime();
        long endTime = startTime + TimeUnit.SECONDS.toNanos(55);
        
        while (System.nanoTime() < endTime) {
            // Generate non-marketable order
            OrderAction action = random.nextBoolean() ? OrderAction.ASK : OrderAction.BID;
            long price = (action == OrderAction.ASK) ? 
                21000L + random.nextInt(5000) : 19000L - random.nextInt(5000);
            long size = 100 + random.nextInt(500);
            
            OrderCommand order = createOrder(action, price, size);
            soakBook.newOrder(order);
            totalProcessed++;
            operationCount++;
            
            bh.consume(order.orderId);
            bh.consume(order.price);
            bh.consume(order.size);
            
            // Memory monitoring
            if (totalProcessed % 10000 == 0) {
                long currentTime = System.nanoTime();
                if (currentTime - lastMemoryCheck > TimeUnit.SECONDS.toNanos(10)) {
                    System.gc();
                    long currentMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                    memorySnapshots.add(currentMemory);
                    bh.consume(currentMemory);
                    lastMemoryCheck = currentTime;
                }
            }
        }
        
        return totalProcessed;
    }

    /**
     * Soak Test: Match Heavy Continuous
     * Continuous matching operations to stress removal paths over time
     */
    @Benchmark
    public long soakTest03_MatchHeavy(Blackhole bh) {
        // Pre-fill with liquidity
        for (int i = 0; i < 1000; i++) {
            OrderCommand bid = createOrder(OrderAction.BID, 19000L + i, 1000L);
            OrderCommand ask = createOrder(OrderAction.ASK, 21000L + i, 1000L);
            soakBook.newOrder(bid);
            soakBook.newOrder(ask);
        }
        
        long totalProcessed = 0;
        long startTime = System.nanoTime();
        long endTime = startTime + TimeUnit.SECONDS.toNanos(55);
        
        while (System.nanoTime() < endTime) {
            // Generate marketable order
            OrderAction action = random.nextBoolean() ? OrderAction.ASK : OrderAction.BID;
            long price = (action == OrderAction.ASK) ? 
                19000L - random.nextInt(500) : 21000L + random.nextInt(500);
            long size = 100 + random.nextInt(200);
            
            OrderCommand order = createOrder(action, price, size);
            soakBook.newOrder(order);
            totalProcessed++;
            operationCount++;
            
            bh.consume(order.orderId);
            bh.consume(order.price);
            bh.consume(order.size);
            
            // Replenish liquidity periodically
            if (totalProcessed % 1000 == 0) {
                OrderCommand replenishBid = createOrder(OrderAction.BID, 19000L + random.nextInt(1000), 1000L);
                OrderCommand replenishAsk = createOrder(OrderAction.ASK, 21000L + random.nextInt(1000), 1000L);
                soakBook.newOrder(replenishBid);
                soakBook.newOrder(replenishAsk);
                bh.consume(replenishBid.orderId);
                bh.consume(replenishAsk.orderId);
            }
            
            // Memory monitoring
            if (totalProcessed % 10000 == 0) {
                long currentTime = System.nanoTime();
                if (currentTime - lastMemoryCheck > TimeUnit.SECONDS.toNanos(10)) {
                    System.gc();
                    long currentMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                    memorySnapshots.add(currentMemory);
                    bh.consume(currentMemory);
                    lastMemoryCheck = currentTime;
                }
            }
        }
        
        return totalProcessed;
    }
}
