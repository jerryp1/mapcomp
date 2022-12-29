package edu.alibaba.mpc4j.s2pc.aby.sbitmap.utils;

import org.roaringbitmap.RoaringBitmap;

import java.io.IOException;
import java.util.List;

/**
 * data retriever, retrieving data to RoaringBitmap.
 *
 * @author Weiran Liu
 * @date 2022/12/29
 */
public interface DataRetriever {
    /**
     * Fetches data to BitPositions.
     *
     * @return an {@link Iterable} of int[], as read from the resource
     * @throws IOException something went wrong while reading the data.
     */
    List<int[]> fetchBitPositions() throws IOException;

    /**
     * Fetches data to Bitmap.
     *
     * @return the fetched Bitmap.
     * @throws IOException something went wrong while reading the data.
     */
    List<RoaringBitmap> fetchBitmaps() throws IOException;
}
