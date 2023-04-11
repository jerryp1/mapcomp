package edu.alibaba.mpc4j.s2pc.opf.sqoprf.ecdh;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfSenderOutput;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;

/**
 * @author Qixian Zhou
 * @date 2023/4/11
 */
public class EcdhSqOprfSenderOutput implements SqOprfSenderOutput {


    /**
     * 椭圆曲线
     */
    private final Ecc ecc;
    /**
     * 密钥
     */
    private final EcdhSqOprfSenderKey key;
    /**
     * 伪随机函数输出字节长度
     */
    private final int prfByteLength;
    /**
     * 批处理数量
     */
    private final int batchSize;

    public EcdhSqOprfSenderOutput(EnvType envType, EcdhSqOprfSenderKey key, int batchSize) {
        assert batchSize > 0;
        this.batchSize = batchSize;
        ecc = EccFactory.createInstance(envType);
        assert key != null;
        this.key = key;
        prfByteLength = ecc.encode(ecc.getG(), false).length;
    }

    @Override
    public int getPrfByteLength() {
        return prfByteLength;
    }

    @Override
    public int getBatchSize() {
        return batchSize;
    }

    @Override
    public byte[] getPrf(byte[] input) {
        ECPoint output = ecc.hashToCurve(input);
        // 将椭圆曲线点编码为byte[]，压缩编码效率比非压缩编码慢
        return ecc.encode(ecc.multiply(output, key.getAlpha()), false);
    }

    @Override
    public byte[] getPrf(int index, byte[] input) {
        return getPrf(input);
    }


}
