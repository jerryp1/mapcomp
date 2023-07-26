package edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * 朴素无贮存区布谷鸟哈希，其主要思路将放缩倍数ε设置为1.5，使得贮存区为空的概率达到要求的程度。
 * <p>
 * Angel、Chen、Laine、Setty率先在PIR中使用了此参数，论文来源：
 * <p>
 * Angel, Sebastian, Hao Chen, Kim Laine, and Srinath Setty. PIR with compressed queries and amortized query processing.
 * SP 2018, pp. 962-979. IEEE, 2018.
 * <p>
 * 由于此参数相对固定，因此后续多个论文都使用了这个方案实现无贮存区布谷鸟哈希。
 *
 * @author Weiran Liu
 * @date 2022/01/11
 */
class NaiveNoStashCuckooHashBin<T> extends AbstractNoStashCuckooHashBin<T> {
    /**
     * 朴素整数布谷鸟哈希ε取值
     */
    private static final double EPSILON = 1.5;
    /**
     * max special (small) item size
     */
    private static final int MAX_SPECIAL_ITEM_SIZE = 128;

    /**
     * Gets ε.
     *
     * @param maxItemSize number of items.
     * @return ε.
     */
    private static double getEpsilon(int maxItemSize) {
        MathPreconditions.checkPositiveInRangeClosed("maxItemSize", maxItemSize, CuckooHashBinFactory.MAX_ITEM_SIZE_UPPER_BOUND);
        if (maxItemSize == 1) {
            // although we can set binNum = 1 when n = 1, in some cases we must require BinNum > 1
            return 2.0;
        } else if (maxItemSize < 4) {
            // 2^1 <= n < 2^2
            return 15.60;
        } else if (maxItemSize < 8) {
            // 2^2 <= n < 2^3
            return 10.00;
        } else if (maxItemSize < 16) {
            // 2^3 <= n < 2^4
            return 6.00;
        } else if (maxItemSize < 32) {
            // 2^4 <= n < 2^5
            return 4.00;
        } else if (maxItemSize < 64) {
            // 2^5 <= n < 2^6
            return 3.00;
        } else if (maxItemSize <= MAX_SPECIAL_ITEM_SIZE) {
            // 2^6 <= n <= 2^7
            return 2.00;
        } else {
            return EPSILON;
        }
    }

    /**
     * 返回布谷鸟哈希的桶数量。
     *
     * @param maxItemSize 预期插入的元素数量。
     * @return 桶数量。
     */
    static int getBinNum(int maxItemSize) {
        return (int) Math.ceil(maxItemSize * getEpsilon(maxItemSize));
    }

    /**
     * 返回朴素布谷鸟哈希的插入的元素数量。
     *
     * @param binNum 桶数量。
     * @return 插入的元素数量。
     */
    static int getMaxItemSize(int binNum) {
        // here we do not consider special cases
        MathPreconditions.checkGreater("binNum", binNum, (int) Math.floor(getBinNum(MAX_SPECIAL_ITEM_SIZE) / EPSILON));
        return (int) Math.floor(binNum / EPSILON);
    }

    NaiveNoStashCuckooHashBin(EnvType envType, int maxItemSize, byte[][] keys) {
        super(envType, CuckooHashBinFactory.CuckooHashBinType.NO_STASH_NAIVE, maxItemSize, keys);
    }

    NaiveNoStashCuckooHashBin(EnvType envType, int maxItemSize, int binNum, byte[][] keys) {
        super(envType, CuckooHashBinFactory.CuckooHashBinType.NO_STASH_NAIVE, maxItemSize, binNum, keys);
    }
}
