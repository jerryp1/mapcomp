package edu.alibaba.mpc4j.crypto.fhe.bfv;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.crypto.fhe.utils.NttContext;

import java.math.BigInteger;

/**
 * @author Qixian Zhou
 * @date 2023/7/14
 */
public class BatchEncoder {


    public long polyModulusDegree;

    public long plainModulus;

    public NttContext nttContext;


    public BatchEncoder(BfvParameters parameters) {

        assert parameters.plainModulus % (2 * parameters.polyModulusDegree) == 1:
                "BatchEncode must be kept m % 2n = 1";
        assert BigInteger.valueOf(parameters.plainModulus).isProbablePrime(CommonConstants.STATS_BIT_LENGTH):
                "BatchEncode must be kept modulus is prime number";

        polyModulusDegree = parameters.polyModulusDegree;
        plainModulus = parameters.plainModulus;
        nttContext = new NttContext(polyModulusDegree, plainModulus);
    }

    public Plaintext encode(long[] values) {

        assert values.length == polyModulusDegree;
        long[] coeffs = nttContext.nttInverse(values);
        return new Plaintext(polyModulusDegree, coeffs);
    }

    public long[] decode(Plaintext plain) {
        return nttContext.nttForward(plain.poly.coeffs);
    }








}
