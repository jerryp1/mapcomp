package edu.alibaba.mpc4j.s2pc.aby.circuit.compare.plain.fa;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.aby.circuit.compare.plain.AbstractPlainCompareParty;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiver;

import java.util.concurrent.TimeUnit;

/**
 * 基于全加器的明文比较协议接收方。
 *
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2023/4/11
 */
public class FullAdderPlainCompareReceiver extends AbstractPlainCompareParty {
    /**
     * COT协议接收方
     */
    private final CotReceiver cotReceiver;

    public FullAdderPlainCompareReceiver(Rpc receiverRpc, Party senderParty, FullAdderPlainCompareConfig config) {
        super(FullAdderPlainComparePtoDesc.getInstance(), receiverRpc, senderParty, config);
        cotReceiver = CotFactory.createReceiver(receiverRpc, senderParty, config.getCotConfig());
        addSubPtos(cotReceiver);
    }

    @Override
    public void init(int maxBitNum) throws MpcAbortException {
        setInitInput(maxBitNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // init COT receiver
        cotReceiver.init(maxBitNum, maxBitNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public boolean lessThan(int x) throws MpcAbortException {
        return false;
    }

}
