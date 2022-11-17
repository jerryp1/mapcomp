package edu.alibaba.mpc4j.dp.stream.tool;

import edu.alibaba.mpc4j.dp.stream.structure.NaiveStreamCounter;
import edu.alibaba.mpc4j.dp.stream.structure.TestStreamCounter;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * stream data utility functions test.
 *
 * @author Weiran Liu
 * @date 2022/11/17
 */
public class StreamDataUtilsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamDataUtilsTest.class);

    @Test
    public void testChess() throws IOException {
        String path = Objects.requireNonNull(
            TestStreamCounter.class.getClassLoader().getResource("chess.dat")
        ).getPath();
        Stream<String> itemStream = StreamDataUtils.obtainItemStream(path);
        assertData("chess", itemStream);
    }

    @Test
    public void testConnect() throws IOException {
        String path = Objects.requireNonNull(
            TestStreamCounter.class.getClassLoader().getResource("connect.dat")
        ).getPath();
        Stream<String> itemStream = StreamDataUtils.obtainItemStream(path);
        assertData("connect", itemStream);
    }

    @Test
    public void testMushroom() throws IOException {
        String path = Objects.requireNonNull(
            TestStreamCounter.class.getClassLoader().getResource("mushroom.dat")
        ).getPath();
        Stream<String> itemStream = StreamDataUtils.obtainItemStream(path);
        assertData("mushroom", itemStream);
    }

    private void assertData(String name, Stream<String> itemStream) {
        NaiveStreamCounter<String> streamCounter = new NaiveStreamCounter<>();
        itemStream.forEach(streamCounter::insert);
        Map<String, Integer> countMap = streamCounter.getItemSet().stream()
            .collect(Collectors.toMap(item -> item, streamCounter::query));
        List<Map.Entry<String, Integer>> countList = new ArrayList<>(countMap.entrySet());
        // descending sort
        countList.sort(Comparator.comparingInt(Map.Entry::getValue));
        Collections.reverse(countList);
        LOGGER.info(
            "{}: # items = {}, # distinct items = {}, max items = <{}, {}>",
            name, streamCounter.getInsertNum(), streamCounter.getItemSet().size(),
            countList.get(0).getKey(), countList.get(0).getValue()
        );
    }
}
