package edu.alibaba.mpc4j.s2pc.opf.groupagg.mix;

import edu.alibaba.mpc4j.common.circuit.prefix.PrefixNode;
import edu.alibaba.mpc4j.common.circuit.prefix.PrefixOp;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Mix group aggregation sender.
 *
 * @author Li Peng
 * @date 2023/11/3
 */
public class MixGroupAggSender extends AbstractGroupAggParty implements PrefixOp {
    /**
     * Osn sender.
     */
    private final OsnSender osnSender;
    /**
     * Osn receiver.
     */
    private final OsnReceiver osnReceiver;
    /**
     * Plain payload mux sender.
     */
    private final PlainPayloadMuxParty plainPayloadMuxSender;
    /**
     * Plain bit mux party.
     */
    private final PlainBitMuxParty plainBitMuxSender;
    /**
     * Zl mux party.
     */
    private final ZlMuxParty zlMuxSender;
    /**
     * prefix aggregate sender
     */
    private final PrefixAggParty prefixAggSender;

    public MixGroupAggSender(Rpc senderRpc, Party receiverParty, MixGroupAggConfig config) {
        super(MixGroupAggPtoDesc.getInstance(), senderRpc, receiverParty, config);
        osnSender = OsnFactory.createSender(senderRpc, receiverParty, config.getOsnConfig());
        osnReceiver = OsnFactory.createReceiver(senderRpc, receiverParty, config.getOsnConfig());
        plainPayloadMuxSender = PlainPlayloadMuxFactory.createSender(senderRpc, receiverParty, config.getPlainPayloadMuxConfig());
        plainBitMuxSender = PlainBitMuxFactory.createSender(senderRpc, receiverParty, config.getPlainBitMuxConfig());
        zlMuxSender = ZlMuxFactory.createSender(senderRpc, receiverParty, config.getZlMuxConfig());
        prefixAggSender = PrefixAggFactory.createPrefixAggSender(senderRpc, receiverParty, config.getPrefixAggConfig());

    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
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
    public GroupAggOut groupAgg(String[][] groupField, long[] aggField, SquareZ2Vector e) throws MpcAbortException {
        // 假定receiver拥有agg
        assert aggField == null;
        // gen bitmap
        String[] mergedGroup = mergeString(groupField);
        Vector<byte[]> bitmaps = genBitmap(mergedGroup, e);
        // osn
        sendGroupNum(ownDistinctGroup.size());
        OsnPartyOutput osnPartyOutput = osnSender.osn(bitmaps, bitmaps.get(0).length);
        // transpose
        SquareZ2Vector[] transposed = transposeOsnResult(osnPartyOutput, ownDistinctGroup.size() + 1);
        SquareZ2Vector[] bitmapShares = Arrays.stream(transposed, 0, transposed.length - 1).toArray(SquareZ2Vector[]::new);
        e = transposed[transposed.length - 1];
        // mul1
        SquareZlVector mul1 = plainPayloadMuxSender.mux(e, null);
        // temporary array
        PrefixAggOutput[] outputs = new PrefixAggOutput[ownDistinctGroup.size()];
        for (int i = 0; i < ownDistinctGroup.size(); i++) {
            SquareZlVector mul = zlMuxSender.mux(bitmapShares[i], mul1);
            // prefix agg
            outputs[i] = prefixAggSender.agg(mergedGroup, mul);
        }
        // reveal

        return null;
    }

    /**
     * Generate vertical bitmaps.
     *
     * @param group group.
     * @return vertical bitmaps.
     */
    private Vector<byte[]> genBitmap(String[] group, SquareZ2Vector e) {
        ownDistinctGroup = Arrays.stream(group).distinct().sorted().collect(Collectors.toList());
        return IntStream.range(0, group.length).mapToObj(i -> {
            BitVector bitVector = BitVectorFactory.createZeros(ownDistinctGroup.size() + 1);
            bitVector.set(ownDistinctGroup.indexOf(group[i]), true);
            bitVector.set(ownDistinctGroup.size(), e.getBitVector().get(i));
            return bitVector.getBytes();
        }).collect(Collectors.toCollection(Vector::new));
    }

    private void sendGroupNum(int groupNum) {
        List<byte[]> receiverDataSizePayload = Collections.singletonList(ByteBuffer.allocate(4).putInt(groupNum).array());
        DataPacketHeader receiverDataSizeHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SEND_GROUP_NUM.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParties()[0].getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(receiverDataSizeHeader, receiverDataSizePayload));
    }

    @Override
    public PrefixNode[] getPrefixSumNodes() {
        return new PrefixNode[0];
    }

    @Override
    public void operateAndUpdate(PrefixNode[] x, PrefixNode[] y, int[] outputIndexes) throws MpcAbortException {

    }
}
