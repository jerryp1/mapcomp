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

    /**
     * context
     */
    private final Context context;
    /**
     * public key
     */
    private PublicKey publicKey;
    /**
     * secret key
     */
    private SecretKey secretKey;

    /**
     * Creates an Encryptor instance initialized with the specified context and public key.
     *
     * @param context   the context.
     * @param publicKey the public key.
     */
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
        if (!Common.productFitsIn(false, coeffCount, coeffModulusSize, 2)) {
            throw new IllegalArgumentException("invalid parameters");
        }
    }

    /**
     * Creates an Encryptor instance initialized with the specified context and secret key.
     *
     * @param context   the context.
     * @param secretKey the secret key.
     */
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

    /**
     * Creates an Encryptor instance initialized with the specified context, secret key, and public key.
     *
     * @param context   the context.
     * @param publicKey the public key.
     * @param secretKey the secret key.
     */
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
     * Encrypts a plaintext with the public key and store the ciphertext in destination.
     *
     * The encryption parameters for the resulting ciphertext correspond to:
     * 1) in BFV/BGV, the highest (data) level in the modulus switching chain,
     * 2) in CKKS, the encryption parameters of the plaintext.
     * Dynamic memory allocations in the process are allocated from the memory
     * pool pointed to by the given MemoryPoolHandle.
     *
     * @param plain       the plaintext to encrypt.
     * @param destination the ciphertext to overwrite with the encrypted plaintext.
     */
    public void encrypt(Plaintext plain, Ciphertext destination) {
        // asymmetric encryption
        encryptInternal(plain, true, false, destination);
    }

    /**
     * Encrypts a plaintext with the public key and returns the ciphertext.
     * The encryption parameters for the resulting ciphertext correspond to the
     * highest (data) level in the modulus switching chain.
     *
     * @param plain the plaintext to encrypt.
     * @return the ciphertext.
     */
    public Ciphertext encrypt(Plaintext plain) {
        // todo: change the return value type to Serializable<Ciphertext>
        Ciphertext destination = new Ciphertext();
        encryptInternal(plain, true, false, destination);
        return destination;
    }

    /**
     * Encrypts a zero plaintext with the public key and stores the result in destination.
     * The encryption parameters for the resulting ciphertext correspond to the
     * highest (data) level in the modulus switching chain.
     *
     * @param destination the ciphertext to overwrite with the encrypted plaintext.
     */
    public void encryptZero(Ciphertext destination) {
        // todo: 为什么使用 first parms 来加密？
        encryptZero(context.getFirstParmsId(), destination);
    }

    /**
     * Encrypts a zero plaintext with the public key and returns the ciphertext.
     * The encryption parameters for the resulting ciphertext correspond to the
     * highest (data) level in the modulus switching chain.
     */
    public Ciphertext encryptZero() {
        // todo: change the return value type to Serializable<Ciphertext>
        Ciphertext destination = new Ciphertext();
        encryptZero(context.getFirstParmsId(), destination);
        return destination;
    }

    /**
     * Encrypts a zero plaintext with the public key and stores the result in destination.
     * The encryption parameters for the resulting ciphertext correspond to the given parms_id.
     *
     * @param parmsId     the parms_id for the resulting ciphertext.
     * @param destination the ciphertext to overwrite with the encrypted plaintext.
     */
    public void encryptZero(ParmsIdType parmsId, Ciphertext destination) {
        encryptZeroInternal(parmsId, true, false, destination);
    }

    /**
     * Encrypts a zero plaintext with the public key and returns the ciphertext.
     * The encryption parameters for the resulting ciphertext correspond to the given parms_id.
     *
     * @param parmsId the parms_id for the resulting ciphertext.
     */
    public Ciphertext encryptZero(ParmsIdType parmsId) {
        // todo: change the return value type to Serializable<Ciphertext>
        Ciphertext destination = new Ciphertext();
        encryptZeroInternal(parmsId, true, false, destination);
        return destination;
    }

    /**
     * Encrypts a plaintext with the secret key and stores the result in destination.
     * The encryption parameters for the resulting ciphertext correspond to:
     * 1) in BFV/BGV, the highest (data) level in the modulus switching chain,
     * 2) in CKKS, the encryption parameters of the plaintext.
     *
     * @param plain       the plaintext to encrypt.
     * @param destination the ciphertext to overwrite with the encrypted plaintext.
     */
    public void encryptSymmetric(Plaintext plain, Ciphertext destination) {
        encryptInternal(plain, false, false, destination);
    }

    /**
     * Encrypts a plaintext with the secret key and returns the ciphertext.
     * The encryption parameters for the resulting ciphertext correspond to the
     * highest (data) level in the modulus switching chain.
     *
     * @param plain the plaintext to encrypt.
     * @return the ciphertext.
     */
    public Ciphertext encryptSymmetric(Plaintext plain) {
        // todo: change the return value type to Serializable<Ciphertext>
        Ciphertext destination = new Ciphertext();
        encryptInternal(plain, false, false, destination);
        return destination;
    }

    /**
     * Encrypts a zero plaintext with the secret key and stores the result in destination.
     * The encryption parameters for the resulting ciphertext correspond to the given parms_id.
     *
     * @param parmsId     the parms_id for the resulting ciphertext.
     * @param destination the ciphertext to overwrite with the encrypted plaintext.
     */
    public void encryptZeroSymmetric(ParmsIdType parmsId, Ciphertext destination) {
        encryptZeroInternal(parmsId, false, false, destination);
    }

    /**
     * Encrypts a zero plaintext with the secret key and returns the ciphertext.
     * The encryption parameters for the resulting ciphertext correspond to the given parms_id.
     *
     * @param parmsId the parms_id for the resulting ciphertext.
     * @return the ciphertext.
     */
    public Ciphertext encryptZeroSymmetric(ParmsIdType parmsId) {
        // todo: change the return value type to Serializable<Ciphertext>
        Ciphertext destination = new Ciphertext();
        encryptZeroInternal(parmsId, false, false, destination);
        return destination;
    }

    /**
     * Encrypts a zero plaintext using the secret key and stores the result in destination.
     * The encryption parameters for the resulting ciphertext correspond to the
     * highest (data) level in the modulus switching chain.
     *
     * @param destination the ciphertext to overwrite with the encrypted plaintext.
     */
    public void encryptZeroSymmetric(Ciphertext destination) {
        encryptZeroSymmetric(context.getFirstParmsId(), destination);
    }

    /**
     * Encrypts a zero plaintext using the secret key and stores the result in destination.
     * The encryption parameters for the resulting ciphertext correspond to the
     * highest (data) level in the modulus switching chain.
     *
     * @return the ciphertext.
     */
    public Ciphertext encryptZeroSymmetric() {
        return encryptZeroSymmetric(context.getFirstParmsId());
    }

    /**
     * Gives a new instance of public key, if it is valid.
     *
     * @param publicKey the public key.
     */
    public void setPublicKey(PublicKey publicKey) {
        if (!ValueChecker.isValidFor(publicKey, context)) {
            throw new IllegalArgumentException("public key is not valid for encryption parameters");
        }
        this.publicKey = publicKey;
    }

    /**
     * Gives a new instance of secret key, if it is valid.
     *
     * @param secretKey the secret key.
     */
    public void setSecretKey(SecretKey secretKey) {
        if (!ValueChecker.isValidFor(secretKey, context)) {
            throw new IllegalArgumentException("public key is not valid for encryption parameters");
        }
        this.secretKey = secretKey;
    }

    /**
     * Encrypts a plaintext and stores the result in destination.
     * The encryption parameters for the resulting ciphertext correspond to the
     * highest (data) level in the modulus switching chain.
     *
     * @param plain        the plaintext to encrypt.
     * @param isAsymmetric whether the encryption is asymmetric.
     * @param saveSeed     whether the seed is saved.
     * @param destination  the ciphertext to overwrite with the encrypted plaintext.
     */
    private void encryptInternal(Plaintext plain, boolean isAsymmetric, boolean saveSeed, Ciphertext destination) {
        // Minimal verification that the keys are set
        if (isAsymmetric) {
            // encrypts using public key
            if (!ValueChecker.isMetaDataValidFor(publicKey, context)) {
                throw new IllegalArgumentException("public key is not set");
            }
        } else {
            // encrypts using secret key
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
            // (round(plain * q/t) + c0, c1)
            ScalingVariant.multiplyAddPlainWithScalingVariant(
                plain, context.firstContextData(), destination.getData(), destination.getPolyModulusDegree(), 0
            );
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

    /**
     * Encrypts a zero plaintext and stores the result in destination.
     * The encryption parameters for the resulting ciphertext correspond to the given parms_id.
     *
     * @param parmsId      the parms ID.
     * @param isAsymmetric whether the encryption is asymmetric.
     * @param saveSeed     whether the seed is saved.
     * @param destination  the ciphertext to overwrite with the encrypted plaintext.
     */
    private void encryptZeroInternal(ParmsIdType parmsId, boolean isAsymmetric, boolean saveSeed,
                                     Ciphertext destination) {
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
        if (isAsymmetric) {
            // firstContext  前面是 keyContext
            Context.ContextData prevContextData = contextData.getPreContextData();
            // todo: 理解这里的逻辑， 为什么要获取 prevContextData
            // 需要判断是否使用 modulus switching吧
            if (prevContextData != null) {
                // Requires modulus switching
                ParmsIdType prevParmsId = prevContextData.getParmsId();
                RnsTool rnsTool = prevContextData.getRnsTool();
                // Zero encryption without modulus switching
                Ciphertext temp = new Ciphertext();
                // temp 底层的数组没有分配长度，可能会出问题， 不用担心，会在函数内部被 resize
                // 这里密文对应的又是 keyParmsId
                // temp = (pk[i] * u + e[i]) in key RnsBase
                RingLwe.encryptZeroAsymmetric(publicKey, context, prevParmsId, isNttForm, temp);
                // Modulus switching
                for (int i = 0; i < temp.getSize(); i++) {
                    if (isNttForm) {
                        // temp in ciphertext RnsBase
                        rnsTool.divideAndRoundQLastNttInplace(
                            temp.getData(),
                            temp.indexAt(i),
                            temp.getPolyModulusDegree(),
                            temp.getCoeffModulusSize(),
                            prevContextData.getSmallNttTables()
                        );
                    } else if (parms.getScheme() != SchemeType.BGV) {
                        // bfv switch-to-next
                        rnsTool.divideAndRoundQLastInplace(
                            temp.getData(), temp.indexAt(i), temp.getPolyModulusDegree(), temp.getCoeffModulusSize()
                        );
                    } else {
                        // bgv switch-to-next
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
                    System.arraycopy(
                        temp.getData(),
                        i * coeffCount * temp.getCoeffModulusSize(),
                        destination.getData(),
                        i * coeffCount * destination.getCoeffModulusSize(),
                        coeffCount * destination.getCoeffModulusSize()
                    );
                }
                destination.setParmsId(parmsId.clone());
                destination.setIsNttForm(isNttForm);
                destination.setScale(temp.getScale());
                destination.setCorrectionFactor(temp.getCorrectionFactor());
            } else {
                // Does not require modulus switching
                RingLwe.encryptZeroAsymmetric(publicKey, context, parmsId, isNttForm, destination);
            }
        } else {
            // Does not require modulus switching, ciphertext in RnsBase of first parms ID
            RingLwe.encryptZeroSymmetric(secretKey, context, parmsId, isNttForm, saveSeed, destination);
        }
    }
}
