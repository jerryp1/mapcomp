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
        prng.generate(destination);

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
        prng.generate( x);
        // 0001 1111
        x[2] &= 0x1F;
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

    /**
     * standard LWE encryption, encrypt zero and store ciphertext in destination.
     *
     * @param publicKey   public key.
     * @param context     context.
     * @param parmsId     parms ID.
     * @param isNttForm   is NTT form.
     * @param destination destination.
     */
    public static void encryptZeroAsymmetric(PublicKey publicKey, Context context, ParmsIdType parmsId, boolean isNttForm, Ciphertext destination) {
        assert ValueChecker.isValidFor(publicKey, context);
        Context.ContextData contextData = context.getContextData(parmsId);
        EncryptionParams parms = contextData.getParms();
        Modulus[] coeffModulus = parms.getCoeffModulus();
        Modulus plainModulus = parms.getPlainModulus();
        int coeffModulusSize = coeffModulus.length;
        int coeffCount = parms.getPolyModulusDegree();
        NttTables[] nttTables = contextData.getSmallNttTables();
        // num of polys
        int encryptedSize = publicKey.data().getSize();
        SchemeType type = parms.getScheme();
        // Make destination have right size and parms_id
        // Ciphertext (c_0,c_1, ...)
        destination.resize(context, parmsId, encryptedSize);
        destination.setIsNttForm(isNttForm);
        destination.setScale(1.0);
        destination.setCorrectionFactor(1);
        // c[j] = public_key[j] * u + e[j] in BFV/CKKS = public_key[j] * u + p * e[j] in BGV, where e[j] <-- chi, u <-- R_3
        // Create a PRNG; u and the noise/error share the same PRNG
        UniformRandomGenerator prng = parms.getRandomGeneratorFactory().create();
        // Generate u <-- R_3
        long[] u = new long[coeffCount * coeffModulusSize];
        samplePolyTernary(prng, parms, u);
        // c[j] = u * public_key[j]
        for (int i = 0; i < coeffModulusSize; i++) {
            // u 是 RnsIter, 这里是对 CoeffIter 操作，RnsIter + startIndex ---> CoeffIter
            NttTool.nttNegacyclicHarveyRns(u, coeffCount, coeffModulusSize, i, nttTables);
            // j 是对 密文多项式的索引
            for (int j = 0; j < encryptedSize; j++) {
                // 注意这里是对 CoeffIter 操作，注意起点的计算
                PolyArithmeticSmallMod.dyadicProductCoeffMod(
                    u,
                    i * coeffCount,
                    publicKey.data().getData(),
                    publicKey.data().indexAt(j) + i * coeffCount,
                    coeffCount,
                    coeffModulus[i],
                    destination.getData(),
                    destination.indexAt(j) + i * coeffCount
                );
                // inverse NTT
                if (!isNttForm) {
                    NttTool.inverseNttNegacyclicHarvey(destination.getData(), destination.indexAt(j) + i * coeffCount, nttTables[i]);
                }
            }
        }
        // Addition with e_0, e_1 is in non-NTT form, Generate e_j <-- chi
        // c[j] = public_key[j] * u + e[j] in BFV/CKKS, = public_key[j] * u + p * e[j] in BGV,
        for (int j = 0; j < encryptedSize; j++) {
            // 采样 noise, 默认使用cbd分布
            // todo: noise 采样方式可配置
            samplePolyCbd(prng, parms, u);
            if (type == SchemeType.BGV) {
                //todo: implement BGV
                throw new IllegalArgumentException("now  cannot support BGV");
            } else { // BFV & CKKS
                if (isNttForm) {
                    // 注意函数签名, 直接处理 u 这个 RnsIter
                    NttTool.nttNegacyclicHarveyRns(u, coeffCount, coeffModulusSize, nttTables);
                }
            }
            // 全部都处理为 CoeffIter 这个粒度的运算，不管输入的数组是表示 RnsIter/PolyIter, 每次都只处理一个 CoeffIter
            // 那么核心的就是 起点的计算 + coeffCount
            // 这里我们是想计算 RnsIter, 由于我们单次运算的粒度是 CoeffIter, 所以这里需要一个循环，k次，即 RnsBaseSize
            // todo: 需要设计一个 统一、高效的 基于 long[] 的 CoeffIter/RnsIter/PolyIter 的处理方式
            for (int i = 0; i < coeffModulusSize; i++) {
                // Ciphertext 有多个 poly（polyIter）, destination.indexAt(j) 定位到单个poly（RnsIter） + i * coeffCount 定位到 CoffIter
                PolyArithmeticSmallMod.addPolyCoeffMod(
                    u,
                    i * coeffCount,
                    destination.getData(),
                    destination.indexAt(j) + i * coeffCount,
                    coeffCount,
                    coeffModulus[i],
                    destination.getData(),
                    destination.indexAt(j) + i * coeffCount
                );
            }
        }
    }

    /**
     * encrypt zero using secret key and store ciphertext destination.
     *
     * @param secretKey   secret key.
     * @param context     context.
     * @param parmsId     parms ID.
     * @param isNttForm   is NTT form.
     * @param saveSeed    save seed.
     * @param destination destination.
     */
    public static void encryptZeroSymmetric(SecretKey secretKey, Context context, ParmsIdType parmsId, Boolean isNttForm,
        Boolean saveSeed, Ciphertext destination) {
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
        bootstrapPrng.generate(publicPrngSeed);
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
        } else if (saveSeed) {
            // Sample non-NTT form and store the seed
            samplePolyUniform(ciphertextPrng, parms, destination.getData(), c1StartIndex);
            // c1 长度 k*N, 遍历 c1 中的每一个 poly, 然后做 ntt
            NttTool.nttNegacyclicHarveyPoly(destination.getData(), 2, coeffCount, coeffModulusSize, 1, nttTables);
        }
        // Sample e <-- chi, 误差分布， seal 里具体采用那种哪种分布，存在一个配置项
        long[] noise = new long[coeffCount * coeffModulusSize];
        // 这里暂时 默认使用 sample_poly_cbd
        // todo: noise 采样方式可配置, for debug, Arrays.fill(noise, 0);
        samplePolyCbd(bootstrapPrng, parms, noise);
        // Calculate -(as+ e) (mod q) and store in c[0] in BFV/CKKS
        // Calculate -(as+pe) (mod q) and store in c[0] in BGV
        for (int i = 0; i < coeffModulusSize; i++) {
            // c1 就是 a, 一个均匀分布的多项式， 这里在计算 as
            PolyArithmeticSmallMod.dyadicProductCoeffMod(
                secretKey.data().getData(),
                i * coeffCount,
                destination.getData(),
                c1StartIndex + i * coeffCount,
                coeffCount,
                coeffModulus[i],
                destination.getData(),
                c0StartIndex + i * coeffCount
            );
            // 到这里 a s 都是 ntt form
            // e 不是，需要根据参数，决定是否将 e 转换为 NTT，还是 将 as 转回系数表示
            if (isNttForm) {
                // Transform the noise e into NTT representation
                NttTool.nttNegacyclicHarveyRns(noise, coeffCount, coeffModulusSize, i, nttTables);
            } else {
                // 把 当前的 c0 = as 转回 系数表示
                NttTool.inverseNttNegacyclicHarvey(destination.getData(), c0StartIndex + i * coeffCount, nttTables[i]);
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
                destination.getData(),
                c0StartIndex + i * coeffCount
            );
            // (as + e, a) ---> (-(as + e), a)
            PolyArithmeticSmallMod.negatePolyCoeffMod(
                destination.getData(),
                c0StartIndex + i * coeffCount,
                coeffCount,
                coeffModulus[i],
                destination.getData(),
                c0StartIndex + i * coeffCount
            );
        }
        if (!isNttForm && !saveSeed) {
            for (int i = 0; i < coeffModulusSize; i++) {
                // Transform the c1 into non-NTT representation
                NttTool.inverseNttNegacyclicHarvey(
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