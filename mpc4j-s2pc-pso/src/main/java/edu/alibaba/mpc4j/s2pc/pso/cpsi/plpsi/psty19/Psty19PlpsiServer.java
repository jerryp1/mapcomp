package edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.psty19;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.AbstractBopprfPlpsiServer;

/**
 * PSTY19 payload-circuit PSI server.
 *
 * @author Feng Han
 * @date 2023/10/20
 */
public class Psty19PlpsiServer<T, X> extends AbstractBopprfPlpsiServer<T, X> {
    public Psty19PlpsiServer(Rpc clientRpc, Party senderParty, Psty19PlpsiConfig config) {
        super(Psty19PlpsiPtoDesc.getInstance(), clientRpc, senderParty, config);
    }
}
