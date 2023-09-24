package edu.alibaba.mpc4j.crypto.fhe.utils;

import edu.alibaba.mpc4j.crypto.fhe.Ciphertext;
import edu.alibaba.mpc4j.crypto.fhe.Plaintext;
import edu.alibaba.mpc4j.crypto.fhe.context.Context;
import edu.alibaba.mpc4j.crypto.fhe.keys.*;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.params.EncryptionParams;
import edu.alibaba.mpc4j.crypto.fhe.params.ParmsIdType;
import edu.alibaba.mpc4j.crypto.fhe.params.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.zq.Common;

/**
 * ref: seal/valcheck.h
 *
 * @author Qixian Zhou
 * @date 2023/9/19
 */
public class ValueChecker {

    public static boolean isValidFor(Plaintext in, Context context) {
        return isBufferValid(in) && isDataValidFor(in, context);
    }

    public static boolean isValidFor(Ciphertext in, Context context) {
        return isBufferValid(in) && isDataValidFor(in, context);
    }

    public static boolean isValidFor(SecretKey in, Context context) {
        return isBufferValid(in) && isDataValidFor(in, context);
    }

    public static boolean isValidFor(PublicKey in, Context context) {
        return isBufferValid(in) && isDataValidFor(in, context);
    }

    public static boolean isValidFor(KeySwitchKeys in, Context context) {
        return isBufferValid(in) && isDataValidFor(in, context);
    }

    public static boolean isValidFor(RelinKeys in, Context context) {
        return isBufferValid(in) && isDataValidFor(in, context);
    }

    public static boolean isValidFor(GaloisKeys in, Context context) {
        return isBufferValid(in) && isDataValidFor(in, context);
    }

    /**
     * Check whether the given plaintext data buffer is valid for a given SEALContext.
     * If the given SEALContext is not set, the encryption parameters are invalid,
     * or the plaintext data buffer does not match the SEALContext, this function
     * returns false. Otherwise, returns true. This function only checks the size of
     * the data buffer and not the plaintext data itself.
     *
     * @param in
     * @return
     */
    public static boolean isBufferValid(Plaintext in) {
        // N
        return in.getCoeffCount() == in.getDynArray().size();
    }

    /**
     * Check whether the given ciphertext data buffer is valid for a given SEALContext.
     * If the given SEALContext is not set, the encryption parameters are invalid,
     * or the ciphertext data buffer does not match the SEALContext, this function
     * returns false. Otherwise, returns true. This function only checks the size of
     * the data buffer and not the ciphertext data itself.
     *
     * @param in
     * @return
     */
    public static boolean isBufferValid(Ciphertext in) {
        // TODO: must be use mulSafe?
        // size * k * N
        return in.getDynArray().size() == Common.mulSafe(in.getSize(), in.getCoeffModulusSize(), false, in.getPolyModulusDegree());
    }

    /**
     * Check whether the given secret key data buffer is valid for a given SEALContext.
     * If the given SEALContext is not set, the encryption parameters are invalid,
     * or the secret key data buffer does not match the SEALContext, this function
     * returns false. Otherwise, returns true. This function only checks the size of
     * the data buffer and not the secret key data itself.
     *
     * @param in
     * @return
     */
    public static boolean isBufferValid(SecretKey in) {
        return isBufferValid(in.data());
    }

    public static boolean isBufferValid(PublicKey in) {
        return isBufferValid(in.data());
    }

    public static boolean isBufferValid(KeySwitchKeys in) {

        for (PublicKey[] a : in.data()) {
            for (PublicKey b : a) {
                if (!isBufferValid(b)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isBufferValid(RelinKeys in) {
        return isBufferValid((KeySwitchKeys) in);
    }

    public static boolean isBufferValid(GaloisKeys in) {
        return isBufferValid((KeySwitchKeys) in);
    }


    public static boolean isDataValidFor(Plaintext in, Context context) {

        if (!isMetaDataValidFor(in, context)) {
            return false;
        }

        // check the data
        if (in.isNttForm()) {
            // 如果当前明文是 NTT form,则 明文是 一个 k * N 的 数组
            Context.ContextData contextData = context.getContextData(in.getParmsId());
            EncryptionParams params = contextData.getParms();
            Modulus[] coeffModulus = params.getCoeffModulus();
            int coeffModulusSize = coeffModulus.length;

            long[] inData = in.getData();
            int inDataIndex = 0;
            // inData 的元素 必须小于每一个 modulus
            // 具体来说 inData[i * N, (i+1) *N) < modulus[i]
            // i \in [0, k)
            for (int i = 0; i < coeffModulusSize; i++) {

                long modulus = coeffModulus[i].getValue();
                int polyModulusDegree = params.getPolyModulusDegree();
                while (polyModulusDegree-- > 0) {
                    if (inData[inDataIndex++] > modulus) {
                        return false;
                    }
                }
            }
        } else {
            // 非 NTT，那么明文就是 一个长度为 N 的多项式
            EncryptionParams params = context.firstContextData().getParms();
            long modulus = params.getPlainModulus().getValue();
            long[] inData = in.getData();
            int size = in.getCoeffCount();
            for (int i = 0; i < size; i++) {
                if (inData[i] >= modulus) {
                    return false;
                }
            }
        }
        return true;
    }


    public static boolean isDataValidFor(Ciphertext in, Context context) {

        if (!isMetaDataValidFor(in, context)) {
            return false;
        }

        // check the data
        Context.ContextData contextData = context.getContextData(in.getParmsId());
        Modulus[] coeffModulus = contextData.getParms().getCoeffModulus();
        int coeffModulusSize = coeffModulus.length;

        long[] inData = in.getData();
        int inDataIndx = 0;
        int size = in.getSize();
        // size 个 poly
        for (int i = 0; i < size; i++) {
            // 每一个 poly 都在 k 个 modulus 的 RNS 下
            for (int j = 0; j < coeffModulusSize; j++) {
                long modulus = coeffModulus[j].getValue();
                int polyModulusDegree = in.getPolyModulusDegree();
                while (polyModulusDegree-- > 0) {
                    if (inData[inDataIndx++] >= modulus) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public static boolean isDataValidFor(SecretKey in, Context context) {

        if (!isMetaDataValidFor(in, context)) {
            return false;
        }

        // check the data
        Context.ContextData contextData = context.keyContextData();
        EncryptionParams parms = contextData.getParms();
        Modulus[] coeffModulus = parms.getCoeffModulus();
        int coeffModulusSize = coeffModulus.length;

        long[] inData = in.data().getData();
        int inDataIndex = 0;

        for (int i = 0; i < coeffModulusSize; i++) {
            long modulus = coeffModulus[i].getValue();
            int polyModulusDegree = parms.getPolyModulusDegree();
            while (polyModulusDegree-- > 0) {
                if (inData[inDataIndex++] >= modulus) {
                    return false;
                }
            }
        }
        return true;
    }


    public static boolean isDataValidFor(PublicKey in, Context context) {

        if (!isMetaDataValidFor(in, context)) {
            return false;
        }

        // check the data
        Context.ContextData contextData = context.keyContextData();
        Modulus[] coeffModulus = contextData.getParms().getCoeffModulus();
        int coeffModulusSize = coeffModulus.length;

        long[] inData = in.data().getData();
        int inDataIndex = 0;
        int size = in.data().getSize();

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < coeffModulusSize; j++) {
                long modulus = coeffModulus[j].getValue();
                int polyModulusDegree = in.data().getPolyModulusDegree();
                while (polyModulusDegree-- > 0) {
                    if (inData[inDataIndex++] >= modulus) {
                        return false;
                    }
                }
            }
        }
        return true;
    }


    public static boolean isDataValidFor(KeySwitchKeys in, Context context) {

        if (!context.isParametersSet()) {
            return false;
        }

        if (!in.parmsId().equals(context.getKeyParmsId())) {
            return false;
        }

        for (PublicKey[] a : in.data()) {
            for (PublicKey b : a) {

                // Check that b is a valid public key; this also checks that its
                // parms_id matches key_parms_id.
                if (!isDataValidFor(b, context)) {
                    return false;
                }
            }
        }
        return true;
    }


    public static boolean isDataValidFor(RelinKeys in, Context context) {

        return isDataValidFor((KeySwitchKeys) in, context);
    }

    public static boolean isDataValidFor(GaloisKeys in, Context context) {

        return isDataValidFor((KeySwitchKeys) in, context);
    }


    public static boolean isMetaDataValidFor(Plaintext in, Context context, boolean allowPureKeyLevels) {

        if (!context.isParametersSet()) {
            return false;
        }

        if (in.isNttForm()) {
            // Are the parameters valid for the plaintext?
            Context.ContextData contextData = context.getContextData(in.getParmsId());
            if (contextData == null) {
                return false;
            }
            // Check whether the parms_id is in the pure key range
            boolean isParamsPureKey = contextData.getChainIndex() > context.firstContextData().getChainIndex();
            if (!allowPureKeyLevels && isParamsPureKey) {
                return false;
            }

            EncryptionParams parms = contextData.getParms();
            Modulus[] coeffModulus = parms.getCoeffModulus();
            int polyModulusDegree = parms.getPolyModulusDegree();
            // Check that coeff_count is appropriately set
            // todo: need mul safe?
            if (Common.mulSafe(coeffModulus.length, polyModulusDegree, false) != in.getCoeffCount()) {
                return false;
            }
        } else {
            EncryptionParams parms = context.firstContextData().getParms();
            int polyModulusDegree = parms.getPolyModulusDegree();
            if (in.getCoeffCount() > polyModulusDegree) {
                return false;
            }
        }

        return true;
    }

    public static boolean isMetaDataValidFor(Ciphertext in, Context context, boolean allowPureKeyLevels) {

        if (!context.isParametersSet()) {
            return false;
        }

        // Are the parameters valid for the ciphertext?
        Context.ContextData contextData = context.getContextData(in.getParmsId());
        if (contextData == null) {
            return false;
        }

        // Check whether the parms_id is in the pure key range
        boolean isParamsPureKey = contextData.getChainIndex() > context.firstContextData().getChainIndex();
        if (!allowPureKeyLevels && isParamsPureKey) {
            return false;
        }

        // Check that the metadata matches
        Modulus[] coeffModulus = contextData.getParms().getCoeffModulus();
        int polyModulusDegree = contextData.getParms().getPolyModulusDegree();

        if ((coeffModulus.length != in.getCoeffModulusSize()) || (polyModulusDegree != in.getPolyModulusDegree())) {
            return false;
        }

        // Check that size is either 0 or within right bounds
        int size = in.getSize();
        if ((size < Constants.CIPHERTEXT_SIZE_MIN && size != 0) || (size > Constants.CIPHERTEXT_SIZE_MAX)) {
            return false;
        }

        // Check that scale is 1.0 in BFV and BGV or not 0.0 in CKKS
        double scale = in.getScale();
        SchemeType scheme = context.firstContextData().getParms().getScheme();
        if (((scale != 1.0) && (scheme == SchemeType.BFV || scheme == SchemeType.BGV)) ||
                (scale == 0.0 && scheme == SchemeType.CKKS)
        ) {
            return false;
        }

        // Check that correction factor is 1 in BFV and CKKS or within the right bound in BGV
        long correctionFactor = in.getCorrectionFactor();
        long plainModulus = context.firstContextData().getParms().getPlainModulus().getValue();

        if (
                ((correctionFactor != 1) && (scheme == SchemeType.BFV || scheme == SchemeType.BGV))
                        || ((correctionFactor == 0 || correctionFactor > plainModulus) && scheme == SchemeType.BGV)

        ) {
            return false;
        }

        return true;
    }

    /**
     * allowPureKeyLevels is default false
     *
     * @param in
     * @param context
     * @return
     */
    public static boolean isMetaDataValidFor(Plaintext in, Context context) {

        return isMetaDataValidFor(in, context, false);
    }


    public static boolean isMetaDataValidFor(Ciphertext in, Context context) {

        return isMetaDataValidFor(in, context, false);
    }


    public static boolean isMetaDataValidFor(SecretKey in, Context context) {

        // Note: we check the underlying Plaintext and allow pure key levels in
        // this check. Then, also need to check that the parms_id matches the
        // key level parms_id; this also means the Plaintext is in NTT form.

        ParmsIdType keyParmsId = context.getKeyParmsId();

        return isMetaDataValidFor(in.data(), context, true) && (in.parmsId().equals(keyParmsId));
    }

    public static boolean isMetaDataValidFor(PublicKey in, Context context) {

        // Note: we check the underlying Ciphertext and allow pure key levels in
        // this check. Then, also need to check that the parms_id matches the
        // key level parms_id, that the Ciphertext is in NTT form, and that the
        // size is minimal (i.e., SEAL_CIPHERTEXT_SIZE_MIN).

        ParmsIdType keyParmsId = context.getKeyParmsId();

        return isMetaDataValidFor(in.data(), context, true)
                && in.data().isNttForm()
                && (in.parmsId().equals(keyParmsId))
                && in.data().getSize() == Constants.CIPHERTEXT_SIZE_MIN;
    }

    public static boolean isMetaDataValidFor(KeySwitchKeys in, Context context) {


        if (!context.isParametersSet()) {
            return false;
        }

        if (!in.parmsId().equals(context.getKeyParmsId())) {
            return false;
        }

        int decompModCount = context.firstContextData().getParms().getCoeffModulus().length;

        for (PublicKey[] a : in.data()) {

            // Check that each highest level component has right size
            if (a.length > 0 && (a.length != decompModCount)) {
                return false;
            }

            for (PublicKey b : a) {

                // Check that b is a valid public key (metadata only); this also
                // checks that its parms_id matches key_parms_id.
                if (!isMetaDataValidFor(b, context)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isMetaDataValidFor(RelinKeys in, Context context) {
        // size 要么为 0，要么在 合理范围内
        boolean sizeCheck = in.size() == 0
                || (in.size() <= Constants.CIPHERTEXT_SIZE_MAX - 2 && in.size() >= Constants.CIPHERTEXT_SIZE_MIN - 2);


        return isMetaDataValidFor((KeySwitchKeys) in, context) && sizeCheck;
    }


    public static boolean isMetaDataValidFor(GaloisKeys in, Context context) {


        boolean metaDataCheck = isMetaDataValidFor((KeySwitchKeys) in, context);

        // size 要么为 0，要么在 合理范围内
        boolean sizeCheck = in.size() == 0
                || (in.size() <= context.keyContextData().getParms().getPolyModulusDegree());
        return metaDataCheck && sizeCheck;
    }


}
