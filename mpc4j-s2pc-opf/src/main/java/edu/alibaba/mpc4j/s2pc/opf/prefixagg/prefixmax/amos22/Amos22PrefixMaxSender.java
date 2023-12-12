package edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixmax.amos22;

import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.GroupTypes.AggTypes;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.share.ShareGroupFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.share.ShareGroupParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxFactory;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixmax.AbstractPrefixMaxAggregator;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleFactory;

import java.util.concurrent.TimeUnit;

public class Amos22PrefixMaxSender extends AbstractPrefixMaxAggregator {

    private final ShareGroupParty shareGroupParty;

    public Amos22PrefixMaxSender(Rpc senderRpc, Party receiverParty, Amos22PrefixMaxConfig config) {
        super(Amos22PrefixMaxPtoDesc.getInstance(), senderRpc, receiverParty, config);
        z2cParty = Z2cFactory.createSender(senderRpc, receiverParty, config.getZ2cConfig());
        shuffleParty = ShuffleFactory.createSender(senderRpc, receiverParty, config.getShuffleConfig());
        z2MuxParty = Z2MuxFactory.createSender(senderRpc, receiverParty, config.getZ2MuxConfig());
        shareGroupParty = ShareGroupFactory.createSender(senderRpc, receiverParty, config.getShareGroupConfig());
        z2IntegerCircuit = new Z2IntegerCircuit(z2cParty);
        zl = config.getZl();
        needShuffle = config.needShuffle();
        plainOutput = config.isPlainOutput();
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        z2cParty.init(maxL * maxNum);
        shuffleParty.init(maxNum);
        z2MuxParty.init(maxNum);
        shareGroupParty.init(1, maxNum, maxL);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }
    @Override
    protected SquareZ2Vector[] aggWithIndicators(SquareZ2Vector groupIndicator1, SquareZ2Vector[] aggField) throws MpcAbortException {
        SquareZ2Vector[] res = shareGroupParty.groupAgg(aggField, null, AggTypes.MAX, groupIndicator1);
        // update groupIndicator1
        groupIndicator1.setBitVector(shareGroupParty.getFlag(groupIndicator1).getBitVector());
        return res;
    }
}
