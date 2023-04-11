package edu.alibaba.mpc4j.s2pc.opf.sqoprf.ecdh;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfSenderOutput;


import edu.alibaba.mpc4j.s2pc.opf.oprf.ra17.Ra17MpOprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.AbstractSqOprfSender;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfSenderKey;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfSenderOutput;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Qixian Zhou
 * @date 2023/4/11
 */
public class EcdhEccSqOprfSender extends AbstractSqOprfSender {

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
    private EcdhSqOprfSenderKey key;


    public EcdhEccSqOprfSender(Rpc senderRpc, Party receiverParty, EcdhEccSqOprfConfig config) {
        super(EcdhEccSqOprfPtoDesc.getInstance(), senderRpc, receiverParty, config);
        ecc = EccFactory.createInstance(envType);
        compressEncode = config.getCompressEncode();
        key = null;
    }


    @Override
    public void init(int maxBatchSize) throws MpcAbortException {
        setInitInput(maxBatchSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // 生成密钥
        key = new EcdhSqOprfSenderKey(ecc.randomZn(secureRandom));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(int maxBatchSize, SqOprfSenderKey key) throws MpcAbortException {
        setInitInput(maxBatchSize);
        logPhaseInfo(PtoState.INIT_BEGIN);
        // 判断传入的 key 的具体类型是否符合要求
        assert (key instanceof EcdhSqOprfSenderKey);
        stopWatch.start();
        // 设置密钥
        this.key = (EcdhSqOprfSenderKey) key;
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SqOprfSenderOutput oprf(int batchSize) throws MpcAbortException {

        setPtoInput(batchSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        DataPacketHeader blindHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), EcdhEccSqOprfPtoDesc.PtoStep.RECEIVER_SEND_BLIND.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> blindPayload = rpc.receive(blindHeader).getPayload();

        stopWatch.start();
        List<byte[]> blindPrf = handleBlindPayload(blindPayload);
        DataPacketHeader blindPrfHeader = new DataPacketHeader(
                encodeTaskId, ptoDesc.getPtoId(), EcdhEccSqOprfPtoDesc.PtoStep.SENDER_SEND_BLIND_PRF.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(blindPrfHeader, blindPrf));
        stopWatch.stop();
        long prfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, prfTime, "Sender blinds");

        logPhaseInfo(PtoState.PTO_END);
        return new EcdhSqOprfSenderOutput(envType, key, batchSize);

    }
    private List<byte[]> handleBlindPayload(List<byte[]> blindPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(blindPayload.size() == batchSize);
        Stream<byte[]> blindStream = blindPayload.stream();
        blindStream = parallel ? blindStream.parallel() : blindStream;
        return blindStream
                // 解码H(m_c)^β
                .map(ecc::decode)
                // 计算H(m_c)^βα
                .map(element -> ecc.multiply(element, key.getAlpha()))
                // 编码
                .map(element -> ecc.encode(element, compressEncode))
                .collect(Collectors.toList());
    }
}
