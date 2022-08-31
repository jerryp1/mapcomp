package edu.alibaba.mpc4j.common.tool.crypto.kyber;

import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;

import java.security.SecureRandom;

/**
 * Kyber钥匙操作（IND-CPA）。论文来源：
 * Mansy D, Rindal P. Endemic oblivious transfer. CCS 2019. 2019: 309-326.
 * @author Sheng Hu
 * @date 2022/08/24
 */
public class KyberKeyOps {

    /**
     * Generates public and private keys for the CPA-secure public-key
     * encryption scheme underlying Kyber.
     * @param paramsK 安全参数
     * @return 论文中的公钥（As+e,p）和私钥s
     */
    public static KyberVecPki generateKyberKeys(int paramsK)  {
        //私钥s
        short[][] skpv = Poly.generateNewPolyVector(paramsK);
        //最后输出时是公钥 As+e
        short[][] pkpv = Poly.generateNewPolyVector(paramsK);
        short[][] e = Poly.generateNewPolyVector(paramsK);
        //prg要求输入为16bit。
        byte[] prgSeed = new byte[KyberParams.SYM_BYTES /2];
        byte[] publicSeed = new byte[KyberParams.SYM_BYTES];
        byte[] noiseSeed = new byte[KyberParams.SYM_BYTES];
        Prg prgFunction = PrgFactory.createInstance(PrgFactory.PrgType.JDK_AES_ECB,64);
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(prgSeed);
        //随机数被扩展至64位
        byte[] fullSeed = prgFunction.extendToBytes(prgSeed);
        //将随机数前32位赋给publicSeed，后32位赋给noiseSeed
        System.arraycopy(fullSeed, 0, publicSeed, 0, KyberParams.SYM_BYTES);
        System.arraycopy(fullSeed, KyberParams.SYM_BYTES, noiseSeed, 0, KyberParams.SYM_BYTES);
        //生成了公钥中的A
        short[][][] a = Indcpa.generateMatrix(publicSeed, false, paramsK);
        byte nonce = (byte) 0;
        //生成了私钥s（k个向量）
        for (int i = 0; i < paramsK; i++) {
            skpv[i] = Poly.getNoisePoly(noiseSeed, nonce, paramsK);
            nonce = (byte) (nonce + (byte) 1);
        }
        //生成了噪声，每计算一步增加一步nonce
        for (int i = 0; i < paramsK; i++) {
            e[i] = Poly.getNoisePoly(noiseSeed, nonce, paramsK);
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
        KyberVecPki packedPki = new KyberVecPki();
        packedPki.setPublicKeyVec(pkpv);
        packedPki.setPublicKeyGenerator(publicSeed);
        packedPki.setPrivateKeyVec(skpv);
        return packedPki;
    }

    /**
     * Decrypt the given byte array using the Kyber public-key encryption scheme
     *
     * @param packedCipherText 压缩，打包后的密文
     * @param privateKey 私钥
     * @param paramsK 安全参数K
     * @return 消息m
     */
    public static byte[] decrypt(byte[] packedCipherText, short[][] privateKey, int paramsK) {
        //解压密文并获得U和v
        UnpackedCipherText unpackedCipherText = Indcpa.unpackCiphertext(packedCipherText, paramsK);
        short[][] u = unpackedCipherText.getU();
        short[] v = unpackedCipherText.getV();
        //将U转为NTT域
        Poly.polyVectorNtt(u);
        // 执行乘法 计算 Us
        short[] mp = Poly.polyVectorPointWiseAccMont(privateKey, u);
        //将乘积转回正常计算域
        Poly.polyInvNttMont(mp);
        // U - v
        mp = Poly.polySub(v, mp);
        Poly.polyReduce(mp);
        //将结果返回成消息
        return Poly.polyToMsg(mp);
    }

    /**
     * Encrypt the given message using the Kyber public-key encryption scheme
     *
     * @param m 加密的消息m
     * @param publicKey 公钥，包含了As+e 和生成 A的种子 p
     * @param coins 生成随机数的种子
     * @param paramsK 安全参数
     * @return 加密后的密文
     */
    public static byte[] encrypt(byte[] m, short[][] publicKey, byte[] publicKeyGenerator,byte[] coins, int paramsK) {
        short[][] r = Poly.generateNewPolyVector(paramsK);
        short[][] ep = Poly.generateNewPolyVector(paramsK);
        short[][] bp = Poly.generateNewPolyVector(paramsK);
        //将m转换为多项式
        short[] k = Poly.polyFromData(m);
        //注意，这里的T/F，和KEY生成的时候是不一样的，计算的是转制后的A
        short[][][] at = Indcpa.generateMatrix(publicKeyGenerator, true, paramsK);
        //生成的随机参数，是r和e1
        for (int i = 0; i < paramsK; i++) {
            r[i] = Poly.getNoisePoly(coins, (byte) (i), paramsK);
            ep[i] = Poly.getNoisePoly(coins, (byte) (i + paramsK), 3);
        }
        //这个是e2
        short[] epp = Poly.getNoisePoly(coins, (byte) (paramsK * 2), 3);
        //将r转换到NTT域进行计算
        Poly.polyVectorNtt(r);
        Poly.polyVectorReduce(r);
        //计算Ar
        for (int i = 0; i < paramsK; i++) {
            bp[i] = Poly.polyVectorPointWiseAccMont(at[i], r);
        }
        //（As+e）* r
        short[] v = Poly.polyVectorPointWiseAccMont(publicKey, r);
        //取消INV域
        Poly.polyVectorInvNttMont(bp);
        //取消INV域
        Poly.polyInvNttMont(v);
        //计算Ar + e1
        Poly.polyVectorAdd(bp, ep);
        // 计算（As+e）* r + e_2 + m
        Poly.polyAdd(Poly.polyAdd(v, epp), k);
        //不知道为什么（As+e）* r + e_2 + m不需要进行reduce。
        Poly.polyVectorReduce(bp);
        //返回密文，pack的时候会执行压缩函数
        return Indcpa.packCiphertext(bp, Poly.polyReduce(v), paramsK);
    }

}
