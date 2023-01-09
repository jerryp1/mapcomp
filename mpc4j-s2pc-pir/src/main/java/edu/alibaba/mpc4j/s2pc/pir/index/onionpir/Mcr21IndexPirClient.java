package edu.alibaba.mpc4j.s2pc.pir.index.onionpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.index.AbstractIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.AbstractIndexPirParams;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * OnionPIR协议客户端。
 *
 * @author Liqiang Peng
 * @date 2022/11/11
 */
public class Mcr21IndexPirClient extends AbstractIndexPirClient {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * OnionPIR方案参数
     */
    private Mcr21IndexPirParams params;

    public Mcr21IndexPirClient(Rpc clientRpc, Party serverParty, Mcr21IndexPirConfig config) {
        super(Mcr21IndexPirPtoDesc.getInstance(), clientRpc, serverParty, config);
    }

    @Override
    public void init(AbstractIndexPirParams indexPirParams, int serverElementSize, int elementByteLength) throws MpcAbortException {
        setInitInput(serverElementSize, elementByteLength);
        info("{}{} Client Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        assert (indexPirParams instanceof Mcr21IndexPirParams);
        params = (Mcr21IndexPirParams) indexPirParams;

        initialized = true;
        info("{}{} Client Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public byte[] pir(int index) throws MpcAbortException {
        setPtoInput(index);
        info("{}{} Client begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 客户端生成BFV算法公私钥对
        List<byte[]> keyPair = Mcr21IndexPirNativeUtils.keyGen(params.getEncryptionParams());
        List<byte[]> encryptedSecretKey = Mcr21IndexPirNativeUtils.encryptSecretKey(params.getEncryptionParams(), keyPair.get(0), keyPair.get(1));
        // 客户端生成并发送问询
        List<byte[]> clientQueryPayload = generateQuery(params.getEncryptionParams(), keyPair.get(0), keyPair.get(1));
        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Mcr21IndexPirPtoDesc.PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientQueryHeader, clientQueryPayload));
        DataPacketHeader clientEncSecretKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Mcr21IndexPirPtoDesc.PtoStep.CLIENT_SEND_ENC_SK.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientEncSecretKeyHeader, encryptedSecretKey));
        DataPacketHeader clientKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Mcr21IndexPirPtoDesc.PtoStep.CLIENT_SEND_GALOIS_KEY_PUBLIC_KEY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        ArrayList<byte[]> clientKey = new ArrayList<>();
        clientKey.add(keyPair.get(0));
        clientKey.add(keyPair.get(2));
        rpc.send(DataPacket.fromByteArrayList(clientKeyHeader, clientKey));

        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), genQueryTime);

        stopWatch.start();
        // 客户端接收并解密回复
        DataPacketHeader serverResponseHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Mcr21IndexPirPtoDesc.PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> serverResponsePayload = rpc.receive(serverResponseHeader).getPayload();
        MpcAbortPreconditions.checkArgument(serverResponsePayload.size() == 1);
        byte[] element = handleServerResponsePayload(keyPair.get(1), serverResponsePayload.get(0));
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), responseTime);

        info("{}{} Client end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return element;
    }

    /**
     * 返回查询密文。
     *
     * @param encryptionParams 加密方案参数。
     * @param publicKey        公钥。
     * @param secretKey        私钥。
     * @return 查询密文。
     */
    public ArrayList<byte[]> generateQuery(byte[] encryptionParams, byte[] publicKey, byte[] secretKey) throws MpcAbortException {
        int[] nvec = params.getDimensionsLength();
        int degree = params.getPolyModulusDegree();
        IntStream.range(0, nvec.length).forEach(i -> {
            assert nvec[i] <= degree;
        });
        // index of FV plaintext
        int indexOfPlaintext = index / params.getElementSizeOfPlaintext();
        // 计算每个维度的坐标
        int[] indices = computeIndices(indexOfPlaintext, nvec);
        ArrayList<Integer> plainQuery = new ArrayList<>();
        // handling first dimension first
        int firstDimensionSize = nvec[0];
        int logSize = (int) Math.ceil(Math.log(firstDimensionSize) / Math.log(2));
        int gap = (int) Math.ceil(degree * 1.0 / (1 << logSize));
        info("Client: index 1 / {} = {}", indices.length, indices[0]);
        plainQuery.add(indices[0] * gap);
        // compressing all the remaining dimensions into one dimension of size equal to sum of remaining dimensions
        int remainingDimensionSize = 0;
        ArrayList<Integer> newIndices = new ArrayList<>();
        if (indices.length > 1) {
            for (int i = 1; i < indices.length; i++) {
                newIndices.add(indices[i] + remainingDimensionSize);
                remainingDimensionSize += nvec[i];
            }
            logSize = (int) Math.ceil(Math.log(remainingDimensionSize * params.getGswDecompSize()) / Math.log(2));
            gap = (int) Math.ceil(degree * 1.0 / (1 << logSize));
            for (int i = 0; i < newIndices.size(); i++) {
                info("Client: index {} / {} = {}", i + 2, indices.length, indices[i + 1]);
                plainQuery.add(newIndices.get(i) * gap);
            }
        }
        // 返回查询密文
        return Mcr21IndexPirNativeUtils.generateQuery(
            encryptionParams, publicKey, secretKey, plainQuery.stream().mapToInt(integer -> integer).toArray(),
            firstDimensionSize, remainingDimensionSize
        );
    }

    /**
     * 解码回复得到检索结果。
     *
     * @param secretKey 私钥。
     * @param response  回复。
     * @return 检索结果。
     * @throws MpcAbortException 如果协议异常中止。
     */
    private byte[] handleServerResponsePayload(byte[] secretKey, byte[] response) throws MpcAbortException {
        long[] decodedCoefficientArray = Mcr21IndexPirNativeUtils.decryptReply(
            params.getEncryptionParams(), secretKey, response
        );
        MpcAbortPreconditions.checkArgument(decodedCoefficientArray.length == params.getPolyModulusDegree());
        byte[] decodeByteArray = convertCoeffsToBytes(decodedCoefficientArray, params.getPlainModulusBitLength());
        byte[] elementBytes = new byte[elementByteLength];
        // offset in FV plaintext
        int offset = index % params.getElementSizeOfPlaintext();
        System.arraycopy(decodeByteArray, offset * elementByteLength, elementBytes, 0, elementByteLength);
        return elementBytes;
    }
}