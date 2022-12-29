package edu.alibaba.mpc4j.s2pc.aby.sbitmap.utils;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;
import org.roaringbitmap.RoaringBitmap;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

/**
 * tests for ZipRealDataRetriever.
 *
 * @author Weiran Liu
 * @date 2022/12/29
 */
public class DataRetrieverTest {
    /**
     * expected universe size
     */
    private static final Map<String, Integer> EXPECTED_UNIVERSE_SIZES = ImmutableMap.<String, Integer>builder()
            .put(ZipRealDataset.CENSUS_INCOME, 199522)
            .put(ZipRealDataset.CENSUS1881, 4277805)
            .put(ZipRealDataset.DIMENSION_008, 3866844)
            .put(ZipRealDataset.DIMENSION_003, 3866846)
            .put(ZipRealDataset.DIMENSION_033, 3866846)
            .put(ZipRealDataset.USCENSUS2000, 36974577)
            .put(ZipRealDataset.WEATHER_SEPT_85, 1015366)
            .put(ZipRealDataset.WIKILEAKS_NOQUOTES, 1353178)
            .put(ZipRealDataset.CENSUS_INCOME_SRT, 199522)
            .put(ZipRealDataset.CENSUS1881_SRT, 4277734)
            .put(ZipRealDataset.WEATHER_SEPT_85_SRT, 1015366)
            .put(ZipRealDataset.WIKILEAKS_NOQUOTES_SRT, 1353132)
        .put(GzBitsetDataset.BITSETS_1925630_96, 95)
            .build();

    /**
     * expected total cardinality
     */
    private static final Map<String, Long> EXPECTED_TOTAL_CARDINALITIES = ImmutableMap.<String, Long>builder()
        .put(ZipRealDataset.CENSUS_INCOME, 6922021L)
        .put(ZipRealDataset.CENSUS1881, 1003861L)
        .put(ZipRealDataset.DIMENSION_008, 2833779L)
        .put(ZipRealDataset.DIMENSION_003, 3866847L)
        .put(ZipRealDataset.DIMENSION_033, 3866847L)
        .put(ZipRealDataset.USCENSUS2000, 5985L)
        .put(ZipRealDataset.WEATHER_SEPT_85, 12870627L)
        .put(ZipRealDataset.WIKILEAKS_NOQUOTES, 275355L)
        .put(ZipRealDataset.CENSUS_INCOME_SRT, 6092864L)
        .put(ZipRealDataset.CENSUS1881_SRT, 680793L)
        .put(ZipRealDataset.WEATHER_SEPT_85_SRT, 16108094L)
        .put(ZipRealDataset.WIKILEAKS_NOQUOTES_SRT, 288013L)
        .put(GzBitsetDataset.BITSETS_1925630_96, 4458278L)
        .build();

    @Test
    public void testTotalCardinality() throws IOException {
        for (String name : ZipRealDataset.ALL) {
            ZipRealDataRetriever zip = new ZipRealDataRetriever(name, ZipRealDataset.getPath(name));
            long totalCardinality = totalCardinality(zip);
            Assert.assertEquals(EXPECTED_TOTAL_CARDINALITIES.get(name).longValue(), totalCardinality);
        }
        for (String name : GzBitsetDataset.ALL) {
            GzBitsetDataRetriever gz = new GzBitsetDataRetriever(name, GzBitsetDataset.getPath(name));
            long totalCardinality = totalCardinality(gz);
            Assert.assertEquals(EXPECTED_TOTAL_CARDINALITIES.get(name).longValue(), totalCardinality);
        }
    }

    private long totalCardinality(DataRetriever dataRetriever) throws IOException {
        long totalCardinality = 0;
        for(int[] data : dataRetriever.fetchBitPositions()) {
            RoaringBitmap r = RoaringBitmap.bitmapOf(data);
            totalCardinality += r.getCardinality();
        }
        return totalCardinality;
    }

    @Test
    public void testUniverseSize() throws IOException {
        for (String name : ZipRealDataset.ALL) {
            ZipRealDataRetriever zip = new ZipRealDataRetriever(name, ZipRealDataset.getPath(name));
            int universeSize = universeSize(zip);
            Assert.assertEquals(EXPECTED_UNIVERSE_SIZES.get(name).intValue(), universeSize);
        }
        for (String name : GzBitsetDataset.ALL) {
            GzBitsetDataRetriever gz = new GzBitsetDataRetriever(name, GzBitsetDataset.getPath(name));
            int universeSize = universeSize(gz);
            Assert.assertEquals(EXPECTED_UNIVERSE_SIZES.get(name).intValue(), universeSize);
        }
    }

    /**
     * Returns the universe size of the int array.
     *
     * @param dataRetriever the ZipRealDataRetriever.
     * @return the universe size of the int array.
     * @throws IOException errors when reading the dataset.
     */
    private int universeSize(DataRetriever dataRetriever) throws IOException {
        int answer = 0;
        for (int[] data : dataRetriever.fetchBitPositions()) {
            // sort data
            Arrays.sort(data);
            // the maximal value in all last int[] is the universe size.
            if (data[data.length - 1] > answer) {
                answer = data[data.length - 1];
            }
        }
        return answer;
    }
}
