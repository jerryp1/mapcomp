package edu.alibaba.mpc4j.s2pc.pir.index.onionpir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.AbstractIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.onionpir.Mcr21IndexPirPtoDesc.PtoStep;

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
    /**
     * OnionPIR方案内部参数
     */
    private Mcr21IndexPirInnerParams innerParams;
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
     * 私钥密文
     */
    private ArrayList<byte[]> encryptedSecretKey = new ArrayList<>();

    public Mcr21IndexPirClient(Rpc clientRpc, Party serverParty, Mcr21IndexPirConfig config) {
        super(Mcr21IndexPirPtoDesc.getInstance(), clientRpc, serverParty, config);
    }

    @Override
    public void init(IndexPirParams indexPirParams, int serverElementSize, int elementByteLength) {
        assert (indexPirParams instanceof Mcr21IndexPirParams);
        params = (Mcr21IndexPirParams) indexPirParams;
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        innerParams = new Mcr21IndexPirInnerParams(params, serverElementSize, elementByteLength);
        setInitInput(serverElementSize, elementByteLength);
        // 客户端生成密钥对
        generateKeyPair();
        // 客户端加密私钥
        encryptedSecretKey = Mcr21IndexPirNativeUtils.encryptSecretKey(
            params.getEncryptionParams(), publicKey, secretKey
        );
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(int serverElementSize, int elementByteLength) {
        params = Mcr21IndexPirParams.DEFAULT_PARAMS;
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        innerParams = new Mcr21IndexPirInnerParams(params, serverElementSize, elementByteLength);
        setInitInput(serverElementSize, elementByteLength);
        // 客户端生成密钥对
        generateKeyPair();
        // 客户端加密私钥
        encryptedSecretKey = Mcr21IndexPirNativeUtils.encryptSecretKey(
            params.getEncryptionParams(), publicKey, secretKey
        );
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
        // 添加公钥
        clientQueryPayload.add(publicKey);
        // 添加Galois密钥
        clientQueryPayload.add(galoisKeys);
        // 添加私钥密文
        clientQueryPayload.addAll(encryptedSecretKey);
        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
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
        byte[] result = handleServerResponsePayload(serverResponsePayload);
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, responseTime, "Server handles reply");

        logPhaseInfo(PtoState.PTO_END);
        return result;
    }

    /**
     * 返回查询密文。
     *
     * @return 查询密文。
     */
    public ArrayList<byte[]> generateQuery() {
        int binNum = innerParams.getBinNum();
        // 前n-1个分块
        int[] nvec = innerParams.getDimensionsLength()[0];
        int indexOfPlaintext = index / innerParams.getElementSizeOfPlaintext()[0];
        // 计算每个维度的坐标
        int[] indices = PirUtils.computeIndices(indexOfPlaintext, nvec);
        ArrayList<byte[]> result = new ArrayList<>(
            Mcr21IndexPirNativeUtils.generateQuery(params.getEncryptionParams(), publicKey, secretKey, indices, nvec)
        );
        if ((binNum > 1) && (innerParams.getPlaintextSize()[0] != innerParams.getPlaintextSize()[binNum - 1])) {
            // 最后一个分块
            int[] lastNvec = innerParams.getDimensionsLength()[binNum - 1];
            int lastIndexOfPlaintext = index / innerParams.getElementSizeOfPlaintext()[binNum - 1];
            // 计算每个维度的坐标
            int[] lastIndices = PirUtils.computeIndices(lastIndexOfPlaintext, lastNvec);
            // 返回查询密文
            result.addAll(
                Mcr21IndexPirNativeUtils.generateQuery(
                    params.getEncryptionParams(), publicKey, secretKey, lastIndices, lastNvec
                )
            );
        }
        return result;
    }

    /**
     * 解码回复得到检索结果。
     *
     * @param response 回复。
     * @return 检索结果。
     */
    private byte[] handleServerResponsePayload(List<byte[]> response) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(response.size() == innerParams.getBinNum());
        byte[] result = new byte[elementByteLength];
        int binNum = innerParams.getBinNum();
        IntStream intStream = this.parallel ? IntStream.range(0, binNum).parallel() : IntStream.range(0, binNum);
        intStream.forEach(i -> {
            int byteLength = i == binNum - 1 ? innerParams.getLastBinByteLength() : innerParams.getBinMaxByteLength();
            long[] coeffs = Mcr21IndexPirNativeUtils.decryptReply(
                params.getEncryptionParams(), secretKey, response.get(i)
            );
            byte[] bytes = PirUtils.convertCoeffsToBytes(coeffs, params.getPlainModulusBitLength());
            int offset = this.index % innerParams.getElementSizeOfPlaintext()[i];
            System.arraycopy(bytes, offset * byteLength, result, i * innerParams.getBinMaxByteLength(), byteLength);
        });
        return result;
    }

    /**
     * 客户端生成密钥对。
     */
    private void generateKeyPair() {
        ArrayList<byte[]> keyPair = Mcr21IndexPirNativeUtils.keyGen(params.getEncryptionParams());
        assert (keyPair.size() == 3);
        this.publicKey = keyPair.remove(0);
        this.secretKey = keyPair.remove(0);
        this.galoisKeys = keyPair.remove(0);
    }
}