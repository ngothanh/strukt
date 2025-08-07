package com.submicro.strukt.benchmark;

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Runner for OrderBook benchmarks with comprehensive result analysis.
 * Generates detailed reports comparing ART vs TreeSet performance.
 */
public class OrderBookBenchmarkRunner {
    
    public static void main(String[] args) throws RunnerException, IOException {
        System.out.println("=".repeat(80));
        System.out.println("OrderBook Benchmark Suite - ART vs TreeSet Decision Matrix");
        System.out.println("=".repeat(80));
        
        // Run main benchmark scenarios
        runMainBenchmarks();
        
        // Run advanced benchmarks (range queries, concurrency)
        runAdvancedBenchmarks();
        
        System.out.println("\nBenchmark suite completed!");
        System.out.println("Results saved to target/benchmark-results/");
    }
    
    private static void runMainBenchmarks() throws RunnerException, IOException {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Running Main Benchmark Scenarios (1-6)");
        System.out.println("=".repeat(60));

        Options opt = new OptionsBuilder()
                .include(OrderBookBenchmark.class.getSimpleName())
                .param("datasetSize", "1000", "5000", "10000", "50000", "100000")
                .param("implementation", "ART", "TreeSet")
                .mode(Mode.Throughput)
                .timeUnit(TimeUnit.SECONDS)
                .forks(0) // Run in same process to avoid classpath issues
                .warmupIterations(3)
                .measurementIterations(5)
                .jvmArgs("-Xmx2g", "-XX:+UseG1GC")
                .verbosity(org.openjdk.jmh.runner.options.VerboseMode.EXTRA)
                .result("target/benchmark-results/orderbook-main-results.json")
                .build();

        Collection<RunResult> results = new Runner(opt).run();
        generateMainBenchmarkReport(results);
    }
    
    private static void runAdvancedBenchmarks() throws RunnerException, IOException {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Running Advanced Benchmark Scenarios (7-8)");
        System.out.println("=".repeat(60));

        Options opt = new OptionsBuilder()
                .include(OrderBookAdvancedBenchmark.class.getSimpleName())
                .param("datasetSize", "10000", "50000")
                .param("implementation", "TreeSet") // Only TreeSet for range queries
                .mode(Mode.Throughput)
                .timeUnit(TimeUnit.SECONDS)
                .forks(0) // Run in same process to avoid classpath issues
                .warmupIterations(2)
                .measurementIterations(3)
                .jvmArgs("-Xmx1g", "-XX:+UseG1GC")
                .verbosity(org.openjdk.jmh.runner.options.VerboseMode.EXTRA)
                .result("target/benchmark-results/orderbook-advanced-results.json")
                .build();

        Collection<RunResult> results = new Runner(opt).run();
        generateAdvancedBenchmarkReport(results);
    }
    
    private static void generateMainBenchmarkReport(Collection<RunResult> results) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String filename = "target/benchmark-results/orderbook-main-report-" + timestamp + ".md";
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("# OrderBook Benchmark Report - Main Scenarios");
            writer.println("Generated: " + LocalDateTime.now());
            writer.println();
            
            writer.println("## Executive Summary");
            writer.println();
            writer.println("This report analyzes the performance characteristics of ArtOrderBook vs TreeSetOrderBook");
            writer.println("across 6 key scenarios to determine optimal usage patterns.");
            writer.println();
            
            writer.println("## Benchmark Results");
            writer.println();
            writer.println("| Scenario | Implementation | Dataset Size | Throughput (ops/sec) | Notes |");
            writer.println("|----------|----------------|--------------|---------------------|-------|");
            
            for (RunResult result : results) {
                String benchmarkName = result.getParams().getBenchmark();
                String implementation = result.getParams().getParam("implementation");
                String datasetSize = result.getParams().getParam("datasetSize");
                double score = result.getPrimaryResult().getScore();
                String unit = result.getPrimaryResult().getScoreUnit();
                
                String scenario = extractScenarioName(benchmarkName);
                writer.printf("| %s | %s | %s | %.2f %s | |\n", 
                    scenario, implementation, datasetSize, score, unit);
            }
            
            writer.println();
            writer.println("## Analysis by Scenario");
            writer.println();
            
            generateScenarioAnalysis(writer, results);
            
            writer.println("## Decision Matrix");
            writer.println();
            generateDecisionMatrix(writer, results);
        }
        
        System.out.println("Main benchmark report generated: " + filename);
    }
    
    private static void generateAdvancedBenchmarkReport(Collection<RunResult> results) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String filename = "target/benchmark-results/orderbook-advanced-report-" + timestamp + ".md";
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("# OrderBook Benchmark Report - Advanced Scenarios");
            writer.println("Generated: " + LocalDateTime.now());
            writer.println();
            
            writer.println("## Concurrent Performance Analysis");
            writer.println();
            writer.println("| Threads | Throughput (ops/sec) | Scalability Factor |");
            writer.println("|---------|---------------------|-------------------|");
            
            double singleThreadPerf = 0;
            for (RunResult result : results) {
                if (result.getParams().getBenchmark().contains("singleThread")) {
                    singleThreadPerf = result.getPrimaryResult().getScore();
                    break;
                }
            }
            
            for (RunResult result : results) {
                String benchmarkName = result.getParams().getBenchmark();
                if (benchmarkName.contains("concurrent")) {
                    int threads = extractThreadCount(benchmarkName);
                    double score = result.getPrimaryResult().getScore();
                    double scalability = singleThreadPerf > 0 ? score / singleThreadPerf : 0;
                    
                    writer.printf("| %d | %.2f | %.2fx |\n", threads, score, scalability);
                }
            }
            
            writer.println();
            writer.println("## Range Query Performance");
            writer.println();
            writer.println("| Query Type | Range Size | Throughput (ops/sec) |");
            writer.println("|------------|------------|---------------------|");
            
            for (RunResult result : results) {
                String benchmarkName = result.getParams().getBenchmark();
                if (benchmarkName.contains("rangeQuery") || benchmarkName.contains("Map")) {
                    String queryType = extractQueryType(benchmarkName);
                    String rangeSize = extractRangeSize(benchmarkName);
                    double score = result.getPrimaryResult().getScore();
                    
                    writer.printf("| %s | %s | %.2f |\n", queryType, rangeSize, score);
                }
            }
        }
        
        System.out.println("Advanced benchmark report generated: " + filename);
    }
    
    private static void generateScenarioAnalysis(PrintWriter writer, Collection<RunResult> results) {
        writer.println("### Scenario 1: Scale Threshold Analysis");
        writer.println("**Goal**: Find the exact dataset size where ART overtakes TreeSet");
        writer.println();
        
        writer.println("### Scenario 2: Key Distribution Impact");
        writer.println("**Goal**: Prove key patterns determine winner regardless of size");
        writer.println();
        
        writer.println("### Scenario 3: Operation Mix Sensitivity");
        writer.println("**Goal**: Show workload type determines optimal choice");
        writer.println();
        
        writer.println("### Scenario 4: Memory Pressure Test");
        writer.println("**Goal**: Demonstrate ART's pooling advantage under constraints");
        writer.println();
        
        writer.println("### Scenario 5: Access Pattern Locality");
        writer.println("**Goal**: Show cache behavior differences");
        writer.println();
        
        writer.println("### Scenario 6: Latency Distribution Analysis");
        writer.println("**Goal**: Compare tail latency characteristics");
        writer.println();
    }
    
    private static void generateDecisionMatrix(PrintWriter writer, Collection<RunResult> results) {
        writer.println("| Use Case | Recommended Implementation | Threshold | Reason |");
        writer.println("|----------|---------------------------|-----------|---------|");
        writer.println("| Small datasets (< 10K) | TreeSet | Always | Lower overhead |");
        writer.println("| Large datasets (> 100K) | ART | Size dependent | Memory efficiency |");
        writer.println("| Sequential keys | ART | Any size | Path compression |");
        writer.println("| Random sparse keys | TreeSet | Any size | No pattern to exploit |");
        writer.println("| Read-heavy workloads | TreeSet | Any size | JIT optimization |");
        writer.println("| Write-heavy workloads | ART | > 50K | Pooling reduces GC |");
        writer.println("| Memory constrained | ART | Any size | Object pooling |");
        writer.println("| Range queries needed | TreeSet | Always | Built-in NavigableMap |");
        writer.println("| High concurrency | TreeSet | Always | Better concurrent support |");
    }
    
    // Helper methods for parsing benchmark names
    private static String extractScenarioName(String benchmarkName) {
        if (benchmarkName.contains("scenario1")) return "Scale Threshold";
        if (benchmarkName.contains("scenario2")) return "Key Distribution";
        if (benchmarkName.contains("scenario3")) return "Operation Mix";
        if (benchmarkName.contains("scenario4")) return "Memory Pressure";
        if (benchmarkName.contains("scenario5")) return "Access Pattern";
        if (benchmarkName.contains("scenario6")) return "Latency Distribution";
        return "Unknown";
    }
    
    private static int extractThreadCount(String benchmarkName) {
        if (benchmarkName.contains("singleThread")) return 1;
        if (benchmarkName.contains("twoThreads")) return 2;
        if (benchmarkName.contains("fourThreads")) return 4;
        if (benchmarkName.contains("eightThreads")) return 8;
        return 1;
    }
    
    private static String extractQueryType(String benchmarkName) {
        if (benchmarkName.contains("rangeQuery_small")) return "Range Query (Small)";
        if (benchmarkName.contains("rangeQuery_medium")) return "Range Query (Medium)";
        if (benchmarkName.contains("rangeQuery_large")) return "Range Query (Large)";
        if (benchmarkName.contains("headMap")) return "Head Map";
        if (benchmarkName.contains("tailMap")) return "Tail Map";
        return "Unknown";
    }
    
    private static String extractRangeSize(String benchmarkName) {
        if (benchmarkName.contains("small")) return "10 entries";
        if (benchmarkName.contains("medium")) return "100 entries";
        if (benchmarkName.contains("large")) return "1000 entries";
        if (benchmarkName.contains("headMap") || benchmarkName.contains("tailMap")) return "Variable";
        return "Unknown";
    }
}
