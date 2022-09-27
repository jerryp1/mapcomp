package edu.alibaba.mpc4j.s2pc.pir.index.xpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.index.AbstractIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirParams;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * XPIR协议客户端。
 *
 * @author Liqiang Peng
 * @date 2022/8/24
 */
public class XPirClient extends AbstractIndexPirClient {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * XPIR方案参数
     */
    private XPirParams params;

    public XPirClient(Rpc clientRpc, Party serverParty, XPirConfig config) {
        super(XPirPtoDesc.getInstance(), clientRpc, serverParty, config);
    }

    @Override
    public void init(IndexPirParams indexPirParams, int serverElementSize, int elementByteLength) throws MpcAbortException {
        setInitInput(serverElementSize, elementByteLength);
        info("{}{} Client Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        assert (indexPirParams instanceof XPirParams);
        params = (XPirParams) indexPirParams;

        initialized = true;
        info("{}{} Client Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public ByteBuffer pir(int index) throws MpcAbortException {
        setPtoInput(index);

        info("{}{} Client begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());
        // 客户端生成BFV算法公私钥对
        stopWatch.start();
        List<byte[]> keyPair = XPirNativeClient.keyGeneration(params.getEncryptionParams());
        stopWatch.stop();
        long keyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 1/3 Key Generation ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), keyGenTime);

        // 客户端生成查询密文
        stopWatch.start();
        List<byte[]> encryptedQueryList = generateQuery(params.getEncryptionParams(), keyPair.get(0), keyPair.get(1));
        DataPacketHeader clientQueryDataPacketHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), XPirPtoDesc.PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientQueryDataPacketHeader, encryptedQueryList));
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 2/3 Generation Query ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), genQueryTime);

        // 客户端接收服务端回复
        DataPacketHeader responseHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), XPirPtoDesc.PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> responsePayload = rpc.receive(responseHeader).getPayload();

        // 客户端解密服务端回复
        stopWatch.start();
        ByteBuffer retrievalResult = ByteBuffer.wrap(decodeReply(keyPair.get(1), responsePayload));
        stopWatch.stop();
        long decodeReplyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 3/3 Decode Reply ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), decodeReplyTime);

        info("{}{} Client end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return retrievalResult;
    }

    /**
     * 解码回复得到检索结果。
     *
     * @param secretKey 私钥。
     * @param response  回复。
     * @return 检索结果。
     * @throws MpcAbortException 如果协议异常中止。
     */
    private byte[] decodeReply(byte[] secretKey, List<byte[]> response) throws MpcAbortException {
        long[] decodedCoeffArray = XPirNativeClient.decodeReply(
            params.getEncryptionParams(), secretKey, (ArrayList<byte[]>) response, params.getDimension()
        );
        byte[] decodeByteArray = convertCoeffsToBytes(decodedCoeffArray);
        byte[] elementBytes = new byte[elementByteLength];
        // offset in FV plaintext
        int offset =  index % params.getElementSizeOfPlaintext();
        System.arraycopy(decodeByteArray, offset * elementByteLength, elementBytes, 0, elementByteLength);
        return elementBytes;
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
        int[] dimensionSize = params.getDimensionsLength();
        // index of FV plaintext
        int indexOfPlaintext = index / params.getElementSizeOfPlaintext();
        // 计算每个维度的坐标
        long[] indices = computeIndices(indexOfPlaintext, dimensionSize);
        ArrayList<Integer> plainQuery = new ArrayList<>();
        int pt;
        for (int i = 0; i < indices.length; i++) {
            // 该维度中密文多项式的数量
            info("Client: index " + (i+1) + " / " + indices.length + " = " + indices[i]);
            info("Client: number of ciphertexts needed for query = " + dimensionSize[i]);
            for (int j = 0; j < dimensionSize[i]; j++) {
                // 第indices.get(i)个待加密明文为1, 其余明文为0
                pt = j == indices[i] ? 1 : 0;
                plainQuery.add(pt);
            }
        }
        // 返回查询密文
        return XPirNativeClient.generateQuery(
            encryptionParams, publicKey, secretKey, plainQuery.stream().mapToInt(integer -> integer).toArray()
        );
    }

    /**
     * 将long型数组转换为字节数组。
     *
     * @param longArray long型数组。
     * @return 字节数组。
     * @throws MpcAbortException 如果协议异常中止。
     */
    private byte[] convertCoeffsToBytes(long[] longArray) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(longArray.length == params.getPolyModulusDegree());
        int logt = params.getPlainModulusBitLength();
        int longArrayLength = longArray.length;
        byte[] byteArray = new byte[longArrayLength * logt / Byte.SIZE];
        int room = Byte.SIZE;
        int j = 0;
        for (long l : longArray) {
            long src = l;
            int rest = logt;
            while (rest != 0 && j < byteArray.length) {
                int shift = Math.min(room, rest);
                byteArray[j] = (byte) (byteArray[j] << shift);
                byteArray[j] = (byte) (byteArray[j] | (src >> (logt - shift)));
                src = src << shift;
                room -= shift;
                rest -= shift;
                if (room == 0) {
                    j++;
                    room = Byte.SIZE;
                }
            }
        }
        return byteArray;
    }

    /**
     * 计算各维度的坐标。
     *
     * @param retrievalIndex 索引值。
     * @param dimensionSize  各维度的长度。
     * @return 各维度的坐标。
     */
    private long[] computeIndices(int retrievalIndex, int[] dimensionSize) {
        long product = Arrays.stream(dimensionSize).asLongStream().reduce(1, (a, b) -> a * b);
        long[] indices = new long[dimensionSize.length];
        for (int i = 0; i < dimensionSize.length; i++) {
            product /= dimensionSize[i];
            long ji = retrievalIndex / product;
            indices[i] = ji;
            retrievalIndex -= ji * product;
        }
        return indices;
    }
}
