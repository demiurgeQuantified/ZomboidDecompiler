package com.github.zomboiddecompiler.steam;

import java.util.Map;
import java.util.HashMap;

/**
 * Representation of a Valve Data File.
 */
public class VDFBlock {
    private final Map<String, String> values = new HashMap<>();
    private final Map<String, VDFBlock> blocks = new HashMap<>();

    /**
     * Returns the value stored with the specified key.
     * @param key The key of the value.
     * @return The string value.
     */
    public String getValue(String key) {
        return values.get(key);
    }

    /**
     * Returns a sub-block stored with the specified key.
     * @param key The key of the block.
     * @return The block.
     */
    public VDFBlock getBlock(String key) {
        return blocks.get(key);
    }

    /**
     * Queries whether the block contains a value with the specified key.
     * @param key The key to query.
     * @return Whether there is a value by that key.
     */
    public boolean hasValue(String key) {
        return values.containsKey(key);
    }

    /**
     * Queries whether the block contains a sub-block with the specified key.
     * @param key The key to query.
     * @return Whether there is a block by that key.
     */
    public boolean hasBlock(String key) {
        return blocks.containsKey(key);
    }

    /**
     * Queries whether the block contains a value or block with the specified key.
     * @param key The key to query.
     * @return Whether there is a value or block by that key.
     */
    public boolean hasKey(String key) {
        return hasValue(key) || hasBlock(key);
    }

    /**
     * Recursively parses a VDF block. A VDF file is itself a block.
     * @param raw The raw text of the VDF block.
     * @return A VDFBlock representation of the input.
     */
    public static VDFBlock parse(String raw) {
        VDFBlock block = new VDFBlock();
        int index = 0;
        int startKey;
        while ((startKey = raw.indexOf('\"', index)) != -1) {
            int endKey = raw.indexOf('\"', startKey + 1);
            String key = raw.substring(startKey + 1, endKey);

            int startValue = getNextNonWhitespaceIndex(raw, endKey);
            int endValue;
            if (raw.codePointAt(startValue) == '{') {
                endValue = getEndOfBlock(raw, startValue);
                block.blocks.put(key,
                        parse(raw.substring(startValue + 1, endValue)));
            } else {
                assert raw.codePointAt(startValue) == '\"';

                endValue = raw.indexOf('\"', startValue + 1);
                block.values.put(key,
                        raw.substring(startValue + 1, endValue));
            }

            index = endValue + 1;
        }

        return block;
    }

    /**
     * Returns the position of the matching closing bracket to an opening bracket.
     * @param vdf The raw VDF text to search through.
     * @param startIndex The position of the opening bracket of the block.
     * @return The position of the matching closing bracket.
     * <br> -1 indicates no matching bracket was found: this generally means the VDF is formatted incorrectly.
     */
    private static int getEndOfBlock(String vdf, int startIndex) {
        assert vdf.codePointAt(startIndex) == '{';

        int depth = 1;
        for (int i = startIndex + 1; i < vdf.length(); i++) {
            int codePoint = vdf.codePointAt(i);
            if (codePoint == '{') {
                depth++;
            } else if (codePoint == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Returns the next code point index that is not whitespace.
     * @param string The string to search.
     * @param startIndex The index to start the search from.
     * @return The index of the next non-whitespace code point.
     * <br> -1 indicates the entire remaining string is whitespace.
     */
    private static int getNextNonWhitespaceIndex(String string, int startIndex) {
        for (int i = startIndex + 1; i < string.length(); i++) {
            int codePoint = string.codePointAt(i);
            if (codePoint != '\t' && codePoint != '\n' && codePoint != ' ') {
                return i;
            }
        }
        return -1;
    }

    private VDFBlock() {}
}
