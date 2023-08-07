package edu.alibaba.mpc4j.s2pc.sbitmap.main;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPto;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidFactory;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidParty;
import edu.alibaba.mpc4j.s2pc.sbitmap.main.SbitmapPtoDesc.PtoStep;
import smile.data.DataFrame;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Sbitmap set operations protocol.
 *
 * @author Li Peng
 * @date 2023/08/03
 */
public class SetOperationsReceiver extends AbstractMultiPartyPto implements SbitmapPtoParty {
    /**
     * dataset
     */
    private DataFrame dataFrame;
    /**
     * number of rows.
     */
    private int rows;
    /**
     * total bytes of rows.
     */
    private int byteRows;
    /**
     * row offset.
     */
    private int rowOffset;
    /**
     * ldp dataset
     */
    private DataFrame slaveLdpDataFrame;
    /**
     * pid receiver.
     */
    private PidParty pidReceiver;

    public SetOperationsReceiver(Rpc slaveRpc, Party hostParty, SbitmapConfig sbitmapConfig) {
        super(SbitmapPtoDesc.getInstance(), new SbitmapPtoConfig(), slaveRpc, hostParty);
        pidReceiver = PidFactory.createServer(slaveRpc, hostParty, sbitmapConfig.getPidConfig());
    }

    /**
     * init the protocol.
     */
    @Override
    public void init() throws MpcAbortException {
        super.initState();
        pidReceiver.init(1000000, 1000000);
    }

    @Override
    public void stop() {
        pidReceiver.destroy();
        destroy();
    }

    /**
     * TODO 协议真实执行过程
     *
     * @param dataFrame
     * @param config
     * @throws MpcAbortException
     */
    @Override
    public void run(DataFrame dataFrame, SbitmapConfig config) throws MpcAbortException {
        setPtoInput(dataFrame, config);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        pidReceiver.pid(null, 1000000);
        stopWatch.stop();
        long slaveSchemaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 5, slaveSchemaTime);

        stopWatch.start();
        slaveLdpDataFrame = SbitmapUtils.ldpDataFrame(dataFrame, config.getLdpConfigMap());
        stopWatch.stop();
        long ldpTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 5, ldpTime);

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
        List<byte[]> slaveOrderSplitsPayload = rpc.receive(slaveOrderSplitsHeader).getPayload();
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

    private void setPtoInput(DataFrame slaveDataFrame, SbitmapConfig slaveConfig) {
        checkInitialized();
        // 验证DataFrame与配置参数中的schema相同
        assert slaveDataFrame.schema().equals(slaveConfig.getSchema());
        this.dataFrame = slaveDataFrame;
        rows = slaveDataFrame.nrows();
        byteRows = CommonUtils.getByteLength(rows);
        rowOffset = byteRows * Byte.SIZE - rows;
        extraInfo++;
    }
}