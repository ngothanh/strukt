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
}
