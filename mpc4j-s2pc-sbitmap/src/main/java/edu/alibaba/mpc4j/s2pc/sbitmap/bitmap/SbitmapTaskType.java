package edu.alibaba.mpc4j.s2pc.sbitmap.bitmap;


/**
 * Sbitmap task type.
 *
 * @author Weiran Liu
 * @date 2022/5/5
 */
public enum SbitmapTaskType {
    /**
     * Set operations(e.g. AND, OR).
     */
    SET_OPERATIONS,
    /**
     * Group aggregation.
     */
    GROUP_AGGREGATIONS,
}
