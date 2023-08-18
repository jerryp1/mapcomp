package edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.polynomial.gf2e.Gf2ePoly;
import edu.alibaba.mpc4j.common.tool.polynomial.gf2e.Gf2ePolyFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * 基于多项式插值的不经意键值对存储器（OKVS）。
 * 与其他OKVS相比，多项式插值OKVS的特点在于，键值（key）和映射值（value）都必须是相同长度的字节数组。
 *
 * @author Weiran Liu
 * @date 2021/09/13
 */
public class PolynomialGf2eDokvs <T> implements Gf2eDokvs<T> {
    public static int getM(EnvType envType, int n) {
        // 等于多项式插值的系数数量
        return Gf2ePolyFactory.getCoefficientNum(envType, n);
    }
    /**
     * number of hash keys
     */
    static int HASH_KEY_NUM = 1;
    /**
     * 多项式插值服务
     */
    private final Gf2ePoly gf2ePoly;
    /**
     * 插值数量
     */
    private final int n;
    /**
     * 插值对的比特长度，要求是Byte.SIZE的整数倍
     */
    private final int l;
    /**
     * the key / value byte length
     */
    private final int byteL;
    /**
     * hm: {0, 1}^* -> {0, 1}^l.
     */
    private final Prf hm;

    PolynomialGf2eDokvs(EnvType envType, int n, int l, byte[] key) {
        MathPreconditions.checkPositive("n", n);
        // 多项式OKVS要求至少编码2个元素
        assert n > 1;
        this.n = n;
        // 要求l > 统计安全常数，且l可以被Byte.SIZE整除
        MathPreconditions.checkGreaterOrEqual("l", l, CommonConstants.STATS_BIT_LENGTH);
        MathPreconditions.checkPositive("l", l);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        // set polynomial service
        gf2ePoly = Gf2ePolyFactory.createInstance(envType, l);
        // set mapping hash
        hm = PrfFactory.createInstance(envType, byteL);
        hm.setKey(key);
    }

    @Override
    public byte[][] encode(Map<T, byte[]> keyValueMap, boolean doublyEncode) throws ArithmeticException {
        assert keyValueMap.size() <= n;
        byte[][] xArray = keyValueMap.keySet().stream().map(key -> {
            byte[] mapKey = hm.getBytes(ObjectUtils.objectToByteArray(key));
            BytesUtils.reduceByteArray(mapKey, l);
            return mapKey;
        }).toArray(byte[][]::new);
        byte[][] yArray = keyValueMap.keySet().stream().map(keyValueMap::get).toArray(byte[][]::new);
        // 给定的键值对数量可能小于n，此时要用dummy interpolate将插入的点数量补充到n
        return gf2ePoly.interpolate(n, xArray, yArray);
    }

    @Override
    public byte[] decode(byte[][] storage, T key) {
        // here we do not verify bit length for each storage, otherwise decode would require O(n) computation.
        assert storage.length == getM();
        byte[] hashKey = hm.getBytes(ObjectUtils.objectToByteArray(key));
        BytesUtils.reduceByteArray(hashKey, l);
        return gf2ePoly.evaluate(storage, hashKey);
    }

    @Override
    public int getN() {
        return n;
    }

    @Override
    public int getL() {
        return l;
    }

    @Override
    public int getM() {
        // 等于多项式插值的系数数量
        return gf2ePoly.coefficientNum(n);
    }

    @Override
    public Gf2eDokvsFactory.Gf2eDokvsType getType() {
        return Gf2eDokvsType.POLYNOMIAL;
    }

    @Override
    public void setParallelEncode(boolean parallelEncode) {
        // 难以进行并行化的编码
    }

    @Override
    public boolean getParallelEncode() {
        return false;
    }
}
