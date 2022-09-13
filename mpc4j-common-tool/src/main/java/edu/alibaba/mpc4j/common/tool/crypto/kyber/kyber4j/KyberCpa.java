package edu.alibaba.mpc4j.common.tool.crypto.kyber.kyber4j;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.Kyber;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;


import java.security.SecureRandom;


/**
 * Kyber-CPA类。
 *
 * @author Sheng Hu
 * @date 2022/09/01
 */
public class KyberCpa implements Kyber {
    /**
     * Kyber中的安全等级
     */
    private final int paramsK;
    /**
     * 公钥（As+e）长度
     */
    private final int paramsPolyvecBytes;
    /**
     * 随机数生成器
     */
    private final SecureRandom secureRandom;
    /**
     * kyber中使用到的hash函数
     */
    private final Hash hashFunction;

    /**
     * kyber中制造噪声时需要的随机扩展函数
     */
    private final Prg prgNoiseLength;
    /**
     * kyber中制造公钥时需要的随机扩展函数
     */
    private final Prg prgPkLength;
    /**
     * kyber中制造矩阵时需要的随机扩展函数
     */
    private final Prg prgMatrixLength672;


    /**
     * 初始化函数
     *
     * @param paramsK 安全等级k
     * @param envType 环境参数
     */
    public KyberCpa(int paramsK, EnvType envType) {
        this.paramsK = paramsK;
        this.secureRandom = new SecureRandom();
        this.hashFunction = HashFactory.createInstance(envType, 16);
        this.prgMatrixLength672 = PrgFactory.createInstance(envType, 672);
        switch (paramsK) {
            case 2:
                this.prgNoiseLength = PrgFactory.createInstance
                        (envType, KyberParams.ETA_512 * KyberParams.PARAMS_N / 4);
                this.prgPkLength =
                        PrgFactory.createInstance
                                (envType, KyberParams.POLY_VECTOR_BYTES_512);
                paramsPolyvecBytes = KyberParams.POLY_VECTOR_BYTES_512;
                break;
            case 3:
                this.prgNoiseLength = PrgFactory.createInstance
                        (envType, KyberParams.ETA_768_1024 * KyberParams.PARAMS_N / 4);
                this.prgPkLength =
                        PrgFactory.createInstance
                                (envType, KyberParams.POLY_VECTOR_BYTES_768);
                paramsPolyvecBytes = KyberParams.POLY_VECTOR_BYTES_768;
                break;
            case 4:
                this.prgNoiseLength = PrgFactory.createInstance
                        (envType, KyberParams.ETA_768_1024 * KyberParams.PARAMS_N / 4);
                this.prgPkLength =
                        PrgFactory.createInstance
                                (envType, KyberParams.POLY_VECTOR_BYTES_1024);
                paramsPolyvecBytes = KyberParams.POLY_VECTOR_BYTES_1024;
                break;
            default:
                throw new IllegalArgumentException("Invalid Secure level: " + paramsK);
        }

    }

    @Override
    public byte[] getRandomKyberPk() {
        byte[] newPublicKey;
        newPublicKey = new byte[paramsPolyvecBytes];
        secureRandom.nextBytes(newPublicKey);
        short[][] r = Poly.polyVectorFromBytes(newPublicKey);
        // 将生成的随机数转移至符合多项式要求的域
        Poly.polyVectorReduce(r);
        return Poly.polyVectorToBytes(r);
    }

    @Override
    public byte[] hashToByte(byte[] inputBytes) {
        return this.prgPkLength.extendToBytes(this.hashFunction.digestToBytes(inputBytes));
    }


    @Override
    public byte[] encaps(byte[] k, short[][] publicKey, byte[] publicKeyGenerator) {
        secureRandom.nextBytes(k);
        assert k.length <= KyberParams.SYM_BYTES;
        byte[] message = new byte[KyberParams.SYM_BYTES];
        // 如果加密的长度不足32bytes那么就自动补充后续的byte。
        if (k.length < KyberParams.SYM_BYTES) {
            byte[] supBytes = new byte[KyberParams.SYM_BYTES - k.length];
            secureRandom.nextBytes(supBytes);
            System.arraycopy(k, 0, message, 0, k.length);
            System.arraycopy(supBytes, 0, message, k.length, KyberParams.SYM_BYTES - k.length);
        } else {
            message = k;
        }
        return Indcpa.encrypt
                (message, publicKey, publicKeyGenerator,
                        this.paramsK, this.hashFunction, this.prgMatrixLength672, this.prgNoiseLength, this.secureRandom);
    }

    @Override
    public byte[] encaps(byte[] k, byte[] publicKey, byte[] publicKeyGenerator) {
        return encaps(k, Poly.polyVectorFromBytes(publicKey), publicKeyGenerator);
    }

    @Override
    public byte[] decaps(byte[] packedCipherText, short[][] privateKey, byte[] publicKeyBytes, byte[] publicKeyGenerator) {
        // cpa方案不要公钥进行解密
        return Indcpa.decrypt(packedCipherText, privateKey, paramsK);
    }

    @Override
    public KyberKeyPairJava generateKyberVecKeys() {
        // 私钥s
        short[][] skpv = Poly.generateNewPolyVector(paramsK);
        // 最后输出时是公钥 As+e
        short[][] pkpv = Poly.generateNewPolyVector(paramsK);
        short[][] e = Poly.generateNewPolyVector(paramsK);
        // prg要求输入为16bit。
        byte[] fullSeed = new byte[KyberParams.SYM_BYTES * 2];
        byte[] publicSeed = new byte[KyberParams.SYM_BYTES];
        byte[] noiseSeed = new byte[KyberParams.SYM_BYTES];
        secureRandom.nextBytes(fullSeed);
        // 将随机数前32位赋给publicSeed，后32位赋给noiseSeed
        System.arraycopy(fullSeed, 0, publicSeed, 0, KyberParams.SYM_BYTES);
        System.arraycopy(fullSeed, KyberParams.SYM_BYTES, noiseSeed, 0, KyberParams.SYM_BYTES);
        // 生成了公钥中的A
        short[][][] a = Indcpa.generateMatrix(publicSeed, false, hashFunction, prgMatrixLength672, paramsK);
        byte nonce = (byte) 0;
        // 生成了私钥s（k个向量）
        for (int i = 0; i < paramsK; i++) {
            skpv[i] = Poly.getNoisePoly(noiseSeed, nonce, paramsK, hashFunction, prgNoiseLength);
            nonce = (byte) (nonce + (byte) 1);
        }
        // 生成了噪声，每计算一步增加一步nonce
        for (int i = 0; i < paramsK; i++) {
            e[i] = Poly.getNoisePoly(noiseSeed, nonce, paramsK, hashFunction, prgNoiseLength);
            nonce = (byte) (nonce + (byte) 1);
        }
        Poly.polyVectorNtt(skpv);
        Poly.polyVectorReduce(skpv);
        Poly.polyVectorNtt(e);
        // 计算 As
        for (int i = 0; i < paramsK; i++) {
            short[] temp = Poly.polyVectorPointWiseAccMont(a[i], skpv);
            pkpv[i] = Poly.polyToMont(temp);
        }
        // 计算 As+e
        Poly.polyVectorAdd(pkpv, e);
        // 每做一步，计算一次模Q
        Poly.polyVectorReduce(pkpv);
        // 将公钥、生成元、私钥放在一起打包
        return new KyberKeyPairJava(Poly.polyVectorToBytes(pkpv), skpv, publicSeed);
    }

    @Override
    public byte[][] packageTwoKeys(byte[] firstKeyBytes, byte[] sencondKeyByte, byte[] publicKeyGenerator) {
        byte[][] pkPair = new byte[3][];
        pkPair[0] = new byte[paramsPolyvecBytes];
        System.arraycopy(firstKeyBytes, 0, pkPair[0], 0, paramsPolyvecBytes);
        pkPair[1] = new byte[paramsPolyvecBytes];
        System.arraycopy(sencondKeyByte, 0, pkPair[1], 0, paramsPolyvecBytes);
        pkPair[2] = new byte[KyberParams.SYM_BYTES];
        System.arraycopy(publicKeyGenerator, 0, pkPair[2], 0, KyberParams.SYM_BYTES);
        return pkPair;
    }

    @Override
    public byte[][] packageNumKeys(byte[] publicKeyBytes, byte[][] randomKeyByte, byte[] publicKeyGenerator, int choice, int n) {
        byte[][] pkPair = new byte[n + 1][];
        for (int i = 0; i < n; i++) {
            pkPair[i] = new byte[paramsPolyvecBytes];
            if (i != choice) {
                System.arraycopy(randomKeyByte[i], 0,
                        pkPair[i], 0, paramsPolyvecBytes);
            } else {
                System.arraycopy(publicKeyBytes, 0,
                        pkPair[i], 0, paramsPolyvecBytes);
            }
        }
        pkPair[n] = new byte[KyberParams.SYM_BYTES];
        System.arraycopy(publicKeyGenerator, 0,
                pkPair[n], 0, KyberParams.SYM_BYTES);
        return pkPair;
    }

}
