package com.submicro.strukt.benchmark;

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Dedicated process benchmark runner that properly handles JMH forking.
 * This runner creates a proper JAR-based execution environment for true process isolation.
 */
public class DedicatedProcessBenchmarkRunner {
    
    public static void main(String[] args) throws RunnerException, IOException {
        System.out.println("=".repeat(80));
        System.out.println("DEDICATED PROCESS ORDERBOOK BENCHMARK");
        System.out.println("True Process Isolation with JMH Forking");
        System.out.println("=".repeat(80));
        
        if (args.length > 0 && "quick".equals(args[0])) {
            runQuickBenchmark();
        } else if (args.length > 0 && "memory".equals(args[0])) {
            runMemoryBenchmark();
        } else if (args.length > 0 && "advanced".equals(args[0])) {
            runAdvancedBenchmark();
        } else {
            runCompleteBenchmark();
        }
        
        System.out.println("\nBenchmark completed successfully!");
        System.out.println("Results saved to target/benchmark-results/");
    }
    
    private static void runQuickBenchmark() throws RunnerException {
        System.out.println("Running Quick Performance Comparison...");
        
        Options opt = new OptionsBuilder()
                .include(OrderBookBenchmark.class.getSimpleName())
                .param("datasetSize", "1000", "10000")
                .param("implementation", "ART", "TreeSet")
                .include("scenario1_scaleThreshold_sequentialInsert")
                .include("scenario2_keyDistribution_sequential")
                .include("scenario2_keyDistribution_randomSparse")
                .mode(Mode.Throughput)
                .timeUnit(TimeUnit.SECONDS)
                .forks(2) // True dedicated processes
                .warmupIterations(1)
                .measurementIterations(2)
                .jvmArgs("-Xmx1g", "-XX:+UseG1GC")
                .result("target/benchmark-results/quick-dedicated-process.json")
                .build();

        Collection<RunResult> results = new Runner(opt).run();
        System.out.printf("Completed %d benchmark runs%n", results.size());
    }
    
    private static void runMemoryBenchmark() throws RunnerException {
        System.out.println("Running Memory Pressure Analysis...");
        
        Options opt = new OptionsBuilder()
                .include(MemoryPressureBenchmark.class.getSimpleName())
                .param("datasetSize", "10000", "50000")
                .param("implementation", "ART", "TreeSet")
                .include("memoryPressure_continuousChurn")
                .include("memoryPressure_rapidAllocation")
                .mode(Mode.Throughput)
                .timeUnit(TimeUnit.SECONDS)
                .forks(2) // True dedicated processes with memory constraints
                .warmupIterations(1)
                .measurementIterations(2)
                .jvmArgs("-Xmx512m", "-XX:+UseG1GC", "-XX:+PrintGC")
                .result("target/benchmark-results/memory-dedicated-process.json")
                .build();

        Collection<RunResult> results = new Runner(opt).run();
        System.out.printf("Completed %d memory benchmark runs%n", results.size());
    }
    
    private static void runAdvancedBenchmark() throws RunnerException {
        System.out.println("Running Advanced Scenarios...");
        
        Options opt = new OptionsBuilder()
                .include(OrderBookAdvancedBenchmark.class.getSimpleName())
                .param("datasetSize", "10000")
                .param("implementation", "TreeSet")
                .include("scenario8_rangeQuery_small")
                .include("scenario8_rangeQuery_medium")
                .include("scenario7_concurrent_singleThread")
                .include("scenario7_concurrent_fourThreads")
                .mode(Mode.Throughput)
                .timeUnit(TimeUnit.SECONDS)
                .forks(2) // True dedicated processes
                .warmupIterations(1)
                .measurementIterations(2)
                .jvmArgs("-Xmx1g", "-XX:+UseG1GC")
                .result("target/benchmark-results/advanced-dedicated-process.json")
                .build();

        Collection<RunResult> results = new Runner(opt).run();
        System.out.printf("Completed %d advanced benchmark runs%n", results.size());
    }
    
    private static void runCompleteBenchmark() throws RunnerException {
        System.out.println("Running Complete Benchmark Suite...");
        
        // Main scenarios
        Options mainOpt = new OptionsBuilder()
                .include(OrderBookBenchmark.class.getSimpleName())
                .param("datasetSize", "1000", "10000", "50000")
                .param("implementation", "ART", "TreeSet")
                .mode(Mode.Throughput)
                .timeUnit(TimeUnit.SECONDS)
                .forks(2) // True dedicated processes
                .warmupIterations(2)
                .measurementIterations(3)
                .jvmArgs("-Xmx2g", "-XX:+UseG1GC")
                .result("target/benchmark-results/complete-main-dedicated.json")
                .build();

        System.out.println("Phase 1: Main scenarios...");
        Collection<RunResult> mainResults = new Runner(mainOpt).run();
        
        // Memory pressure scenarios
        Options memoryOpt = new OptionsBuilder()
                .include(MemoryPressureBenchmark.class.getSimpleName())
                .param("datasetSize", "10000", "50000")
                .param("implementation", "ART", "TreeSet")
                .mode(Mode.Throughput)
                .timeUnit(TimeUnit.SECONDS)
                .forks(2) // True dedicated processes with memory constraints
                .warmupIterations(1)
                .measurementIterations(2)
                .jvmArgs("-Xmx512m", "-XX:+UseG1GC", "-XX:+PrintGC")
                .result("target/benchmark-results/complete-memory-dedicated.json")
                .build();

        System.out.println("Phase 2: Memory pressure scenarios...");
        Collection<RunResult> memoryResults = new Runner(memoryOpt).run();
        
        System.out.printf("Completed %d main + %d memory benchmark runs%n", 
                         mainResults.size(), memoryResults.size());
    }
    
    /**
     * Run a single focused benchmark scenario
     */
    public static void runSingleScenario(String scenarioPattern, String datasetSize, String implementation) throws RunnerException {
        System.out.printf("Running focused benchmark: %s with %s dataset using %s%n", 
                         scenarioPattern, datasetSize, implementation);
        
        Options opt = new OptionsBuilder()
                .include(scenarioPattern)
                .param("datasetSize", datasetSize)
                .param("implementation", implementation)
                .mode(Mode.Throughput)
                .timeUnit(TimeUnit.SECONDS)
                .forks(2) // True dedicated processes
                .warmupIterations(2)
                .measurementIterations(3)
                .jvmArgs("-Xmx1g", "-XX:+UseG1GC")
                .result("target/benchmark-results/focused-" + scenarioPattern.replace("*", "all") + ".json")
                .build();

        Collection<RunResult> results = new Runner(opt).run();
        
        // Print summary
        System.out.println("\n" + "=".repeat(60));
        System.out.println("BENCHMARK RESULTS SUMMARY");
        System.out.println("=".repeat(60));
        
        for (RunResult result : results) {
            String benchmark = result.getParams().getBenchmark();
            String impl = result.getParams().getParam("implementation");
            String size = result.getParams().getParam("datasetSize");
            double score = result.getPrimaryResult().getScore();
            String unit = result.getPrimaryResult().getScoreUnit();
            
            System.out.printf("%-50s | %8s | %8s | %12.2f %s%n", 
                             benchmark.substring(benchmark.lastIndexOf('.') + 1), 
                             impl, size, score, unit);
        }
    }
    
    /**
     * Compare ART vs TreeSet for a specific scenario
     */
    public static void runComparison(String scenarioPattern, String datasetSize) throws RunnerException {
        System.out.printf("Running ART vs TreeSet comparison for %s with dataset size %s%n", 
                         scenarioPattern, datasetSize);
        
        Options opt = new OptionsBuilder()
                .include(scenarioPattern)
                .param("datasetSize", datasetSize)
                .param("implementation", "ART", "TreeSet")
                .mode(Mode.Throughput)
                .timeUnit(TimeUnit.SECONDS)
                .forks(2) // True dedicated processes
                .warmupIterations(3)
                .measurementIterations(5)
                .jvmArgs("-Xmx1g", "-XX:+UseG1GC")
                .result("target/benchmark-results/comparison-" + scenarioPattern.replace("*", "all") + "-" + datasetSize + ".json")
                .build();

        Collection<RunResult> results = new Runner(opt).run();
        
        // Analyze results
        RunResult artResult = null;
        RunResult treeSetResult = null;
        
        for (RunResult result : results) {
            String impl = result.getParams().getParam("implementation");
            if ("ART".equals(impl)) {
                artResult = result;
            } else if ("TreeSet".equals(impl)) {
                treeSetResult = result;
            }
        }
        
        if (artResult != null && treeSetResult != null) {
            double artScore = artResult.getPrimaryResult().getScore();
            double treeSetScore = treeSetResult.getPrimaryResult().getScore();
            double ratio = artScore / treeSetScore;
            
            System.out.println("\n" + "=".repeat(60));
            System.out.println("PERFORMANCE COMPARISON RESULTS");
            System.out.println("=".repeat(60));
            System.out.printf("ART Score:     %12.2f ops/sec%n", artScore);
            System.out.printf("TreeSet Score: %12.2f ops/sec%n", treeSetScore);
            System.out.printf("ART/TreeSet:   %12.2fx%n", ratio);
            
            if (ratio > 1.1) {
                System.out.printf("WINNER: ART is %.1fx faster than TreeSet%n", ratio);
            } else if (ratio < 0.9) {
                System.out.printf("WINNER: TreeSet is %.1fx faster than ART%n", 1.0/ratio);
            } else {
                System.out.println("RESULT: Performance is comparable (within 10%)");
            }
        }
    }
}
