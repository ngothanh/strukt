package com.submicro.strukt.art;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Complex test scenarios for LongAdaptiveRadixTreeMap that exercise ArtNode4 functionality
 * through realistic tree operations. These tests are designed to help understand how ArtNode4
 * handles branching at different levels and path compression scenarios.
 * <p>
 * Each test focuses on complex scenarios with keys that differ in multiple bytes (4-5 byte differences)
 * and trigger branching at various levels to demonstrate ArtNode4's internal behavior.
 */
class LongAdaptiveRadixTreeMapTest {

    private LongAdaptiveRadixTreeMap<String> tree;

    @BeforeEach
    void setUp() {
        tree = new LongAdaptiveRadixTreeMap<>();
    }

    @Test
    @DisplayName("Complex Scenario 1: Multi-level branching with shared prefixes")
    void testMultiLevelBranchingWithSharedPrefixes() {
        /*
         * This test demonstrates how ArtNode4 handles keys that share prefixes at different levels.
         * Keys are chosen to trigger branching at various byte positions.
         * 
         * Key analysis (in hex, showing byte-level differences):
         * 0x123456789ABCDEF0 = [F0, DE, BC, 9A, 78, 56, 34, 12] (little-endian bytes)
         * 0x123456789ABCDE01 = [01, DE, BC, 9A, 78, 56, 34, 12] - differs at byte 0
         * 0x123456789ABC1234 = [34, 12, BC, 9A, 78, 56, 34, 12] - differs at bytes 0,1
         * 0x12345678FEDCBA98 = [98, BA, DC, FE, 78, 56, 34, 12] - differs at bytes 0,1,2,3
         * 
         * This creates a complex tree structure where ArtNode4 must handle:
         * - Path compression for shared prefixes
         * - Branching at different levels
         * - Linear search within nodes
         */
        
        // Insert keys with complex branching patterns
        tree.put(0x123456789ABCDEF0L, "key1_long_prefix");
        tree.put(0x123456789ABCDE01L, "key2_diff_byte0");
        tree.put(0x123456789ABC1234L, "key3_diff_byte01");
        tree.put(0x12345678FEDCBA98L, "key4_diff_byte0123");

        // Verify all keys are accessible
        assertEquals("key1_long_prefix", tree.get(0x123456789ABCDEF0L));
        assertEquals("key2_diff_byte0", tree.get(0x123456789ABCDE01L));
        assertEquals("key3_diff_byte01", tree.get(0x123456789ABC1234L));
        assertEquals("key4_diff_byte0123", tree.get(0x12345678FEDCBA98L));
        
        // Verify non-existing keys return null (test branching precision)
        assertNull(tree.get(0x123456789ABCDEF1L)); // Close but different
        assertNull(tree.get(0x123456789ABCDE02L)); // Close but different
        assertNull(tree.get(0x123456789ABC1235L)); // Close but different
    }

    @Test
    @DisplayName("Complex Scenario 2: Path compression with deep branching")
    void testPathCompressionWithDeepBranching() {
        /*
         * This test shows how ArtNode4 handles path compression when keys share
         * long prefixes but eventually diverge at deeper levels.
         * 
         * Key analysis (focusing on where they diverge):
         * 0xABCDEF1234567890 = [90, 78, 56, 34, 12, EF, CD, AB]
         * 0xABCDEF1234567891 = [91, 78, 56, 34, 12, EF, CD, AB] - differs only at byte 0
         * 0xABCDEF1234567892 = [92, 78, 56, 34, 12, EF, CD, AB] - differs only at byte 0  
         * 0xABCDEF123456ABCD = [CD, AB, 56, 34, 12, EF, CD, AB] - differs at bytes 0,1
         * 
         * This tests:
         * - Deep path compression (7 bytes shared)
         * - Branching at the deepest level (byte 0)
         * - ArtNode4's ability to handle very similar keys
         */
        
        tree.put(0xABCDEF1234567890L, "deep_path_1");
        tree.put(0xABCDEF1234567891L, "deep_path_2");
        tree.put(0xABCDEF1234567892L, "deep_path_3");
        tree.put(0xABCDEF123456ABCDL, "deep_branch");
        
        // All keys should be retrievable
        assertEquals("deep_path_1", tree.get(0xABCDEF1234567890L));
        assertEquals("deep_path_2", tree.get(0xABCDEF1234567891L));
        assertEquals("deep_path_3", tree.get(0xABCDEF1234567892L));
        assertEquals("deep_branch", tree.get(0xABCDEF123456ABCDL));
        
        // Test that the tree correctly handles the shared prefix
        assertNull(tree.get(0xABCDEF1234567893L)); // Similar but not inserted
        assertNull(tree.get(0xABCDEF123456ABCEL)); // Similar but not inserted
        assertNull(tree.get(0xABCDEF123456789AL)); // Different pattern
    }

    @Test
    @DisplayName("Complex Scenario 3: Mixed branching levels with ArtNode4 capacity")
    void testMixedBranchingLevelsWithCapacity() {
        /*
         * This test exercises ArtNode4's capacity (4 children) with keys that
         * branch at different levels, testing both the linear search and
         * the branching logic.
         * 
         * Key analysis (showing different branching patterns):
         * 0x1111111111111111 = [11, 11, 11, 11, 11, 11, 11, 11] - all same bytes
         * 0x2222222222222222 = [22, 22, 22, 22, 22, 22, 22, 22] - differs at all bytes
         * 0x1111111133333333 = [33, 33, 33, 33, 11, 11, 11, 11] - shares 4-byte prefix
         * 0x1111111144444444 = [44, 44, 44, 44, 11, 11, 11, 11] - shares 4-byte prefix
         * 
         * This creates a scenario where:
         * - Two keys share no prefix (1111... vs 2222...)
         * - Two keys share a 4-byte prefix with the first key
         * - ArtNode4 reaches its capacity of 4 children
         */
        
        tree.put(0x1111111111111111L, "pattern_all_1s");
        tree.put(0x2222222222222222L, "pattern_all_2s");
        tree.put(0x1111111133333333L, "pattern_mixed_3s");
        tree.put(0x1111111144444444L, "pattern_mixed_4s");
        
        // Verify all 4 keys (ArtNode4 at capacity)
        assertEquals("pattern_all_1s", tree.get(0x1111111111111111L));
        assertEquals("pattern_all_2s", tree.get(0x2222222222222222L));
        assertEquals("pattern_mixed_3s", tree.get(0x1111111133333333L));
        assertEquals("pattern_mixed_4s", tree.get(0x1111111144444444L));
        
        // Test edge cases around the patterns
        assertNull(tree.get(0x1111111111111112L)); // Close to first key
        assertNull(tree.get(0x1111111155555555L)); // Follows the prefix pattern but different
        assertNull(tree.get(0x3333333333333333L)); // Completely different pattern
    }

    @Test
    @DisplayName("Complex Scenario 4: Extreme branching with byte-level differences")
    void testExtremeBranchingWithByteLevelDifferences() {
        /*
         * This test demonstrates how ArtNode4 handles keys that differ at specific
         * byte positions, creating a complex tree structure with multiple branching points.
         * 
         * Key analysis (focusing on specific byte differences):
         * 0xFF00FF00FF00FF00 = [00, FF, 00, FF, 00, FF, 00, FF] - alternating pattern
         * 0xFF00FF00FF00FF01 = [01, FF, 00, FF, 00, FF, 00, FF] - differs at byte 0
         * 0xFF00FF00FF0000FF = [FF, 00, 00, FF, 00, FF, 00, FF] - differs at byte 1  
         * 0xFF00FF0000FF00FF = [FF, 00, FF, 00, 00, FF, 00, FF] - differs at byte 2
         * 
         * This tests:
         * - Branching at different specific byte positions
         * - Handling of alternating bit patterns
         * - ArtNode4's precision in byte-level comparisons
         * - Key updates on complex patterns
         */
        
        tree.put(0xFF00FF00FF00FF00L, "alternating_pattern_base");
        tree.put(0xFF00FF00FF00FF01L, "byte0_different");
        tree.put(0xFF00FF00FF0000FFL, "byte1_different");
        tree.put(0xFF00FF0000FF00FFL, "byte2_different");
        
        // Verify all complex keys are accessible
        assertEquals("alternating_pattern_base", tree.get(0xFF00FF00FF00FF00L));
        assertEquals("byte0_different", tree.get(0xFF00FF00FF00FF01L));
        assertEquals("byte1_different", tree.get(0xFF00FF00FF0000FFL));
        assertEquals("byte2_different", tree.get(0xFF00FF0000FF00FFL));
        
        // Test that similar but non-inserted keys return null
        assertNull(tree.get(0xFF00FF00FF00FF02L)); // Similar to first variant
        assertNull(tree.get(0xFF00FF00FF0001FFL)); // Similar to second variant
        assertNull(tree.get(0xFF00FF0001FF00FFL)); // Similar to third variant
        
        // Test key update on complex keys
        tree.put(0xFF00FF00FF00FF00L, "updated_base_pattern");
        assertEquals("updated_base_pattern", tree.get(0xFF00FF00FF00FF00L));
        
        // Ensure other keys are unaffected by the update
        assertEquals("byte0_different", tree.get(0xFF00FF00FF00FF01L));
        assertEquals("byte1_different", tree.get(0xFF00FF00FF0000FFL));
        assertEquals("byte2_different", tree.get(0xFF00FF0000FF00FFL));
    }

    @Test
    @DisplayName("Demonstrate early exit optimization at intermediate tree levels")
    void testEarlyExitOptimizationAtIntermediateLevels() {
        /*
         * This test creates a complex multi-level tree structure and demonstrates
         * how the early exit optimization works at intermediate levels, not just at the root.
         *
         * We'll build a tree with shared prefixes that forces branching at multiple levels,
         * then show how the optimization prevents traversal of entire subtrees.
         */

        // Build a complex tree structure with multiple branching levels
        // All keys share the prefix 0x1234567800000000 (bytes 7,6,5,4 = 12,34,56,78)
        // This will create nodes at levels 56, 48, 40, 32 before any branching occurs

        // Group 1: Keys that branch at level 24 (byte 3)
        tree.put(0x12345678AA000000L, "group1_key1");  // byte 3 = AA
        tree.put(0x12345678BB000000L, "group1_key2");  // byte 3 = BB

        // Group 2: Keys that branch at level 16 (byte 2)
        tree.put(0x12345678AA110000L, "group2_key1");  // byte 3 = AA, byte 2 = 11
        tree.put(0x12345678AA220000L, "group2_key2");  // byte 3 = AA, byte 2 = 22

        // Group 3: Keys that branch at level 8 (byte 1)
        tree.put(0x12345678AA111100L, "group3_key1");  // bytes 3,2,1 = AA,11,11
        tree.put(0x12345678AA112200L, "group3_key2");  // bytes 3,2,1 = AA,11,22

        // Group 4: Keys that branch at level 0 (byte 0)
        tree.put(0x12345678AA111111L, "group4_key1");  // bytes 3,2,1,0 = AA,11,11,11
        tree.put(0x12345678AA111122L, "group4_key2");  // bytes 3,2,1,0 = AA,11,11,22

        // Verify all keys are accessible
        assertEquals("group1_key1", tree.get(0x12345678AA000000L));
        assertEquals("group1_key2", tree.get(0x12345678BB000000L));
        assertEquals("group2_key1", tree.get(0x12345678AA110000L));
        assertEquals("group2_key2", tree.get(0x12345678AA220000L));
        assertEquals("group3_key1", tree.get(0x12345678AA111100L));
        assertEquals("group3_key2", tree.get(0x12345678AA112200L));
        assertEquals("group4_key1", tree.get(0x12345678AA111111L));
        assertEquals("group4_key2", tree.get(0x12345678AA111122L));

        /*
         * Now test the optimization at different intermediate levels:
         *
         * Test Case 1: Search for key that differs at byte 3 (level 24)
         * Search key: 0x12345678CC111111 (byte 3 = CC, but tree only has AA and BB)
         *
         * Tree structure:
         * - Root at level 56 (shared prefix 0x1234567800000000)
         * - Child nodes at level 24 handling byte 3 (AA branch vs BB branch)
         * - When searching the AA subtree with nodeLevel=16, the condition triggers:
         *   level=24, nodeLevel=16 (level != nodeLevel ✓)
         *   key ^ nodeKey = 0x12345678CC111111 ^ 0x12345678AA111111 = 0x0000000066000000
         *   mask = (-1L << (16 + 8)) = 0xFFFFFFFFFF000000
         *   (0x0000000066000000 & 0xFFFFFFFFFF000000) = 0x0000000066000000 ≠ 0 ✓
         *
         *   This detects that the keys differ in byte 3 (above level 16+8=24),
         *   so the entire AA subtree can be skipped!
         */
        assertNull(tree.get(0x12345678CC111111L));

        /*
         * Test Case 2: Search for key that differs at byte 2 (level 16)
         * Search key: 0x12345678AA331111 (follows AA branch but byte 2 = 33)
         *
         * This will:
         * 1. Successfully traverse to the AA subtree at level 24
         * 2. At level 16 nodes (handling byte 2), find branches for 11 and 22
         * 3. When checking the 11-branch subtree with nodeLevel=8:
         *    level=16, nodeLevel=8 (level != nodeLevel ✓)
         *    key ^ nodeKey = 0x12345678AA331111 ^ 0x12345678AA111111 = 0x0000000000220000
         *    mask = (-1L << (8 + 8)) = 0xFFFFFFFFFFFF0000
         *    (0x0000000000220000 & 0xFFFFFFFFFFFF0000) = 0x0000000000220000 ≠ 0 ✓
         *
         *    This detects the difference in byte 2, avoiding traversal of the 11-subtree
         */
        assertNull(tree.get(0x12345678AA331111L));

        /*
         * Test Case 3: Search for key that differs at byte 1 (level 8)
         * Search key: 0x12345678AA113311 (follows AA->11 path but byte 1 = 33)
         *
         * This will:
         * 1. Successfully traverse AA subtree (level 24)
         * 2. Successfully traverse 11 subtree (level 16)
         * 3. At level 8 nodes, find branches for byte 1 values 11 and 22
         * 4. When checking the 11-branch subtree with nodeLevel=0:
         *    level=8, nodeLevel=0 (level != nodeLevel ✓)
         *    key ^ nodeKey = 0x12345678AA113311 ^ 0x12345678AA111111 = 0x0000000000002200
         *    mask = (-1L << (0 + 8)) = 0xFFFFFFFFFFFFFF00
         *    (0x0000000000002200 & 0xFFFFFFFFFFFFFF00) = 0x0000000000002200 ≠ 0 ✓
         *
         *    This detects the difference in byte 1, avoiding leaf-level comparison
         */
        assertNull(tree.get(0x12345678AA113311L));

        /*
         * Test Case 4: Key that only differs in byte 0 (no early exit possible)
         * Search key: 0x12345678AA111133 (follows full path but byte 0 = 33)
         *
         * This will traverse the entire path:
         * AA subtree -> 11 subtree -> 11 subtree -> leaf comparison
         * Only fails at the final leaf comparison since all intermediate
         * levels match perfectly.
         */
        assertNull(tree.get(0x12345678AA111133L));
    }

    @Test
    @DisplayName("Test early termination in sorted key search")
    void testEarlyTerminationInSortedKeySearch() {
        /*
         * This test verifies that the linear search in ArtNode4 can terminate early
         * when the search key is smaller than the current key being examined,
         * since keys are stored in sorted order.
         */

        // Insert keys that will create a node with multiple children in sorted order
        // All keys share prefix 0x1234567800000000, differing only in byte 0
        tree.put(0x1234567800000010L, "key_10");  // byte 0 = 0x10
        tree.put(0x1234567800000030L, "key_30");  // byte 0 = 0x30
        tree.put(0x1234567800000050L, "key_50");  // byte 0 = 0x50
        tree.put(0x1234567800000070L, "key_70");  // byte 0 = 0x70

        // Verify all keys are accessible
        assertEquals("key_10", tree.get(0x1234567800000010L));
        assertEquals("key_30", tree.get(0x1234567800000030L));
        assertEquals("key_50", tree.get(0x1234567800000050L));
        assertEquals("key_70", tree.get(0x1234567800000070L));

        /*
         * Test early termination scenarios:
         *
         * When searching for 0x1234567800000025 (byte 0 = 0x25):
         * - Check key[0] = 0x10: 0x25 > 0x10, continue
         * - Check key[1] = 0x30: 0x25 < 0x30, BREAK (early termination)
         * - Never checks key[2] = 0x50 or key[3] = 0x70
         *
         * This demonstrates the optimization where we don't need to check
         * all children when we know the search key is smaller than remaining keys.
         */
        assertNull(tree.get(0x1234567800000025L)); // Between 0x10 and 0x30
        assertNull(tree.get(0x1234567800000045L)); // Between 0x30 and 0x50
        assertNull(tree.get(0x1234567800000065L)); // Between 0x50 and 0x70
        assertNull(tree.get(0x1234567800000005L)); // Smaller than 0x10
        assertNull(tree.get(0x1234567800000080L)); // Larger than 0x70

        /*
         * Edge cases that should still work correctly:
         */
        assertNull(tree.get(0x123456780000000FL)); // Just before first key
        assertNull(tree.get(0x1234567800000011L)); // Just after first key
        assertNull(tree.get(0x1234567800000071L)); // Just after last key
    }
}
