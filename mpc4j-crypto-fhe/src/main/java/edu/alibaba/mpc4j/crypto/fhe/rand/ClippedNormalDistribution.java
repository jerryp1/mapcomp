package edu.alibaba.mpc4j.crypto.fhe.rand;

import org.apache.commons.math3.distribution.NormalDistribution;

import java.security.SecureRandom;

/**
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/clipnormal.h
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/9/20
 */
public class ClippedNormalDistribution {

    // todo: 需要一个表示分布的对象吗？如果需要如何表示？math3 的不支持 SecureRandom, 一定需要 SecureRandom 吗？
//    private SecureRandom normal;

    private double mean;

    private double standardDeviation;

    private double maxDeviation;


    public ClippedNormalDistribution(double mean, double standardDeviation, double maxDeviation) {

        if (standardDeviation < 0) {
            throw new IllegalArgumentException("standardDeviation must be >= 0");
        }

        if (maxDeviation < 0) {
            throw new IllegalArgumentException("maxDeviation must be >= 0");
        }
        this.mean = mean;
        this.standardDeviation = standardDeviation;
        this.maxDeviation = maxDeviation;
    }

    public static double sample(UniformRandomGenerator engine, ClippedNormalDistribution parm) {
        return parm.sample(engine);
    }


    public double sample(UniformRandomGenerator engine) {

        while (true) {
            // 满足特定 均值和方差的 高斯分布的 随机数
            double value = engine.secureRandom.nextGaussian() * standardDeviation + mean;
            double deviation = Math.abs(value - mean);
            if (deviation <= maxDeviation) {
                return value;
            }
        }
    }

}
