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

/**
 * Generates matching secret key and public key. An existing KeyGenerator can
 * also at any time be used to generate relinearization keys and Galois keys.
 * Constructing a KeyGenerator requires only a SEALContext.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/keygenerator.h
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/9/14
 */
public class KeyGenerator {

    /**
     * context
     */
    private Context context;
    /**
     * secret key
     */
    private SecretKey secretKey;
    /**
     * secret key array size
     */
    private int secretKeyArraySize = 0;
    /**
     * secret key array
     */
    private long[] secretKeyArray;
    /**
     * is sk generated
     */
    boolean skGenerated = false;

    /**
     * constructor.
     *
     * @param context context.
     */
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

    /**
     * constructor.
     *
     * @param context   context.
     * @param secretKey secret key.
     */
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

    /**
     * return secret key.
     *
     * @return secret key.
     */
    public SecretKey getSecretKey() {
        return secretKey;
    }

    /**
     * create secret keys array.
     *
     * @param isInitialized is secret key generated.
     */
    private void generateSk(boolean isInitialized) {
        // Extract encryption parameters.
        Context.ContextData contextData = context.keyContextData();
        EncryptionParams params = contextData.getParms();
        Modulus[] coeffModulus = params.getCoeffModulus();
        // N
        int coeffCount = params.getPolyModulusDegree();
        // k
        int coeffModulusSize = coeffModulus.length;
        if (!isInitialized) {
            secretKey = new SecretKey();
            skGenerated = false;
            // represent secret key in RNS form
            secretKey.data().resize(Common.mulSafe(coeffCount, coeffModulusSize, false));
            // Generate secret key, s \in [-1, 0, 1]
            RingLwe.samplePolyTernary(params.getRandomGeneratorFactory().create(), params, secretKey.data().getData());
            // get \phi^{i} and \phi^{-i} mod q
            NttTables[] nttTables = contextData.getSmallNttTables();
            // Transform the secret s into NTT representation.
            NttTool.nttNegacyclicHarveyRns(secretKey.data().getData(), coeffCount, coeffModulusSize, nttTables);
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

    /**
     * create public key.
     *
     * @param destination store public key.
     */
    public void createPublicKey(PublicKey destination) {
        generatePk(false, destination);
    }

    /**
     * create relinearization keys.
     *
     * @param destination store relinearization keys.
     */
    public void createRelinKeys(RelinKeys destination) {
        createRelinKeys(1, false, destination);
    }

    /**
     * create relinearization keys.
     *
     * @return relinearization keys.
     */
    public RelinKeys createRelinKeys() {
        // todo: convert return type to Serializable<RelinKeys>
        return createRelinKeys(1, false);
    }

    /**
     * create Galois keys.
     *
     * @param galoisElts  Galois elements.
     * @param destination store Galois keys.
     */
    public void createGaloisKeys(int[] galoisElts, GaloisKeys destination) {
        createGaloisKeys(galoisElts, false, destination);
    }

    /**
     * create Galois keys with default Galois elements.
     *
     * @param destination store Galois keys.
     */
    public void createGaloisKeys(GaloisKeys destination) {
        createGaloisKeys(context.keyContextData().getGaloisTool().getEltsAll(), destination);
    }

    /**
     * create Galois keys with default Galois elements.
     *
     * @return Galois keys
     */
    public GaloisKeys createGaloisKeys() {
        // todo: convert return type to Serializable<RelinKeys>
        return createGaloisKeys(context.keyContextData().getGaloisTool().getEltsAll());
    }

    /**
     * create Galois keys.
     *
     * @param galoisElts Galois elements.
     * @return Galois keys.
     */
    public GaloisKeys createGaloisKeys(int[] galoisElts) {
        // todo: convert return type to Serializable<RelinKeys>
        return createGaloisKeys(galoisElts, false);
    }

    /**
     * create Galois keys from steps.
     * @param steps       steps.
     * @param destination store Galois keys.
     */
    public void createGaloisKeysFromSteps(int[] steps, GaloisKeys destination) {
        if (!context.keyContextData().getQualifiers().isUsingBatching()) {
            throw new IllegalArgumentException("encryption parameters do not support batching");
        }
        createGaloisKeys(context.keyContextData().getGaloisTool().getEltsFromSteps(steps), false, destination);
    }

    /**
     * create public key.
     *
     * @param saveSeed save seed.
     * @return public key.
     */
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
        // Generate ciphertext: (c[0], c[1]) = ([-(as + e)]_q, a)
        RingLwe.encryptZeroSymmetric(secretKey, context, contextData.getParmsId(), true, saveSeed, publicKey.data());
        // set the parmsId
        // todo: really need deep-copy?
        publicKey.setParmsId(contextData.getParmsId().clone());
        return publicKey;
    }

    /**
     * create public key.
     *
     * @param saveSeed  save seed.
     * @param publicKey store public key.
     */
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
        // Generate ciphertext: (c[0], c[1]) = ([-(as + e)]_q, a)
        RingLwe.encryptZeroSymmetric(secretKey, context, contextData.getParmsId(), true, saveSeed, publicKey.data());
        // set the parmsId
        // todo: really need deep-copy?
        publicKey.setParmsId(contextData.getParmsId().clone());
    }

    /**
     * create one key-switch key.
     *
     * @param newKeys          new key.
     * @param destinations     store key-switch key.
     * @param destinationIndex destination index.
     * @param saveSeed         save seed.
     */
    private void generateOneKeySwitchKey(long[] newKeys, PublicKey[][] destinations, int destinationIndex, boolean saveSeed) {
        if (!context.isUsingKeySwitching()) {
            throw new IllegalArgumentException("key switching is not supported by the context");
        }
        int coeffCount = context.keyContextData().getParms().getPolyModulusDegree();
        // q_1, ..., q_{k-1}
        int decomposeModCount = context.firstContextData().getParms().getCoeffModulus().length;
        Context.ContextData keyContextData = context.keyContextData();
        EncryptionParams keyParms = keyContextData.getParms();
        // q_1, ..., q_{k-1}, q_k
        Modulus[] keyModulus = keyParms.getCoeffModulus();
        // todo: really need this check?
        if (!Common.productFitsIn(false, coeffCount, decomposeModCount)) {
            throw new IllegalArgumentException("invalid parameters");
        }
        destinations[destinationIndex] = new PublicKey[decomposeModCount];
        for (int i = 0; i < decomposeModCount; i++) {
            destinations[destinationIndex][i] = new PublicKey();
        }
        // 这里面是比较耗时的操作，所以考虑并发
        for (int i = 0; i < decomposeModCount; i++) {
            long[] temp = new long[coeffCount];
            // Generate ciphertext: (c[0], c[1]) = ([-(as + e)]_q, a), represented in NTT form. RnsBase is q_1, ..., q_{k-1}, q_k
            RingLwe.encryptZeroSymmetric(secretKey, context, keyContextData.getParmsId(), true, saveSeed, destinations[destinationIndex][i].data());
            // factor = q_{k-1} mod q_i
            long factor = UintArithmeticSmallMod.barrettReduce64(keyModulus[keyModulus.length - 1].getValue(), keyModulus[i]);
            // temp = key' * factor mod q_i
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(newKeys, i * coeffCount, coeffCount, factor, keyModulus[i], temp, 0);
            // (c[0], c[1]) = ([-(as + e) + new_key * q_k]_q, a), represented in NTT form. RnsBase is q_1, ..., q_{k-1}, q_k
            PolyArithmeticSmallMod.addPolyCoeffMod(
                destinations[destinationIndex][i].data().getData(),
                i * coeffCount, // 第 0个 RnsIter 中的 第 i 个 CoeffIter
                temp,
                0,
                coeffCount,
                keyModulus[i],
                destinations[destinationIndex][i].data().getData(),
                i * coeffCount
            );
        }
    }

    /**
     * generate one key-switch keys.
     *
     * @param newKeys          new keys.
     * @param startIndex       start index.
     * @param destinations     store key-switch keys.
     * @param destinationIndex destination index.
     * @param saveSeed         save seed.
     */
    private void generateOneKeySwitchKey(long[] newKeys, int startIndex, PublicKey[][] destinations, int destinationIndex,
                                         boolean saveSeed) {
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
        destinations[destinationIndex] = new PublicKey[decomposeModCount];
        for (int i = 0; i < decomposeModCount; i++) {
            destinations[destinationIndex][i] = new PublicKey();
        }
        for (int i = 0; i < decomposeModCount; i++) {
            long[] temp = new long[coeffCount];
            // destination: (c[0], c[1]) = ([-(as + e)]_q, a)
            RingLwe.encryptZeroSymmetric(secretKey, context, keyContextData.getParmsId(), true, saveSeed, destinations[destinationIndex][i].data());
            long factor = UintArithmeticSmallMod.barrettReduce64(keyModulus[keyModulus.length - 1].getValue(), keyModulus[i]);
            // new_keys * q_k mod q_i
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(newKeys, startIndex + i * coeffCount, coeffCount, factor, keyModulus[i], temp, 0);
            // ([-(as + e) + new_keys * q_k]_q_i, a)
            PolyArithmeticSmallMod.addPolyCoeffMod(destinations[destinationIndex][i].data().getData(), i * coeffCount, temp, 0, coeffCount, keyModulus[i], destinations[destinationIndex][i].data().getData(), i * coeffCount);
        }
    }

    /**
     * generate key-switch keys.
     *
     * @param newKeys            new keys.
     * @param newKeysCoeffCount  new keys' coeff count.
     * @param newKeysModulusSize new keys' modulus size.
     * @param numKeys            keys num.
     * @param destination        destination, store key-switch keys.
     * @param saveSeed           save seed.
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
        destination.resizeRows(numKeys);
        // store Enc(newKey * q_k) with NTT form in destination
        for (int i = 0; i < numKeys; i++) {
            generateOneKeySwitchKey(newKeys, i * coeffCount * coeffModulusSize, destination.data(), i, saveSeed);
        }
    }


    /**
     * generate key-switch keys.
     *
     * @param newKeys            new keys.
     * @param startIndex         start index.
     * @param newKeysCoeffCount  new keys' coeff count.
     * @param newKeysModulusSize new keys' modulus size.
     * @param numKeys            keys num.
     * @param destination        destination, store key-switch keys.
     * @param saveSeed           save seed.
     */
    private void generateKeySwitchKeys(long[] newKeys, int startIndex, int newKeysCoeffCount, int newKeysModulusSize,
        int numKeys, KeySwitchKeys destination, boolean saveSeed) {
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
        // resize the size of key-switch key
        destination.resizeRows(numKeys);
        // store Enc(newKey * q_k) with NTT form in destination
        for (int i = 0; i < numKeys; i++) {
            generateOneKeySwitchKey(newKeys, startIndex + i * coeffCount * coeffModulusSize, destination.data(), i, saveSeed);
        }
    }

    /**
     * create the specified number of relinearization keys.
     *
     * @param count       The number of relinearization keys to generate.
     * @param saveSeed    If true, save seed instead of a polynomial.
     * @param destination store relinearization keys.
     */
    private void createRelinKeys(int count, boolean saveSeed, RelinKeys destination) {
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
        // Make sure we have enough secret keys computed, sk^1, ..., sk^{count+1}
        computeSecretKeyArray(contextData, count + 1);
        // Assume the secret key is already transformed into NTT form.
        // [(-(a*s + e) + q_k * s^2), a] in RNS form, RNS base is {q_1, ..., q_{k-1}, q_k}
        // [(-(a*s + e) + q_k * s^2)_{q_1}, a_{q_1}], ..., [(-(a*s + e) + q_k * s^2)_{q_{k-1}}, a_{q_{k-1}}], [(-(a*s + e))_{q_k}, a_{q_{k}}]
        generateKeySwitchKeys(secretKeyArray, coeffCount * coeffModulusSize, coeffCount, coeffModulusSize, count, destination, saveSeed);
        // todo: really need deep-copy?
        destination.setParmsId(contextData.getParmsId().clone());
    }

    /**
     * create the specified number of relinearization keys.
     *
     * @param count    The number of relinearization keys to generate.
     * @param saveSeed If true, save seed instead of a polynomial.
     * @return relinearization keys.
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
        // Make sure we have enough secret keys computed, sk^1, ..., sk^{count+1}
        computeSecretKeyArray(contextData, count + 1);
        // Assume the secret key is already transformed into NTT form.
        RelinKeys relinKeys = new RelinKeys();
        // [(-(a*s + e) + q_k * s^2), a] in RNS form, RNS base is {q_1, ..., q_{k-1}, q_k}
        // [(-(a*s + e) + q_k * s^2)_{q_1}, a_{q_1}], ..., [(-(a*s + e) + q_k * s^2)_{q_{k-1}}, a_{q_{k-1}}], [(-(a*s + e))_{q_k}, a_{q_{k}}]
        // todo: qixian
        generateKeySwitchKeys(secretKeyArray, coeffCount * coeffModulusSize, coeffCount, coeffModulusSize, count, relinKeys, saveSeed);
        // todo: really need deep-copy?
        relinKeys.setParmsId(contextData.getParmsId().clone());
        return relinKeys;
    }

    /**
     * create Galois keys.
     *
     * @param galoisElts  Galois elements.
     * @param saveSeed    save seed.
     * @param destination store Galois keys.
     */
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
        //todo: really need check?
        if (!Common.productFitsIn(false, coeffCount, coeffModulusSize, 2)) {
            throw new IllegalArgumentException("invalid parameters");
        }
        // The max number of keys is equal to number of coefficients
        destination.resizeRows(coeffCount);
        for (int galoisElt : galoisElts) {
            // Verify coprime conditions.
            // 最低位为0, 则一定是偶数
            if ((galoisElt & 1) == 0 || (galoisElt >= coeffCount << 1)) {
                throw new IllegalArgumentException("Galois element is not valid");
            }
            // Do we already have the key?
            if (destination.hasKey(galoisElt)) {
                continue;
            }
            // Rotate secret key for each coeff_modulus, A RnsIter
            long[] rotatedSecretKey = new long[coeffModulusSize * coeffCount];
            galoisTool.applyGaloisNttRnsIter(secretKey.data().getData(), coeffCount, coeffModulusSize, galoisElt, rotatedSecretKey, coeffCount);
            // Initialize Galois key
            // This is the location in the galois_keys vector
            int index = GaloisKeys.getIndex(galoisElt);
            // Create Galois keys.
            // [(-(a*s + e) + q_k * s_rotation), a] in RNS form, RNS base is {q_1, ..., q_{k-1}, q_k}
            // [(-(a*s + e) + q_k * s_rotation)_{q_1}, a_{q_1}], ..., [(-(a*s + e) + q_k * s_rotation)_{q_{k-1}}, a_{q_{k-1}}], [(-(a*s + e))_{q_k}, a_{q_{k}}]
            generateOneKeySwitchKey(rotatedSecretKey, destination.data(), index, saveSeed);
        }
        // Set the parmsId
        destination.setParmsId(contextData.getParmsId().clone());
    }

    /**
     * create Galois keys.
     *
     * @param galoisElts  Galois elements.
     * @param saveSeed    save seed.
     * @return Galois keys.
     */
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
        // The max number of keys is equal to number of coefficients
        galoisKeys.resizeRows(coeffCount);
        for (int galoisElt : galoisElts) {
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

    /**
     * compute secret key array, s, s^2, s^3, ..., s^{max_power}.
     *
     * @param contextData context data.
     * @param maxPower    max power.
     */
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
        long[] newSecretKeyArray = new long[newSize * coeffCount * coeffModulusSize];
        // copy old this.secretKeyArray
        System.arraycopy(this.secretKeyArray, 0, newSecretKeyArray, 0, oldSize * coeffCount * coeffModulusSize);
        // Since all the key powers in secret_key_array_ are already NTT transformed, to get the next one we simply
        // need to compute a dyadic product of the last one with the first one [which is equal to NTT(secret_key_)].
        // compute sk^1, sk^2 = sk * sk, sk^3 = sk^2 * sk
        // secretKeyArray: [0, 1 * k * N), [1*k*N, 2*k*N), ..., [(oldSize - 1) * k * N , oldSize * k * N)
        // newSecretKeyArray: [0, 1 * k * N), [1*k*N, 2*k*N), ..., [(oldSize - 1) * k * N , oldSize * k * N),...,[(newSize - 1) * k * N , newSize * k * N)
        // newSecretKeyArray[ oldSize * k * N , (oldSize + 1) * k * N ) = newSecretKeyArray[(oldSize - 1) * k * N , oldSize * k * N) *  secretKeyArray[(oldSize - 1) * k * N , oldSize * k * N)
        int oldStartIndex = (oldSize - 1) * coeffCount * coeffModulusSize;
        for (int i = oldSize - 1; i < oldSize - 1 + newSize - oldSize; i++) {
            int newStartIndex = i * coeffCount * coeffModulusSize;
            int newStartIndexPlusOne = (i + 1) * coeffCount * coeffModulusSize;
            PolyArithmeticSmallMod.dyadicProductCoeffModRns(
                newSecretKeyArray,
                newStartIndex,
                coeffCount,
                coeffModulusSize,
                secretKeyArray,
                oldStartIndex,
                coeffCount,
                coeffModulusSize,
                coeffModulus,
                newSecretKeyArray,
                newStartIndexPlusOne,
                coeffCount,
                coeffModulusSize
            );
        }
        // update size and array
        secretKeyArraySize = newSize;
        secretKeyArray = newSecretKeyArray;
    }
}