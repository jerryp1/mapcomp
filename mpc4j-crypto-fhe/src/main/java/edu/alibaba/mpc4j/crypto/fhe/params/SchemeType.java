package edu.alibaba.mpc4j.crypto.fhe.params;

/**
 * @author Qixian Zhou
 * @date 2023/8/30
 */
public enum SchemeType {

    NONE(0),
    
    BFV(1),

    CKKS(2),

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
