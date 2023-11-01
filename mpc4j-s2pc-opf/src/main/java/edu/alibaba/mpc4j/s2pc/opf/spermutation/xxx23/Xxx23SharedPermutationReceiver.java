package edu.alibaba.mpc4j.s2pc.opf.spermutation.xxx23;

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
import edu.alibaba.mpc4j.s2pc.opf.spermutation.xxx23.Xxx23SharedPermutationPtoDesc.PtoStep;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Xxx+23 shared permutation receiver.
 *
 * @author Li Peng
 * @date 2023/5/25
 */
public class Xxx23SharedPermutationReceiver extends AbstractSharedPermutationParty {
    /**
     * Shuffle receiver.
     */
    private final ShuffleParty shuffleReceiver;
    /**
     * Un-shuffle receiver.
     */
    private final ShuffleParty unShuffleReceiver;

    public Xxx23SharedPermutationReceiver(Rpc receiverRpc, Party senderParty, Xxx23SharedPermutationConfig config) {
        super(Xxx23SharedPermutationPtoDesc.getInstance(), receiverRpc, senderParty, config);
        shuffleReceiver = ShuffleFactory.createReceiver(receiverRpc, senderParty, config.getShuffleConfig());
        unShuffleReceiver = ShuffleFactory.createReceiver(receiverRpc, senderParty, config.getUnShuffleConfig());
        secureRandom = new SecureRandom();
    }

    @Override
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        shuffleReceiver.init(maxNum);
        unShuffleReceiver.init(maxNum);
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
        List<Vector<byte[]>> shuffledInputs = shuffleReceiver.shuffle(Collections.singletonList(perms), randomPerms);
        Vector<byte[]> shuffledPerms = shuffledInputs.get(0);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, ptoTime);

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

        // apply permutation
        Vector<byte[]> permutedX = BenesNetworkUtils.permutation(plainPerms, x);
        stopWatch.stop();
        ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, ptoTime);

        // un-shuffle
        stopWatch.start();
        List<Vector<byte[]>> unShuffledX = unShuffleReceiver.shuffle(Collections.singletonList(permutedX), randomPerms);

        stopWatch.stop();
        ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, ptoTime);

        logPhaseInfo(PtoState.PTO_END);
        return unShuffledX.get(0);
    }
}
