package edu.alibaba.mpc4j.s2pc.opf.oprf.ra17;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.AbstractMpOprfSender;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfSenderOutput;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * RA17-MPOPRF协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/02/07
 */
public class Ra17MpOprfSender extends AbstractMpOprfSender {
    /**
     * 椭圆曲线
     */
    private final Ecc ecc;
    /**
     * 是否压缩编码
     */
    private final boolean compressEncode;
    /**
     * 密钥
     */
    private BigInteger alpha;

    public Ra17MpOprfSender(Rpc senderRpc, Party receiverParty, Ra17MpOprfConfig config) {
        super(Ra17MpOprfPtoDesc.getInstance(), senderRpc, receiverParty, config);
        ecc = EccFactory.createInstance(envType);
        compressEncode = config.getCompressEncode();
    }

    @Override
    public void init(int maxBatchSize, int maxPrfNum) {
        setInitInput(maxBatchSize, maxPrfNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // 生成α
        alpha = ecc.randomZn(secureRandom);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public MpOprfSenderOutput oprf(int batchSize) throws MpcAbortException {
        setPtoInput(batchSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        DataPacketHeader blindHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Ra17MpOprfPtoDesc.PtoStep.RECEIVER_SEND_BLIND.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> blindPayload = rpc.receive(blindHeader).getPayload();

        stopWatch.start();
        List<byte[]> blindPrf = handleBlindPayload(blindPayload);
        DataPacketHeader blindPrfHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), Ra17MpOprfPtoDesc.PtoStep.SENDER_SEND_BLIND_PRF.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(blindPrfHeader, blindPrf));
        stopWatch.stop();
        long prfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, prfTime, "Sender blinds");

        logPhaseInfo(PtoState.PTO_END);
        return new Ra17MpOprfSenderOutput(envType, alpha, batchSize);
    }

    private List<byte[]> handleBlindPayload(List<byte[]> blindPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(blindPayload.size() == batchSize);
        Stream<byte[]> blindStream = blindPayload.stream();
        blindStream = parallel ? blindStream.parallel() : blindStream;
        return blindStream
            // 解码H(m_c)^β
            .map(ecc::decode)
            // 计算H(m_c)^βα
            .map(element -> ecc.multiply(element, alpha))
            // 编码
            .map(element -> ecc.encode(element, compressEncode))
            .collect(Collectors.toList());
    }
}
