package edu.alibaba.mpc4j.s2pc.pso.mqrpmt.czz22;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteMulEcc;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.filter.Filter;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory;
import edu.alibaba.mpc4j.s2pc.pso.mqrpmt.AbstractMqRpmtClient;
import edu.alibaba.mpc4j.s2pc.pso.mqrpmt.czz22.Czz22ByteEccCwMqRpmtPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * ZZL22-字节椭圆曲线mqRPMT协议客户端。
 *
 * @author Weiran Liu
 * @date 2022/9/11
 */
public class Czz22ByteEccCwMqRpmtClient extends AbstractMqRpmtClient {
    /**
     * 字节椭圆曲线
     */
    private final ByteMulEcc byteMulEcc;
    /**
     * 是否使用过滤器
     */
    private final boolean useFilter;
    /**
     * PEQT映射密钥
     */
    private byte[] peqtMapKey;
    /**
     * PEQT映射伪随机函数
     */
    private Prf peqtMapPrf;
    /**
     * 客户端密钥β
     */
    private byte[] beta;

    public Czz22ByteEccCwMqRpmtClient(Rpc clientRpc, Party serverParty, Czz22ByteEccCwMqRpmtConfig config) {
        super(Czz22ByteEccCwMqRpmtPtoDesc.getInstance(), clientRpc, serverParty, config);
        byteMulEcc = ByteEccFactory.createMulInstance(envType);
        useFilter = config.getUseFilter();
    }

    @Override
    public void init(int maxClientSetSize, int maxServerSetSize) throws MpcAbortException {
        setInitInput(maxClientSetSize, maxServerSetSize);
        info("{}{} Client Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        beta = byteMulEcc.randomScalar(secureRandom);
        DataPacketHeader peqtKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PEQT_KEY.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> peqtKeyPayload = rpc.receive(peqtKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(peqtKeyPayload.size() == 1);
        peqtMapKey = peqtKeyPayload.remove(0);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Client Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public boolean[] mqRpmt(Set<ByteBuffer> clientElementSet, int serverSetSize) throws MpcAbortException {
        setPtoInput(clientElementSet, serverSetSize);
        info("{}{} Client begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        int peqtByteLength = Czz22ByteEccCwMqRpmtPtoDesc.getPeqtByteLength(serverElementSize, clientElementSize);
        peqtMapPrf = PrfFactory.createInstance(envType, peqtByteLength);
        peqtMapPrf.setKey(peqtMapKey);
        // 客户端计算并发送H(y)^β
        List<byte[]> hyBetaPayload = generateHyBetaPayload();
        DataPacketHeader hyBetaHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_HY_BETA.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(hyBetaHeader, hyBetaPayload));
        stopWatch.stop();
        long hyBetaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), hyBetaTime);

        stopWatch.start();
        // 客户端接收H(x)^α
        DataPacketHeader hxAlphaHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_HX_ALPHA.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> hxAlphaPayload = rpc.receive(hxAlphaHeader).getPayload();
        // 客户端计算H(H(x)^αβ)
        ByteBuffer[] clientPeqtArray = handleHxAlphaPayload(hxAlphaPayload);
        // 客户端接收H(H(y)^βα)
        DataPacketHeader peqtHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_HY_BETA_ALPHA.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> peqtPayload = rpc.receive(peqtHeader).getPayload();
        boolean[] containVector = handlePeqtPayload(peqtPayload, clientPeqtArray);
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), peqtTime);

        info("{}{} Client end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return containVector;
    }

    private List<byte[]> generateHyBetaPayload() {
        Stream<ByteBuffer> clientElementStream = clientElementArrayList.stream();
        clientElementStream = parallel ? clientElementStream.parallel() : clientElementStream;
        return clientElementStream
            .map(clientElement -> byteMulEcc.hashToCurve(clientElement.array()))
            .map(p -> byteMulEcc.mul(p, beta))
            .collect(Collectors.toList());
    }

    private ByteBuffer[] handleHxAlphaPayload(List<byte[]> hxAlphaPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(hxAlphaPayload.size() == serverElementSize);
        Stream<byte[]> hxAlphaStream = hxAlphaPayload.stream();
        hxAlphaStream = parallel ? hxAlphaStream.parallel() : hxAlphaStream;
        return hxAlphaStream
            .map(p -> byteMulEcc.mul(p, beta))
            .map(p -> peqtMapPrf.getBytes(p))
            .map(ByteBuffer::wrap)
            .toArray(ByteBuffer[]::new);
    }

    private boolean[] handlePeqtPayload(List<byte[]> peqtPayload, ByteBuffer[] clientPeqtArray) throws MpcAbortException {
        if (useFilter) {
            try {
                Filter<byte[]> filter = FilterFactory.createFilter(envType, peqtPayload);
                boolean[] containVector = new boolean[serverElementSize];
                IntStream.range(0, serverElementSize).forEach(serverElementIndex -> {
                    if (filter.mightContain(clientPeqtArray[serverElementIndex].array())) {
                        containVector[serverElementIndex] = true;
                    }
                });
                return containVector;
            } catch (IllegalArgumentException e) {
                throw new MpcAbortException();
            }
        } else {
            MpcAbortPreconditions.checkArgument(peqtPayload.size() == clientElementSize);
            Set<ByteBuffer> set = peqtPayload.stream().map(ByteBuffer::wrap).collect(Collectors.toSet());
            boolean[] containVector = new boolean[serverElementSize];
            IntStream.range(0, serverElementSize).forEach(serverElementIndex -> {
                if (set.contains(clientPeqtArray[serverElementIndex])) {
                    containVector[serverElementIndex] = true;
                }
            });
            return containVector;
        }
    }
}
