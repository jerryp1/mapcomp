package edu.alibaba.mpc4j.s2pc.pcg.ot.lot.nc.direct;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.LotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.core.CoreLotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.core.CoreLotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.nc.AbstractNcLotReceiver;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 直接NC-2^l选1-OT协议接收方。
 *
 * @author Hanwen Feng
 * @date 2022/8/18
 */
public class DirectNcLotReceiver extends AbstractNcLotReceiver {
    /**
     * 核2^l选1-OT协议接收方
     */
    private final CoreLotReceiver coreLotReceiver;

    public DirectNcLotReceiver(Rpc receiverRpc, Party senderParty, DirectNcLotConfig config) {
        super(DirectNcLotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreLotReceiver = CoreLotFactory.createReceiver(receiverRpc, senderParty, config.getCoreLotConfig());
        addSubPtos(coreLotReceiver);
    }

    @Override
    public void init(int inputBitLength, int num) throws MpcAbortException {
        setInitInput(inputBitLength, num);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        coreLotReceiver.init(inputBitLength, num);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public LotReceiverOutput receive() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        byte[][] choices = IntStream.range(0, num)
            .mapToObj(index -> {
                byte[] choice = new byte[inputByteLength];
                secureRandom.nextBytes(choice);
                BytesUtils.reduceByteArray(choice, inputBitLength);
                return choice;
            })
            .toArray(byte[][]::new);
        LotReceiverOutput receiverOutput = coreLotReceiver.receive(choices);
        stopWatch.stop();
        long coreLotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        receiverOutput.reduce(num);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, coreLotTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }
}
