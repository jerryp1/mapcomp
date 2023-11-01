package edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.psty19;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.AbstractBopprfPlpsiClient;

/**
 * PSTY19 payload-circuit PSI client.
 *
 * @author Feng Han
 * @date 2023/10/20
 */
public class Psty19PlpsiClient<T> extends AbstractBopprfPlpsiClient<T> {

    public Psty19PlpsiClient(Rpc serverRpc, Party clientParty, Psty19PlpsiConfig config) {
        super(Psty19PlpsiPtoDesc.getInstance(), serverRpc, clientParty, config);
    }
}
