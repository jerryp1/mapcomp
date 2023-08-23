package edu.alibaba.mpc4j.s2pc.pso.psi.rt21;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteMulElligatorEcc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.utils.X25519ByteEccUtils;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;

import java.security.SecureRandom;

public class Rt21ElligatorPsiUtils {

    public final static  Hash hash = HashFactory.createInstance(HashFactory.HashType.JDK_SHA256, X25519ByteEccUtils.POINT_BYTES);


    /**
     * 私有构造函数。
     */
    private Rt21ElligatorPsiUtils() {
        // empty
    }

    public static byte[] generateKaMessage (ByteMulElligatorEcc byteMulElligatorEcc, byte[] input, SecureRandom secureRandom) {
        int pointByteLength = byteMulElligatorEcc.pointByteLength();
        int scalarByteLength = byteMulElligatorEcc.scalarByteLength();
        boolean encodeSuccess = false;
        byte[] randomScalar = new byte[scalarByteLength];
        byte[] point = new byte[pointByteLength];
        byte[] encodedPoint = new byte[pointByteLength];
        while (!encodeSuccess) {
            randomScalar = byteMulElligatorEcc.randomScalar(secureRandom);
            encodeSuccess = byteMulElligatorEcc.baseMul(randomScalar, point, encodedPoint);
        }
        System.arraycopy(randomScalar, 0, input, 0, scalarByteLength);
        return encodedPoint;
    }
    
    public static byte[] generateKaKey (ByteMulElligatorEcc byteMulElligatorEcc, byte[] point, byte[] input) {
        return byteMulElligatorEcc.uniformMul(point, input);
    }
}
