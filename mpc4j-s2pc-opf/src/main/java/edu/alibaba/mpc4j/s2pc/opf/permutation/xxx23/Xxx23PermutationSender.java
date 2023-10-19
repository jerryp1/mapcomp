package edu.alibaba.mpc4j.s2pc.opf.permutation.xxx23;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
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

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.common.tool.EnvType.STANDARD_JDK;

/**
 * RRK+20 Zl Max Sender.
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public class Xxx23PermutationSender extends AbstractPermutationSender {
    /**
     * Environment type.
     */
    private static final EnvType ENV_TYPE = STANDARD_JDK;
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


    public Xxx23PermutationSender(Rpc senderRpc, Party receiverParty, Xxx23PermutationConfig config) {
        super(Xxx23PermutationPtoDesc.getInstance(), senderRpc, receiverParty, config);
        osnSender = OsnFactory.createSender(senderRpc, receiverParty, config.getOsnConfig());
        zlcSender = ZlcFactory.createSender(senderRpc, receiverParty, config.getZlcConfig());
        z2cSender = Z2cFactory.createSender(senderRpc, receiverParty, config.getZ2cConfig());
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        osnSender.init(maxNum);
        zlcSender.init(maxNum);
        z2cSender.init(maxNum * maxL);
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
        SquareZ2Vector[] booleanPerm = a2b(perm);
        // matrix transpose
        Vector<byte[]> transposedPerm = Arrays.stream(ZlDatabase.create(envType, true, Arrays.stream(booleanPerm)
            .map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new)).getBytesData()).collect(Collectors.toCollection(Vector::new));
        // permute
        byte[][] permutedBytes = permute(transposedPerm, xi);
        // matrix transpose
        ZlDatabase database = ZlDatabase.create(l, permutedBytes);
        SquareZ2Vector[] permutedZ2Shares = Arrays.stream(database.bitPartition(envType, true)).map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        // b2a
        SquareZlVector permutedZlShares = b2a(permutedZ2Shares);
        logPhaseInfo(PtoState.PTO_END);
        return permutedZlShares;
    }

    private ZlVector applyPermutation(ZlVector x, int[] perm) {
        BigInteger[] xBigInt = x.getElements();
        BigInteger[] result = new BigInteger[num];
        for (int i = 0; i < num; i++) {
            result[i] = xBigInt[perm[i]];
        }
        return ZlVector.create(zl, result);
    }


    public byte[][] permute(Vector<byte[]> perm, ZlVector xi) throws MpcAbortException {
        // osn1
        stopWatch.start();
        OsnPartyOutput osnPartyOutput = osnSender.osn(perm, byteL);

        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, ptoTime);

        // test
//        ZlVector testX = ZlVector.create(zl, IntStream.range(0, num).mapToObj(BigInteger::valueOf).toArray(BigInteger[]::new));
//        Vector<byte[]> testOsnInputs = Arrays.stream(testX.getElements()).map(x ->
//            BigIntegerUtils.nonNegBigIntegerToByteArray(x, byteL)).collect(Collectors.toCollection(Vector::new));
//        OsnPartyOutput testOsnPartyOutput = osnSender.osn(testOsnInputs, byteL);

//        SquareZ2Vector[] testOsnResultShares = IntStream.range(0, num).mapToObj(i ->
//            SquareZ2Vector.create(BitVectorFactory.create(l, testOsnPartyOutput.getShare(i)), false)).toArray(SquareZ2Vector[]::new);
//        BigInteger[] result = Arrays.stream(z2cSender.revealOwn(testOsnResultShares)).map(v -> BigIntegerUtils.byteArrayToNonNegBigInteger(v.getBytes())).toArray(BigInteger[]::new);
//        System.out.println(123);

        // reveal and permute
        stopWatch.start();
        SquareZlVector osnResultShares = SquareZlVector.create(zl, IntStream.range(0, num).mapToObj(osnPartyOutput::getShare)
            .map(BigIntegerUtils::byteArrayToNonNegBigInteger).toArray(BigInteger[]::new), false);
        int[] perm1 = Arrays.stream(zlcSender.revealOwn(osnResultShares).getElements()).mapToInt(BigInteger::intValue).toArray();
        ZlVector permutedX = applyPermutation(xi, perm1);
        Vector<byte[]> osnInputs2 = Arrays.stream(permutedX.getElements()).map(x ->
            BigIntegerUtils.nonNegBigIntegerToByteArray(x, byteL)).collect(Collectors.toCollection(Vector::new));
        stopWatch.stop();
        ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, ptoTime);

        // osn2
        stopWatch.start();
        OsnPartyOutput osnPartyOutput2 = osnSender.osn(osnInputs2, byteL);
        stopWatch.stop();
        ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, ptoTime);

        // boolean shares
        return IntStream.range(0, num).mapToObj(osnPartyOutput2::getShare).toArray(byte[][]::new);
    }

    private SquareZ2Vector[] a2b(SquareZlVector input) {
        return null;
    }

    private SquareZlVector b2a(SquareZ2Vector[] input) {
        return null;
    }

}
