package edu.alibaba.mpc4j.s2pc.aby.generic.dabit.zl.egk20;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.generic.dabit.zl.AbstractZlDaBitGenParty;
import edu.alibaba.mpc4j.s2pc.aby.generic.dabit.zl.SquareZlDaBitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcParty;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * EGK+20 Zl (no MAC) daBit generation sender.
 *
 * @author Weiran Liu
 * @date 2023/5/18
 */
public class Egk20NoMacZlDaBitGenSender extends AbstractZlDaBitGenParty {
    /**
     * Zl sender
     */
    private final ZlcParty zlcSender;
    /**
     * Z2 sender
     */
    private final Z2cParty z2cSender;

    public Egk20NoMacZlDaBitGenSender(Rpc senderPpc, Party receiverParty, Egk20NoMacZlDaBitGenConfig config) {
        super(Egk20NoMacZlDaBitGenPtoDesc.getInstance(), senderPpc, receiverParty, config);
        zlcSender = ZlcFactory.createSender(senderPpc, receiverParty, config.getZlcConfig());
        addSubPtos(zlcSender);
        z2cSender = Z2cFactory.createSender(senderPpc, receiverParty, config.getZ2cConfig());
        addSubPtos(z2cSender);
    }

    @Override
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        zlcSender.init(maxNum, maxNum);
        z2cSender.init(maxNum, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlDaBitVector generate(int num) throws MpcAbortException {
        setPtoInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // The parties generate a random bit [b]_{2^k} in the arithmetic part of F_{ABB} by computing a + b − 2ab
        BigInteger[] zlArray = IntStream.range(0, num)
            .mapToObj(index -> {
                boolean b = secureRandom.nextBoolean();
                return b ? zl.createOne() : zl.createZero();
            })
            .toArray(BigInteger[]::new);
        ZlVector randomZlVector0 = ZlVector.create(zl, zlArray);
        BigInteger[] twoArray = IntStream.range(0, num)
            .mapToObj(index -> zl.module(BigIntegerUtils.BIGINT_2))
            .toArray(BigInteger[]::new);
        ZlVector twoZlVector = ZlVector.create(zl, twoArray);
        SquareZlVector squareZlVector00 = zlcSender.shareOwn(randomZlVector0);
        SquareZlVector squareZlVector10 = zlcSender.shareOther(num);
        SquareZlVector addAb0 = zlcSender.add(squareZlVector00, squareZlVector10);
        SquareZlVector mul2Ab0 = zlcSender.mul(squareZlVector00, squareZlVector10);
        mul2Ab0 = zlcSender.mul(mul2Ab0, zlcSender.create(twoZlVector));
        SquareZlVector squareZlVector0 = zlcSender.sub(addAb0, mul2Ab0);
        stopWatch.stop();
        long zlTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, zlTime);

        stopWatch.start();
        ZlVector zlVector0 = squareZlVector0.getZlVector();
        // P0 computes [b_i mod 2]_2. In order to have the MAC, we must explicitly share the Z2 vector.
        BitVector randomZ2Vector0 = BitVectorFactory.createZeros(num);
        for (int index = 0; index < num; index++) {
            randomZ2Vector0.set(index, zlVector0.getElement(index).and(BigInteger.ONE).equals(BigInteger.ONE));
        }
        SquareZ2Vector squareZ2Vector0 = SquareZ2Vector.create(randomZ2Vector0, false);
        SquareZlDaBitVector senderOutput = SquareZlDaBitVector.create(squareZlVector0, squareZ2Vector0);
        stopWatch.stop();
        long z2Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, z2Time);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }
}
