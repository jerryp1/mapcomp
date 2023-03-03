package edu.alibaba.mpc4j.s2pc.pir.index.xpir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.index.AbstractIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.xpir.Mbfk16IndexPirPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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
     * XPIR方案内部参数
     */
    private Mbfk16IndexPirInnerParams innerParams;
    /**
     * BFV明文（点值表示）
     */
    private List<ArrayList<byte[]>> encodedDatabase;

    public Mbfk16IndexPirServer(Rpc serverRpc, Party clientParty, Mbfk16IndexPirConfig config) {
        super(Mbfk16IndexPirPtoDesc.getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(IndexPirParams indexPirParams, ArrayList<ByteBuffer> elementArrayList, int elementByteLength) {
        assert (indexPirParams instanceof Mbfk16IndexPirParams);
        params = (Mbfk16IndexPirParams) indexPirParams;
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        innerParams = new Mbfk16IndexPirInnerParams(params, elementArrayList.size(), elementByteLength);
        setInitInput(elementArrayList, elementByteLength, innerParams.getBinMaxByteLength());
        // 服务端对数据库进行编码
        int binNum = innerParams.getBinNum();
        IntStream intStream = this.parallel ? IntStream.range(0, binNum).parallel() : IntStream.range(0, binNum);
        encodedDatabase = intStream.mapToObj(this::preprocessDatabase).collect(Collectors.toList());
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(ArrayList<ByteBuffer> elementArrayList, int elementByteLength) {
        params = Mbfk16IndexPirParams.DEFAULT_PARAMS;
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        innerParams = new Mbfk16IndexPirInnerParams(params, elementArrayList.size(), elementByteLength);
        setInitInput(elementArrayList, elementByteLength, innerParams.getBinMaxByteLength());
        // 服务端对数据库进行编码
        int binNum = innerParams.getBinNum();
        IntStream intStream = this.parallel ? IntStream.range(0, binNum).parallel() : IntStream.range(0, binNum);
        encodedDatabase = intStream.mapToObj(this::preprocessDatabase).collect(Collectors.toList());
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
        int totalSize = clientQueryPayload.size();
        int binNum = innerParams.getBinNum();
        ArrayList<ArrayList<byte[]>> clientQuery = new ArrayList<>();
        int expectSize1 = Arrays.stream(innerParams.getDimensionsLength()[0]).sum(), expectSize2 = 0;
        if ((binNum > 1) && (innerParams.getPlaintextSize()[0] != innerParams.getPlaintextSize()[binNum - 1])) {
            expectSize2 = Arrays.stream(innerParams.getDimensionsLength()[binNum - 1]).sum();
        }
        MpcAbortPreconditions.checkArgument(totalSize == expectSize1 + expectSize2);
        if ((binNum > 1) && (innerParams.getPlaintextSize()[0] != innerParams.getPlaintextSize()[binNum - 1])) {
            for (int i = 0; i < binNum - 1; i++) {
                clientQuery.add(new ArrayList<>());
                for (int j = 0; j < expectSize1; j++) {
                    clientQuery.get(i).add(clientQueryPayload.get(j));
                }
            }
            clientQuery.add(new ArrayList<>());
            for (int j = 0; j < expectSize2; j++) {
                clientQuery.get(binNum - 1).add(clientQueryPayload.get(expectSize1 + j));
            }
        } else {
            for (int i = 0; i < binNum; i++) {
                clientQuery.add(new ArrayList<>());
                for (int j = 0; j < expectSize1; j++) {
                    clientQuery.get(i).add(clientQueryPayload.get(j));
                }
            }
        }
        IntStream intStream = this.parallel ? IntStream.range(0, binNum).parallel() : IntStream.range(0, binNum);
        return intStream
            .mapToObj(i -> Mbfk16IndexPirNativeUtils.generateReply(
                params.getEncryptionParams(), clientQuery.get(i), encodedDatabase.get(i), innerParams.getDimensionsLength()[i])
            )
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 返回数据库编码后的多项式。
     *
     * @param binIndex 分块索引。
     * @return 数据库编码后的多项式。
     */
    private ArrayList<byte[]> preprocessDatabase(int binIndex) {
        int byteLength = elementByteArray.get(binIndex)[0].length;
        byte[] combinedBytes = new byte[num * byteLength];
        IntStream.range(0, num).forEach(index -> {
            byte[] element = elementByteArray.get(binIndex)[index];
            System.arraycopy(element, 0, combinedBytes, index * byteLength, byteLength);
        });
        // number of FV plaintexts needed to create the d-dimensional matrix
        int prod = Arrays.stream(innerParams.getDimensionsLength()[binIndex]).reduce(1, (a, b) -> a * b);
        assert (innerParams.getPlaintextSize()[binIndex] <= prod);
        ArrayList<long[]> coeffsList = new ArrayList<>();
        // 每个多项式包含的字节长度
        int byteSizeOfPlaintext = innerParams.getElementSizeOfPlaintext()[binIndex] * byteLength;
        // 数据库总字节长度
        int totalByteSize = num * byteLength;
        // 一个多项式中需要使用的系数个数
        int usedCoeffSize = innerParams.getElementSizeOfPlaintext()[binIndex] *
            ((int) Math.ceil(Byte.SIZE * byteLength / (double) params.getPlainModulusBitLength()));
        // 系数个数不大于多项式阶数
        assert (usedCoeffSize <= params.getPolyModulusDegree())
            : "coefficient num must be less than or equal to polynomial degree";
        // 字节转换为多项式系数
        int offset = 0;
        for (int i = 0; i < innerParams.getPlaintextSize()[binIndex]; i++) {
            int processByteSize;
            if (totalByteSize <= offset) {
                break;
            } else if (totalByteSize < offset + byteSizeOfPlaintext) {
                processByteSize = totalByteSize - offset;
            } else {
                processByteSize = byteSizeOfPlaintext;
            }
            assert (processByteSize % byteLength == 0);
            // Get the coefficients of the elements that will be packed in plaintext i
            long[] coeffs = IndexPirUtils.convertBytesToCoeffs(
                params.getPlainModulusBitLength(), offset, processByteSize, combinedBytes
            );
            assert (coeffs.length <= usedCoeffSize);
            offset += processByteSize;
            long[] paddingCoeffsArray = new long[params.getPolyModulusDegree()];
            System.arraycopy(coeffs, 0, paddingCoeffsArray, 0, coeffs.length);
            // Pad the rest with 1s
            IntStream.range(coeffs.length, params.getPolyModulusDegree()).forEach(j -> paddingCoeffsArray[j] = 1L);
            coeffsList.add(paddingCoeffsArray);
        }
        // Add padding plaintext to make database a matrix
        int currentPlaintextSize = coeffsList.size();
        assert (currentPlaintextSize <= innerParams.getPlaintextSize()[binIndex]);
        IntStream.range(0, (prod - currentPlaintextSize))
            .mapToObj(i -> IntStream.range(0, params.getPolyModulusDegree()).mapToLong(i1 -> 1L).toArray())
            .forEach(coeffsList::add);
        return Mbfk16IndexPirNativeUtils.nttTransform(params.getEncryptionParams(), coeffsList);
    }
}