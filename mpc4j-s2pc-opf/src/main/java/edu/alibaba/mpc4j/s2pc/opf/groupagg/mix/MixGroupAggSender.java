package edu.alibaba.mpc4j.s2pc.opf.groupagg.mix;

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
import edu.alibaba.mpc4j.s2pc.opf.groupagg.mix.MixGroupAggPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnSender;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggFactory;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggOutput;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggParty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class MixGroupAggSender extends AbstractGroupAggParty {
    private static final Logger LOGGER = LoggerFactory.getLogger(MixGroupAggSender.class);
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
     * B2a sender
     */
    private final B2aParty b2aSender;
    /**
     * Secret shares of bitmaps.
     */
    private SquareZ2Vector[] bitmapShares;
    /**
     * Aggregation attribute in zl.
     */
    private SquareZlVector aggZl;
    /**
     * A map relation between group value and its index.
     */
    private Map<String, Integer> senderGroupMap;

    public MixGroupAggSender(Rpc senderRpc, Party receiverParty, MixGroupAggConfig config) {
        super(MixGroupAggPtoDesc.getInstance(), senderRpc, receiverParty, config);
        osnSender = OsnFactory.createSender(senderRpc, receiverParty, config.getOsnConfig());
        plainPayloadMuxReceiver = PlainPlayloadMuxFactory.createReceiver(senderRpc, receiverParty, config.getPlainPayloadMuxConfig());
        zlMuxSender = ZlMuxFactory.createSender(senderRpc, receiverParty, config.getZlMuxConfig());
        z2cSender = Z2cFactory.createSender(senderRpc, receiverParty, config.getZ2cConfig());
        zlcSender = ZlcFactory.createSender(senderRpc, receiverParty, config.getZlcConfig());
        prefixAggSender = PrefixAggFactory.createPrefixAggSender(senderRpc, receiverParty, config.getPrefixAggConfig());
        b2aSender = B2aFactory.createSender(senderRpc, receiverParty, config.getB2aConfig());
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
        List<String> senderDistinctGroup = Arrays.asList(GroupAggUtils.genStringSetFromRange(senderGroupBitLength));
        senderGroupMap = new HashMap<>(senderGroupNum);
        for (int i = 0; i < senderGroupNum; i++) {
            senderGroupMap.put(senderDistinctGroup.get(i), i);
        }

        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public GroupAggOut groupAgg(String[] groupField, long[] aggField, SquareZ2Vector intersFlagE) throws MpcAbortException {
        setPtoInput(groupField, aggField, intersFlagE);
        // group
        if (aggField == null) {
            group();
        } else {
            groupWithSenderAgg();
        }
        // agg
        agg();
        return null;
    }

    private void group() throws MpcAbortException {
        // gen bitmap
        Vector<byte[]> bitmaps = genBitmap(groupAttr, e);
        // osn
        stopWatch.start();
        groupTripleNum = TRIPLE_NUM;
        LOGGER.info("osn1");
        OsnPartyOutput osnPartyOutput = osnSender.osn(bitmaps, bitmaps.get(0).length);
        stopWatch.stop();
        groupStep1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        // transpose
        SquareZ2Vector[] transposed = GroupAggUtils.transposeOsnResult(osnPartyOutput, senderGroupNum + 1);
        bitmapShares = Arrays.stream(transposed, 0, transposed.length - 1).toArray(SquareZ2Vector[]::new);
        e = transposed[transposed.length - 1];

        // mul1
        stopWatch.start();
        LOGGER.info("mux1");
        aggZl = plainPayloadMuxReceiver.mux(e, null, 64);
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
        LOGGER.info("osn1");
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
        LOGGER.info("mux1");
        aggZl = zlMuxSender.mux(e, aggZl);
        stopWatch.stop();
        groupStep2Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        groupTripleNum = TRIPLE_NUM - groupTripleNum;
    }

    private void agg() throws MpcAbortException {
        // temporary array
        PrefixAggOutput[] outputs = new PrefixAggOutput[senderGroupNum];
        aggTripleNum = TRIPLE_NUM;
        LOGGER.info("agg");
        for (int i = 0; i < senderGroupNum; i++) {
            stopWatch.start();
            SquareZlVector mul = zlMuxSender.mux(bitmapShares[i], aggZl);
            stopWatch.stop();
            aggTime += stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            // prefix agg
            stopWatch.start();
            outputs[i] = prefixAggSender.agg((String[]) null, mul);
            z2cSender.revealOther(outputs[i].getIndicator());
            zlcSender.revealOther(outputs[i].getAggs());
            stopWatch.stop();
            aggTime += stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
        }
        aggTripleNum = TRIPLE_NUM - aggTripleNum;
    }

    /**
     * Generate horizontal bitmaps.
     *
     * @param group group.
     * @return vertical bitmaps.
     */
    private Vector<byte[]> genBitmap(String[] group, SquareZ2Vector e) {
        return IntStream.range(0, group.length).parallel().mapToObj(i -> {
            byte[] bytes = new byte[CommonUtils.getByteLength(senderGroupNum + 1)];
            BinaryUtils.setBoolean(bytes, senderGroupMap.get(group[i]), true);
            BinaryUtils.setBoolean(bytes, senderGroupNum, e.getBitVector().get(i));
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
        return IntStream.range(0, group.length).parallel().mapToObj(i -> {
            ByteBuffer buffer = ByteBuffer.allocate(payloadByteLen);
            byte[] bytes = new byte[CommonUtils.getByteLength(senderGroupNum + 1)];
            BinaryUtils.setBoolean(bytes, senderGroupMap.get(group[i]), true);
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
