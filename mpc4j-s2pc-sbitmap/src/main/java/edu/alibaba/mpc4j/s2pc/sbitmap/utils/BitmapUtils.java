package edu.alibaba.mpc4j.s2pc.sbitmap.utils;

import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.RoaringPlainBitmap;
import edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.MutablePlainBitmap;
import org.roaringbitmap.RoaringBitmap;

import java.util.Arrays;
import java.util.Iterator;

/**
 * @author Li Peng
 * @date 2023/8/14
 */
public class BitmapUtils {

    /**
     * Random flip the empty containers with dp-bounded probability.
     *
     * @param bitmap roaringBitmap.
     */
    public static MutablePlainBitmap toWindowPlainBitmap(RoaringPlainBitmap bitmap, double epsilon) {
        return toWindowPlainBitmap(bitmap, 1 << 16, epsilon);
    }

    /**
     * Random flip the empty containers with dp-bounded probability.
     *
     * @param bitmap roaringBitmap.
     */
    public static MutablePlainBitmap toWindowPlainBitmap(RoaringPlainBitmap bitmap, int w, double epsilon) {
        // 添加元素
        RoaringBitmap roaringBitmap = bitmap.getBitmap();
        MutablePlainBitmap mutablePlainBitmap = new MutablePlainBitmap(bitmap.totalBitNum(), w);
        for (Integer integer : roaringBitmap) {
            mutablePlainBitmap.add(integer);
        }
        // 随机填充剩余节点
        mutablePlainBitmap = toDpRandomWindowsPlainBitmap(mutablePlainBitmap, epsilon);

        return mutablePlainBitmap;
    }

    /**
     * Random flip the empty containers with dp-bounded probability.
     *
     * @param mutablePlainBitmap windowPlainBitmap.
     */
    public static MutablePlainBitmap toDpRandomWindowsPlainBitmap(MutablePlainBitmap mutablePlainBitmap, double epsilon) {
        MutablePlainBitmap roaringBitmapClone = mutablePlainBitmap.clone();

        Iterator<BitVector> iterator = roaringBitmapClone.getContainerIterator();
        int containerIndex = 0;
        while(iterator.hasNext()) {
            if (DpUtils.sample(epsilon)) {
                // append when dp.
                roaringBitmapClone.append(containerIndex, BitVectorFactory.createZeros(mutablePlainBitmap.getW()));
            }
            containerIndex += 1;
        }
        return roaringBitmapClone;
    }


    public static RoaringBitmap repairToRoaring(int totalBitNum, int[] keys, BitVector[] bitVectors) {
        assert keys.length == bitVectors.length : "Length of keys and bitVectors not match";
        int newNum = CommonUtils.getUnitNum(totalBitNum, 1 << 16);
        assert newNum != 0;
        int oldBlockSize = bitVectors[0].bitNum();
        int newBlockSize = 1 << 16;
        char[] newKeys;
        BitVector[] newBitVectors;
        RoaringBitmap roaringBitmap = new RoaringBitmap();
        byte[][] oldBytes = Arrays.stream(bitVectors).map(BitVector::getBytes).toArray(byte[][]::new);
        byte[][] newBytes = new byte[newNum][];
//        for (int i = 0; i < keys.length;i++) {
//            int start = i * oldBlockSize;
//            int end = (i + 1) * oldBlockSize - 1;
//
//        }
        // TODO 这里要尤其注意大小端序问题 以及边界条件
        if (newBlockSize <= oldBlockSize) {
            int lastI = 0;
            int newI = 0;
            while (newI < newNum) {
                int newStart = newI * (1 << 16);
                int newEnd = (newI + 1) * (1 << 16) - 1;
                int oldStart = lastI * oldBlockSize;
                int oldEnd = (lastI + 1) * (1 << 16) - 1;
                if (newStart > oldEnd) {
                    lastI++;
                    continue;
                }
                if ((newStart <= oldEnd) && (newStart > oldStart) && (newEnd > oldEnd)) {
                    int dif = oldEnd - newStart;
                }
            }
        }
        roaringBitmap.stream();
        return null;
    }

}
