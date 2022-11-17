package edu.alibaba.mpc4j.dp.stream.structure;

import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * StreamCounter test.
 * <p>
 * stream_counter_example_data.txt: an example data provided by Xiaochen Li, with the following features:
 * <ul>
 *     <li> Keys (num = 37): {480, 484, ..., 520}</li>
 *     <li> Max Values: Key = 500, Value = 817.</li>
 *     <li> Min Values: Keys = 480, 481, 482, 518, Value = 0.</li>
 * </ul>
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/16
 */
public class TestStreamCounter {
    /**
     * File path for stream_counter_example_data.txt
     */
    private static final String STREAM_COUNTER_EXAMPLE_DATA_PATH = Objects.requireNonNull(
        TestStreamCounter.class.getClassLoader().getResource("stream_counter_example_data.txt")
    ).getPath();
    /**
     * Key set for stream_counter_example_data.txt
     */
    private static final Set<String> STREAM_COUNTER_EXAMPLE_KEYS = IntStream.rangeClosed(480, 520)
        .mapToObj(String::valueOf).collect(Collectors.toSet());
    /**
     * Key num for stream_counter_example_data.txt
     */
    private static final int STREAM_COUNTER_EXAMPLE_KEY_NUM = STREAM_COUNTER_EXAMPLE_KEYS.size();

    @Test
    public void testNaiveStreamCounterExample() throws IOException {
        NaiveStreamCounter<String> streamCounter = new NaiveStreamCounter<>();
        List<Map.Entry<String, Integer>> countList = getExampleCountList(streamCounter);
        assertExampleTopEntries(countList);
    }

    @Test
    public void testFullHeavyPartHeavyGuardianStreamCounterExample() throws IOException {
        HeavyGuardianStreamCounter<String> streamCounter = new HeavyGuardianStreamCounter<>(
            1, STREAM_COUNTER_EXAMPLE_KEY_NUM, 0
        );
        List<Map.Entry<String, Integer>> countList = getExampleCountList(streamCounter);
        assertExampleTopEntries(countList);
    }

    @Test
    public void testFullPartHeavyGuardianStreamCounterExample() throws IOException {
        int keyNum = STREAM_COUNTER_EXAMPLE_KEY_NUM;
        HeavyGuardianStreamCounter<String> streamCounter
            = new HeavyGuardianStreamCounter<>(1, keyNum / 2, keyNum - keyNum / 2);
        List<Map.Entry<String, Integer>> countList = getExampleCountList(streamCounter);
        assertExampleTopEntries(countList);
    }

    @Test
    public void testHalfPartHeavyGuardianStreamCounterExample() throws IOException {
        HeavyGuardianStreamCounter<String> streamCounter
            = new HeavyGuardianStreamCounter<>(1, STREAM_COUNTER_EXAMPLE_KEY_NUM / 2, 0);
        List<Map.Entry<String, Integer>> countList = getExampleCountList(streamCounter);
        assertExampleTopEntries(countList);
    }

    private List<Map.Entry<String, Integer>> getExampleCountList(StreamCounter<String> streamCounter) throws IOException {
        Files.lines(Paths.get(STREAM_COUNTER_EXAMPLE_DATA_PATH)).forEach(streamCounter::insert);
        Map<String, Integer> countMap = STREAM_COUNTER_EXAMPLE_KEYS.stream()
            .collect(Collectors.toMap(item -> item, streamCounter::query));
        List<Map.Entry<String, Integer>> countList = new ArrayList<>(countMap.entrySet());
        // descending sort
        countList.sort(Comparator.comparingInt(Map.Entry::getValue));
        Collections.reverse(countList);
        return countList;
    }

    private void assertExampleTopEntries(List<Map.Entry<String, Integer>> orderedCountList) {
        Map.Entry<String, Integer> maxEntry = orderedCountList.get(0);
        Assert.assertEquals("500", maxEntry.getKey());
        Assert.assertEquals(817, maxEntry.getValue().intValue());
        Map.Entry<String, Integer> secondMaxEntry = orderedCountList.get(1);
        Assert.assertEquals("499", secondMaxEntry.getKey());
        Assert.assertEquals(792, secondMaxEntry.getValue().intValue());
    }
}
