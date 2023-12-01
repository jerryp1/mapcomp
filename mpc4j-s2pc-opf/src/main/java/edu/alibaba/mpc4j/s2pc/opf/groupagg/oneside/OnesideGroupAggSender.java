package edu.alibaba.mpc4j.s2pc.opf.groupagg.oneside;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.B2aFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.B2aParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPayloadMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPlayloadMuxFactory;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.AbstractGroupAggParty;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggOut;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggUtils;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.oneside.OnesideGroupAggPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnSender;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggFactory;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggOutput;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggParty;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.hardcode.HardcodeZ2MtgSender.TRIPLE_NUM;


/**
 * Mix group aggregation sender.
 *
 * @author Li Peng
 * @date 2023/11/3
 */
public class OnesideGroupAggSender extends AbstractGroupAggParty {
    /**
     * Osn sender.
     */
    private final OsnSender osnSender;
    /**
     * Plain payload mux sender.
     */
    private final PlainPayloadMuxParty plainPayloadMuxReceiver;
    /**
     * Zl mux party.
     */
    private final ZlMuxParty zlMuxSender;
    /**
     * Z2 circuit sender.
     */
    private final Z2cParty z2cSender;
    /**
     * Zl circuit party.
     */
    private final ZlcParty zlcSender;
    /**
     * prefix aggregate sender
     */
    private final PrefixAggParty prefixAggSender;

    private final B2aParty b2aSender;

//    private final Z2MuxParty z2MuxSender;

    protected List<String> senderDistinctGroup;

    private SquareZ2Vector[] bitmapShares;

    private SquareZlVector aggZl;

    public OnesideGroupAggSender(Rpc senderRpc, Party receiverParty, OneSideGroupAggConfig config) {
        super(OnesideGroupAggPtoDesc.getInstance(), senderRpc, receiverParty, config);
        osnSender = OsnFactory.createSender(senderRpc, receiverParty, config.getOsnConfig());
        plainPayloadMuxReceiver = PlainPlayloadMuxFactory.createReceiver(senderRpc, receiverParty, config.getPlainPayloadMuxConfig());
        zlMuxSender = ZlMuxFactory.createSender(senderRpc, receiverParty, config.getZlMuxConfig());
        z2cSender = Z2cFactory.createSender(senderRpc, receiverParty, config.getZ2cConfig());
        zlcSender = ZlcFactory.createSender(senderRpc, receiverParty, config.getZlcConfig());
        prefixAggSender = PrefixAggFactory.createPrefixAggSender(senderRpc, receiverParty, config.getPrefixAggConfig());
        b2aSender = B2aFactory.createSender(senderRpc, receiverParty, config.getB2aConfig());
//        z2MuxSender = Z2MuxFactory.createSender(senderRpc, receiverParty, config.getZ2MuxConfig());
//        addMultipleSubPtos(osnSender);
//        addMultipleSubPtos(plainPayloadMuxReceiver);
//        addSubPtos(zlMuxSender);
//        addSubPtos(z2cSender);
//        addSubPtos(zlcSender);
//        addSubPtos(prefixAggSender);
    }

    @Override
    public void init(Properties properties) throws MpcAbortException {
        super.init(properties);
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();

        osnSender.init(maxNum);
        plainPayloadMuxReceiver.init(maxNum);
        zlMuxSender.init(maxNum);
        z2cSender.init(maxNum);
        zlcSender.init(1);
        prefixAggSender.init(maxL, maxNum);
        b2aSender.init(maxL, maxNum);
        // generate distinct group
        senderDistinctGroup = Arrays.asList(GroupAggUtils.genStringSetFromRange(senderGroupBitLength));

        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public GroupAggOut groupAgg(String[] groupField, long[] aggField, SquareZ2Vector intersFlagE) throws MpcAbortException {
        setPtoInput(groupField, aggField, intersFlagE);
        if (aggField == null) {
            // receiver has groups
            group();
        } else {
            // sender has groups
            groupWithSenderAgg();
        }
        // agg
        agg();
        return null;
    }

    private void group() throws MpcAbortException {
        // gen bitmap
        Vector<byte[]> osnInput = genOsnInput(e);
        // osn
        stopWatch.start();
        groupTripleNum = TRIPLE_NUM;
        OsnPartyOutput osnPartyOutput = osnSender.osn(osnInput, osnInput.get(0).length);
        stopWatch.stop();
        groupStep1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        // transpose
        SquareZ2Vector[] transposed = GroupAggUtils.transposeOsnResult(osnPartyOutput, 1);
        e = transposed[0];

        // mul1
        stopWatch.start();
        aggZl = plainPayloadMuxReceiver.mux(e, null, Long.SIZE);
        stopWatch.stop();
        groupStep2Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        groupTripleNum = TRIPLE_NUM - groupTripleNum;
    }

    private void groupWithSenderAgg() throws MpcAbortException {
        // gen bitmap
        Vector<byte[]> bitmaps = genBitmapWithAgg(groupAttr, e, aggAttr);
        // osn
        stopWatch.start();
        groupTripleNum = TRIPLE_NUM;
        int payloadByteLen = CommonUtils.getByteLength(senderGroupNum + 1) + Long.BYTES;
        OsnPartyOutput osnPartyOutput = osnSender.osn(bitmaps, payloadByteLen);
        stopWatch.stop();
        groupStep1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        // transpose
        SquareZ2Vector[] transposed = GroupAggUtils.transposeOsnResult(osnPartyOutput, payloadByteLen * Byte.SIZE);
        bitmapShares = Arrays.stream(transposed, 0, senderGroupNum).toArray(SquareZ2Vector[]::new);
        e = transposed[senderGroupNum];
        // get aggs
        SquareZ2Vector[] aggZ2 = new SquareZ2Vector[Long.SIZE];
        System.arraycopy(transposed, CommonUtils.getByteLength(senderGroupNum + 1) * Byte.SIZE, aggZ2, 0, Long.SIZE);
        aggZl = b2aSender.b2a(aggZ2);

        // mul1
        stopWatch.start();
        aggZl = zlMuxSender.mux(e, aggZl);
        stopWatch.stop();
        groupStep2Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        groupTripleNum = TRIPLE_NUM - groupTripleNum;
    }

    private void agg() throws MpcAbortException {
        aggTripleNum = TRIPLE_NUM;
        // prefix agg
        stopWatch.start();
        PrefixAggOutput outputs = prefixAggSender.agg((String[]) null, aggZl);
        z2cSender.revealOther(outputs.getIndicator());
        zlcSender.revealOther(outputs.getAggs());
        stopWatch.stop();
        aggTime += stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        aggTripleNum = TRIPLE_NUM - aggTripleNum;
    }

    /**
     * Generate horizontal bitmaps.
     *
     * @return vertical bitmaps.
     */
    private Vector<byte[]> genOsnInput(SquareZ2Vector e) {
        return IntStream.range(0, num).mapToObj(i -> {
            byte[] bytes = new byte[CommonUtils.getByteLength(1)];
            BinaryUtils.setBoolean(bytes, 0, e.getBitVector().get(i));
            return bytes;
        }).collect(Collectors.toCollection(Vector::new));
    }

    /**
     * Generate horizontal bitmaps.
     *
     * @param group group.
     * @return vertical bitmaps.
     */
    private Vector<byte[]> genBitmapWithAgg(String[] group, SquareZ2Vector e, long[] aggAtt) {
        int payloadByteLen = CommonUtils.getByteLength(senderGroupNum + 1) + Long.BYTES;
        return IntStream.range(0, group.length).mapToObj(i -> {
            ByteBuffer buffer = ByteBuffer.allocate(payloadByteLen);
            byte[] bytes = new byte[CommonUtils.getByteLength(senderGroupNum + 1)];
            BinaryUtils.setBoolean(bytes, senderDistinctGroup.indexOf(group[i]), true);
            BinaryUtils.setBoolean(bytes, senderGroupNum, e.getBitVector().get(i));
            buffer.put(bytes);
            buffer.put(LongUtils.longToByteArray(aggAtt[i]));
            return buffer.array();
        }).collect(Collectors.toCollection(Vector::new));
    }

    protected void revealOtherGroup(Vector<byte[]> input) {
        List<byte[]> otherShares = new ArrayList<>(input);

        DataPacketHeader sendSharesHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.REVEAL_OUTPUT.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParties()[0].getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(sendSharesHeader, otherShares));
        extraInfo++;
    }
}
