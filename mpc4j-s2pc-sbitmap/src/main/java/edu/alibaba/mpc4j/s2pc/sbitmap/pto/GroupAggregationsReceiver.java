package edu.alibaba.mpc4j.s2pc.sbitmap.pto;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidFactory;
import edu.alibaba.mpc4j.s2pc.sbitmap.main.SbitmapConfig;
import edu.alibaba.mpc4j.s2pc.sbitmap.main.SbitmapPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.sbitmap.utils.SbitmapUtils;
import smile.data.DataFrame;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
//        slaveLdpDataFrame = SbitmapUtils.ldpDataFrame(dataFrame, config.getLdpConfigMap());
        stopWatch.stop();
        long ldpTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 5, ldpTime);

        // TODO 加入聚合算子操作（count/sum）@风笛


        stopWatch.start();
//        List<byte[]> slaveDataPayload = generateSlaveDataPayload();
        DataPacketHeader slaveDataHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.AND.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParties()[0].getPartyId()
        );
//        rpc.send(DataPacket.fromByteArrayList(slaveDataHeader, slaveDataPayload));
        stopWatch.stop();
        long slaveDataTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 5, slaveDataTime);

        stopWatch.start();
        DataPacketHeader slaveOrderSplitsHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.AND.ordinal(), extraInfo,
            otherParties()[0].getPartyId(), ownParty().getPartyId()
        );
//        List<byte[]> slaveOrderSplitsPayload = rpc.receive(slaveOrderSplitsHeader).getPayload();
        stopWatch.stop();
        long orderSplitsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 5, orderSplitsTime);

        stopWatch.start();
//        List<byte[]> slaveSplitsPayload = generateSplitsNodes(slaveOrderSplitsPayload);
        DataPacketHeader slaveSplitsHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.AND.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParties()[0].getPartyId()
        );
//        rpc.send(DataPacket.fromByteArrayList(slaveSplitsHeader, slaveSplitsPayload));
        stopWatch.stop();
        long splitNodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 5, 5, splitNodeTime);

        logPhaseInfo(PtoState.PTO_END);
    }
}