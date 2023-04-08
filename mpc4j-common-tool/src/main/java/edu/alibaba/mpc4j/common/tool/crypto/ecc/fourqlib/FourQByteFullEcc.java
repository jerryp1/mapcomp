package edu.alibaba.mpc4j.common.tool.crypto.ecc.fourqlib;


import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteFullEcc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.utils.FourQByteEccUtils;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.math.BigInteger;
import java.security.SecureRandom;
/**
 * FourQlib实现的FourQ全功能字节椭圆曲线。
 * @author Qixian Zhou
 * @date 2023/4/6
 */
public class FourQByteFullEcc implements ByteFullEcc {


    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_TOOL_NAME);
    }

    /**
     * 哈希函数
     */
    private final Hash hash;

    public FourQByteFullEcc() {
        hash = HashFactory.createInstance(HashFactory.HashType.JDK_SHA256, FourQByteEccUtils.POINT_BYTES);
    }

    @Override
    public BigInteger getN() {
        return FourQByteEccUtils.N;
    }

    @Override
    public BigInteger randomZn(SecureRandom secureRandom) {
        return BigIntegerUtils.randomPositive(FourQByteEccUtils.N, secureRandom);
    }

    @Override
    public byte[] add(byte[] p, byte[] q) {

        assert p.length == FourQByteEccUtils.POINT_BYTES && q.length == FourQByteEccUtils.POINT_BYTES;
        assert isValidPoint(p) && isValidPoint(q);

        return nativeAdd(p, q);
    }

    @Override
    public void addi(byte[] p, byte[] q) {
        assert p.length == FourQByteEccUtils.POINT_BYTES && q.length == FourQByteEccUtils.POINT_BYTES;
        assert isValidPoint(p) && isValidPoint(q);

        byte[] r = nativeAdd(p, q);
        // reset p
        System.arraycopy(r, 0, p, 0, FourQByteEccUtils.POINT_BYTES);
    }

    @Override
    public byte[] neg(byte[] p) {
        assert p.length == FourQByteEccUtils.POINT_BYTES;
        assert isValidPoint(p);

        return nativeNeg(p);

    }

    @Override
    public void negi(byte[] p) {
        assert p.length == FourQByteEccUtils.POINT_BYTES;
        assert isValidPoint(p);

        byte[] r = nativeNeg(p);
        // reset p
        System.arraycopy(r, 0, p, 0, FourQByteEccUtils.POINT_BYTES);
    }

    @Override
    public byte[] sub(byte[] p, byte[] q) {
        assert p.length == FourQByteEccUtils.POINT_BYTES && q.length == FourQByteEccUtils.POINT_BYTES;
        assert isValidPoint(p) && isValidPoint(q);
        // p + (-q)
        byte[] q_neg = nativeNeg(q);
        return nativeAdd(p, q_neg);
    }

    @Override
    public void subi(byte[] p, byte[] q) {

        assert p.length == FourQByteEccUtils.POINT_BYTES && q.length == FourQByteEccUtils.POINT_BYTES;
        assert isValidPoint(p) && isValidPoint(q);
        // p + (-q)
        byte[] q_neg = nativeNeg(q);

        byte[] r = nativeAdd(p, q_neg);
        // reset p
        System.arraycopy(r, 0, p, 0, FourQByteEccUtils.POINT_BYTES);
    }


    @Override
    public byte[] mul(byte[] p, BigInteger k) {

        assert p.length == FourQByteEccUtils.POINT_BYTES;
        assert isValidPoint(p);

        byte[] byteK = FourQByteEccUtils.toByteK(k);
        return nativeMul(p, byteK);
    }

    @Override
    public byte[] baseMul(BigInteger k) {
        byte[] byteK = FourQByteEccUtils.toByteK(k);
        return nativeBaseMul(byteK);
    }

    @Override
    public byte[] randomScalar(SecureRandom secureRandom) {

        BigInteger zn = randomZn(secureRandom);
        byte[] k = BigIntegerUtils.nonNegBigIntegerToByteArray(zn, FourQByteEccUtils.SCALAR_BYTES);
        BytesUtils.innerReverseByteArray(k);
        return k;
    }

    @Override
    public boolean isValidPoint(byte[] p) {

        return nativeIsValidPoint(p);
    }

    @Override
    public byte[] getInfinity() {
        // 注意一定要 deep copy
        return BytesUtils.clone(FourQByteEccUtils.POINT_INFINITY);
    }

    @Override
    public byte[] getG() {
        // must be deep copy
        return BytesUtils.clone(FourQByteEccUtils.POINT_B);
    }

    /**
     * 通过生成随机字节数组的方式来生成 随机点，无法稳定的通过test
     * 改成随机生成 scalar * BasePoint 来生成随机点
     * @param secureRandom 随机状态。
     * @return
     */
    @Override
    public byte[] randomPoint(SecureRandom secureRandom) {

        byte[] r = randomScalar(secureRandom);
        return nativeBaseMul(r);
    }

    @Override
    public byte[] hashToCurve(byte[] message) {
        assert message.length > 0;
        // hash
        byte[] hashed = hash.digestToBytes(message);
        return nativeHashToCurve(hashed);
    }

    @Override
    public byte[] mul(byte[] p, byte[] k) {

        assert p.length == FourQByteEccUtils.POINT_BYTES;
        assert isValidPoint(p);
        assert k.length == FourQByteEccUtils.SCALAR_BYTES;

        return nativeMul(p, k);
    }

    @Override
    public byte[] baseMul(byte[] k) {
        assert k.length == FourQByteEccUtils.SCALAR_BYTES;
        return nativeBaseMul(k);
    }

    @Override
    public ByteEccFactory.ByteEccType getByteEccType() {
        return ByteEccFactory.ByteEccType.FOUR_Q;
    }

    @Override
    public int pointByteLength() {
        return FourQByteEccUtils.POINT_BYTES;
    }

    @Override
    public int scalarByteLength() {
        return FourQByteEccUtils.SCALAR_BYTES;
    }


    private native byte[] nativeMul(byte[] p, byte[] k);

    private native byte[] nativeBaseMul(byte[] k);

    private native boolean nativeIsValidPoint(byte[] p);

    private native byte[] nativeNeg(byte[] p);

    private native byte[] nativeAdd(byte[] p, byte[] q);

    private native byte[] nativeHashToCurve(byte[] message);

}
