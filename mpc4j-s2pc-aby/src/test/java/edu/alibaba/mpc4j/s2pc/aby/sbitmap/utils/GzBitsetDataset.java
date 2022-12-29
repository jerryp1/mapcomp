package edu.alibaba.mpc4j.s2pc.aby.sbitmap.utils;

import java.util.Objects;

/**
 * bitset dataset used in tests. The source code is also modified from:
 * <p>
 * https://github.com/RoaringBitmap/RoaringBitmap/blob/master/real-roaring-dataset/src/main/java/org/roaringbitmap/RealDataset.java
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/12/29
 */
public class GzBitsetDataset {
    /**
     * private constructor.
     */
    private GzBitsetDataset() {
        // empty
    }

    /**
     * gz file postfix (gz extension)
     */
    private static final String GZ_EXTENSION = ".gz";
    /**
     * the dictionary
     */
    private static final String DICTIONARY = "/sbitmap/";
    /**
     * dataset names
     */
    public static final String BITSETS_1925630_96 = "bitsets_1925630_96";
    /**
     * all dataset names
     */
    public static final String[] ALL = new String[]{
        BITSETS_1925630_96,
    };

    /**
     * Returns the absolute path for the given dataset name.
     *
     * @param name the dataset name.
     * @return the absolute path for the given dataset name.
     */
    public static String getPath(String name) {
        return Objects.requireNonNull(ZipRealDataset.class.getResource(DICTIONARY + name + GZ_EXTENSION)).getPath();
    }
}
