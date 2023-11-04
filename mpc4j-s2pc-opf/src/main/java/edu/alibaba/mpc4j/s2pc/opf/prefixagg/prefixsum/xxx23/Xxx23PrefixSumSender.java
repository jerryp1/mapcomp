package edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixsum.xxx23;

import edu.alibaba.mpc4j.common.circuit.prefix.PrefixTreeFactory;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixsum.PrefixSumAggregatorPrefix;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleFactory;

import java.util.concurrent.TimeUnit;

/**
 * Prefix sum sender.
 *
 * @author Li Peng
 * @date 2023/5/30
 */
public class Xxx23PrefixSumSender extends PrefixSumAggregatorPrefix {

    public Xxx23PrefixSumSender(Rpc senderRpc, Party receiverParty, Xxx23PrefixSumConfig config) {
        super(Xxx23PrefixSumPtoDesc.getInstance(), senderRpc, receiverParty, config);
        z2cParty = Z2cFactory.createSender(senderRpc, receiverParty, config.getZ2cConfig());
        zlcParty = ZlcFactory.createSender(senderRpc, receiverParty, config.getZlcConfig());
        zlMuxParty = ZlMuxFactory.createSender(senderRpc, receiverParty, config.getZlMuxConfig());
        shuffleParty = ShuffleFactory.createSender(senderRpc, receiverParty, config.getShuffleConfig());
        z2IntegerCircuit = new Z2IntegerCircuit(z2cParty);
        prefixTree = PrefixTreeFactory.createPrefixSumTree(config.getPrefixTreeType(), this);
        zl = config.getZl();
        needShuffle = config.needShuffle();
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        z2cParty.init(maxL * maxNum);
        zlcParty.init(maxNum);
        zlMuxParty.init(maxNum);
        shuffleParty.init(maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }
}
