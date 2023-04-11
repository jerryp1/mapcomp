package edu.alibaba.mpc4j.s2pc.opf.sqoprf.ecdh;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfReceiverOutput;

import edu.alibaba.mpc4j.s2pc.opf.sqoprf.AbstractSqOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfReceiverOutput;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Qixian Zhou
 * @date 2023/4/11
 */
public class EcdhEccSqOprfReceiver extends AbstractSqOprfReceiver {
    /**
     * 椭圆曲线
     */
    private final Ecc ecc;
    /**
     * 是否压缩编码
     */
    private final boolean compressEncode;
    /**
     * 伪随机函数输出字节长度
     */
    private final int prfByteLength;
    /**
     * β^{-1}
     */
    private BigInteger[] inverseBetas;

    public EcdhEccSqOprfReceiver(Rpc receiverRpc, Party senderParty, EcdhEccSqOprfConfig config) {
        super(EcdhEccSqOprfPtoDesc.getInstance(), receiverRpc, senderParty, config);
        ecc = EccFactory.createInstance(envType);
        compressEncode = config.getCompressEncode();
        prfByteLength = ecc.encode(ecc.getG(), false).length;
    }



    @Override
    public void init(int maxBatchSize) throws MpcAbortException {
        setInitInput(maxBatchSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SqOprfReceiverOutput oprf(byte[][] inputs) throws MpcAbortException {
        setPtoInput(inputs);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        List<byte[]> blindPayload = generateBlindPayload();
        DataPacketHeader blindHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), EcdhEccSqOprfPtoDesc.PtoStep.RECEIVER_SEND_BLIND.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(blindHeader, blindPayload));
        stopWatch.stop();
        long blindTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, blindTime, "Receiver blinds");

        DataPacketHeader blindPrfHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), EcdhEccSqOprfPtoDesc.PtoStep.SENDER_SEND_BLIND_PRF.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> blindPrfPayload = rpc.receive(blindPrfHeader).getPayload();

        stopWatch.start();
        SqOprfReceiverOutput receiverOutput = handleBlindPrfPayload(blindPrfPayload);
        stopWatch.stop();
        long deBlindTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, deBlindTime, "Receiver de-blinds");

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;

    }
    private List<byte[]> generateBlindPayload() {
        BigInteger n = ecc.getN();
        inverseBetas = new BigInteger[batchSize];
        IntStream batchIntStream = IntStream.range(0, batchSize);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;

        return batchIntStream
                .mapToObj(index -> {
                    // 生成盲化因子
                    BigInteger beta = ecc.randomZn(secureRandom);
                    inverseBetas[index] = beta.modInverse(n);
                    // hash to point
                    ECPoint element = ecc.hashToCurve(inputs[index]);
                    // 盲化
                    return ecc.multiply(element, beta);
                })
                .map(element -> ecc.encode(element, compressEncode))
                .collect(Collectors.toList());
    }

    private SqOprfReceiverOutput handleBlindPrfPayload(List<byte[]> blindPrfPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(blindPrfPayload.size() == batchSize);
        byte[][] blindPrfArray = blindPrfPayload.toArray(new byte[0][]);
        IntStream batchIntStream = IntStream.range(0, batchSize);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        byte[][] prfs = batchIntStream
                .mapToObj(index -> {
                    // 解码
                    ECPoint element = ecc.decode(blindPrfArray[index]);
                    // 去盲化
                    return ecc.multiply(element, inverseBetas[index]);
                })
                .map(element -> ecc.encode(element, false))
                .toArray(byte[][]::new);
        return new SqOprfReceiverOutput(prfByteLength, inputs, prfs);
    }


}
