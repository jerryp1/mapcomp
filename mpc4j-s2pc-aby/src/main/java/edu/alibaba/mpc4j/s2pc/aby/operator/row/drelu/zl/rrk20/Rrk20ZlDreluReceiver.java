package edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.rrk20;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.AbstractZlDreluParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.MillionaireFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.MillionaireParty;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * RRK+20 Zl DReLU Receiver.
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public class Rrk20ZlDreluReceiver extends AbstractZlDreluParty {
    /**
     * Millionaire receiver
     */
    private final MillionaireParty millionaireReceiver;
    /**
     * z2 circuit receiver.
     */
    private final Z2cParty z2cReceiver;
    /**
     * most significant bit.
     */
    private SquareZ2Vector msb;
    /**
     * remaining x
     */
    private byte[][] remainingX;

    public Rrk20ZlDreluReceiver(Rpc receiverRpc, Party senderParty, Rrk20ZlDreluConfig config) {
        super(Rrk20ZlDreluPtoDesc.getInstance(), receiverRpc, senderParty, config);
        millionaireReceiver = MillionaireFactory.createSender(receiverRpc, senderParty, config.getMillionaireConfig());
        addSubPtos(millionaireReceiver);
        z2cReceiver = Z2cFactory.createReceiver(receiverRpc, senderParty, config.getZ2cConfig());
        addSubPtos(z2cReceiver);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        millionaireReceiver.init(maxL, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector drelu(SquareZlVector xi) throws MpcAbortException {
        setPtoInput(xi);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // prepare
        stopWatch.start();
        partitionInputs(xi);
        stopWatch.stop();
        long prepareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, prepareTime);

        // millionaire and xor
        stopWatch.start();
        SquareZ2Vector carry = millionaireReceiver.lt(l, remainingX);
        SquareZ2Vector one = SquareZ2Vector.createOnes(num);
        SquareZ2Vector drelu = z2cReceiver.xor(msb, z2cReceiver.xor(carry, one));
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, ptoTime);

        logPhaseInfo(PtoState.PTO_END);

        return drelu;
    }

    private void partitionInputs(SquareZlVector xi) {
        byte[] msbBytes = new byte[CommonUtils.getByteLength(num)];
        BigInteger[] remaining = new BigInteger[num];
        for (int i = 0; i < num; i++) {
            BigInteger x = xi.getZlVector().getElement(i);
            BinaryUtils.setBoolean(msbBytes, i, x.testBit(zl.getL()));
            remaining[i] = x.setBit(i);
        }
        remainingX = Arrays.stream(remaining)
                .map(v -> BigInteger.ONE.shiftLeft(l - 1).subtract(BigInteger.ONE).subtract(v))
                .map(v -> BigIntegerUtils.nonNegBigIntegerToByteArray(v, CommonUtils.getByteLength(zl.getL() - 1)))
                .toArray(byte[][]::new);
        msb = SquareZ2Vector.create(BitVectorFactory.create(num, msbBytes), false);
    }
}
