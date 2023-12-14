package edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.sorting;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Sorting-based group aggregation protocol description. The protocol comes from the following paper:
 * <p>
 * </p>
 *
 * @author Li Peng
 * @date 2023/11/3
 */
public class SortingGroupAggPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -1861431552725647393L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "SORTING_GROUP_AGG";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * sender shares.
         */
        SEND_SHARES,
        /**
         * reveal output
         */
        REVEAL_OUTPUT,
        /**
         * test
         */
        TEST,
        /**
         * reveal bit
         */
        REVEAL_BIT,
    }

    /**
     * singleton mode
     */
    private static final SortingGroupAggPtoDesc INSTANCE = new SortingGroupAggPtoDesc();

    /**
     * private constructor.
     */
    private SortingGroupAggPtoDesc() {
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
