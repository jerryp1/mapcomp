package edu.alibaba.mpc4j.s2pc.pir.index.onionpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.index.AbstractIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.AbstractIndexPirServer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * OnionPIR协议服务端。
 *
 * @author Liqiang Peng
 * @date 2022/11/14
 */
public class Mcr21IndexPirServer extends AbstractIndexPirServer {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * OnionPIR方案参数
     */
    private Mcr21IndexPirParams params;
    /**
     * Decomposed BFV明文
     */
    private ArrayList<int[]> bfvPlaintext;

    public Mcr21IndexPirServer(Rpc serverRpc, Party clientParty, Mcr21IndexPirConfig config) {
        super(Mcr21IndexPirPtoDesc.getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(AbstractIndexPirParams indexPirParams, ArrayList<ByteBuffer> elementArrayList, int elementByteLength)
        throws MpcAbortException {
        setInitInput(elementArrayList, elementByteLength);
        assert (indexPirParams instanceof Mcr21IndexPirParams);
        params = (Mcr21IndexPirParams) indexPirParams;
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
        bfvPlaintext = Mcr21IndexPirNativeUtils.preprocessDatabase(params.getEncryptionParams(), coeffArray);
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

        // 服务端接收问询
        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Mcr21IndexPirPtoDesc.PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        ArrayList<byte[]> clientQueryPayload = new ArrayList<>(rpc.receive(clientQueryHeader).getPayload());
        MpcAbortPreconditions.checkArgument(clientQueryPayload.size() == 3);
        // 服务端接收密钥密文
        DataPacketHeader clientEncSecretKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Mcr21IndexPirPtoDesc.PtoStep.CLIENT_SEND_ENC_SK.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        ArrayList<byte[]> clientEncSecretKeyPayload = new ArrayList<>(rpc.receive(clientEncSecretKeyHeader).getPayload());
        // 服务端接收Galois密钥，公钥
        DataPacketHeader clientKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Mcr21IndexPirPtoDesc.PtoStep.CLIENT_SEND_GALOIS_KEY_PUBLIC_KEY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        ArrayList<byte[]> clientKeyPayload = new ArrayList<>(rpc.receive(clientKeyHeader).getPayload());
        MpcAbortPreconditions.checkArgument(clientKeyPayload.size() == 2);

        // 服务端处理问询
        stopWatch.start();
        byte[] serverResponse = Mcr21IndexPirNativeUtils.generateReply(
            params.getEncryptionParams(), clientKeyPayload.get(0), clientKeyPayload.get(1), clientEncSecretKeyPayload,
            clientQueryPayload, bfvPlaintext, params.getDimensionsLength()
        );
        DataPacketHeader serverResponseHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Mcr21IndexPirPtoDesc.PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverResponseHeader, Collections.singletonList(serverResponse)));
        stopWatch.stop();
        long genResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), genResponseTime);

        info("{}{} Server end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }
}
