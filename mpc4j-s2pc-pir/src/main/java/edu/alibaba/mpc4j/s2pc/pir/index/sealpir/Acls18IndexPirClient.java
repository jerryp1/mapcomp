package edu.alibaba.mpc4j.s2pc.pir.index.sealpir;

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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * SEAL PIR协议客户端。
 *
 * @author Liqiang Peng
 * @date 2023/1/17
 */
public class Acls18IndexPirClient extends AbstractIndexPirClient {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * SEAL PIR方案参数
     */
    private Acls18IndexPirParams params;
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

    public Acls18IndexPirClient(Rpc clientRpc, Party serverParty, Acls18IndexPirConfig config) {
        super(Acls18IndexPirPtoDesc.getInstance(), clientRpc, serverParty, config);
    }

    @Override
    public void init(AbstractIndexPirParams indexPirParams, int serverElementSize, int elementByteLength) {
        setInitInput(serverElementSize, elementByteLength);
        info("{}{} Client Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        assert (indexPirParams instanceof Acls18IndexPirParams);
        params = (Acls18IndexPirParams) indexPirParams;

        stopWatch.start();
        // 客户端生成密钥对
        clientGenerateKeyPair(params.getEncryptionParams());
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Client Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public byte[] pir(int index) throws MpcAbortException {
        setPtoInput(index);
        info("{}{} Client begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 客户端生成并发送问询
        List<byte[]> clientQueryPayload = generateQuery(params.getEncryptionParams(), publicKey, secretKey);
        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Acls18IndexPirPtoDesc.PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        // 添加Galois密钥
        clientQueryPayload.add(galoisKeys);
        rpc.send(DataPacket.fromByteArrayList(clientQueryHeader, clientQueryPayload));
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), genQueryTime);

        stopWatch.start();
        // 客户端接收并解密回复
        DataPacketHeader serverResponseHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Acls18IndexPirPtoDesc.PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> serverResponsePayload = rpc.receive(serverResponseHeader).getPayload();
        byte[] element = handleServerResponsePayload(secretKey, serverResponsePayload);
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
            Acls18IndexPirNativeUtils.generateQuery(encryptionParams, publicKey, secretKey, indices, nvec)
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
                Acls18IndexPirNativeUtils.generateQuery(encryptionParams, publicKey, secretKey, lastIndices, lastNvec)
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
     * @throws MpcAbortException 如果协议异常中止。
     */
    private byte[] handleServerResponsePayload(byte[] secretKey, List<byte[]> response) throws MpcAbortException {
        byte[] elementBytes = new byte[elementByteLength];
        int expansionRatio = params.getExpansionRatio();
        int bundleNum = params.getBundleNum();
        int dimension = params.getDimension();
        int bundleResponseSize = IntStream.range(0, dimension - 1).map(i -> expansionRatio).reduce(1, (a, b) -> a * b);
        MpcAbortPreconditions.checkArgument(response.size() == bundleResponseSize * bundleNum);
        int maxElementByteLength = params.getPolyModulusDegree() * params.getPlainModulusBitLength() / Byte.SIZE;
        IntStream intStream = this.parallel ? IntStream.range(0, bundleNum).parallel() : IntStream.range(0, bundleNum);
        intStream.forEach(bundleIndex -> {
            long[] coeffs = Acls18IndexPirNativeUtils.decryptReply(
                params.getEncryptionParams(),
                secretKey,
                Lists.newArrayList(
                    response.subList(bundleIndex * bundleResponseSize, (bundleIndex + 1) * bundleResponseSize)
                ),
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

    /**
     * 客户端生成密钥对。
     *
     * @param sealContext SEAL上下文参数。
     */
    private void clientGenerateKeyPair(byte[] sealContext) {
        List<byte[]> keyPair = Acls18IndexPirNativeUtils.keyGen(sealContext);
        assert (keyPair.size() == 3);
        this.publicKey = keyPair.remove(0);
        this.secretKey = keyPair.remove(0);
        this.galoisKeys = keyPair.remove(0);
    }
}
