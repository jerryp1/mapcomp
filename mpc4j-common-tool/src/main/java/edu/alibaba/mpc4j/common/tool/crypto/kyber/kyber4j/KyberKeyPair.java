package edu.alibaba.mpc4j.common.tool.crypto.kyber.kyber4j;

/**
 * Kyber的密钥类。论文来源：
 * Joppe Bos, Léo Ducas, Eike Kiltz, Tancrède Lepoint, Vadim Lyubashevsky, John M. Schanck, Peter Schwabe,
 * Gregor Seiler, Damien Stehlé.CRYSTALS – Kyber: a CCA-secure module-lattice-based KEM
 * 2018 IEEE European Symposium on Security and Privacy (EuroS&P). IEEE, 2018: 353-367.
 *
 * @author Sheng Hu
 * @date 2022/08/25
 */
public class KyberKeyPair {
    /**
     * 公钥
     */
    private final byte[] publicKey;
    /**
     * 公钥生成元
     */
    private final byte[] publicKeyGenerator;
    /**
     * 私钥
     */
    private final short[][] privateKey;

    /**
     * Default Constructor
     */
    public KyberKeyPair(byte[] publicKeyVec, short[][] privateKey, byte[] publicKeyGenerator) {
        this.publicKey = publicKeyVec;
        this.privateKey = privateKey;
        this.publicKeyGenerator = publicKeyGenerator;
    }

    /**
     * @return the PublicKeyVec
     */
    public byte[] getPublicKey() {
        return publicKey;
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
    public short[][] getPrivateKey() {
        return privateKey;
    }




}
