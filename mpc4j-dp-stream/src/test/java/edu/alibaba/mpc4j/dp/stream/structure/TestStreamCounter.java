package edu.alibaba.mpc4j.dp.stream.structure;

import edu.alibaba.mpc4j.dp.stream.tool.StreamDataUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
    private static final String EXAMPLE_DATA_PATH = Objects.requireNonNull(
        TestStreamCounter.class.getClassLoader().getResource("stream_counter_example_data.txt")
    ).getPath();
    /**
     * Key set for stream_counter_example_data.txt
     */
    private static final Set<String> EXAMPLE_DOMAIN = IntStream.rangeClosed(480, 520)
        .mapToObj(String::valueOf).collect(Collectors.toSet());
    /**
     * Key num for stream_counter_example_data.txt
     */
    private static final int EXAMPLE_D = EXAMPLE_DOMAIN.size();

    @Test
    public void testNaiveStreamCounterExample() throws IOException {
        NaiveStreamCounter streamCounter = new NaiveStreamCounter();
        List<Map.Entry<String, Integer>> countList = getExampleCountList(streamCounter);
        assertExampleTopEntries(countList);
    }

    @Test
    public void testFullHeavyPartHeavyGuardianExample() throws IOException {
        HeavyGuardian streamCounter = new HeavyGuardian(
            1, EXAMPLE_D, 0
        );
        List<Map.Entry<String, Integer>> countList = getExampleCountList(streamCounter);
        assertExampleTopEntries(countList);
    }

    @Test
    public void testFullPartHeavyGuardianExample() throws IOException {
        int keyNum = EXAMPLE_D;
        HeavyGuardian streamCounter
            = new HeavyGuardian(1, keyNum / 2, keyNum - keyNum / 2);
        List<Map.Entry<String, Integer>> countList = getExampleCountList(streamCounter);
        assertExampleTopEntries(countList);
    }

    @Test
    public void testHalfPartHeavyGuardianExample() throws IOException {
        HeavyGuardian streamCounter
            = new HeavyGuardian(1, EXAMPLE_D / 2, 0);
        List<Map.Entry<String, Integer>> countList = getExampleCountList(streamCounter);
        assertExampleTopEntries(countList);
    }

    private List<Map.Entry<String, Integer>> getExampleCountList(StreamCounter streamCounter) throws IOException {
        Stream<String> dataStream = StreamDataUtils.obtainItemStream(EXAMPLE_DATA_PATH);
        dataStream.forEach(streamCounter::insert);
        dataStream.close();
        Map<String, Integer> countMap = EXAMPLE_DOMAIN.stream()
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
