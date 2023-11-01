package edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.rs21;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.AbstractBopprfPlpsiClient;

/**
 * RS21 payload-circuit PSI client.
 *
 * @author Feng Han
 * @date 2023/10/20
 */
public class Rs21PlpsiClient<T> extends AbstractBopprfPlpsiClient<T> {

    public Rs21PlpsiClient(Rpc serverRpc, Party clientParty, Rs21PlpsiConfig config) {
        super(Rs21PlpsiPtoDesc.getInstance(), serverRpc, clientParty, config);
    }
}
