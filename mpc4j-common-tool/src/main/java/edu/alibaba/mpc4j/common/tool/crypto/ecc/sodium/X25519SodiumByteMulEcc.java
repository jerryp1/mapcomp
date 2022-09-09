package edu.alibaba.mpc4j.common.tool.crypto.ecc.sodium;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteMulEcc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.utils.X25519ByteEccUtils;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.security.SecureRandom;

/**
 * Sodium实现的X25519乘法字节椭圆曲线。
 *
 * @author Weiran Liu
 * @date 2022/9/6
 */
public class X25519SodiumByteMulEcc implements ByteMulEcc {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_TOOL_NAME);
    }

    /**
     * 哈希函数
     */
    private final Hash hash;

    public X25519SodiumByteMulEcc() {
        hash = HashFactory.createInstance(HashFactory.HashType.JDK_SHA256, X25519ByteEccUtils.POINT_BYTES);
        X25519ByteEccUtils.precomputeBase();
    }

    @Override
    public byte[] randomScalar(SecureRandom secureRandom) {
        return X25519ByteEccUtils.randomClampScalar(secureRandom);
    }

    @Override
    public boolean isValidPoint(byte[] p) {
        return X25519ByteEccUtils.checkPoint(p);
    }

    @Override
    public byte[] getInfinity() {
        return BytesUtils.clone(X25519ByteEccUtils.POINT_INFINITY);
    }

    @Override
    public byte[] getG() {
        return BytesUtils.clone(X25519ByteEccUtils.POINT_B);
    }

    @Override
    public byte[] randomPoint(SecureRandom secureRandom) {
        return X25519ByteEccUtils.randomPoint(secureRandom);
    }

    @Override
    public byte[] hashToCurve(byte[] message) {
        return hash.digestToBytes(message);
    }

    @Override
    public byte[] mul(byte[] p, byte[] k) {
        assert X25519ByteEccUtils.checkPoint(p);
        assert X25519ByteEccUtils.checkClampScalar(k);
        return nativeMul(p, k);
    }

    private native byte[] nativeMul(byte[] p, byte[] k);

    @Override
    public byte[] baseMul(byte[] k) {
        assert X25519ByteEccUtils.checkClampScalar(k);
        return nativeBaseMul(k);
    }

    private native byte[] nativeBaseMul(byte[] k);

    @Override
    public ByteEccFactory.ByteEccType getByteEccType() {
        return ByteEccFactory.ByteEccType.X25519_SODIUM;
    }
}
