package edu.alibaba.mpc4j.s2pc.opf.groupagg.mix;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.benes.BenesNetworkUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.PlainBitMuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.PlainBitMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPayloadMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPlayloadMuxFactory;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.AbstractGroupAggParty;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggOut;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.mix.MixGroupAggPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnReceiver;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnSender;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggFactory;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggOutput;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggParty;

import java.nio.ByteBuffer;
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
    /**
     * Plain payload mux receiver.
     */
    private final PlainPayloadMuxParty plainPayloadMuxReceiver;
    /**
     * Plain bit mux receiver.
     */
    private final PlainBitMuxParty plainBitMuxReceiver;
    /**
     * Zl mux receiver.
     */
    private final ZlMuxParty zlMuxReceiver;
    /**
     * Prefix aggregation party.
     */
    private final PrefixAggParty prefixAggReceiver;

    public MixGroupAggReceiver(Rpc receiverRpc, Party senderParty, MixGroupAggConfig config) {
        super(MixGroupAggPtoDesc.getInstance(), receiverRpc, senderParty, config);
        osnSender = OsnFactory.createSender(receiverRpc, senderParty, config.getOsnConfig());
        osnReceiver = OsnFactory.createReceiver(receiverRpc, senderParty, config.getOsnConfig());
        plainPayloadMuxReceiver = PlainPlayloadMuxFactory.createReceiver(receiverRpc, senderParty, config.getPlainPayloadMuxConfig());
        plainBitMuxReceiver = PlainBitMuxFactory.createReceiver(receiverRpc, senderParty, config.getPlainBitMuxConfig());
        zlMuxReceiver = ZlMuxFactory.createReceiver(receiverRpc, senderParty, config.getZlMuxConfig());
        prefixAggReceiver = PrefixAggFactory.createPrefixAggReceiver(receiverRpc, senderParty, config.getPrefixAggConfig());
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
    public GroupAggOut groupAgg(String[][] groupField, long[] aggField, SquareZ2Vector e) throws MpcAbortException {
        // 假定receiver拥有agg
        assert aggField != null;
        // obtain sorting permutation
        String[] mergedGroup = mergeString(groupField);
        int[] perms = obtainPerms(mergedGroup);
        // apply perms to group and agg
        String[] permutedGroup = applyPermutation(mergedGroup, perms);
        if (aggField != null) {
            aggField = applyPermutation(aggField, perms);
        }
        // osn
        receiveGroupNum();
        OsnPartyOutput osnPartyOutput = osnReceiver.osn(perms, CommonUtils.getByteLength(otherDistinctGroupNum + 1));
        // transpose
        SquareZ2Vector[] transposed = transposeOsnResult(osnPartyOutput, otherDistinctGroupNum + 1);
        SquareZ2Vector[] bitmapShares = Arrays.stream(transposed, 0, transposed.length - 1).toArray(SquareZ2Vector[]::new);
        e = SquareZ2Vector.create(transposed[transposed.length - 1].getBitVector().xor(e.getBitVector()), false);
        // mul1
        SquareZlVector mul1 = plainPayloadMuxReceiver.mux(e, aggField);
        // temporary array
        PrefixAggOutput[] outputs = new PrefixAggOutput[ownDistinctGroup.size()];
        for (int i = 0; i < ownDistinctGroup.size(); i++) {
            SquareZlVector mul = zlMuxReceiver.mux(bitmapShares[i], mul1);
            // prefix agg
            outputs[i] = prefixAggReceiver.agg(permutedGroup, mul);
        }
        // reveal


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

    private void receiveGroupNum() {
        DataPacketHeader senderDataSizeHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SEND_GROUP_NUM.ordinal(), extraInfo,
            otherParties()[0].getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> senderDataSizePayload = rpc.receive(senderDataSizeHeader).getPayload();
        otherDistinctGroupNum = ByteBuffer.wrap(senderDataSizePayload.get(0)).getInt();
    }
}
