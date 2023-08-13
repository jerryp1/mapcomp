package edu.alibaba.mpc4j.crypto.fhe.utils;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.math.BigInteger;

/**
 *
 * A module to multiply polynomials using the Number Theoretic Transform (NTT),
 *
 * @author Qixian Zhou
 * @date 2023/7/12
 */
public class NttContext {


    public RingLweZp64 ringLweZp64;

    // x^N + 1
    public long polyDegree;
    // Z_Q
    public long coeffModulus;
    // root of unity, length is polyDegree
    public long[] rootOfUnityArray;
    // inverse root of unity , length is polyDegree
    public long[] rootOfUnityInvArray;

    // root of unity
    public long rootOfUnity;

    // for Iterate NTT Implementation
    public int[] reversedBits;

    public NttContext(long polyDegree, long coeffModulus){

        assert (polyDegree & (polyDegree - 1)) == 0: "n must be integr power of 2";
        assert BigInteger.valueOf(coeffModulus).isProbablePrime(CommonConstants.STATS_BIT_LENGTH): "Coeff modulus must be prime number.";
        assert (coeffModulus - 1) % (2 * polyDegree) == 0: "2n | Q - 1 must be satisfied";

        this.polyDegree = polyDegree;
        this.coeffModulus = coeffModulus;

        ringLweZp64 = new RingLweZp64(EnvType.STANDARD_JDK, coeffModulus);
        rootOfUnity = ringLweZp64.rootOfUnity(2 * polyDegree);
        precompute();
    }

    private void precompute() {

        rootOfUnityArray = new long[(int) polyDegree];
        rootOfUnityArray[0] = 1L;
        for (int i = 1; i < polyDegree; i++) {
            rootOfUnityArray[i] = ringLweZp64.mul(rootOfUnityArray[i-1], rootOfUnity);
        }

        rootOfUnityInvArray = new long[(int) polyDegree];
        rootOfUnityInvArray[0] = 1L;
        long rootOfUnityInv = ringLweZp64.modInv(rootOfUnity);
        for (int i = 1; i < polyDegree; i++) {
            rootOfUnityInvArray[i] = ringLweZp64.mul(rootOfUnityInvArray[i-1], rootOfUnityInv);
        }

        reversedBits = new int[(int) polyDegree];
        int width = LongUtils.ceilLog2(polyDegree);
        for (int i = 0; i < polyDegree; i++) {
            reversedBits[i] = reverseBits(i, width);
        }

    }


    public long[] ntt(long[] coeffs, long[] rootOfUnityArray) {

        int numCoeffs = coeffs.length;
        assert numCoeffs == rootOfUnityArray.length: "Length of roots of unity it too small, Length is: " + rootOfUnityArray.length;

        long[] result = reverseCoeffs(coeffs);

        int logNumCoeffs = LongUtils.ceilLog2(numCoeffs);

        for (int logm = 1; logm < logNumCoeffs  + 1; logm += 1) {
            for (int j = 0; j < numCoeffs; j+= ( 1 << logm)) {
                for (int i = 0; i < 1 << (logm - 1); i++) {

                    int indexEven = j + i;
                    int indexOdd = j + i + ( 1 << (logm - 1));

                    int rootIndex = ( i << ( 1 + logNumCoeffs - logm) );
                    long omegaFactor = ringLweZp64.mul(rootOfUnityArray[rootIndex], result[indexOdd]);

                    long butterflyPlus = ringLweZp64.add(result[indexEven], omegaFactor);
                    long butterflyMinus = ringLweZp64.sub(result[indexEven], omegaFactor);

                    result[indexEven] = butterflyPlus;
                    result[indexOdd] = butterflyMinus;
                }
            }
        }
        return result;
    }

    public long[] nttForward(long[] coeffs) {

        assert coeffs.length == this.polyDegree: "input coeffs length must be equal poly degree";
        // pre-comute the coeffs
        // \overline {a} = [a0 * g_{2n}^0, a1 * g_{2n}^1, a2 * g_{2n}^2, ...., a_{n-1} * g_{2n}^{n-1}]
        long[] nttInput = new long[coeffs.length];
        for (int i = 0; i < coeffs.length; i++) {
            nttInput[i] = ringLweZp64.mul(coeffs[i], this.rootOfUnityArray[i]);
        }
        return ntt(nttInput, this.rootOfUnityArray);
    }

    public long[] nttInverse(long[] coeffs) {

        assert coeffs.length == this.polyDegree: "input coeffs length must be equal poly degree";

        long[] invCoeffs = ntt(coeffs, this.rootOfUnityInvArray);
        long polyDegreeInv = ringLweZp64.modInv(polyDegree);
        long[] result = new long[coeffs.length];

        for (int i = 0; i < coeffs.length; i++) {
            long tmp = ringLweZp64.mul(invCoeffs[i], this.rootOfUnityInvArray[i]);
            result[i] = ringLweZp64.mul(tmp, polyDegreeInv);
        }
        return result;
    }

    private long[] reverseCoeffs(long[] coeffs) {

        long[] result = new long[coeffs.length];
        int width = LongUtils.ceilLog2(coeffs.length);
        for (int i = 0; i < coeffs.length; i++) {
            result[i] = coeffs[reverseBits(i, width) ];
        }
        return result;
    }



    private int reverseBits(int value, int width) {
        int result = 0;
        for (int i = 0; i < width; i++) {
            result = (result << 1) | (value & 1);
            value >>= 1;
        }
        return result;
    }

}
