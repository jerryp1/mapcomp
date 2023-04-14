package edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.plain.naive;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.plain.AbstractPlainPeqtParty;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * naive plain private equality test receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/14
 */
public class NaivePlainPeqtReceiver extends AbstractPlainPeqtParty {
    /**
     * Boolean circuit receiver
     */
    private final BcParty bcReceiver;

    public NaivePlainPeqtReceiver(Rpc receiverRpc, Party senderParty, NaivePlainPeqtConfig config) {
        super(NaivePlainPeqtPtoDesc.getInstance(), receiverRpc, senderParty, config);
        bcReceiver = BcFactory.createReceiver(receiverRpc, senderParty, config.getBcConfig());
        addSubPtos(bcReceiver);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        bcReceiver.init(maxNum * maxL, maxNum * maxL);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareShareZ2Vector peqt(int l, byte[][] ys) throws MpcAbortException {
        setPtoInput(l, ys);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // transpose ys into bit vectors.
        ZlDatabase zlDatabase = ZlDatabase.create(l, ys);
        BitVector[] y = zlDatabase.bitPartition(envType, parallel);
        stopWatch.stop();
        long prepareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, prepareTime);

        stopWatch.start();
        // P1 gets and sends the share
        int[] nums = new int[l];
        Arrays.fill(nums, num);
        SquareShareZ2Vector[] x1 = bcReceiver.shareOther(nums);
        SquareShareZ2Vector[] y1 = bcReceiver.shareOwn(y);
        stopWatch.stop();
        long shareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, shareTime);

        stopWatch.start();
        SquareShareZ2Vector z1 = SquareShareZ2Vector.createOnes(num);
        // bit-wise XOR and NOT
        SquareShareZ2Vector[] bitwise1 = bcReceiver.xor(x1, y1);
        bitwise1 = bcReceiver.not(bitwise1);
        // AND all results
        for (int i = 0; i < l; i++) {
            z1 = bcReceiver.and(z1, bitwise1[i]);
        }
        stopWatch.stop();
        long bitwiseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, bitwiseTime);

        logPhaseInfo(PtoState.PTO_END);
        return z1;
    }
}
