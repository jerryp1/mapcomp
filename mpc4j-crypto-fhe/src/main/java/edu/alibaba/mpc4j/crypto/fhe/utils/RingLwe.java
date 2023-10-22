package edu.alibaba.mpc4j.crypto.fhe.utils;

import edu.alibaba.mpc4j.crypto.fhe.Ciphertext;
import edu.alibaba.mpc4j.crypto.fhe.context.Context;
import edu.alibaba.mpc4j.crypto.fhe.keys.PublicKey;
import edu.alibaba.mpc4j.crypto.fhe.keys.SecretKey;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.ntt.NttTables;
import edu.alibaba.mpc4j.crypto.fhe.ntt.NttTool;
import edu.alibaba.mpc4j.crypto.fhe.params.EncryptionParams;
import edu.alibaba.mpc4j.crypto.fhe.params.ParmsIdType;
import edu.alibaba.mpc4j.crypto.fhe.params.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.rand.ClippedNormalDistribution;
import edu.alibaba.mpc4j.crypto.fhe.rand.UniformRandomGenerator;
import edu.alibaba.mpc4j.crypto.fhe.rand.UniformRandomGeneratorFactory;
import edu.alibaba.mpc4j.crypto.fhe.rq.PolyArithmeticSmallMod;
import edu.alibaba.mpc4j.crypto.fhe.zq.Common;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintArithmeticSmallMod;

import java.util.Arrays;

/**
 * This class provides some operations under Ring LWE.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/rlwe.h
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/8/29
 */
public class RingLwe {

    /**
     * @param prng
     * @param parms
     * @param destination 一个多项式在 RNS 下的表示，长度为 k * N 的数组
     */
    public static void samplePolyNormal(UniformRandomGenerator prng, EncryptionParams parms, long[] destination) {

        Modulus[] coeffModulus = parms.getCoeffModulus();
        int coeffModulusSize = coeffModulus.length;
        int coeffCount = parms.getPolyModulusDegree();

        if (Common.areClose(GlobalVariables.NOISE_MAX_DEVIATION, 0.0)) {
            Arrays.fill(destination, 0, coeffCount * coeffModulusSize, 0);
            return;
        }

        ClippedNormalDistribution dist = new ClippedNormalDistribution(0, GlobalVariables.NOISE_STANDARD_DEVIATION, GlobalVariables.NOISE_MAX_DEVIATION);

        for (int j = 0; j < coeffCount; j++) {
            // 每一列生成一个相同的随机数
            // 此处的 long 表示 int64
            long noise = (long) dist.sample(prng);
            // rand == 0 ---> (-1) --->  static_cast<uint64_t>(-1) = uint64_t::max, 即 0xFFFFFFF, 概率 1/3
            // rand != 0 ---> 0 -----> 0 ---> 0 ， 概率 2/3
            long flag = noise < 0 ? -1 : 0;

            for (int i = 0; i < coeffModulusSize; i++) {
                destination[j + i * coeffCount] = noise + (flag & coeffModulus[i].getValue());
            }
        }
    }

    /**
     * @param prng
     * @param parms
     * @param destination 表示多个poly 在 RNS 下的表示，可认为是 size * k * N 长度的数组
     * @param startIndex  指明当前处理的是 哪一个 poly
     */
    public static void samplePolyNormal(UniformRandomGenerator prng, EncryptionParams parms, long[] destination, int startIndex) {

        Modulus[] coeffModulus = parms.getCoeffModulus();
        int coeffModulusSize = coeffModulus.length;
        int coeffCount = parms.getPolyModulusDegree();

        if (Common.areClose(GlobalVariables.NOISE_MAX_DEVIATION, 0.0)) {
            Arrays.fill(destination, startIndex, startIndex + coeffCount * coeffModulusSize, 0);
            return;
        }

        ClippedNormalDistribution dist = new ClippedNormalDistribution(0, GlobalVariables.NOISE_STANDARD_DEVIATION, GlobalVariables.NOISE_MAX_DEVIATION);

        for (int j = 0; j < coeffCount; j++) {
            // 每一列生成一个相同的随机数
            // 此处的 long 表示 int64
            long noise = (long) dist.sample(prng);
            // rand == 0 ---> (-1) --->  static_cast<uint64_t>(-1) = uint64_t::max, 即 0xFFFFFFF, 概率 1/3
            // rand != 0 ---> 0 -----> 0 ---> 0 ， 概率 2/3
            long flag = noise < 0 ? -1 : 0;

            for (int i = 0; i < coeffModulusSize; i++) {
                destination[startIndex + j + i * coeffCount] = noise + (flag & coeffModulus[i].getValue());
            }
        }
    }


    /**
     * @param prng
     * @param parms
     * @param destination 此时的 long[] 表示 1 个多项式在 RNS 下被分解为 k 个多项式
     */
    public static void samplePolyUniform(
            UniformRandomGenerator prng, EncryptionParams parms, long[] destination
    ) {

        Modulus[] coeffModulus = parms.getCoeffModulus();
        int coeffModulusSize = coeffModulus.length;
        int coeffCount = parms.getPolyModulusDegree();
        // need mul safe?
        int destByteCount = Common.mulSafe(coeffModulusSize, coeffCount, false);

        long maxRandom = 0xFFFFFFFFFFFFFFFFL;

        // Fill the destination buffer with fresh randomness
        prng.generate(destByteCount, destination);

        // 逐步处理每一个多项式
        // RNS 下，1个Poly 被 分解为 k 个
        // 均匀分布是按行采样，即每一行（每一个子多项式） 的系数是满足均匀分布的
        for (int j = 0; j < coeffModulusSize; j++) {
            Modulus modulus = coeffModulus[j];
            long maxMultiple = maxRandom - UintArithmeticSmallMod.barrettReduce64(maxRandom, modulus) - 1;

            // 遍历 每一个 coeff
            for (int i = 0; i < coeffCount; i++) {
                // This ensures uniform distribution
                while (Long.compareUnsigned(destination[j * coeffCount + i], maxMultiple) >= 0) {
                    destination[j * coeffCount + i] = prng.secureRandom.nextLong();
                }
                // 修改原数组
                destination[j * coeffCount + i] = UintArithmeticSmallMod.barrettReduce64(destination[j * coeffCount + i], modulus);
            }
        }
    }


    /**
     * @param prng
     * @param parms
     * @param destination 此时表示 多个多项式 在 RNS 下的表示，数组长度为  size * k * N
     * @param startIndex  标记当前处理的是哪一个多项式 [0, size)
     */
    public static void samplePolyUniform(
            UniformRandomGenerator prng, EncryptionParams parms, long[] destination, int startIndex
    ) {

        Modulus[] coeffModulus = parms.getCoeffModulus();
        int coeffModulusSize = coeffModulus.length;
        int coeffCount = parms.getPolyModulusDegree();
        // need mul safe?
        int destByteCount = Common.mulSafe(coeffModulusSize, coeffCount, false, Constants.BYTES_PER_UINT64);

        long maxRandom = 0xFFFFFFFFFFFFFFFFL;

        // Fill the destination buffer with fresh randomness
//        System.out.println("destination: " + destination.length);
//        System.out.println("k * N: " + coeffModulusSize + " * " + coeffCount + ": " + coeffModulusSize * coeffCount);
        prng.generate(destByteCount, destination, startIndex);

        // 逐步处理每一个多项式
        // RNS 下，1个Poly 被 分解为 k 个
        for (int j = 0; j < coeffModulusSize; j++) {
            Modulus modulus = coeffModulus[j];

            long maxMultiple = maxRandom - UintArithmeticSmallMod.barrettReduce64(maxRandom, modulus) - 1;

            // 遍历 每一个 coeff
            for (int i = 0; i < coeffCount; i++) {
                // This ensures uniform distribution
                while (Long.compareUnsigned(destination[startIndex + j * coeffCount + i], maxMultiple) >= 0) {
                    // rand 可能为负数，所以要用 compareUnsigned
                    destination[startIndex + j * coeffCount + i] = prng.secureRandom.nextLong();
                }
                // 修改原数组
                destination[startIndex + j * coeffCount + i] = UintArithmeticSmallMod.barrettReduce64(destination[startIndex + j * coeffCount + i], modulus);
            }
        }
    }


    private static int cbd(UniformRandomGenerator prng) {
        byte[] x = new byte[6];
        prng.generate(6, x);
        x[2] &= 0x1F; // 0001 1111
        x[5] &= 0x1F;
        return Common.hammingWeight(x[0]) + Common.hammingWeight(x[1]) + Common.hammingWeight(x[2])
                - Common.hammingWeight(x[3]) - Common.hammingWeight(x[4]) - Common.hammingWeight(x[5]);
    }

    /**
     * Generate a polynomial from a centered binomial distribution and store in RNS representation.
     *
     * @param prng
     * @param parms
     * @param destination single poly in RNS, length is k * N
     */
    public static void samplePolyCbd(UniformRandomGenerator prng, EncryptionParams parms, long[] destination) {

        Modulus[] coeffModulus = parms.getCoeffModulus();
        int coeffModulusSize = coeffModulus.length;
        int coeffCount = parms.getPolyModulusDegree();

        if (Common.areClose(GlobalVariables.NOISE_MAX_DEVIATION, 0.0)) {
            Arrays.fill(destination, 0, coeffCount * coeffModulusSize, 0);
            return;
        }

        if (!Common.areClose(GlobalVariables.NOISE_STANDARD_DEVIATION, 3.2)) {
            throw new IllegalArgumentException("centered binomial distribution only supports standard deviation 3.2; use rounded Gaussian instead");
        }

        // 逐列采样
        for (int j = 0; j < coeffCount; j++) {
            // 每一列生成一个相同的随机数, 这里的 int 是 i32
            int noise = cbd(prng);
            // flag 是 u64
            long flag = (noise < 0) ? -1 : 0;
            for (int i = 0; i < coeffModulusSize; i++) {
                destination[j + i * coeffCount] = (long) noise + (flag & coeffModulus[i].getValue());
            }
        }
    }


    /**
     * Generate a polynomial from a centered binomial distribution and store in RNS representation.
     *
     * @param prng
     * @param parms
     * @param destination multi poly in RNS, length is size * k * N
     * @param startIndex  startIndex of a singel poly in destination
     */
    public static void samplePolyCbd(UniformRandomGenerator prng, EncryptionParams parms, long[] destination, int startIndex) {

        Modulus[] coeffModulus = parms.getCoeffModulus();
        int coeffModulusSize = coeffModulus.length;
        int coeffCount = parms.getPolyModulusDegree();

        if (Common.areClose(GlobalVariables.NOISE_MAX_DEVIATION, 0.0)) {
            Arrays.fill(destination, 0, coeffCount * coeffModulusSize, 0);
            return;
        }

        if (!Common.areClose(GlobalVariables.NOISE_STANDARD_DEVIATION, 3.2)) {
            throw new IllegalArgumentException("centered binomial distribution only supports standard deviation 3.2; use rounded Gaussian instead");
        }

        // 逐列采样
        for (int j = 0; j < coeffCount; j++) {
            // 每一列生成一个相同的随机数, 这里的 int 是 i32
            int noise = cbd(prng);
            // flag 是 u64
            long flag = (noise < 0) ? -1 : 0;
            for (int i = 0; i < coeffModulusSize; i++) {
                destination[startIndex + j + i * coeffCount] = (long) noise + (flag & coeffModulus[i].getValue());
            }
        }
    }


    /**
     * @param prng
     * @param parms
     * @param destination single poly in RNS, length is k * N
     */
    public static void samplePolyTernary(UniformRandomGenerator prng, EncryptionParams parms, long[] destination) {

        Modulus[] coeffModulus = parms.getCoeffModulus();
        int coeffModulusSize = coeffModulus.length;
        int coeffCount = parms.getPolyModulusDegree();
        // 用连续的 1-D Array 表示 k * N 这个二维数组
        // [0, N) --> [N, 2N ) ---> [(k-1)N, kN) 就是这样一个结构
        // 每一个小区间表示在 qi 下的一个 N-1 阶 多项式
        assert destination.length >= coeffModulusSize * coeffCount;

        // [0, 2] uniformly random numer
        int min = 0;
        int max = 2;

        for (int j = 0; j < coeffCount; j++) {
            // 每一列生成一个相同的随机数
            long rand = prng.secureRandom.nextInt(max - min + 1) + min;
            // rand == 0 ---> (-1) --->  static_cast<uint64_t>(-1) = uint64_t::max, 即 0xFFFFFFF, 概率 1/3
            // rand != 0 ---> 0 -----> 0 ---> 0 ， 概率 2/3
            long flag = rand == 0 ? -1 : 0;

            for (int i = 0; i < coeffModulusSize; i++) {
                destination[j + i * coeffCount] = rand + (flag & coeffModulus[i].getValue()) - 1;
            }
            // des[J + N] = rand  + (0) -1  ,概率 2/3, 其中 rand  != 0, rand 为 1 或 2，最终结果为 0 or 1, 概率 各 1/3
            // des[J + N] = rand  + (qi) -1 , 概率 1/3， 其中 rand = 0, 则最终值是 qi -1 ,正好等于 mod qi 下的 -1
            // 所以最终就得到了 [-1, 0, 1] 的一个均匀分布
        }

    }

    /**
     * @param prng
     * @param parms
     * @param destination multi poly in RNS, length is size * k * N
     * @param startIndex  startIndex of singel poly in destination
     */
    public static void samplePolyTernary(UniformRandomGenerator prng, EncryptionParams parms, long[] destination, int startIndex) {

        Modulus[] coeffModulus = parms.getCoeffModulus();
        int coeffModulusSize = coeffModulus.length;
        int coeffCount = parms.getPolyModulusDegree();
        // 用连续的 1-D Array 表示 k * N 这个二维数组
        // [0, N) --> [N, 2N ) ---> [(k-1)N, kN) 就是这样一个结构
        // 每一个小区间表示在 qi 下的一个 N-1 阶 多项式
        assert destination.length >= coeffModulusSize * coeffCount;

        // [0, 2] uniformly random numer
        int min = 0;
        int max = 2;

        for (int j = 0; j < coeffCount; j++) {
            // 每一列生成一个相同的随机数
            long rand = prng.secureRandom.nextInt(max - min + 1) + min;
            // rand == 0 ---> (-1) --->  static_cast<uint64_t>(-1) = uint64_t::max, 即 0xFFFFFFF, 概率 1/3
            // rand != 0 ---> 0 -----> 0 ---> 0 ， 概率 2/3
            long flag = rand == 0 ? -1 : 0;

            for (int i = 0; i < coeffModulusSize; i++) {
                destination[startIndex + j + i * coeffCount] = rand + (flag & coeffModulus[i].getValue()) - 1;
            }
            // des[J + N] = rand  + (0) -1  ,概率 2/3, 其中 rand  != 0, rand 为 1 或 2，最终结果为 0 or 1, 概率 各 1/3
            // des[J + N] = rand  + (qi) -1 , 概率 1/3， 其中 rand = 0, 则最终值是 qi -1 ,正好等于 mod qi 下的 -1
            // 所以最终就得到了 [-1, 0, 1] 的一个均匀分布
        }

    }


    public static void encryptZeroAsymmetric(PublicKey publicKey, Context context, ParmsIdType parmsId, boolean isNttForm, Ciphertext destination) {


        assert ValueChecker.isValidFor(publicKey, context);

        Context.ContextData contextData = context.getContextData(parmsId);
        EncryptionParams parms = contextData.getParms();
        Modulus[] coeffModulus = parms.getCoeffModulus();
        Modulus plainModulus = parms.getPlainModulus();
        int coeffModulusSize = coeffModulus.length;
        int coeffCount = parms.getPolyModulusDegree();
        NttTables[] nttTables = contextData.getSmallNttTables();
        int encryptedSize = publicKey.data().getSize();
        SchemeType type = parms.getScheme();

        // Make destination have right size and parms_id
        // Ciphertext (c_0,c_1, ...)
        destination.resize(context, parmsId, encryptedSize);
        destination.setIsNttForm(isNttForm);
        destination.setScale(1.0);
        destination.setCorrectionFactor(1);

        // c[j] = public_key[j] * u + e[j] in BFV/CKKS = public_key[j] * u + p * e[j] in BGV
        // where e[j] <-- chi, u <-- R_3

        // Create a PRNG; u and the noise/error share the same PRNG
        UniformRandomGenerator prng = parms.getRandomGeneratorFactory().create();

        // Generate u <-- R_3
        long[] u = new long[coeffCount * coeffModulusSize];
        samplePolyTernary(prng, parms, u);

//        System.out.println("u length: " + u.length);
//        StringBuilder sb = new StringBuilder();
//        sb.append("u:\n {");
//        for (int i = 0; i < u.length; i++) {
//            sb.append(u[i]);
//            if (i != u.length - 1) {
//                sb.append(", ");
//            }
//        }
//        sb.append("}");
//        System.out.println(sb);
//
//        sb = new StringBuilder();
//        sb.append("u:\n {");
//        for (int i = 0; i < u.length; i++) {
//            sb.append(u[i]);
//            sb.append("L");
//            if (i != u.length - 1) {
//                sb.append(", ");
//            }
//        }
//        sb.append("}");
//        System.out.println(sb);

//         覆盖固定 u
//        u = new long[]  {1099511600896L, 0L, 1099511600896L, 1099511600896L, 1099511600896L, 1L, 0L, 1099511600896L, 0L, 1099511600896L, 1L, 1099511600896L, 0L, 0L, 1L, 1L, 1L, 1099511600896L, 0L, 1L, 1L, 1099511600896L, 0L, 1099511600896L, 1099511600896L, 1099511600896L, 1L, 0L, 0L, 1L, 1099511600896L, 1L, 0L, 1L, 1099511600896L, 1L, 0L, 0L, 1099511600896L, 0L, 1099511600896L, 0L, 1099511600896L, 1099511600896L, 1L, 0L, 1L, 1099511600896L, 1099511600896L, 1L, 1L, 0L, 1L, 1099511600896L, 0L, 1L, 1099511600896L, 1L, 0L, 1099511600896L, 1099511600896L, 1L, 1099511600896L, 0L, 1L, 1L, 1099511600896L, 1099511600896L, 1L, 1099511600896L, 1099511600896L, 1099511600896L, 1L, 1L, 0L, 0L, 1099511600896L, 1L, 1L, 1099511600896L, 1099511600896L, 1L, 1099511600896L, 1099511600896L, 0L, 1L, 1L, 0L, 0L, 1L, 0L, 1099511600896L, 1L, 1L, 0L, 1099511600896L, 1099511600896L, 1099511600896L, 0L, 0L, 1099511600896L, 0L, 1L, 1L, 1099511600896L, 1099511600896L, 0L, 1099511600896L, 0L, 1L, 0L, 1099511600896L, 1L, 1099511600896L, 1099511600896L, 0L, 1L, 1L, 1L, 0L, 1099511600896L, 1L, 1L, 0L, 1L, 0L, 1L, 1099511600896L, 1099511603712L, 0L, 1099511603712L, 1099511603712L, 1099511603712L, 1L, 0L, 1099511603712L, 0L, 1099511603712L, 1L, 1099511603712L, 0L, 0L, 1L, 1L, 1L, 1099511603712L, 0L, 1L, 1L, 1099511603712L, 0L, 1099511603712L, 1099511603712L, 1099511603712L, 1L, 0L, 0L, 1L, 1099511603712L, 1L, 0L, 1L, 1099511603712L, 1L, 0L, 0L, 1099511603712L, 0L, 1099511603712L, 0L, 1099511603712L, 1099511603712L, 1L, 0L, 1L, 1099511603712L, 1099511603712L, 1L, 1L, 0L, 1L, 1099511603712L, 0L, 1L, 1099511603712L, 1L, 0L, 1099511603712L, 1099511603712L, 1L, 1099511603712L, 0L, 1L, 1L, 1099511603712L, 1099511603712L, 1L, 1099511603712L, 1099511603712L, 1099511603712L, 1L, 1L, 0L, 0L, 1099511603712L, 1L, 1L, 1099511603712L, 1099511603712L, 1L, 1099511603712L, 1099511603712L, 0L, 1L, 1L, 0L, 0L, 1L, 0L, 1099511603712L, 1L, 1L, 0L, 1099511603712L, 1099511603712L, 1099511603712L, 0L, 0L, 1099511603712L, 0L, 1L, 1L, 1099511603712L, 1099511603712L, 0L, 1099511603712L, 0L, 1L, 0L, 1099511603712L, 1L, 1099511603712L, 1099511603712L, 0L, 1L, 1L, 1L, 0L, 1099511603712L, 1L, 1L, 0L, 1L, 0L, 1L, 1099511603712L, 1099511607040L, 0L, 1099511607040L, 1099511607040L, 1099511607040L, 1L, 0L, 1099511607040L, 0L, 1099511607040L, 1L, 1099511607040L, 0L, 0L, 1L, 1L, 1L, 1099511607040L, 0L, 1L, 1L, 1099511607040L, 0L, 1099511607040L, 1099511607040L, 1099511607040L, 1L, 0L, 0L, 1L, 1099511607040L, 1L, 0L, 1L, 1099511607040L, 1L, 0L, 0L, 1099511607040L, 0L, 1099511607040L, 0L, 1099511607040L, 1099511607040L, 1L, 0L, 1L, 1099511607040L, 1099511607040L, 1L, 1L, 0L, 1L, 1099511607040L, 0L, 1L, 1099511607040L, 1L, 0L, 1099511607040L, 1099511607040L, 1L, 1099511607040L, 0L, 1L, 1L, 1099511607040L, 1099511607040L, 1L, 1099511607040L, 1099511607040L, 1099511607040L, 1L, 1L, 0L, 0L, 1099511607040L, 1L, 1L, 1099511607040L, 1099511607040L, 1L, 1099511607040L, 1099511607040L, 0L, 1L, 1L, 0L, 0L, 1L, 0L, 1099511607040L, 1L, 1L, 0L, 1099511607040L, 1099511607040L, 1099511607040L, 0L, 0L, 1099511607040L, 0L, 1L, 1L, 1099511607040L, 1099511607040L, 0L, 1099511607040L, 0L, 1L, 0L, 1099511607040L, 1L, 1099511607040L, 1099511607040L, 0L, 1L, 1L, 1L, 0L, 1099511607040L, 1L, 1L, 0L, 1L, 0L, 1L, 1099511607040L, 1099511619840L, 0L, 1099511619840L, 1099511619840L, 1099511619840L, 1L, 0L, 1099511619840L, 0L, 1099511619840L, 1L, 1099511619840L, 0L, 0L, 1L, 1L, 1L, 1099511619840L, 0L, 1L, 1L, 1099511619840L, 0L, 1099511619840L, 1099511619840L, 1099511619840L, 1L, 0L, 0L, 1L, 1099511619840L, 1L, 0L, 1L, 1099511619840L, 1L, 0L, 0L, 1099511619840L, 0L, 1099511619840L, 0L, 1099511619840L, 1099511619840L, 1L, 0L, 1L, 1099511619840L, 1099511619840L, 1L, 1L, 0L, 1L, 1099511619840L, 0L, 1L, 1099511619840L, 1L, 0L, 1099511619840L, 1099511619840L, 1L, 1099511619840L, 0L, 1L, 1L, 1099511619840L, 1099511619840L, 1L, 1099511619840L, 1099511619840L, 1099511619840L, 1L, 1L, 0L, 0L, 1099511619840L, 1L, 1L, 1099511619840L, 1099511619840L, 1L, 1099511619840L, 1099511619840L, 0L, 1L, 1L, 0L, 0L, 1L, 0L, 1099511619840L, 1L, 1L, 0L, 1099511619840L, 1099511619840L, 1099511619840L, 0L, 0L, 1099511619840L, 0L, 1L, 1L, 1099511619840L, 1099511619840L, 0L, 1099511619840L, 0L, 1L, 0L, 1099511619840L, 1L, 1099511619840L, 1099511619840L, 0L, 1L, 1L, 1L, 0L, 1099511619840L, 1L, 1L, 0L, 1L, 0L, 1L, 1099511619840L}
//        ;

//        u = FileUtils.readDataFromFile("/Users/qixian/mpc4j-ali/qixian-108/mpc4j/mpc4j-crypto-fhe/src/main/java/edu/alibaba/mpc4j/crypto/fhe/u.txt");


//        System.out.println("u:\n " + Arrays.toString(u));

        // c[j] = u * public_key[j]
        for (int i = 0; i < coeffModulusSize; i++) {
            // u 是 RnsIter, 这里是对 CoeffIter 操作，RnsIter + startIndex ---> CoeffIter
            NttTool.nttNegAcyclicHarvey(u, i * coeffCount, nttTables[i]);
            // j 是对 密文多项式的索引
            for (int j = 0; j < encryptedSize; j++) {
                // 注意这里是对 CoeffIter 操作，注意起点的计算
                PolyArithmeticSmallMod.dyadicProductCoeffModCoeffIter(
                        u,
                        i * coeffCount,
                        publicKey.data().getData(), // 密文，PolyIter
                        publicKey.data().indexAt(j) + i * coeffCount, // RnsIter + startIndx = CoeffIter
                        coeffCount,
                        coeffModulus[i],
                        destination.indexAt(j) + i * coeffCount,
                        destination.getData()
                );
                // Addition with e_0, e_1 is in non-NTT form
                if (!isNttForm) {
                    NttTool.inverseNttNegAcyclicHarvey(destination.getData(), destination.indexAt(j) + i * coeffCount, nttTables[i]);
                }
            }
        }
        // Generate e_j <-- chi
        // c[j] = public_key[j] * u + e[j] in BFV/CKKS, = public_key[j] * u + p * e[j] in BGV,

        for (int j = 0; j < encryptedSize; j++) {
            // 采样 noise, 默认使用cbd分布
            // todo: noise 采样方式可配置
            samplePolyCbd(prng, parms, u);
//             噪音置 0
//            Arrays.fill(u, 0);
            // 把 u 视为 RnsIter
            if (type == SchemeType.BGV) {
                //todo: implement BGV
                throw new IllegalArgumentException("now  cannot support BGV");
            } else { // BFV & CKKS
                if (isNttForm) {
                    // 注意函数签名, 直接处理 u 这个 RnsIter
                    NttTool.nttNegAcyclicHarveyRnsIter(u, coeffCount, coeffModulusSize, nttTables);
                }
            }
            //开始完成加法
            // 全部都处理为 CoeffIter 这个粒度的运算，不管输入的数组是表示 RnsIter/PolyIter, 每次都只处理一个 CoeffIter
            // 那么核心的就是 起点的计算 + coeffCount
            // 这里我们是想计算 RnsIter, 由于我们单次运算的粒度是 CoeffIter, 所以这里需要一个循环，k次，即 RnsBaseSize
            // todo: 需要设计一个 统一、高效的 基于 long[] 的 CoeffIter/RnsIter/PolyIter 的处理方式
            for (int i = 0; i < coeffModulusSize; i++) {
                PolyArithmeticSmallMod.addPolyCoeffMod(
                        u,
                        i * coeffCount,
                        destination.getData(), // Ciphertext 有多个 poly（polyIter）, destination.indexAt(j) 定位到单个poly（RnsIter） + i * coeffCount 定位到 CoffIter
                        destination.indexAt(j) + i * coeffCount,
                        coeffCount,
                        coeffModulus[i],
                        destination.indexAt(j) + i * coeffCount,
                        destination.getData()
                );
            }
        }
    }


    public static void encryptZeroSymmetric(
            SecretKey secretKey,
            Context context,
            ParmsIdType parmsId,
            Boolean isNttForm,
            Boolean saveSeed,
            Ciphertext destination) {

        assert ValueChecker.isValidFor(secretKey, context);

        Context.ContextData contextData = context.getContextData(parmsId);
        EncryptionParams parms = contextData.getParms();
        Modulus[] coeffModulus = parms.getCoeffModulus();
        Modulus plainModulus = parms.getPlainModulus();
        int coeffModulusSize = coeffModulus.length;
        int coeffCount = parms.getPolyModulusDegree();
        NttTables[] nttTables = contextData.getSmallNttTables();
        int encryptedSize = 2;
        SchemeType type = parms.getScheme();

        // If a polynomial is too small to store UniformRandomGeneratorInfo,
        // it is best to just disable save_seed. Note that the size needed is
        // the size of UniformRandomGeneratorInfo plus one (uint64_t) because
        // of an indicator word that indicates a seeded ciphertext.
        // todo: need mulSafe?
        int polyUint64Count = Common.mulSafe(coeffCount, coeffModulusSize, false);


        // 暂时不考虑 saveSeed 的相关实现
//        size_t prng_info_byte_count =
//                static_cast<size_t>(UniformRandomGeneratorInfo::SaveSize(compr_mode_type::none));
//        size_t prng_info_uint64_count =
//                divide_round_up(prng_info_byte_count, static_cast<size_t>(bytes_per_uint64));
//        if (save_seed && poly_uint64_count < prng_info_uint64_count + 1)
//        {
//            save_seed = false;
//        }
        // Ciphertext resize
        destination.resize(context, parmsId, encryptedSize);
        destination.setIsNttForm(isNttForm);
        destination.setScale(1.0);
        destination.setCorrectionFactor(1);

        // Create an instance of a random number generator. We use this for sampling
        // a seed for a second PRNG used for sampling u (the seed can be public
        // information. This PRNG is also used for sampling the noise/error below.
        UniformRandomGenerator bootstrapPrng = parms.getRandomGeneratorFactory().create();
        // Sample a public seed for generating uniform randomness
        long[] publicPrngSeed = new long[UniformRandomGeneratorFactory.PRNG_SEED_UINT64_COUNT];
        bootstrapPrng.generate(UniformRandomGeneratorFactory.PRNG_SEED_BYTE_COUNT, publicPrngSeed);

        // Set up a new default PRNG for expanding u from the seed sampled above
        UniformRandomGenerator ciphertextPrng = UniformRandomGeneratorFactory.defaultFactory().create(publicPrngSeed);

        // Generate ciphertext: (c[0], c[1]) = ([-(as+ e)]_q, a) in BFV/CKKS
        // Generate ciphertext: (c[0], c[1]) = ([-(as+pe)]_q, a) in BGV
        int c0StartIndex = 0;
        int c1StartIndex = destination.indexAt(1);

        // Sample a uniformly at random
        // c1 就是随机数 a
        if (isNttForm || !saveSeed) {
            // Sample the NTT form directly
            samplePolyUniform(ciphertextPrng, parms, destination.getData(), c1StartIndex);

//            System.out.println("destination c1: \n" + Arrays.toString(
//                    Arrays.copyOfRange(destination.getData(), c1StartIndex, destination.getData().length)
//            ));

//            StringBuilder sb = new StringBuilder();
//            sb.append("destination c1:\n {");
//            for (int i = c1StartIndex; i < destination.getData().length; i++) {
//                sb.append(destination.getData()[i]);
//                sb.append("L");
//                if (i != destination.getData().length - 1) {
//                    sb.append(", ");
//                }
//            }
//            sb.append("}");
//            System.out.println(sb);


            // 固定 c1
//            long[] fixed = new long[]{1002238238463L, 471594953260L, 559148573876L, 585757011248L, 22911478458L, 85643639255L, 913655075473L, 868712486992L, 1068013220567L, 574575458065L, 382814142369L, 745545385230L, 797228212316L, 820339240202L, 323432942282L, 506373657548L, 583872600753L, 32432268203L, 255521896950L, 815822510506L, 1044551411155L, 1029535034300L, 248363993720L, 921773314637L, 923043166785L, 729040070057L, 40328539620L, 234659635908L, 940173398645L, 268650270365L, 400927964649L, 899734358259L, 339312226668L, 487406253312L, 863512957140L, 616754912197L, 349771603150L, 578997598138L, 872723591724L, 37002123495L, 548734077137L, 447727715093L, 75090965822L, 807821615898L, 862805102490L, 857813524990L, 408901069156L, 83216676152L, 788974279059L, 884328842751L, 1001879090012L, 649499657463L, 1056383718191L, 840206183486L, 428130729323L, 507432395272L, 206877401164L, 602666089049L, 461599480516L, 1094838353742L, 505903539016L, 613626607830L, 123411688875L, 942537186469L, 942962311760L, 969314544492L, 411295154045L, 541302127334L, 580190562479L, 211328187237L, 705993558191L, 542777360273L, 51093812427L, 1042672169812L, 1069570792067L, 792023648287L, 386457233860L, 696355080916L, 1011217280588L, 894941929791L, 248151094171L, 423725020731L, 796391987787L, 916935269039L, 826580740776L, 142117000778L, 847765455814L, 970809312589L, 78293326217L, 1052191651818L, 360279242263L, 840484033415L, 791079333742L, 649542735047L, 323301988378L, 438942947910L, 178931720048L, 786261249612L, 237727830324L, 438302941555L, 862898033468L, 161723379605L, 248795759728L, 490330087375L, 899705152108L, 909895932325L, 1015670428985L, 644950636895L, 284229974513L, 257924524015L, 378615681313L, 925736968122L, 885705485180L, 424474154480L, 256063327316L, 236418526959L, 344175703966L, 883061284378L, 618743875439L, 1084554758080L, 712566258999L, 806874024363L, 263919950179L, 873917627913L, 370280428157L, 291930204584L, 691259722855L, 420956260208L, 421650074599L, 521491774040L, 1053938690871L, 192318417320L, 268566700954L, 382391208021L, 526744185111L, 371169803649L, 673833562964L, 920447306139L, 326910293895L, 850733901006L, 639494859677L, 1064862692071L, 617219730626L, 628844501326L, 190718956823L, 828768025430L, 848471366755L, 868341758438L, 874445887191L, 757179573375L, 574046744059L, 561915794053L, 694878567385L, 590748931901L, 259834506906L, 221054990805L, 399831160307L, 694252863525L, 416755857563L, 456624473422L, 1008203845771L, 691146333368L, 695619094141L, 184753692348L, 64131362623L, 60337298168L, 720588809524L, 463522298686L, 980273937658L, 352813194230L, 468639020487L, 696432466944L, 463111611701L, 899838191234L, 885230502270L, 746484476090L, 984175665812L, 279673780156L, 139043562839L, 255001606851L, 318652199467L, 934470859980L, 903406371676L, 459936398688L, 709923002709L, 700793799003L, 843615404682L, 929214477736L, 540776943643L, 363279140918L, 940557576198L, 61327042767L, 353545823846L, 524948552650L, 277336356847L, 1028641837131L, 665678234358L, 50229279457L, 458387311126L, 732908970988L, 468334168268L, 179739119716L, 321254094417L, 751355463660L, 836811401548L, 378142194541L, 723139535880L, 1089376298388L, 544901172144L, 927302981894L, 104821145835L, 361570506005L, 414757596889L, 308690049519L, 717724659239L, 413182397459L, 438030799489L, 450708331271L, 132105281067L, 64829149010L, 71719580948L, 733706305626L, 691479031664L, 822263630060L, 529391722665L, 660358950219L, 250411519944L, 16355695412L, 932015877032L, 97350299938L, 1070991464428L, 574185959393L, 85479111928L, 975522234984L, 418892440966L, 884044137865L, 322798555498L, 574000004201L, 187601737436L, 1023418058242L, 950282960064L, 525241272833L, 859913712524L, 273131890334L, 255339934339L, 71676448632L, 408905718872L, 35422448272L, 223020158593L, 561738589037L, 782214204942L, 964655878050L, 556351481529L, 157103353262L, 34314449556L, 758463720486L, 446078384137L, 972079852790L, 464069107136L, 379849726373L, 714241166682L, 477166959265L, 835078105705L, 248364644131L, 325784447660L, 9019298711L, 275423316235L, 363851582214L, 882484625720L, 687108954819L, 175240778760L, 483956836310L, 660975391556L, 732668319099L, 480827341340L, 465088194304L, 566591798223L, 475970028128L, 449292439180L, 161025198365L, 883701492531L, 42023413130L, 108146981568L, 401713887729L, 402544540474L, 934599491605L, 710308159827L, 862419311741L, 329949536958L, 111752444995L, 846042361066L, 132953978898L, 659572102827L, 560684104545L, 1092768415133L, 878391616036L, 1022444778591L, 326620754729L, 241824584281L, 596936162638L, 251780367748L, 1089765898361L, 812961024135L, 1038625007635L, 465730096798L, 945512282345L, 384724143010L, 56104559137L, 452275774954L, 112207663249L, 279662506080L, 404558977135L, 646080776108L, 313386478120L, 629630566407L, 1033709315536L, 754597294419L, 28719547825L, 188352917803L, 540249249833L, 178615604485L, 1005822947463L, 280874973046L, 27031871459L, 949370215601L, 614300253170L, 210672945426L, 602925089236L, 22578656669L, 144187431886L, 918416965404L, 668614902590L, 911336315878L, 879666481189L, 979552477285L, 1015808192784L, 330415257378L, 990010905568L, 31078020760L, 329780361843L, 168181551082L, 1055256994430L, 865564612908L, 891105971456L, 115600854559L, 804300534016L, 544365734765L, 419090549541L, 1072659624941L, 602367007782L, 772161951954L, 31988716636L, 375755403522L, 115093617428L, 460592836000L, 123977479415L, 479392652439L, 746020977097L, 625548651362L, 346211445686L, 531811820887L, 395194999408L, 469597669617L, 469018019449L, 949497911821L, 625673180213L, 934246077422L, 1034628455304L, 1015108144012L, 479172616516L, 968828348882L, 635381873029L, 705312821118L, 1013852200391L, 27803901457L, 302526233126L, 374219330099L, 935795624424L, 1070182583078L, 153038461400L, 199303598909L, 215981205749L, 881980314436L, 195241871057L, 215625316950L, 431565393211L, 718080039154L, 563560415928L, 25875060598L, 997946728194L, 122325848413L, 695533054363L, 829088372673L, 290544211276L, 6282881095L, 1041428377853L, 1049368932507L, 678656707707L, 240330983934L, 21508601187L, 1043753210366L, 968396332121L, 589042849633L, 881024748925L, 997749482625L, 741436851622L, 333173482493L, 65614744125L, 684005980285L, 39625502048L, 972586674192L, 780450069979L, 896102547740L, 1019630567062L, 32364051921L, 766510511664L, 885006669163L, 219924375738L, 60074482985L, 65429079782L, 186931000807L, 453463471578L, 366548233012L, 407917573569L, 966615459091L, 252960449212L, 429459236258L, 68473071721L, 84841375459L, 136167250217L, 1073124303588L, 139710205843L, 625146236037L, 217077072542L, 681017284385L, 438468470169L, 736192111144L, 91589003168L, 575998262861L, 306408747346L, 553893146155L, 1084734822227L, 1011430796491L, 833900749126L, 968239588179L, 732358627196L, 860198763273L, 274491428150L, 322890785340L, 1034058263601L, 877862092716L, 626532674929L, 116151501612L, 424433973894L, 732933511628L, 656419722775L, 78521155103L, 957872115916L, 977295670607L, 77451482699L, 1070647061923L, 360896848577L, 127815971098L, 312834364706L, 851944677756L, 328015465713L, 257436898962L, 850208561201L, 870524669425L, 995288713028L, 297389821024L, 1019164298782L, 365574459591L, 104128054313L, 700035664209L, 224480181109L, 106002422015L, 265777021761L, 454973477754L, 435313782570L, 371668259331L, 1040121354645L, 969149080916L, 728281952781L, 854851924295L, 812164015768L, 94071845750L, 726526281589L, 910153726188L, 183485198838L, 979656290962L, 367309234146L, 129281671599L, 475760489130L, 295330906976L, 546549195022L, 159107448155L, 418601707685L, 191199186773L, 213230269001L, 840515698117L, 1086354710051L, 725356353991L, 859754577007L, 153944016444L, 700525125952L, 212746831076L, 983285473481L, 790699367278L, 29985606856L, 121395668858L, 294239038997L}
//                    ;
//            long[] fixed = FileUtils.readDataFromFile("/Users/qixian/mpc4j-ali/qixian-108/mpc4j/mpc4j-crypto-fhe/src/main/java/edu/alibaba/mpc4j/crypto/fhe/c1.txt");
//
//            for (int i = 0; i < fixed.length; i++) {
//                destination.getData()[c1StartIndex + i] = fixed[i];
//            }
            // 固定 destination.getData() 中的 c1 ，用于 debug
//            System.out.println("destination c1: \n" + Arrays.toString(
//                    Arrays.copyOfRange(destination.getData(), c1StartIndex, destination.getData().length)
//            ));

        } else if (saveSeed) {
            // Sample non-NTT form and store the seed
            samplePolyUniform(ciphertextPrng, parms, destination.getData(), c1StartIndex);
            // c1 长度 k*N, 遍历 c1 中的每一个 poly, 然后做 ntt
            for (int i = 0; i < coeffModulusSize; i++) {
                NttTool.nttNegAcyclicHarvey(destination.getData(), c1StartIndex + i * coeffCount, nttTables[i]);
            }
        }

        // Sample e <-- chi, 误差分布， seal 里具体采用那种哪种分布，存在一个配置项
        /*
         * // Which distribution to use for noise sampling: rounded Gaussian or Centered Binomial Distribution
         * #ifdef SEAL_USE_GAUSSIAN_NOISE（默认 OFF）
         * #define SEAL_NOISE_SAMPLER sample_poly_normal
         * #else
         * #define SEAL_NOISE_SAMPLER sample_poly_cbd
         * #endif
         */
        long[] noise = new long[coeffCount * coeffModulusSize];
        // 这里暂时 默认使用 sample_poly_cbd
        // todo: noise 采样方式可配置
        samplePolyCbd(bootstrapPrng, parms, noise);
        // 置 0, for debug
//        Arrays.fill(noise, 0);

        // Calculate -(as+ e) (mod q) and store in c[0] in BFV/CKKS
        // Calculate -(as+pe) (mod q) and store in c[0] in BGV
        // 处理 RNS 下的每一个多项式计算
        for (int i = 0; i < coeffModulusSize; i++) {
            // c1 就是 a, 一个均匀分布的多项式， 这里在计算 as
            PolyArithmeticSmallMod.dyadicProductCoeffMod(
                    secretKey.data().getData(),
                    i * coeffCount,
                    destination.getData(),
                    c1StartIndex + i * coeffCount,
                    coeffCount,
                    coeffModulus[i],
                    c0StartIndex + i * coeffCount,
                    destination.getData()
            );
            // 到这里 a s 都是 ntt form
            // e 不是，需要根据参数，决定是否将 e 转换为 NTT，还是 将 as 转回系数表示
            if (isNttForm) {
                // Transform the noise e into NTT representation
                NttTool.nttNegAcyclicHarvey(noise, i * coeffCount, nttTables[i]);
            } else {
                // 把 当前的 c0 = as 转回 系数表示
                NttTool.inverseNttNegAcyclicHarvey(destination.getData(), c0StartIndex + i * coeffCount, nttTables[i]);
            }

            if (type == SchemeType.BGV) {
                throw new IllegalArgumentException("can not support BGV");
            }

            // c0 = as + e
            PolyArithmeticSmallMod.addPolyCoeffMod(
                    noise,
                    i * coeffCount,
                    destination.getData(),
                    c0StartIndex + i * coeffCount,
                    coeffCount,
                    coeffModulus[i],
                    c0StartIndex + i * coeffCount,
                    destination.getData()
            );
            // (as + e, a) ---> (-(as + e), a)
            PolyArithmeticSmallMod.negatePolyCoeffMod(
                    destination.getData(),
                    c0StartIndex + i * coeffCount,
                    coeffCount,
                    coeffModulus[i],
                    c0StartIndex + i * coeffCount,
                    destination.getData());
        }

        if (!isNttForm && !saveSeed) {
            for (int i = 0; i < coeffModulusSize; i++) {
                // Transform the c1 into non-NTT representation
                NttTool.inverseNttNegAcyclicHarvey(
                        destination.getData(),
                        c1StartIndex + i * coeffCount,
                        nttTables[i]);
            }
        }

        if (saveSeed) {
            // TODO:
        }
    }


}
