package edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixmax.xxx23;

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
import edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.PlainBitMuxFactory;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixmax.AbstractPrefixMaxAggregator;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleFactory;

import java.util.concurrent.TimeUnit;

/**
 * Prefix max sender.
 *
 * @author Li Peng
 * @date 2023/11/1
 */
public class Xxx23PrefixMaxSender extends AbstractPrefixMaxAggregator {

    public Xxx23PrefixMaxSender(Rpc senderRpc, Party receiverParty, Xxx23PrefixMaxConfig config) {
        super(Xxx23PrefixMaxPtoDesc.getInstance(), senderRpc, receiverParty, config);
        z2cParty = Z2cFactory.createSender(senderRpc, receiverParty, config.getZ2cConfig());
        zlcParty = ZlcFactory.createSender(senderRpc, receiverParty, config.getZlcConfig());
        zlMuxParty = ZlMuxFactory.createSender(senderRpc, receiverParty, config.getZlMuxConfig());
        zlGreaterParty = ZlGreaterFactory.createSender(senderRpc, receiverParty, config.getZlGreaterConfig());
        shuffleParty = ShuffleFactory.createSender(senderRpc, receiverParty, config.getShuffleConfig());
        plainBitMuxParty = PlainBitMuxFactory.createSender(senderRpc, receiverParty, config.getPlainBitMuxConfig());
//        addSubPtos(zlcParty);
//        addSubPtos(zlMuxParty);
//        addSubPtos(zlGreaterParty);
//        addSubPtos(shuffleParty);
//        addSubPtos(plainBitMuxParty);
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
        plainBitMuxParty.init(maxNum);
        shuffleParty.init(maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }
}
