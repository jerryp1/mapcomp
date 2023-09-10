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

        long[][] ptr = PolyCore.allocatePoly(1, 1);
        ptr[0][0] = 0x1234567812345678L;
        PolyCore.setZeroPoly(1, 1, ptr);
        Assert.assertEquals(0, ptr[0][0]);

        ptr = PolyCore.allocatePoly(2, 3);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 2; j++) {
                ptr[i][j] = 0x1234567812345678L;
            }
        }

        PolyCore.setZeroPoly( 2, 3, ptr);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 2; j++) {
               Assert.assertEquals(0, ptr[i][j]);
            }
        }
    }

    @Test
    public void allocateZeroPoly() {

        long[][] ptr = PolyCore.allocateZeroPoly(1, 1);
        Assert.assertEquals(0, ptr[0][0]);

        ptr = PolyCore.allocateZeroPoly(2, 3);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 2; j++) {
                Assert.assertEquals(0, ptr[i][j]);
            }
        }
    }

    @Test
    public void allocatePolyArray() {

        long[][][] ptr = PolyCore.allocatePolyArray(1, 1, 1);
        Assert.assertEquals(0, ptr[0][0][0]);

        ptr = PolyCore.allocatePolyArray(2, 1, 1);
        Assert.assertEquals(0, ptr[0][0][0]);
        Assert.assertEquals(0, ptr[1][0][0]);
    }

    @Test
    public void setZeroPolyArray() {

        long[][][] ptr = PolyCore.allocatePolyArray(1, 1, 1);
        ptr[0][0][0] = 0x1234567812345678L;
        PolyCore.setZeroPolyArray(1, 1, 1, ptr);
        Assert.assertEquals(0, ptr[0][0][0]);

        ptr = PolyCore.allocatePolyArray(2, 3, 4);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 3; k++) {
                    ptr[i][j][k] = 2;
                }
            }
        }

        PolyCore.setZeroPolyArray(2, 3, 4, ptr);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 3; k++) {
                    Assert.assertEquals(0, ptr[i][j][k]);
                }
            }
        }
    }

    @Test
    public void setPoly() {

        long[][] ptr1 = PolyCore.allocatePoly(2, 3);
        long[][] ptr2 = PolyCore.allocatePoly(2, 3);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 2; j++) {
                ptr1[i][j]  = (i + 1L);
            }
        }

        PolyCore.setPoly(ptr1, 2, 3, ptr2);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 2; j++) {
                Assert.assertEquals(ptr2[i][j], i + 1L);
            }
        }

        PolyCore.setPoly(ptr1, 2, 3, ptr1);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 2; j++) {
                Assert.assertEquals(ptr2[i][j], i + 1L);
            }
        }
    }

    @Test
    public void setPolyArray() {

        long[][][] ptr1 = PolyCore.allocatePolyArray(1, 2, 3);
        long[][][] ptr2 = PolyCore.allocatePolyArray(1, 2, 3);
        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 2; k++) {
                    ptr1[i][j][k]  = (i + 1L);
                }
            }
        }

        PolyCore.setPolyArray(ptr1, 1, 2, 3, ptr2);
        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 2; k++) {
                    Assert.assertEquals(ptr2[i][j][k], i + 1L);
                }
            }
        }
    }



}
