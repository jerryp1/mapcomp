package edu.alibaba.mpc4j.s2pc.opf.permutation.php24;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.benes.BenesNetworkUtils;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.crypto.matrix.TransposeUtils;
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
 * Php+24 permutation sender.
 */
public class Php24PermutationSender extends AbstractPermutationSender {
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

    public Php24PermutationSender(Rpc senderRpc, Party receiverParty, Php24PermutationConfig config) {
        super(Php24PermutationPtoDesc.getInstance(), senderRpc, receiverParty, config);
        osnSender = OsnFactory.createSender(senderRpc, receiverParty, config.getOsnConfig());
        zlcSender = ZlcFactory.createSender(senderRpc, receiverParty, config.getZlcConfig());
        z2cSender = Z2cFactory.createSender(senderRpc, receiverParty, config.getZ2cConfig());
        a2bSender = A2bFactory.createSender(senderRpc, receiverParty, config.getA2bConfig());
        b2aSender = B2aFactory.createSender(senderRpc, receiverParty, config.getB2aConfig());
        addMultipleSubPtos(osnSender, zlcSender, z2cSender, a2bSender, b2aSender);
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
        int byteL = xi.getZl().getByteL();
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
        Vector<byte[]> transposedPerm = TransposeUtils.transposeMergeToVector(Arrays.stream(booleanPerm)
            .map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new));
        stopWatch.stop();
        ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 5, ptoTime);
        // permute
        stopWatch.start();
        Vector<byte[]> permutedBytes = permute(transposedPerm, Arrays.stream(xi.getElements())
            .map(v -> BigIntegerUtils.nonNegBigIntegerToByteArray(v, byteL)).collect(Collectors.toCollection(Vector::new)));
        stopWatch.stop();
        ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 5, ptoTime);
        // matrix transpose
        stopWatch.start();
        SquareZ2Vector[] permutedZ2Shares = Arrays.stream(TransposeUtils.transposeSplit(permutedBytes, l))
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


    @Override
    public Vector<byte[]> permute(Vector<byte[]> perm, Vector<byte[]> xi) throws MpcAbortException {
        setPtoInput(perm, xi);
        int inputByteL = xi.get(0).length;
        int permByteL = perm.get(0).length;
        // osn1
        OsnPartyOutput osnPartyOutput = osnSender.osn(perm, permByteL);

        // reveal and permute
        SquareZ2Vector[] osnResultShares = IntStream.range(0, num).mapToObj(osnPartyOutput::getShare)
            .map(v -> SquareZ2Vector.create(permByteL * Byte.SIZE, v, false)).toArray(SquareZ2Vector[]::new);
        int[] perm1 = Arrays.stream(z2cSender.revealOwn(osnResultShares)).map(BitVector::getBytes)
            .mapToInt(v -> BigIntegerUtils.byteArrayToNonNegBigInteger(v).intValue()).toArray();
        Vector<byte[]> osnInputs2 = BenesNetworkUtils.permutation(perm1, xi);

        // osn2
        OsnPartyOutput osnPartyOutput2 = osnSender.osn(osnInputs2, inputByteL);

        // boolean shares
        return IntStream.range(0, num).mapToObj(osnPartyOutput2::getShare).collect(Collectors.toCollection(Vector::new));
    }

}
