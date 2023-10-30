package edu.alibaba.mpc4j.s2pc.aby.operator.psorter.ahi22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Ahi22 Permutable Sorter Protocol Description. The protocol comes from Section 5.2.2 of the following paper:
 *  <p>
 *  Asharov, Gilad, et al. "Efficient secure three-party sorting with applications to data analysis and heavy hitters."
 *  Proceedings of the 2022 ACM SIGSAC Conference on Computer and Communications Security. 2022.
 *  </p>
 *
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
    private static final String PTO_NAME = "AHI22_P_SORTER";

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
