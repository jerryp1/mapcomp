package edu.alibaba.mpc4j.s2pc.sbitmap.utils;

import java.util.Objects;

/**
 * real dataset used in tests. The source code is modified from:
 * <p>
 * https://github.com/RoaringBitmap/RoaringBitmap/blob/master/real-roaring-dataset/src/main/java/org/roaringbitmap/RealDataset.java
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/12/29
 */
public class ZipRealDataset {
    /**
     * private constructor.
     */
    private ZipRealDataset() {
        // empty
    }

    /**
     * zip file postfix (zip extension)
     */
    private static final String ZIP_EXTENSION = ".zip";
    /**
     * the dictionary
     */
    private static final String DICTIONARY = "/sbitmap/";
    /**
     * dataset names
     */
    public static final String CENSUS_INCOME = "census-income";
    public static final String CENSUS1881 = "census1881";
    public static final String DIMENSION_008 = "dimension_008";
    public static final String DIMENSION_003 = "dimension_003";
    public static final String DIMENSION_033 = "dimension_033";
    public static final String USCENSUS2000 = "uscensus2000";
    public static final String WEATHER_SEPT_85 = "weather_sept_85";
    public static final String WIKILEAKS_NOQUOTES = "wikileaks-noquotes";
    public static final String CENSUS_INCOME_SRT = "census-income_srt";
    public static final String CENSUS1881_SRT = "census1881_srt";
    public static final String WEATHER_SEPT_85_SRT = "weather_sept_85_srt";
    public static final String WIKILEAKS_NOQUOTES_SRT = "wikileaks-noquotes_srt";
    /**
     * all dataset names
     */
    public static final String[] ALL = new String[]{
        CENSUS_INCOME,
        CENSUS1881,
        DIMENSION_008,
        DIMENSION_003,
        DIMENSION_033,
        USCENSUS2000,
        WEATHER_SEPT_85,
        WIKILEAKS_NOQUOTES,
        CENSUS_INCOME_SRT,
        CENSUS1881_SRT,
        WEATHER_SEPT_85_SRT,
        WIKILEAKS_NOQUOTES_SRT,
    };

    /**
     * Returns the absolute path for the given dataset name.
     *
     * @param name the dataset name.
     * @return the absolute path for the given dataset name.
     */
    public static String getPath(String name) {
        return Objects.requireNonNull(ZipRealDataset.class.getResource(DICTIONARY + name + ZIP_EXTENSION)).getPath();
    }
}
