package edu.alibaba.mpc4j.common.tool.crypto.kyber.kyber4j;

import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.Kyber;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.KyberKey;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.KyberKeyFactory;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.KyberKeyPairJava;
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
        assert m.length <= KyberParams.SYM_BYTES;
        byte[] message = new byte[KyberParams.SYM_BYTES];
        //如果加密的长度不足32bytes那么就自动补充后续的byte。
        if(m.length < KyberParams.SYM_BYTES){
            byte[] supBytes = new byte[KyberParams.SYM_BYTES - m.length];
            secureRandom.nextBytes(supBytes);
            System.arraycopy(m,0,message,0,m.length);
            System.arraycopy(supBytes,0,message,m.length,KyberParams.SYM_BYTES - m.length);
        }else {
            message = m;
        }
        return Indcpa.encrypt
                (message,publicKey,publicKeyGenerator,this.paramsK,this.hashFunction,this.prgMatrixLength672,this.prgNoiseLength);
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
    public KyberKey generateKyberVecKeys() {
        KyberKey packedKey = KyberKeyFactory.createInstance(KyberKeyFactory.KyberKeyType.KYBER_KEY_JAVA,
                this.paramsK,this.secureRandom,this.hashFunction,this.prgNoiseLength,this.prgMatrixLength672);
        packedKey.generateKyberKeys();
        return packedKey;
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
