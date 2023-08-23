package edu.alibaba.mpc4j.s2pc.sbitmap.utils;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.Bitmap;
import edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.MutablePlainBitmap;
import edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.RoaringPlainBitmap;
import edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.container.Container;
import edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.container.PlainContainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Bitmap Utilities.
 *
 * @author Li Peng
 * @date 2023/8/14
 */
public class BitmapUtils {

    /**
     * Random flip the empty containers with dp-bounded probability.
     *
     * @param bitmap roaringBitmap.
     */
    public static MutablePlainBitmap toMutablePlainBitmap(RoaringPlainBitmap bitmap, double epsilon) {
        return toDpMutablePlainBitmap(bitmap, RoaringPlainBitmap.CONTAINER_SIZE, epsilon);
    }

    /**
     * Random flip the empty containers with dp-bounded probability.
     *
     * @param bitmap roaringBitmap.
     */
    public static MutablePlainBitmap toDpMutablePlainBitmap(RoaringPlainBitmap bitmap, int containerSize, double epsilon) {
        RoaringPlainBitmap roaringBitmap = bitmap.toFull();
        MutablePlainBitmap mutablePlainBitmap = roaringBitmap.resizeContainer(containerSize);
        PlainContainer[] oldContainers = Arrays.stream(mutablePlainBitmap.getContainers()).map(v -> (PlainContainer) v).toArray(PlainContainer[]::new);
        int[] oldKeys = mutablePlainBitmap.getKeys();

        List<Container> newContainers = new ArrayList<>();
        List<Integer> newKeys = new ArrayList<>();
        for (int i = 0; i < mutablePlainBitmap.getContainerNum(); i++) {
            if (oldContainers[i].bitCount() == 0 && DpUtils.sample(epsilon)) {
                continue;
            }
            newContainers.add(oldContainers[i]);
            newKeys.add(oldKeys[i]);
        }
        return MutablePlainBitmap.create(mutablePlainBitmap.totalBitNum(), newKeys.stream().mapToInt(i -> i).toArray(), newContainers.toArray(new Container[0]));
    }


    /**
     * Resize container of oldBitmap to new container size.
     *
     * @param oldBitmap        old bitmap.
     * @param newContainerSize new container size.
     * @return resized container.
     */
    public static MutablePlainBitmap resize(MutablePlainBitmap oldBitmap, int newContainerSize) {
        if (oldBitmap.getContainerSize() == newContainerSize) {
            return oldBitmap.clone();
        }
        BitVector[] oldVectors = Arrays.stream(oldBitmap.getContainers()).map(Container::getBitVector).toArray(BitVector[]::new);
        int oldContainerSize = oldBitmap.getContainerSize();
        int[] oldKeys = oldBitmap.getKeys();
        List<Integer> newKeysList = new ArrayList<>();

        int newIndex = 0;
        List<List<BitVector>> newVectors = new ArrayList<>();
        List<BitVector> newVector = new ArrayList<>();
        for (int i = 0; i < oldKeys.length; i++) {
            int oldIndex = oldKeys[i];

            int oldStart = oldKeys[i] * oldContainerSize;
            int oldEnd = (oldKeys[i] + 1) * oldContainerSize;

            int newStart = newIndex * newContainerSize;
            int newEnd = (newIndex + 1) * newContainerSize;

            // 情况6
            while (newEnd < oldStart) {
                newVector = new ArrayList<>();
                newIndex++;
                newStart = newIndex * newContainerSize;
                newEnd = (newIndex + 1) * newContainerSize;
            }
            if (oldEnd <= newEnd) {

                if (newStart <= oldEnd) {
                    // 情况2
                    newVector.add(oldVectors[oldIndex].copy().split(oldEnd - newStart));
                } else {
                    //情况1
                    continue;
                }
            }
            if (newEnd <= oldEnd) {
                if (newStart < oldStart) {
                    // 情况5
                    newVector.add(oldVectors[oldIndex].copy().split(newEnd - oldStart));
                } else {
                    // 情况4
                    BitVector temp = oldVectors[oldIndex].copy();
                    temp.split(newStart - oldStart);
                    temp = temp.split(newContainerSize);
                    newVector.add(temp);
                }
            } else {
                // 情况3
                newVector.add(oldVectors[oldIndex].copy());
            }
            if (newEnd <= oldEnd) {
                // 情况4/5
                newVectors.add(newVector);
                newKeysList.add(newIndex);
                newVector = new ArrayList<>();
                newIndex++;
            }
        }
        // merge
        BitVector[] newContainers = newVectors.stream()
            .map(list -> list.stream()
                .reduce(BitVectorFactory.createEmpty(), (a, b) -> {
                    a.merge(b);
                    return a;
                }))
            .toArray(BitVector[]::new);
        int[] newKeys = newKeysList.stream().mapToInt(i -> i).toArray();
        return MutablePlainBitmap.create(oldBitmap.totalBitNum(), newKeys, newContainers);
    }

    /**
     * Check the equality of container sizes of input bitmaps
     *
     * @param x input x.
     * @param y input y.
     */
    public static void checkContainerSize(Bitmap x, Bitmap y) {
        MathPreconditions.checkEqual("x.containerSize", "y.containerSize", x.getContainerSize(), y.getContainerSize());
    }

}
