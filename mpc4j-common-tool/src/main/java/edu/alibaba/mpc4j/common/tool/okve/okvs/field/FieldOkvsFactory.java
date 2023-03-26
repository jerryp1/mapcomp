package edu.alibaba.mpc4j.common.tool.okve.okvs.field;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * Field OKVS factory.
 *
 * @author Weiran Liu
 * @date 2023/3/27
 */
public class FieldOkvsFactory {
    /**
     * private constructor.
     */
    private FieldOkvsFactory() {
        // empty
    }

    public enum FieldOkvsType {
        /**
         * polynomial interpolation
         */
        POLYNOMIAL,
        /**
         * MegaBin
         */
        MEGA_BIN,
    }

    /**
     * Creates a field OKVS.
     *
     * @param envType the environment.
     * @param type    the type.
     * @param n       the number of key-value pairs.
     * @param l       the input / output bit length, must satifies l % Byte.SIZE == 0.
     * @param keys    the hash keys.
     * @return a field OKVS.
     */
    public static FieldOkvs createInstance(EnvType envType, FieldOkvsType type, int n, int l, byte[][] keys) {
        assert keys.length == getHashNum(type);
        switch (type) {
            case POLYNOMIAL:
                return new PolyFieldOkvs(envType, n, l);
            case MEGA_BIN:
                return new MegaBinFieldOkvs(envType, n, l, keys[0]);
            default:
                throw new IllegalArgumentException("Invalid " + FieldOkvsType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Gets the number of hashes.
     *
     * @param type the type.
     * @return the number of hashes.
     */
    public static int getHashNum(FieldOkvsType type) {
        switch (type) {
            case POLYNOMIAL:
                return 0;
            case MEGA_BIN:
                return 1;
            default:
                throw new IllegalArgumentException("Invalid " + FieldOkvsType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Gets m.
     *
     * @param type the type.
     * @param n    the number of key-value pairs.
     * @return m.
     */
    public static int getM(FieldOkvsType type, int n) {
        assert n > 1 : "n must be greater than 1: " + n;
        switch (type) {
            case POLYNOMIAL:
                return n;
            case MEGA_BIN:
                int binNum = MegaBinFieldOkvs.getBinNum(n);
                int binSize = MegaBinFieldOkvs.getBinSize(n);
                return binNum * binSize;
            default:
                throw new IllegalArgumentException("Invalid " + FieldOkvsType.class.getSimpleName() + ": " + type.name());
        }
    }
}
