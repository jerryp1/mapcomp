package edu.alibaba.mpc4j.s2pc.pcg.edabit;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;

/**
 * Edabit生成参与方
 *
 * @author qiyuan.gxw
 * Date: 2023-04-04
 */
public abstract class AbstractEdabitParty extends AbstractTwoPartyPto implements EdabitParty {
    protected final EdabitConfig config;

    public AbstractEdabitParty(PtoDesc ptoDesc, Rpc rpc, Party otherParty, EdabitConfig config) {
        super(ptoDesc, rpc, otherParty, config);
        this.config = config;
    }

    protected void setInitInput() {

    }

    protected void setPtoInput() {

    }
}
