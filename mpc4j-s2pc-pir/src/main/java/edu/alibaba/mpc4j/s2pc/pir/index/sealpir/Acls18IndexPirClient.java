package edu.alibaba.mpc4j.s2pc.pir.index.sealpir;

import com.google.common.collect.Lists;
import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.AbstractIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.sealpir.Acls18IndexPirPtoDesc.PtoStep;

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
     * SEAL PIR方案内部参数
     */
    private Acls18IndexPirInnerParams innerParams;
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
    public void init(IndexPirParams indexPirParams, int serverElementSize, int elementByteLength) {
        assert (indexPirParams instanceof Acls18IndexPirParams);
        params = (Acls18IndexPirParams) indexPirParams;
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        innerParams = new Acls18IndexPirInnerParams(params, serverElementSize, elementByteLength);
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
        params = Acls18IndexPirParams.DEFAULT_PARAMS;
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        innerParams = new Acls18IndexPirInnerParams(params, serverElementSize, elementByteLength);
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
        // 添加Galois密钥
        clientQueryPayload.add(galoisKeys);
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
        int binNum = innerParams.getBinNum();
        // 前n-1个分块
        int[] nvec = innerParams.getDimensionsLength()[0];
        int indexOfPlaintext = index / innerParams.getElementSizeOfPlaintext()[0];
        // 计算每个维度的坐标
        int[] indices = PirUtils.computeIndices(indexOfPlaintext, nvec);
        ArrayList<byte[]> result = new ArrayList<>(
            Acls18IndexPirNativeUtils.generateQuery(params.getEncryptionParams(), publicKey, secretKey, indices, nvec)
        );
        if ((binNum > 1) && (innerParams.getPlaintextSize()[0] != innerParams.getPlaintextSize()[binNum - 1])) {
            // 最后一个分块
            int[] lastNvec = innerParams.getDimensionsLength()[binNum - 1];
            int lastIndexOfPlaintext = index / innerParams.getElementSizeOfPlaintext()[binNum - 1];
            // 计算每个维度的坐标
            int[] lastIndices = PirUtils.computeIndices(lastIndexOfPlaintext, lastNvec);
            // 返回查询密文
            result.addAll(
                Acls18IndexPirNativeUtils.generateQuery(
                    params.getEncryptionParams(), publicKey, secretKey, lastIndices, lastNvec
                )
            );
        }
        return result;
    }

    /**
     * 解码回复得到检索结果。
     *
     * @param response  回复。
     * @return 检索结果。
     * @throws MpcAbortException 如果协议异常中止。
     */
    private byte[] handleServerResponsePayload(List<byte[]> response) throws MpcAbortException {
        byte[] element = new byte[elementByteLength];
        int expansionRatio = params.getExpansionRatio();
        int binNum = innerParams.getBinNum();
        int dimension = params.getDimension();
        int binResponseSize = IntStream.range(0, dimension - 1).map(i -> expansionRatio).reduce(1, (a, b) -> a * b);
        MpcAbortPreconditions.checkArgument(response.size() == binResponseSize * binNum);
        IntStream intStream = this.parallel ? IntStream.range(0, binNum).parallel() : IntStream.range(0, binNum);
        intStream.forEach(i -> {
            int byteLength = i == binNum - 1 ? innerParams.getLastBinByteLength() : innerParams.getBinMaxByteLength();
            long[] coeffs = Acls18IndexPirNativeUtils.decryptReply(
                params.getEncryptionParams(),
                secretKey,
                Lists.newArrayList(response.subList(i * binResponseSize, (i + 1) * binResponseSize)),
                params.getDimension()
            );
            byte[] bytes = PirUtils.convertCoeffsToBytes(coeffs, params.getPlainModulusBitLength());
            int offset = this.index % innerParams.getElementSizeOfPlaintext()[i];
            System.arraycopy(bytes, offset * byteLength, element, i * innerParams.getBinMaxByteLength(), byteLength);
        });
        return element;
    }

    /**
     * 客户端生成密钥对。
     */
    private void generateKeyPair() {
        List<byte[]> keyPair = Acls18IndexPirNativeUtils.keyGen(params.getEncryptionParams());
        assert (keyPair.size() == 3);
        this.publicKey = keyPair.remove(0);
        this.secretKey = keyPair.remove(0);
        this.galoisKeys = keyPair.remove(0);
    }
}
