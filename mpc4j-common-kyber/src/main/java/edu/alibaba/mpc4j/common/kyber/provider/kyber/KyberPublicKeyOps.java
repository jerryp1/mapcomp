package edu.alibaba.mpc4j.common.kyber.provider.kyber;

import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Kyber公钥操作。论文来源：
 * Mansy D, Rindal P. Endemic oblivious transfer. CCS 2019. 2019: 309-326.
 * @author Sheng Hu
 * @date 2022/08/24
 */
public class KyberPublicKeyOps {
    /**
     * 根据输入的seed，和安全参数生成公钥
     * @param hashInput 随机数种子
     * @param hashFunction 随机数生成器
     * @return 论文中的公钥（As+e,p）和私钥s
     */
    public static byte[] kyberPKHash(byte[] hashInput,Hash hashFunction){
        int byteLength = hashInput.length;
        int startPoint = 0;
        byte[] hashOutput = new byte[byteLength];
        while(byteLength > KyberParams.paramsSymBytes){
            System.arraycopy(hashFunction.digestToBytes(Arrays.copyOfRange(hashInput,startPoint,startPoint + KyberParams.paramsSymBytes)),
                    0,hashOutput,startPoint,KyberParams.paramsSymBytes);
            startPoint = startPoint + KyberParams.paramsSymBytes;
            byteLength = byteLength - KyberParams.paramsSymBytes;
        }
        if(byteLength > 0){
            System.arraycopy(hashFunction.digestToBytes(Arrays.copyOfRange(hashInput,startPoint,startPoint + byteLength)),
                    0,hashOutput,startPoint,byteLength);
        }
        short[][] r = Poly.polyVectorFromBytes(hashOutput);
        Poly.polyVectorReduce(r);
        hashOutput = Poly.polyVectorToBytes(r);

        return hashOutput;
    }

    /**
     * 根据输入的seed，和安全参数生成公钥
     * @param paramsK 安全参数
     * @return 论文中的公钥（As+e,p）和私钥s
     */
    public static byte[] getRandomKyberPK(int paramsK) throws NoSuchAlgorithmException {
        byte[] newPublicKey;
        switch (paramsK) {
            case 2:
                newPublicKey = new byte[KyberParams.paramsPolyvecBytesK512];
                break;
            case 3:
                newPublicKey = new byte[KyberParams.paramsPolyvecBytesK768];
                break;
            default:
                newPublicKey = new byte[KyberParams.paramsPolyvecBytesK1024];
        }
        //采用了代码中原有的随机数生成方式。
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(newPublicKey);
        short[][] r = Poly.polyVectorFromBytes(newPublicKey);
        Poly.polyVectorReduce(r);
        newPublicKey = Poly.polyVectorToBytes(r);
        return newPublicKey;
    }

    /**
     * 计算两个公钥的和
     * @param KeyA 多项式A的参数
     * @param KeyB 多项式B的参数
     * @return 多项式A+B的参数
     */
    public static short[][] kyberPKAdd(short[][] KeyA,short[][] KeyB){
        short[][] KeyC = Poly.polyVectorAdd(KeyA,KeyB);
        return Poly.polyVectorReduce(KeyC);
    }

    /**
     * 计算两个公钥的差
     * @param KeyA 多项式A的参数
     * @param KeyB 多项式B的参数
     * @return 多项式A-B的参数
     */
    public static short[][] kyberPKSub(short[][] KeyA,short[][] KeyB){
        int paramsK = KeyA.length;
        short[][] KeyC = new short[paramsK][];
        for(int i = 0;i < paramsK;i++){
            KeyC[i] = Poly.polySub(KeyA[i],KeyB[i]);
        }
        return Poly.polyVectorReduce(KeyC);
    }


}
