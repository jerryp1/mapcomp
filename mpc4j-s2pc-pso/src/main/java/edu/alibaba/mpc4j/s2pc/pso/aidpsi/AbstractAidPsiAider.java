package edu.alibaba.mpc4j.s2pc.pso.aidpsi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyPto;

/**
 * abstract aid PSI aider.
 *
 * @author Weiran Liu
 * @date 2023/5/4
 */
public abstract class AbstractAidPsiAider extends AbstractThreePartyPto implements AidPsiAider {

    protected AbstractAidPsiAider(PtoDesc ptoDesc, Rpc aiderRpc, Party serverParty, Party clientParty, AidPsiConfig config) {
        super(ptoDesc, aiderRpc, serverParty, clientParty, config);
    }

    protected void setInitInput() {
        initState();
    }

    protected void setPtoInput() {
        checkInitialized();
        extraInfo++;
    }
}
