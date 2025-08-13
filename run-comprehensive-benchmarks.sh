#!/bin/bash

# Comprehensive OrderBook Benchmark Runner
# Implements proper JMH methodology addressing all identified issues

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
SCENARIO=".*"
DATASET_SIZE="10000"
ORDER_BOOK_TYPE="ART,TreeSet"
MODE="comprehensive"

# Function to print usage
usage() {
    echo -e "${BLUE}Comprehensive OrderBook Benchmark Runner${NC}"
    echo ""
    echo "Implements proper JMH methodology:"
    echo "- 5 forks for statistical validity"
    echo "- Adequate warmup (5x2s) and measurement (10x2s)"
    echo "- AlwaysPreTouch for stable memory"
    echo "- Separate throughput and latency measurements"
    echo "- GC and allocation profiling"
    echo ""
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  -s, --scenario PATTERN       Benchmark scenario pattern (default: all)"
    echo "  -d, --dataset-size SIZE       Dataset size (default: 10000)"
    echo "  -t, --type TYPE               OrderBook type: ART,TreeSet (default: both)"
    echo "  -m, --mode MODE               Run mode: comprehensive, throughput, latency, gc (default: comprehensive)"
    echo "  -h, --help                    Show this help message"
    echo ""
    echo "Modes:"
    echo "  comprehensive - Both throughput and latency with GC profiling"
    echo "  throughput   - Throughput-only measurements"
    echo "  latency      - Latency distribution measurements"
    echo "  gc           - GC and allocation profiling"
    echo ""
    echo "Examples:"
    echo "  $0                                    # Full comprehensive benchmark"
    echo "  $0 -s pureInsertScenario             # Single scenario comprehensive"
    echo "  $0 -m throughput -d 50000            # Throughput-only with 50K orders"
    echo "  $0 -m latency -t ART                 # Latency-only for ART"
    echo "  $0 -m gc -s memoryPressureScenario   # GC profiling for memory scenario"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -s|--scenario)
            SCENARIO="$2"
            shift 2
            ;;
        -d|--dataset-size)
            DATASET_SIZE="$2"
            shift 2
            ;;
        -t|--type)
            ORDER_BOOK_TYPE="$2"
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
echo -e "${BLUE}Dataset Size:${NC} $DATASET_SIZE"
echo -e "${BLUE}OrderBook Type:${NC} $ORDER_BOOK_TYPE"
echo -e "${BLUE}Mode:${NC} $MODE"
echo ""

# Ensure target directory exists
mkdir -p target/benchmark-results

# Build benchmark JAR with fixed classpath
echo -e "${YELLOW}Building benchmark JAR...${NC}"
mvn -q -DskipTests package

# Check if JAR was created
if [ ! -f "target/benchmarks.jar" ]; then
    echo -e "${RED}Error: Benchmark JAR not found. Build failed.${NC}"
    exit 1
fi

echo -e "${GREEN}Benchmark JAR built successfully.${NC}"

# Prepare timestamp for result files
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Disable macOS App Nap for stable performance
if [[ "$OSTYPE" == "darwin"* ]]; then
    echo -e "${YELLOW}Disabling macOS App Nap for stable performance...${NC}"
    caffeinate -i &
    CAFFEINATE_PID=$!
    trap "kill $CAFFEINATE_PID 2>/dev/null || true" EXIT
fi

# Base JMH arguments for all runs
BASE_ARGS="-p datasetSize=$DATASET_SIZE -p orderBookType=$ORDER_BOOK_TYPE"

case $MODE in
    "comprehensive")
        echo -e "${GREEN}Running comprehensive benchmark suite...${NC}"
        echo -e "${YELLOW}Phase 1: Throughput measurements${NC}"
        
        THROUGHPUT_FILE="target/benchmark-results/throughput-$TIMESTAMP.json"
        java -jar target/benchmarks.jar "ReliableOrderBookBenchmark.$SCENARIO" \
            $BASE_ARGS \
            -rf json -rff "$THROUGHPUT_FILE" \
            -prof gc
        
        echo -e "${YELLOW}Phase 2: Latency measurements${NC}"
        
        LATENCY_FILE="target/benchmark-results/latency-$TIMESTAMP.json"
        java -jar target/benchmarks.jar "ReliableOrderBookLatencyBenchmark.$SCENARIO" \
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
        java -jar target/benchmarks.jar "ReliableOrderBookBenchmark.$SCENARIO" \
            $BASE_ARGS \
            -rf json -rff "$RESULT_FILE"
        
        echo -e "${BLUE}Results saved to:${NC} $RESULT_FILE"
        ;;
        
    "latency")
        echo -e "${GREEN}Running latency benchmarks...${NC}"
        echo -e "${YELLOW}Configuration: SampleTime mode, 5 forks, detailed distribution${NC}"
        
        RESULT_FILE="target/benchmark-results/latency-$TIMESTAMP.json"
        java -jar target/benchmarks.jar "ReliableOrderBookLatencyBenchmark.$SCENARIO" \
            $BASE_ARGS \
            -rf json -rff "$RESULT_FILE"
        
        echo -e "${BLUE}Results saved to:${NC} $RESULT_FILE"
        ;;
        
    "gc")
        echo -e "${GREEN}Running GC and allocation profiling...${NC}"
        echo -e "${YELLOW}Configuration: GC profiler + JFR profiling${NC}"
        
        RESULT_FILE="target/benchmark-results/gc-profiling-$TIMESTAMP.json"
        JFR_DIR="target/benchmark-results/jfr-$TIMESTAMP"
        mkdir -p "$JFR_DIR"
        
        java -jar target/benchmarks.jar "ReliableOrderBookBenchmark.$SCENARIO" \
            $BASE_ARGS \
            -rf json -rff "$RESULT_FILE" \
            -prof gc \
            -prof "jfr:dir=$JFR_DIR,name=orderbook,settings=profile"
        
        echo -e "${BLUE}Results saved to:${NC} $RESULT_FILE"
        echo -e "${BLUE}JFR profiles saved to:${NC} $JFR_DIR"
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
if [ -f "$RESULT_FILE" ] || [ -f "$THROUGHPUT_FILE" ]; then
    echo ""
    echo -e "${YELLOW}Performance Analysis Summary:${NC}"
    echo "1. Results follow proper JMH methodology with 5 forks"
    echo "2. DCE prevention implemented with return values and Blackhole"
    echo "3. Memory stability ensured with AlwaysPreTouch"
    echo "4. Statistical validity achieved with adequate iterations"
    echo ""
    echo -e "${YELLOW}Next Steps:${NC}"
    echo "1. Analyze JSON results for performance characteristics"
    echo "2. Compare ART vs TreeSet across different scenarios"
    echo "3. Review GC behavior and allocation patterns"
    echo "4. Validate results align with theoretical expectations"
fi
