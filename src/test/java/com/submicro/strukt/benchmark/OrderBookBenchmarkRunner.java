package com.submicro.strukt.benchmark;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

/**
 * Dedicated runner for OrderBook benchmarks.
 * Runs benchmarks in separate processes for accurate measurements.
 * 
 * Usage:
 * - Run all scenarios: java OrderBookBenchmarkRunner
 * - Run specific scenario: java OrderBookBenchmarkRunner pureInsert
 * - Run with specific parameters: java OrderBookBenchmarkRunner -p datasetSize=100000 -p orderBookType=ART
 */
public class OrderBookBenchmarkRunner {
    
    public static void main(String[] args) throws RunnerException {
        String scenarioFilter = args.length > 0 ? args[0] : ".*";
        
        Options opt = new OptionsBuilder()
                .include(OrderBookBenchmark.class.getSimpleName() + "." + scenarioFilter)
                .forks(1) // Run in separate process for accurate measurements
                .warmupIterations(10)
                .warmupTime(TimeValue.seconds(10))
                .measurementIterations(10)
                .measurementTime(TimeValue.seconds(10))
                .threads(1) // Single threaded for now
                .jvmArgs(
                    "-Xmx4G", "-Xms4G",
                    "-XX:+UseG1GC",
                    "-XX:+UnlockExperimentalVMOptions",
                    "-XX:+UseStringDeduplication",
                    "-XX:MaxGCPauseMillis=200",
                    "-Xlog:gc*:target/benchmark-results/gc-" + System.currentTimeMillis() + ".log"
                )
                .result("target/benchmark-results/orderbook-benchmark-" + System.currentTimeMillis() + ".json")
                .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.JSON)
                .build();

        new Runner(opt).run();
    }
    
    /**
     * Run only throughput benchmarks
     */
    public static void runThroughputOnly() throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(OrderBookBenchmark.class.getSimpleName())
                .mode(org.openjdk.jmh.annotations.Mode.Throughput)
                .forks(1)
                .warmupIterations(5)
                .warmupTime(TimeValue.seconds(10))
                .measurementIterations(5)
                .measurementTime(TimeValue.seconds(10))
                .threads(1)
                .jvmArgs("-Xmx4G", "-Xms4G", "-XX:+UseG1GC")
                .result("target/benchmark-results/orderbook-throughput-" + System.currentTimeMillis() + ".json")
                .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.JSON)
                .build();

        new Runner(opt).run();
    }
    
    /**
     * Run only latency benchmarks
     */
    public static void runLatencyOnly() throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(OrderBookBenchmark.class.getSimpleName())
                .mode(org.openjdk.jmh.annotations.Mode.SampleTime)
                .forks(1)
                .warmupIterations(10)
                .warmupTime(TimeValue.seconds(10))
                .measurementIterations(10)
                .measurementTime(TimeValue.seconds(10))
                .threads(1)
                .jvmArgs("-Xmx4G", "-Xms4G", "-XX:+UseG1GC")
                .result("target/benchmark-results/orderbook-latency-" + System.currentTimeMillis() + ".json")
                .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.JSON)
                .build();

        new Runner(opt).run();
    }
    
    /**
     * Run multi-threaded benchmarks
     */
    public static void runMultiThreaded() throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(OrderBookBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(5)
                .warmupTime(TimeValue.seconds(10))
                .measurementIterations(5)
                .measurementTime(TimeValue.seconds(10))
                .threads(4) // Multi-threaded
                .jvmArgs("-Xmx4G", "-Xms4G", "-XX:+UseG1GC")
                .result("target/benchmark-results/orderbook-multithreaded-" + System.currentTimeMillis() + ".json")
                .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.JSON)
                .build();

        new Runner(opt).run();
    }
    
    /**
     * Run performance comparison between ART and TreeSet implementations
     */
    public static void runImplementationComparison() throws RunnerException {
        // Run ART implementation
        Options artOpt = new OptionsBuilder()
                .include(OrderBookBenchmark.class.getSimpleName())
                .param("orderBookType", "ART")
                .param("datasetSize", "100000")
                .forks(1)
                .warmupIterations(5)
                .warmupTime(TimeValue.seconds(10))
                .measurementIterations(5)
                .measurementTime(TimeValue.seconds(10))
                .threads(1)
                .jvmArgs("-Xmx4G", "-Xms4G", "-XX:+UseG1GC")
                .result("target/benchmark-results/orderbook-art-" + System.currentTimeMillis() + ".json")
                .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.JSON)
                .build();

        // Run TreeSet implementation
        Options treeSetOpt = new OptionsBuilder()
                .include(OrderBookBenchmark.class.getSimpleName())
                .param("orderBookType", "TreeSet")
                .param("datasetSize", "100000")
                .forks(1)
                .warmupIterations(5)
                .warmupTime(TimeValue.seconds(10))
                .measurementIterations(5)
                .measurementTime(TimeValue.seconds(10))
                .threads(1)
                .jvmArgs("-Xmx4G", "-Xms4G", "-XX:+UseG1GC")
                .result("target/benchmark-results/orderbook-treeset-" + System.currentTimeMillis() + ".json")
                .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.JSON)
                .build();

        System.out.println("Running ART implementation benchmarks...");
        new Runner(artOpt).run();
        
        System.out.println("Running TreeSet implementation benchmarks...");
        new Runner(treeSetOpt).run();
    }
    
    /**
     * Run capacity density tests (25%, 50%, 75%, 100% capacity)
     */
    public static void runCapacityDensityTests() throws RunnerException {
        String[] capacities = {"10000", "50000", "100000", "1000000"};
        
        for (String capacity : capacities) {
            System.out.println("Running benchmark with dataset size: " + capacity);
            
            Options opt = new OptionsBuilder()
                    .include(OrderBookBenchmark.class.getSimpleName())
                    .param("datasetSize", capacity)
                    .forks(1)
                    .warmupIterations(3)
                    .warmupTime(TimeValue.seconds(5))
                    .measurementIterations(3)
                    .measurementTime(TimeValue.seconds(5))
                    .threads(1)
                    .jvmArgs("-Xmx4G", "-Xms4G", "-XX:+UseG1GC")
                    .result("target/benchmark-results/orderbook-capacity-" + capacity + "-" + System.currentTimeMillis() + ".json")
                    .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.JSON)
                    .build();

            new Runner(opt).run();
        }
    }
}
