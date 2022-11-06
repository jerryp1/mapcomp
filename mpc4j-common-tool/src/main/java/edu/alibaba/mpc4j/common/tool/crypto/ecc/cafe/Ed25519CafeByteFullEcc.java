package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteFullEcc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.utils.Ed25519ByteEccUtils;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Cafe实现的Ed25519全功能字节椭圆曲线。
 *
 * @author Weiran Liu
 * @date 2022/11/6
 */
public class Ed25519CafeByteFullEcc implements ByteFullEcc {
    /**
     * 椭圆曲线点字节长度
     */
    private static final int POINT_BYTE_LENGTH = Ed25519ByteEccUtils.POINT_BYTES;
    /**
     * 哈希函数
     */
    private final Hash hash;

    public Ed25519CafeByteFullEcc() {
        hash = HashFactory.createInstance(HashFactory.HashType.JDK_SHA256, POINT_BYTE_LENGTH);
        Ed25519ByteEccUtils.precomputeBase();
    }

    @Override
    public BigInteger getN() {
        return Ed25519ByteEccUtils.N;
    }

    @Override
    public BigInteger randomZn(SecureRandom secureRandom) {
        return BigIntegerUtils.randomPositive(Ed25519ByteEccUtils.N, secureRandom);
    }

    @Override
    public byte[] randomScalar(SecureRandom secureRandom) {
        BigInteger zn = randomZn(secureRandom);
        byte[] k = BigIntegerUtils.nonNegBigIntegerToByteArray(zn, Ed25519ByteEccUtils.SCALAR_BYTES);
        BytesUtils.innerReverseByteArray(k);
        return k;
    }

    @Override
    public byte[] randomPoint(SecureRandom secureRandom) {
        byte[] p = new byte[POINT_BYTE_LENGTH];
        boolean success = false;
        while (!success) {
            secureRandom.nextBytes(p);
            p[Ed25519ByteEccUtils.POINT_BYTES - 1] &= 0x7F;
            success = Ed25519ByteEccUtils.validPoint(p);
        }
        // 需要乘以cofactor
        byte[] r = new byte[POINT_BYTE_LENGTH];
        Ed25519ByteEccUtils.scalarMultEncoded(Ed25519ByteEccUtils.SCALAR_COFACTOR, p, r);
        return r;
    }

    @Override
    public byte[] hashToCurve(byte[] message) {
        // 简单的重复哈希
        byte[] p = hash.digestToBytes(message);
        p[Ed25519ByteEccUtils.POINT_BYTES - 1] &= 0x7F;
        boolean success = false;
        while (!success) {
            success = Ed25519ByteEccUtils.validPoint(p);
            if (!success) {
                p = hash.digestToBytes(p);
                p[Ed25519ByteEccUtils.POINT_BYTES - 1] &= 0x7F;
            }
        }
        byte[] r = new byte[POINT_BYTE_LENGTH];
        Ed25519ByteEccUtils.scalarMultEncoded(Ed25519ByteEccUtils.SCALAR_COFACTOR, p, r);
        return r;
    }

    @Override
    public byte[] add(byte[] p, byte[] q) {
        try {
            EdwardsPoint pFieldElement = new CompressedEdwardsY(p).decompress();
            EdwardsPoint qFieldElement = new CompressedEdwardsY(q).decompress();
            return pFieldElement.add(qFieldElement).compress().toByteArray();
        } catch (InvalidEncodingException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Invalid point p pr q");
        }
    }

    @Override
    public void addi(byte[] p, byte[] q) {
        try {
            EdwardsPoint pFieldElement = new CompressedEdwardsY(p).decompress();
            EdwardsPoint qFieldElement = new CompressedEdwardsY(q).decompress();
            byte[] r = pFieldElement.add(qFieldElement).compress().toByteArray();
            System.arraycopy(r, 0, p, 0, p.length);
        } catch (InvalidEncodingException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Invalid point p pr q");
        }
    }

    @Override
    public byte[] neg(byte[] p) {
        try {
            EdwardsPoint pFieldElement = new CompressedEdwardsY(p).decompress();
            return pFieldElement.negate().compress().toByteArray();
        } catch (InvalidEncodingException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Invalid point p");
        }
    }

    @Override
    public void negi(byte[] p) {
        try {
            EdwardsPoint pFieldElement = new CompressedEdwardsY(p).decompress();
            byte[] r = pFieldElement.negate().compress().toByteArray();
            System.arraycopy(r, 0, p, 0, p.length);
        } catch (InvalidEncodingException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Invalid point p");
        }
    }

    @Override
    public byte[] sub(byte[] p, byte[] q) {
        try {
            EdwardsPoint pFieldElement = new CompressedEdwardsY(p).decompress();
            EdwardsPoint qFieldElement = new CompressedEdwardsY(q).decompress();
            return pFieldElement.subtract(qFieldElement).compress().toByteArray();
        } catch (InvalidEncodingException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Invalid point p pr q");
        }
    }

    @Override
    public void subi(byte[] p, byte[] q) {
        try {
            EdwardsPoint pFieldElement = new CompressedEdwardsY(p).decompress();
            EdwardsPoint qFieldElement = new CompressedEdwardsY(q).decompress();
            byte[] r = pFieldElement.subtract(qFieldElement).compress().toByteArray();
            System.arraycopy(r, 0, p, 0, p.length);
        } catch (InvalidEncodingException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Invalid point p pr q");
        }
    }

    @Override
    public byte[] mul(byte[] p, BigInteger k) {
        assert p.length == POINT_BYTE_LENGTH;
        byte[] byteK = Ed25519ByteEccUtils.toByteK(k);
        try {
            Scalar scalarK = new Scalar(byteK);
            EdwardsPoint pFieldElement = new CompressedEdwardsY(p).decompress();
            return pFieldElement.multiply(scalarK).compress().toByteArray();
        } catch (InvalidEncodingException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Invalid point p");
        }
    }

    @Override
    public byte[] baseMul(BigInteger k) {
        byte[] byteK = Ed25519ByteEccUtils.toByteK(k);
        Scalar scalarK = new Scalar(byteK);
        return Constants.ED25519_BASEPOINT_TABLE.multiply(scalarK).compress().toByteArray();
    }

    @Override
    public boolean isValidPoint(byte[] p) {
        return Ed25519ByteEccUtils.validPoint(p);
    }

    @Override
    public byte[] getInfinity() {
        return BytesUtils.clone(Ed25519ByteEccUtils.POINT_INFINITY);
    }

    @Override
    public byte[] getG() {
        return Constants.ED25519_BASEPOINT.compress().toByteArray();
    }

    @Override
    public byte[] mul(byte[] p, byte[] k) {
        assert p.length == POINT_BYTE_LENGTH;
        try {
            Scalar scalarK = new Scalar(k);
            EdwardsPoint pFieldElement = new CompressedEdwardsY(p).decompress();
            return pFieldElement.multiply(scalarK).compress().toByteArray();
        } catch (InvalidEncodingException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Invalid point p");
        }
    }

    @Override
    public byte[] baseMul(byte[] k) {
        Scalar scalarK = new Scalar(k);
        return Constants.ED25519_BASEPOINT_TABLE.multiply(scalarK).compress().toByteArray();
    }

    @Override
    public ByteEccFactory.ByteEccType getByteEccType() {
        return ByteEccFactory.ByteEccType.ED25519_CAFE;
    }
}
