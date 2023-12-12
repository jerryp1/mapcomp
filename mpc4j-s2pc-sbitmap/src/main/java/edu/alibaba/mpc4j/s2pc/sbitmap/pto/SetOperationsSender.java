package edu.alibaba.mpc4j.s2pc.sbitmap.pto;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidFactory;
import edu.alibaba.mpc4j.s2pc.sbitmap.main.GroupAggregationConfig;
import edu.alibaba.mpc4j.s2pc.sbitmap.main.GroupAggregationPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.sbitmap.utils.SbitmapUtils;
import smile.data.DataFrame;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Sbitmap set operations protocol.
 *
 * @author Li Peng
 * @date 2023/08/03
 */
public class SetOperationsSender extends AbstractSbitmapPtoParty implements SbitmapPtoParty {

    public SetOperationsSender(Rpc ownRpc, Party otherParty, GroupAggregationConfig groupAggregationConfig) {
        super(ownRpc, otherParty);
        pidParty = PidFactory.createClient(ownRpc, otherParty, groupAggregationConfig.getPidConfig());
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
     * @param dataFrame dataset
     * @param config    config
     * @throws MpcAbortException if the protocol aborts.
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
        System.out.println(bitmapData);
        stopWatch.stop();
        long ldpTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 5, ldpTime);

        // secret share
        stopWatch.start();
//        List<byte[]> senderDataSizePayload = generateSlaveDataPayload();
        DataPacketHeader slaveDataHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.AND.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParties()[0].getPartyId()
        );
//        rpc.send(DataPacket.fromByteArrayList(senderDataSizeHeader, senderDataSizePayload));
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
