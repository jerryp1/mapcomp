package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pcot.bea95;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pcot.AbstractPcotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pcot.bea95.Bea95PcotPtoDesc.PtoStep;

/**
 * Bea95-PCOT协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
public class Bea95PcotReceiver extends AbstractPcotReceiver {

    public Bea95PcotReceiver(Rpc receiverRpc, Party senderParty, Bea95PcotConfig config) {
        super(Bea95PcotPtoDesc.getInstance(), receiverRpc, senderParty, config);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        initialized = true;
        info("{}{} Recv. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public CotReceiverOutput receive(CotReceiverOutput preReceiverOutput, boolean[] choices) {
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());
        setPtoInput(preReceiverOutput, choices);

        stopWatch.start();
        byte[] xors = BinaryUtils.binaryToRoundByteArray(choices);
        byte[] preChoiceBytes = BinaryUtils.binaryToRoundByteArray(preReceiverOutput.getChoices());
        BytesUtils.xori(xors, preChoiceBytes);
        List<byte[]> xorPayload = new LinkedList<>();
        xorPayload.add(xors);
        DataPacketHeader xorHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_XOR.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(xorHeader, xorPayload));
        stopWatch.stop();
        long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), time);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        byte[][] rbArray = Arrays.copyOf(preReceiverOutput.getRbArray(), preReceiverOutput.getRbArray().length);
        return CotReceiverOutput.create(choices, rbArray);
    }
}
