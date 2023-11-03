package edu.alibaba.mpc4j.crypto.fhe.ntt;

import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;

/**
 * This class is used to construct multiple NttTables objects based on multiple Modulus objects.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/ntt.cpp#L301
 * </p>
 *
 * @author Qixian Zhou, Weiran Liu
 * @date 2023/8/27
 */
public class NttTablesCreateIter {
    /**
     * Creates multiple NTT tables based on multiple modulus and stores in nttTables.
     *
     * @param coeffCountPower k, where n = 2^k.
     * @param modulusArray    modulus array.
     * @param nttTables       where to store the created NTT tables.
     */
    public static void createNttTables(int coeffCountPower, Modulus[] modulusArray, NttTables[] nttTables) {
        assert modulusArray.length == nttTables.length;
        for (int i = 0; i < modulusArray.length; i++) {
            nttTables[i] = new NttTables(coeffCountPower, modulusArray[i]);
        }
    }
}
