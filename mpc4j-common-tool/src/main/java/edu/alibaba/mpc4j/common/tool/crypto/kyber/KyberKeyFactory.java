package edu.alibaba.mpc4j.common.tool.crypto.kyber;

import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;

import java.security.SecureRandom;

/**
 * Kyber-Key工厂类。
 *
 * @author Sheng Hu
 * @date 2022/09/06
 */
public class KyberKeyFactory {
    /**
     * 私有构造函数。
     */
    private KyberKeyFactory() {
    }

    /**
     * KyberKey方案枚举类
     */
    public enum KyberKeyType {
        /**
         * Kyber的java实现
         */
        KYBER_KEY_JAVA,
    }

    /**
     * 创建Kyber类型
     */
    public static KyberKey createInstance(KyberKeyType kyberType, int paramsK, SecureRandom secureRandom, Hash hashFunction, Prg prgNoiseLength,Prg prgMatrixLength672) {
        switch (kyberType) {
            case KYBER_KEY_JAVA:
                return new KyberKeyPairJava(paramsK,secureRandom,hashFunction,prgNoiseLength,prgMatrixLength672);
            default:
                throw new IllegalArgumentException("Invalid " + KyberKeyFactory.KyberKeyType.class.getSimpleName() + ": " + kyberType.name());
        }
    }
}
