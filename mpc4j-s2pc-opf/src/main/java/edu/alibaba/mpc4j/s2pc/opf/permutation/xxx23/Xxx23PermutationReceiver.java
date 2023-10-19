package edu.alibaba.mpc4j.s2pc.opf.permutation.xxx23;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
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
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnReceiver;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnSender;
import edu.alibaba.mpc4j.s2pc.opf.permutation.AbstractPermutationReceiver;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * RRK+20 Zl Max Receiver.
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public class Xxx23PermutationReceiver extends AbstractPermutationReceiver {
    /**
     * Osn receciver.
     */
    private final OsnReceiver osnReceiver;
    /**
     * Zl circuit receiver.
     */
    private final ZlcParty zlcReceiver;
    /**
     * Z2 circuit receiver.
     */
    private final Z2cParty z2cReceiver;
    /**
     * Secure random.
     */
    private final SecureRandom secureRandom;

    public Xxx23PermutationReceiver(Rpc receiverRpc, Party senderParty, Xxx23PermutationConfig config) {
        super(Xxx23PermutationPtoDesc.getInstance(), receiverRpc, senderParty, config);
        osnReceiver = OsnFactory.createReceiver(receiverRpc, senderParty, config.getOsnConfig());
        zlcReceiver = ZlcFactory.createReceiver(receiverRpc, senderParty, config.getZlcConfig());
        z2cReceiver = Z2cFactory.createReceiver(receiverRpc, senderParty, config.getZ2cConfig());
        secureRandom = new SecureRandom();
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        osnReceiver.init(maxNum);
        zlcReceiver.init(maxNum);
        z2cReceiver.init(maxNum * maxL);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlVector permutate(SquareZlVector perm) throws MpcAbortException {
        setPtoInput(perm);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // a2b
        SquareZ2Vector[] booleanPerm = a2b(perm);
        // matrix transpose
        Vector<byte[]> transposedPerm = Arrays.stream(ZlDatabase.create(envType, true, Arrays.stream(booleanPerm)
            .map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new)).getBytesData()).collect(Collectors.toCollection(Vector::new));
        // permute
        byte[][] permutedBytes = permute(transposedPerm);
        // matrix transpose
        ZlDatabase database = ZlDatabase.create(l, permutedBytes);
        SquareZ2Vector[] permutedZ2Shares = Arrays.stream(database.bitPartition(envType, true))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        // b2a
        SquareZlVector permutedZlShares = b2a(permutedZ2Shares);
        logPhaseInfo(PtoState.PTO_END);
        return permutedZlShares;
    }

    private int[] reversePermutation(int[] perm) {
        int[] result = new int[num];
        for (int i = 0; i < num; i++) {
            result[perm[i]] = i;
        }
        return result;
    }

    private ZlVector applyPermutation(ZlVector x, int[] perm) {
        BigInteger[] xBigInt = x.getElements();
        BigInteger[] result = new BigInteger[num];
        for (int i = 0; i < num; i++) {
            result[i] = xBigInt[perm[i]];
        }
        return ZlVector.create(zl, result);
    }

    private Vector<byte[]> applyPermutation(Vector<byte[]> x, int[] perm) {
        Vector<byte[]> result = new Vector<>(num);
        for (int i = 0; i < num; i++) {
            result.set(i , x.elementAt(perm[i]));
        }
        return result;
    }



    public byte[][] permute(Vector<byte[]> perm) throws MpcAbortException {
        // osn1
        stopWatch.start();

        // generate random permutation
        List<Integer> randomPermList = IntStream.range(0, num)
            .boxed()
            .collect(Collectors.toList());
        Collections.shuffle(randomPermList, secureRandom);
        int[] randomPerm = randomPermList.stream().mapToInt(permutation -> permutation).toArray();
        // locally apply permutation
        Vector<byte[]> permutedPerm = applyPermutation(perm, randomPerm);
        // osn
        OsnPartyOutput osnPartyOutput = osnReceiver.osn(randomPerm, byteL);
        // locally add
        SquareZ2Vector[] osnResultShares = IntStream.range(0, num)
            .mapToObj(i -> BytesUtils.xor(permutedPerm.elementAt(i), osnPartyOutput.getShare(i)))
            .map(v -> SquareZ2Vector.create(num, v, false))
            .toArray(SquareZ2Vector[]::new);

        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, ptoTime);

        // test
//        OsnPartyOutput testOsnPartyOutput = osnReceiver.osn(randomPerm, byteL);
//        SquareZ2Vector[] testOsnResultShares = IntStream.range(0, num).mapToObj(i ->
//            SquareZ2Vector.create(BitVectorFactory.create(l, testOsnPartyOutput.getShare(i)), false)).toArray(SquareZ2Vector[]::new);
//        z2cReceiver.revealOther(testOsnResultShares);


        // reveal and permute
        stopWatch.start();
        z2cReceiver.revealOther(osnResultShares);

        int[] reversePerm = reversePermutation(randomPerm);
        stopWatch.stop();
        ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, ptoTime);

        // osn2
        stopWatch.start();
        OsnPartyOutput osnPartyOutput2 = osnReceiver.osn(reversePerm, byteL);
        stopWatch.stop();
        ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, ptoTime);

        logPhaseInfo(PtoState.PTO_END);

        return IntStream.range(0, num).mapToObj(osnPartyOutput2::getShare).toArray(byte[][]::new);
    }

    private SquareZ2Vector[] a2b(SquareZlVector input) {
        return null;
    }

    private SquareZlVector b2a(SquareZ2Vector[] input) {
        return null;
    }
}
