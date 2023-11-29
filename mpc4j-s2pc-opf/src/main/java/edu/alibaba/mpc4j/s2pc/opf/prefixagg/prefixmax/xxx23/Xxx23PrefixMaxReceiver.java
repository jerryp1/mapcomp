package edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixmax.xxx23;

import edu.alibaba.mpc4j.common.circuit.prefix.PrefixTreeFactory;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.a2b.A2bFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.B2aFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.greater.zl.ZlGreaterFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.PlainBitMuxFactory;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggNode;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixmax.AbstractPrefixMaxAggregator;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleFactory;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Prefix max receiver.
 *
 * @author Li Peng
 * @date 2023/11/1
 */
public class Xxx23PrefixMaxReceiver extends AbstractPrefixMaxAggregator {

    public Xxx23PrefixMaxReceiver(Rpc receiverRpc, Party senderParty, Xxx23PrefixMaxConfig config) {
        super(Xxx23PrefixMaxPtoDesc.getInstance(), receiverRpc, senderParty, config);
        z2cParty = Z2cFactory.createReceiver(receiverRpc, senderParty, config.getZ2cConfig());
        zlcParty = ZlcFactory.createReceiver(receiverRpc, senderParty, config.getZlcConfig());
        zlMuxParty = ZlMuxFactory.createReceiver(receiverRpc, senderParty, config.getZlMuxConfig());
        zlGreaterParty = ZlGreaterFactory.createReceiver(receiverRpc, senderParty, config.getZlGreaterConfig());
        shuffleParty = ShuffleFactory.createReceiver(receiverRpc, senderParty, config.getShuffleConfig());
        a2bParty = A2bFactory.createReceiver(receiverRpc, senderParty, config.getA2bConfig());
        b2aParty = B2aFactory.createReceiver(receiverRpc, senderParty, config.getB2aConfig());
        z2MuxParty = Z2MuxFactory.createReceiver(receiverRpc, senderParty, config.getZ2MuxConfig());

//        addSubPtos(z2cParty);
//        addSubPtos(zlcParty);
//        addSubPtos(zlMuxParty);
//        addSubPtos(zlGreaterParty);
//        addSubPtos(plainBitMuxParty);
//        addSubPtos(shuffleParty);
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
