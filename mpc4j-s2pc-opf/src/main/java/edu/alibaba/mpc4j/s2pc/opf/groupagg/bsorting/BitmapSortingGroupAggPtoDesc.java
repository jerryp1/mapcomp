package edu.alibaba.mpc4j.s2pc.opf.groupagg.bsorting;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

import java.io.Serializable;

/**
 * Bitmap assist sorting-based group aggregation protocol description. The protocol comes from the following paper:
 * <p>
 * </p>
 *
 * @author Li Peng
 * @date 2023/11/20
 */
public class BitmapSortingGroupAggPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -7453570605640191836L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "BITMAP_ASSIST_SORTING_GROUP_AGG";

    /**
     * protocol step
     */
    enum PtoStep {
        // sender send group byte length
        SENDER_SEND_GROUP_BYTE_LENGTH,

        // receiver send group byte length
        RECEIVER_SEND_GROUP_BYTE_LENGTH,

        SENDER_SEND_BETA,

        SEND_SHARES,

        SEND_BITMAP_SHARES,

        REVEAL_OUTPUT,

        TEST,

        REVEAL_BIT,
    }

    /**
     * singleton mode
     */
    private static final BitmapSortingGroupAggPtoDesc INSTANCE = new BitmapSortingGroupAggPtoDesc();

    /**
     * private constructor.
     */
    private BitmapSortingGroupAggPtoDesc() {
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
