package edu.alibaba.mpc4j.crypto.fhe.utils;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Qixian Zhou
 * @date 2023/8/31
 */
public class DynArray implements Cloneable {

    private int capacity = 0;

    private int size = 0;

    private long[] data;


    public DynArray() {
        data = new long[0];
    }

    public DynArray(int size) {
        data = new long[0];
        resize(size);
    }

    public DynArray(int capacity, int size) {

        if (capacity < size) {
            throw new IllegalArgumentException("capacity cannot be smaller than size");
        }
        data = new long[0];
        //
        reserve(capacity);
        resize(size);
    }

    public DynArray(long[] data, int capacity) {

        this.capacity = capacity;
        this.size = data.length;
        this.data = new long[capacity];

        System.arraycopy(data, 0, this.data, 0, size);
    }

    public DynArray(long[] data) {

        this.capacity = data.length;
        this.size = data.length;
        this.data = new long[capacity];

        System.arraycopy(data, 0, this.data, 0, size);
    }


    public DynArray(long[] data, int capacity, int size, boolean fillZero) {
        if (capacity < size) {
            throw new IllegalArgumentException("capacity cannot be smaller than size");
        }

        this.data = data;
        resize(size, fillZero);
    }

    /**
     * deep-copy a DynArray object
     *
     * @param copy another DynArray object
     */
    public DynArray(DynArray copy) {
        this.capacity = copy.capacity;
        this.size = copy.size;
        this.data = new long[size];
        System.arraycopy(copy.data, 0, this.data, 0, size);
    }


    public long at(int index) {
        return data[index];
    }

    public long set(int index, long value) {
        return data[index] = value;
    }


    public boolean empty() {
        return size == 0;
    }

    /**
     * Reallocates the array so that its capacity exactly matches its size.
     */
    public void shrinkToFit() {
        reserve(size);
    }

    /**
     * Allocates enough memory for storing a given number of elements without
     * changing the size of the array. If the given capacity is smaller than
     * the current size, the size is automatically set to equal the new capacity.
     *
     * @param capacity The capacity of the array
     */
    public void reserve(int capacity) {

        int copySize = Math.min(capacity, size);
        //
        long[] newData = new long[capacity];
        System.arraycopy(data, 0, newData, 0, copySize);
        this.data = newData;

        this.capacity = capacity;
        size = copySize;
    }


    /**
     * Resizes the array to given size. When resizing to larger size the data
     * in the array remains unchanged and any new space is initialized to zero
     * if fill_zero is set to true; when resizing to smaller size the last
     * elements of the array are dropped. If the capacity is not already large
     * enough to hold the new size, the array is also reallocated.
     *
     * @param size     The size of the array
     * @param fillZero If true, fills expanded space with zeros
     */
    public void resize(int size, boolean fillZero) {

        if (size <= capacity) {
            // Are we changing size to bigger within current capacity?
            // If so, need to set top terms to zero
            if (size > this.size && fillZero) {
                // 当前 size 是 this.size , 需要 expand, expand 的部分为 0
                Arrays.fill(data, this.size, size, 0);

                // set size
                this.size = size;
                return;
            }
        }

        // At this point we know for sure that size_ <= capacity_ < size so need
        // to reallocate to bigger
        long[] newData = new long[size];
        // copy original datd
        System.arraycopy(data, 0, newData, 0, size);
        if (fillZero) {
            Arrays.fill(newData, this.size, size, 0);
        }
        this.data = newData;

        capacity = size;
        this.size = size;
    }

    /**
     * fillZero is default ture.
     *
     * @param size size
     */
    public void resize(int size) {

        if (size <= capacity) {
            // Are we changing size to bigger within current capacity?
            // If so, need to set top terms to zero
//            if (size > this.size) {
//                // 当前 size 是 this.size , 需要 expand, expand 的部分为 0
//                // 因为 Java 新建数组时，默认所有值为0, 因此不需要再进一步处理了
//                Arrays.fill(data, this.size, size, 0);
//            }
            // set size
            this.size = size;
            return;
        }

        // At this point we know for sure that size_ <= capacity_ < size so need
        // to reallocate to bigger
        long[] newData = new long[size];
        // copy original datd
        System.arraycopy(data, 0, newData, 0, this.size);
        // fill zero is true
        // todo: 感觉这一步应该是不需要的？因为其余位置默认为 0
        Arrays.fill(newData, this.size, size, 0);

        this.data = newData;

        capacity = size;
        this.size = size;
    }

    public void setData(long[] data) {
        assert data.length == size;
        System.arraycopy(data, 0, this.data, 0, size);
    }

    public void setZero(int startIndex, int length) {

        Arrays.fill(data, startIndex, startIndex + length, 0);
    }

    public void setZero(int startIndex) {
        Arrays.fill(data, startIndex, size, 0);
    }

    public void setZero() {
        Arrays.fill(data, 0, size, 0);
    }


    /**
     * Sets the size of the array to zero. The capacity is not changed.
     */
    public void clear() {
        size = 0;
    }


    public boolean isZero() {
        return Arrays.stream(data).allMatch(n -> n == 0);
    }


    public void release() {
        capacity = 0;
        size = 0;
        data = new long[0];
    }

    public long[] data() {
        return data;
    }

    /**
     *
     * @param startIndex
     * @param endIndex
     * @return data[startIndex, endIndex)
     */
    public long[] data(int startIndex, int endIndex) {
        assert startIndex < endIndex;
        assert endIndex <= size;
        long[] result = new long[endIndex - startIndex + 1];

        System.arraycopy(data, startIndex, result, 0, endIndex - startIndex + 1);
        return result;
    }

    public int capacity() {
        return capacity;
    }

    public int size() {
        return size;
    }

    public int maxSize() {
        return Integer.MAX_VALUE;
    }


    @Override
    public DynArray clone() {
        try {
            DynArray clone = (DynArray) super.clone();
            clone.data = new long[this.data.length];
            System.arraycopy(this.data, 0, clone.data, 0, this.data.length);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }


    @Override
    public String toString() {
        return "DynArray{" +
                "capacity=" + capacity +
                ", size=" + size +
                ", data=" + Arrays.toString(data) +
                '}';
    }
}
