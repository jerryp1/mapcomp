package edu.alibaba.mpc4j.s2pc.sbitmap.bitmap;

import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.SecureBitmapFactory.SecureBitmapType;
import edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.container.Container;
import edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.container.PlainContainer;
import edu.alibaba.mpc4j.s2pc.sbitmap.utils.BitmapUtils;
import edu.alibaba.mpc4j.s2pc.sbitmap.utils.RoaringBitmapUtils;
import org.roaringbitmap.RoaringBitmap;

import java.util.Arrays;

/**
 * Mutable bitmap as a intermediate structure for transfering plain bitmap to secure bitmap.
 *
 * @author Li Peng
 * @date 2023/8/14
 */
public class MutablePlainBitmap implements PlainBitmap, Cloneable {
    /**
     * total number of bits.
     */
    private int totalBitNum;

    /**
     * container size.
     */
    private int containerSize;
    /**
     * keys
     */
    private int[] keys;

    private PlainContainer[] containers;

    /**
     * private constructor
     */
    private MutablePlainBitmap() {
        // empty
    }

    public static MutablePlainBitmap create(int totalBitNum, int[] keys, Container[] containers) {
        return create(totalBitNum, keys, Arrays.stream(containers).map(Container::getBitVector).toArray(BitVector[]::new));
    }

    public static MutablePlainBitmap create(int totalBitNum, int[] keys, BitVector[] bitVectors) {
        MutablePlainBitmap bitmap = new MutablePlainBitmap();
        bitmap.totalBitNum = totalBitNum;
        bitmap.containerSize = bitVectors[0].bitNum();
        bitmap.keys = keys;
        bitmap.containers = Arrays.stream(bitVectors).map(PlainContainer::create).toArray(PlainContainer[]::new);
        return bitmap;
    }

    public static MutablePlainBitmap createEmpty(int totalBitNum, int containerSize) {
        MutablePlainBitmap bitmap = new MutablePlainBitmap();
        bitmap.totalBitNum = totalBitNum;
        bitmap.containerSize = containerSize;
        bitmap.keys = new int[0];
        bitmap.containers = new PlainContainer[0];
        return bitmap;
    }

    /**
     * add element in the index.
     *
     * @param index index.
     */
    public void add(int index) {
        final int hb = highBits(index);
        final int i = getIndex(hb);
        if (i >= 0) {
            setContainerAtIndex(i, addToContainer(getContainerAtIndex(i), lowBits(index)));
        } else {
            final PlainContainer newContainer = PlainContainer.create(BitVectorFactory.createZeros(containerSize));
            insertNewKeyValueAt(-i - 1, hb, addToContainer(newContainer, lowBits(index)));
        }
    }

    private int highBits(int x) {
        return x >>> containerSize;
    }

    /**
     * The method is suggested from
     * https://stackoverflow.com/questions/12766590/get-n-least-significant-bits-from-an-int.
     *
     * @param x x
     * @return return.
     */
    private int lowBits(int x) {
        return x & ((1 << containerSize) - 1);
    }

    private int getIndex(int x) {
        return binarySearch(0, keys.length, x);
    }

    private int binarySearch(int begin, int end, int key) {
        return hybridUnsignedBinarySearch(keys, begin, end, key);
    }

    // starts with binary search and finishes with a sequential search
    private static int hybridUnsignedBinarySearch(final int[] array, final int begin,
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

    void setContainerAtIndex(int i, PlainContainer c) {
        this.containers[i] = c;
    }

    protected PlainContainer getContainerAtIndex(int i) {
        return this.containers[i];
    }

    private PlainContainer addToContainer(PlainContainer container, int x) {
        PlainContainer containerCopy = (PlainContainer) container.copy();
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
    void insertNewKeyValueAt(int i, int key, PlainContainer value) {
        extendArray(1);
        System.arraycopy(keys, i, keys, i + 1, keys.length - i);
        keys[i] = key;
        System.arraycopy(containers, i, containers, i + 1, keys.length - i);
        containers[i] = value;
    }

    @Override
    public MutablePlainBitmap clone() {
        try {
            final MutablePlainBitmap x = (MutablePlainBitmap) super.clone();
            x.keys = keys.clone();
            x.containers = containers.clone();
            x.containerSize = containerSize;
            return x;
        } catch (final CloneNotSupportedException e) {
            throw new RuntimeException("shouldn't happen with clone", e);
        }
    }

    public void append(int key, PlainContainer container) {
        if (keys.length > 0 && key < keys[keys.length - 1]) {
            throw new IllegalArgumentException("append only: "
                + (key) + " < " + (keys[keys.length - 1]));
        }
        extendArray(1);
        keys[keys.length - 1] = key;
        containers[keys.length - 1] = container;
    }

    @Override
    public int getContainerSize() {
        return containerSize;
    }

    @Override
    public PlainBitmap andi(PlainBitmap other) {
        throw new UnsupportedOperationException("Inner AND is not supported in intermediate bitmap");
    }

    @Override
    public PlainBitmap ori(PlainBitmap other) {
        throw new UnsupportedOperationException("Inner OR is not supported in intermediate bitmap");
    }

    @Override
    public int bitCount() {
        throw new UnsupportedOperationException("Bitcount is not supported in intermediate bitmap");
    }

    @Override
    public SecureBitmapType getType() {
        return null;
    }

    @Override
    public int totalBitNum() {
        return totalBitNum;
    }


    @Override
    public boolean isPlain() {
        return true;
    }

    @Override
    public boolean isFull() {
        return false;
    }

    @Override
    public Container[] getContainers() {
        return containers;
    }

    @Override
    public int[] getKeys() {
        return keys;
    }

    @Override
    public MutablePlainBitmap resizeContainer(int containerSize) {
        // resize
        return BitmapUtils.resize(this, containerSize);
    }

    @Override
    public boolean isIntermediate() {
        return true;
    }

    public RoaringPlainBitmap toRoaringPlainBitmap() {
        // resize
        MutablePlainBitmap mutablePlainBitmap = BitmapUtils.resize(this, RoaringPlainBitmap.CONTAINER_SIZE);
        int[] newKeys = mutablePlainBitmap.getKeys();
        // to roaring
        RoaringBitmap roaringBitmap = RoaringBitmapUtils.toRoaringBitmap(newKeys, Arrays.stream(mutablePlainBitmap.getContainers())
            .map(Container::getBitVector).toArray(BitVector[]::new));
        return RoaringPlainBitmap.fromBitmap(totalBitNum, roaringBitmap);
    }
}
