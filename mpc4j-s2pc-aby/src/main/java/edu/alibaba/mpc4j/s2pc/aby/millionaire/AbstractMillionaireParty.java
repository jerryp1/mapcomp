package edu.alibaba.mpc4j.s2pc.aby.millionaire;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * Abstract Millionaire Protocol Party.
 *
 * @author Li Peng
 * @date 2023/4/24
 */
public class AbstractMillionaireParty extends AbstractTwoPartyPto implements MillionaireParty {
    protected AbstractMillionaireParty(PtoDesc ptoDesc, Rpc rpc, Party otherParty, MultiPartyPtoConfig config) {
        super(ptoDesc, rpc, otherParty, config);
    }

    @Override
    public void init(int maxBitNum) throws MpcAbortException {

    }
}
