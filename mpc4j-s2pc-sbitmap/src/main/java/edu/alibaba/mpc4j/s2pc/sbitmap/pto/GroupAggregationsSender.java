package edu.alibaba.mpc4j.s2pc.sbitmap.pto;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.ZlMaxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidFactory;
import edu.alibaba.mpc4j.s2pc.sbitmap.main.GroupAggregationConfig;
import edu.alibaba.mpc4j.s2pc.sbitmap.main.GroupAggregationPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.sbitmap.utils.SbitmapUtils;
import smile.data.DataFrame;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Sbitmap group aggregations protocol.
 *
 * @author Li Peng
 * @date 2023/08/03
 */
public class GroupAggregationsSender extends AbstractSbitmapPtoParty implements SbitmapPtoParty {

    public GroupAggregationsSender(Rpc ownRpc, Party otherParty, GroupAggregationConfig groupAggregationConfig) {
        super(ownRpc, otherParty);
        pidParty = PidFactory.createClient(ownRpc, otherParty, groupAggregationConfig.getPidConfig());
        z2cParty= Z2cFactory.createSender(ownRpc, otherParty, groupAggregationConfig.getZ2cConfig());
        zlcParty = ZlcFactory.createSender(ownRpc,otherParty, groupAggregationConfig.getZlcConfig());
        zlMuxParty = ZlMuxFactory.createSender(ownRpc,otherParty, groupAggregationConfig.getZlMuxConfig());
        zlMaxParty = ZlMaxFactory.createSender(ownRpc, otherParty, groupAggregationConfig.getZlMaxConfig());
    }

    /**
     * init the protocol.
     */
    @Override
    public void init() throws MpcAbortException {
        super.initState();
    }

    @Override
    public void stop() {
        pidParty.destroy();
        destroy();
    }

    /**
     * Protocol steps.
     *
     * @param dataFrame dataset.
     * @param config    config.
     * @throws MpcAbortException the protocol failure aborts.
     */
    @Override
    public void run(DataFrame dataFrame, GroupAggregationConfig config) throws MpcAbortException {
        // 交换数据长度
        List<byte[]> senderDataSizePayload = Collections.singletonList(ByteBuffer.allocate(4).putInt(dataFrame.size()).array());
        DataPacketHeader senderDataSizeHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.AND.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParties()[0].getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(senderDataSizeHeader, senderDataSizePayload));

        DataPacketHeader receiverDataSizeHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.AND.ordinal(), extraInfo,
            otherParties()[0].getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> receiverDataSizePayload = rpc.receive(receiverDataSizeHeader).getPayload();
        otherDataSize = ByteBuffer.wrap(receiverDataSizePayload.get(0)).getInt();
        // init
        pidParty.init(dataFrame.size(), otherDataSize);
        // set input
        setPtoInput(dataFrame, config);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // join
        stopWatch.start();
        join();
        stopWatch.stop();
        long slaveSchemaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 5, slaveSchemaTime);

        // generate bitmap
        stopWatch.start();
        bitmapData = SbitmapUtils.createBitmapForNominals(dataFrame);
        stopWatch.stop();
        long ldpTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 5, ldpTime);

        // secret share aggregation field
        stopWatch.start();
        if (hasField(dataFrame.schema(), aggregationField)) {
            aggreSs = zlcParty.shareOwn(ZlVector.createEmpty(zl));
        } else {
            aggreSs = zlcParty.shareOther(rows);
        }

        // secret share group indicator,  and exchange plain grouping keys
        // secret share group indicator
        BitVector[] ownInput = null;
        SquareZ2Vector[] senderAggSs = z2cParty.shareOwn(ownInput);
        SquareZ2Vector[] receiverAggSs = z2cParty.shareOther(IntStream.range(0, ownInput.length).map(i -> rows).toArray());
        senderGroupSize = senderAggSs.length;
        receiverGroupSize = receiverAggSs.length;
        // exchange grouping keys
        List<byte[]> senderGroupingKeyBytes = null;
        DataPacketHeader senderGroupingKeyHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.AND.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParties()[0].getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(senderGroupingKeyHeader, senderGroupingKeyBytes));

        DataPacketHeader receiverGroupingKeyHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.AND.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParties()[0].getPartyId()
        );
        List<byte[]> receiverGroupingKeyPayload = rpc.receive(receiverGroupingKeyHeader).getPayload();
        // assemble plain grouping keys
        List<byte[]> assembledGroupKeys = new ArrayList<>(senderGroupSize * receiverGroupSize);
        for (int i = 0; i < senderGroupSize;i++) {
            for (int j = 0; j < receiverGroupSize;j++) {
                assembledGroupKeys.add(ByteBuffer.allocate((senderGroupNum + receiverGroupNum) * groupKeyByteLength)
                    .put(senderGroupingKeyBytes.get(i)).put(receiverGroupingKeyPayload.get(j)).array());
            }
        }

        // and 没有merge
        SquareZ2Vector[] wholeGroupSs = new SquareZ2Vector[senderGroupSize * receiverGroupSize];
        for (int i = 0; i < senderGroupSize; i++) {
            for (int j = 0; j < receiverGroupSize; j++) {
                wholeGroupSs[i*senderGroupSize + j] = z2cParty.and(senderAggSs[i], receiverAggSs[j]);
            }
        }

        // mux 没有merge
        SquareZlVector[] muxResult = new SquareZlVector[senderGroupSize * receiverGroupSize];
        for (int i = 0; i < senderGroupSize; i++) {
            for (int j = 0; j < receiverGroupSize; j++) {
                muxResult[i*senderGroupSize+j] = zlMuxParty.mux(wholeGroupSs[i*senderGroupSize+j], aggreSs);
            }
        }

        // aggregation ,目前是sum，max
        stopWatch.start();
        SquareZlVector[] maxResult = new SquareZlVector[senderGroupSize * receiverGroupSize];
        for (int i = 0; i < senderGroupSize; i ++) {
            for (int j = 0 ; j < receiverGroupSize; j++) {
                maxResult[i*senderGroupSize+j] = zlMaxParty.max(muxResult[i*senderGroupSize+j]);
            }
        }

        stopWatch.stop();
        long splitNodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 5, 5, splitNodeTime);

        logPhaseInfo(PtoState.PTO_END);
    }

}

