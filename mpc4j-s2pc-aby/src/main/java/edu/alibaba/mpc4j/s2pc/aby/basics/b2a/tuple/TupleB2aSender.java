package edu.alibaba.mpc4j.s2pc.aby.basics.b2a.tuple;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.AbstractB2aParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.tuple.TupleBit2aSender;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.pcg.b2a.B2aTupleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.b2a.B2aTupleParty;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSender;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * DSZ15 B2a Sender.
 *
 * @author Li Peng
 * @date 2023/11/21
 */
public class TupleB2aSender extends AbstractB2aParty {
    /**
     * COT sender.
     */
    private final CotSender cotSender;
    /**
     * B2a tuple sender.
     */
    private final B2aTupleParty b2aTupleSender;
    /**
     * Z2 circuit sender.
     */
    private final Z2cParty z2cSender;
    /**
     * tuple bit2a sender
     */
    private final TupleBit2aSender tupleBit2aSender;

    public TupleB2aSender(Rpc rpc, Party otherParty, TupleB2aConfig config) {
        super(TupleB2aPtoDesc.getInstance(), rpc, otherParty, config);
        cotSender = CotFactory.createSender(rpc, otherParty, config.getCotConfig());
        addSubPtos(cotSender);
        b2aTupleSender = B2aTupleFactory.createSender(rpc, otherParty, config.getB2aTupleConfig());
        addSubPtos(b2aTupleSender);
        z2cSender = Z2cFactory.createSender(rpc, otherParty, config.getZ2cConfig());
        addSubPtos(z2cSender);
        tupleBit2aSender = (TupleBit2aSender) Bit2aFactory.createSender(rpc, otherParty, config.getTupleBit2aConfig());
        addSubPtos(tupleBit2aSender);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        cotSender.init(delta, maxNum * maxL);
        b2aTupleSender.init(maxNum);
        z2cSender.init(maxNum);
        tupleBit2aSender.init(maxL, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlVector b2a(SquareZ2Vector[] xi) throws MpcAbortException {
        setPtoInput(xi);

        logPhaseInfo(PtoState.PTO_BEGIN);

        // b2aa
        stopWatch.start();
        SquareZlVector r = b2aWithTuple();
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, ptoTime);


        logPhaseInfo(PtoState.PTO_END);
        return r;
    }

    private SquareZlVector b2aWithTuple() throws MpcAbortException {
        BigInteger[] result = new BigInteger[num];
        IntStream.range(0, num).forEach(i -> result[i] = BigInteger.ZERO);
        for (int i = 0; i < l; i++) {
            SquareZlVector bit2a = tupleBit2aSender.bit2a(input[i]);
            for (int j = 0; j < num; j++) {
                result[j] = zl.add(result[j], bit2a.getZlVector()
                    .getElement(j).multiply(BigInteger.ONE.shiftLeft(l - i - 1)));
            }
        }
        return SquareZlVector.create(zl, result, false);
    }

}
