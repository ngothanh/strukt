package com.submicro.strukt.benchmark;

import com.submicro.strukt.art.order.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive OrderBook Benchmark Suite
 * 
 * Implements rigorous JMH methodology following all established principles:
 * - 5 forks for statistical validity across JVMs
 * - Adequate warmup (5×2s) and measurement (10×2s) for C1→C2 settling
 * - AlwaysPreTouch for memory stability
 * - Proper DCE prevention with return values + Blackhole consumption
 * - Deterministic data generation with fixed RNG seed
 * - Multiple dataset sizes: 10K, 100K, 1M orders
 * - Comprehensive scenario coverage for realistic trading patterns
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(value = 5, jvmArgs = {"-Xmx8G", "-Xms8G", "-XX:+UseG1GC", "-XX:+AlwaysPreTouch"})
@Threads(1)
@State(Scope.Benchmark)
public class OrderBookBenchmarkSuite {

    // Test parameters - varied dataset sizes for scaling analysis
    @Param({"10000", "100000", "1000000"})
    public int datasetSize;

    @Param({"ART", "TreeSet"})
    public String orderBookType;

    // Benchmark configuration constants (reproducible across runs)
    public static final long RNG_SEED = 42L;
    public static final long TICK_SIZE = 1L;
    public static final long PRICE_MID = 20000L;
    public static final long PRICE_SPREAD = 10000L; // ±5000 from mid
    public static final int HOTSPOT_CONCENTRATION = 80; // 80% concentration
    public static final int SPARSE_LEVEL_SPACING = 128; // Every 128 ticks

    // Test data for all scenarios
    public List<OrderCommand> pureInsertOrders;
    public List<OrderCommand> pureMatchOrders;
    public List<OrderCommand> partialMatchOrders;
    public List<OrderCommand> randomMixOrders;
    public List<OrderCommand> hotspotSinglePriceOrders;
    public List<OrderCommand> hotspotNarrowBandOrders;
    public List<OrderCommand> coldBookSparseOrders;
    public List<OrderCommand> wideDenseBookOrders;
    public List<OrderCommand> duplicateSubmitOrders;

    // Pre-fill data for scenarios requiring initial state
    public List<OrderCommand> preFillDeepLiquidityBids;
    public List<OrderCommand> preFillDeepLiquidityAsks;
    public List<OrderCommand> preFillSmallOrders;
    public List<OrderCommand> preFillMixedBook;
    public List<OrderCommand> preFillSparseBook;
    public List<OrderCommand> preFillDenseBook;

    private Random random;
    private long orderIdCounter = 1;

    @Setup(Level.Trial)
    public void setup() {
        // Fixed seed for reproducibility across runs and implementations
        random = new Random(RNG_SEED);
        orderIdCounter = 1;
        
        System.out.println("=== OrderBook Benchmark Setup ===");
        System.out.println("Implementation: " + orderBookType);
        System.out.println("Dataset Size: " + datasetSize);
        System.out.println("RNG Seed: " + RNG_SEED);
        System.out.println("JVM: " + System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version"));
        
        // Generate all test data
        generateAllTestData();
        
        System.out.println("Test data generation completed.");
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

    private void generateAllTestData() {
        // Core scenario data generation
        pureInsertOrders = generatePureInsertOrders();
        pureMatchOrders = generatePureMatchOrders();
        partialMatchOrders = generatePartialMatchOrders();
        randomMixOrders = generateRandomMixOrders();
        hotspotSinglePriceOrders = generateHotspotSinglePriceOrders();
        hotspotNarrowBandOrders = generateHotspotNarrowBandOrders();
        coldBookSparseOrders = generateColdBookSparseOrders();
        wideDenseBookOrders = generateWideDenseBookOrders();
        duplicateSubmitOrders = generateDuplicateSubmitOrders();
        
        // Pre-fill data generation
        preFillDeepLiquidityBids = generateDeepLiquidityBids();
        preFillDeepLiquidityAsks = generateDeepLiquidityAsks();
        preFillSmallOrders = generateSmallOrders();
        preFillMixedBook = generateMixedBookPrefill();
        preFillSparseBook = generateSparseBookPrefill();
        preFillDenseBook = generateDenseBookPrefill();
    }

    // ========== SCENARIO 1: PURE INSERT ==========
    /**
     * Pure Insert Scenario - Tests order placement without matches.
     * Stream: 100% orders that cannot match (prices outside spread)
     * Goals: Price-level insert cost, bucket DLL append, best-pointer updates, ART/TreeMap path length
     */
    @Benchmark
    public long scenario01_PureInsert(Blackhole bh) {
        OrderBook book = createOrderBook();
        long totalProcessed = 0;
        
        for (OrderCommand order : pureInsertOrders) {
            book.newOrder(order);
            totalProcessed++;
            // DCE prevention: consume order details
            bh.consume(order.orderId);
            bh.consume(order.price);
            bh.consume(order.size);
        }
        
        return totalProcessed;
    }

    private List<OrderCommand> generatePureInsertOrders() {
        List<OrderCommand> orders = new ArrayList<>(datasetSize);
        
        // Generate non-overlapping price ranges to ensure no matches
        // ASK orders: PRICE_MID + PRICE_SPREAD/2 and above
        // BID orders: PRICE_MID - PRICE_SPREAD/2 and below
        for (int i = 0; i < datasetSize; i++) {
            if (i % 2 == 0) {
                // ASK orders above spread
                long price = PRICE_MID + PRICE_SPREAD/2 + random.nextInt((int)PRICE_SPREAD);
                orders.add(createOrder(OrderAction.ASK, price, generateLogNormalSize()));
            } else {
                // BID orders below spread
                long price = PRICE_MID - PRICE_SPREAD/2 - random.nextInt((int)PRICE_SPREAD);
                orders.add(createOrder(OrderAction.BID, price, generateLogNormalSize()));
            }
        }
        
        return orders;
    }

    // ========== SCENARIO 2: PURE MATCH ==========
    /**
     * Pure Match Scenario - Tests order matching against pre-filled book.
     * Prefill: Deep liquidity on opposite side
     * Stream: Orders that always cross and fully fill
     * Goals: Hot removal path, best handoff at same price, price-level deletion cost
     */
    @Benchmark
    public long scenario02_PureMatch(Blackhole bh) {
        OrderBook book = createOrderBook();
        
        // Pre-fill with deep liquidity
        for (OrderCommand order : preFillDeepLiquidityBids) {
            book.newOrder(order);
        }
        for (OrderCommand order : preFillDeepLiquidityAsks) {
            book.newOrder(order);
        }
        
        long totalProcessed = 0;
        for (OrderCommand order : pureMatchOrders) {
            book.newOrder(order);
            totalProcessed++;
            bh.consume(order.orderId);
            bh.consume(order.price);
            bh.consume(order.size);
        }
        
        return totalProcessed;
    }

    private List<OrderCommand> generatePureMatchOrders() {
        List<OrderCommand> orders = new ArrayList<>(datasetSize);
        
        // Generate orders that will always match against pre-filled liquidity
        for (int i = 0; i < datasetSize; i++) {
            if (i % 2 == 0) {
                // ASK orders that cross bid spread (aggressive sells)
                long price = PRICE_MID - PRICE_SPREAD/4 - random.nextInt((int)PRICE_SPREAD/4);
                orders.add(createOrder(OrderAction.ASK, price, 50 + random.nextInt(150)));
            } else {
                // BID orders that cross ask spread (aggressive buys)
                long price = PRICE_MID + PRICE_SPREAD/4 + random.nextInt((int)PRICE_SPREAD/4);
                orders.add(createOrder(OrderAction.BID, price, 50 + random.nextInt(150)));
            }
        }
        
        return orders;
    }

    private List<OrderCommand> generateDeepLiquidityBids() {
        List<OrderCommand> orders = new ArrayList<>(datasetSize / 4);
        
        // Large bid orders for matching scenarios
        for (int i = 0; i < datasetSize / 4; i++) {
            long price = PRICE_MID - PRICE_SPREAD/4 + random.nextInt((int)PRICE_SPREAD/8);
            orders.add(createOrder(OrderAction.BID, price, 1000 + random.nextInt(2000)));
        }
        
        return orders;
    }

    private List<OrderCommand> generateDeepLiquidityAsks() {
        List<OrderCommand> orders = new ArrayList<>(datasetSize / 4);
        
        // Large ask orders for matching scenarios
        for (int i = 0; i < datasetSize / 4; i++) {
            long price = PRICE_MID + PRICE_SPREAD/4 + random.nextInt((int)PRICE_SPREAD/8);
            orders.add(createOrder(OrderAction.ASK, price, 1000 + random.nextInt(2000)));
        }
        
        return orders;
    }

    // ========== SCENARIO 3: PARTIAL MATCH ==========
    /**
     * Partial Match Scenario - Tests orders that partially match and insert remainder.
     * Prefill: Many small orders at multiple prices
     * Stream: Larger orders that partially consume several price levels, leaving remainder
     * Goals: Mixed hot paths (match + insert), pointer maintenance, bucket churn
     */
    @Benchmark
    public long scenario03_PartialMatch(Blackhole bh) {
        OrderBook book = createOrderBook();
        
        // Pre-fill with small orders for partial matching
        for (OrderCommand order : preFillSmallOrders) {
            book.newOrder(order);
        }
        
        long totalProcessed = 0;
        for (OrderCommand order : partialMatchOrders) {
            book.newOrder(order);
            totalProcessed++;
            bh.consume(order.orderId);
            bh.consume(order.price);
            bh.consume(order.size);
        }
        
        return totalProcessed;
    }

    private List<OrderCommand> generatePartialMatchOrders() {
        List<OrderCommand> orders = new ArrayList<>(datasetSize);
        
        // Generate larger orders that will partially match against small pre-filled orders
        for (int i = 0; i < datasetSize; i++) {
            if (i % 2 == 0) {
                // Large ASK orders
                long price = PRICE_MID - PRICE_SPREAD/8 + random.nextInt((int)PRICE_SPREAD/4);
                orders.add(createOrder(OrderAction.ASK, price, 500 + random.nextInt(1500)));
            } else {
                // Large BID orders
                long price = PRICE_MID - PRICE_SPREAD/8 + random.nextInt((int)PRICE_SPREAD/4);
                orders.add(createOrder(OrderAction.BID, price, 500 + random.nextInt(1500)));
            }
        }
        
        return orders;
    }

    private List<OrderCommand> generateSmallOrders() {
        List<OrderCommand> orders = new ArrayList<>(datasetSize / 2);
        
        // Small orders for partial matching scenarios
        for (int i = 0; i < datasetSize / 2; i++) {
            if (i % 2 == 0) {
                long price = PRICE_MID - PRICE_SPREAD/8 + random.nextInt((int)PRICE_SPREAD/4);
                orders.add(createOrder(OrderAction.BID, price, 50 + random.nextInt(100)));
            } else {
                long price = PRICE_MID - PRICE_SPREAD/8 + random.nextInt((int)PRICE_SPREAD/4);
                orders.add(createOrder(OrderAction.ASK, price, 50 + random.nextInt(100)));
            }
        }
        
        return orders;
    }

    // ========== UTILITY METHODS ==========
    
    /**
     * Generate log-normal distributed order sizes (captures realistic heavy tail)
     */
    private long generateLogNormalSize() {
        double logNormal = Math.exp(random.nextGaussian() * 0.5 + 4.0);
        return Math.max(1, Math.round(logNormal));
    }
    
    /**
     * Create order with proper field initialization
     */
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

    // ========== SCENARIO 4: RANDOM MIX ==========
    /**
     * Random Mix Scenario - Tests realistic mix of insert and match operations.
     * Stream: 50% inserts, 40% marketable, 10% at spread; 52% BID / 48% ASK
     * Goals: Realistic blend; average case performance
     */
    @Benchmark
    public long scenario04_RandomMix(Blackhole bh) {
        OrderBook book = createOrderBook();

        // Pre-fill with mixed book to create realistic environment
        for (OrderCommand order : preFillMixedBook) {
            book.newOrder(order);
        }

        long totalProcessed = 0;
        for (OrderCommand order : randomMixOrders) {
            book.newOrder(order);
            totalProcessed++;
            bh.consume(order.orderId);
            bh.consume(order.price);
            bh.consume(order.size);
        }

        return totalProcessed;
    }

    private List<OrderCommand> generateRandomMixOrders() {
        List<OrderCommand> orders = new ArrayList<>(datasetSize);

        // 50% inserts, 40% marketable, 10% at spread
        for (int i = 0; i < datasetSize; i++) {
            double rand = random.nextDouble();
            // 52% BID / 48% ASK ratio
            OrderAction action = random.nextDouble() < 0.52 ? OrderAction.BID : OrderAction.ASK;

            if (rand < 0.5) {
                // 50% inserts (non-marketable)
                long price = generateNonMarketablePrice(action);
                orders.add(createOrder(action, price, generateLogNormalSize()));
            } else if (rand < 0.9) {
                // 40% marketable
                long price = generateMarketablePrice(action);
                orders.add(createOrder(action, price, generateLogNormalSize()));
            } else {
                // 10% at spread
                long price = action == OrderAction.ASK ? PRICE_MID + 1 : PRICE_MID - 1;
                orders.add(createOrder(action, price, generateLogNormalSize()));
            }
        }

        return orders;
    }

    private long generateNonMarketablePrice(OrderAction action) {
        if (action == OrderAction.ASK) {
            return PRICE_MID + 100 + random.nextInt((int)PRICE_SPREAD/2);
        } else {
            return PRICE_MID - 100 - random.nextInt((int)PRICE_SPREAD/2);
        }
    }

    private long generateMarketablePrice(OrderAction action) {
        if (action == OrderAction.ASK) {
            return PRICE_MID - 50 - random.nextInt(50);
        } else {
            return PRICE_MID + 50 + random.nextInt(50);
        }
    }

    private List<OrderCommand> generateMixedBookPrefill() {
        List<OrderCommand> orders = new ArrayList<>(200);

        // Create realistic spread with normal distribution around mid
        for (int i = 0; i < 100; i++) {
            // Bid side - normal distribution below mid
            long bidPrice = PRICE_MID - 10 - Math.abs((long)(random.nextGaussian() * 50));
            orders.add(createOrder(OrderAction.BID, bidPrice, 100 + random.nextInt(300)));

            // Ask side - normal distribution above mid
            long askPrice = PRICE_MID + 10 + Math.abs((long)(random.nextGaussian() * 50));
            orders.add(createOrder(OrderAction.ASK, askPrice, 100 + random.nextInt(300)));
        }

        return orders;
    }

    // ========== SCENARIO 5: HOTSPOT SINGLE PRICE ==========
    /**
     * Hotspot Single Price Scenario - Tests performance when orders concentrate at specific price.
     * Stream: ≥80% of orders at one price (the top), rest ±1 tick
     * Goals: Bucket DLL pressure, same-price turnover; how well best handoff avoids tree scans
     */
    @Benchmark
    public long scenario05_HotspotSinglePrice(Blackhole bh) {
        OrderBook book = createOrderBook();
        long totalProcessed = 0;

        for (OrderCommand order : hotspotSinglePriceOrders) {
            book.newOrder(order);
            totalProcessed++;
            bh.consume(order.orderId);
            bh.consume(order.price);
            bh.consume(order.size);
        }

        return totalProcessed;
    }

    private List<OrderCommand> generateHotspotSinglePriceOrders() {
        List<OrderCommand> orders = new ArrayList<>(datasetSize);
        long hotspotPrice = PRICE_MID;

        for (int i = 0; i < datasetSize; i++) {
            boolean isHotspot = random.nextInt(100) < HOTSPOT_CONCENTRATION;
            OrderAction action = random.nextBoolean() ? OrderAction.ASK : OrderAction.BID;

            if (isHotspot) {
                // 80% at hotspot price
                orders.add(createOrder(action, hotspotPrice, generateLogNormalSize()));
            } else {
                // 20% scattered around ±1 tick
                long price = hotspotPrice + (random.nextInt(3) - 1) * TICK_SIZE;
                orders.add(createOrder(action, price, generateLogNormalSize()));
            }
        }

        return orders;
    }

    // ========== SCENARIO 6: HOTSPOT NARROW BAND ==========
    /**
     * Hotspot Narrow Band Scenario - Tests performance with concentrated price-level activity.
     * Stream: ≥80% within a ±2-tick band; rapid price-level create/remove churn
     * Goals: Price-level churn (insert/remove keys) throughput and latency
     */
    @Benchmark
    public long scenario06_HotspotNarrowBand(Blackhole bh) {
        OrderBook book = createOrderBook();
        long totalProcessed = 0;

        for (OrderCommand order : hotspotNarrowBandOrders) {
            book.newOrder(order);
            totalProcessed++;
            bh.consume(order.orderId);
            bh.consume(order.price);
            bh.consume(order.size);
        }

        return totalProcessed;
    }

    private List<OrderCommand> generateHotspotNarrowBandOrders() {
        List<OrderCommand> orders = new ArrayList<>(datasetSize);
        long bandCenter = PRICE_MID;
        int bandWidth = 4; // ±2 ticks

        for (int i = 0; i < datasetSize; i++) {
            boolean isInBand = random.nextInt(100) < HOTSPOT_CONCENTRATION;
            OrderAction action = random.nextBoolean() ? OrderAction.ASK : OrderAction.BID;

            if (isInBand) {
                // 80% within narrow band
                long price = bandCenter + (random.nextInt(bandWidth + 1) - bandWidth/2) * TICK_SIZE;
                orders.add(createOrder(action, price, generateLogNormalSize()));
            } else {
                // 20% outside band
                long price = bandCenter + (random.nextInt(1000) - 500) * TICK_SIZE;
                orders.add(createOrder(action, price, generateLogNormalSize()));
            }
        }

        return orders;
    }

    // ========== SCENARIO 7: COLD BOOK SPARSE ==========
    /**
     * Cold Book Sparse Scenario - Tests performance on sparse order book.
     * Prefill: Widely spaced price levels (every 128 ticks)
     * Stream: Random prices likely to create new levels
     * Goals: Tree descent + node splits (ART) vs TreeMap rebalancing
     */
    @Benchmark
    public long scenario07_ColdBookSparse(Blackhole bh) {
        OrderBook book = createOrderBook();

        // Pre-fill with sparse levels
        for (OrderCommand order : preFillSparseBook) {
            book.newOrder(order);
        }

        long totalProcessed = 0;
        for (OrderCommand order : coldBookSparseOrders) {
            book.newOrder(order);
            totalProcessed++;
            bh.consume(order.orderId);
            bh.consume(order.price);
            bh.consume(order.size);
        }

        return totalProcessed;
    }

    private List<OrderCommand> generateColdBookSparseOrders() {
        List<OrderCommand> orders = new ArrayList<>(datasetSize);

        // Generate orders at random sparse prices likely to create new levels
        for (int i = 0; i < datasetSize; i++) {
            OrderAction action = random.nextBoolean() ? OrderAction.ASK : OrderAction.BID;
            // Random price in wide range
            long price = PRICE_MID - PRICE_SPREAD + random.nextInt((int)(2 * PRICE_SPREAD));
            orders.add(createOrder(action, price, generateLogNormalSize()));
        }

        return orders;
    }

    private List<OrderCommand> generateSparseBookPrefill() {
        List<OrderCommand> orders = new ArrayList<>();

        // Create levels every SPARSE_LEVEL_SPACING ticks
        for (long price = PRICE_MID - PRICE_SPREAD; price <= PRICE_MID + PRICE_SPREAD; price += SPARSE_LEVEL_SPACING) {
            if (price < PRICE_MID) {
                orders.add(createOrder(OrderAction.BID, price, 100 + random.nextInt(200)));
            } else if (price > PRICE_MID) {
                orders.add(createOrder(OrderAction.ASK, price, 100 + random.nextInt(200)));
            }
        }

        return orders;
    }

    // ========== SCENARIO 8: WIDE DENSE BOOK ==========
    /**
     * Wide Dense Book Scenario - Tests performance with many contiguous price levels.
     * Prefill: Many contiguous price levels (4k levels, small buckets)
     * Stream: Inserts and matches across the band
     * Goals: Cache behavior, pointer locality, ART node fanout advantages
     */
    @Benchmark
    public long scenario08_WideDenseBook(Blackhole bh) {
        OrderBook book = createOrderBook();

        // Pre-fill with dense levels
        for (OrderCommand order : preFillDenseBook) {
            book.newOrder(order);
        }

        long totalProcessed = 0;
        for (OrderCommand order : wideDenseBookOrders) {
            book.newOrder(order);
            totalProcessed++;
            bh.consume(order.orderId);
            bh.consume(order.price);
            bh.consume(order.size);
        }

        return totalProcessed;
    }

    private List<OrderCommand> generateWideDenseBookOrders() {
        List<OrderCommand> orders = new ArrayList<>(datasetSize);

        // Generate orders across the dense book range
        int denseBookLevels = Math.min(4000, (int)PRICE_SPREAD); // Limit to available range
        for (int i = 0; i < datasetSize; i++) {
            OrderAction action = random.nextBoolean() ? OrderAction.ASK : OrderAction.BID;
            // Price within dense book range
            long price = PRICE_MID - denseBookLevels/2 + random.nextInt(denseBookLevels);
            orders.add(createOrder(action, price, generateLogNormalSize()));
        }

        return orders;
    }

    private List<OrderCommand> generateDenseBookPrefill() {
        List<OrderCommand> orders = new ArrayList<>();

        // Create contiguous levels with small buckets
        int denseBookLevels = Math.min(4000, (int)PRICE_SPREAD); // Limit to available range
        for (long price = PRICE_MID - denseBookLevels/2; price <= PRICE_MID + denseBookLevels/2; price += TICK_SIZE) {
            if (price < PRICE_MID) {
                orders.add(createOrder(OrderAction.BID, price, 10 + random.nextInt(30)));
            } else if (price > PRICE_MID) {
                orders.add(createOrder(OrderAction.ASK, price, 10 + random.nextInt(30)));
            }
        }

        return orders;
    }

    // ========== SCENARIO 9: DUPLICATE SUBMIT ==========
    /**
     * Duplicate Submit Scenario - Tests handling of duplicate order IDs.
     * Stream: 10% duplicate order IDs (ensure rejection before matching)
     * Goals: Branch behavior for dup rejects; no unintended matches
     */
    @Benchmark
    public long scenario09_DuplicateSubmit(Blackhole bh) {
        OrderBook book = createOrderBook();
        long totalProcessed = 0;

        for (OrderCommand order : duplicateSubmitOrders) {
            book.newOrder(order);
            totalProcessed++;
            bh.consume(order.orderId);
            bh.consume(order.price);
            bh.consume(order.size);
        }

        return totalProcessed;
    }

    private List<OrderCommand> generateDuplicateSubmitOrders() {
        List<OrderCommand> orders = new ArrayList<>(datasetSize);
        List<Long> usedOrderIds = new ArrayList<>();

        for (int i = 0; i < datasetSize; i++) {
            boolean isDuplicate = random.nextInt(100) < 10 && !usedOrderIds.isEmpty(); // 10% duplicates
            OrderAction action = random.nextBoolean() ? OrderAction.ASK : OrderAction.BID;
            long price = PRICE_MID + (random.nextInt(201) - 100) * TICK_SIZE;

            if (isDuplicate) {
                // Use existing order ID
                long existingId = usedOrderIds.get(random.nextInt(usedOrderIds.size()));
                OrderCommand cmd = createOrder(action, price, generateLogNormalSize());
                cmd.orderId = existingId;
                orders.add(cmd);
            } else {
                // New order
                OrderCommand cmd = createOrder(action, price, generateLogNormalSize());
                usedOrderIds.add(cmd.orderId);
                orders.add(cmd);
            }
        }

        return orders;
    }
}
