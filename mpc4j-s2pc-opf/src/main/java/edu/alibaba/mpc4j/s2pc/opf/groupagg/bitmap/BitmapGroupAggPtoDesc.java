package edu.alibaba.mpc4j.s2pc.opf.groupagg.bitmap;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Bitmap group aggregation protocol description. The protocol comes from the following paper:
 * <p>
 * </p>
 *
 * @author Li Peng
 * @date 2023/11/8
 */
public class BitmapGroupAggPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -6372870765708365521L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "BITMAP_GROUP_AGG";

    /**
     * protocol step
     */
    enum PtoStep {
        // send gruop NUM
        SEND_GROUP_NUM
    }

    /**
     * singleton mode
     */
    private static final BitmapGroupAggPtoDesc INSTANCE = new BitmapGroupAggPtoDesc();

    /**
     * private constructor.
     */
    private BitmapGroupAggPtoDesc() {
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
