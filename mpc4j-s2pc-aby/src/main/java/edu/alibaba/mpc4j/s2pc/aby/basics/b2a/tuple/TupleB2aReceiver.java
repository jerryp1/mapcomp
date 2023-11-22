package edu.alibaba.mpc4j.s2pc.aby.basics.b2a.tuple;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.AbstractB2aParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.tuple.TupleBit2aReceiver;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.pcg.b2a.B2aTupleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.b2a.B2aTupleParty;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiver;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Hardcode B2a Receiver.
 *
 * @author Li Peng
 * @date 2023/11/21
 */
public class TupleB2aReceiver extends AbstractB2aParty {
    /**
     * COT receiver.
     */
    private final CotReceiver cotReceiver;
    /**
     * B2a tuple receiver
     */
    private final B2aTupleParty b2aTupleReceiver;
    /**
     * Z2 circuit receiver.
     */
    private final Z2cParty z2cReceiver;
    /**
     * Tuple bit2a receiver.
     */
    private final TupleBit2aReceiver tupleBit2aReceiver;

    public TupleB2aReceiver(Rpc rpc, Party otherParty, TupleB2aConfig config) {
        super(TupleB2aPtoDesc.getInstance(), rpc, otherParty, config);
        cotReceiver = CotFactory.createReceiver(rpc, otherParty, config.getCotConfig());
        addSubPtos(cotReceiver);
        b2aTupleReceiver = B2aTupleFactory.createReceiver(rpc, otherParty, config.getB2aTupleConfig());
        addSubPtos(b2aTupleReceiver);
        z2cReceiver = Z2cFactory.createReceiver(rpc, otherParty, config.getZ2cConfig());
        addSubPtos(z2cReceiver);
        tupleBit2aReceiver = (TupleBit2aReceiver) Bit2aFactory.createReceiver(rpc, otherParty, config.getTupleBit2aConfig());
        addSubPtos(tupleBit2aReceiver);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        cotReceiver.init(maxNum * maxL);
        b2aTupleReceiver.init(maxNum);
        z2cReceiver.init(maxNum);
        tupleBit2aReceiver.init(maxL, maxNum);
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
            SquareZlVector bit2a = tupleBit2aReceiver.bit2a(input[i]);
            for (int j = 0; j < num; j++) {
                result[j] = zl.add(result[j], bit2a.getZlVector()
                    .getElement(j).multiply(BigInteger.ONE.shiftLeft(l - i - 1)));
            }
        }
        return SquareZlVector.create(zl, result, false);
    }
}
