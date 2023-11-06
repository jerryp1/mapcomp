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
import edu.alibaba.mpc4j.s2pc.sbitmap.main.SbitmapConfig;
import edu.alibaba.mpc4j.s2pc.sbitmap.main.SbitmapPtoDesc.PtoStep;
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
public class GroupAggregationsReceiver extends AbstractSbitmapPtoParty implements SbitmapPtoParty {

    public GroupAggregationsReceiver(Rpc ownRpc, Party otherParty, SbitmapConfig sbitmapConfig) {
        super(ownRpc, otherParty);
        pidParty = PidFactory.createServer(ownRpc, otherParty, sbitmapConfig.getPidConfig());
        z2cParty= Z2cFactory.createReceiver(ownRpc, otherParty,sbitmapConfig.getZ2cConfig());
        zlcParty = ZlcFactory.createReceiver(ownRpc,otherParty,sbitmapConfig.getZlcConfig());
        zlMuxParty = ZlMuxFactory.createReceiver(ownRpc,otherParty,sbitmapConfig.getZlMuxConfig());
        zlMaxParty = ZlMaxFactory.createReceiver(ownRpc, otherParty, sbitmapConfig.getZlMaxConfig());
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
    public void run(DataFrame dataFrame, SbitmapConfig config) throws MpcAbortException {
        // 交换数据长度
        List<byte[]> receiverDataSizePayload = Collections.singletonList(ByteBuffer.allocate(4).putInt(dataFrame.size()).array());
        DataPacketHeader receiverDataSizeHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.AND.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParties()[0].getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(receiverDataSizeHeader, receiverDataSizePayload));

        DataPacketHeader senderDataSizeHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.AND.ordinal(), extraInfo,
            otherParties()[0].getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> senderDataSizePayload = rpc.receive(senderDataSizeHeader).getPayload();
        otherDataSize = ByteBuffer.wrap(senderDataSizePayload.get(0)).getInt();
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
        stopWatch.start();
        // secret share
        BitVector[] ownInput = null;
        SquareZ2Vector[] senderAggSs = z2cParty.shareOther(IntStream.range(0, ownInput.length).map(i -> rows).toArray());
        SquareZ2Vector[] receiverAggSs = z2cParty.shareOwn(ownInput);
        senderGroupSize = senderAggSs.length;
        receiverGroupSize = receiverAggSs.length;

        // exchange grouping keys
        DataPacketHeader senderGroupingKeyHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.AND.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParties()[0].getPartyId()
        );
        List<byte[]> senderGroupingKeyPayload = rpc.receive(senderGroupingKeyHeader).getPayload();

        List<byte[]> receiverGroupingKeyBytes = null;
        DataPacketHeader receiverGroupingKeyHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.AND.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParties()[0].getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(receiverGroupingKeyHeader, receiverGroupingKeyBytes));

        // assemble plain grouping keys
        List<byte[]> assembledGroupKeys = new ArrayList<>(senderGroupSize * receiverGroupSize);
        for (int i = 0; i < senderGroupSize;i++) {
            for (int j = 0; j < receiverGroupSize;j++) {
                assembledGroupKeys.add(ByteBuffer.allocate((senderGroupNum + receiverGroupNum) * groupKeyByteLength)
                    .put(receiverGroupingKeyBytes.get(i)).put(senderGroupingKeyPayload.get(j)).array());
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
