package edu.alibaba.mpc4j.dp.stream.tool.structure;

import edu.alibaba.mpc4j.dp.stream.structure.HeavyGuardianStreamCounter;
import edu.alibaba.mpc4j.dp.stream.structure.NaiveStreamCounter;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Weiran Liu
 * @date 2022/11/16
 */
public class TestStreamCounter {

    @Test
    public void testNaiveStreamCounter() throws IOException {
        String filePath = TestStreamCounter.class.getClassLoader().getResource("stream_counter_example_data.txt").getPath();
        Set<String> items = Files.lines(Paths.get(filePath)).collect(Collectors.toSet());
        NaiveStreamCounter<String> naiveStreamCounter = new NaiveStreamCounter<>();
        Files.lines(Paths.get(filePath)).forEach(naiveStreamCounter::insert);
        Map<String, Integer> countMap = items.stream().collect(Collectors.toMap(item -> item, naiveStreamCounter::query));
        List<Map.Entry<String, Integer>> countList = new ArrayList<>(countMap.entrySet());
        countList.sort(Comparator.comparingInt(Map.Entry::getValue));
        for (Map.Entry<String, Integer> count : countList) {
            System.out.println("Key = " + count.getKey() + ", Value = " + count.getValue());
        }
    }

    @Test
    public void testHeavyGuardian() throws IOException {
        String filePath = TestStreamCounter.class.getClassLoader().getResource("stream_counter_example_data.txt").getPath();
        Set<String> items = Files.lines(Paths.get(filePath)).collect(Collectors.toSet());
        HeavyGuardianStreamCounter<String> streamCounter = new HeavyGuardianStreamCounter<>(1, 20, 0);
        Files.lines(Paths.get(filePath)).forEach(streamCounter::insert);
        Map<String, Integer> countMap = items.stream().collect(Collectors.toMap(item -> item, streamCounter::query));
        List<Map.Entry<String, Integer>> countList = new ArrayList<>(countMap.entrySet());
        countList.sort(Comparator.comparingInt(Map.Entry::getValue));
        for (Map.Entry<String, Integer> count : countList) {
            System.out.println("Key = " + count.getKey() + ", Value = " + count.getValue());
        }
    }
}
