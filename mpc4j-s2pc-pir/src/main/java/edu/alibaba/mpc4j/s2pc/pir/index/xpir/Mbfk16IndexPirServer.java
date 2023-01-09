package edu.alibaba.mpc4j.s2pc.pir.index.xpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.index.AbstractIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.AbstractIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.xpir.Mbfk16IndexPirPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * XPIR协议服务端。
 *
 * @author Liqiang Peng
 * @date 2022/8/24
 */
public class Mbfk16IndexPirServer extends AbstractIndexPirServer {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * XPIR方案参数
     */
    private Mbfk16IndexPirParams params;
    /**
     * BFV明文（点值表示）
     */
    private ArrayList<byte[]> bfvPlaintext;

    public Mbfk16IndexPirServer(Rpc serverRpc, Party clientParty, Mbfk16IndexPirConfig config) {
        super(Mbfk16IndexPirPtoDesc.getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(AbstractIndexPirParams indexPirParams, ArrayList<ByteBuffer> elementArrayList, int elementByteLength)
        throws MpcAbortException {
        setInitInput(elementArrayList, elementByteLength);
        assert (indexPirParams instanceof Mbfk16IndexPirParams);
        params = (Mbfk16IndexPirParams) indexPirParams;
        info("{}{} Server Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 服务端对数据库进行编码
        ArrayList<long[]> coeffArray = encodeDatabase(
            params.getPolyModulusDegree(),
            params.getPlaintextSize(),
            params.getPlainModulusBitLength(),
            params.getDimensionsLength(),
            params.getElementSizeOfPlaintext()
        );
        // NTT转换
        bfvPlaintext = Mbfk16IndexPirNativeUtils.nttTransform(params.getEncryptionParams(), coeffArray);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Server Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public void pir() throws MpcAbortException {
        setPtoInput();
        info("{}{} Server begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 服务端接收并处理问询
        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        stopWatch.stop();
        long receiveQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), receiveQueryTime);

        stopWatch.start();
        ArrayList<byte[]> clientQueryPayload = new ArrayList<>(rpc.receive(clientQueryHeader).getPayload());
        int querySize = Arrays.stream(params.getDimensionsLength()).sum();
        MpcAbortPreconditions.checkArgument(clientQueryPayload.size() == querySize);
        ArrayList<byte[]> serverResponsePayload = Mbfk16IndexPirNativeUtils.generateReply(
            params.getEncryptionParams(), clientQueryPayload, bfvPlaintext, params.getDimensionsLength()
        );
        DataPacketHeader serverResponseHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverResponseHeader, serverResponsePayload));
        stopWatch.stop();
        long genResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), genResponseTime);

        info("{}{} Server end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }
}
