package edu.alibaba.mpc4j.dp.stream.structure;

import java.util.Set;

/**
 * Streaming-data counter interface.
 *
 * @author Weiran Liu
 * @date 2022/11/16
 */
public interface StreamCounter<T> {
    /**
     * Insert an item.
     *
     * @param item the item.
     * @return return true if the item is not ignored and successfully inserted.
     */
    @SuppressWarnings("UnusedReturnValue")
    boolean insert(T item);

    /**
     * Query an item.
     *
     * @param item the item.
     * @return the query count, or 0 if no item matches.
     */
    int query(T item);

    /**
     * Return the total insert item num.
     *
     * @return the total insert item num.
     */
    int getInsertNum();

    /**
     * Return the item set.
     *
     * @return the item set.
     */
    Set<T> getItemSet();
}
