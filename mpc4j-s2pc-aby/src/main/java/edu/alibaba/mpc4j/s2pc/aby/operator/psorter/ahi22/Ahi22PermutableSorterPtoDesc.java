package edu.alibaba.mpc4j.s2pc.aby.operator.psorter.ahi22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.rrk20.Rrk20ZlMaxPtoDesc;

import java.io.Serializable;

/**
 * @author Li Peng
 * @date 2023/10/11
 */
public class Ahi22PermutableSorterPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -507571340734010712L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "AHI+22_P_SORTER";

    /**
     * protocol step
     */
    enum PtoStep {
        // empty
    }

    /**
     * singleton mode
     */
    private static final Ahi22PermutableSorterPtoDesc INSTANCE =
        new Ahi22PermutableSorterPtoDesc();

    /**
     * private constructor.
     */
    private Ahi22PermutableSorterPtoDesc() {
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
