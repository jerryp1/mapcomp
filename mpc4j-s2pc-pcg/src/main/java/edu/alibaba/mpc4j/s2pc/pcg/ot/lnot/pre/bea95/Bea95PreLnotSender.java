package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre.bea95;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre.AbstractPreLnotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre.bea95.Bea95PreLnotPtoDesc.PtoStep;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Bea95 pre-compute 1-out-of-n (with n = 2^l) OT sender.
 *
 * @author Weiran Liu
 * @date 2023/4/11
 */
public class Bea95PreLnotSender extends AbstractPreLnotSender {

    public Bea95PreLnotSender(Rpc senderRpc, Party receiverParty, Bea95PreLnotConfig config) {
        super(Bea95PreLnotPtoDesc.getInstance(), senderRpc, receiverParty, config);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public LnotSenderOutput send(LnotSenderOutput preSenderOutput) throws MpcAbortException {
        setPtoInput(preSenderOutput);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        DataPacketHeader deltaHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_DELTA.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> deltaPayload = rpc.receive(deltaHeader).getPayload();
        int[] deltas = handleDeltaPayload(deltaPayload);
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        byte[][][] shiftRsArray = indexIntStream
            .mapToObj(index -> {
                byte[][] rs = preSenderOutput.getRs(index);
                int delta = deltas[index];
                // shift rs into the correct position
                byte[][] shiftRs = new byte[n][];
                for (int choice = 0; choice < n; choice++) {
                    int shiftPosition = choice - delta;
                    shiftPosition = shiftPosition < 0 ? shiftPosition + n : shiftPosition;
                    shiftRs[choice] = BytesUtils.clone(rs[shiftPosition]);
                }
                return shiftRs;
            })
            .toArray(byte[][][]::new);
        LnotSenderOutput senderOutput = LnotSenderOutput.create(l, shiftRsArray);
        stopWatch.stop();
        long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, time);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private int[] handleDeltaPayload(List<byte[]> deltaPayload) throws MpcAbortException {
        // each row can contain Integer.MAX_VALUE * Byte.BYTES / l number of Δ. Here we ignore Byte.BYTES.
        // When l = 1, rows = (num + maxPerNum - 1) / maxPerNum would exceed Integer.MAX_VALUE, we divide 2.
        int maxPerNum = Integer.MAX_VALUE / 2 / l;
        // number of rows
        int rows = CommonUtils.getUnitNum(num, maxPerNum);
        MpcAbortPreconditions.checkArgument(deltaPayload.size() == rows);
        // parse row
        BigInteger[] rowArray = deltaPayload.stream()
            .map(BigIntegerUtils::byteArrayToNonNegBigInteger)
            .toArray(BigInteger[]::new);
        int[] deltas = new int[num];
        BigInteger mod = BigInteger.valueOf((1L << l) - 1);
        for (int index = num - 1; index >= 0; index--) {
            int rowIndex = index % rows;
            deltas[index] = rowArray[rowIndex].and(mod).intValue();
            rowArray[rowIndex] = rowArray[rowIndex].shiftRight(l);
        }
        return deltas;
    }
}
