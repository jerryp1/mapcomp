package edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.oneside;

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
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.ZlDreluFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.ZlDreluParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPayloadMuxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPlayloadMuxFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnSender;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.AbstractGroupAggParty;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.GroupAggOut;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.GroupAggUtils;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.oneside.OnesideGroupAggPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggFactory;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggOutput;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggParty;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.hardcode.HardcodeZ2MtgSender.TRIPLE_NUM;


/**
 * Mix group aggregation sender.
 *
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
    /**
     * B2a sender.
     */
    private final B2aParty b2aSender;
    /**
     * Zl Drelu sender.
     */
    private final ZlDreluParty zlDreluSender;
    /**
     * Sender distinct group.
     */
    protected List<String> senderDistinctGroup;
    /**
     * Aggregation attribute in zl.
     */
    private SquareZlVector aggZl;
    /**
     * Summation in zl.
     */
    private SquareZlVector sumZl;

    public OnesideGroupAggSender(Rpc senderRpc, Party receiverParty, OneSideGroupAggConfig config) {
        super(OnesideGroupAggPtoDesc.getInstance(), senderRpc, receiverParty, config);
        osnSender = OsnFactory.createSender(senderRpc, receiverParty, config.getOsnConfig());
        plainPayloadMuxReceiver = PlainPlayloadMuxFactory.createReceiver(senderRpc, receiverParty, config.getPlainPayloadMuxConfig());
        zlMuxSender = ZlMuxFactory.createSender(senderRpc, receiverParty, config.getZlMuxConfig());
        z2cSender = Z2cFactory.createSender(senderRpc, receiverParty, config.getZ2cConfig());
        zlcSender = ZlcFactory.createSender(senderRpc, receiverParty, config.getZlcConfig());
        prefixAggSender = PrefixAggFactory.createPrefixAggSender(senderRpc, receiverParty, config.getPrefixAggConfig());
        b2aSender = B2aFactory.createSender(senderRpc, receiverParty, config.getB2aConfig());
        zlDreluSender = ZlDreluFactory.createSender(senderRpc, receiverParty, config.getZlDreluConfig());
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
        zlDreluSender.init(maxL, maxNum);
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
        if (havingState) {
            getSum();
        }
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
        Vector<byte[]> osnInput = genOsnInput(e, aggAttr);
        // osn
        stopWatch.start();
        groupTripleNum = TRIPLE_NUM;
        int payloadByteLen = dummyPayload ? CommonUtils.getByteLength(1) + 2 * Long.BYTES :
            CommonUtils.getByteLength(1) + Long.BYTES;
        OsnPartyOutput osnPartyOutput = osnSender.osn(osnInput, payloadByteLen);
        stopWatch.stop();
        groupStep1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        // transpose
        SquareZ2Vector[] transposed = GroupAggUtils.transposeOsnResult(osnPartyOutput, payloadByteLen * Byte.SIZE);
        e = transposed[0];
        // get aggs
        SquareZ2Vector[] aggZ2 = new SquareZ2Vector[Long.SIZE];
        System.arraycopy(transposed, Byte.SIZE, aggZ2, 0, Long.SIZE);
        aggZl = b2aSender.b2a(aggZ2);

        // mul1
        stopWatch.start();
        aggZl = zlMuxSender.mux(e, aggZl);
        stopWatch.stop();
        groupStep2Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        groupTripleNum = TRIPLE_NUM - groupTripleNum;

        if (dummyPayload) {
            zlMuxSender.mux(e, b2aSender.b2a(aggZ2));
        }
    }

    private void agg() throws MpcAbortException {
        aggTripleNum = TRIPLE_NUM;
        // prefix agg
        stopWatch.start();
        PrefixAggOutput outputs = prefixAggSender.agg((String[]) null, aggZl);
        if (dummyPayload) {
            prefixAggSender.agg((String[]) null, aggZl);
        }
        z2cSender.revealOther(outputs.getIndicator());
        // if q11 then compare
        SquareZlVector aggTemp = outputs.getAggs();
        if (havingState) {
            SquareZ2Vector compare = zlDreluSender.drelu(zlcSender.sub(aggTemp, sumZl));
            z2cSender.revealOther(compare);
        }
        zlcSender.revealOther(aggTemp);
        stopWatch.stop();
        aggTime += stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        aggTripleNum = TRIPLE_NUM - aggTripleNum;
    }

    private void getSum() throws MpcAbortException {
        SquareZlVector mul = plainPayloadMuxReceiver.mux(e, aggAttr, Long.SIZE);
        BigInteger sum = Arrays.stream(mul.getZlVector().getElements()).reduce(BigInteger.ZERO, (a, b) -> zl.add(a, b));
        sumZl = SquareZlVector.create(zl, IntStream.range(0, num).mapToObj(i -> sum).toArray(BigInteger[]::new), false);
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
     * @return vertical bitmaps.
     */
    private Vector<byte[]> genOsnInput(SquareZ2Vector e, long[] aggAtt) {
        int payloadByteLen = dummyPayload ? CommonUtils.getByteLength(senderGroupNum + 1) + 2 * Long.BYTES :
            CommonUtils.getByteLength(senderGroupNum + 1) + Long.BYTES;
        IntStream intStream = parallel ? IntStream.range(0, num).parallel() : IntStream.range(0, num);
        return intStream.mapToObj(i -> {
            ByteBuffer buffer = ByteBuffer.allocate(payloadByteLen);
            byte[] bytes = new byte[1];
            BinaryUtils.setBoolean(bytes, 0, e.getBitVector().get(i));
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
