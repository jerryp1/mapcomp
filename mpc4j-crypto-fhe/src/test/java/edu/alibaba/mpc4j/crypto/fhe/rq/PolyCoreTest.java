package edu.alibaba.mpc4j.crypto.fhe.rq;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Qixian Zhou
 * @date 2023/8/29
 */
public class PolyCoreTest {


    @Test
    public void setZeroPoly() {

        long[] ptr = PolyCore.allocatePoly(1, 1);
        ptr[0] = 0x1234567812345678L;
        PolyCore.setZeroPoly(1, 1, ptr);
        Assert.assertEquals(0, ptr[0]);

        ptr = PolyCore.allocatePoly(2, 3);
        for (int i = 0; i < 6; i++) {
            ptr[i] = 0x1234567812345678L;
        }

        PolyCore.setZeroPoly(2, 3, ptr);
        for (int i = 0; i < 6; i++) {
            Assert.assertEquals(0, ptr[i]);
        }
    }

    @Test
    public void allocateZeroPoly() {

        long[] ptr = PolyCore.allocateZeroPoly(1, 1);
        Assert.assertEquals(0, ptr[0]);

        ptr = PolyCore.allocateZeroPoly(2, 3);
        for (int i = 0; i < 6; i++) {
            Assert.assertEquals(0, ptr[i]);
        }
    }

    @Test
    public void allocatePolyArray() {

        long[] ptr = PolyCore.allocatePolyArray(1, 1, 1);
        Assert.assertEquals(0, ptr[0]);

        ptr = PolyCore.allocatePolyArray(2, 1, 1);
        Assert.assertEquals(0, ptr[0]);
        Assert.assertEquals(0, ptr[1]);
    }

    @Test
    public void setZeroPolyArray() {

        long[] ptr = PolyCore.allocatePolyArray(1, 1, 1);
        ptr[0] = 0x1234567812345678L;
        PolyCore.setZeroPolyArray(1, 1, 1, ptr);
        Assert.assertEquals(0, ptr[0]);

        ptr = PolyCore.allocatePolyArray(2, 3, 4);
        for (int i = 0; i < 24; i++) {
            ptr[i] = 0x1234567812345678L;
        }

        PolyCore.setZeroPolyArray(2, 3, 4, ptr);
        for (int i = 0; i < 2; i++) {
            Assert.assertEquals(0, ptr[i]);
        }
    }

    @Test
    public void setPoly() {

        long[] ptr1 = PolyCore.allocatePoly(2, 3);
        long[] ptr2 = PolyCore.allocatePoly(2, 3);
        for (int i = 0; i < 6; i++) {
            ptr1[i] = (i + 1L);
        }

        PolyCore.setPoly(ptr1, 2, 3, ptr2);
        for (int i = 0; i < 6; i++) {
            Assert.assertEquals(ptr2[i], i + 1L);
        }

        PolyCore.setPoly(ptr1, 2, 3, ptr1);
        for (int i = 0; i < 6; i++) {
            Assert.assertEquals(ptr2[i], i + 1L);
        }
    }

    @Test
    public void setPolyArray() {

        long[] ptr1 = PolyCore.allocatePolyArray(1, 2, 3);
        long[] ptr2 = PolyCore.allocatePolyArray(1, 2, 3);
        for (int i = 0; i < 6; i++) {
            ptr1[i] = (i + 1L);
        }

        PolyCore.setPolyArray(ptr1, 1, 2, 3, ptr2);
        for (int i = 0; i < 6; i++) {
            Assert.assertEquals(ptr2[i], i + 1L);
        }
    }

}
