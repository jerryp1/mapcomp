package edu.alibaba.mpc4j.common.tool.crypto.kyber.kyber4j;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.Kyber;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.KyberKey;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.KyberKeyFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.security.SecureRandom;

import static edu.alibaba.mpc4j.common.tool.crypto.kyber.kyber4j.Indcpa.unpackPublicKey;
/**
 * Kyber-CCA抽象类。
 *
 * @author Sheng Hu
 * @date 2022/09/01
 */
public class KyberCca  implements Kyber{
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
     * @param paramsK 安全参数k
     * @param secureRandom random函数
     * @param hashFunction 随机数种子
     */
    public KyberCca(int paramsK, SecureRandom secureRandom, Hash hashFunction) {
        this.paramsK = paramsK;
        this.secureRandom = secureRandom;
        this.hashFunction = hashFunction;
        this.prgMatrixLength672 = PrgFactory.createInstance
                (PrgFactory.PrgType.BC_SM4_ECB, 672);
        this.prgEncryptLength32 = PrgFactory.createInstance(PrgFactory.PrgType.BC_SM4_ECB,KyberParams.SYM_BYTES);
        this.prgEncryptLength64 = PrgFactory.createInstance(PrgFactory.PrgType.BC_SM4_ECB,2 * KyberParams.SYM_BYTES);
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
    public byte[] hashToByte(short[][] inputVector) {
        return this.prgPkLength.extendToBytes(this.hashFunction.digestToBytes(Poly.polyVectorToBytes(inputVector)));
    }

    @Override
    public byte[] hashToByte(byte[] inputBytes) {
        return this.prgPkLength.extendToBytes(this.hashFunction.digestToBytes(inputBytes));
    }

    @Override
    public byte[] encrypt(byte[] m, byte[] publicKey) {
        UnpackedPublicKey unpackedPublicKey = unpackPublicKey(publicKey, paramsK);
        byte[] newPublicKey = Poly.polyVectorToBytes(unpackedPublicKey.getPublicKeyPolyvec());
        return encrypt(m,newPublicKey, unpackedPublicKey.getPublicKeyPolyvec(), unpackedPublicKey.getSeed());
    }

    @Override
    public byte[] encrypt(byte[] m, short[][] publicKeyVec, byte[] publicKeyGenerator) {
        return encrypt(m, Poly.polyVectorToBytes(publicKeyVec),publicKeyVec, publicKeyGenerator);
    }

    @Override
    public byte[] encrypt(byte[] m, byte[] publicKeyBytes, byte[] publicKeyGenerator) {
        return encrypt(m, publicKeyBytes,Poly.polyVectorFromBytes(publicKeyBytes), publicKeyGenerator);
    }

    public byte[] encrypt(byte[] message, byte[] publicKeyBytes,short[][] publicKeyVector ,byte[] publicKeyGenerator){
        byte[] m = prgEncryptLength32.extendToBytes(hashFunction.digestToBytes(message));
        byte[] fullKey = new byte[paramsPolyvecBytes + KyberParams.SYM_BYTES];
        System.arraycopy(publicKeyBytes,0,fullKey,0,paramsPolyvecBytes);
        System.arraycopy(publicKeyGenerator,0,fullKey,paramsPolyvecBytes,KyberParams.SYM_BYTES);
        byte[] publicHashKey = prgEncryptLength32.extendToBytes(hashFunction.digestToBytes(fullKey));
        byte[] fullCode = new byte[2 * KyberParams.SYM_BYTES];
        System.arraycopy(m,0,fullCode,0,KyberParams.SYM_BYTES);
        System.arraycopy(publicHashKey,0,fullCode,KyberParams.SYM_BYTES,KyberParams.SYM_BYTES);
        //计算G（H（pk），m）需要注意的是原有代码是hash到了64bit，我们这里是使用了相同的扩展函数扩展到64bit的。
        byte[] kr = prgEncryptLength64.extendToBytes(hashFunction.digestToBytes(fullCode));
        //从中选取后32bit作为随机数种子
        byte[] subkr = new byte[KyberParams.SYM_BYTES];
        System.arraycopy(kr,KyberParams.SYM_BYTES,subkr,0,KyberParams.SYM_BYTES);
        //以subkr为随机数种子计算密文c
        byte[] cipherText =
                Indcpa.encrypt(m,publicKeyVector,publicKeyGenerator,
                        paramsK,hashFunction,prgMatrixLength672,prgNoiseLength,subkr);
        //论文中的H(C)
        byte[] krc = prgEncryptLength32.extendToBytes(hashFunction.digestToBytes(cipherText));
        byte[] newKr = new byte[2 * KyberParams.SYM_BYTES];
        System.arraycopy(kr,0,newKr,0,KyberParams.SYM_BYTES);
        System.arraycopy(krc,0,newKr,KyberParams.SYM_BYTES,KyberParams.SYM_BYTES);
        //请注意这里修改了message这个值，是作为密文输入的
        byte[] newMessage = hashFunction.digestToBytes(newKr);
        System.arraycopy(newMessage, 0, message, 0, KyberParams.SYM_BYTES / CommonConstants.BLOCK_LONG_LENGTH);
        return cipherText;
    }

    @Override
    public byte[] decrypt(byte[] packedCipherText, byte[] privateKey) {
        assert false:"Wrong input, CCA decryption scheme needs to use public key";
        return new byte[0];
    }

    @Override
    public byte[] decrypt(byte[] packedCipherText, short[][] privateKey) {
        assert false:"Wrong input, CCA decryption scheme needs to use public key";
        return new byte[0];
    }

    @Override
    public byte[] decrypt(byte[] packedCipherText, short[][] privateKey, byte[] publicKeyBytes, byte[] publicKeyGenerator) {
        byte[] message = Indcpa.decrypt(packedCipherText, privateKey, this.paramsK);
        byte[] fullKey = new byte[paramsPolyvecBytes + KyberParams.SYM_BYTES];
        System.arraycopy(publicKeyBytes,0,fullKey,0,paramsPolyvecBytes);
        System.arraycopy(publicKeyGenerator,0,fullKey,paramsPolyvecBytes,KyberParams.SYM_BYTES);
        byte[] publicHashKey = prgEncryptLength32.extendToBytes(hashFunction.digestToBytes(fullKey));
        byte[] fullCode = new byte[2 * KyberParams.SYM_BYTES];
        System.arraycopy(message,0,fullCode,0,KyberParams.SYM_BYTES);
        System.arraycopy(publicHashKey,0,fullCode,KyberParams.SYM_BYTES,KyberParams.SYM_BYTES);
        //计算G（H（pk），m）需要注意的是原有代码是hash到了64bit，我们这里是使用了相同的扩展函数扩展到64bit的。
        byte[] kr = prgEncryptLength64.extendToBytes(hashFunction.digestToBytes(fullCode));
        //从kr中选取后32bit作为随机数种子
        byte[] subkr = new byte[KyberParams.SYM_BYTES];
        System.arraycopy(kr,KyberParams.SYM_BYTES,subkr,0,KyberParams.SYM_BYTES);
        //以subkr为随机数种子计算密文c
        byte[] cipherText =
                Indcpa.encrypt(message,Poly.polyVectorFromBytes(publicKeyBytes),publicKeyGenerator,
                        paramsK,hashFunction,prgMatrixLength672,prgNoiseLength,subkr);
        //论文中的H(C)
        byte[] krc = prgEncryptLength32.extendToBytes(hashFunction.digestToBytes(cipherText));
        byte[] newKr = new byte[2 * KyberParams.SYM_BYTES];
        if(BytesUtils.equals(cipherText,packedCipherText)){
            //读取kr的前32bytes
            System.arraycopy(kr,0,newKr,0,KyberParams.SYM_BYTES);

        }else {
            //如果走入这个分支说明输入的密文是错误的，因此返回的解密值也是随机的
            byte[] randomByte = new byte[KyberParams.SYM_BYTES];
            secureRandom.nextBytes(randomByte);
            System.arraycopy(randomByte,0,newKr,0,KyberParams.SYM_BYTES);
        }
        System.arraycopy(krc,0,newKr,KyberParams.SYM_BYTES,KyberParams.SYM_BYTES);
        return hashFunction.digestToBytes(newKr);
    }

    @Override
    public KyberKey generateKyberVecKeys() {
        KyberKey packedKey = KyberKeyFactory.createInstance(KyberKeyFactory.KyberKeyType.KYBER_KEY_JAVA,
                this.paramsK,this.secureRandom,this.hashFunction,this.prgNoiseLength,this.prgMatrixLength672);
        packedKey.generateKyberKeys();
        return packedKey;
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
