package edu.alibaba.mpc4j.s2pc.opf.shuffle.xxx23b;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.benes.BenesNetworkUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnReceiver;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnSender;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.AbstractShuffleParty;

import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Xxx+23 un-shuffle sender.
 *
 * @author Li Peng
 * @date 2023/5/26
 */
public class Xxx23bShuffleSender extends AbstractShuffleParty {
    /**
     * Osn sender.
     */
    private final OsnSender osnSender;
    /**
     * Osn receiver.
     */
    private final OsnReceiver osnReceiver;

    public Xxx23bShuffleSender(Rpc senderRpc, Party receiverParty, Xxx23bShuffleConfig config) {
        super(Xxx23bShufflePtoDesc.getInstance(), senderRpc, receiverParty, config);
        osnSender = OsnFactory.createSender(senderRpc, receiverParty, config.getOsnConfig());
        osnReceiver = OsnFactory.createReceiver(senderRpc, receiverParty, config.getOsnConfig());
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        osnSender.init(maxNum);
        osnReceiver.init(maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public List<Vector<byte[]>> shuffle(List<Vector<byte[]>> x, int[] randomPerm) throws MpcAbortException {
        setPtoInput(x);
        logPhaseInfo(PtoState.PTO_BEGIN);
        // merge
        Vector<byte[]> input = x.size() <= 1 ? x.get(0) : merge(x);
        // osn1
        stopWatch.start();
        int[] reversePerm = reversePermutation(randomPerm);
        OsnPartyOutput osn2Output = osnReceiver.osn(reversePerm, byteL);
        Vector<byte[]> osn2OutputBytes = IntStream.range(0, num)
            .mapToObj(osn2Output::getShare).collect(Collectors.toCollection(Vector::new));
        // permute local share and merge
        Vector<byte[]> randomPermutedX = BenesNetworkUtils.permutation(reversePerm, input);
        Vector<byte[]> mergedX = IntStream.range(0, num).mapToObj(i -> BytesUtils.xor(osn2OutputBytes.elementAt(i), randomPermutedX.elementAt(i)))
            .collect(Collectors.toCollection(Vector::new));
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, ptoTime);
        // osn2
        stopWatch.start();
        OsnPartyOutput osnOutput = osnSender.osn(mergedX, byteL);
        Vector<byte[]> osnOutputBytes = IntStream.range(0, num)
            .mapToObj(osnOutput::getShare).collect(Collectors.toCollection(Vector::new));

        stopWatch.stop();
        ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, ptoTime);

        // split
        List<Vector<byte[]>> output = x.size() <= 1 ? Collections.singletonList(osnOutputBytes) : split(osnOutputBytes, x.size());

        logPhaseInfo(PtoState.PTO_END);
        return output;
    }


}