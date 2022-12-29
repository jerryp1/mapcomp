package edu.alibaba.mpc4j.s2pc.aby.sbitmap.utils;

import com.google.common.base.Preconditions;
import org.roaringbitmap.RoaringBitmap;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * This class will retrieve bitmaps that have been previously stored in a portable format (as lists of ints) inside a
 * zip file. The source code is modified from:
 * <p>
 * https://github.com/RoaringBitmap/RoaringBitmap/blob/master/real-roaring-dataset/src/main/java/org/roaringbitmap/
 * ZipRealDataRetriever.java
 * </p>
 *
 * @author Daniel Lemire, Weiran Liu
 * @date 2022/12/29
 */
public class ZipRealDataRetriever implements DataRetriever {
    /**
     * zip file postfix.
     */
    private static final String ZIP_EXTENSION = ".zip";
    /**
     * dataset name
     */
    private final String name;
    /**
     * dataset path
     */
    private final String path;

    public ZipRealDataRetriever(String name, String path) {
        Preconditions.checkArgument(path.endsWith(ZIP_EXTENSION), "path must ends with %s: %s", ZIP_EXTENSION, path);
        this.name = name;
        this.path = path;
    }

    @Override
    public List<int[]> fetchBitPositions() throws IOException {
        List<int[]> bitPositions = new ArrayList<>();
        final ZipInputStream zis = getFileAsStream();
        final BufferedReader buf = new BufferedReader(new InputStreamReader(zis));
        while (true) {
            ZipEntry nextEntry = zis.getNextEntry();
            if (nextEntry == null) {
                break;
            }
            // a single, perhaps very long, line
            String oneLine = buf.readLine();
            bitPositions.add(readLine(oneLine));
        }
        return bitPositions;
    }

    @Override
    public List<RoaringBitmap> fetchBitmaps() throws IOException {
        List<RoaringBitmap> bitmaps = new ArrayList<>();
        final ZipInputStream zis = getFileAsStream();
        final BufferedReader buf = new BufferedReader(new InputStreamReader(zis));
        while (true) {
            ZipEntry nextEntry = zis.getNextEntry();
            if (nextEntry == null) {
                break;
            }
            // a single, perhaps very long, line
            String oneLine = buf.readLine();
            int[] bitPositions = readLine(oneLine);
            bitmaps.add(RoaringBitmap.bitmapOf(bitPositions));
        }
        buf.close();
        zis.close();
        return bitmaps;
    }

    private int[] readLine(String line) {
        String[] positions = line.split(",");
        int[] answer = new int[positions.length];
        for (int i = 0; i < positions.length; i++) {
            answer[i] = Integer.parseInt(positions[i]);
        }
        return answer;
    }

    private File getFile() throws FileNotFoundException {
        File file = new File(path);
        if (!file.exists()) {
            throw new FileNotFoundException("file for dataset '" + name + "' does not exist under the path: " + path);
        }
        return file;
    }

    private ZipInputStream getFileAsStream() throws FileNotFoundException {
        return new ZipInputStream(new FileInputStream(getFile()));
    }
}
