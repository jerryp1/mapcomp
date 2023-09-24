package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.context.Context;
import edu.alibaba.mpc4j.crypto.fhe.keys.*;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.ntt.NttTables;
import edu.alibaba.mpc4j.crypto.fhe.ntt.NttTool;
import edu.alibaba.mpc4j.crypto.fhe.params.EncryptionParams;
import edu.alibaba.mpc4j.crypto.fhe.rq.PolyArithmeticSmallMod;
import edu.alibaba.mpc4j.crypto.fhe.utils.Constants;
import edu.alibaba.mpc4j.crypto.fhe.utils.GaloisTool;
import edu.alibaba.mpc4j.crypto.fhe.utils.RingLwe;
import edu.alibaba.mpc4j.crypto.fhe.utils.ValueChecker;
import edu.alibaba.mpc4j.crypto.fhe.zq.Common;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintArithmeticSmallMod;

import java.util.stream.IntStream;

/**
 * @author Qixian Zhou
 * @date 2023/9/14
 */
public class KeyGenerator {

    private Context context;

    private SecretKey secretKey;

    private int secretKeyArraySize = 0;

    private long[] secretKeyArray;

    boolean skGenerated = false;


    public KeyGenerator(Context context) {

        if (!context.isParametersSet()) {
            throw new IllegalArgumentException("encryption parameters are not set correctly");
        }
        this.context = context;
        // Secret key has not been generated
        skGenerated = false;
        // Generate the secret and public key
        generateSk(false);
    }

    public KeyGenerator(Context context, SecretKey secretKey) {

        if (!context.isParametersSet()) {
            throw new IllegalArgumentException("encryption parameters are not set correctly");
        }
        if (ValueChecker.isValidFor(secretKey, context)) {
            throw new IllegalArgumentException("secret key is not valid for encryption parameters");
        }

        this.secretKey = secretKey;
        skGenerated = true;
        // only need to compute secretKeyArray
        generateSk(true);
    }


    private void generateSk(boolean isInitialized) {

        // Extract encryption parameters.
        Context.ContextData contextData = context.keyContextData();
        EncryptionParams params = contextData.getParms();
        Modulus[] coeffModulus = params.getCoeffModulus();
        int coeffCount = params.getPolyModulusDegree(); // N
        int coeffModulusSize = coeffModulus.length; // k

        if (!isInitialized) {

            secretKey = new SecretKey();
            skGenerated = false;
            // RNS 表示下 需要 k 个 Poly, 每个 Poly 有 N 个 slots
            secretKey.data().resize(Common.mulSafe(coeffCount, coeffModulusSize, false));
            // Generate secret key
            RingLwe.samplePolyTernary(params.getRandomGeneratorFactory().create(), params, secretKey.data().getData());

            // Transform the secret s into NTT representation.
            NttTables[] nttTables = contextData.getSmallNttTables();
            NttTool.nttNegAcyclicHarvey(secretKey.data().getData(), coeffModulusSize, nttTables);

            // Set the parms_id for secret key
            // TODO: here need deep-copy?
            secretKey.setParmsId(contextData.getParmsId().clone());
        }
        // Set the secret_key_array to have size 1 (first power of secret)
        secretKeyArray = new long[coeffCount * coeffModulusSize];
        // TODO: why need copy?
        System.arraycopy(secretKey.data().getData(), 0, secretKeyArray, 0, coeffCount * coeffModulusSize);
        secretKeyArraySize = 1;

        skGenerated = true;
    }

    public void createPublicKey(PublicKey destination) {
        generatePk(false, destination);
    }

    public void createRelinKeys(RelinKeys relinKeys) {
        createRelinKeys(1, false, relinKeys);
    }

    /**
     * todo: convert return type to Serializable<RelinKeys>
     *
     * @return
     */
    public RelinKeys createRelinKeys() {
        return createRelinKeys(1, false);
    }

    public void createGaloisKeys(int[] galoisElts, GaloisKeys destination) {
        createGaloisKeys(galoisElts, false, destination);
    }

    public void createGaloisKeys(GaloisKeys destination) {
        createGaloisKeys(
                context.keyContextData().getGaloisTool().getEltsAll()
                , destination);
    }

    /**
     * todo: convert return type to Serializable<RelinKeys>
     *
     * @return
     */
    public GaloisKeys createGaloisKeys() {
        return createGaloisKeys(context.keyContextData().getGaloisTool().getEltsAll());
    }

    /**
     * todo: convert return type to Serializable<RelinKeys>
     *
     * @param galoisElts
     * @return
     */
    public GaloisKeys createGaloisKeys(int[] galoisElts) {
        return createGaloisKeys(galoisElts, false);
    }


    public void createGaloisKeysFromSteps(int[] steps, GaloisKeys destination) {

        if (!context.keyContextData().getQualifiers().isUsingBatching()) {
            throw new IllegalArgumentException("encryption parameters do not support batching");
        }

        createGaloisKeys(context.keyContextData().getGaloisTool().getEltsFromSteps(steps), false, destination);
    }


    private PublicKey generatePk(boolean saveSeed) {

        if (!skGenerated) {
            throw new IllegalArgumentException("cannot generate public key for unspecified secret key");
        }

        Context.ContextData contextData = context.keyContextData();
        EncryptionParams parms = contextData.getParms();
        Modulus[] coeffModulus = parms.getCoeffModulus();
        int coeffCount = parms.getPolyModulusDegree();
        int coeffModulusSize = coeffModulus.length;

        // size check , todo: can remove?
        if (!Common.productFitsIn(false, coeffCount, coeffModulusSize)) {
            throw new IllegalArgumentException("valid parameters");
        }

        PublicKey publicKey = new PublicKey();
        RingLwe.encryptZeroSymmetric(secretKey, context, contextData.getParmsId(), true, saveSeed, publicKey.data());

        // set the parmsId
        // todo: really need deep-copy?
        publicKey.setParmsId(contextData.getParmsId().clone());

        return publicKey;
    }

    private void generatePk(boolean saveSeed, PublicKey publicKey) {

        if (!skGenerated) {
            throw new IllegalArgumentException("cannot generate public key for unspecified secret key");
        }

        Context.ContextData contextData = context.keyContextData();
        EncryptionParams parms = contextData.getParms();
        Modulus[] coeffModulus = parms.getCoeffModulus();
        int coeffCount = parms.getPolyModulusDegree();
        int coeffModulusSize = coeffModulus.length;

        // size check , todo: can remove?
        if (!Common.productFitsIn(false, coeffCount, coeffModulusSize)) {
            throw new IllegalArgumentException("valid parameters");
        }

        RingLwe.encryptZeroSymmetric(secretKey, context, contextData.getParmsId(), true, saveSeed, publicKey.data());

        // set the parmsId
        // todo: really need deep-copy?
        publicKey.setParmsId(contextData.getParmsId().clone());
    }

    /**
     * @param newKey      single poly in RNS, length is k * N
     * @param destination
     * @param saveSeed
     */
    private void generateOneKeySwitchKey(long[] newKey, PublicKey[] destination, boolean saveSeed) {


        if (!context.isUsingKeySwitching()) {
            throw new IllegalArgumentException("keyswitching is not supported by the context");
        }

        int coeffCount = context.keyContextData().getParms().getPolyModulusDegree();
        int decomposeModCount = context.firstContextData().getParms().getCoeffModulus().length;
        Context.ContextData keyContextData = context.keyContextData();
        EncryptionParams keyParms = keyContextData.getParms();
        Modulus[] keyModulus = keyParms.getCoeffModulus();
        // todo: really need this check?
        if (!Common.productFitsIn(false, coeffCount, decomposeModCount)) {
            throw new IllegalArgumentException("invalid parameters");
        }

        assert destination.length >= decomposeModCount;
        // 这里面是比较耗时的操作，所以考虑并发
        IntStream.range(0, decomposeModCount).parallel().forEach(
                i -> {
                    long[] temp = new long[coeffCount];

                    RingLwe.encryptZeroSymmetric(
                            secretKey,
                            context,
                            keyContextData.getParmsId(),
                            true,
                            saveSeed,
                            destination[i].data()
                    );

                    long factor = UintArithmeticSmallMod.barrettReduce64(keyModulus[keyModulus.length - 1].getValue(), keyModulus[i]);

                    PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(
                            newKey, i * coeffCount, coeffCount, factor, keyModulus[i], 0, temp
                    );

                    // We use the SeqIter at get<3>(I) to find the i-th RNS factor of the first destination polynomial.

                    // destination[i].data() 是 Ciphertext, .getData() 是 Ciphertext 的底层数组，可认为是 multi-poly in RNS, size * k * N
                    PolyArithmeticSmallMod.addPolyCoeffMod(destination[i].data().getData(), 0, temp, 0, coeffCount, keyModulus[i], 0, destination[i].data().getData());
                }
        );
    }


    /**
     * @param newKeys     multi-poly in RNS (polyIter), length is size * k * N
     * @param startIndex  startIndex of the RnsIter used in newKeys
     * @param destination
     * @param saveSeed
     */
    private void generateOneKeySwitchKey(long[] newKeys, int startIndex, PublicKey[] destination, boolean saveSeed) {

        if (!context.isUsingKeySwitching()) {
            throw new IllegalArgumentException("key switching is not supported by the context");
        }

        int coeffCount = context.keyContextData().getParms().getPolyModulusDegree();
        int decomposeModCount = context.firstContextData().getParms().getCoeffModulus().length;
        Context.ContextData keyContextData = context.keyContextData();
        EncryptionParams keyParms = keyContextData.getParms();
        Modulus[] keyModulus = keyParms.getCoeffModulus();
        // todo: really need this check?
        if (!Common.productFitsIn(false, coeffCount, decomposeModCount)) {
            throw new IllegalArgumentException("invalid parameters");
        }
        // 实例化 destination
        // 这里的 destination 是一个 null 的 PublicKey[], 在这里实例化，无法影响到外层，所以又重写了一个重载函数


        assert destination.length >= decomposeModCount;
        // 这里面是比较耗时的操作，所以考虑并发
        IntStream.range(0, decomposeModCount).parallel().forEach(
                i -> {
                    long[] temp = new long[coeffCount];

                    RingLwe.encryptZeroSymmetric(
                            secretKey,
                            context,
                            keyContextData.getParmsId(),
                            true,
                            saveSeed,
                            destination[i].data()
                    );

                    long factor = UintArithmeticSmallMod.barrettReduce64(keyModulus[keyModulus.length - 1].getValue(), keyModulus[i]);

                    PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(
                            newKeys, startIndex + i * coeffCount, coeffCount, factor, keyModulus[i], 0, temp
                    );

                    // We use the SeqIter at get<3>(I) to find the i-th RNS factor of the first destination polynomial.

                    // destination[i].data() 是 Ciphertext, .getData() 是 Ciphertext 的底层数组，可认为是 multi-poly in RNS, size * k * N
                    PolyArithmeticSmallMod.addPolyCoeffMod(destination[i].data().getData(), 0, temp, 0, coeffCount, keyModulus[i], 0, destination[i].data().getData());
                }
        );
    }

    private void generateOneKeySwitchKey(long[] newKeys, PublicKey[][] destinations, int destinationIndex, boolean saveSeed) {

        if (!context.isUsingKeySwitching()) {
            throw new IllegalArgumentException("key switching is not supported by the context");
        }

        int coeffCount = context.keyContextData().getParms().getPolyModulusDegree();
        int decomposeModCount = context.firstContextData().getParms().getCoeffModulus().length;
        Context.ContextData keyContextData = context.keyContextData();
        EncryptionParams keyParms = keyContextData.getParms();
        Modulus[] keyModulus = keyParms.getCoeffModulus();
        // todo: really need this check?
        if (!Common.productFitsIn(false, coeffCount, decomposeModCount)) {
            throw new IllegalArgumentException("invalid parameters");
        }
        // 实例化 destination
        // 通过 二维数组的 index 去实例化，可以影响到调用函数外的 destinations
        // 这里就是实例化了长度为 new PublicKey[decomposeModCount]
        destinations[destinationIndex] = IntStream.range(0, decomposeModCount)
                .mapToObj(i -> new PublicKey()).toArray(PublicKey[]::new);

        // 这里面是比较耗时的操作，所以考虑并发
        IntStream.range(0, decomposeModCount).parallel().forEach(
                i -> {
                    long[] temp = new long[coeffCount];

                    RingLwe.encryptZeroSymmetric(
                            secretKey,
                            context,
                            keyContextData.getParmsId(),
                            true,
                            saveSeed,
                            destinations[destinationIndex][i].data()
                    );

                    long factor = UintArithmeticSmallMod.barrettReduce64(keyModulus[keyModulus.length - 1].getValue(), keyModulus[i]);

                    PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(
                            newKeys,
                            i * coeffCount,
                            coeffCount,
                            factor,
                            keyModulus[i],
                            0,
                            temp
                    );

                    // We use the SeqIter at get<3>(I) to find the i-th RNS factor of the first destination polynomial.

                    // destination[i].data() 是 Ciphertext, .getData() 是 Ciphertext 的底层数组，可认为是 multi-poly in RNS, size * k * N
                    PolyArithmeticSmallMod.addPolyCoeffMod(
                            destinations[destinationIndex][i].data().getData(),
                            i * coeffCount,
                            temp,
                            0,
                            coeffCount,
                            keyModulus[i],
                            i * coeffCount,
                            destinations[destinationIndex][i].data().getData());
                }
        );
    }

    /**
     * 注意函数签名，是为了处理 destinations[destinationIndex]
     *
     * @param newKeys          + startIndex =  RnsIter, length is k * N
     * @param startIndex
     * @param destinations
     * @param destinationIndex
     * @param saveSeed
     */
    private void generateOneKeySwitchKey(long[] newKeys, int startIndex, PublicKey[][] destinations, int destinationIndex, boolean saveSeed) {

        if (!context.isUsingKeySwitching()) {
            throw new IllegalArgumentException("key switching is not supported by the context");
        }

        int coeffCount = context.keyContextData().getParms().getPolyModulusDegree();
        int decomposeModCount = context.firstContextData().getParms().getCoeffModulus().length;
        Context.ContextData keyContextData = context.keyContextData();
        EncryptionParams keyParms = keyContextData.getParms();
        Modulus[] keyModulus = keyParms.getCoeffModulus();
        // todo: really need this check?
        if (!Common.productFitsIn(false, coeffCount, decomposeModCount)) {
            throw new IllegalArgumentException("invalid parameters");
        }
        // 实例化 destination
        // 通过 二维数组的 index 去实例化，可以影响到调用函数外的 destinations
        // 这里就是实例化了长度为 new PublicKey[decomposeModCount]
        destinations[destinationIndex] = IntStream.range(0, decomposeModCount)
                .mapToObj(i -> new PublicKey()).toArray(PublicKey[]::new);


        // 这里面是比较耗时的操作，所以考虑并发
        IntStream.range(0, decomposeModCount).parallel().forEach(
                i -> {
                    long[] temp = new long[coeffCount];

                    RingLwe.encryptZeroSymmetric(
                            secretKey,
                            context,
                            keyContextData.getParmsId(),
                            true,
                            saveSeed,
                            destinations[destinationIndex][i].data()
                    );

                    long factor = UintArithmeticSmallMod.barrettReduce64(keyModulus[keyModulus.length - 1].getValue(), keyModulus[i]);
//                    System.out.println("i: " + i + ", startIndex + i * coeffCount: " + startIndex + i * coeffCount);

                    PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(
                            newKeys, startIndex + i * coeffCount, coeffCount, factor, keyModulus[i], 0, temp
                    );

                    // We use the SeqIter at get<3>(I) to find the i-th RNS factor of the first destination polynomial.

                    // destination[i].data() 是 Ciphertext, .getData() 是 Ciphertext 的底层数组，
                    // 可认为是 multi-poly in RNS, size * k * N
                    // 注意第二个参数，index 的计算，这里的逻辑有点复杂
                    // size * k * N , 这里不管是哪一个 密文 （destinations[destinationIndex][i].data().getData()）
                    // 都取 第 0 个 k * N , 然后在 k * N 里，取 第 i/k 个, 一个具体的例子
                    // i = 0 --> c0 (size * k * N ) --> c0[0] --> [0, k * N) ---> c0[0][i] --> c0[0][0] --> [0, N)
                    // i = 1 --->c1 (size * k * N ) ---> c1[0] ---> [0, k * N) ---> c1[0][i] --> c0[0][1] --> [N, 2N)
                    PolyArithmeticSmallMod.addPolyCoeffMod(
                            destinations[destinationIndex][i].data().getData(),
                            i * coeffCount,
                            temp,
                            0,
                            coeffCount,
                            keyModulus[i],
                            i * coeffCount,
                            destinations[destinationIndex][i].data().getData());
                }
        );



//        // 这里面是比较耗时的操作，所以考虑并发
//        IntStream.range(0, decomposeModCount).parallel().forEach(
//                i -> {
//                    long[] temp = new long[coeffCount];
//
//                    RingLwe.encryptZeroSymmetric(
//                            secretKey,
//                            context,
//                            keyContextData.getParmsId(),
//                            true,
//                            saveSeed,
//                            destinations[destinationIndex][i].data()
//                    );
//
//                    long factor = UintArithmeticSmallMod.barrettReduce64(keyModulus[keyModulus.length - 1].getValue(), keyModulus[i]);
////                    System.out.println("i: " + i + ", startIndex + i * coeffCount: " + startIndex + i * coeffCount);
//
//                    PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(
//                            newKeys, startIndex + i * coeffCount, coeffCount, factor, keyModulus[i], 0, temp
//                    );
//
//                    // We use the SeqIter at get<3>(I) to find the i-th RNS factor of the first destination polynomial.
//
//                    // destination[i].data() 是 Ciphertext, .getData() 是 Ciphertext 的底层数组，
//                    // 可认为是 multi-poly in RNS, size * k * N
//
//
//                    PolyArithmeticSmallMod.addPolyCoeffMod(
//                            destinations[destinationIndex][i].data().getData(),
//                            destinations[destinationIndex][i].data().getData(i), // 返回 第 i 个 poly 的起点
//                            temp,
//                            0,
//                            coeffCount,
//                            keyModulus[i],
//                            destinations[destinationIndex][i].data().getData(i),
//                            destinations[destinationIndex][i].data().getData());
//                }
//        );

    }


    /**
     * @param newKeys            polyIter, size * k * N
     * @param newKeysCoeffCount  N
     * @param newKeysModulusSize k, 为了避免使用 PolyIter 对象带来的额外开销， 使用 long[] + k + N 来等价表示 PolyIter
     * @param numKeys
     * @param destination
     * @param saveSeed
     */
    private void generateKeySwitchKeys(long[] newKeys, int newKeysCoeffCount, int newKeysModulusSize, int numKeys, KeySwitchKeys destination, boolean saveSeed) {


        int coeffCount = context.keyContextData().getParms().getPolyModulusDegree();
        Context.ContextData keyContextData = context.keyContextData();
        EncryptionParams keyParms = keyContextData.getParms();
        int coeffModulusSize = keyParms.getCoeffModulus().length;


        // size check
        if (!Common.productFitsIn(false, coeffCount, coeffModulusSize, numKeys)) {
            throw new IllegalArgumentException("invalid parameters");
        }

        assert newKeysCoeffCount == coeffCount;
        assert newKeysModulusSize == coeffModulusSize;

        // destination本质上是 PublicKey[][], 之前new的时候还没有初始化这个数组，这里需要补上
//        int decomposeModCount = context.firstContextData().getParms().getCoeffModulus().length;
        destination.resizeRows(numKeys);

        // 注意起点的计算
//        IntStream.range(0, numKeys).parallel().forEach(
//                i -> generateOneKeySwitchKey(newKeys, i * coeffCount * coeffModulusSize, destination.data()[i], saveSeed)
//        );

        // 注意这个被调用的函数签名
        IntStream.range(0, numKeys).parallel().forEach(
                i -> generateOneKeySwitchKey(newKeys, i * coeffCount * coeffModulusSize, destination.data(), i, saveSeed)
        );

    }

    /**
     * @param newKeys            polyIter, size * k * N
     * @param newKeysCoeffCount  N
     * @param newKeysModulusSize k, 为了避免使用 PolyIter 对象带来的额外开销， 使用 long[] + k + N 来等价表示 PolyIter
     * @param numKeys
     * @param destination
     * @param saveSeed
     */
    /**
     * @param newKeys            polyIter, size * k * N
     * @param startIndex         newKeys 中的起点，处理这个起点往后的区间
     * @param newKeysCoeffCount  k
     * @param newKeysModulusSize N
     * @param numKeys
     * @param destination
     * @param saveSeed
     */
    private void generateKeySwitchKeys(long[] newKeys, int startIndex, int newKeysCoeffCount, int newKeysModulusSize, int numKeys, KeySwitchKeys destination, boolean saveSeed) {

        assert startIndex % (newKeysCoeffCount * newKeysModulusSize) == 0;

        int coeffCount = context.keyContextData().getParms().getPolyModulusDegree();
        Context.ContextData keyContextData = context.keyContextData();
        EncryptionParams keyParms = keyContextData.getParms();
        int coeffModulusSize = keyParms.getCoeffModulus().length;

        // size check
        if (!Common.productFitsIn(false, coeffCount, coeffModulusSize, numKeys)) {
            throw new IllegalArgumentException("invalid parameters");
        }

        assert newKeysCoeffCount == coeffCount;
        assert newKeysModulusSize == coeffModulusSize;

        destination.resizeRows(numKeys);

        // 注意起点的计算
        IntStream.range(0, numKeys).parallel().forEach(
                i -> generateOneKeySwitchKey(newKeys, startIndex + i * coeffCount * coeffModulusSize, destination.data(), i, saveSeed)
        );
    }


    private void createRelinKeys(int count, boolean saveSeed, RelinKeys destination) {

        if (!skGenerated) {
            throw new IllegalArgumentException("cannot generate relinearization keys for unspecified secret key");
        }
        // count 是 密文中多项式的数量
        if (count == 0 || count > Constants.CIPHERTEXT_SIZE_MAX - 2) {
            throw new IllegalArgumentException("invalid count");
        }

        // Extract encryption parameters.
        Context.ContextData contextData = context.keyContextData();
        EncryptionParams parms = contextData.getParms();
        int coeffCount = parms.getPolyModulusDegree();
        int coeffModulusSize = parms.getCoeffModulus().length;

        // todo: really need check?
        if (!Common.productFitsIn(false, coeffCount, coeffModulusSize)) {
            throw new IllegalArgumentException("invalid parameters");
        }

        // Make sure we have enough secret keys computed
        // 计算 sk sk^2, ..., sk^{count+1}
        computeSecretKeyArray(contextData, count + 1);

        // Assume the secret key is already transformed into NTT form.
//        RelinKeys relinKeys = new RelinKeys();
//        System.out.println("secretKeyArray: " + secretKeyArray.length);
        // 开始计算 KeySwitchKeys
        // 注意第二个参数是 secretKeyArray 的起点
        generateKeySwitchKeys(secretKeyArray,
                coeffCount * coeffModulusSize,
                coeffCount,
                coeffModulusSize,
                count,
                (KeySwitchKeys) destination,
                saveSeed
        );
        // todo: really need deep-copy?
        destination.setParmsId(contextData.getParmsId().clone());

    }

    /**
     * Generates and returns the specified number of relinearization keys.
     *
     * @param count    The number of relinearization keys to generate
     * @param saveSeed If true, save seed instead of a polynomial.
     * @return
     */
    private RelinKeys createRelinKeys(int count, boolean saveSeed) {

        if (!skGenerated) {
            throw new IllegalArgumentException("cannot generate relinearization keys for unspecified secret key");
        }

        if (count == 0 || count > Constants.CIPHERTEXT_SIZE_MAX - 2) {
            throw new IllegalArgumentException("invalid count");
        }

        // Extract encryption parameters.
        Context.ContextData contextData = context.keyContextData();
        EncryptionParams parms = contextData.getParms();
        int coeffCount = parms.getPolyModulusDegree();
        int coeffModulusSize = parms.getCoeffModulus().length;

        // todo: really need check?
        if (!Common.productFitsIn(false, coeffCount, coeffModulusSize)) {
            throw new IllegalArgumentException("invalid parameters");
        }

        // Make sure we have enough secret keys computed
        computeSecretKeyArray(contextData, count + 1);

        // Assume the secret key is already transformed into NTT form.
        RelinKeys relinKeys = new RelinKeys();

        generateKeySwitchKeys(secretKeyArray,
                coeffCount * coeffModulusSize,
                coeffCount,
                coeffModulusSize,
                coeffCount,
                (KeySwitchKeys) relinKeys,
                saveSeed
        );
        // todo: really need deep-copy?
        relinKeys.setParmsId(contextData.getParmsId().clone());

        return relinKeys;
    }

    private void createGaloisKeys(int[] galoisElts, boolean saveSeed, GaloisKeys destination) {
        if (!skGenerated) {
            throw new IllegalArgumentException("cannot generate Galois keys for unspecified secret key");
        }

        Context.ContextData contextData = context.keyContextData();
        EncryptionParams parms = contextData.getParms();
        Modulus[] coeffModulus = parms.getCoeffModulus();
        GaloisTool galoisTool = contextData.getGaloisTool();
        int coeffCount = parms.getPolyModulusDegree();
        int coeffModulusSize = coeffModulus.length;

        if (!Common.productFitsIn(false, coeffCount, coeffModulusSize, 2)) {
            throw new IllegalArgumentException("invalid parameters");
        }

//        GaloisKeys galoisKeys = new GaloisKeys();
        // The max number of keys is equal to number of coefficients
//        destination.resize(coeffCount, decomposeModCount);
        destination.resizeRows(coeffCount);
        for (int galoisElt : galoisElts) {

//            System.out.println("galoisElt: " + galoisElt);


            // Verify coprime conditions.
            // 最低位为0, 则一定是偶数
            if ((galoisElt & 1) == 0 || (galoisElt >= coeffCount << 1)) {
                throw new IllegalArgumentException("Galois element is not valid");
            }
            // Do we already have the key?
            if (destination.hasKey(galoisElt)) {
                continue;
            }
            // Rotate secret key for each coeff_modulus
            long[] rotatedSecretKey = new long[coeffModulusSize * coeffCount];
            galoisTool.applyGaloisNttRnsIter(secretKey.data().getData(), coeffCount, coeffModulusSize, galoisElt, rotatedSecretKey, coeffCount);

            // Initialize Galois key
            // This is the location in the galois_keys vector
            int index = GaloisKeys.getIndex(galoisElt);

            // Create Galois keys.
            // 注意这里是把整个 二维数组 PublicKey[][] 传进去，然后用 index 来指定哪一行
            generateOneKeySwitchKey(rotatedSecretKey, destination.data(), index, saveSeed);

//            generateOneKeySwitchKey(rotatedSecretKey, destination.data(index), saveSeed);
        }

        // Set the parmsId
        destination.setParmsId(contextData.getParmsId().clone());

    }

    private GaloisKeys createGaloisKeys(int[] galoisElts, boolean saveSeed) {
        if (!skGenerated) {
            throw new IllegalArgumentException("cannot generate Galois keys for unspecified secret key");
        }

        Context.ContextData contextData = context.keyContextData();
        EncryptionParams parms = contextData.getParms();
        Modulus[] coeffModulus = parms.getCoeffModulus();
        GaloisTool galoisTool = contextData.getGaloisTool();
        int coeffCount = parms.getPolyModulusDegree();
        int coeffModulusSize = coeffModulus.length;

        if (!Common.productFitsIn(false, coeffCount, coeffModulusSize, 2)) {
            throw new IllegalArgumentException("invalid parameters");
        }

        GaloisKeys galoisKeys = new GaloisKeys();
//        int decomposeModCount = context.firstContextData().getParms().getCoeffModulus().length;
        // The max number of keys is equal to number of coefficients
//        galoisKeys.resize(coeffCount, decomposeModCount);
        galoisKeys.resizeRows(coeffCount);

        for (int galoisElt : galoisElts) {

//            System.out.println("galoisElt: " + galoisElt);
            // Verify coprime conditions.
            // 最低位为0, 则一定是偶数
            if ((galoisElt & 1) == 0 || (galoisElt >= (coeffCount << 1))) {
                throw new IllegalArgumentException("Galois element is not valid");
            }
            // Do we already have the key?
            if (galoisKeys.hasKey(galoisElt)) {
                continue;
            }
            // Rotate secret key for each coeff_modulus
            long[] rotatedSecretKey = new long[coeffModulusSize * coeffCount];
            galoisTool.applyGaloisNttRnsIter(secretKey.data().getData(), coeffCount, coeffModulusSize, galoisElt, rotatedSecretKey, coeffCount);

            // Initialize Galois key
            // This is the location in the galois_keys vector
            int index = GaloisKeys.getIndex(galoisElt);

            // Create Galois keys.
            // 一定要注意 galoisKeys.data(index) 和 galoisKeys.data()[index] 是不同的语义
            generateOneKeySwitchKey(rotatedSecretKey, galoisKeys.data(), index, saveSeed);
        }

        // Set the parmsId
        galoisKeys.setParmsId(contextData.getParmsId().clone());

        return galoisKeys;
    }


    private void computeSecretKeyArray(Context.ContextData contextData, int maxPower) {

        assert maxPower >= 1;
        assert (secretKeyArraySize > 0 && secretKeyArray != null);

        EncryptionParams parms = contextData.getParms();
        Modulus[] coeffModulus = parms.getCoeffModulus();
        int coeffCount = parms.getPolyModulusDegree();
        int coeffModulusSize = coeffModulus.length;

        // size check
        // todo: really need?
        if (!Common.productFitsIn(false, coeffCount, coeffModulusSize, maxPower)) {
            throw new IllegalArgumentException("invalid parameter");
        }

        int oldSize = secretKeyArraySize;
        int newSize = Math.max(oldSize, maxPower);
        if (oldSize == newSize) {
            return;
        }
        // Need to extend the array
        // Compute powers of secret key until max_power

        // 分配足够大的长度
        long[] newSecretKeyArray = new long[newSize * coeffCount * coeffModulusSize];
        // 先拷贝 old this.secretKeyArray 中的数据
        System.arraycopy(this.secretKeyArray, 0, newSecretKeyArray, 0, this.secretKeyArray.length);

        // Since all of the key powers in secret_key_array_ are already NTT transformed, to get the next one we simply
        // need to compute a dyadic product of the last one with the first one [which is equal to NTT(secret_key_)].
        // 计算 sk^1 sk^2 sk^3 ....
        // 计算逻辑是 sk^2 = sk * sk,  sk^3 = sk^2 * sk

        // 注意 i 的起点
        // secretKeyArray: [0, 1 * k * N), [1*k*N, 2*k*N), ..., [(oldSize - 1) * k * N , oldSize * k * N)
        // newSecretKeyArray: [0, 1 * k * N), [1*k*N, 2*k*N), ..., [(oldSize - 1) * k * N , oldSize * k * N),...,[(newSize - 1) * k * N , newSize * k * N)
        // 现在的计算逻辑是这样：newSecretKeyArray[ oldSize * k * N , (oldSize + 1) * k * N ) = newSecretKeyArray[(oldSize - 1) * k * N , oldSize * k * N) *  secretKeyArray[(oldSize - 1) * k * N , oldSize * k * N)
        // 注意 第二项的起点 是不变的

        int oldStartIndex = (oldSize - 1) * coeffCount * coeffModulusSize;
        // 注意到这里是没办法并发的，后一个计算结果 依赖于 前一个计算结果
        for (int i = oldSize - 1; i < oldSize - 1 + newSize - oldSize; i++) {

            int newStartIndex = i * coeffCount * coeffModulusSize;
            int newStartIndexPlusOne = (i + 1) * coeffCount * coeffModulusSize;

            PolyArithmeticSmallMod.dyadicProductCoeffModRnsIter(
                    newSecretKeyArray,
                    newStartIndex,
                    secretKeyArray,
                    oldStartIndex,
                    coeffModulusSize,
                    coeffCount,
                    coeffModulus,
                    newStartIndexPlusOne,
                    newSecretKeyArray
            );
        }


        // Do we still need to update size?
//        oldSize = secretKeyArraySize;
//        newSize = Math.max(oldSize, maxPower);
//        if (oldSize == newSize) {
//            return;
//        }
        // update size and array
        secretKeyArraySize = newSize;
        secretKeyArray = newSecretKeyArray;
    }


}
