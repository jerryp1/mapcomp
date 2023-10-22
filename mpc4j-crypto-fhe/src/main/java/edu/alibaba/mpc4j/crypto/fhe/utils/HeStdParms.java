package edu.alibaba.mpc4j.crypto.fhe.utils;

/**
 * Largest allowed bit counts for coeff_modulus based on the security estimates from
 * HomomorphicEncryption.org security standard. We refer Microsoft SEAL samples the secret key
 * from a ternary {-1, 0, 1} distribution.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/hestdparms.h
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/8/29
 */
public class HeStdParms {


    public final static double HE_STD_PARMS_ERROR_STD_DEV = 3.2;


    /**
     * Ternary secret; 128 bits classical security
     *
     * @param polyModulusDegree N
     * @return Largest allowed bit counts for coeff_modulus under 128-bit security
     */
    public static int heStdParms128Tc(int polyModulusDegree) {

        switch (polyModulusDegree) {
            case 1024:
                return 27;
            case 2048:
                return 54;
            case 4096:
                return 109;
            case 8192:
                return 218;
            case 16384:
                return 438;
            case 32768:
                return 881;
            default:
                return 0;
        }
    }

    /**
     * Ternary secret; 192 bits classical security
     *
     * @param polyModulusDegree N
     * @return Largest allowed bit counts for coeff_modulus under 192-bit security
     */
    public static int heStdParms192Tc(int polyModulusDegree) {

        switch (polyModulusDegree) {
            case 1024:
                return 19;
            case 2048:
                return 37;
            case 4096:
                return 75;
            case 8192:
                return 152;
            case 16384:
                return 305;
            case 32768:
                return 611;
            default:
                return 0;
        }

    }

    /**
     * Ternary secret; 256 bits classical security
     *
     * @param polyModulusDegree N
     * @return Largest allowed bit counts for coeff_modulus under 256-bit security
     */
    public static int heStdParms256Tc(int polyModulusDegree) {

        switch (polyModulusDegree) {
            case 1024:
                return 14;
            case 2048:
                return 29;
            case 4096:
                return 58;
            case 8192:
                return 118;
            case 16384:
                return 237;
            case 32768:
                return 476;
            default:
                return 0;
        }

    }


    /**
     * Ternary secret; 128 bits quantum security
     *
     * @param polyModulusDegree N
     * @return Largest allowed bit counts for coeff_modulus under 128 bits quantum security
     */
    public static int heStdParms128Tq(int polyModulusDegree) {

        switch (polyModulusDegree) {
            case 1024:
                return 25;
            case 2048:
                return 51;
            case 4096:
                return 101;
            case 8192:
                return 202;
            case 16384:
                return 411;
            case 32768:
                return 827;
            default:
                return 0;
        }

    }

    /**
     * Ternary secret; 192 bits quantum security
     *
     * @param polyModulusDegree N
     * @return Largest allowed bit counts for coeff_modulus under 192 bits quantum security
     */
    public static int heStdParms192Tq(int polyModulusDegree) {

        switch (polyModulusDegree) {
            case 1024:
                return 17;
            case 2048:
                return 35;
            case 4096:
                return 70;
            case 8192:
                return 141;
            case 16384:
                return 284;
            case 32768:
                return 571;
            default:
                return 0;
        }
    }

    /**
     * Ternary secret; 256 bits quantum security
     *
     * @param polyModulusDegree N
     * @return Largest allowed bit counts for coeff_modulus under 256 bits quantum security
     */
    public static int heStdParms256Tq(int polyModulusDegree) {

        switch (polyModulusDegree) {
            case 1024:
                return 13;
            case 2048:
                return 27;
            case 4096:
                return 54;
            case 8192:
                return 109;
            case 16384:
                return 220;
            case 32768:
                return 443;
            default:
                return 0;
        }
    }

}
