package edu.alibaba.mpc4j.s2pc.opf.groupagg.mix;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.benes.BenesNetworkUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.AbstractGroupAggParty;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggOut;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnReceiver;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnSender;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.AbstractShuffleParty;

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
 * Mix group aggregation receiver.
 *
 * @author Li Peng
 * @date 2023/11/3
 */
public class MixGroupAggReceiver extends AbstractGroupAggParty {
    /**
     * Osn sender.
     */
    private final OsnSender osnSender;
    /**
     * Osn receiver.
     */
    private final OsnReceiver osnReceiver;

    public MixGroupAggReceiver(Rpc receiverRpc, Party senderParty, MixGroupAggConfig config) {
        super(MixGroupAggPtoDesc.getInstance(), receiverRpc, senderParty, config);
        osnSender = OsnFactory.createSender(receiverRpc, senderParty, config.getOsnConfig());
        osnReceiver = OsnFactory.createReceiver(receiverRpc, senderParty, config.getOsnConfig());
        secureRandom = new SecureRandom();
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        osnReceiver.init(maxNum);
        osnSender.init(maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public GroupAggOut groupAgg(String[][] groupField, long[]... aggField) {
        // obtain sorting permutation

        return null;
    }

    public List<Vector<byte[]>> shuffle(List<Vector<byte[]>> x, int[] randomPerm) throws MpcAbortException {
        setPtoInput(x);
        logPhaseInfo(PtoState.PTO_BEGIN);
        // merge
        int[] originByteLen = x.stream().mapToInt(single -> single.elementAt(0).length).toArray();
        Vector<byte[]> input = x.size() <= 1 ? x.get(0) : merge(x);
        // osn1
        stopWatch.start();
        OsnPartyOutput osnOutput = osnReceiver.osn(randomPerm, input.elementAt(0).length);
        Vector<byte[]> osnOutputBytes = IntStream.range(0, num)
            .mapToObj(osnOutput::getShare).collect(Collectors.toCollection(Vector::new));
        // permute local share and merge
        Vector<byte[]> randomPermutedX = BenesNetworkUtils.permutation(randomPerm, input);
        Vector<byte[]> mergedX = IntStream.range(0, num).mapToObj(i -> BytesUtils.xor(osnOutputBytes.elementAt(i), randomPermutedX.elementAt(i)))
            .collect(Collectors.toCollection(Vector::new));
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, ptoTime);

        // osn2
        stopWatch.start();
        OsnPartyOutput osn2Output = osnSender.osn(mergedX, input.elementAt(0).length);
        Vector<byte[]> osn2OutputBytes = IntStream.range(0, num)
            .mapToObj(osn2Output::getShare).collect(Collectors.toCollection(Vector::new));
        stopWatch.stop();
        ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, ptoTime);
        // split
        List<Vector<byte[]>> output = x.size() <= 1 ? Collections.singletonList(osn2OutputBytes) : split(osn2OutputBytes, originByteLen);
        logPhaseInfo(PtoState.PTO_END);
        return output;
    }

     private int[] obtainPerms(String[] keys){
         Tuple[] tuples = IntStream.range(0, num).mapToObj(j -> new Tuple(keys[j], j)).toArray(Tuple[]::new);
         Arrays.sort(tuples);
         return IntStream.range(0, num).map(j -> tuples[j].getValue()).toArray();
     }

    private static class Tuple implements Comparable<Tuple> {
        private final String key;
        private final int value;

        public Tuple(String key, int value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public int getValue() {
            return value;
        }

        @Override
        public int compareTo(Tuple o) {
            return key.compareTo(o.getKey());
        }
    }
}
