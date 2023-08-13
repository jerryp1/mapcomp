package edu.alibaba.mpc4j.crypto.fhe.utils;

import edu.alibaba.mpc4j.common.tool.EnvType;
import org.junit.Test;

import java.math.BigInteger;

/**
 * @author Qixian Zhou
 * @date 2023/7/12
 */
public class RingLweZp64Test {




    @Test
    public void testNegativeNumber() {

        RingLweZp64 zp = new RingLweZp64(EnvType.STANDARD, 19);
        assert zp.add(-1, -2) == 16;
        assert zp.mul(-1, -2) == 2;
        assert zp.sub(-1, -2) == 1;
        assert zp.modExp(-1, 100) == 1;
        assert zp.modInv(-1) == zp.modInv(18);
    }


    @Test
    public void testModInv() {

        RingLweZp64 zp = new RingLweZp64(EnvType.STANDARD, 19);
        assert zp.modInv(7) == 11;
        zp = new RingLweZp64(EnvType.STANDARD, 103);
        assert zp.modInv(43) == 12;
        zp = new RingLweZp64(EnvType.STANDARD, 97);
        assert zp.modInv(94) == 32;
    }


    @Test
    public void testRootOfUnity() {

        RingLweZp64 zp = new RingLweZp64(EnvType.STANDARD, 5);
        assert zp.rootOfUnity(2) == 4;
        zp = new RingLweZp64(EnvType.STANDARD, 7);
        assert zp.rootOfUnity(3) == 2;
        zp = new RingLweZp64(EnvType.STANDARD, 11);
        assert zp.rootOfUnity(5) == 4;
    }

    @Test
    public void testFindSmallestGenerator() {

        RingLweZp64 zp = new RingLweZp64(EnvType.STANDARD, 5);
        assert zp.findSmallestGenerator() == 2;
        zp = new RingLweZp64(EnvType.STANDARD, 7);
        assert zp.findSmallestGenerator() == 3;
        zp = new RingLweZp64(EnvType.STANDARD, 11);
        assert zp.findSmallestGenerator() == 2;
        zp = new RingLweZp64(EnvType.STANDARD, 262147);
        assert zp.findSmallestGenerator() == 2;
        zp = new RingLweZp64(EnvType.STANDARD, 262151);
        assert zp.findSmallestGenerator() == 13;
        zp = new RingLweZp64(EnvType.STANDARD, 262153);
        assert zp.findSmallestGenerator() == 10;
        zp = new RingLweZp64(EnvType.STANDARD, 262187);
        assert zp.findSmallestGenerator() == 2;
        zp = new RingLweZp64(EnvType.STANDARD, 262193);
        assert zp.findSmallestGenerator() == 3;
        zp = new RingLweZp64(EnvType.STANDARD, 262217);
        assert zp.findSmallestGenerator() == 3;
        zp = new RingLweZp64(EnvType.STANDARD, 262231);
        assert zp.findSmallestGenerator() == 12;
        zp = new RingLweZp64(EnvType.STANDARD, 262237);
        assert zp.findSmallestGenerator() == 7;

    }
}
