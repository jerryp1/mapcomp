package edu.alibaba.mpc4j.common.tool.crypto.kyber.kyber4j;

import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Indistinguishability under chosen plaintext attack (IND-CPA) helper class
 *
 * @author Steven K Fisher <swiftcryptollc@gmail.com>
 */
public class Indcpa {
    /**
     * Pseudo-random function to derive a deterministic array of random bytes
     * from the supplied secret key object and other parameters.
     * PRF伪随机函数
     *
     * @param key   输入的参数，将决定hash值
     * @param nonce 增加到Key后
     * @return 返回是根据参数生成的固定值
     */
    public static byte[] generatePrfByteArray(byte[] key, byte nonce, Hash hashFunction, Prg prgFunction) {
        byte[] newKey = new byte[key.length + 1];
        System.arraycopy(key, 0, newKey, 0, key.length);
        newKey[key.length] = nonce;
        return prgFunction.extendToBytes(hashFunction.digestToBytes(newKey));
    }

    /**
     * Runs rejection sampling on uniform random bytes to generate uniform
     * random integers modulo `Q`
     * 对均匀随机字节进行拒绝采样，生成模为'Q'的均匀随机整数
     *
     * @param uniformRandom 随机参数 short[] R 和 int I
     * @param buf           随机数
     * @param bufl          随机数的长度
     * @param l             最终生成的uniformI的长度上限
     *                      结果是uniformRandom 随机参数 short[] R 和 int I 被重新修订了。
     */
    public static void generateUniform(KyberUniformRandom uniformRandom, byte[] buf, int bufl, int l) {
        short[] uniformR = new short[KyberParams.POLY_BYTES];
        int d1;
        int d2;
        // Always start at 0
        int uniformI = 0;
        int j = 0;
        while ((uniformI < l) && ((j + KyberParams.MATH_THREE) <= bufl)) {
            d1 = (((buf[j] & 0xFF) | ((buf[j + 1] & 0xFF) << 8)) & 0xFFF);
            d2 = (((((buf[j + 1] & 0xFF)) >> 4) | ((buf[j + 2] & 0xFF) << 4)) & 0xFFF);
            j = j + 3;
            if (d1 < KyberParams.PARAMS_Q) {
                uniformR[uniformI] = (short) d1;
                uniformI++;
            }
            if (uniformI < l && d2 < KyberParams.PARAMS_Q) {
                uniformR[uniformI] = (short) d2;
                uniformI++;
            }
        }
        uniformRandom.setUniformI(uniformI);
        uniformRandom.setUniformR(uniformR);
    }

    /**
     * Generate a polynomial vector matrix from the given seed
     * 根据Seed生成多项式矩阵
     *
     * @param seed       生成种子
     * @param transposed 决定了i，j的先后顺序
     * @param paramsK    安全参数
     * @return 生成的矩阵
     */
    public static short[][][] generateMatrix(byte[] seed, boolean transposed, Hash hashFunction, Prg prgFunction, int paramsK) {
        short[][][] r = new short[paramsK][paramsK][KyberParams.POLY_BYTES];
        KyberUniformRandom uniformRandom = new KyberUniformRandom();
        for (int i = 0; i < paramsK; i++) {
            // 生成一个空向量
            r[i] = Poly.generateNewPolyVector(paramsK);
            for (int j = 0; j < paramsK; j++) {
                byte[] ij = new byte[2];
                if (transposed) {
                    ij[0] = (byte) i;
                    ij[1] = (byte) j;
                } else {
                    ij[0] = (byte) j;
                    ij[1] = (byte) i;
                }
                byte[] hashSeed = new byte[34];
                System.arraycopy(seed, 0, hashSeed, 0, KyberParams.SYM_BYTES);
                System.arraycopy(ij, 0, hashSeed, KyberParams.SYM_BYTES, 2);
                byte[] buf = prgFunction.extendToBytes(hashFunction.digestToBytes(hashSeed));
                // 将随机生成的504个byte输入，并计算系数是否满足小于Q,最长可以得到min（256，332）。（504）/3 * 2 = 332
                generateUniform(uniformRandom, Arrays.copyOfRange(buf, 0, 504), 504, KyberParams.PARAMS_N);
                int ui = uniformRandom.getUniformI();
                r[i][j] = uniformRandom.getUniformR();
                while (ui < KyberParams.PARAMS_N) {
                    // 如果写入的数不到256，那么说嘛写入的数大多大于Q，那么则用buf的后半部分进行额外的补充（672-504） = 168。
                    generateUniform(uniformRandom, Arrays.copyOfRange(buf, 504, 672), 168, KyberParams.PARAMS_N - ui);
                    int ctrn = uniformRandom.getUniformI();
                    short[] missing = uniformRandom.getUniformR();
                    // 只补充后面的部分，不会修改前面的部分。
                    if (KyberParams.PARAMS_N - ui >= 0) {
                        System.arraycopy(missing, 0, r[i][j], ui, KyberParams.PARAMS_N - ui);
                    }
                    ui = ui + ctrn;
                }
            }
        }
        return r;
    }

    /**
     * Pack the ciphertext into a byte array
     * 将密文（u,v）进行压缩。
     *
     * @param u       论文中的u
     * @param v       论文中的v（包含了明文的部分）
     * @param paramsK 安全系数K
     * @return 压缩后的密文转为了byte数组
     */
    public static byte[] packCiphertext(short[][] u, short[] v, int paramsK) {
        byte[] bCompress = Poly.compressPolyVector(u, paramsK);
        byte[] vCompress = Poly.compressPoly(v, paramsK);
        byte[] returnArray = new byte[bCompress.length + vCompress.length];
        System.arraycopy(bCompress, 0, returnArray, 0, bCompress.length);
        System.arraycopy(vCompress, 0, returnArray, bCompress.length, vCompress.length);
        return returnArray;
    }

    /**
     * Unpack the ciphertext from a byte array into a polynomial vector and
     * vector
     * 解包密文
     *
     * @param c       打包后的密文
     * @param paramsK 安全参数
     * @return 论文中的U和v
     */
    public static UnpackedCipherText unpackCiphertext(byte[] c, int paramsK) {
        UnpackedCipherText unpackedCipherText = new UnpackedCipherText();
        byte[] uc;
        byte[] vc;
        switch (paramsK) {
            case 2:
                uc = new byte[KyberParams.POLY_VECTOR_COMPRESSED_BYTES_512];
                break;
            case 3:
                uc = new byte[KyberParams.POLY_VECTOR_COMPRESSED_BYTES_768];
                break;
            default:
                uc = new byte[KyberParams.POLY_VECTOR_COMPRESSED_BYTES_1024];
        }
        System.arraycopy(c, 0, uc, 0, uc.length);
        vc = new byte[c.length - uc.length];
        System.arraycopy(c, uc.length, vc, 0, vc.length);
        unpackedCipherText.setU(Poly.decompressPolyVector(uc, paramsK));
        unpackedCipherText.setV(Poly.decompressPoly(vc, paramsK));

        return unpackedCipherText;
    }

    /**
     * @param m                  加密消息
     * @param publicKey          一部分的公钥（As+e）
     * @param publicKeyGenerator 公钥生成器
     * @param paramsK            安全等级
     * @param hashFunction       哈希函数
     * @param prgMatrixLength672 矩阵所需扩展函数
     * @param prgNoiseLength     噪声所需扩展函数
     * @return 加密后的密文
     */
    public static byte[] encrypt(byte[] m, short[][] publicKey, byte[] publicKeyGenerator, int paramsK, Hash hashFunction,
                                 Prg prgMatrixLength672, Prg prgNoiseLength, SecureRandom secureRandom) {
        byte[] coins = new byte[KyberParams.SYM_BYTES];
        secureRandom.nextBytes(coins);
        return encrypt(m, publicKey, publicKeyGenerator, paramsK, hashFunction, prgMatrixLength672, prgNoiseLength, coins);
    }

    /**
     * @param m                  加密消息
     * @param publicKey          一部分的公钥（As+e）
     * @param publicKeyGenerator 公钥生成器
     * @param paramsK            安全等级
     * @param hashFunction       哈希函数
     * @param prgMatrixLength672 矩阵所需扩展函数
     * @param prgNoiseLength     噪声所需扩展函数
     * @param coins              随机数种子
     * @return 加密后的密文
     */
    public static byte[] encrypt(byte[] m, short[][] publicKey, byte[] publicKeyGenerator, int paramsK, Hash hashFunction,
                                 Prg prgMatrixLength672, Prg prgNoiseLength, byte[] coins) {
        short[][] r = Poly.generateNewPolyVector(paramsK);
        short[][] ep = Poly.generateNewPolyVector(paramsK);
        short[][] bp = Poly.generateNewPolyVector(paramsK);
        // 将m转换为多项式
        short[] k = Poly.polyFromData(m);
        // 注意，这里的T/F，和KEY生成的时候是不一样的，计算的是转制后的A
        short[][][] at = Indcpa.generateMatrix(publicKeyGenerator, true, hashFunction, prgMatrixLength672, paramsK);
        // 生成的随机参数，是r和e1
        for (int i = 0; i < paramsK; i++) {
            r[i] = Poly.getNoisePoly(coins, (byte) (i), paramsK, hashFunction, prgNoiseLength);
            ep[i] = Poly.getNoisePoly(coins, (byte) (i + paramsK), 3, hashFunction, prgNoiseLength);
        }
        // 这个是e2
        short[] epp = Poly.getNoisePoly(coins, (byte) (paramsK * 2), 3, hashFunction, prgNoiseLength);
        // 将r转换到NTT域进行计算
        Poly.polyVectorNtt(r);
        Poly.polyVectorReduce(r);
        // 计算Ar
        for (int i = 0; i < paramsK; i++) {
            bp[i] = Poly.polyVectorPointWiseAccMont(at[i], r);
        }
        //（As+e）* r
        short[] v = Poly.polyVectorPointWiseAccMont(publicKey, r);
        // 取消INV域
        Poly.polyVectorInvNttMont(bp);
        // 取消INV域
        Poly.polyInvNttMont(v);
        // 计算Ar + e1
        Poly.polyVectorAdd(bp, ep);
        // 计算（As+e）* r + e_2 + m
        Poly.polyAdd(Poly.polyAdd(v, epp), k);
        // 不知道为什么（As+e）* r + e_2 + m不需要进行reduce。
        Poly.polyVectorReduce(bp);
        // 返回密文，pack的时候会执行压缩函数
        return Indcpa.packCiphertext(bp, Poly.polyReduce(v), paramsK);
    }

    /**
     * 解密函数
     *
     * @param packedCipherText 打包好的密文
     * @param privateKey       私钥
     * @param paramsK          安全等级
     * @return 解密后的明文
     */
    public static byte[] decrypt(byte[] packedCipherText, short[][] privateKey, int paramsK) {
        // 解压密文并获得U和v
        UnpackedCipherText unpackedCipherText = Indcpa.unpackCiphertext(packedCipherText, paramsK);
        short[][] u = unpackedCipherText.getU();
        short[] v = unpackedCipherText.getV();
        // 将U转为NTT域
        Poly.polyVectorNtt(u);
        // 执行乘法 计算 Us
        short[] mp = Poly.polyVectorPointWiseAccMont(privateKey, u);
        // 将乘积转回正常计算域
        Poly.polyInvNttMont(mp);
        // U - v
        mp = Poly.polySub(v, mp);
        Poly.polyReduce(mp);
        // 将结果返回成消息
        return Poly.polyToMsg(mp);
    }
}
