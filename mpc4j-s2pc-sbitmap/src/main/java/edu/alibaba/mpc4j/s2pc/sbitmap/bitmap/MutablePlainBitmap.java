package edu.alibaba.mpc4j.s2pc.sbitmap.bitmap;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.SecureBitmapFactory.SecureBitmapType;
import edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.container.Container;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Window bitmap as a intermediate structure for transfering from plain bitmap to secure bitmap.
 * TODO 没有设置长度上限等参数
 *
 * @author Li Peng
 * @date 2023/8/14
 */
public class MutablePlainBitmap implements PlainBitmap, Cloneable {
    /**
     * total number of bits.
     */
    protected int totalBitNum;
    /**
     * total number of containers.
     */
    protected int totalContainerNum;
    /**
     * total number of bytes.
     */
    protected int totalByteNum;

    /**
     * window size.
     */
    int w;
    /**
     * keys
     */
    int[] keys;
    /**
     * bit vectors.
     */
    BitVector[] containers;

    Container[] container;

    public MutablePlainBitmap(int totalBitNum, int w) {
        this.totalBitNum = totalBitNum;
        this.w = w;
        this.keys = null;
        this.containers = null;
    }

    /**
     * 在index处添加元素
     * @param index
     */
    public void add(int index) {
        final int hb = highBits(index);
        final int i = getIndex(hb);
        if (i >= 0) {
            setContainerAtIndex(i, addToContainer(getContainerAtIndex(i), lowBits(index)));
        } else {
            final BitVector newContainer = BitVectorFactory.createZeros(w) ;
            insertNewKeyValueAt(-i - 1, hb, addToContainer(newContainer, lowBits(index)));
        }
    }

    private int highBits(int x) {
        return x >>> w;
    }

    /**
     * The method is suggested from
     * https://stackoverflow.com/questions/12766590/get-n-least-significant-bits-from-an-int.
     * @param x x
     * @return return.
     */
    private int lowBits(int x) {
        return x & ((1 << w) - 1);
    }

    private int getIndex(int x) {
        return binarySearch(0, keys.length, x);
    }

    private int binarySearch(int begin, int end, int key) {
        return hybridUnsignedBinarySearch(keys, begin, end, key);
    }

    // starts with binary search and finishes with a sequential search
    protected static int hybridUnsignedBinarySearch(final int[] array, final int begin,
                                                    final int end, final int k) {
        // next line accelerates the possibly common case where the value would
        // be inserted at the end
        if ((end > 0) && ((array[end - 1]) < (int) k)) {
            return -end - 1;
        }
        int low = begin;
        int high = end - 1;
        // 32 in the next line matches the size of a cache line
        while (low + 32 <= high) {
            final int middleIndex = (low + high) >>> 1;
            final int middleValue = (array[middleIndex]);

            if (middleValue < (int) k) {
                low = middleIndex + 1;
            } else if (middleValue > (int) k) {
                high = middleIndex - 1;
            } else {
                return middleIndex;
            }
        }
        // we finish the job with a sequential search
        int x = low;
        for (; x <= high; ++x) {
            final int val = (array[x]);
            if (val >= (int) k) {
                if (val == (int) k) {
                    return x;
                }
                break;
            }
        }
        return -(x + 1);
    }

    void setContainerAtIndex(int i, BitVector c) {
        this.containers[i] = c;
    }

    protected BitVector getContainerAtIndex(int i) {
        return this.containers[i];
    }

    private BitVector addToContainer(BitVector container, int x) {
        BitVector containerCopy = container.copy();
        containerCopy.set(x, true);
        return containerCopy;
    }

    // make sure there is capacity for at least k more elements
    void extendArray(int k) {
        int size = keys.length;
        // size + 1 could overflow
        if (size + k > keys.length) {
            int newCapacity;
            if (keys.length < 1024) {
                newCapacity = 2 * (size + k);
            } else {
                newCapacity = 5 * (size + k) / 4;
            }
            keys = Arrays.copyOf(keys, newCapacity);
            containers = Arrays.copyOf(containers, newCapacity);
        }
    }

    // insert a new key, it is assumed that it does not exist
    void insertNewKeyValueAt(int i, int key, BitVector value) {
        extendArray(1);
        System.arraycopy(keys, i, keys, i + 1, keys.length - i);
        keys[i] = key;
        System.arraycopy(containers, i, containers, i + 1, keys.length - i);
        containers[i] = value;
    }

    public Iterator<BitVector> getContainerIterator() {
        return new Iterator<BitVector>() {
            int i = 0;
            @Override
            public boolean hasNext() {
                return i < containers.length;
            }

            @Override
            public BitVector next() {
                return containers[i++];
            }
        };
    }

    @Override
    public MutablePlainBitmap clone() {
        try {
            final MutablePlainBitmap x = (MutablePlainBitmap) super.clone();
            x.keys = keys.clone();
            x.containers = containers.clone();
            x.w = w;
            return x;
        } catch (final CloneNotSupportedException e) {
            throw new RuntimeException("shouldn't happen with clone", e);
        }
    }

    public void append(int key, BitVector container) {
        if (keys.length > 0 && key < keys[keys.length - 1]) {
            throw new IllegalArgumentException("append only: "
                + (key) + " < " + (keys[keys.length - 1]));
        }
        extendArray(1);
        keys[keys.length - 1] = key;
        containers[keys.length - 1] = container;
    }

    public int getW() {
        return w;
    }

    @Override
    public PlainBitmap and(PlainBitmap other) throws MpcAbortException {
        // TODO
        throw new UnsupportedOperationException("And is not supported in WindowPlainBitmap");
    }

    @Override
    public PlainBitmap or(PlainBitmap other) throws MpcAbortException {
        // TODO
        throw new UnsupportedOperationException("Or is not supported in WindowPlainBitmap");
    }

    @Override
    public int bitCount() throws MpcAbortException {
        // TODO
        throw new UnsupportedOperationException("BitCount is not supported in WindowPlainBitmap");
    }

    @Override
    public SecureBitmapType getType() {
        return null;
    }

    @Override
    public int totalBitNum() {
        return 0;
    }

    @Override
    public int totalByteNum() {
        return 0;
    }

    @Override
    public boolean isPlain() {
        return false;
    }

    @Override
    public boolean isFull() {
        return false;
    }

    @Override
    public int getContainerSize() {
        return w;
    }

    @Override
    public BitVector[] getContainers() {
        return containers;
    }

    @Override
    public Container[] getContainer() {
        return container;
    }

    @Override
    public int[] getKeys() {
        return keys;
    }

    @Override
    public MutablePlainBitmap resizeBlock(int blockSize) {
        // TODO 非常重要
        return null;
    }
}
