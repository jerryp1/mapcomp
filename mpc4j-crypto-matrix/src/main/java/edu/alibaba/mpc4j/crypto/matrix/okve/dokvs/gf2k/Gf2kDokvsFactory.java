package edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2k;

/**
 * GF(2^k)-DOKVS factory.
 *
 * @author Weiran Liu
 * @date 2023/7/11
 */
public class Gf2kDokvsFactory {
    /**
     * private constructor.
     */
    private Gf2kDokvsFactory() {
        // empty
    }

    /**
     * GF(2^k)-DOKVS type
     */
    public enum Gf2kDokvsType {
        /**
         * binary singleton garbled cuckoo table with 2 hash functions.
         */
        H2_BINARY_SINGLETON_GCT,
        /**
         * binary blazing fast garbled cuckoo table with 2 hash functions.
         */
        H2_BINARY_BLAZE_GCT,
        /**
         * field blazing fast garbled cuckoo table with 2 hash functions.
         */
        H2_FIELD_BLAZE_GCT,
        /**
         * field cluster blazing fast garbled cuckoo table with 2 hash functions.
         */
        H2_FIELD_CLUSTER_BLAZE_GCT,
        /**
         * binary singleton garbled cuckoo table with 3 hash functions.
         */
        H3_BINARY_SINGLETON_GCT,
        /**
         * binary blazing fast garbled cuckoo table with 3 hash function.
         */
        H3_BINARY_BLAZE_GCT,
        /**
         * field blazing fast using garbled cuckoo table with 3 hash function.
         */
        H3_FIELD_BLAZE_GCT,
        /**
         * field cluster blazing fast garbled cuckoo table with 3 hash functions.
         */
        H3_FIELD_CLUSTER_BLAZE_GCT,
    }
}
