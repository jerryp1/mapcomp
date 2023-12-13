package edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixmax.php24;

import edu.alibaba.mpc4j.common.circuit.prefix.PrefixTreeFactory;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.aby.basics.a2b.A2bFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.B2aFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.greater.zl.ZlGreaterFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixmax.AbstractPrefixMaxAggregator;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleFactory;

import java.util.concurrent.TimeUnit;

/**
 * Prefix max receiver.
 *
 * @author Li Peng
 * @date 2023/11/1
 */
public class Php24PrefixMaxReceiver extends AbstractPrefixMaxAggregator {

    public Php24PrefixMaxReceiver(Rpc receiverRpc, Party senderParty, Php24PrefixMaxConfig config) {
        super(Php24PrefixMaxPtoDesc.getInstance(), receiverRpc, senderParty, config);
        z2cParty = Z2cFactory.createReceiver(receiverRpc, senderParty, config.getZ2cConfig());
        zlcParty = ZlcFactory.createReceiver(receiverRpc, senderParty, config.getZlcConfig());
        zlMuxParty = ZlMuxFactory.createReceiver(receiverRpc, senderParty, config.getZlMuxConfig());
        zlGreaterParty = ZlGreaterFactory.createReceiver(receiverRpc, senderParty, config.getZlGreaterConfig());
        shuffleParty = ShuffleFactory.createReceiver(receiverRpc, senderParty, config.getShuffleConfig());
        a2bParty = A2bFactory.createReceiver(receiverRpc, senderParty, config.getA2bConfig());
        b2aParty = B2aFactory.createReceiver(receiverRpc, senderParty, config.getB2aConfig());
        z2MuxParty = Z2MuxFactory.createReceiver(receiverRpc, senderParty, config.getZ2MuxConfig());
        z2IntegerCircuit = new Z2IntegerCircuit(z2cParty);
        prefixTree = PrefixTreeFactory.createPrefixSumTree(config.getPrefixTreeType(), this);
        zl = config.getZl();
        needShuffle = config.needShuffle();
        plainOutput = config.isPlainOutput();
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        z2cParty.init(maxL * maxNum);
        zlcParty.init(1);
        zlMuxParty.init(maxNum);
        zlGreaterParty.init(maxL, maxNum);
        shuffleParty.init(maxNum);
        a2bParty.init(maxL, maxNum);
        b2aParty.init(maxL, maxNum);
        z2MuxParty.init(maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

}
