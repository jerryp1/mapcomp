package edu.alibaba.mpc4j.s2pc.aby.operator.psorter.ahi22;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
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
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxParty;

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
    /**
     * Zl mux sender.
     */
    private final ZlMuxParty zlMuxSender;

    public Ahi22PermutableSorterSender(Rpc rpc, Party otherParty, Ahi22PermutableSorterConfig config) {
        super(Ahi22PermutableSorterPtoDesc.getInstance(), rpc, otherParty, config);
        bit2aSender = Bit2aFactory.createSender(rpc, otherParty, config.getBit2aConfig());
        zlcSender = ZlcFactory.createSender(rpc, otherParty, config.getZlcConfig());
        zlMuxSender = ZlMuxFactory.createSender(rpc, otherParty, config.getZlMuxConfig());
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
        zlMuxSender.init(maxNum);
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


        stopWatch.start();
        // reveal
//        ZlVector[] trueS0 = zlcSender.revealOwn(s0);
//        ZlVector[] trueS1 = zlcSender.revealOwn(s1);
//        SquareZlVector[] result = new SquareZlVector[numSort];
//        for (int i = 0; i < numSort; i ++) {
//            result[i] = zlcSender.add(s0[i], zlMuxSender.mux(xiArray[i], zlcSender.sub(s1[i], s0[i])));
//        }
        SquareZlVector[] result = zlcSender.add(s0, zlMuxSender.mux(xiArray, zlcSender.sub(s1, s0)));
        stopWatch.stop();
        long resultTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        System.out.println("### b2a: " + b2aTime + " ms.");
        System.out.println("### s0s1: " + s0s1Time + " ms.");
        System.out.println("### result: " + resultTime + " ms.");

        return result;
    }

    void checkInputs(MpcZ2Vector[][] xiArrays) {
        Arrays.stream(xiArrays).forEach(xi ->
            MathPreconditions.checkEqual("Number of input bits", "1", xi.length, 1));
    }

//    private SquareZlVector[] mul(SquareZlVector[] x) throws MpcAbortException {
////        MathPreconditions.checkEqual("x.length", "y.length", x.length, y.length);
//        Prg prg = PrgFactory.createInstance(envType, byteL);
////        BitVector[] inputBits = Arrays.stream(y).map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new);
//        for (int i = 0; i < numSort; i++) {
//            CotSenderOutput cotSenderOutput = cotSender.send(num);
//            // generate random rs
//            BigInteger[] rs = ZlVector.createRandom(zl, num, secureRandom).getElements();
//            int finalI = i;
//            byte[][] r0s = IntStream.range(0, num)
//                .mapToObj(j -> rs[finalI])
//                .map(r -> BigIntegerUtils.nonNegBigIntegerToByteArray(r, byteL))
//                .toArray(byte[][]::new);
//            byte[][] r1s = IntStream.range(0, num)
//                .mapToObj(j -> zl.add(rs[finalI], x[]))
//                .map(r -> BigIntegerUtils.nonNegBigIntegerToByteArray(r, byteL))
//                .toArray(byte[][]::new);
//        }
//    }
}
