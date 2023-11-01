package edu.alibaba.mpc4j.common.circuit.prefixsum;

/**
 * Prefix sum tree factory.
 *
 * @author Li Peng
 * @date 2023/10/27
 */
public class PrefixSumTreeFactory {
    /**
     * Adder type enums.
     */
    public enum PrefixSumTreeTypes {
        /**
         * Parallel prefix tree of Sklansky structure. The structure comes from the following paper:
         *
         * <p>
         * Sklansky, Jack. "Conditional-sum addition logic." IRE Transactions on Electronic computers 2 (1960): 226-231.
         * </p>
         */
        SKLANSKY,
        /**
         * Parallel prefix tree of Brent-Kung structure. The structure comes from the following paper:
         *
         * <p>
         * Brent, and Kung. "A regular layout for parallel adders." IEEE transactions on Computers 100.3 (1982): 260-264.
         * </p>
         */
        BRENT_KUNG,
        /**
         * Parallel prefix tree of Kogge-Stone structure. The structure comes from the following paper:
         *
         * <p>
         * Kogge, Peter M., and Harold S. Stone. "A parallel algorithm for the efficient solution of a general class of
         * recurrence equations." IEEE transactions on computers 100.8 (1973): 786-793.
         * </p>
         */
        KOGGE_STONE,
    }

    /**
     * Creates a prefix sum tree.
     *
     * @param type        type of adder.
     * @param prefixSumOp specified prefix sum operation.
     * @return a prefix sum tree.
     */
    public static PrefixSumTree createPrefixSumTree(PrefixSumTreeTypes type, PrefixSumOp prefixSumOp) {
        switch (type) {
            case SKLANSKY:
                return new SklanskyTree(prefixSumOp);
            case BRENT_KUNG:
                return new BrentKungTree(prefixSumOp);
            case KOGGE_STONE:
                return new KoggeStoneTree(prefixSumOp);
            default:
                throw new IllegalArgumentException("Invalid " + PrefixSumTreeFactory.PrefixSumTreeTypes.class.getSimpleName() + ": " + type.name());
        }
    }
}
