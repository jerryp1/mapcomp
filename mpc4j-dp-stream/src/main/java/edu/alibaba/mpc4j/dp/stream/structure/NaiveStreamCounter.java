package edu.alibaba.mpc4j.dp.stream.structure;

import java.util.HashMap;
import java.util.Map;

/**
 * Naive stream counter.
 *
 * @author Weiran Liu
 * @date 2022/11/16
 */
public class NaiveStreamCounter<T> implements StreamCounter<T> {
    /**
     * counter
     */
    private final Map<T, Integer> countMap;
    /**
     * the total number of insert items
     */
    private int insertNum;

    public NaiveStreamCounter() {
        countMap = new HashMap<>();
        insertNum = 0;
    }

    @Override
    public boolean insert(T item) {
        insertNum++;
        if (countMap.containsKey(item)) {
            int count = countMap.get(item);
            count++;
            countMap.put(item, count);
        } else {
            countMap.put(item, 1);
        }
        return true;
    }

    @Override
    public int query(T item) {
        if (countMap.containsKey(item)) {
            return countMap.get(item);
        }
        return 0;
    }

    @Override
    public int getInsertNum() {
        return insertNum;
    }
}
