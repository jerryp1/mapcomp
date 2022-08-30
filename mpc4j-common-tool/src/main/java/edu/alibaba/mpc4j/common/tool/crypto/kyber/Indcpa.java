package edu.alibaba.mpc4j.common.tool.crypto.kyber;

import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;

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
     * @param l hash的长度
     * @param key 输入的参数，将决定hash值
     * @param nonce 增加到Key后
     * @return 返回是根据参数生成的固定值
     */
    public static byte[] generatePRFByteArray(int l, byte[] key, byte nonce) {
        Hash hashFunction = HashFactory.createInstance(HashFactory.HashType.BC_BLAKE_2B_160,16);
        Prg prgFunction = PrgFactory.createInstance(PrgFactory.PrgType.JDK_SECURE_RANDOM,l);
        //KeccakSponge xof = new Shake256();
        byte[] newKey = new byte[key.length + 1];
        System.arraycopy(key, 0, newKey, 0, key.length);
        newKey[key.length] = nonce;
        //xof.getAbsorbStream().write(newKey);
        //xof.getSqueezeStream().read(hash);
        return prgFunction.extendToBytes(hashFunction.digestToBytes(newKey));
    }

    /**
     * Runs rejection sampling on uniform random bytes to generate uniform
     * random integers modulo `Q`
     * 对均匀随机字节进行拒绝采样，生成模为'Q'的均匀随机整数
     * @param uniformRandom 随机参数 short[] R 和 int I
     * @param buf 随机数
     * @param bufl 随机数的长度
     * @param l
     * @return uniformRandom 随机参数 short[] R 和 int I 被重新修订了。
     */
    public static void generateUniform(KyberUniformRandom uniformRandom, byte[] buf, int bufl, int l) {
        short[] uniformR = new short[KyberParams.paramsPolyBytes];
        int d1;
        int d2;
        int uniformI = 0; // Always start at 0
        int j = 0;
        while ((uniformI < l) && ((j + 3) <= bufl)) {
            d1 = (int) (((((int) (buf[j] & 0xFF))) | (((int) (buf[j + 1] & 0xFF)) << 8)) & 0xFFF);
            d2 = (int) (((((int) (buf[j + 1] & 0xFF)) >> 4) | (((int) (buf[j + 2] & 0xFF)) << 4)) & 0xFFF);
            j = j + 3;
            if (d1 < (int) KyberParams.paramsQ) {
                uniformR[uniformI] = (short) d1;
                uniformI++;
            }
            if (uniformI < l && d2 < (int) KyberParams.paramsQ) {
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
     * @param seed 生成种子
     * @param transposed 决定了i，j的先后顺序
     * @param paramsK 安全参数
     * @return 生成的矩阵
     */
    public static short[][][] generateMatrix(byte[] seed, boolean transposed, int paramsK) {
        short[][][] r = new short[paramsK][paramsK][KyberParams.paramsPolyBytes];
        KyberUniformRandom uniformRandom = new KyberUniformRandom();
        //KeccakSponge xof = new Shake128();
        for (int i = 0; i < paramsK; i++) {
            //生成一个空向量
            r[i] = Poly.generateNewPolyVector(paramsK);
            for (int j = 0; j < paramsK; j++) {
                Hash hashFunction = HashFactory.createInstance(HashFactory.HashType.BC_BLAKE_2B_160,16);
                Prg prgFunction = PrgFactory.createInstance(PrgFactory.PrgType.JDK_SECURE_RANDOM,672);
                //xof.reset();
                //xof.getAbsorbStream().write(seed);
                byte[] ij = new byte[2];
                if (transposed) {
                    ij[0] = (byte) i;
                    ij[1] = (byte) j;
                } else {
                    ij[0] = (byte) j;
                    ij[1] = (byte) i;
                }
                byte[] hashSeed = new byte[34];
                System.arraycopy(seed,0,hashSeed,0,KyberParams.paramsSymBytes);
                System.arraycopy(ij,0,hashSeed,KyberParams.paramsSymBytes,2);
                byte[] buf = prgFunction.extendToBytes(hashFunction.digestToBytes(hashSeed));
                //将随机生成的504个byte输入，并计算系数是否满足小于Q,最长可以得到min（256，332）。（504）/3 * 2 = 332
                generateUniform(uniformRandom, Arrays.copyOfRange(buf, 0, 504), 504, KyberParams.paramsN);
                int ui = uniformRandom.getUniformI();
                r[i][j] = uniformRandom.getUniformR();
                while (ui < KyberParams.paramsN) {
                    //如果写入的数不到256，那么说嘛写入的数大多大于Q，那么则用buf的后半部分进行额外的补充（672-504） = 168。
                    generateUniform(uniformRandom, Arrays.copyOfRange(buf, 504, 672), 168, KyberParams.paramsN - ui);
                    int ctrn = uniformRandom.getUniformI();
                    short[] missing = uniformRandom.getUniformR();
                    for (int k = ui; k < KyberParams.paramsN; k++) {
                        //只补充后面的部分，不会修改前面的部分。
                        r[i][j][k] = missing[k - ui];
                    }
                    ui = ui + ctrn;
                }
            }
        }
        return r;
    }
    /**
     * Pack the public key with the given public key and seed into a polynomial vector
     * 将公钥以及生成参数（seed）放入byte
     * @param publicKey 公钥
     * @param seed 随机数生成种子
     * @param paramsK 安全系数
     * @return byte数组
     */
    public static byte[] packPublicKey(short[][] publicKey, byte[] seed, int paramsK) {
        byte[] initialArray = Poly.polyVectorToBytes(publicKey);
        byte[] packedPublicKey;
        switch (paramsK) {
            case 2:
                packedPublicKey = new byte[KyberParams.paramsIndcpaPublicKeyBytesK512];
                System.arraycopy(initialArray, 0, packedPublicKey, 0, initialArray.length);
                System.arraycopy(seed, 0, packedPublicKey, initialArray.length, seed.length);
                break;
            case 3:
                packedPublicKey = new byte[KyberParams.paramsIndcpaPublicKeyBytesK768];
                System.arraycopy(initialArray, 0, packedPublicKey, 0, initialArray.length);
                System.arraycopy(seed, 0, packedPublicKey, initialArray.length, seed.length);
                break;
            default:
                packedPublicKey = new byte[KyberParams.paramsIndcpaPublicKeyBytesK1024];
                System.arraycopy(initialArray, 0, packedPublicKey, 0, initialArray.length);
                System.arraycopy(seed, 0, packedPublicKey, initialArray.length, seed.length);
        }

        return packedPublicKey;
    }

    /**
     * Unpack the packed public key into the public key polynomial vector and
     * see
     * 将公钥从byte转为多项式
     * @param packedPublicKey 打包后的公钥
     * @param paramsK 安全参数
     * @return 解开后的公钥
     */
    public static UnpackedPublicKey unpackPublicKey(byte[] packedPublicKey, int paramsK) {
        UnpackedPublicKey unpackedKey = new UnpackedPublicKey();
        switch (paramsK) {
            case 2:
                unpackedKey.setPublicKeyPolyvec(Poly.polyVectorFromBytes(Arrays.copyOfRange(packedPublicKey, 0, KyberParams.paramsPolyvecBytesK512)));
                unpackedKey.setSeed(Arrays.copyOfRange(packedPublicKey, KyberParams.paramsPolyvecBytesK512, packedPublicKey.length));
                break;
            case 3:
                unpackedKey.setPublicKeyPolyvec(Poly.polyVectorFromBytes(Arrays.copyOfRange(packedPublicKey, 0, KyberParams.paramsPolyvecBytesK768)));
                unpackedKey.setSeed(Arrays.copyOfRange(packedPublicKey, KyberParams.paramsPolyvecBytesK768, packedPublicKey.length));
                break;
            default:
                unpackedKey.setPublicKeyPolyvec(Poly.polyVectorFromBytes(Arrays.copyOfRange(packedPublicKey, 0, KyberParams.paramsPolyvecBytesK1024)));
                unpackedKey.setSeed(Arrays.copyOfRange(packedPublicKey, KyberParams.paramsPolyvecBytesK1024, packedPublicKey.length));
        }
        return unpackedKey;
    }

    /**
     * Pack the private key into a byte array
     * 将私钥从多项式转为byte
     * @param privateKey 私钥
     * @param paramsK 安全参数
     * @return 打包后的私钥
     */
    public static byte[] packPrivateKey(short[][] privateKey, int paramsK) {
        byte[] packedPrivateKey = Poly.polyVectorToBytes(privateKey);
        return packedPrivateKey;
    }

    /**
     * Unpack the private key byte array into a polynomial vector
     * 将私钥从byte转为多项式
     * @param packedPrivateKey 打包后的私钥
     * @param paramsK 安全参数
     * @return 打包后的私钥
     */
    public static short[][] unpackPrivateKey(byte[] packedPrivateKey, int paramsK) {
        short[][] unpackedPrivateKey = Poly.polyVectorFromBytes(packedPrivateKey);
        return unpackedPrivateKey;
    }
    /**
     * Pack the ciphertext into a byte array
     * 将密文（u,v）进行压缩。
     * @param u 论文中的u
     * @param v 论文中的v（包含了明文的部分）
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
     * @param c 打包后的密文
     * @param paramsK 安全参数
     * @return 论文中的U和v
     */
    public static UnpackedCipherText unpackCiphertext(byte[] c, int paramsK) {
        UnpackedCipherText unpackedCipherText = new UnpackedCipherText();
        byte[] uc;
        byte[] vc;
        switch (paramsK) {
            case 2:
                uc = new byte[KyberParams.paramsPolyvecCompressedBytesK512];
                break;
            case 3:
                uc = new byte[KyberParams.paramsPolyvecCompressedBytesK768];
                break;
            default:
                uc = new byte[KyberParams.paramsPolyvecCompressedBytesK1024];
        }
        System.arraycopy(c, 0, uc, 0, uc.length);
        vc = new byte[c.length - uc.length];
        System.arraycopy(c, uc.length, vc, 0, vc.length);
        unpackedCipherText.setU(Poly.decompressPolyVector(uc, paramsK));
        unpackedCipherText.setV(Poly.decompressPoly(vc, paramsK));

        return unpackedCipherText;
    }
    /**
     * Generates public and private keys for the CPA-secure public-key
     * encryption scheme underlying Kyber.
     * @param paramsK 安全参数
     * @return 论文中的公钥（As+e,p）和私钥s
     */
    public static KyberPackedPKI generateKyberKeys(int paramsK) {
        //私钥s
        short[][] skpv = Poly.generateNewPolyVector(paramsK);
        //最后输出时是公钥 As+e
        short[][] pkpv = Poly.generateNewPolyVector(paramsK);
        short[][] e = Poly.generateNewPolyVector(paramsK);
        byte[] publicSeed = new byte[KyberParams.paramsSymBytes];
        byte[] noiseSeed = new byte[KyberParams.paramsSymBytes];
        Prg prgFunction = PrgFactory.createInstance(PrgFactory.PrgType.JDK_SECURE_RANDOM,64);
        //MessageDigest h = new SHA3_512();
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(publicSeed);
        //随机数被扩展至64位
        byte[] fullSeed = prgFunction.extendToBytes(publicSeed);
        //将随机数前32位赋给publicSeed，后32位赋给noiseSeed
        System.arraycopy(fullSeed, 0, publicSeed, 0, KyberParams.paramsSymBytes);
        System.arraycopy(fullSeed, KyberParams.paramsSymBytes, noiseSeed, 0, KyberParams.paramsSymBytes);
        //生成了公钥中的A
        short[][][] a = generateMatrix(publicSeed, false, paramsK);
        byte nonce = (byte) 0;
        //生成了私钥
        for (int i = 0; i < paramsK; i++) {
            skpv[i] = Poly.getNoisePoly(noiseSeed, nonce, paramsK);
            nonce = (byte) (nonce + (byte) 1);
        }
        //生成了噪声，没计算一步增加一步nonce
        for (int i = 0; i < paramsK; i++) {
            e[i] = Poly.getNoisePoly(noiseSeed, nonce, paramsK);
            nonce = (byte) (nonce + (byte) 1);
        }
        Poly.polyVectorNTT(skpv);
        Poly.polyVectorReduce(skpv);
        e = Poly.polyVectorNTT(e);
        //计算 As
        for (int i = 0; i < paramsK; i++) {
            short[] temp = Poly.polyVectorPointWiseAccMont(a[i], skpv);
            pkpv[i] = Poly.polyToMont(temp);
        }
        //计算 As+e
        Poly.polyVectorAdd(pkpv, e);
        pkpv = Poly.polyVectorReduce(pkpv);
        KyberPackedPKI packedPKI = new KyberPackedPKI();
        packedPKI.setPackedPrivateKey(packPrivateKey(skpv, paramsK));
        packedPKI.setPackedPublicKey(packPublicKey(pkpv, publicSeed, paramsK));
        return packedPKI;
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
    public static byte[] encrypt(byte[] m, byte[] publicKey, byte[] coins, int paramsK) {
        short[][] sp = Poly.generateNewPolyVector(paramsK);
        short[][] ep = Poly.generateNewPolyVector(paramsK);
        short[][] bp = Poly.generateNewPolyVector(paramsK);
        //将公钥（As+e）和seed一同放入UnpackedPublicKey
        UnpackedPublicKey unpackedPublicKey = unpackPublicKey(publicKey, paramsK);
        //将m转换为多项式
        short[] k = Poly.polyFromData(m);
        //根据seed生成多项式，不知道这里拷贝的意义是什么(seed并不会在函数中被修改，不知道为啥拷贝一下，这里和生成的时候采用的transposed是不同的)
        //这里相反的T or F 是不是因为转成byte以后的顺序相反了，后续研究一下哈哈哈
        short[][][] at = generateMatrix(unpackedPublicKey.getSeed(), true, paramsK);
        //生成的随机参数，是r和e1
        for (int i = 0; i < paramsK; i++) {
            sp[i] = Poly.getNoisePoly(coins, (byte) (i), paramsK);
            ep[i] = Poly.getNoisePoly(coins, (byte) (i + paramsK), 3);
        }
        //这个像是e2
        short[] epp = Poly.getNoisePoly(coins, (byte) (paramsK * 2), 3);
        //这个像是r转换到NTT域进行计算
        sp = Poly.polyVectorNTT(sp);
        sp = Poly.polyVectorReduce(sp);
        //Ar
        for (int i = 0; i < paramsK; i++) {
            bp[i] = Poly.polyVectorPointWiseAccMont(at[i], sp);
        }
        //（As+e）* r
        short[] v = Poly.polyVectorPointWiseAccMont(unpackedPublicKey.getPublicKeyPolyvec(), sp);
        //取消INV域
        bp = Poly.polyVectorInvNTTMont(bp);
        //取消INV域
        v = Poly.polyInvNTTMont(v);
        //Ar + e1
        bp = Poly.polyVectorAdd(bp, ep);
        // （As+e）* r + e_2 + m
        v = Poly.polyAdd(Poly.polyAdd(v, epp), k);
        //压缩
        bp = Poly.polyVectorReduce(bp);
        //返回密文，pack的时候会执行压缩函数
        return packCiphertext(bp, Poly.polyReduce(v), paramsK);
    }
    /**
     * Decrypt the given byte array using the Kyber public-key encryption scheme
     *
     * @param packedCipherText 压缩，打包后的密文
     * @param privateKey 私钥
     * @param paramsK 安全参数K
     * @return 消息m
     */
    public static byte[] decrypt(byte[] packedCipherText, byte[] privateKey, int paramsK) {
        //解压缩并获得U和v
        UnpackedCipherText unpackedCipherText = unpackCiphertext(packedCipherText, paramsK);
        short[][] U = unpackedCipherText.getU();
        short[] v = unpackedCipherText.getV();
        //获得私钥
        short[][] unpackedPrivateKey = unpackPrivateKey(privateKey, paramsK);
        //将U转为NTT域
        U = Poly.polyVectorNTT(U);
        // 执行乘法 计算 Us
        short[] mp = Poly.polyVectorPointWiseAccMont(unpackedPrivateKey, U);
        //将乘积转回正常计算域
        mp = Poly.polyInvNTTMont(mp);
        // U - v
        mp = Poly.polySub(v, mp);
        mp = Poly.polyReduce(mp);
        //将结果返回成消息
        return Poly.polyToMsg(mp);
    }



}
