package edu.alibaba.mpc4j.s2pc.pir.index.sealpir;

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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * SEAL PIR协议服务端。
 *
 * @author Liqiang Peng
 * @date 2023/1/17
 */
public class Acls18IndexPirServer extends AbstractIndexPirServer {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * XPIR方案参数
     */
    private Acls18IndexPirParams params;
    /**
     * BFV明文（点值表示）
     */
    private List<ArrayList<byte[]>> bfvPlaintext;
    /**
     * Galois密钥
     */
    private byte[] galoisKeys;

    public Acls18IndexPirServer(Rpc serverRpc, Party clientParty, Acls18IndexPirConfig config) {
        super(Acls18IndexPirPtoDesc.getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(AbstractIndexPirParams indexPirParams, ArrayList<ByteBuffer> elementArrayList,
                     int elementByteLength) {
        assert (indexPirParams instanceof Acls18IndexPirParams);
        params = (Acls18IndexPirParams) indexPirParams;
        info("{}{} Server Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
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
            return Acls18IndexPirNativeUtils.nttTransform(params.getEncryptionParams(), coeffArray);
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

        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Acls18IndexPirPtoDesc.PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        ArrayList<byte[]> clientQueryPayload = new ArrayList<>(rpc.receive(clientQueryHeader).getPayload());

        // 服务端接收并处理问询
        stopWatch.start();
        ArrayList<ArrayList<byte[]>> clientQuery = handleClientQueryPayload(clientQueryPayload);
        int bundleNum = params.getBundleNum();
        IntStream intStream = this.parallel ? IntStream.range(0, bundleNum).parallel() : IntStream.range(0, bundleNum);
        ArrayList<byte[]> serverResponsePayload = intStream
            .mapToObj(i -> Acls18IndexPirNativeUtils.generateReply(
                params.getEncryptionParams(), galoisKeys, clientQuery.get(i), bfvPlaintext.get(i),
                params.getDimensionsLength()[i])
            )
            .flatMap(Collection::stream).collect(Collectors.toCollection(ArrayList::new));
        DataPacketHeader serverResponseHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Acls18IndexPirPtoDesc.PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverResponseHeader, serverResponsePayload));
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
        int expectSize2 = 0;
        int[] nvec = params.getDimensionsLength()[0];
        ArrayList<ArrayList<byte[]>> clientQuery = new ArrayList<>();
        int expectSize1 = Arrays.stream(nvec).map(k -> (int) Math.ceil((k + 0.0) / params.getPolyModulusDegree())).sum();
        if ((bundleNum > 1) && (params.getPlaintextSize()[0] != params.getPlaintextSize()[bundleNum - 1])) {
            nvec = params.getDimensionsLength()[bundleNum - 1];
            expectSize2 = Arrays.stream(nvec).map(k -> (int) Math.ceil((k + 0.0) / params.getPolyModulusDegree())).sum();
            for (int i = 0; i < bundleNum - 1; i++) {
                clientQuery.add(new ArrayList<>());
                for (int j = 0; j < expectSize1; j++) {
                    clientQuery.get(i).add(clientQueryPayload.get(j));
                }
            }
            clientQuery.add(new ArrayList<>());
            for (int j = 0; j < expectSize2; j++) {
                clientQuery.get(bundleNum - 1).add(clientQueryPayload.get(expectSize1 + j));
            }
        } else {
            for (int i = 0; i < bundleNum; i++) {
                clientQuery.add(new ArrayList<>());
                for (int j = 0; j < expectSize1; j++) {
                    clientQuery.get(i).add(clientQueryPayload.get(j));
                }
            }
        }
        MpcAbortPreconditions.checkArgument(totalSize == expectSize1 + expectSize2 + 1);
        this.galoisKeys = clientQueryPayload.get(expectSize1 + expectSize2);
        return clientQuery;
    }
}
