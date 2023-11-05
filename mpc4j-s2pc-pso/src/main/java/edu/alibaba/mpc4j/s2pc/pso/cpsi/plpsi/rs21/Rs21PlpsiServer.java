package edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.rs21;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.AbstractBopprfPlpsiServer;

/**
 * RS21 payload-circuit PSI server.
 *
 * @author Feng Han
 * @date 2023/10/20
 */
public class Rs21PlpsiServer<T, X> extends AbstractBopprfPlpsiServer<T, X> {

    public Rs21PlpsiServer(Rpc clientRpc, Party senderParty, Rs21PlpsiConfig config) {
        super(Rs21PlpsiPtoDesc.getInstance(), clientRpc, senderParty, config);
    }
}
