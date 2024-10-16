package edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.prefixxor.php24;

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
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.PlainBitMuxFactory;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.prefixxor.AbstractPrefixXorAggregator;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleFactory;

import java.util.concurrent.TimeUnit;

/**
 * Prefix xor sender.
 *
 */
public class Php24PrefixXorSender extends AbstractPrefixXorAggregator {

    public Php24PrefixXorSender(Rpc senderRpc, Party receiverParty, Php24PrefixXorConfig config) {
        super(Php24PrefixXorPtoDesc.getInstance(), senderRpc, receiverParty, config);
        z2cParty = Z2cFactory.createSender(senderRpc, receiverParty, config.getZ2cConfig());
        zlcParty = ZlcFactory.createSender(senderRpc, receiverParty, config.getZlcConfig());
        zlMuxParty = ZlMuxFactory.createSender(senderRpc, receiverParty, config.getZlMuxConfig());
        shuffleParty = ShuffleFactory.createSender(senderRpc, receiverParty, config.getShuffleConfig());
        a2bParty = A2bFactory.createSender(senderRpc, receiverParty, config.getA2bConfig());
        b2aParty = B2aFactory.createSender(senderRpc, receiverParty, config.getB2aConfig());
        z2MuxParty = Z2MuxFactory.createSender(senderRpc, receiverParty, config.getZ2MuxConfig());
        pbMuxParty = PlainBitMuxFactory.createSender(senderRpc, receiverParty, config.getPlainBitMuxConfig());
        z2IntegerCircuit = new Z2IntegerCircuit(z2cParty);
        prefixTree = PrefixTreeFactory.createPrefixSumTree(config.getPrefixTreeType(), this);
        zl = config.getZl();
        needShuffle = config.needShuffle();
        plainOutput = config.isPlainOutput();
    }

    @Override
    public boolean isSender() {
        return true;
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        z2cParty.init(maxL * maxNum);
        zlcParty.init(1);
        zlMuxParty.init(maxNum);
        shuffleParty.init(maxNum);
        a2bParty.init(maxL, maxNum);
        b2aParty.init(maxL, maxNum);
        z2MuxParty.init(maxNum);
        pbMuxParty.init(maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }
}
