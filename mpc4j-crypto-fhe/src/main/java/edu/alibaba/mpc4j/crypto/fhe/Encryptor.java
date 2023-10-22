package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.context.Context;
import edu.alibaba.mpc4j.crypto.fhe.keys.PublicKey;
import edu.alibaba.mpc4j.crypto.fhe.keys.SecretKey;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.params.EncryptionParams;
import edu.alibaba.mpc4j.crypto.fhe.params.ParmsIdType;
import edu.alibaba.mpc4j.crypto.fhe.params.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.rns.RnsTool;
import edu.alibaba.mpc4j.crypto.fhe.utils.RingLwe;
import edu.alibaba.mpc4j.crypto.fhe.utils.ScalingVariant;
import edu.alibaba.mpc4j.crypto.fhe.utils.ValueChecker;
import edu.alibaba.mpc4j.crypto.fhe.zq.Common;


/**
 * Encrypts Plaintext objects into Ciphertext objects. Constructing an Encryptor
 * requires a SEALContext with valid encryption parameters, the public key and/or
 * the secret key. If an Encrytor is given a secret key, it supports symmetric-key
 * encryption. If an Encryptor is given a public key, it supports asymmetric-key
 * encryption.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/encryptor.h
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/9/25
 */
public class Encryptor {

    private Context context;

    private PublicKey publicKey;
    // 为何 加密操作符，能够持有 私钥呢？
    private SecretKey secretKey;

    public Encryptor(Context context, PublicKey publicKey) {

        this.context = context;
        if (!context.isParametersSet()) {
            throw new IllegalArgumentException("encryption parameters are not set correctly");
        }

        setPublicKey(publicKey);

        EncryptionParams parms = context.keyContextData().getParms();
        Modulus[] coeffModulus = parms.getCoeffModulus();
        int coeffCount = parms.getPolyModulusDegree();
        int coeffModulusSize = coeffModulus.length;
        // todo: really need check?
        if (!Common.productFitsIn(false, coeffCount, coeffModulusSize, 2)) {
            throw new IllegalArgumentException("invalid parameters");
        }
    }

    public Encryptor(Context context, SecretKey secretKey) {

        this.context = context;
        if (!context.isParametersSet()) {
            throw new IllegalArgumentException("encryption parameters are not set correctly");
        }

        setSecretKey(secretKey);

        EncryptionParams parms = context.keyContextData().getParms();
        Modulus[] coeffModulus = parms.getCoeffModulus();
        int coeffCount = parms.getPolyModulusDegree();
        int coeffModulusSize = coeffModulus.length;
        // todo: really need check?
        if (!Common.productFitsIn(false, coeffCount, coeffModulusSize, 2)) {
            throw new IllegalArgumentException("invalid parameters");
        }
    }

    public Encryptor(Context context, PublicKey publicKey, SecretKey secretKey) {

        if (!context.isParametersSet()) {
            throw new IllegalArgumentException("encryption parameters are not set correctly");
        }

        this.context = context;
        setPublicKey(publicKey);
        setSecretKey(secretKey);

        EncryptionParams parms = context.keyContextData().getParms();
        Modulus[] coeffModulus = parms.getCoeffModulus();
        int coeffCount = parms.getPolyModulusDegree();
        int coeffModulusSize = coeffModulus.length;
        // todo: really need check?
        if (!Common.productFitsIn(false, coeffCount, coeffModulusSize, 2)) {
            throw new IllegalArgumentException("invalid parameters");
        }
    }

    /**
     * ncrypts a plaintext with the public key and stores the result in
     * destination.
     * <p>
     * The encryption parameters for the resulting ciphertext correspond to:
     * 1) in BFV/BGV, the highest (data) level in the modulus switching chain,
     * 2) in CKKS, the encryption parameters of the plaintext.
     * Dynamic memory allocations in the process are allocated from the memory
     * pool pointed to by the given MemoryPoolHandle.
     *
     * @param plain       The plaintext to encrypt
     * @param destination The ciphertext to overwrite with the encrypted plaintext
     */
    public void encrypt(Plaintext plain, Ciphertext destination) {
        encryptInternal(plain, true, false, destination);
    }


    /**
     * todo: change the return value type to Serializable<Ciphertext>
     *
     * @param plain
     * @return
     */
    public Ciphertext encrypt(Plaintext plain) {
        Ciphertext destination = new Ciphertext();

        encryptInternal(plain, true, false, destination);
        return destination;
    }

    /**
     * Encrypts a zero plaintext with the public key and stores the result in
     * destination.
     * <p>
     * The encryption parameters for the resulting ciphertext correspond to the
     * highest (data) level in the modulus switching chain.
     *
     * @param destination The ciphertext to overwrite with the encrypted plaintext
     */
    public void encryptZero(Ciphertext destination) {
        // todo: 为什么使用 first parms 来加密？
        encryptZero(context.getFirstParmsId(), destination);
    }

    /**
     * todo: change the return value type to Serializable<Ciphertext>
     *
     * @return
     */
    public Ciphertext encryptZero() {

        Ciphertext destination = new Ciphertext();
        encryptZero(context.getFirstParmsId(), destination);
        return destination;
    }

    /**
     * Encrypts a zero plaintext with the public key and stores the result in
     * destination.
     * <p>
     * The encryption parameters for the resulting ciphertext correspond to the
     * given parms_id. Dynamic memory allocations in the process are allocated
     * from the memory pool pointed to by the given MemoryPoolHandle.
     *
     * @param parmsId     The parms_id for the resulting ciphertext
     * @param destination The ciphertext to overwrite with the encrypted plaintext
     */
    public void encryptZero(ParmsIdType parmsId, Ciphertext destination) {
        encryptZeroInternal(parmsId, true, false, destination);
    }


    /**
     * todo: change the return value type to Serializable<Ciphertext>
     *
     * @param parmsId
     * @return
     */
    public Ciphertext encryptZero(ParmsIdType parmsId) {

        Ciphertext destination = new Ciphertext();
        encryptZeroInternal(parmsId, true, false, destination);
        return destination;
    }


    /**
     * Encrypts a plaintext with the secret key and stores the result in
     * destination.
     * <p>
     * The encryption parameters for the resulting ciphertext correspond to:
     * 1) in BFV/BGV, the highest (data) level in the modulus switching chain,
     * 2) in CKKS, the encryption parameters of the plaintext.
     * Dynamic memory allocations in the process are allocated from the memory
     * pool pointed to by the given MemoryPoolHandle.
     *
     * @param plain       The plaintext to encrypt
     * @param destination The ciphertext to overwrite with the encrypted plaintext
     */
    public void encryptSymmetric(Plaintext plain, Ciphertext destination) {
        encryptInternal(plain, false, false, destination);
    }

    /**
     * todo: change the return value type to Serializable<Ciphertext>
     *
     * @param plain
     * @return
     */
    public Ciphertext encryptSymmetric(Plaintext plain) {

        Ciphertext destination = new Ciphertext();
        encryptInternal(plain, false, false, destination);
        return destination;
    }


    /**
     * Encrypts a zero plaintext with the secret key and stores the result in
     * destination.
     * <p>
     * The encryption parameters for the resulting ciphertext correspond to the
     * given parms_id. Dynamic memory allocations in the process are allocated
     * from the memory pool pointed to by the given MemoryPoolHandle.
     *
     * @param parmsId     The parms_id for the resulting ciphertext
     * @param destination The ciphertext to overwrite with the encrypted plaintext
     */
    public void encryptZeroSymmetric(ParmsIdType parmsId, Ciphertext destination) {
        encryptZeroInternal(parmsId, false, false, destination);
    }

    /**
     * todo: change the return value type to Serializable<Ciphertext>
     *
     * @param parmsId
     * @return
     */
    public Ciphertext encryptZeroSymmetric(ParmsIdType parmsId) {

        Ciphertext destination = new Ciphertext();
        encryptZeroInternal(parmsId, false, false, destination);
        return destination;
    }


    public void encryptZeroSymmetric(Ciphertext destination) {
        encryptZeroSymmetric(context.getFirstParmsId(), destination);
    }

    public Ciphertext encryptZeroSymmetric() {

        return encryptZeroSymmetric(context.getFirstParmsId());
    }


    /**
     * Give a new instance of secret key.
     *
     * @param publicKey The public key
     */
    public void setPublicKey(PublicKey publicKey) {

        if (!ValueChecker.isValidFor(publicKey, context)) {
            throw new IllegalArgumentException("public key is not valid for encryption parameters");
        }
        this.publicKey = publicKey;
    }

    /**
     * Give a new instance of secret key.
     *
     * @param secretKey The secret key
     */
    public void setSecretKey(SecretKey secretKey) {

        if (!ValueChecker.isValidFor(secretKey, context)) {
            throw new IllegalArgumentException("public key is not valid for encryption parameters");
        }
        this.secretKey = secretKey;
    }


    private void encryptInternal(Plaintext plain, boolean isAsymmetric, boolean saveSeed, Ciphertext destination) {

        // Minimal verification that the keys are set
        if (isAsymmetric) { // 非对称加密用 public key
            if (!ValueChecker.isMetaDataValidFor(publicKey, context)) {
                throw new IllegalArgumentException("public key is not set");
            }
        } else { // 对称加密 用 secret key
            if (!ValueChecker.isMetaDataValidFor(secretKey, context)) {
                throw new IllegalArgumentException("secret key is not set");
            }
        }

        // Verify that plain is valid
        if (!ValueChecker.isValidFor(plain, context)) {
            throw new IllegalArgumentException("plain is not valid for encryption parameters");
        }

        SchemeType scheme = context.keyContextData().getParms().getScheme();
        if (scheme == SchemeType.BFV) {

            if (plain.isNttForm()) {
                throw new IllegalArgumentException("plain cannot be in NTT form");
            }

            encryptZeroInternal(context.getFirstParmsId(), isAsymmetric, saveSeed, destination);

            // Multiply plain by scalar coeff_div_plaintext and reposition if in upper-half.
            // Result gets added into the c_0 term of ciphertext (c_0,c_1).
            // \Delta = q / t (coeff_div_plaintext), \Delta * m
            // 这里只处理 c0, 注意函数签名和起点
            // 这里就是计算 c0 + \Delta * m , 前面的 C0 是对 zero 的加密
            // 到这里就是完整的加密
            ScalingVariant.multiplyAddPlainWithScalingVariant(
                    plain,
                    context.firstContextData(),
                    destination.getData(),
                    destination.getPolyModulusDegree(),
                    0
            );

//            System.out.println("destination after scaling: ");
//            System.out.println(Arrays.toString(destination.getData()));


        } else if (scheme == SchemeType.CKKS) {
            // todo: implement CKKS
            throw new IllegalArgumentException("now cannot support CKKS");
        } else if (scheme == SchemeType.BGV) {
            // todo: implement BGV
            throw new IllegalArgumentException("now cannot support BGV");
        } else {
            throw new IllegalArgumentException("unsupported scheme");
        }
    }


    private void encryptZeroInternal(ParmsIdType parmsId, boolean isAsymmetric, boolean saveSeed, Ciphertext destination) {


        Context.ContextData contextData = context.getContextData(parmsId);
        if (contextData == null) {
            throw new IllegalArgumentException("parmsId is not valid for encryption parameters");
        }

        EncryptionParams parms = contextData.getParms();
        int coeffModulusSize = parms.getCoeffModulus().length;
        int coeffCount = parms.getPolyModulusDegree();
        boolean isNttForm = false;

        if (parms.getScheme() == SchemeType.CKKS) {
            isNttForm = true;
            //todo: implement CKKS
            throw new IllegalArgumentException("now cannot support CKKS");
        } else if (parms.getScheme() != SchemeType.BFV && parms.getScheme() != SchemeType.BGV) {
            throw new IllegalArgumentException("unsupported scheme");
        }
        // Resize destination and save results
        destination.resize(context, parmsId, 2);
        // If asymmetric key encryption 非对称加密
        if (isAsymmetric) {
            // firstContext  前面是 keyContext
            Context.ContextData prevContextData = contextData.getPreContextData();
            // todo: 理解这里的逻辑， 为什么要获取 prevContextData
            if (prevContextData != null) {
                // Requires modulus switching
                ParmsIdType prevParmsId = prevContextData.getParmsId();
                RnsTool rnsTool = prevContextData.getRnsTool();

                // Zero encryption without modulus switching
                Ciphertext temp = new Ciphertext();
                // temp 底层的数组没有分配长度，可能会出问题， 不用担心，会在函数内部被 resize
                // 这里密文对应的又是 keyParmsId
                RingLwe.encryptZeroAsymmetric(publicKey, context, prevParmsId, isNttForm, temp);
                // Modulus switching
                // 这里需要按 密文中多项式数量来迭代，又需要处理 RnsIter/PolyIter, 这里涉及到 RnsTool, 就非常麻烦了
                // 但是突然想到，我的 RnsIter 只持有数组的一个引用，整体上的开销也还好？
                // 不是这样的，现在密文 的底层数组是一个整体，长度是 size * k * N, 目前的RnsIter 底层数组的长度设计的是
                // k * N , 这里又涉及到数组切分, 需要 new long[] 的操作，还是开销会很大
                // 目前看来，最高效的方式 还是 long[] + 辅助信息 来分别表示 RnsIter/PolyIter
                // 再通过 long[] + startIndex 来依次处理每一个 coeffCount
                // 这里的问题是，这里依次处理 每一个 coeffCount 是在函数调用处处理, 还是封装到函数内部？
                // 可能封装到函数内部更合理，因为调用处就不需要关心细节了，只需要知道 我这里想要处理的是 RnsIter 还是 PolyIter
                // 直接扔给对应的函数处理即可
                // 总之这里还没完全想清楚怎么设计比较好，先把功能性的东西写对吧
                // 想来想去，这里的复杂度并不会留给用户，而是库的开发者 底层所有的函数 只处理单个 CoeffIter即可，上层调用的时候
                // 需要自己计算起点
                // 遍历密文中的每一个poly(Rns)

//                System.out.println("temp: ");
//                System.out.println(Arrays.toString(temp.getData()));


                for (int i = 0; i < temp.getSize(); i++) {
                    if (isNttForm) {
                        // 注意函数签名
                        rnsTool.divideAndRoundQLastNttInplace(
                                temp.getData(),
                                temp.getPolyModulusDegree(),
                                temp.getCoeffModulusSize(),
                                temp.indexAt(i),
                                prevContextData.getSmallNttTables()
                        );
                    } else if (parms.getScheme() != SchemeType.BGV) { // bfv switch-to-next
                        // 直接处理整个 RnsIter
                        rnsTool.divideAndRoundQLastInplace(
                                temp.getData(),
                                temp.getPolyModulusDegree(),
                                temp.getCoeffModulusSize(),
                                temp.indexAt(i)
                        );
                    } else { // bgv switch-to-next
                        // todo: implement BGV
                        throw new IllegalArgumentException("now cannot support BGV");
                    }
                    // 处理完一个多项式，将结果拷贝回 destination, 注意起点
                    // 这里尤其尤其要注意！！temp 和 destination 是完全不同的密文！
                    // temp 密文对应的 ParmsId 是 prevParmsId
                    // 而 destination 对应的 ParmsId 是 输入的 parmsId
                    // 这就导致二者的 coeffModulusSize 不一样，那么 Copy 的时候就不正确！
                    // 举个更具体的例子， temp: size = 2, k = 2, N = 8
                    //                 destination: size = 2, k = 1, N = 8
                    // 这里用到的 coeffModulusSize 是 destination 的
                    // 这里在 copy 的时候 二者用的是相同的 coeffModulusSize, 这就导致出错！
                    // 要个用个的
//                    System.arraycopy(
//                            temp.getData(),
//                            i * coeffCount * coeffModulusSize,
//                            destination.getData(),
//                            i * coeffCount * coeffModulusSize,
//                            coeffCount * coeffModulusSize
//                    );
                    // 这是改正后的
                    // 就是这个地方，困扰了我很久很久！！
                    System.arraycopy(
                            temp.getData(),
                            i * coeffCount * temp.getCoeffModulusSize(),
                            destination.getData(),
                            i * coeffCount * destination.getCoeffModulusSize(),
                            coeffCount * destination.getCoeffModulusSize()
                    );

                }

//                System.out.println("after SEAL_ITERATE, destination: ");
//                System.out.println(Arrays.toString(destination.getData()));

                destination.setParmsId(parmsId.clone());
                destination.setIsNttForm(isNttForm);
                destination.setScale(temp.getScale());
                destination.setCorrectionFactor(temp.getCorrectionFactor());
            } else {
                // Does not require modulus switching
                RingLwe.encryptZeroAsymmetric(publicKey, context, parmsId, isNttForm, destination);
            }
        } else {
            // Does not require modulus switching
            RingLwe.encryptZeroSymmetric(secretKey, context, parmsId, isNttForm, saveSeed, destination);
        }
    }

}
