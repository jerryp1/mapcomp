package edu.alibaba.mpc4j.s2pc.opf.spermutation.php24b;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.benes.BenesNetworkUtils;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleFactory;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleParty;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleUtils;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.AbstractSharedPermutationParty;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.php24b.Php24bSharedPermutationPtoDesc.PtoStep;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Php+24 shared permutation receiver.
 *
 */
public class Php24bSharedPermutationReceiver extends AbstractSharedPermutationParty {
    /**
     * Shuffle receiver.
     */
    private final ShuffleParty shuffleReceiver;

    public Php24bSharedPermutationReceiver(Rpc receiverRpc, Party senderParty, Php24bSharedPermutationConfig config) {
        super(Php24bSharedPermutationPtoDesc.getInstance(), receiverRpc, senderParty, config);
        shuffleReceiver = ShuffleFactory.createReceiver(receiverRpc, senderParty, config.getShuffleConfig());
        addSubPtos(shuffleReceiver);
        secureRandom = new SecureRandom();
    }

    @Override
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        shuffleReceiver.init(maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Vector<byte[]> permute(Vector<byte[]> perms, Vector<byte[]> x) throws MpcAbortException {
        setPtoInput(perms, x);
        logPhaseInfo(PtoState.PTO_BEGIN);
        // shuffle
        stopWatch.start();
        int[] randomPerms = ShuffleUtils.generateRandomPerm(num);
        List<Vector<byte[]>> shuffledInputs = shuffleReceiver.shuffle(Arrays.asList(perms, x), randomPerms);
        Vector<byte[]> shuffledPerms = shuffledInputs.get(0);
        Vector<byte[]> shuffledX = shuffledInputs.get(1);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, ptoTime);

        // reveal
        stopWatch.start();
        DataPacketHeader revealHeader2 = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.REVEAL1.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> reveal2Payload = rpc.receive(revealHeader2).getPayload();
        int[] plainPerms = IntStream.range(0, num).mapToObj(i -> BytesUtils.xor(shuffledPerms.get(i), reveal2Payload.get(i)))
            .mapToInt(v -> BigIntegerUtils.byteArrayToNonNegBigInteger(v).intValue()).toArray();

        DataPacketHeader revealHeader1 = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.REVEAL2.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(revealHeader1, new ArrayList<>(shuffledPerms)));

        // reverse permutation
        int[] reversedPlainPerms = ShuffleUtils.reversePermutation(plainPerms);
        // apply permutation
        Vector<byte[]> permutedX = BenesNetworkUtils.permutation(reversedPlainPerms, shuffledX);
        stopWatch.stop();
        ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, ptoTime);
        logPhaseInfo(PtoState.PTO_END);
        return permutedX;
    }
}
