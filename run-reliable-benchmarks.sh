#!/bin/bash

# Reliable OrderBook Benchmark Runner Script
# Implements proper JMH methodology with multiple forks, adequate warmup, and GC profiling

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
    echo -e "${BLUE}Reliable OrderBook Benchmark Runner${NC}"
    echo ""
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  -s, --scenario PATTERN       Benchmark scenario pattern (default: all)"
    echo "  -d, --dataset-size SIZE       Dataset size (default: 10000)"
    echo "  -t, --type TYPE               OrderBook type: ART,TreeSet (default: both)"
    echo "  -m, --mode MODE               Run mode: comprehensive, quick, gc, memory (default: comprehensive)"
    echo "  -h, --help                    Show this help message"
    echo ""
    echo "Scenarios:"
    echo "  pureInsertScenario     - Pure order insertion (no matches)"
    echo "  pureMatchScenario      - Pure order matching"
    echo "  partialMatchScenario   - Partial order matching"
    echo "  randomMixScenario      - Random mix of operations"
    echo "  hotspotMatchScenario   - Hotspot price concentration"
    echo "  coldBookScenario       - Sparse order book"
    echo "  memoryPressureScenario - Memory allocation pressure"
    echo ""
    echo "Modes:"
    echo "  comprehensive - Full benchmark with 3 forks, 5 warmup, 10 measurement iterations"
    echo "  quick        - Quick validation with minimal iterations"
    echo "  gc           - Comprehensive with GC profiling"
    echo "  memory       - Memory allocation profiling"
    echo ""
    echo "Examples:"
    echo "  $0                                    # Run comprehensive benchmark"
    echo "  $0 -s pureInsertScenario             # Run only pure insert scenario"
    echo "  $0 -s pureInsertScenario -d 100000   # Pure insert with 100K orders"
    echo "  $0 -t ART -m quick                   # Quick test with ART only"
    echo "  $0 -m gc -d 50000                    # GC profiling with 50K orders"
    echo "  $0 -m memory -s memoryPressureScenario # Memory profiling"
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

echo -e "${GREEN}=== Reliable OrderBook Benchmark Suite ===${NC}"
echo -e "${BLUE}Scenario:${NC} $SCENARIO"
echo -e "${BLUE}Dataset Size:${NC} $DATASET_SIZE"
echo -e "${BLUE}OrderBook Type:${NC} $ORDER_BOOK_TYPE"
echo -e "${BLUE}Mode:${NC} $MODE"
echo ""

# Ensure target directory exists
mkdir -p target/benchmark-results

# Compile project
echo -e "${YELLOW}Compiling project...${NC}"
mvn clean compile test-compile -q

# Build classpath
echo -e "${YELLOW}Building classpath...${NC}"
CLASSPATH="target/test-classes:target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q)"

# Prepare timestamp for result files
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Base JMH command
JMH_BASE="java -cp \"$CLASSPATH\" com.submicro.strukt.benchmark.ReliableOrderBookBenchmarkRunner"

case $MODE in
    "comprehensive")
        echo -e "${GREEN}Running comprehensive benchmark suite...${NC}"
        echo -e "${YELLOW}Configuration: 3 forks, 5 warmup iterations, 10 measurement iterations${NC}"
        
        RESULT_FILE="target/benchmark-results/reliable-comprehensive-$TIMESTAMP.json"
        JMH_CMD="$JMH_BASE \"$SCENARIO\" \"$DATASET_SIZE\" \"$ORDER_BOOK_TYPE\""
        JMH_CMD="$JMH_CMD -rf json -rff $RESULT_FILE"
        ;;
        
    "quick")
        echo -e "${GREEN}Running quick validation...${NC}"
        echo -e "${YELLOW}Configuration: 1 fork, 1 warmup iteration, 1 measurement iteration${NC}"

        RESULT_FILE="target/benchmark-results/reliable-quick-$TIMESTAMP.json"
        JMH_CMD="java -cp \"$CLASSPATH\" -Xmx2G -Xms2G"
        JMH_CMD="$JMH_CMD org.openjdk.jmh.Main \"ReliableOrderBookBenchmark.$SCENARIO\""
        JMH_CMD="$JMH_CMD -p datasetSize=$DATASET_SIZE -p orderBookType=$ORDER_BOOK_TYPE"
        JMH_CMD="$JMH_CMD -wi 1 -i 1 -f 1 -t 1"
        JMH_CMD="$JMH_CMD -rf json -rff \"$RESULT_FILE\""
        ;;
        
    "gc")
        echo -e "${GREEN}Running benchmark with GC profiling...${NC}"
        echo -e "${YELLOW}Configuration: 3 forks, 5 warmup, 10 measurement + GC profiling${NC}"
        
        RESULT_FILE="target/benchmark-results/reliable-gc-$TIMESTAMP.json"
        GC_LOG="target/benchmark-results/gc-reliable-$TIMESTAMP.log"
        
        JMH_CMD="java -cp \"$CLASSPATH\""
        JMH_CMD="$JMH_CMD -Xmx4G -Xms4G -XX:+UseG1GC -XX:+AlwaysPreTouch"
        JMH_CMD="$JMH_CMD -XX:MaxGCPauseMillis=200 -Xlog:gc*:$GC_LOG"
        JMH_CMD="$JMH_CMD org.openjdk.jmh.Main \"ReliableOrderBookBenchmark.$SCENARIO\""
        JMH_CMD="$JMH_CMD -p datasetSize=$DATASET_SIZE -p orderBookType=$ORDER_BOOK_TYPE"
        JMH_CMD="$JMH_CMD -wi 5 -i 10 -f 3 -t 1"
        JMH_CMD="$JMH_CMD -prof gc"
        JMH_CMD="$JMH_CMD -rf json -rff $RESULT_FILE"
        
        echo -e "${BLUE}GC log will be saved to:${NC} $GC_LOG"
        ;;
        
    "memory")
        echo -e "${GREEN}Running memory allocation profiling...${NC}"
        echo -e "${YELLOW}Configuration: 3 forks, 3 warmup, 5 measurement + allocation profiling${NC}"
        
        RESULT_FILE="target/benchmark-results/reliable-memory-$TIMESTAMP.json"
        
        JMH_CMD="java -cp \"$CLASSPATH\""
        JMH_CMD="$JMH_CMD -Xmx4G -Xms4G -XX:+UseG1GC"
        JMH_CMD="$JMH_CMD org.openjdk.jmh.Main \"ReliableOrderBookBenchmark.$SCENARIO\""
        JMH_CMD="$JMH_CMD -p datasetSize=$DATASET_SIZE -p orderBookType=$ORDER_BOOK_TYPE"
        JMH_CMD="$JMH_CMD -wi 3 -i 5 -f 3 -t 1"
        JMH_CMD="$JMH_CMD -prof gc -prof stack"
        JMH_CMD="$JMH_CMD -rf json -rff $RESULT_FILE"
        ;;
        
    *)
        echo -e "${RED}Unknown mode: $MODE${NC}"
        usage
        exit 1
        ;;
esac

echo -e "${BLUE}Results will be saved to:${NC} $RESULT_FILE"
echo ""

# Execute the benchmark
echo -e "${GREEN}Starting benchmark execution...${NC}"
eval $JMH_CMD

echo ""
echo -e "${GREEN}Benchmark completed successfully!${NC}"
echo -e "${BLUE}Results saved to:${NC} $RESULT_FILE"

if [ "$MODE" = "gc" ]; then
    echo -e "${BLUE}GC log saved to:${NC} $GC_LOG"
    echo ""
    echo -e "${YELLOW}GC Analysis Summary:${NC}"
    if [ -f "$GC_LOG" ]; then
        echo "Total GC events: $(grep -c "GC(" "$GC_LOG" || echo "0")"
        echo "Average pause time: $(grep "Pause" "$GC_LOG" | awk '{sum+=$NF; count++} END {if(count>0) print sum/count "ms"; else print "N/A"}')"
    fi
fi

echo ""
echo -e "${GREEN}Benchmark suite execution completed!${NC}"
echo -e "${YELLOW}Next steps:${NC}"
echo "1. Analyze results in $RESULT_FILE"
echo "2. Compare performance between ART and TreeSet implementations"
echo "3. Review GC behavior if GC profiling was enabled"
echo "4. Run with larger dataset sizes for scaling analysis"
