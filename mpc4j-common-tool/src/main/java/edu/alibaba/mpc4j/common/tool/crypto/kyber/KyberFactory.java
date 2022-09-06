package edu.alibaba.mpc4j.common.tool.crypto.kyber;

import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.kyber4j.KyberCpa;

import java.security.SecureRandom;

/**
 * Kyber工厂类。
 *
 * @author Sheng Hu
 * @date 2022/09/01
 */
public class KyberFactory {
    /**
     * 私有构造函数。
     */
    private KyberFactory() {
        // empty
    }

    /**
     * Kyber方案枚举类
     */
    public enum KyberType {
        /**
         * Kyber的cpa实现
         */
        KYBER_CPA,
        /**
         * Kyber的cca实现
         */
        KYBER_CCA,
    }

    /**
     * 创建Kyber类型
     */
    public static Kyber createInstance(KyberType kyberType, int paramsK, SecureRandom secureRandom, Hash hashFunction) {
        switch (kyberType) {
            case KYBER_CPA:
                return new KyberCpa(paramsK,secureRandom,hashFunction);
            case KYBER_CCA:

            default:
                throw new IllegalArgumentException("Invalid " + KyberFactory.KyberType.class.getSimpleName() + ": " + kyberType.name());
        }
    }
}
