package edu.alibaba.mpc4j.common.kyber.provider;

/**
 * Kyber未封装的，以向量形式存储的密钥。论文来源：
 * Joppe Bos, Léo Ducas, Eike Kiltz, Tancrède Lepoint, Vadim Lyubashevsky, John M. Schanck, Peter Schwabe,
 * Gregor Seiler, Damien Stehlé.CRYSTALS – Kyber: a CCA-secure module-lattice-based KEM
 * 2018 IEEE European Symposium on Security and Privacy (EuroS&P). IEEE, 2018: 353-367.
 * @author Sheng Hu
 * @date 2022/08/25
 */
public class KyberVecPki {
    private short[][] publicKeyVec;
    private byte[] publicKeyGenerator;
    private short[][] privateKeyVec;

    /**
     *  Default Constructor
     */
    public KyberVecPki() {

    }
    /**
     * @return the PublicKeyVec
     */
    public short[][] getPublicKeyVec() {return publicKeyVec;}
    /**
     * @param publicKeyVec the PublicKeyVec to set
     */
    public void setPublicKeyVec(short[][] publicKeyVec) {this.publicKeyVec = publicKeyVec;}

    /**
     * @return the PublcKeyGenerator
     */
    public byte[] getPublicKeyGenerator() {return publicKeyGenerator;}
    /**
     * @param publicKeyGenerator the publicKeyGenerator to set
     */
    public void setPublicKeyGenerator(byte[] publicKeyGenerator) {this.publicKeyGenerator = publicKeyGenerator;}

    /**
     * @return the PrivateKeyVec
     */
    public short[][] getPrivateKeyVec() {return privateKeyVec;}
    /**
     * @param privateKeyVec the PublicKeyVec to set
     */
    public void setPrivateKeyVec(short[][] privateKeyVec) {this.privateKeyVec = privateKeyVec;}


}
