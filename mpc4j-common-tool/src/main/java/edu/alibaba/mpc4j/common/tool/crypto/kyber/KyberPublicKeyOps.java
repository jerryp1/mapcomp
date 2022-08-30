package edu.alibaba.mpc4j.common.tool.crypto.kyber;

import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
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
     * @param inputVector 随机数种子
     * @param hashFunction 随机数生成器
     * @return 论文中的公钥（As+e,p）和私钥s
     */
    public static short[][] kyberPkHash(short[][] inputVector, Hash hashFunction){
        byte[] hashInput = Poly.polyVectorToBytes(inputVector);
        int byteLength = hashInput.length;
        int startPoint = 0;
        byte[] hashOutput = new byte[byteLength];
        while(byteLength > KyberParams.SYM_BYTES){
            System.arraycopy(hashFunction.digestToBytes(Arrays.copyOfRange(hashInput,startPoint,startPoint + KyberParams.SYM_BYTES)),
                    0,hashOutput,startPoint,KyberParams.SYM_BYTES);
            startPoint = startPoint + KyberParams.SYM_BYTES;
            byteLength = byteLength - KyberParams.SYM_BYTES;
        }
        if(byteLength > 0){
            System.arraycopy(hashFunction.digestToBytes(Arrays.copyOfRange(hashInput,startPoint,startPoint + byteLength)),
                    0,hashOutput,startPoint,byteLength);
        }
        short[][] r = Poly.polyVectorFromBytes(hashOutput);
        Poly.polyVectorReduce(r);

        return r;
    }

    /**
     * 由安全参数随机生成公钥
     * @param paramsK 安全参数
     * @return 论文中的公钥 As+e
     */
    public static short[][] getRandomKyberPk(int paramsK){
        byte[] newPublicKey;

        switch (paramsK) {
            case 2:
                newPublicKey = new byte[KyberParams.POLY_VECTOR_BYTES_512];
                break;
            case 3:
                newPublicKey = new byte[KyberParams.POLY_VECTOR_BYTES_768];
                break;
            default:
                newPublicKey = new byte[KyberParams.POLY_VECTOR_BYTES_1024];
        }
        //采用了代码中原有的随机数生成方式。
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(newPublicKey);
        short[][] r = Poly.polyVectorFromBytes(newPublicKey);
        //将生成的随机数转移至符合多项式要求的域
        Poly.polyVectorReduce(r);
        return r;
    }

    /**
     * 由安全参数随机生成公钥
     * @return 论文中的公钥 As+e
     */
    public static byte[] getRandomKeyGenerator(){
        //随机数种子为32byte
        byte[] newRandomKeyGenerator = new byte[KyberParams.SYM_BYTES];
        //随机生成
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(newRandomKeyGenerator);
        return newRandomKeyGenerator;
    }

    /**
     * 计算两个公钥的和
     * @param keyA 多项式A的参数
     * @param keyB 多项式B的参数
     * @return 多项式A+B的参数
     */
    public static short[][] kyberPkAdd(short[][] keyA, short[][] keyB){
        short[][] keyC = Poly.polyVectorAdd(keyA,keyB);
        return Poly.polyVectorReduce(keyC);
    }

    /**
     * 计算两个公钥的差
     * @param keyA 多项式A的参数
     * @param keyB 多项式B的参数
     * @return 多项式A-B的参数
     */
    public static short[][] kyberPkSub(short[][] keyA, short[][] keyB){
        int paramsK = keyA.length;
        short[][] keyC = new short[paramsK][];
        for(int i = 0;i < paramsK;i++){
            keyC[i] = polySub(keyA[i],keyB[i]);
        }
        return Poly.polyVectorReduce(keyC);
    }

    /**
     * Subtract two polynomials
     *
     * @param polyA 多项式A的系数组
     * @param polyB 多项式B的系数组
     * @return 返回值是多项式A的系数减多项式B的系数
     */
    public static short[] polySub(short[] polyA, short[] polyB) {
        short[] polyC = new short[polyA.length];
        for (int i = 0; i < KyberParams.PARAMS_N; i++) {
            polyC[i] = (short) (polyA[i] - polyB[i]);
        }
        return polyC;
    }
}
