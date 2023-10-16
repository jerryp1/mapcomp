package edu.alibaba.mpc4j.s2pc.aby.operator.psorter.ahi22;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.zl.MpcZlParty;
import edu.alibaba.mpc4j.common.circuit.zl.MpcZlVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.psorter.AbstractPermutableSorterParty;

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

    public Ahi22PermutableSorterReceiver(Rpc rpc, Party otherParty, Ahi22PermutableSorterConfig config) {
        super(Ahi22PermutableSorterPtoDesc.getInstance(), rpc, otherParty, config);
        bit2aReceiver = Bit2aFactory.createReceiver(rpc, otherParty, config.getBit2aConfig());
        zlcReceiver = ZlcFactory.createReceiver(rpc, otherParty, config.getZlcConfig());
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

        return zlcReceiver.add(s0, zlcReceiver.mul(f1, zlcReceiver.sub(s1, s0)));
    }

    void checkInputs(MpcZ2Vector[][] xiArrays) {
        Arrays.stream(xiArrays).forEach(xi ->
            MathPreconditions.checkEqual("Number of input bits", "1", xi.length, 1));
    }
}
