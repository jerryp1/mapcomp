package edu.alibaba.mpc4j.s2pc.opf.prefixmax.xxx23;

import edu.alibaba.mpc4j.common.circuit.prefix.PrefixTreeFactory;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.greater.zl.ZlGreaterFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.opf.prefixmax.PrefixMaxAggregator;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleFactory;

import java.util.concurrent.TimeUnit;

/**
 * Prefix max receiver.
 *
 * @author Li Peng
 * @date 2023/11/1
 */
public class Xxx23PrefixMaxReceiver extends PrefixMaxAggregator {

    public Xxx23PrefixMaxReceiver(Rpc receiverRpc, Party senderParty, Xxx23PrefixMaxConfig config) {
        super(Xxx23PrefixMaxPtoDesc.getInstance(), receiverRpc, senderParty, config);
        z2cParty = Z2cFactory.createReceiver(receiverRpc, senderParty, config.getZ2cConfig());
        zlcParty = ZlcFactory.createReceiver(receiverRpc, senderParty, config.getZlcConfig());
        zlMuxParty = ZlMuxFactory.createReceiver(receiverRpc, senderParty, config.getZlMuxConfig());
        zlGreaterParty = ZlGreaterFactory.createReceiver(receiverRpc, senderParty, config.getZlGreaterConfig());
        shuffleParty = ShuffleFactory.createReceiver(receiverRpc, senderParty, config.getShuffleConfig());
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
        zlGreaterParty.init(maxL, maxNum);
        shuffleParty.init(maxL, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }
}