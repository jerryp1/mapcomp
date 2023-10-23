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
import java.util.concurrent.TimeUnit;

/**
 * Ahi22 Permutable Sorter Receiver.
 *
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
    public SquareZlVector sort(SquareZ2Vector[] xiArray) throws MpcAbortException {
        checkInputs(xiArray);
        setPtoInput(xiArray);
        logPhaseInfo(PtoState.PTO_BEGIN);
        SquareZ2Vector xi = xiArray[0];

        stopWatch.start();
        SquareZlVector result = execute(xi);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);

        logStepInfo(PtoState.PTO_STEP, 1, 1, ptoTime);

        logPhaseInfo(PtoState.PTO_END);

        return result;
    }

    void checkInputs(MpcZ2Vector[] xiArrays) {
        MathPreconditions.checkEqual("Number of input bits", "1", xiArrays.length, 1);
    }

    private SquareZlVector execute(SquareZ2Vector xi) throws MpcAbortException {
        SquareZlVector f1 = bit2aReceiver.bit2a(xi);
        SquareZlVector ones = SquareZlVector.createOnes(zl, num);

        BigInteger[] f0BigInt = zlcReceiver.sub(ones, f1).getZlVector().getElements();
        BigInteger[] f1BigInt = f1.getZlVector().getElements();
        BigInteger[] s0BigInt = new BigInteger[num];
        BigInteger[] s1BigInt = new BigInteger[num];
        for (int i = 0; i < num; i++) {
            if (i == 0) {
                s0BigInt[i] = f0BigInt[i];
                continue;
            }
            s0BigInt[i] = zl.add(s0BigInt[i - 1], f0BigInt[i]);
        }

        for (int i = 0; i < num; i++) {
            if (i == 0) {
                s1BigInt[i] = zl.add(s0BigInt[num - 1], f1BigInt[i]);
                continue;
            }
            s1BigInt[i] = zl.add(s1BigInt[i - 1], f1BigInt[i]);
        }

        SquareZlVector s0 = SquareZlVector.create(zl, s0BigInt, false);
        SquareZlVector s1 = SquareZlVector.create(zl, s1BigInt, false);
        return zlcReceiver.add(s0, zlMuxReceiver.mux(xi, zlcReceiver.sub(s1, s0)));
    }
}
