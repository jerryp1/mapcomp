package edu.alibaba.mpc4j.s2pc.aby.operator.psorter.ahi22;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.zl.MpcZlParty;
import edu.alibaba.mpc4j.common.circuit.zl.MpcZlVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.psorter.AbstractPermutableSorterParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxParty;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * @author Li Peng
 * @date 2023/10/11
 */
public class Ahi22PermutableSorterReceiver extends AbstractPermutableSorterParty {
    /**
     * Bit2a receiver.
     */
    private final Bit2aParty bit2aReceiver;
    /**
     * Zl circuit receiver.
     */
    private final ZlcParty zlcReceiver;
    /**
     * Zl mux receiver.
     */
    private final ZlMuxParty zlMuxReceiver;

    public Ahi22PermutableSorterReceiver(Rpc rpc, Party otherParty, Ahi22PermutableSorterConfig config) {
        super(Ahi22PermutableSorterPtoDesc.getInstance(), rpc, otherParty, config);
        bit2aReceiver = Bit2aFactory.createReceiver(rpc, otherParty, config.getBit2aConfig());
        zlcReceiver = ZlcFactory.createReceiver(rpc, otherParty, config.getZlcConfig());
        zlMuxReceiver = ZlMuxFactory.createReceiver(rpc, otherParty, config.getZlMuxConfig());
        zl = config.getBit2aConfig().getZl();
        l = zl.getL();
        byteL = zl.getByteL();
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        bit2aReceiver.init(maxL, maxNum);
        zlcReceiver.init(maxNum);
        zlMuxReceiver.init(maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlVector[] sort(SquareZ2Vector[][] xiArrays) throws MpcAbortException {
        checkInputs(xiArrays);
        setPtoInput(xiArrays);
        SquareZ2Vector[] xiArray = Arrays.stream(xiArrays).map(xi -> xi[0]).toArray(SquareZ2Vector[]::new);
//        SquareZlVector[] f1 = new SquareZlVector[numSort];
//        for (int i = 0; i < numSort; i++) {
//            f1[i] =  bit2aReceiver.bit2a(xiArray[i]);
//        }
        SquareZlVector[] f1 = bit2aReceiver.bit2a(xiArray);
        SquareZlVector[] ones = IntStream.range(0, numSort).mapToObj(i -> SquareZlVector.createOnes(zl, num)).toArray(SquareZlVector[]::new);

        SquareZlVector[] f0 = zlcReceiver.sub(ones,f1);
        SquareZlVector[] s0 = new SquareZlVector[numSort];
        SquareZlVector[] s1 = new SquareZlVector[numSort];
        for (int i = 0; i < numSort; i++) {
            if (i == 0) {
                BigInteger[] zeros = IntStream.range(0, num).mapToObj(j -> BigInteger.ZERO).toArray(BigInteger[]::new);
                s0[i] = zlcReceiver.add(SquareZlVector.create(zl, zeros, false),f0[i]);
//                s1[i] = SquareZlVector.create(zl, zeros, false);
                continue;
            }
            s0[i] = zlcReceiver.add(s0[i - 1], f0[i]);

        }

        for (int i = 0; i < numSort; i++) {
            if (i == 0) {
                s1[i] = zlcReceiver.add(s0[numSort- 1], f1[i]);
                continue;
            }
            s1[i] = zlcReceiver.add(s1[i - 1], f1[i]);
        }

        // reveal
//        zlcReceiver.revealOther(s0);
//        zlcReceiver.revealOther(s1);
        SquareZlVector[] result = zlcReceiver.add(s0, zlMuxReceiver.mux(xiArray, zlcReceiver.sub(s1, s0)));

//        SquareZlVector[] result = new SquareZlVector[numSort];
//        for (int i = 0; i < numSort; i ++) {
//            result[i] = zlcReceiver.add(s0[i], zlMuxReceiver.mux(xiArray[i], zlcReceiver.sub(s1[i], s0[i])));
//        }
        return result;
//        return null;
    }

    void checkInputs(MpcZ2Vector[][] xiArrays) {
        Arrays.stream(xiArrays).forEach(xi ->
            MathPreconditions.checkEqual("Number of input bits", "1", xi.length, 1));
    }
}
