package com.submicro.strukt.benchmark;

import com.submicro.strukt.art.order.OrderAction;
import com.submicro.strukt.art.order.OrderCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Utility class for generating various types of test data for OrderBook benchmarks.
 * Supports different key distribution patterns and operation mixes.
 */
public class BenchmarkDataGenerator {
    
    private final Random random;
    private long orderIdCounter = 1;
    private long uidCounter = 1000;
    
    public BenchmarkDataGenerator(long seed) {
        this.random = new Random(seed);
    }
    
    /**
     * Generate sequential price orders (1, 2, 3, 4, 5...)
     */
    public List<OrderCommand> generateSequentialOrders(int count, OrderAction action) {
        List<OrderCommand> orders = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            orders.add(createOrder(action, i + 1, randomSize()));
        }
        return orders;
    }
    
    /**
     * Generate random sparse orders using Random.nextLong()
     */
    public List<OrderCommand> generateRandomSparseOrders(int count, OrderAction action) {
        List<OrderCommand> orders = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long price = Math.abs(random.nextLong() % 1_000_000) + 1; // Keep prices reasonable
            orders.add(createOrder(action, price, randomSize()));
        }
        return orders;
    }
    
    /**
     * Generate clustered orders (1000-1999, 5000-5999, 10000-10999...)
     */
    public List<OrderCommand> generateClusteredOrders(int count, OrderAction action) {
        List<OrderCommand> orders = new ArrayList<>(count);
        int clustersNeeded = (count + 999) / 1000; // Round up
        
        for (int cluster = 0; cluster < clustersNeeded; cluster++) {
            long basePrice = (cluster * 4000) + 1000; // Clusters at 1000, 5000, 9000, etc.
            int ordersInCluster = Math.min(1000, count - (cluster * 1000));
            
            for (int i = 0; i < ordersInCluster; i++) {
                long price = basePrice + i;
                orders.add(createOrder(action, price, randomSize()));
            }
        }
        return orders;
    }
    
    /**
     * Generate prefix-shared orders (0x1234xxxx, 0x5678xxxx patterns)
     */
    public List<OrderCommand> generatePrefixSharedOrders(int count, OrderAction action) {
        List<OrderCommand> orders = new ArrayList<>(count);
        long[] prefixes = {0x1234_0000L, 0x5678_0000L, 0xABCD_0000L, 0xEF01_0000L};
        
        for (int i = 0; i < count; i++) {
            long prefix = prefixes[i % prefixes.length];
            long suffix = random.nextInt(0xFFFF) + 1; // Avoid zero prices
            long price = prefix | suffix;
            orders.add(createOrder(action, price, randomSize()));
        }
        return orders;
    }
    
    /**
     * Generate mixed operation workload
     */
    public List<OrderCommand> generateMixedWorkload(int totalOps, double readRatio, double writeRatio, double removeRatio) {
        List<OrderCommand> operations = new ArrayList<>(totalOps);
        
        // First, populate with some initial orders
        int initialOrders = Math.min(1000, totalOps / 10);
        for (int i = 0; i < initialOrders; i++) {
            OrderAction action = random.nextBoolean() ? OrderAction.ASK : OrderAction.BID;
            long price = random.nextInt(1000) + 100; // Prices 100-1099
            operations.add(createOrder(action, price, randomSize()));
        }
        
        // Generate mixed operations based on ratios
        for (int i = initialOrders; i < totalOps; i++) {
            double opType = random.nextDouble();
            OrderAction action = random.nextBoolean() ? OrderAction.ASK : OrderAction.BID;
            
            if (opType < readRatio) {
                // Read operation - create an order that will likely match existing ones
                long price = random.nextInt(1000) + 100;
                operations.add(createOrder(action, price, 1)); // Small size for quick matching
            } else if (opType < readRatio + writeRatio) {
                // Write operation - create an order that won't match (different price range)
                long price = random.nextInt(1000) + 2000; // Higher price range
                operations.add(createOrder(action, price, randomSize()));
            } else {
                // Remove operation - create a matching order to remove existing ones
                long price = random.nextInt(1000) + 100;
                operations.add(createOrder(action.opposite(), price, randomSize()));
            }
        }
        
        return operations;
    }
    
    /**
     * Generate orders for hotspot access pattern (80% reads from 20% of keys)
     */
    public List<OrderCommand> generateHotspotOrders(int count, OrderAction action) {
        List<OrderCommand> orders = new ArrayList<>(count);
        
        // 20% of price range (hot prices)
        long hotPriceStart = 1000;
        long hotPriceEnd = 1200;
        
        // 80% of price range (cold prices)  
        long coldPriceStart = 1200;
        long coldPriceEnd = 2000;
        
        for (int i = 0; i < count; i++) {
            long price;
            if (random.nextDouble() < 0.8) {
                // 80% of operations target hot prices (20% of price range)
                price = hotPriceStart + random.nextInt((int)(hotPriceEnd - hotPriceStart));
            } else {
                // 20% of operations target cold prices (80% of price range)
                price = coldPriceStart + random.nextInt((int)(coldPriceEnd - coldPriceStart));
            }
            orders.add(createOrder(action, price, randomSize()));
        }
        
        return orders;
    }
    
    /**
     * Generate orders for range query testing
     */
    public List<OrderCommand> generateRangeQueryOrders(int count, OrderAction action) {
        List<OrderCommand> orders = new ArrayList<>(count);
        
        // Generate orders with well-distributed prices for range queries
        for (int i = 0; i < count; i++) {
            long price = (i * 10) + 1000; // Prices: 1000, 1010, 1020, etc.
            orders.add(createOrder(action, price, randomSize()));
        }
        
        return orders;
    }
    
    private OrderCommand createOrder(OrderAction action, long price, long size) {
        OrderCommand cmd = new OrderCommand();
        cmd.id = orderIdCounter;
        cmd.orderId = orderIdCounter++;
        cmd.action = action;
        cmd.price = price;
        cmd.size = size;
        cmd.uid = uidCounter++;
        cmd.timestamp = System.currentTimeMillis();
        cmd.reserveBidPrice = 0L;
        cmd.symbol = 1;
        return cmd;
    }
    
    private long randomSize() {
        return random.nextInt(100) + 1; // Size between 1-100
    }
    
    /**
     * Reset counters for consistent test runs
     */
    public void reset() {
        orderIdCounter = 1;
        uidCounter = 1000;
    }
}
