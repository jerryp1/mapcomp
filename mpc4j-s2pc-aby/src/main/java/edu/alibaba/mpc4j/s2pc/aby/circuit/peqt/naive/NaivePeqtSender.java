package edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.naive;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.AbstractPeqtParty;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * naive private equality test sender.
 *
 * @author Weiran Liu
 * @date 2023/4/14
 */
public class NaivePeqtSender extends AbstractPeqtParty {
    /**
     * Boolean circuit sender
     */
    private final BcParty bcSender;

    public NaivePeqtSender(Rpc senderRpc, Party receiverParty, NaivePeqtConfig config) {
        super(NaivePeqtPtoDesc.getInstance(), senderRpc, receiverParty, config);
        bcSender = BcFactory.createSender(senderRpc, receiverParty, config.getBcConfig());
        addSubPtos(bcSender);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        bcSender.init(maxNum * maxL, maxNum * maxL);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareShareZ2Vector peqt(int l, byte[][] xs) throws MpcAbortException {
        setPtoInput(l, xs);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // transpose xs into bit vectors.
        ZlDatabase zlDatabase = ZlDatabase.create(l, xs);
        BitVector[] x = zlDatabase.bitPartition(envType, parallel);
        stopWatch.stop();
        long prepareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, prepareTime);

        stopWatch.start();
        // P0 sends and gets the share
        SquareShareZ2Vector[] x0 = bcSender.shareOwn(x);
        int[] nums = new int[l];
        Arrays.fill(nums, num);
        SquareShareZ2Vector[] y0 = bcSender.shareOther(nums);
        stopWatch.stop();
        long shareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, shareTime);

        stopWatch.start();
        // bit-wise XOR and NOT
        SquareShareZ2Vector[] eqs0 = bcSender.xor(x0, y0);
        eqs0 = bcSender.not(eqs0);
        // tree-based AND
        int logL = LongUtils.ceilLog2(l);
        for (int h = 1; h <= logL; h++) {
            int nodeNum = eqs0.length / 2;
            SquareShareZ2Vector[] eqsx0 = new SquareShareZ2Vector[nodeNum];
            SquareShareZ2Vector[] eqsy0 = new SquareShareZ2Vector[nodeNum];
            for (int i = 0; i < nodeNum; i++) {
                eqsx0[i] = eqs0[i * 2];
                eqsy0[i] = eqs0[i * 2 + 1];
            }
            SquareShareZ2Vector[] eqsz0 = bcSender.and(eqsx0, eqsy0);
            if (eqs0.length % 2 == 1) {
                eqsz0 = Arrays.copyOf(eqsz0, nodeNum + 1);
                eqsz0[nodeNum] = eqs0[eqs0.length - 1];
            }
            eqs0 = eqsz0;
        }
        stopWatch.stop();
        long bitwiseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, bitwiseTime);

        logPhaseInfo(PtoState.PTO_END);
        return eqs0[0];
    }
}
