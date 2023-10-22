package edu.alibaba.mpc4j.crypto.fhe.params;

/**
 * Describes the type of encryption scheme to be used.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/encryptionparams.h#L25
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/8/30
 */
public enum SchemeType {
    /**
     * NONE
     */
    NONE(0),
    /**
     * BFV
     */
    BFV(1),
    /**
     * CKKS
     */
    CKKS(2),
    /**
     * BGV
     */
    BGV(3);

    private final int value;

    SchemeType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static SchemeType getByValue(int value) {

        switch (value) {
            case 0:
                return NONE;
            case 1:
                return BFV;
            case 2:
                return CKKS;
            case 3:
                return BGV;
            default:
                throw new IllegalArgumentException("no match scheme for given value");
        }
//        for (SchemeType scheme: SchemeType.values()) {
//            if (scheme.getValue() == value) {
//                return scheme;
//            }
//        }
//        throw new IllegalArgumentException("no match scheme for given value");
    }

}
