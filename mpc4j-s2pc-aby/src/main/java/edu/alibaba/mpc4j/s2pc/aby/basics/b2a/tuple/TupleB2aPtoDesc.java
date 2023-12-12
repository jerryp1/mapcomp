package edu.alibaba.mpc4j.s2pc.aby.basics.b2a.tuple;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Hardcode B2a protocol description.
 *
 * @author Li Peng
 * @date 2023/11/21
 */
public class TupleB2aPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 5155177877103964391L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "TUPLE_B2A";

    /**
     * protocol step
     */
    enum PtoStep {
        // empty
    }

    /**
     * singleton mode
     */
    private static final TupleB2aPtoDesc INSTANCE =
        new TupleB2aPtoDesc();

    /**
     * private constructor.
     */
    private TupleB2aPtoDesc() {
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
