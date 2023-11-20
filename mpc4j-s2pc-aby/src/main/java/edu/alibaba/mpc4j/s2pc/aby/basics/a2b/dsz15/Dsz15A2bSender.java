package edu.alibaba.mpc4j.s2pc.aby.basics.a2b.dsz15;

import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.crypto.matrix.TransposeUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.aby.basics.a2b.AbstractA2bParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSender;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;


/**
 * DSZ15 A2b Sender.
 *
 * @author Li Peng
 * @date 2023/10/20
 */
public class Dsz15A2bSender extends AbstractA2bParty {
    /**
     * COT sender.
     */
    private final CotSender cotSender;
    /**
     * Z2 circuit sender
     */
    private final Z2cParty z2cSender;
    /**
     * Z2 integer circuit
     */
    private final Z2IntegerCircuit z2IntegerCircuit;

    public Dsz15A2bSender(Rpc rpc, Party otherParty, Dsz15A2bConfig config) {
        super(Dsz15A2bPtoDesc.getInstance(), rpc, otherParty, config);
        cotSender = CotFactory.createSender(rpc, otherParty, config.getCotConfig());
        addSubPtos(cotSender);
        z2cSender = Z2cFactory.createSender(rpc, otherParty, config.getZ2cConfig());
        addSubPtos(z2cSender);
        z2IntegerCircuit = new Z2IntegerCircuit(z2cSender);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        cotSender.init(delta, maxNum * maxL);
        z2cSender.init(maxNum * maxL);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector[] a2b(SquareZlVector xi) throws MpcAbortException {
        setPtoInput(xi);

        logPhaseInfo(PtoState.PTO_BEGIN);
        // transpose and re-share
        stopWatch.start();
        BitVector[] bitVectors = TransposeUtils.transposeSplit(xi.getZlVector().getElements(), l);
        int[] nums = IntStream.range(0, l).map(i -> num).toArray();
        SquareZ2Vector[] reSharedX0 = z2cSender.shareOwn(bitVectors);
        SquareZ2Vector[] reSharedX1 = z2cSender.shareOther(nums);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, ptoTime);

        // add
        stopWatch.start();
        SquareZ2Vector[] result = Arrays.stream(z2IntegerCircuit.add(reSharedX0, reSharedX1))
            .map(v -> (SquareZ2Vector) v).toArray(SquareZ2Vector[]::new);
        stopWatch.stop();
        ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, ptoTime);

        logPhaseInfo(PtoState.PTO_END);
        return result;
    }

    @Override
    public SquareZ2Vector[][] a2b(SquareZlVector[] xi) throws MpcAbortException {
        setPtoInputs(xi);

        logPhaseInfo(PtoState.PTO_BEGIN);
        // transpose and re-share
        stopWatch.start();
        BitVector[][] bitVectors = Arrays.stream(xi).map(x -> TransposeUtils.transposeSplit(x.getZlVector().getElements(),l)).toArray(BitVector[][]::new);
        // merge
        BitVector[] mergeRes = IntStream.range(0, bitVectors[0].length).mapToObj(i -> {
            BitVector[] tmp = Arrays.stream(bitVectors).map(x -> x[i]).toArray(BitVector[]::new);
            return BitVectorFactory.mergeWithPadding(tmp);
        }).toArray(BitVector[]::new);
        // re-share
        int[] nums = IntStream.range(0, l).map(i -> mergeRes[0].bitNum()).toArray();
        SquareZ2Vector[] reSharedX0 = z2cSender.shareOwn(mergeRes);
        SquareZ2Vector[] reSharedX1 = z2cSender.shareOther(nums);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, ptoTime);

        // add
        stopWatch.start();
        SquareZ2Vector[] midResult = Arrays.stream(z2IntegerCircuit.add(reSharedX0, reSharedX1))
            .map(v -> (SquareZ2Vector) v).toArray(SquareZ2Vector[]::new);
        int[] targetBitLens = Arrays.stream(xi).mapToInt(SquareZlVector::getNum).toArray();
        BitVector[][] vec = Arrays.stream(midResult).map(x -> x.getBitVector().splitWithPadding(targetBitLens)).toArray(BitVector[][]::new);
        SquareZ2Vector[][] result = IntStream.range(0, vec[0].length).mapToObj(i ->
            Arrays.stream(vec).map(vectors -> SquareZ2Vector.create(vectors[i], xi[0].isPlain()))
                .toArray(SquareZ2Vector[]::new)).toArray(SquareZ2Vector[][]::new);
        stopWatch.stop();
        ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, ptoTime);

        logPhaseInfo(PtoState.PTO_END);
        return result;
    }
}
