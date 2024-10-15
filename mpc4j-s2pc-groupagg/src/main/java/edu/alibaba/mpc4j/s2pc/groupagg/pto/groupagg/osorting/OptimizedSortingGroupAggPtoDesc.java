package edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.osorting;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Optimized sorting-based group aggregation protocol description. The protocol comes from the following paper:
 * <p>
 * </p>
 */
public class OptimizedSortingGroupAggPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int)  -1233482021596952844L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "OPTIMIZED_SORTING_GROUP_AGG";

    /**
     * protocol step.
     */
    enum PtoStep {
        /**
         * reveal output.
         */
        REVEAL_OUTPUT,
        /**
         * test.
         */
        TEST,
        /**
         * reveal bit.
         */
        REVEAL_BIT,
    }

    /**
     * singleton mode
     */
    private static final OptimizedSortingGroupAggPtoDesc INSTANCE = new OptimizedSortingGroupAggPtoDesc();

    /**
     * private constructor.
     */
    private OptimizedSortingGroupAggPtoDesc() {
        // empty
    }

    public static PtoDesc getInstance() {
        return INSTANCE;
    }

    static {
        PtoDescManager.registerPtoDesc(getInstance());
    }

    @Override
    public int getPtoId() {
        return PTO_ID;
    }

    @Override
    public String getPtoName() {
        return PTO_NAME;
    }
}
