package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * 字节椭圆曲线，数据表示标准与国际标准兼容。
 *
 * @author Weiran Liu
 * @date 2022/9/1
 */
public interface ByteEcc {
    /**
     * 返回椭圆曲线的阶。
     *
     * @return 椭圆曲线的阶。
     */
    BigInteger getN();

    /**
     * 返回椭圆曲线点字节长度。
     *
     * @return 椭圆曲线点字节长度。
     */
    int pointByteLength();

    /**
     * 返回1个随机幂指数。
     *
     * @param secureRandom 随机状态。
     * @return 随机幂指数。
     */
    default BigInteger randomZn(SecureRandom secureRandom) {
        return BigIntegerUtils.randomPositive(getN(), secureRandom);
    }

    /**
     * 验证给定点是否为合法椭圆曲线点。
     *
     * @param p 椭圆曲线点。
     * @return 如果合法，返回{@code true}，否则返回{@code false}。
     */
    boolean isValid(byte[] p);

    /**
     * 返回椭圆曲线无穷远点。
     *
     * @return 无穷远点。
     */
    byte[] getInfinity();

    /**
     * 返回椭圆曲线的生成元。
     *
     * @return 椭圆曲线的生成元。
     */
    byte[] getG();

    /**
     * 返回1个随机的椭圆曲线点。
     *
     * @param secureRandom 随机状态。
     * @return 随机椭圆曲线点。
     */
    byte[] randomPoint(SecureRandom secureRandom);

    /**
     * 将{@code byte[]}表示的数据映射到椭圆曲线上。
     *
     * @param message 数据。
     * @return 数据的椭圆曲线哈希结果。
     */
    byte[] hashToCurve(byte[] message);

    /**
     * 计算R = P + Q。
     *
     * @param p 椭圆曲线点P。
     * @param q 椭圆曲线点Q.
     * @return 结果R。
     */
     byte[] add(byte[] p, byte[] q);

    /**
     * 计算P = P + Q。
     *
     * @param p 椭圆曲线点P。
     * @param q 椭圆曲线点Q。
     */
     void addi(byte[] p, byte[] q);

    /**
     * 计算R = -1 · P
     * @param p 椭圆曲线点P。
     * @return 结果R。
     */
     byte[] neg(byte[] p);

    /**
     * 计算P = -1 · P
     * @param p 椭圆曲线点P。
     */
    void negi(byte[] p);

    /**
     * 计算R = P - Q。
     *
     * @param p 椭圆曲线点P。
     * @param q 椭圆曲线点Q。
     * @return 结果R。
     */
     byte[] sub(byte[] p, byte[] q);

    /**
     * 计算P = P - Q。
     *
     * @param p 椭圆曲线点P。
     * @param q 椭圆曲线点Q。
     */
     void subi(byte[] p, byte[] q);

    /**
     * 计算R = k · P。
     *
     * @param p 椭圆曲线点P。
     * @param k 幂指数k。
     * @return 结果R。
     */
    byte[] mul(byte[] p, BigInteger k);

    /**
     * 计算R = k · G。
     *
     * @param k 幂指数k。
     * @return 结果R。
     */
    byte[] baseMul(BigInteger k);

    /**
     * 返回字节椭圆曲线类型。
     *
     * @return 椭圆曲线类型。
     */
    ByteEccFactory.ByteEccType getByteEccType();
}
