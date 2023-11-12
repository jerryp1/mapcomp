package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.hardcode;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.AbstractZ2MtgParty;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2Triple;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgParty;

/**
 * cache Z2 multiplication triple generator receiver.
 *
 * @author Li Peng
 * @date 2023/11/11
 */
public class HardcodeZ2MtgReceiver extends AbstractZ2MtgParty {

    public HardcodeZ2MtgReceiver(Rpc receiverRpc, Party senderParty, HardcodeZ2MtgConfig config) {
        super(HardcodeZ2MtgPtoDesc.getInstance(), receiverRpc, senderParty, config);
    }

    public HardcodeZ2MtgReceiver(Rpc receiverRpc, Party senderParty, Party aiderParty, HardcodeZ2MtgConfig config) {
        super(HardcodeZ2MtgPtoDesc.getInstance(), receiverRpc, senderParty, config);
    }

    @Override
    public void init(int updateNum) throws MpcAbortException {
        setInitInput(updateNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Z2Triple generate(int num) throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);
        int byteNum = CommonUtils.getByteLength(num);
        Z2Triple z2Triple =  Z2Triple.create(num,new byte[byteNum], new byte[byteNum], new byte[byteNum]);
        logPhaseInfo(PtoState.PTO_END);
        return z2Triple;
    }
}
