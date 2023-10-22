package edu.alibaba.mpc4j.crypto.fhe.modulus;

/**
 * This class contains static methods for creating a plaintext modulus easily.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/a0fc0b732f44fa5242593ab488c8b2b3076a5f76/native/src/seal/modulus.h#L523
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/8/29
 */
public class PlainModulus {

    /**
     * Creates a prime number Modulus for use as plain_modulus encryption
     * parameter that supports batching with a given poly_modulus_degree.
     *
     * @param polyModulusDegree N
     * @param bitSize           bit-size of prime number
     * @return a modulus with prime number
     */
    public static Modulus batching(int polyModulusDegree, int bitSize) {

        return CoeffModulus.create(polyModulusDegree, bitSize);
    }

    /**
     * Creates several prime number Modulus elements that can be used as
     * plain_modulus encryption parameters, each supporting batching with a given
     * poly_modulus_degree.
     *
     * @param polyModulusDegree N
     * @param bitSizes          bit-size of prime numbers
     * @return a modulus array with prime numbers
     */
    public static Modulus[] batching(int polyModulusDegree, int[] bitSizes) {
        return CoeffModulus.create(polyModulusDegree, bitSizes);
    }

}
