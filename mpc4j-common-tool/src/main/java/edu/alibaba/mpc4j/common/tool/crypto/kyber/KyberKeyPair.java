package edu.alibaba.mpc4j.common.tool.crypto.kyber;

import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.kyber4j.Indcpa;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.kyber4j.KyberParams;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.kyber4j.Poly;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;

import java.security.SecureRandom;

/**
 * Kyber未封装的，以向量形式存储的密钥。论文来源：
 * Joppe Bos, Léo Ducas, Eike Kiltz, Tancrède Lepoint, Vadim Lyubashevsky, John M. Schanck, Peter Schwabe,
 * Gregor Seiler, Damien Stehlé.CRYSTALS – Kyber: a CCA-secure module-lattice-based KEM
 * 2018 IEEE European Symposium on Security and Privacy (EuroS&P). IEEE, 2018: 353-367.
 *
 * @author Sheng Hu
 * @date 2022/08/25
 */
public class KyberKeyPair {
    private byte[] publicKeyBytes;
    private byte[] publicKeyGenerator;
    private short[][] privateKeyVec;

    /**
     * Default Constructor
     */
    public KyberKeyPair() {

    }

    private void setKeyPair(byte[] publicKeyVec, short[][] privateKeyVec, byte[] publicKeyGenerator) {
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

    /**
     * 计算公私钥
     * @param paramsK 安全等级
     * @param hashFunction 哈希函数
     * @param prgNoiseLength 计算噪声时的扩展函数
     * @param prgMatrixLength672 计算矩阵时的扩展函数
     * @param secureRandom 随机函数
     */
    public void generateKyberKeys(int paramsK, Hash hashFunction, Prg prgNoiseLength, Prg prgMatrixLength672, SecureRandom secureRandom){
        //私钥s
        short[][] skpv = Poly.generateNewPolyVector(paramsK);
        //最后输出时是公钥 As+e
        short[][] pkpv = Poly.generateNewPolyVector(paramsK);
        short[][] e = Poly.generateNewPolyVector(paramsK);
        //prg要求输入为16bit。
        byte[] fullSeed = new byte[KyberParams.SYM_BYTES * 2];
        byte[] publicSeed = new byte[KyberParams.SYM_BYTES];
        byte[] noiseSeed = new byte[KyberParams.SYM_BYTES];
        secureRandom.nextBytes(fullSeed);
        //将随机数前32位赋给publicSeed，后32位赋给noiseSeed
        System.arraycopy(fullSeed, 0, publicSeed, 0, KyberParams.SYM_BYTES);
        System.arraycopy(fullSeed, KyberParams.SYM_BYTES, noiseSeed, 0, KyberParams.SYM_BYTES);
        //生成了公钥中的A
        short[][][] a = Indcpa.generateMatrix(publicSeed, false, hashFunction, prgMatrixLength672, paramsK);
        byte nonce = (byte) 0;
        //生成了私钥s（k个向量）
        for (int i = 0; i < paramsK; i++) {
            skpv[i] = Poly.getNoisePoly(noiseSeed, nonce, paramsK, hashFunction, prgNoiseLength);
            nonce = (byte) (nonce + (byte) 1);
        }
        //生成了噪声，每计算一步增加一步nonce
        for (int i = 0; i < paramsK; i++) {
            e[i] = Poly.getNoisePoly(noiseSeed, nonce, paramsK, hashFunction, prgNoiseLength);
            nonce = (byte) (nonce + (byte) 1);
        }
        Poly.polyVectorNtt(skpv);
        Poly.polyVectorReduce(skpv);
        Poly.polyVectorNtt(e);
        //计算 As
        for (int i = 0; i < paramsK; i++) {
            short[] temp = Poly.polyVectorPointWiseAccMont(a[i], skpv);
            pkpv[i] = Poly.polyToMont(temp);
        }
        //计算 As+e
        Poly.polyVectorAdd(pkpv, e);
        //每做一步，计算一次模Q
        Poly.polyVectorReduce(pkpv);
        //将公钥、生成元、私钥放在一起打包
        setKeyPair(Poly.polyVectorToBytes(pkpv),skpv,publicSeed);
    }


}
