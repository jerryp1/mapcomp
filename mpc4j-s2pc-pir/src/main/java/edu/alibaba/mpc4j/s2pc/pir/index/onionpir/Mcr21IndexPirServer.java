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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    private List<ArrayList<long[]>> bfvPlaintext;
    /**
     * 公钥
     */
    private byte[] publicKey;
    /**
     * Galois密钥
     */
    private byte[] galoisKeys;
    /**
     * 私钥密文
     */
    private ArrayList<byte[]> encryptedSecretKey;

    public Mcr21IndexPirServer(Rpc serverRpc, Party clientParty, Mcr21IndexPirConfig config) {
        super(Mcr21IndexPirPtoDesc.getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(AbstractIndexPirParams indexPirParams, ArrayList<ByteBuffer> elementArrayList,
                     int elementByteLength) {
        assert (indexPirParams instanceof Mcr21IndexPirParams);
        params = (Mcr21IndexPirParams) indexPirParams;
        info("{}{} Server Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 一个多项式可表示的字节长度
        int maxElementByteLength = params.getPolyModulusDegree() * params.getPlainModulusBitLength() / Byte.SIZE;
        setInitInput(elementArrayList, elementByteLength, maxElementByteLength);
        // 服务端对数据库进行编码
        int bundleNum = params.getBundleNum();
        IntStream intStream = this.parallel ? IntStream.range(0, bundleNum).parallel() : IntStream.range(0, bundleNum);
        bfvPlaintext = intStream.mapToObj(bundleIndex -> {
            int bundleElementByteLength = bundleIndex == bundleNum - 1 ?
                elementByteLength % maxElementByteLength : maxElementByteLength;
            ArrayList<long[]> coeffArray = encodeDatabase(
                params.getPolyModulusDegree(),
                params.getPlaintextSize()[bundleIndex],
                params.getPlainModulusBitLength(),
                params.getDimensionsLength()[bundleIndex],
                params.getElementSizeOfPlaintext()[bundleIndex],
                bundleElementByteLength,
                bundleIndex
            );
            return Mcr21IndexPirNativeUtils.preprocessDatabase(params.getEncryptionParams(), coeffArray);
        }).collect(Collectors.toList());
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

        // 服务端处理问询
        stopWatch.start();
        ArrayList<ArrayList<byte[]>> clientQuery = handleClientQueryPayload(clientQueryPayload);
        int bundleNum = params.getBundleNum();
        IntStream intStream = this.parallel ? IntStream.range(0, bundleNum).parallel() : IntStream.range(0, bundleNum);
        ArrayList<byte[]> serverResponse = intStream
            .mapToObj(bundleIndex ->
                Mcr21IndexPirNativeUtils.generateReply(
                    params.getEncryptionParams(),
                    publicKey,
                    galoisKeys,
                    encryptedSecretKey,
                    clientQuery.get(bundleIndex),
                    bfvPlaintext.get(bundleIndex),
                    params.getDimensionsLength()[bundleIndex]
                ))
            .collect(Collectors.toCollection(ArrayList::new));
        DataPacketHeader serverResponseHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Mcr21IndexPirPtoDesc.PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverResponseHeader, serverResponse));
        stopWatch.stop();
        long genResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), genResponseTime);

        info("{}{} Server end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    /**
     * 服务端处理客户端查询信息。
     *
     * @param clientQueryPayload 客户端查询信息。
     * @return 查询密文。
     * @throws MpcAbortException 如果协议异常中止。
     */
    private ArrayList<ArrayList<byte[]>> handleClientQueryPayload(ArrayList<byte[]> clientQueryPayload)
        throws MpcAbortException {
        int totalSize = clientQueryPayload.size();
        int bundleNum = params.getBundleNum();
        ArrayList<ArrayList<byte[]>> clientQuery = new ArrayList<>();
        int expectSize, querySize1, querySize2 = 0;
        if (params.getDimensionsLength()[0].length == 1) {
            querySize1 = 2;
        } else {
            querySize1 = 3;
        }
        for (int i = 0; i < bundleNum; i++) {
            clientQuery.add(new ArrayList<>());
            for (int j = 0; j < querySize1; j++) {
                clientQuery.get(i).add(clientQueryPayload.get(j));
            }
        }
        if ((bundleNum > 1) && (params.getPlaintextSize()[0] != params.getPlaintextSize()[bundleNum - 1])) {
            if (params.getDimensionsLength()[bundleNum - 1].length == 1) {
                querySize2 = 2;
            } else {
                querySize2 = 3;
            }
            clientQuery.get(bundleNum - 1).clear();
            for (int j = 0; j < querySize2; j++) {
                clientQuery.get(bundleNum - 1).add(clientQueryPayload.get(querySize1 + j));
            }
        }
        int querySize = querySize1 + querySize2;
        expectSize = querySize + 2 + params.getGswDecompSize() * 2;
        MpcAbortPreconditions.checkArgument(totalSize == expectSize);
        this.publicKey = clientQueryPayload.get(querySize);
        this.galoisKeys = clientQueryPayload.get(querySize + 1);
        this.encryptedSecretKey = new ArrayList<>();
        this.encryptedSecretKey.addAll(
            clientQueryPayload.subList(querySize + 2, querySize + 2 + params.getGswDecompSize() * 2)
        );
        return clientQuery;
    }
}
