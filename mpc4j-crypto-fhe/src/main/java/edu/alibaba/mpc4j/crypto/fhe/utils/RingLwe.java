package edu.alibaba.mpc4j.crypto.fhe.utils;

import edu.alibaba.mpc4j.crypto.fhe.Ciphertext;
import edu.alibaba.mpc4j.crypto.fhe.context.Context;
import edu.alibaba.mpc4j.crypto.fhe.keys.SecretKey;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.ntt.NttHandler;
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
import edu.alibaba.mpc4j.crypto.fhe.zq.UintArithmetic;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintArithmeticMod;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintArithmeticSmallMod;

import java.util.Arrays;

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


    public static void encryptZeroSymmetric(SecretKey secretKey, Context context, ParmsIdType parmsId, Boolean isNttForm, Boolean saveSeed, Ciphertext destination) {

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
        UniformRandomGenerator ciphertextPrng = UniformRandomGeneratorFactory.defaultFactory().create();

        // Generate ciphertext: (c[0], c[1]) = ([-(as+ e)]_q, a) in BFV/CKKS
        // Generate ciphertext: (c[0], c[1]) = ([-(as+pe)]_q, a) in BGV
        int c0StartIndex = 0;
        int c1StartIndex = destination.getData(1);

        // Sample a uniformly at random
        if (isNttForm || !saveSeed) {
            // Sample the NTT form directly
            samplePolyUniform(ciphertextPrng, parms, destination.getData(), c1StartIndex);
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
        samplePolyCbd(bootstrapPrng, parms, noise);

        // Calculate -(as+ e) (mod q) and store in c[0] in BFV/CKKS
        // Calculate -(as+pe) (mod q) and store in c[0] in BGV
        // 处理 RNS 下的每一个多项式计算
        for (int i = 0; i < coeffModulusSize; i++) {
            // c1 就是 a, 一个均匀分布的多项式， 这里在计算 as
            PolyArithmeticSmallMod.dyadicProductCoeffMod(secretKey.data().getData(), i * coeffCount, destination.getData(), c1StartIndex + i * coeffCount, coeffCount, coeffModulus[i],
                    c0StartIndex + i * coeffCount, destination.getData()
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
                    destination.getData());
            // (as + e, a) ---> (-(as + e), a)
            PolyArithmeticSmallMod.negatePolyCoeffMod(destination.getData(), c0StartIndex + i * coeffCount, coeffCount, coeffModulus[i], c0StartIndex, destination.getData());

        }

        if (!isNttForm && !saveSeed) {
            for (int i = 0; i < coeffModulusSize; i++) {
                // Transform the c1 into non-NTT representation
                NttTool.inverseNttNegAcyclicHarvey(destination.getData(), c1StartIndex + i * coeffCount, nttTables[i]);
            }
        }

        if (saveSeed) {
            // TODO:
        }
    }


}
