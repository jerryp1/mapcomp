package edu.alibaba.mpc4j.common.tool.crypto.kyber.kyber4j;

import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.Kyber;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.KyberKeyPair;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;

import static edu.alibaba.mpc4j.common.tool.crypto.kyber.kyber4j.Indcpa.*;

import java.security.SecureRandom;
import java.util.Arrays;


/**
 * Kyber-CPA抽象类。
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
     * kyber中使用到的hash函数
     */
    private final Prg prgNoiseLength;
    /**
     * 生成指定hash函数时所需要使用的prg
     */
    private final Prg prgPkLength;
    /**
     * 生成指定hash函数时所需要使用的prg
     */
    private final Prg prgMatrixLength672;


    /**
     * @param paramsK 安全等级
     */
    public KyberCpa(int paramsK, SecureRandom secureRandom, Hash hashFunction) {
        this.paramsK = paramsK;
        this.secureRandom = secureRandom;
        this.hashFunction = hashFunction;
        this.prgMatrixLength672 = PrgFactory.createInstance
                (PrgFactory.PrgType.BC_SM4_ECB, 672);
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
        short[][] r = polyVectorFromBytes(newPublicKey);
        //将生成的随机数转移至符合多项式要求的域
        Poly.polyVectorReduce(r);
        return Poly.polyVectorToBytes(r);
    }

    @Override
    public short[][] hashToKyberPk(short[][] inputVector) {
        byte[] inputByte = polyVectorToBytes(inputVector);
        return hashToKyberPk(inputByte);
    }

    @Override
    public short[][] hashToKyberPk(byte[] inputBytes) {
        short[][] r =
                polyVectorFromBytes(this.prgPkLength.extendToBytes(this.hashFunction.digestToBytes(inputBytes)));
        Poly.polyVectorReduce(r);
        return r;
    }

    @Override
    public byte[] hashToByte(short[][] inputVector) {
        return this.prgPkLength.extendToBytes(this.hashFunction.digestToBytes(polyVectorToBytes(inputVector)));
    }

    @Override
    public byte[] hashToByte(byte[] inputByte) {
        return this.prgPkLength.extendToBytes(this.hashFunction.digestToBytes(inputByte));
    }

    @Override
    public byte[] encrypt(byte[] m, byte[] publicKey) {
        UnpackedPublicKey unpackedPublicKey = unpackPublicKey(publicKey, paramsK);
        return encrypt(m, unpackedPublicKey.getPublicKeyPolyvec(), unpackedPublicKey.getSeed());
    }

    @Override
    public byte[] encrypt(byte[] m, short[][] publicKey, byte[] publicKeyGenerator) {
        byte[] coins = new byte[KyberParams.SYM_BYTES];
        short[][] r = Poly.generateNewPolyVector(paramsK);
        short[][] ep = Poly.generateNewPolyVector(paramsK);
        short[][] bp = Poly.generateNewPolyVector(paramsK);
        //将m转换为多项式
        short[] k = Poly.polyFromData(m);
        //注意，这里的T/F，和KEY生成的时候是不一样的，计算的是转制后的A
        short[][][] at = Indcpa.generateMatrix(publicKeyGenerator, true, hashFunction, prgMatrixLength672, paramsK);
        //生成的随机参数，是r和e1
        for (int i = 0; i < paramsK; i++) {
            r[i] = Poly.getNoisePoly(coins, (byte) (i), paramsK, this.hashFunction, this.prgNoiseLength);
            ep[i] = Poly.getNoisePoly(coins, (byte) (i + paramsK), 3, this.hashFunction, this.prgNoiseLength);
        }
        //这个是e2
        short[] epp = Poly.getNoisePoly(coins, (byte) (paramsK * 2), 3, this.hashFunction, this.prgNoiseLength);
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

    @Override
    public byte[] encrypt(byte[] m, byte[] publicKey, byte[] publicKeyGenerator) {
        return encrypt(m, Poly.polyVectorFromBytes(publicKey), publicKeyGenerator);
    }

    @Override
    public byte[] decrypt(byte[] packedCipherText, byte[] privateKey) {
        //获得私钥
        short[][] unpackedPrivateKey = unpackPrivateKey(privateKey);
        return decrypt(packedCipherText, unpackedPrivateKey);
    }

    @Override
    public byte[] decrypt(byte[] packedCipherText, short[][] privateKey) {
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

    @Override
    public KyberKeyPair generateKyberVecKeys() {
        KyberKeyPair packedPki = new KyberKeyPair();
        packedPki.generateKyberKeys
                (this.paramsK, this.hashFunction, this.prgNoiseLength, this.prgMatrixLength672, this.secureRandom);
        return packedPki;
    }

    @Override
    public byte[] polyVectorToBytes(short[][] polyA) {
        byte[] r = new byte[this.paramsK * KyberParams.POLY_BYTES];
        for (int i = 0; i < this.paramsK; i++) {
            byte[] byteA = Poly.polyToBytes(polyA[i]);
            System.arraycopy(byteA, 0, r, i * KyberParams.POLY_BYTES, byteA.length);
        }
        return r;
    }

    @Override
    public short[][] polyVectorFromBytes(byte[] polyA) {
        short[][] r = new short[this.paramsK][KyberParams.POLY_BYTES];
        for (int i = 0; i < this.paramsK; i++) {
            int start = (i * KyberParams.POLY_BYTES);
            int end = (i + 1) * KyberParams.POLY_BYTES;
            r[i] = Poly.polyFromBytes(Arrays.copyOfRange(polyA, start, end));
        }
        return r;
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
