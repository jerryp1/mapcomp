package edu.alibaba.mpc4j.s2pc.opf.shuffle.php24b;

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
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleUtils;

import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Php+24 un-shuffle sender.
 *
 */
public class Php24bShuffleSender extends AbstractShuffleParty {
    /**
     * Osn sender.
     */
    private final OsnSender osnSender;
    /**
     * Osn receiver.
     */
    private final OsnReceiver osnReceiver;

    public Php24bShuffleSender(Rpc senderRpc, Party receiverParty, Php24bShuffleConfig config) {
        super(Php24bShufflePtoDesc.getInstance(), senderRpc, receiverParty, config);
        osnSender = OsnFactory.createSender(senderRpc, receiverParty, config.getOsnConfig());
        osnReceiver = OsnFactory.createReceiver(senderRpc, receiverParty, config.getOsnConfig());
        addMultipleSubPtos(osnSender, osnReceiver);
    }

    @Override
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
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
        int[] originByteLen = x.stream().mapToInt(single -> single.elementAt(0).length).toArray();
        Vector<byte[]> input = x.size() <= 1 ? x.get(0) : merge(x);
        // osn1
        stopWatch.start();
        int[] reversePerm = ShuffleUtils.reversePermutation(randomPerm);
        OsnPartyOutput osn2Output = osnReceiver.osn(reversePerm, input.elementAt(0).length);
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
        OsnPartyOutput osnOutput = osnSender.osn(mergedX, input.elementAt(0).length);
        Vector<byte[]> osnOutputBytes = IntStream.range(0, num)
            .mapToObj(osnOutput::getShare).collect(Collectors.toCollection(Vector::new));

        stopWatch.stop();
        ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, ptoTime);

        // split
        List<Vector<byte[]>> output = x.size() <= 1 ? Collections.singletonList(osnOutputBytes) : split(osnOutputBytes, originByteLen);

        logPhaseInfo(PtoState.PTO_END);
        return output;
    }
}
