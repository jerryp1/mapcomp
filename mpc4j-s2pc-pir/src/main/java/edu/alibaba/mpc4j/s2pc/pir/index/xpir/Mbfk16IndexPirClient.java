package edu.alibaba.mpc4j.s2pc.pir.index.xpir;

import com.google.common.collect.Lists;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.index.AbstractIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.AbstractIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.xpir.Mbfk16IndexPirPtoDesc.PtoStep;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * XPIR协议客户端。
 *
 * @author Liqiang Peng
 * @date 2022/8/24
 */
public class Mbfk16IndexPirClient extends AbstractIndexPirClient {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * XPIR方案参数
     */
    private Mbfk16IndexPirParams params;

    public Mbfk16IndexPirClient(Rpc clientRpc, Party serverParty, Mbfk16IndexPirConfig config) {
        super(Mbfk16IndexPirPtoDesc.getInstance(), clientRpc, serverParty, config);
    }

    @Override
    public void init(AbstractIndexPirParams indexPirParams, int serverElementSize, int elementByteLength) {
        setInitInput(serverElementSize, elementByteLength);
        info("{}{} Client Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        assert (indexPirParams instanceof Mbfk16IndexPirParams);
        params = (Mbfk16IndexPirParams) indexPirParams;

        initialized = true;
        info("{}{} Client Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public byte[] pir(int index) throws MpcAbortException {
        setPtoInput(index);
        info("{}{} Client begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 客户端生成BFV算法公私钥对
        List<byte[]> keyPair = Mbfk16IndexPirNativeUtils.keyGen(params.getEncryptionParams());
        // 客户端生成并发送问询
        List<byte[]> clientQueryPayload = generateQuery(params.getEncryptionParams(), keyPair.get(0), keyPair.get(1));
        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientQueryHeader, clientQueryPayload));
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), genQueryTime);

        stopWatch.start();
        // 客户端接收并解密回复
        DataPacketHeader serverResponseHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> serverResponsePayload = rpc.receive(serverResponseHeader).getPayload();
        MpcAbortPreconditions.checkArgument(serverResponsePayload.size() % params.getBundleNum() == 0);
        byte[] element = handleServerResponsePayload(keyPair.get(1), serverResponsePayload);
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
    public ArrayList<byte[]> generateQuery(byte[] encryptionParams, byte[] publicKey, byte[] secretKey) {
        int bundleNum = params.getBundleNum();
        // 前n-1个分块
        int[] nvec = params.getDimensionsLength()[0];
        int indexOfPlaintext = index / params.getElementSizeOfPlaintext()[0];
        // 计算每个维度的坐标
        int[] indices = computeIndices(indexOfPlaintext, nvec);
        IntStream.range(0, indices.length)
            .forEach(i -> info("Client: index {} / {} = {} / {}", i + 1, indices.length, indices[i], nvec[i]));
        ArrayList<byte[]> result = new ArrayList<>(
            Mbfk16IndexPirNativeUtils.generateQuery(encryptionParams, publicKey, secretKey, indices, nvec)
        );
        if ((bundleNum > 1) && (params.getPlaintextSize()[0] != params.getPlaintextSize()[bundleNum - 1])) {
            // 最后一个分块
            int[] lastNvec = params.getDimensionsLength()[bundleNum - 1];
            int lastIndexOfPlaintext = index / params.getElementSizeOfPlaintext()[bundleNum - 1];
            // 计算每个维度的坐标
            int[] lastIndices = computeIndices(lastIndexOfPlaintext, lastNvec);
            IntStream.range(0, lastIndices.length).forEach(i -> info("Client: last bundle index {} / {} = {} / {}",
                i + 1, lastIndices.length, lastIndices[i], lastNvec[i]));
            // 返回查询密文
            result.addAll(
                Mbfk16IndexPirNativeUtils.generateQuery(encryptionParams, publicKey, secretKey, lastIndices, lastNvec)
            );
        }
        return result;
    }

    /**
     * 解码回复得到检索结果。
     *
     * @param secretKey 私钥。
     * @param response  回复。
     * @return 检索结果。
     */
    private byte[] handleServerResponsePayload(byte[] secretKey, List<byte[]> response) {
        byte[] elementBytes = new byte[elementByteLength];
        int expansion = response.size() / params.getBundleNum();
        int maxElementByteLength = params.getPolyModulusDegree() * params.getPlainModulusBitLength() / Byte.SIZE;
        int bundleNum = params.getBundleNum();
        IntStream intStream = this.parallel ? IntStream.range(0, bundleNum).parallel() : IntStream.range(0, bundleNum);
        intStream.forEach(bundleIndex -> {
            long[] coeffs = Mbfk16IndexPirNativeUtils.decryptReply(
                params.getEncryptionParams(),
                secretKey,
                Lists.newArrayList(response.subList(bundleIndex * expansion, (bundleIndex + 1) * expansion)),
                params.getDimension()
            );
            assert (coeffs.length == params.getPolyModulusDegree()) : "多项式阶不匹配";
            byte[] bytes = convertCoeffsToBytes(coeffs, params.getPlainModulusBitLength());
            int offset = this.index % params.getElementSizeOfPlaintext()[bundleIndex];
            int size = bundleIndex == params.getBundleNum() - 1 ?
                elementByteLength % maxElementByteLength : maxElementByteLength;
            System.arraycopy(bytes, offset * size, elementBytes, bundleIndex * maxElementByteLength, size);
        });
        return elementBytes;
    }
}
