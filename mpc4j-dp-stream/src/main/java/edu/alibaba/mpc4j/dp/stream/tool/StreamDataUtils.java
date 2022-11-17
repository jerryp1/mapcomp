package edu.alibaba.mpc4j.dp.stream.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * streaming data (.dat)
 *
 * @author Weiran Liu
 * @date 2022/11/17
 */
public class StreamDataUtils {

    private StreamDataUtils() {
        // empty
    }

    /**
     * Obtain item stream from the .dat file.
     *
     * @param path file path.
     * @return item string.
     */
    public static Stream<String> obtainItemStream(String path) throws IOException {
        return Files.lines(Paths.get(path))
            // each line contains more items, split by " "
            .map(line -> line.split(" "))
            .flatMap(Arrays::stream);
    }
}
