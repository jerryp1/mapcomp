package edu.alibaba.mpc4j.s2pc.opf.permutation.xxx23;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.benes.BenesNetworkUtils;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.a2b.A2bFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.a2b.A2bParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.B2aFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.B2aParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcParty;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnSender;
import edu.alibaba.mpc4j.s2pc.opf.permutation.AbstractPermutationSender;

import java.util.Arrays;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Xxx23 permutation sender.
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public class Xxx23PermutationSender extends AbstractPermutationSender {
    /**
     * Osn sender.
     */
    private final OsnSender osnSender;
    /**
     * Zl circuit sender.
     */
    private final ZlcParty zlcSender;
    /**
     * Z2 circuit sender.
     */
    private final Z2cParty z2cSender;
    /**
     * A2b sender.
     */
    private final A2bParty a2bSender;
    /**
     * B2a sender.
     */
    private final B2aParty b2aSender;

    public Xxx23PermutationSender(Rpc senderRpc, Party receiverParty, Xxx23PermutationConfig config) {
        super(Xxx23PermutationPtoDesc.getInstance(), senderRpc, receiverParty, config);
        osnSender = OsnFactory.createSender(senderRpc, receiverParty, config.getOsnConfig());
        zlcSender = ZlcFactory.createSender(senderRpc, receiverParty, config.getZlcConfig());
        z2cSender = Z2cFactory.createSender(senderRpc, receiverParty, config.getZ2cConfig());
        a2bSender = A2bFactory.createSender(senderRpc, receiverParty, config.getA2bConfig());
        b2aSender = B2aFactory.createSender(senderRpc, receiverParty, config.getB2aConfig());
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        osnSender.init(maxNum);
        zlcSender.init(maxNum);
        z2cSender.init(maxNum * maxL);
        a2bSender.init(maxL, maxNum);
        b2aSender.init(maxL, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlVector permute(SquareZlVector perm, ZlVector xi) throws MpcAbortException {
        setPtoInput(perm, xi);
        logPhaseInfo(PtoState.PTO_BEGIN);
        // a2b
        stopWatch.start();
        SquareZ2Vector[] booleanPerm = a2bSender.a2b(perm);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 5, ptoTime);
        // matrix transpose
        stopWatch.start();
        Vector<byte[]> transposedPerm = Arrays.stream(ZlDatabase.create(envType, true, Arrays.stream(booleanPerm)
            .map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new)).getBytesData()).collect(Collectors.toCollection(Vector::new));
        stopWatch.stop();
        ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 5, ptoTime);
        // permute
        stopWatch.start();
        byte[][] permutedBytes = permute(transposedPerm, xi);
        stopWatch.stop();
        ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 5, ptoTime);
        // matrix transpose
        stopWatch.start();
        ZlDatabase database = ZlDatabase.create(l, permutedBytes);
        SquareZ2Vector[] permutedZ2Shares = Arrays.stream(database.bitPartition(envType, true))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        stopWatch.stop();
        ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 5, ptoTime);
        // b2a
        stopWatch.start();
        SquareZlVector permutedZlShares = b2aSender.b2a(permutedZ2Shares);
        stopWatch.stop();
        ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 5, 5, ptoTime);
        logPhaseInfo(PtoState.PTO_END);
        return permutedZlShares;
    }


    public byte[][] permute(Vector<byte[]> perm, ZlVector xi) throws MpcAbortException {
        // osn1
        OsnPartyOutput osnPartyOutput = osnSender.osn(perm, byteL);

        // reveal and permute
        SquareZ2Vector[] osnResultShares = IntStream.range(0, num).mapToObj(osnPartyOutput::getShare)
            .map(v -> SquareZ2Vector.create(l, v, false)).toArray(SquareZ2Vector[]::new);
        int[] perm1 = Arrays.stream(z2cSender.revealOwn(osnResultShares)).map(BitVector::getBytes)
            .mapToInt(v -> BigIntegerUtils.byteArrayToNonNegBigInteger(v).intValue()).toArray();
        Vector<byte[]> osnInputs2 = BenesNetworkUtils.permutation(perm1, Arrays.stream(xi.getElements()).map(v -> BigIntegerUtils.nonNegBigIntegerToByteArray(v, byteL)).collect(Collectors.toCollection(Vector::new)));

        // osn2
        OsnPartyOutput osnPartyOutput2 = osnSender.osn(osnInputs2, byteL);

        // boolean shares
        return IntStream.range(0, num).mapToObj(osnPartyOutput2::getShare).toArray(byte[][]::new);
    }

}