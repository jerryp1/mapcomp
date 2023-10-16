package edu.alibaba.mpc4j.s2pc.aby.operator.psorter;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.ZlMaxFactory.ZlMaxType;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.ZlMaxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.rrk20.Rrk20ZlMaxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.rrk20.Rrk20ZlMaxReceiver;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.rrk20.Rrk20ZlMaxSender;
import edu.alibaba.mpc4j.s2pc.aby.operator.psorter.ahi22.Ahi22PermutableSorterConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.psorter.ahi22.Ahi22PermutableSorterReceiver;
import edu.alibaba.mpc4j.s2pc.aby.operator.psorter.ahi22.Ahi22PermutableSorterSender;

/**
 * @author Li Peng
 * @date 2023/10/11
 */
public class PermutableSorterFactory {
    /**
     * Private constructor.
     */
    private PermutableSorterFactory() {
        // empty
    }

    /**
     * Permutable Sorter type enums.
     */
    public enum PermutableSorterTypes {
        /**
         * AHI+22.
         */
        AHI22,
    }

//    /**
//     * Creates a sorter.
//     *
//     * @param type type of sorter.
//     * @return a adder.
//     */
//    public static Sorter createSorter(PermutableSorterTypes type, Z2IntegerCircuit circuit) {
//        switch (type) {
//            case AHI22:
//                return new BitonicSorter(circuit);
//            default:
//                throw new IllegalArgumentException("Invalid " + MultiplierFactory.MultiplierTypes.class.getSimpleName() + ": " + type.name());
//        }
//    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static PermutableSorterParty createSender(Rpc senderRpc, Party receiverParty, PermutableSorterConfig config) {
        PermutableSorterTypes type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case AHI22:
                return new Ahi22PermutableSorterSender(senderRpc, receiverParty, (Ahi22PermutableSorterConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PermutableSorterTypes.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a receiver.
     *
     * @param receiverRpc the receiver RPC.
     * @param senderParty the sender party.
     * @param config      the config.
     * @return a receiver.
     */
    public static PermutableSorterParty createReceiver(Rpc receiverRpc, Party senderParty, PermutableSorterConfig config) {
        PermutableSorterTypes type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case AHI22:
                return new Ahi22PermutableSorterReceiver(receiverRpc, senderParty, (Ahi22PermutableSorterConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PermutableSorterTypes.class.getSimpleName() + ": " + type.name());
        }
    }
}
