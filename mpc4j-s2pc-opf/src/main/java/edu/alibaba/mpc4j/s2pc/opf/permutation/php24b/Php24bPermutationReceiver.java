package edu.alibaba.mpc4j.s2pc.opf.permutation.php24b;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.crypto.matrix.TransposeUtils;
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
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnReceiver;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnSender;
import edu.alibaba.mpc4j.s2pc.opf.permutation.AbstractPermutationReceiver;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Php+24 permutation receiver.
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public class Php24bPermutationReceiver extends AbstractPermutationReceiver {
    /**
     * Osn sender.
     */
    private final OsnSender osnSender;
    /**
     * Osn receiver.
     */
    private final OsnReceiver osnReceiver;
    /**
     * Zl circuit receiver.
     */
    private final ZlcParty zlcReceiver;
    /**
     * Z2 circuit sender.
     */
    private final Z2cParty z2cSender;
    /**
     * A2b Receiver.
     */
    private final A2bParty a2bReceiver;
    /**
     * B2a Receiver.
     */
    private final B2aParty b2aReceiver;

    public Php24bPermutationReceiver(Rpc receiverRpc, Party senderParty, Php24bPermutationConfig config) {
        super(Php24bPermutationPtoDesc.getInstance(), receiverRpc, senderParty, config);
        osnSender = OsnFactory.createSender(receiverRpc, senderParty, config.getOsnConfig());
        osnReceiver = OsnFactory.createReceiver(receiverRpc, senderParty, config.getOsnConfig());
        zlcReceiver = ZlcFactory.createReceiver(receiverRpc, senderParty, config.getZlcConfig());
        z2cSender = Z2cFactory.createSender(receiverRpc, senderParty, config.getZ2cConfig());
        a2bReceiver = A2bFactory.createReceiver(receiverRpc, senderParty, config.getA2bConfig());
        b2aReceiver = B2aFactory.createReceiver(receiverRpc, senderParty, config.getB2aConfig());
        addMultipleSubPtos(osnReceiver, osnSender, zlcReceiver, z2cSender, a2bReceiver, b2aReceiver);
        secureRandom = new SecureRandom();
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        osnReceiver.init(maxNum);
        osnSender.init(maxNum);
        zlcReceiver.init(maxNum);
        z2cSender.init(maxNum * maxL);
        a2bReceiver.init(maxL, maxNum);
        b2aReceiver.init(maxL, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlVector permute(SquareZlVector perm) throws MpcAbortException {
        setPtoInput(perm);
        logPhaseInfo(PtoState.PTO_BEGIN);
        // a2b
        stopWatch.start();
        SquareZ2Vector[] booleanPerm = a2bReceiver.a2b(perm);
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
        logStepInfo(PtoState.PTO_STEP, 1, 5, ptoTime);
        // permute
        stopWatch.start();
        Vector<byte[]> permutedBytes = permute(transposedPerm, perm.getZl().getByteL());
        stopWatch.stop();
        ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 5, ptoTime);
        // matrix transpose
        stopWatch.start();
        SquareZ2Vector[] permutedZ2Shares = Arrays.stream(TransposeUtils.transposeSplit(permutedBytes, l))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        stopWatch.stop();
        ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 5, ptoTime);
        stopWatch.start();
        // b2a
        SquareZlVector permutedZlShares = b2aReceiver.b2a(permutedZ2Shares);
        stopWatch.stop();
        ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 5, ptoTime);
        logPhaseInfo(PtoState.PTO_END);
        return permutedZlShares;
    }

    @Override
    public Vector<byte[]> permute(Vector<byte[]> perm, int inputByteL) throws MpcAbortException {
        setPtoInput(perm);
        int permByteL = perm.get(0).length;
        // osn1
        OsnPartyOutput osnPartyOutput = osnSender.osn(perm, permByteL);
        // reveal and permute
        SquareZ2Vector[] osnResultShares = IntStream.range(0, num).mapToObj(osnPartyOutput::getShare)
            .map(v -> SquareZ2Vector.create(permByteL * Byte.SIZE, v, false)).toArray(SquareZ2Vector[]::new);
        int[] perms = Arrays.stream(z2cSender.revealOwn(osnResultShares)).map(BitVector::getBytes)
            .mapToInt(v -> BigIntegerUtils.byteArrayToNonNegBigInteger(v).intValue()).toArray();
        int[] reversePerms = reversePermutation(perms);
        // osn2
        OsnPartyOutput osnPartyOutput2 = osnReceiver.osn(reversePerms, inputByteL);
        // boolean shares
        return IntStream.range(0, num).mapToObj(osnPartyOutput2::getShare).collect(Collectors.toCollection(Vector::new));
    }

}
