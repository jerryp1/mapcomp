package edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo;

import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;

import java.util.ArrayList;

/**
 * 无暂存区布谷鸟哈希。
 *
 * @author Weiran Liu
 * @date 2021/04/10
 */
public interface NoStashCuckooHashBin<T> extends CuckooHashBin<T> {

    @Override
    default int stashSize() {
        return 0;
    }

    @Override
    default ArrayList<HashBinEntry<T>> getStash() {
        return new ArrayList<>(0);
    }

    @Override
    default int itemNumInBins() {
        return itemSize() + paddingItemSize();
    }

    @Override
    default int itemNumInStash() {
        return 0;
    }
}
