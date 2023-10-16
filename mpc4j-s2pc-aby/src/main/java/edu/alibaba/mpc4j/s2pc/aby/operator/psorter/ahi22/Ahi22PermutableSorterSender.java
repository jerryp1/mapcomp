package edu.alibaba.mpc4j.s2pc.aby.operator.psorter.ahi22;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
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
public class Ahi22PermutableSorterSender extends AbstractPermutableSorterParty {
    /**
     * Bit2a sender.
     */
    private final Bit2aParty bit2aSender;
    /**
     * Zl circuit sender.
     */
    private final ZlcParty zlcSender;

    public Ahi22PermutableSorterSender(Rpc rpc, Party otherParty, Ahi22PermutableSorterConfig config) {
        super(Ahi22PermutableSorterPtoDesc.getInstance(), rpc, otherParty, config);
        bit2aSender = Bit2aFactory.createSender(rpc, otherParty, config.getBit2aConfig());
        zlcSender = ZlcFactory.createSender(rpc, otherParty, config.getZlcConfig());
        zl = config.getBit2aConfig().getZl();
        l = zl.getL();
        byteL = zl.getByteL();
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        bit2aSender.init(maxL, maxNum);
        zlcSender.init(maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlVector[] sort(SquareZ2Vector[][] xiArrays) throws MpcAbortException {
//        stopWatch.start();
        checkInputs(xiArrays);
        setPtoInput(xiArrays);
//        stopWatch.stop();
//        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
//        stopWatch.reset();
//        System.out.println("### init: " + initTime + " ms.");

        stopWatch.start();
        SquareZ2Vector[] xiArray = Arrays.stream(xiArrays).map(xi -> xi[0]).toArray(SquareZ2Vector[]::new);
        // 先尝试分开写
//        SquareZlVector[] f1 = new SquareZlVector[numSort];
//        for (int i = 0; i < numSort; i++) {
//            f1[i] =  bit2aSender.bit2a(xiArray[i]);
//        }
        SquareZlVector[] f1 = bit2aSender.bit2a(xiArray);
        stopWatch.stop();
        long b2aTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        System.out.println("### b2a: " + b2aTime + " ms.");

        stopWatch.start();
        SquareZlVector[] ones = IntStream.range(0, numSort).mapToObj(i -> SquareZlVector.createOnes(zl, num)).toArray(SquareZlVector[]::new);

        SquareZlVector[] f0 = zlcSender.sub(ones, f1);
        SquareZlVector[] s0 = new SquareZlVector[numSort];
        SquareZlVector[] s1 = new SquareZlVector[numSort];
        // s0
        for (int i = 0; i < numSort; i++) {
            if (i == 0) {
                BigInteger[] zeros = IntStream.range(0, num).mapToObj(j -> BigInteger.ZERO).toArray(BigInteger[]::new);
                s0[i] = zlcSender.add(SquareZlVector.create(zl, zeros, false),f0[i]);
                continue;
            }
            s0[i] = zlcSender.add(s0[i - 1], f0[i]);
        }
        // s1
        for (int i = 0; i < numSort; i++) {
            if (i == 0) {
                s1[i] = zlcSender.add(s0[numSort- 1], f1[i]);
                continue;
            }
            s1[i] = zlcSender.add(s1[i - 1], f1[i]);
        }
        stopWatch.stop();
        long s0s1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        System.out.println("### s0s1: " + s0s1Time + " ms.");

        stopWatch.start();
        // reveal
//        ZlVector[] trueS0 = zlcSender.revealOwn(s0);
//        ZlVector[] trueS1 = zlcSender.revealOwn(s1);

        SquareZlVector[] result = zlcSender.add(s0, zlcSender.mul(f1, zlcSender.sub(s1, s0)));
        stopWatch.stop();
        long resultTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        System.out.println("### result: " + resultTime + " ms.");

        return result;
    }

    void checkInputs(MpcZ2Vector[][] xiArrays) {
        Arrays.stream(xiArrays).forEach(xi ->
            MathPreconditions.checkEqual("Number of input bits", "1", xi.length, 1));
    }

}
