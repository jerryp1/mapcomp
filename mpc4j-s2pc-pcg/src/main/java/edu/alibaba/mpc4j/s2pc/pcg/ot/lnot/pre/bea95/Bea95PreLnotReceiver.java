package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre.bea95;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre.AbstractPreLnotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre.bea95.Bea95PreLnotPtoDesc.PtoStep;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Bea95 pre-compute 1-out-of-n (with n = 2^l) OT receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/11
 */
public class Bea95PreLnotReceiver extends AbstractPreLnotReceiver {

    public Bea95PreLnotReceiver(Rpc receiverRpc, Party senderParty, Bea95PreLnotConfig config) {
        super(Bea95PreLnotPtoDesc.getInstance(), receiverRpc, senderParty, config);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public LnotReceiverOutput receive(LnotReceiverOutput preReceiverOutput, int[] choiceArray) {
        logPhaseInfo(PtoState.PTO_BEGIN);
        setPtoInput(preReceiverOutput, choiceArray);

        stopWatch.start();
        List<byte[]> deltaPayload = generateDeltaPayload();
        DataPacketHeader deltaHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_DELTA.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(deltaHeader, deltaPayload));
        byte[][] rsArray = IntStream.range(0, num)
            .mapToObj(preReceiverOutput::getRb)
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);
        LnotReceiverOutput receiverOutput = LnotReceiverOutput.create(l, choiceArray, rsArray);
        stopWatch.stop();
        long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, time);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private List<byte[]> generateDeltaPayload() {
        // each row can contain Integer.MAX_VALUE * Byte.BYTES / l number of Δ. Here we ignore Byte.BYTES.
        // When l = 1, rows = (num + maxPerNum - 1) / maxPerNum would exceed Integer.MAX_VALUE, we divide 2.
        int maxPerNum = Integer.MAX_VALUE / 2 / l;
        // number of rows
        int rows = CommonUtils.getUnitNum(num, maxPerNum);
        BigInteger[] rowArray = IntStream.range(0, rows).mapToObj(index -> BigInteger.ZERO).toArray(BigInteger[]::new);
        for (int index = 0; index < num; index++) {
            int rowIndex = index % rows;
            int randomChoice = preReceiverOutput.getChoice(index);
            int delta = choiceArray[index] - randomChoice;
            delta = delta < 0 ? delta + n : delta;
            rowArray[rowIndex] = rowArray[rowIndex].shiftLeft(l).add(BigInteger.valueOf(delta));
        }
        return Arrays.stream(rowArray)
            .map(row -> {
                int byteLength = CommonUtils.getByteLength(row.bitLength());
                return BigIntegerUtils.nonNegBigIntegerToByteArray(row, byteLength);
            })
            .collect(Collectors.toList());
    }
}
