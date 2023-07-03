package edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.crypto.matrix.okve.cuckootable.CuckooTableSingletonTcFinder;
import edu.alibaba.mpc4j.crypto.matrix.okve.cuckootable.H2CuckooTableTcFinder;

/**
 * GF(2^e)-DOKVS factory.
 *
 * @author Weiran Liu
 * @date 2023/7/3
 */
public class Gf2eDokvsFactory {
    /**
     * private constructor.
     */
    private Gf2eDokvsFactory() {
        // empty
    }

    /**
     * GF(2^l)-DOKVS type
     */
    public enum Gf2eDokvsType {
        /**
         * two-core garbled cuckoo table with 2 hash functions.
         */
        H2_TWO_CORE_GCT,
        /**
         * singleton garbled cuckoo table with 2 hash functions.
         */
        H2_SINGLETON_GCT,
        /**
         * singleton garbled cuckoo table with 3 hash functions.
         */
        H3_SINGLETON_GCT,
    }

    /**
     * Creates an instance.
     *
     * @param envType environment.
     * @param type    type.
     * @param n       number of key-value pairs.
     * @param l       value bit length.
     * @param keys    keys.
     * @return and instance.
     */
    public static <X> Gf2eDokvs<X> createInstance(EnvType envType, Gf2eDokvsType type, int l, int n, byte[][] keys) {
        switch (type) {
            case H2_TWO_CORE_GCT:
                return new H2GctGf2eDokvs<>(envType, l, n, keys, new H2CuckooTableTcFinder<>());
            case H2_SINGLETON_GCT:
                return new H2GctGf2eDokvs<>(envType, l, n, keys, new CuckooTableSingletonTcFinder<>());
            case H3_SINGLETON_GCT:
                return new H3GctGfe2Dokvs<>(envType, l, n, keys);
            default:
                throw new IllegalArgumentException("Invalid " + Gf2eDokvsType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Gets number of required hashes.
     *
     * @param type type.
     * @return number of required hashes.
     */
    public static int getHashNum(Gf2eDokvsType type) {
        switch (type) {
            case H2_TWO_CORE_GCT:
            case H2_SINGLETON_GCT:
                return H2GctGf2eDokvs.TOTAL_HASH_NUM;
            case H3_SINGLETON_GCT:
                return H3GctGfe2Dokvs.TOTAL_HASH_NUM;
            default:
                throw new IllegalArgumentException("Invalid " + Gf2eDokvsType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Gets m with m % Byte.SIZE == 0.
     *
     * @param type type.
     * @param n    number of key-value pairs.
     * @return m.
     */
    public static int getM(Gf2eDokvsType type, int n) {
        assert n > 0;
        switch (type) {
            case H2_TWO_CORE_GCT:
            case H2_SINGLETON_GCT:
                return H2GctGf2eDokvs.getLm(n) + H2GctGf2eDokvs.getRm(n);
            case H3_SINGLETON_GCT:
                return H3GctGfe2Dokvs.getLm(n) + H3GctGfe2Dokvs.getRm(n);
            default:
                throw new IllegalArgumentException("Invalid " + Gf2eDokvsType.class.getSimpleName() + ": " + type.name());
        }
    }
}
