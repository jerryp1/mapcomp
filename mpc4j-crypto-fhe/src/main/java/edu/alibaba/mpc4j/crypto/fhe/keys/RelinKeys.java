package edu.alibaba.mpc4j.crypto.fhe.keys;

/**
 * Class to store relinearization keys.
 * <p>
 * Freshly encrypted ciphertexts have a size of 2, and multiplying ciphertexts
 * of sizes K and L results in a ciphertext of size K+L-1. Unfortunately, this
 * growth in size slows down further multiplications and increases noise growth.
 * Relinearization is an operation that has no semantic meaning, but it reduces
 * the size of ciphertexts back to 2. Microsoft SEAL can only relinearize size 3
 * ciphertexts back to size 2, so if the ciphertexts grow larger than size 3,
 * there is no way to reduce their size. Relinearization requires an instance of
 * RelinKeys to be created by the secret key owner and to be shared with the
 * evaluator. Note that plain multiplication is fundamentally different from
 * normal multiplication and does not result in ciphertext size growth.
 * <p>
 * Typically, one should always relinearize after each multiplications. However,
 * in some cases relinearization should be postponed as late as possible due to
 * its computational cost. For example, suppose the computation involves several
 * homomorphic multiplications followed by a sum of the results. In this case it
 * makes sense to not relinearize each product, but instead add them first and
 * only then relinearize the sum. This is particularly important when using the
 * CKKS scheme, where relinearization is much more computationally costly than
 * multiplications and additions.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/relinkeys.h
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/9/14
 */
public class RelinKeys extends KeySwitchKeys {


    public RelinKeys() {
        super();
    }


    public static int getIndex(int keyPower) {

        if (keyPower < 2) {
            throw new IllegalArgumentException("keyPower con not be less than 2");
        }

        return keyPower - 2;
    }

    /**
     * @param keyPower The power of the secret key
     * @return whether a relinearization key corresponding to a given power of
     * the secret key exists.
     */
    public boolean hasKey(int keyPower) {
        int index = getIndex(keyPower);

        return data().length > index && data(index).length > 0;
    }

    public PublicKey[] key(int keyPower) {

        return data(getIndex(keyPower));
    }


}
