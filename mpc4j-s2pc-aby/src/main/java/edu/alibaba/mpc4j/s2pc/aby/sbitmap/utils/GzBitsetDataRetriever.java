package edu.alibaba.mpc4j.s2pc.aby.sbitmap.utils;

import com.google.common.base.Preconditions;
import org.roaringbitmap.BitSetUtil;
import org.roaringbitmap.RoaringBitmap;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * This class will retrieve bitmaps that have been previously stored in a portable format (as lists of longs) inside a
 * gz file. The source code is modified from:
 * <p>
 * https://github.com/RoaringBitmap/RoaringBitmap/blob/master/jmh/src/jmh/java/org/roaringbitmap/BitSetUtilBenchmark.java
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/12/29
 */
public class GzBitsetDataRetriever implements DataRetriever {
    /**
     * gz file postfix.
     */
    private static final String GZ_EXTENSION = ".gz";
    /**
     * dataset name
     */
    private final String name;
    /**
     * dataset path
     */
    private final String path;

    public GzBitsetDataRetriever(String name, String path) {
        Preconditions.checkArgument(path.endsWith(GZ_EXTENSION), "path must ends with %s: %s", GZ_EXTENSION, path);
        this.name = name;
        this.path = path;
    }

    @Override
    public List<int[]> fetchBitPositions() throws IOException {
        return Arrays.stream(deserialize())
            .map(BitSetUtil::bitmapOf)
            .map(bitmap -> bitmap.stream().toArray())
            .collect(Collectors.toList());
    }

    @Override
    public List<RoaringBitmap> fetchBitmaps() throws IOException {
        return Arrays.stream(deserialize())
            .map(BitSetUtil::bitmapOf)
            .collect(Collectors.toList());
    }

    private long[][] deserialize() throws IOException {
        final DataInputStream dos = new DataInputStream(getFileAsStream());
        // the first 4 bytes defines the number of bitsets
        final long[][] bitset = new long[dos.readInt()][];
        for (int i = 0; i < bitset.length; i++) {
            // for each bitset, the first 4 bytes defines the number of words
            final int wordSize = dos.readInt();
            final long[] words = new long[wordSize];
            for (int j = 0; j < wordSize; j++) {
                words[j] = dos.readLong();
            }
            bitset[i] = words;
        }
        dos.close();
        return bitset;
    }

    private File getFile() throws FileNotFoundException {
        File file = new File(path);
        if (!file.exists()) {
            throw new FileNotFoundException("file for dataset '" + name + "' does not exist under the path: " + path);
        }
        return file;
    }

    private GZIPInputStream getFileAsStream() throws IOException {
        return new GZIPInputStream(new FileInputStream(getFile()));
    }
}
