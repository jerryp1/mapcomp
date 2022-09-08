package edu.alibaba.mpc4j.common.tool.crypto.kyber.kyber4j;

import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;

import java.security.SecureRandom;

/**
 * Kyber的密钥类。论文来源：
 * Joppe Bos, Léo Ducas, Eike Kiltz, Tancrède Lepoint, Vadim Lyubashevsky, John M. Schanck, Peter Schwabe,
 * Gregor Seiler, Damien Stehlé.CRYSTALS – Kyber: a CCA-secure module-lattice-based KEM
 * 2018 IEEE European Symposium on Security and Privacy (EuroS&P). IEEE, 2018: 353-367.
 *
 * @author Sheng Hu
 * @date 2022/08/25
 */
public class KyberKeyPairJava {
    /**
     * 公钥
     */
    private final byte[] publicKeyBytes;
    /**
     * 公钥生成元
     */
    private final byte[] publicKeyGenerator;
    /**
     * 私钥
     */
    private final short[][] privateKeyVec;

    /**
     * Default Constructor
     */
    public KyberKeyPairJava(byte[] publicKeyVec, short[][] privateKeyVec, byte[] publicKeyGenerator) {
        this.publicKeyBytes = publicKeyVec;
        this.privateKeyVec = privateKeyVec;
        this.publicKeyGenerator = publicKeyGenerator;
    }

    /**
     * @return the PublicKeyVec
     */
    public byte[] getPublicKeyBytes() {
        return publicKeyBytes;
    }

    /**
     * @return the PublcKeyGenerator
     */
    public byte[] getPublicKeyGenerator() {
        return publicKeyGenerator;
    }


    /**
     * @return the PrivateKeyVec
     */
    public short[][] getPrivateKeyVec() {
        return privateKeyVec;
    }




}
