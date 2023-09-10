package edu.alibaba.mpc4j.crypto.fhe.ntt;

import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;

import java.util.stream.IntStream;

/**
 * @author Qixian Zhou
 * @date 2023/8/27
 */
public class NttTablesCreateIter {

    private int coeffCountPower;

    private Modulus[] modulus;

    public NttTablesCreateIter() {}

    public NttTablesCreateIter(int coeffCountPower, Modulus[] modulus) {
        this.coeffCountPower = coeffCountPower;
        this.modulus = modulus;
    }


    public static void createNttTables(int coeffCountPower, Modulus[] modulus, NttTables[] nttTables) {

        assert modulus.length == nttTables.length;

        IntStream.range(0, modulus.length).parallel().forEach(
                i -> nttTables[i] = new NttTables(coeffCountPower, modulus[i])
        );
    }


}
