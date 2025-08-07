package com.submicro.strukt.benchmark;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Master runner for all OrderBook benchmarks.
 * Executes the complete benchmark suite and generates comprehensive analysis.
 */
public class RunAllBenchmarks {
    
    public static void main(String[] args) throws RunnerException, IOException {
        System.out.println("=".repeat(100));
        System.out.println("ORDERBOOK BENCHMARK SUITE - COMPLETE ANALYSIS");
        System.out.println("ART vs TreeSet Decision Matrix Generation");
        System.out.println("=".repeat(100));
        
        // Ensure results directory exists
        Files.createDirectories(Paths.get("target/benchmark-results"));
        
        // Run all benchmark suites
        runQuickBenchmarks();
        runMainBenchmarks();
        runMemoryPressureBenchmarks();
        runAdvancedBenchmarks();
        
        // Generate comprehensive analysis
        BenchmarkAnalyzer.generateComprehensiveReport();
        
        System.out.println("\n" + "=".repeat(100));
        System.out.println("BENCHMARK SUITE COMPLETED SUCCESSFULLY!");
        System.out.println("Check target/benchmark-results/ for detailed reports");
        System.out.println("=".repeat(100));
    }
    
    private static void runQuickBenchmarks() throws RunnerException {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("PHASE 1: Quick Performance Overview");
        System.out.println("=".repeat(80));

        Options opt = new OptionsBuilder()
                .include(OrderBookBenchmark.class.getSimpleName())
                .param("datasetSize", "1000", "10000", "50000")
                .param("implementation", "ART", "TreeSet")
                .include("scenario1_scaleThreshold_sequentialInsert")
                .include("scenario2_keyDistribution_sequential")
                .include("scenario2_keyDistribution_randomSparse")
                .mode(org.openjdk.jmh.annotations.Mode.Throughput)
                .timeUnit(java.util.concurrent.TimeUnit.SECONDS)
                .forks(1) // Use single fork to avoid classpath issues with exec plugin
                .warmupIterations(2)
                .measurementIterations(3)
                .jvmArgs("-Xmx1g", "-XX:+UseG1GC")
                .result("target/benchmark-results/quick-overview.json")
                .build();

        System.out.println("Running quick performance overview...");
        new Runner(opt).run();
    }
    
    private static void runMainBenchmarks() throws RunnerException {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("PHASE 2: Main Benchmark Scenarios (1-6)");
        System.out.println("=".repeat(80));

        Options opt = new OptionsBuilder()
                .include(OrderBookBenchmark.class.getSimpleName())
                .param("datasetSize", "1000", "5000", "10000", "50000", "100000")
                .param("implementation", "ART", "TreeSet")
                .mode(org.openjdk.jmh.annotations.Mode.Throughput)
                .timeUnit(java.util.concurrent.TimeUnit.SECONDS)
                .forks(1) // Use single fork to avoid classpath issues with exec plugin
                .warmupIterations(3)
                .measurementIterations(5)
                .jvmArgs("-Xmx2g", "-XX:+UseG1GC")
                .result("target/benchmark-results/main-scenarios.json")
                .build();

        System.out.println("Running main benchmark scenarios...");
        new Runner(opt).run();
    }
    
    private static void runMemoryPressureBenchmarks() throws RunnerException {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("PHASE 3: Memory Pressure Analysis");
        System.out.println("=".repeat(80));

        Options opt = new OptionsBuilder()
                .include(MemoryPressureBenchmark.class.getSimpleName())
                .param("datasetSize", "10000", "50000", "100000")
                .param("implementation", "ART", "TreeSet")
                .mode(org.openjdk.jmh.annotations.Mode.Throughput)
                .timeUnit(java.util.concurrent.TimeUnit.SECONDS)
                .forks(1) // Use single fork to avoid classpath issues with exec plugin
                .warmupIterations(2)
                .measurementIterations(3)
                .jvmArgs("-Xmx512m", "-XX:+UseG1GC", "-XX:+PrintGC")
                .result("target/benchmark-results/memory-pressure.json")
                .build();

        System.out.println("Running memory pressure benchmarks...");
        new Runner(opt).run();
    }
    
    private static void runAdvancedBenchmarks() throws RunnerException {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("PHASE 4: Advanced Scenarios (Concurrency & Range Queries)");
        System.out.println("=".repeat(80));

        Options opt = new OptionsBuilder()
                .include(OrderBookAdvancedBenchmark.class.getSimpleName())
                .param("datasetSize", "10000", "50000")
                .param("implementation", "TreeSet") // Only TreeSet for range queries
                .mode(org.openjdk.jmh.annotations.Mode.Throughput)
                .timeUnit(java.util.concurrent.TimeUnit.SECONDS)
                .forks(1) // Use single fork to avoid classpath issues with exec plugin
                .warmupIterations(2)
                .measurementIterations(3)
                .jvmArgs("-Xmx1g", "-XX:+UseG1GC")
                .result("target/benchmark-results/advanced-scenarios.json")
                .build();

        System.out.println("Running advanced benchmark scenarios...");
        new Runner(opt).run();
    }
    
    /**
     * Run a focused benchmark for specific scenarios
     */
    public static void runFocusedBenchmark(String scenario, String... dataSizes) throws RunnerException {
        System.out.println("Running focused benchmark for: " + scenario);

        Options opt;
        if (dataSizes.length > 0) {
            opt = new OptionsBuilder()
                    .include(scenario)
                    .param("implementation", "ART", "TreeSet")
                    .param("datasetSize", dataSizes)
                    .forks(1)
                    .warmupIterations(2)
                    .measurementIterations(3)
                    .build();
        } else {
            opt = new OptionsBuilder()
                    .include(scenario)
                    .param("implementation", "ART", "TreeSet")
                    .forks(1)
                    .warmupIterations(2)
                    .measurementIterations(3)
                    .build();
        }

        new Runner(opt).run();
    }
    
    /**
     * Run performance comparison for specific dataset size
     */
    public static void runSizeComparison(String size) throws RunnerException {
        System.out.println("Running size comparison for dataset: " + size);
        
        Options opt = new OptionsBuilder()
                .include(OrderBookBenchmark.class.getSimpleName())
                .param("datasetSize", size)
                .param("implementation", "ART", "TreeSet")
                .forks(1)
                .warmupIterations(3)
                .measurementIterations(5)
                .result("target/benchmark-results/size-comparison-" + size + ".json")
                .build();

        new Runner(opt).run();
    }
    
    /**
     * Run memory-constrained benchmarks with different heap sizes
     */
    public static void runMemoryConstrainedBenchmarks() throws RunnerException {
        System.out.println("Running memory-constrained benchmarks...");
        
        String[] heapSizes = {"-Xmx256m", "-Xmx512m", "-Xmx1g"};
        
        for (String heapSize : heapSizes) {
            System.out.println("Testing with heap size: " + heapSize);
            
            Options opt = new OptionsBuilder()
                    .include(MemoryPressureBenchmark.class.getSimpleName())
                    .param("datasetSize", "50000")
                    .param("implementation", "ART", "TreeSet")
                    .forks(1)
                    .warmupIterations(2)
                    .measurementIterations(3)
                    .jvmArgs(heapSize, "-XX:+UseG1GC", "-XX:+PrintGC")
                    .result("target/benchmark-results/memory-" + heapSize.replace("-Xmx", "") + ".json")
                    .build();

            new Runner(opt).run();
        }
    }
    
    /**
     * Generate summary report of key findings
     */
    public static void generateSummaryReport() throws IOException {
        System.out.println("\nGenerating summary report...");
        
        // This would analyze all the JSON results and create a summary
        // For now, we'll generate the comprehensive analysis
        BenchmarkAnalyzer.generateComprehensiveReport();
        
        System.out.println("Summary report generated in target/benchmark-results/");
    }
}
