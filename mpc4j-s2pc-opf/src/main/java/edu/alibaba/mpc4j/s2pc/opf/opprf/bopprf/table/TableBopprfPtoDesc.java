package edu.alibaba.mpc4j.s2pc.opf.opprf.bopprf.table;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Table Batch OPRRF protocol description. The scheme is described by the following paper:
 * <p>
 * Kolesnikov, Vladimir, Naor Matania, Benny Pinkas, Mike Rosulek, and Ni Trieu. Practical multi-party private set
 * intersection from symmetric-key techniques. CCS 2017, pp. 1257-1272. 2017.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/4/9
 */
class TableBopprfPtoDesc implements PtoDesc {
    /**
     * the protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 4325106241890196218L);
    /**
     * the protocol name
     */
    private static final String PTO_NAME = "TABLE_BOPPRF";

    /**
     * the protocol step
     */
    enum PtoStep {
        /**
         * the sender sends vs
         */
        SENDER_SEND_VS,
        /**
         * the sender sends tables
         */
        SENDER_SENDS_TABLES,
    }

    /**
     * the singleton mode
     */
    private static final TableBopprfPtoDesc INSTANCE = new TableBopprfPtoDesc();

    /**
     * private constructor.
     */
    private TableBopprfPtoDesc() {
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
