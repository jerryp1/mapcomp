package edu.alibaba.mpc4j.s2pc.aby.operator.psorter.ahi22;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.aby.operator.psorter.PermutableSorterFactory.PartyTypes;

/**
 * Ahi22 Permutable Sorter Receiver.
 *
 * @author Li Peng
 * @date 2023/10/11
 */
public class Ahi22PermutableSorterReceiver extends AbstractAhi22PermutableSorterParty {
    public Ahi22PermutableSorterReceiver(Rpc rpc, Party otherParty, Ahi22PermutableSorterConfig config) {
        super(Ahi22PermutableSorterPtoDesc.getInstance(), rpc, otherParty, config, PartyTypes.RECEIVER);
    }
}
