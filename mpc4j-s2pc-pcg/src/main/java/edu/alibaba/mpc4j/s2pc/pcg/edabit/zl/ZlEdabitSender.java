package edu.alibaba.mpc4j.s2pc.pcg.edabit.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.edabit.AbstractEdabitParty;
import edu.alibaba.mpc4j.s2pc.pcg.edabit.Edabit;
import edu.alibaba.mpc4j.s2pc.pcg.edabit.EdabitConfig;

/**
 * Zl-Edabit参与方2
 *
 * @author qiyuan.gxw
 * Date: 2023-04-04
 */
public class ZlEdabitSender extends AbstractEdabitParty {
    public ZlEdabitSender(Rpc rpc, Party otherParty, EdabitConfig config) {
        super(ZlEdabitPtoDesc.getInstance(), rpc, otherParty, config);
    }

    @Override
    public void init() throws MpcAbortException {

    }

    @Override
    public Edabit generate(int num) throws MpcAbortException {
        return null;
    }
}
