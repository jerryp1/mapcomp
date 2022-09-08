package edu.alibaba.mpc4j.common.tool.crypto.kyber.kyber4j;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.Kyber;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.security.SecureRandom;


/**
 * Kyber-CCA类。
 *
 * @author Sheng Hu
 * @date 2022/09/01
 */
public class KyberCca implements Kyber {
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
     * kyber中计算加密时需要的随机扩展函数
     */
    private final Prg prgEncryptLength32;
    /**
     * kyber中计算加密时需要的随机扩展函数(用于之前两个的汇总)
     */
    private final Prg prgEncryptLength64;

    /**
     * 初始化函数
     *
     * @param paramsK      安全参数k
     */
    public KyberCca(int paramsK) {
        this.paramsK = paramsK;
        this.secureRandom = new SecureRandom();
        this.hashFunction = HashFactory.createInstance(HashFactory.HashType.BC_BLAKE_2B_160,16);
        this.prgMatrixLength672 = PrgFactory.createInstance
                (PrgFactory.PrgType.BC_SM4_ECB, 672);
        this.prgEncryptLength32 = PrgFactory.createInstance(PrgFactory.PrgType.BC_SM4_ECB, KyberParams.SYM_BYTES);
        this.prgEncryptLength64 = PrgFactory.createInstance(PrgFactory.PrgType.BC_SM4_ECB, 2 * KyberParams.SYM_BYTES);
        switch (paramsK) {
            case 2:
                this.prgNoiseLength = PrgFactory.createInstance
                        (PrgFactory.PrgType.BC_SM4_ECB, KyberParams.ETA_512 * KyberParams.PARAMS_N / 4);
                this.prgPkLength =
                        PrgFactory.createInstance
                                (PrgFactory.PrgType.BC_SM4_ECB, KyberParams.POLY_VECTOR_BYTES_512);
                paramsPolyvecBytes = KyberParams.POLY_VECTOR_BYTES_512;
                break;
            case 3:
                this.prgNoiseLength = PrgFactory.createInstance
                        (PrgFactory.PrgType.BC_SM4_ECB, KyberParams.ETA_768_1024 * KyberParams.PARAMS_N / 4);
                this.prgPkLength =
                        PrgFactory.createInstance
                                (PrgFactory.PrgType.BC_SM4_ECB, KyberParams.POLY_VECTOR_BYTES_768);
                paramsPolyvecBytes = KyberParams.POLY_VECTOR_BYTES_768;
                break;
            case 4:
                this.prgNoiseLength = PrgFactory.createInstance
                        (PrgFactory.PrgType.BC_SM4_ECB, KyberParams.ETA_768_1024 * KyberParams.PARAMS_N / 4);
                this.prgPkLength =
                        PrgFactory.createInstance
                                (PrgFactory.PrgType.BC_SM4_ECB, KyberParams.POLY_VECTOR_BYTES_1024);
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
        //将生成的随机数转移至符合多项式要求的域
        Poly.polyVectorReduce(r);
        return Poly.polyVectorToBytes(r);
    }

    @Override
    public byte[] hashToByte(byte[] inputBytes) {
        return this.prgPkLength.extendToBytes(this.hashFunction.digestToBytes(inputBytes));
    }

    @Override
    public byte[] encaps(byte[] k, short[][] publicKeyVec, byte[] publicKeyGenerator) {
        return encaps(k, Poly.polyVectorToBytes(publicKeyVec), publicKeyVec, publicKeyGenerator);
    }

    @Override
    public byte[] encaps(byte[] k, byte[] publicKeyBytes, byte[] publicKeyGenerator) {
        return encaps(k, publicKeyBytes, Poly.polyVectorFromBytes(publicKeyBytes), publicKeyGenerator);
    }

    /**
     * 基于cca的打包函数，中间会修改message。
     * @param k 输入的秘密值，会在函数中修改
     * @param publicKeyBytes 公钥
     * @param publicKeyVector 公钥的short格式（两者同时需要的原因在于byte格式的用于计算hash值，short格式的用于加密）
     * @param publicKeyGenerator 公钥的生成元
     * @return 返回值为加密后的密文
     */
    public byte[] encaps(byte[] k, byte[] publicKeyBytes, short[][] publicKeyVector, byte[] publicKeyGenerator) {
        byte[] randomSeed = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(randomSeed);
        byte[] m = prgEncryptLength32.extendToBytes(hashFunction.digestToBytes(randomSeed));
        // 计算G（H（pk），m）需要注意的是原有代码是hash到了64bit，我们这里是使用了相同的扩展函数扩展到64bit的。
        byte[] kr =countHashKey(m,publicKeyBytes,publicKeyGenerator);
        // 从中选取后32bit作为随机数种子
        byte[] coins = new byte[KyberParams.SYM_BYTES];
        System.arraycopy(kr, KyberParams.SYM_BYTES, coins, 0, KyberParams.SYM_BYTES);
        // 以subkr为随机数种子计算密文c
        byte[] cipherText =
                Indcpa.encrypt(m, publicKeyVector, publicKeyGenerator,
                        paramsK, hashFunction, prgMatrixLength672, prgNoiseLength, coins);
        // 论文中的H(C)
        byte[] krc = prgEncryptLength32.extendToBytes(hashFunction.digestToBytes(cipherText));
        byte[] newKr = new byte[2 * KyberParams.SYM_BYTES];
        System.arraycopy(kr, 0, newKr, 0, KyberParams.SYM_BYTES);
        System.arraycopy(krc, 0, newKr, KyberParams.SYM_BYTES, KyberParams.SYM_BYTES);
        // 请注意这里修改了k这个值。
        byte[] newMessage = hashFunction.digestToBytes(newKr);
        System.arraycopy(newMessage, 0, k, 0, KyberParams.SYM_BYTES / CommonConstants.BLOCK_LONG_LENGTH);
        return cipherText;
    }

    @Override
    public byte[] decaps(byte[] packedCipherText, short[][] privateKey, byte[] publicKeyBytes, byte[] publicKeyGenerator) {
        byte[] message = Indcpa.decrypt(packedCipherText, privateKey, this.paramsK);
        // 计算G（H（pk），m）需要注意的是原有代码是hash到了64bit，我们这里是使用了相同的扩展函数扩展到64bit的。
        byte[] kr =countHashKey(message,publicKeyBytes,publicKeyGenerator);
        // 从kr中选取后32bit作为随机数种子
        byte[] coins = new byte[KyberParams.SYM_BYTES];
        System.arraycopy(kr, KyberParams.SYM_BYTES, coins, 0, KyberParams.SYM_BYTES);
        // 以subkr为随机数种子计算密文c
        byte[] cipherText =
                Indcpa.encrypt(message, Poly.polyVectorFromBytes(publicKeyBytes), publicKeyGenerator,
                        paramsK, hashFunction, prgMatrixLength672, prgNoiseLength, coins);
        // 论文中的H(C)
        byte[] krc = prgEncryptLength32.extendToBytes(hashFunction.digestToBytes(cipherText));
        byte[] newKr = new byte[2 * KyberParams.SYM_BYTES];
        if (BytesUtils.equals(cipherText, packedCipherText)) {
            // 读取kr的前32bytes
            System.arraycopy(kr, 0, newKr, 0, KyberParams.SYM_BYTES);
        } else {
            // 如果走入这个分支说明输入的密文是错误的，因此返回的解密值也是随机的
            byte[] randomByte = new byte[KyberParams.SYM_BYTES];
            secureRandom.nextBytes(randomByte);
            System.arraycopy(randomByte, 0, newKr, 0, KyberParams.SYM_BYTES);
        }
        System.arraycopy(krc, 0, newKr, KyberParams.SYM_BYTES, KyberParams.SYM_BYTES);
        return hashFunction.digestToBytes(newKr);
    }

    /**
     * 计算论文中的 G（H（PK），m）
     * @param message 需要加密的秘密值
     * @param publicKeyBytes 公钥（As+e）
     * @param publicKeyGenerator 公钥生成元
     * @return  G（H（PK），m）
     */
    public byte[] countHashKey(byte[] message, byte[] publicKeyBytes, byte[] publicKeyGenerator){
        byte[] fullKey = new byte[paramsPolyvecBytes + KyberParams.SYM_BYTES];
        System.arraycopy(publicKeyBytes, 0, fullKey, 0, paramsPolyvecBytes);
        System.arraycopy(publicKeyGenerator, 0, fullKey, paramsPolyvecBytes, KyberParams.SYM_BYTES);
        byte[] publicHashKey = prgEncryptLength32.extendToBytes(hashFunction.digestToBytes(fullKey));
        byte[] fullCode = new byte[2 * KyberParams.SYM_BYTES];
        System.arraycopy(message, 0, fullCode, 0, KyberParams.SYM_BYTES);
        System.arraycopy(publicHashKey, 0, fullCode, KyberParams.SYM_BYTES, KyberParams.SYM_BYTES);
        // 计算G（H（pk），m）需要注意的是原有代码是hash到了64bit，我们这里是使用了相同的扩展函数扩展到64bit的。
        return prgEncryptLength64.extendToBytes(hashFunction.digestToBytes(fullCode));
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
    public byte[][] packageTwoKeys(byte[] publicKeyBytes, byte[] randomKeyByte, byte[] publicKeyGenerator, int sigma) {
        byte[][] pkPair = new byte[3][];
        pkPair[sigma] = new byte[paramsPolyvecBytes];
        System.arraycopy(publicKeyBytes, 0, pkPair[sigma], 0, paramsPolyvecBytes);
        pkPair[1 - sigma] = new byte[paramsPolyvecBytes];
        System.arraycopy(randomKeyByte, 0, pkPair[1 - sigma], 0, paramsPolyvecBytes);
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
