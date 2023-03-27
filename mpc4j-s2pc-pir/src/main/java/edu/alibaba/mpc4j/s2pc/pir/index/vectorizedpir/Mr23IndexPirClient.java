package edu.alibaba.mpc4j.s2pc.pir.index.vectorizedpir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.AbstractIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.vectorizedpir.Mr23IndexPirPtoDesc.PtoStep;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Vectorized PIR协议客户端。
 *
 * @author Liqiang Peng
 * @date 2023/3/6
 */
public class Mr23IndexPirClient extends AbstractIndexPirClient {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * Vectorized PIR方案参数
     */
    private Mr23IndexPirParams params;
    /**
     * Vectorized PIR方案内部参数
     */
    private Mr23IndexPirInnerParams innerParams;
    /**
     * 公钥
     */
    private byte[] publicKey;
    /**
     * 私钥
     */
    private byte[] secretKey;
    /**
     * Galois密钥
     */
    private byte[] galoisKeys;
    /**
     * Relin Keys密钥
     */
    private byte[] relinKeys;
    /**
     * 移位
     */
    private int offset;

    public Mr23IndexPirClient(Rpc clientRpc, Party serverParty, Mr23IndexPirConfig config) {
        super(Mr23IndexPirPtoDesc.getInstance(), clientRpc, serverParty, config);
    }

    @Override
    public void init(IndexPirParams indexPirParams, int serverElementSize, int elementByteLength) {
        assert (indexPirParams instanceof Mr23IndexPirParams);
        params = (Mr23IndexPirParams) indexPirParams;
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        innerParams = new Mr23IndexPirInnerParams(params, serverElementSize, elementByteLength);
        setInitInput(serverElementSize, elementByteLength);
        // 客户端生成密钥对
        generateKeyPair();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(int serverElementSize, int elementByteLength) {
        params = Mr23IndexPirParams.DEFAULT_PARAMS;
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        innerParams = new Mr23IndexPirInnerParams(params, serverElementSize, elementByteLength);
        setInitInput(serverElementSize, elementByteLength);
        // 客户端生成密钥对
        generateKeyPair();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[] pir(int index) throws MpcAbortException {
        setPtoInput(index);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // 客户端生成并发送问询
        List<byte[]> clientQueryPayload = generateQuery();
        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        clientQueryPayload.add(publicKey);
        clientQueryPayload.add(relinKeys);
        clientQueryPayload.add(galoisKeys);
        rpc.send(DataPacket.fromByteArrayList(clientQueryHeader, clientQueryPayload));
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, genQueryTime, "Client generates query");

        // 客户端接收并解密回复
        DataPacketHeader serverResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> serverResponsePayload = rpc.receive(serverResponseHeader).getPayload();

        stopWatch.start();
        byte[] element = handleServerResponsePayload(serverResponsePayload);
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, responseTime, "Client handles reply");

        logPhaseInfo(PtoState.PTO_END);
        return element;
    }

    /**
     * 返回查询密文。
     *
     * @return 查询密文。
     */
    public ArrayList<byte[]> generateQuery() {
        int[] temp = PirUtils.computeIndices(index, innerParams.getDimensionsSize());
        int[] permutedIndices = IntStream.range(0, params.getDimension())
            .map(i -> temp[params.getDimension() - 1 - i])
            .toArray();
        int[] indices = new int[params.getDimension()];
        for (int i = 0; i < params.getDimension(); i++) {
            indices[i] = permutedIndices[i];
            for (int j = 0; j < i; j++) {
                indices[i] = (indices[i] + permutedIndices[j]) % params.getFirstTwoDimensionSize();
            }
        }
        this.offset = indices[params.getDimension() - 1];
        return Mr23IndexPirNativeUtils.generateQuery(
            params.getEncryptionParams(), publicKey, secretKey, indices, params.getFirstTwoDimensionSize()
        );
    }

    /**
     * 解码回复得到检索结果。
     *
     * @param response 回复。
     * @return 检索结果。
     * @throws MpcAbortException 如果协议异常中止。
     */
    private byte[] handleServerResponsePayload(List<byte[]> response) throws MpcAbortException {
        byte[] elementBytes = new byte[elementByteLength];
        int binNum = innerParams.getBinNum();
        MpcAbortPreconditions.checkArgument(response.size() == binNum);
        IntStream intStream = this.parallel ? IntStream.range(0, binNum).parallel() : IntStream.range(0, binNum);
        intStream.forEach(binIndex -> {
            long coeffs = Mr23IndexPirNativeUtils.decryptReply(
                params.getEncryptionParams(),
                secretKey,
                response.get(binIndex),
                offset,
                params.getFirstTwoDimensionSize()
            );
            byte[] bytes = PirUtils.convertCoeffsToBytes(new long[]{coeffs}, params.getPlainModulusBitLength());
            int byteLength = binIndex == binNum - 1 ? innerParams.getLastBinByteLength() : innerParams.getBinMaxByteLength();
            System.arraycopy(bytes, 0, elementBytes, binIndex * innerParams.getBinMaxByteLength(), byteLength);
        });
        return elementBytes;
    }

    /**
     * 客户端生成密钥对。
     */
    private void generateKeyPair() {
        List<byte[]> keyPair = Mr23IndexPirNativeUtils.keyGen(
            params.getEncryptionParams(), params.getFirstTwoDimensionSize()
        );
        assert (keyPair.size() == 4);
        this.publicKey = keyPair.remove(0);
        this.secretKey = keyPair.remove(0);
        this.relinKeys = keyPair.remove(0);
        this.galoisKeys = keyPair.remove(0);
    }
}
