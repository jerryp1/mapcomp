package edu.alibaba.mpc4j.crypto.fhe.ntt;

import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;

import java.util.stream.IntStream;

/**
 * This class is used to construct multiple NttTables objects based on multiple Modulus objects.
 * todo: Consider removing this class and implementing the corresponding static method directly in NttTables.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/ntt.cpp#L301
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/8/27
 */
public class NttTablesCreateIter {

    private int coeffCountPower;

    private Modulus[] modulus;

    public NttTablesCreateIter() {
    }

    public NttTablesCreateIter(int coeffCountPower, Modulus[] modulus) {
        this.coeffCountPower = coeffCountPower;
        this.modulus = modulus;
    }


    public static void createNttTables(int coeffCountPower, Modulus[] modulus, NttTables[] nttTables) {

        assert modulus.length == nttTables.length;

        for (int i = 0; i < modulus.length; i++) {
            nttTables[i] = new NttTables(coeffCountPower, modulus[i]);
        }

//        IntStream.range(0, modulus.length).parallel().forEach(
//                i -> nttTables[i] = new NttTables(coeffCountPower, modulus[i])
//        );
    }

//    public static void createNttTable(int coeffCountPower, Modulus modulus, NttTables nttTables) {
//
//
//
//        IntStream.range(0, modulus.length).parallel().forEach(
//                i -> nttTables[i] = new NttTables(coeffCountPower, modulus[i])
//        );
//    }


}
