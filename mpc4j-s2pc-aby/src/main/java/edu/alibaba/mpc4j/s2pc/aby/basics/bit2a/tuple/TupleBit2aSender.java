package edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.tuple;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.AbstractBit2aParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.pcg.b2a.B2aTuple;
import edu.alibaba.mpc4j.s2pc.pcg.b2a.B2aTupleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.b2a.B2aTupleParty;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSender;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Tuple Bit2a Sender.
 *
 * @author Li Peng
 * @date 2023/11/21
 */
public class TupleBit2aSender extends AbstractBit2aParty {
    /**
     * COT sender.
     */
    private final CotSender cotSender;
    /**
     * Z2 circuit party.
     */
    private final Z2cParty z2cSender;
    /**
     * B2a tuple party.
     */
    private final B2aTupleParty b2aTupleSender;

    public TupleBit2aSender(Rpc rpc, Party otherParty, TupleBit2aConfig config) {
        super(TupleBit2aPtoDesc.getInstance(), rpc, otherParty, config);
        cotSender = CotFactory.createSender(rpc, otherParty, config.getCotConfig());
        z2cSender = Z2cFactory.createSender(rpc, otherParty, config.getZ2cConfig());
        b2aTupleSender = B2aTupleFactory.createSender(rpc, otherParty, config.getB2aTupleConfig());
        addMultipleSubPtos(cotSender, z2cSender);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        cotSender.init(delta, maxNum);
        z2cSender.init(maxNum);
        b2aTupleSender.init(maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlVector bit2a(SquareZ2Vector xi) throws MpcAbortException {
        setPtoInput(xi);

        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        SquareZlVector result = b2aWithTuple();
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, ptoTime);

        logPhaseInfo(PtoState.PTO_END);
        return result;
    }

    private SquareZlVector b2aWithTuple() throws MpcAbortException {
        // generate tuples
        B2aTuple tuple = b2aTupleSender.generate(num);

        BitVector inputBitVector = tuple.getA().xor(input.getBitVector());
        z2cSender.revealOther(SquareZ2Vector.create(inputBitVector, false));
        BitVector xor = z2cSender.revealOwn(SquareZ2Vector.create(inputBitVector, false));
        BigInteger[] result = new BigInteger[num];
        for (int j = 0; j < num; j++) {
            if (xor.get(j)) {
                // receiver compute - b
                result[j] = zl.sub(BigInteger.ZERO, tuple.getB().getElement(j));
            } else {
                result[j] = tuple.getB().getElement(j);
            }
        }
        return SquareZlVector.create(zl, result, false);
    }

    @Override
    public SquareZlVector[] bit2a(SquareZ2Vector[] xiArray) throws MpcAbortException {
        // merge
        SquareZ2Vector mergedXiArray = SquareZ2Vector.mergeWithPadding(xiArray);
        // bit2a
        SquareZlVector mergedZiArray = bit2a(mergedXiArray);
        // split
        int[] nums = Arrays.stream(xiArray)
            .mapToInt(SquareZ2Vector::getNum).toArray();

        return Arrays.stream(ZlVector.splitWithPadding(mergedZiArray.getZlVector(), nums))
            .map(z -> SquareZlVector.create(z, false)).toArray(SquareZlVector[]::new);
    }
}
