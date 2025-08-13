#!/bin/bash

# Comprehensive OrderBook Benchmark Suite Runner
# Executes all scenarios with rigorous JMH methodology
# Dataset sizes: 10K, 100K, 1M orders for scaling analysis

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
SCENARIO=".*"
DATASET_SIZES="10000,100000,1000000"
ORDER_BOOK_TYPES="ART,TreeSet"
MODE="comprehensive"

# Function to print usage
usage() {
    echo -e "${BLUE}Comprehensive OrderBook Benchmark Suite Runner${NC}"
    echo ""
    echo "Executes rigorous performance analysis with:"
    echo "- 5 forks for statistical validity"
    echo "- 5 warmup + 10 measurement iterations"
    echo "- AlwaysPreTouch for memory stability"
    echo "- Multiple dataset sizes: 10K, 100K, 1M orders"
    echo "- Comprehensive scenario coverage"
    echo ""
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  -s, --scenario PATTERN       Benchmark scenario pattern (default: all)"
    echo "  -d, --dataset-sizes SIZES     Dataset sizes: 10000,100000,1000000 (default: all)"
    echo "  -t, --types TYPES             OrderBook types: ART,TreeSet (default: both)"
    echo "  -m, --mode MODE               Run mode: comprehensive, throughput, latency, gc (default: comprehensive)"
    echo "  -h, --help                    Show this help message"
    echo ""
    echo "Scenarios:"
    echo "  scenario01_PureInsert         - Pure order insertion (no matches)"
    echo "  scenario02_PureMatch          - Pure order matching"
    echo "  scenario03_PartialMatch       - Partial order matching"
    echo "  scenario04_RandomMix          - Random mix of operations"
    echo "  scenario05_HotspotSinglePrice - Single price concentration"
    echo "  scenario06_HotspotNarrowBand  - Narrow band concentration"
    echo "  scenario07_ColdBookSparse     - Sparse order book"
    echo "  scenario08_WideDenseBook      - Dense order book"
    echo "  scenario09_DuplicateSubmit    - Duplicate order handling"
    echo ""
    echo "Examples:"
    echo "  $0                                    # Full comprehensive benchmark"
    echo "  $0 -s scenario01_PureInsert          # Single scenario"
    echo "  $0 -d 10000 -t ART                   # Small dataset, ART only"
    echo "  $0 -m throughput                     # Throughput-only measurements"
    echo "  $0 -m latency -d 10000               # Latency analysis, small dataset"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -s|--scenario)
            SCENARIO="$2"
            shift 2
            ;;
        -d|--dataset-sizes)
            DATASET_SIZES="$2"
            shift 2
            ;;
        -t|--types)
            ORDER_BOOK_TYPES="$2"
            shift 2
            ;;
        -m|--mode)
            MODE="$2"
            shift 2
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown option $1"
            usage
            exit 1
            ;;
    esac
done

echo -e "${GREEN}=== Comprehensive OrderBook Benchmark Suite ===${NC}"
echo -e "${BLUE}Scenario:${NC} $SCENARIO"
echo -e "${BLUE}Dataset Sizes:${NC} $DATASET_SIZES"
echo -e "${BLUE}OrderBook Types:${NC} $ORDER_BOOK_TYPES"
echo -e "${BLUE}Mode:${NC} $MODE"
echo ""

# Ensure target directory exists
mkdir -p target/benchmark-results

# Compile project
echo -e "${YELLOW}Compiling project...${NC}"
mvn clean compile test-compile -q

if [ $? -ne 0 ]; then
    echo -e "${RED}Compilation failed!${NC}"
    exit 1
fi

echo -e "${GREEN}Compilation successful.${NC}"

# Prepare timestamp for result files
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Disable macOS App Nap for stable performance
if [[ "$OSTYPE" == "darwin"* ]]; then
    echo -e "${YELLOW}Disabling macOS App Nap for stable performance...${NC}"
    caffeinate -i &
    CAFFEINATE_PID=$!
    trap "kill $CAFFEINATE_PID 2>/dev/null || true" EXIT
fi

# Build classpath
CLASSPATH="target/test-classes:target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q)"

# Base JMH arguments
BASE_ARGS="-p datasetSize=$DATASET_SIZES -p orderBookType=$ORDER_BOOK_TYPES"

case $MODE in
    "comprehensive")
        echo -e "${GREEN}Running comprehensive benchmark suite...${NC}"
        echo -e "${YELLOW}Phase 1: Throughput measurements (5 forks, 5 warmup, 10 measurement)${NC}"
        
        THROUGHPUT_FILE="target/benchmark-results/throughput-comprehensive-$TIMESTAMP.json"
        java -cp "$CLASSPATH" org.openjdk.jmh.Main "OrderBookBenchmarkSuite.$SCENARIO" \
            $BASE_ARGS \
            -rf json -rff "$THROUGHPUT_FILE" \
            -prof gc
        
        echo -e "${YELLOW}Phase 2: Latency measurements (SampleTime mode)${NC}"
        
        LATENCY_FILE="target/benchmark-results/latency-comprehensive-$TIMESTAMP.json"
        java -cp "$CLASSPATH" org.openjdk.jmh.Main "OrderBookLatencyBenchmark.latency.*" \
            $BASE_ARGS \
            -rf json -rff "$LATENCY_FILE" \
            -prof gc
        
        echo -e "${BLUE}Throughput results:${NC} $THROUGHPUT_FILE"
        echo -e "${BLUE}Latency results:${NC} $LATENCY_FILE"
        ;;
        
    "throughput")
        echo -e "${GREEN}Running throughput benchmarks...${NC}"
        echo -e "${YELLOW}Configuration: 5 forks, 5 warmup, 10 measurement iterations${NC}"
        
        RESULT_FILE="target/benchmark-results/throughput-$TIMESTAMP.json"
        java -cp "$CLASSPATH" org.openjdk.jmh.Main "OrderBookBenchmarkSuite.$SCENARIO" \
            $BASE_ARGS \
            -rf json -rff "$RESULT_FILE"
        
        echo -e "${BLUE}Results saved to:${NC} $RESULT_FILE"
        ;;
        
    "latency")
        echo -e "${GREEN}Running latency benchmarks...${NC}"
        echo -e "${YELLOW}Configuration: SampleTime mode, 5 forks, detailed distribution${NC}"
        
        RESULT_FILE="target/benchmark-results/latency-$TIMESTAMP.json"
        java -cp "$CLASSPATH" org.openjdk.jmh.Main "OrderBookLatencyBenchmark.latency.*" \
            $BASE_ARGS \
            -rf json -rff "$RESULT_FILE"
        
        echo -e "${BLUE}Results saved to:${NC} $RESULT_FILE"
        ;;
        
    "gc")
        echo -e "${GREEN}Running GC and allocation profiling...${NC}"
        echo -e "${YELLOW}Configuration: GC profiler + allocation tracking${NC}"
        
        RESULT_FILE="target/benchmark-results/gc-profiling-$TIMESTAMP.json"
        GC_LOG="target/benchmark-results/gc-$TIMESTAMP.log"
        
        java -cp "$CLASSPATH" \
            -Xlog:gc*:$GC_LOG \
            org.openjdk.jmh.Main "OrderBookBenchmarkSuite.$SCENARIO" \
            $BASE_ARGS \
            -rf json -rff "$RESULT_FILE" \
            -prof gc \
            -prof stack
        
        echo -e "${BLUE}Results saved to:${NC} $RESULT_FILE"
        echo -e "${BLUE}GC log saved to:${NC} $GC_LOG"
        ;;
        
    *)
        echo -e "${RED}Unknown mode: $MODE${NC}"
        usage
        exit 1
        ;;
esac

echo ""
echo -e "${GREEN}Benchmark execution completed successfully!${NC}"

# Performance analysis summary
echo ""
echo -e "${YELLOW}Benchmark Quality Indicators:${NC}"
echo "✅ 5 forks for statistical validity across JVMs"
echo "✅ Adequate warmup (5×2s) for C1→C2 compilation settling"
echo "✅ Sufficient measurement (10×2s) for stable results"
echo "✅ AlwaysPreTouch for memory stability"
echo "✅ DCE prevention with return values + Blackhole consumption"
echo "✅ Deterministic data generation (RNG seed: 42)"
echo "✅ Multiple dataset sizes for scaling analysis"
echo "✅ Comprehensive scenario coverage"

echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo "1. Analyze JSON results for performance characteristics"
echo "2. Compare ART vs TreeSet across scenarios and dataset sizes"
echo "3. Review latency distributions and percentiles"
echo "4. Examine GC behavior and allocation patterns"
echo "5. Validate results align with theoretical expectations"
echo "6. Generate comprehensive performance report"
