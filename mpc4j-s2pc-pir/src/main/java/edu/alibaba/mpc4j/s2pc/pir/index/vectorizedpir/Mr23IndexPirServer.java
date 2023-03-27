package edu.alibaba.mpc4j.s2pc.pir.index.vectorizedpir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.AbstractIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.vectorizedpir.Mr23IndexPirPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toCollection;

/**
 * Vectorized PIR协议服务端。
 *
 * @author Liqiang Peng
 * @date 2023/3/6
 */
public class Mr23IndexPirServer extends AbstractIndexPirServer {

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
     * BFV明文（点值表示）
     */
    private List<ArrayList<byte[]>> encodedDatabase = new ArrayList<>();

    public Mr23IndexPirServer(Rpc serverRpc, Party clientParty, Mr23IndexPirConfig config) {
        super(Mr23IndexPirPtoDesc.getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(IndexPirParams indexPirParams, ArrayList<ByteBuffer> elementArrayList, int elementByteLength) {
        assert (indexPirParams instanceof Mr23IndexPirParams);
        params = (Mr23IndexPirParams) indexPirParams;
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        innerParams = new Mr23IndexPirInnerParams(params, elementArrayList.size(), elementByteLength);
        setInitInput(elementArrayList, elementByteLength, innerParams.getBinMaxByteLength());
        // 服务端对数据库进行编码
        int binNum = innerParams.getBinNum();
        int totalSize = params.getFirstTwoDimensionSize() * params.getThirdDimensionSize();
        IntStream intStream = this.parallel ? IntStream.range(0, binNum).parallel() : IntStream.range(0, binNum);
        encodedDatabase = intStream.mapToObj(i -> preprocessDatabase(i, totalSize)).collect(Collectors.toList());
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(ArrayList<ByteBuffer> elementArrayList, int elementByteLength) {
        params = Mr23IndexPirParams.DEFAULT_PARAMS;
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        innerParams = new Mr23IndexPirInnerParams(params, elementArrayList.size(), elementByteLength);
        setInitInput(elementArrayList, elementByteLength, innerParams.getBinMaxByteLength());
        // 服务端对数据库进行编码
        int binNum = innerParams.getBinNum();
        int totalSize = params.getFirstTwoDimensionSize() * params.getThirdDimensionSize();
        IntStream intStream = this.parallel ? IntStream.range(0, binNum).parallel() : IntStream.range(0, binNum);
        encodedDatabase = intStream.mapToObj(i -> preprocessDatabase(i, totalSize)).collect(Collectors.toList());
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void pir() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        ArrayList<byte[]> clientQueryPayload = new ArrayList<>(rpc.receive(clientQueryHeader).getPayload());

        // 服务端接收并处理问询
        stopWatch.start();
        ArrayList<byte[]> serverResponsePayload = handleClientQueryPayload(clientQueryPayload);
        DataPacketHeader serverResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverResponseHeader, serverResponsePayload));
        stopWatch.stop();
        long genResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, genResponseTime, "Server generates reply");

        logPhaseInfo(PtoState.PTO_END);
    }

    /**
     * 服务端处理客户端查询信息。
     *
     * @param clientQueryPayload 客户端查询信息。
     * @return 检索结果密文。
     * @throws MpcAbortException 如果协议异常中止。
     */
    private ArrayList<byte[]> handleClientQueryPayload(ArrayList<byte[]> clientQueryPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(clientQueryPayload.size() == params.getDimension() + 3);
        int binNum = innerParams.getBinNum();
        ArrayList<byte[]> clientQuery = IntStream.range(0, params.getDimension())
            .mapToObj(clientQueryPayload::get)
            .collect(toCollection(ArrayList::new));
        byte[] publicKey = clientQueryPayload.get(params.getDimension());
        byte[] relinKeys = clientQueryPayload.get(params.getDimension() + 1);
        byte[] galoisKeys = clientQueryPayload.get(params.getDimension() + 2);
        IntStream intStream = this.parallel ? IntStream.range(0, binNum).parallel() : IntStream.range(0, binNum);
        return intStream
            .mapToObj(i -> Mr23IndexPirNativeUtils.generateReply(
                params.getEncryptionParams(), clientQuery, encodedDatabase.get(i), publicKey, relinKeys, galoisKeys,
                params.getFirstTwoDimensionSize(), params.getThirdDimensionSize()
            )).collect(toCollection(ArrayList::new));
    }

    /**
     * 返回数据库编码后的多项式。
     *
     * @param binIndex  分块索引。
     * @param totalSize 多项式数量。
     * @return 数据库编码后的多项式。
     */
    private ArrayList<byte[]> preprocessDatabase(int binIndex, int totalSize) {
        long[] coeffs = new long[num];
        int byteLength = elementByteArray.get(binIndex)[0].length;
        IntStream.range(0, num).forEach(i -> {
            long[] temp = PirUtils.convertBytesToCoeffs(
                params.getPlainModulusBitLength(), 0, byteLength, elementByteArray.get(binIndex)[i]
            );
            assert temp.length == 1;
            coeffs[i] = temp[0];
        });
        return Mr23IndexPirNativeUtils.preprocessDatabase(
            params.getEncryptionParams(), coeffs, innerParams.getDimensionsSize(), totalSize
        );
    }
}