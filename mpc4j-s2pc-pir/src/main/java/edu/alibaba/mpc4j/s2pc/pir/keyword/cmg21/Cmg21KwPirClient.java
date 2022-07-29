package edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.s2pc.pir.keyword.AbstractKwPirClient;
import edu.alibaba.mpc4j.s2pc.pir.keyword.PolynomialUtils;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.OFBBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.createCuckooHashBin;
import static edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.getMaxItemSize;

/**
 * CMG21关键词索引PIR协议客户端。
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public class Cmg21KwPirClient extends AbstractKwPirClient {
    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * 关键词索引PIR方案参数
     */
    private final Cmg21KwPirParams params;
    /**
     * 无贮存区布谷鸟哈希类型
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * 无贮存区布谷鸟哈希分桶
     */
    private CuckooHashBin<ByteBuffer> cuckooHashBin;
    /**
     * 环境类型
     */
    private final EnvType envType;
    /**
     * β^{-1}
     */
    private BigInteger[] inverseBetas;

    public Cmg21KwPirClient(Rpc clientRpc, Party serverParty, Cmg21KwPirConfig config) {
        super(Cmg21KwPirPtoDesc.getInstance(), clientRpc, serverParty, config);
        this.cuckooHashBinType = config.getCuckooHashBinType();
        this.envType = config.getEnvType();
        this.params = config.getParams();
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
    }

    @Override
    public void init() {
        info("{}{} Client Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());
        // 客户端不需要预计算
        info("{}{} Client Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }


    @Override
    public Map<ByteBuffer, ByteBuffer> pir(Set<ByteBuffer> serverElementSet, int elementByteLength, int labelByteLength,
                                           int retrievalElementSize, int retrievalNumber) throws MpcAbortException {
        int maxClientElementSize = CuckooHashBinFactory.getMaxItemSize(cuckooHashBinType, params.getBinNum());
        assert params.getMaxItemPerQuery() <= maxClientElementSize;
        setInitInput(serverElementSet, elementByteLength, labelByteLength, params.getMaxItemPerQuery());
        initialized = true;
        setPtoInput(retrievalNumber, retrievalElementSize);

        info("{}{} Client begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        // 客户端接收服务端哈希密钥
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Cmg21KwPirPtoDesc.PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(),
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> hashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(hashKeyPayload.size() == params.getHashNum());

        Map<ByteBuffer, ByteBuffer> pirResultMap = new HashMap<>();
        for (int retrievalIndex = 0; retrievalIndex < retrievalNumber; retrievalIndex++) {
            info("{}{} Client Retrieval Number {})", ptoStepLogPrefix, getPtoDesc().getPtoName(), retrievalIndex + 1);
            // 客户端随机选取查询元素集合
            ArrayList<ByteBuffer> retrievalElementList = randomSelectRetrievalElementSet();

            // 客户端执行OPRF协议
            stopWatch.start();
            List<byte[]> blindPayload = generateBlindElements(retrievalElementList);
            DataPacketHeader blindHeader = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), Cmg21KwPirPtoDesc.PtoStep.CLIENT_SEND_BLIND.ordinal(), retrievalIndex,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(blindHeader, blindPayload));
            DataPacketHeader blindPrfHeader = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), Cmg21KwPirPtoDesc.PtoStep.SERVER_SEND_BLIND_PRF.ordinal(), retrievalIndex,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> blindPrfPayload = rpc.receive(blindPrfHeader).getPayload();
            ArrayList<ByteBuffer> oprfOutputs = handleBlindPrf(blindPrfPayload);
            Map<ByteBuffer, ByteBuffer> oprfMap = IntStream.range(0, clientElementSize)
                .boxed()
                .collect(Collectors.toMap(oprfOutputs::get, retrievalElementList::get, (a, b) -> b));
            stopWatch.stop();
            long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            info("{}{} Client Step 1/5 OPRF ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), oprfTime);

            // 客户端布谷鸟哈希分桶
            stopWatch.start();
            byte[][] hashKeys = hashKeyPayload.toArray(new byte[hashKeyPayload.size()][]);
            boolean cuckooHashSucceed = generateCuckooHashBin(oprfOutputs, params.getBinNum(), hashKeys);
            DataPacketHeader cuckooHashResultHeader = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), Cmg21KwPirPtoDesc.PtoStep.CLIENT_SEND_CUCKOO_HASH_RESULT.ordinal(), retrievalIndex,
                rpc.ownParty().getPartyId(), otherParty().getPartyId()
            );
            List<byte[]> cuckooHashResultBytes = new ArrayList<>();
            cuckooHashResultBytes.add(BigIntegerUtils.bigIntegerToByteArray(
                cuckooHashSucceed ? BigInteger.ONE : BigInteger.ZERO)
            );
            rpc.send(DataPacket.fromByteArrayList(cuckooHashResultHeader, cuckooHashResultBytes));
            MpcAbortPreconditions.checkArgument(cuckooHashSucceed, "cuckoo hash failed.");
            stopWatch.stop();
            long cuckooHashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            info("{}{} Client Step 2/5 cuckoo hash ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cuckooHashTime);

            // 客户端生成BFV算法密钥和参数
            stopWatch.start();
            List<byte[]> encryptionParams = Cmg21KwPirNativeClient.genEncryptionParameters(
                params.getPolyModulusDegree(), params.getPlainModulus(), params.getCoeffModulusBits());
            DataPacketHeader encryptionParamsDataPacketHeader = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), Cmg21KwPirPtoDesc.PtoStep.CLIENT_SEND_ENCRYPTION_PARAMS.ordinal(), retrievalIndex,
                rpc.ownParty().getPartyId(), otherParty().getPartyId()
            );
            MpcAbortPreconditions.checkArgument(encryptionParams.size() == 4);
            rpc.send(DataPacket.fromByteArrayList(encryptionParamsDataPacketHeader, encryptionParams.subList(0, 3)));
            stopWatch.stop();
            long keyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            info("{}{} Client Step key generation 3/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), keyGenTime);

            // 客户端加密查询信息
            stopWatch.start();
            List<long[][]> encodedQuery = encodeQuery();
            Stream<long[][]> stream = parallel ? encodedQuery.stream().parallel() : encodedQuery.stream();
            List<byte[]> queryCiphertextList = stream
                .map(i -> Cmg21KwPirNativeClient.generateQuery(
                    i, encryptionParams.get(0), encryptionParams.get(2), encryptionParams.get(3)))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
            DataPacketHeader clientQueryDataPacketHeader = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), Cmg21KwPirPtoDesc.PtoStep.CLIENT_SEND_QUERY.ordinal(), retrievalIndex,
                rpc.ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(clientQueryDataPacketHeader, queryCiphertextList));
            stopWatch.stop();
            long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            info("{}{} Client Step query generation 4/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), genQueryTime);

            info("{}{} Client receive Server's reply", ptoStepLogPrefix, getPtoDesc().getPtoName());
            DataPacketHeader serverItemResponseDataPacketSpec = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), Cmg21KwPirPtoDesc.PtoStep.SERVER_SEND_ITEM_RESPONSE.ordinal(), retrievalIndex,
                otherParty().getPartyId(), rpc.ownParty().getPartyId()
            );
            List<byte[]> itemResponse = rpc.receive(serverItemResponseDataPacketSpec).getPayload();
            DataPacketHeader serverLabelResponseDataPacketSpec = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), Cmg21KwPirPtoDesc.PtoStep.SERVER_SEND_LABEL_RESPONSE.ordinal(), retrievalIndex,
                otherParty().getPartyId(), rpc.ownParty().getPartyId()
            );
            List<byte[]> labelResponse = rpc.receive(serverLabelResponseDataPacketSpec).getPayload();
            int ciphertextNumber = params.getBinNum() / (params.getPolyModulusDegree() / params.getItemEncodedSlotSize());
            MpcAbortPreconditions.checkArgument(itemResponse.size() % ciphertextNumber == 0 &&
                labelResponse.size() % ciphertextNumber == 0);

            // 客户端解密服务端回复
            stopWatch.start();
            Stream<byte[]> itemStream = parallel ? itemResponse.stream().parallel() : itemResponse.stream();
            List<long[]> decodedItem = itemStream
                .map(i -> Cmg21KwPirNativeClient.decodeReply(i, encryptionParams.get(0), encryptionParams.get(3))
                ).collect(Collectors.toList());
            Stream<byte[]> labelStream = parallel ? labelResponse.stream().parallel() : labelResponse.stream();
            List<long[]> decodedLabel = labelStream
                .map(i -> Cmg21KwPirNativeClient.decodeReply(i, encryptionParams.get(0), encryptionParams.get(3))
                ).collect(Collectors.toList());
            pirResultMap.putAll(recoverPirResult(decodedItem, decodedLabel, oprfMap));
            stopWatch.stop();
            long decodeResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            info("{}{} Client Step (decode response) 5/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(),
                decodeResponseTime);
        }

        info("{}{} Client end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return pirResultMap;
    }

    /**
     * 恢复关键词索引PIR结果。
     *
     * @param decryptedItemResponse  解密后的服务端回复元素。
     * @param decryptedLabelResponse 解密后的服务端回复标签。
     * @param oprfMap                OPRF映射。
     * @return 关键词索引PIR结果
     */
    private Map<ByteBuffer, ByteBuffer> recoverPirResult(List<long[]> decryptedItemResponse,
                                                         List<long[]> decryptedLabelResponse,
                                                         Map<ByteBuffer, ByteBuffer> oprfMap) {
        Map<ByteBuffer, ByteBuffer> pirResultMap = new HashMap<>();
        int ciphertextNum = params.getBinNum() / (params.getPolyModulusDegree() / params.getItemEncodedSlotSize());
        int itemPerCiphertext = params.getPolyModulusDegree() / params.getItemEncodedSlotSize();
        int itemPartitionNum = decryptedItemResponse.size() / ciphertextNum;
        int labelPartitionNum = (int) Math.ceil((labelByteLength + CommonConstants.BLOCK_BYTE_LENGTH) * 8.0 /
            ((BigInteger.valueOf(params.getPlainModulus()).bitLength() - 1) * params.getItemEncodedSlotSize()));
        int shiftBits = (int) Math.ceil((labelByteLength + CommonConstants.BLOCK_BYTE_LENGTH) * 8.0 /
            (params.getItemEncodedSlotSize() * labelPartitionNum));
        for (int i = 0; i < decryptedItemResponse.size(); i++) {
            // 找到匹配元素的所在行
            List<Integer> matchedItem = new ArrayList<>();
            for (int j = 0; j < params.getItemEncodedSlotSize() * itemPerCiphertext; j++) {
                if (decryptedItemResponse.get(i)[j] == 0) {
                    matchedItem.add(j);
                }
            }
            for (int j = 0; j < matchedItem.size() - params.getItemEncodedSlotSize() + 1; j++) {
                if (matchedItem.get(j) % params.getItemEncodedSlotSize() == 0) {
                    if (matchedItem.get(j + params.getItemEncodedSlotSize() - 1) - matchedItem.get(j)
                        == params.getItemEncodedSlotSize() - 1) {
                        int hashBinIndex = matchedItem.get(j) / params.getItemEncodedSlotSize() + (i / itemPartitionNum) * itemPerCiphertext;
                        BigInteger label = BigInteger.ZERO;
                        int index = 0;
                        for (int l = 0; l < labelPartitionNum; l++) {
                            for (int k = 0; k < params.getItemEncodedSlotSize(); k++) {
                                BigInteger temp = BigInteger.valueOf(
                                        decryptedLabelResponse.get(i * labelPartitionNum + l)[matchedItem.get(j + k)])
                                    .shiftLeft(shiftBits * index);
                                label = label.add(temp);
                                index++;
                            }
                        }
                        byte[] keyBytes = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                        IntStream.range(0, CommonConstants.BLOCK_BYTE_LENGTH)
                            .forEach(k -> keyBytes[k] = cuckooHashBin.getHashBinEntry(hashBinIndex).getItem().array()[k]);
                        pirResultMap.put(oprfMap.get(cuckooHashBin.getHashBinEntry(hashBinIndex).getItem()),
                            labelDecryption(keyBytes, ByteBuffer.wrap(BigIntegerUtils.nonNegBigIntegerToByteArray(
                                label, labelByteLength + CommonConstants.BLOCK_BYTE_LENGTH)))
                        );
                        j = j + params.getItemEncodedSlotSize() - 1;
                    }
                }
            }
        }
        return pirResultMap;
    }

    /**
     * 返回标签明文。
     *
     * @param keyBytes       密钥字节。
     * @param encryptedLabel 标签密文。
     * @return 标签明文。
     */
    private ByteBuffer labelDecryption(byte[] keyBytes, ByteBuffer encryptedLabel) {
        BlockCipher blockCipher = new AESEngine();
        StreamCipher streamCipher = new OFBBlockCipher(blockCipher, 8 * CommonConstants.BLOCK_BYTE_LENGTH);
        byte[] ivBytes = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        System.arraycopy(encryptedLabel.array(), 0, ivBytes, 0, CommonConstants.BLOCK_BYTE_LENGTH);
        byte[] inputs = new byte[encryptedLabel.array().length - CommonConstants.BLOCK_BYTE_LENGTH];
        System.arraycopy(encryptedLabel.array(), CommonConstants.BLOCK_BYTE_LENGTH, inputs, 0, labelByteLength);
        KeyParameter key = new KeyParameter(keyBytes);
        CipherParameters withIv = new ParametersWithIV(key, ivBytes);
        streamCipher.init(false, withIv);
        byte[] outputs = new byte[labelByteLength];
        streamCipher.processBytes(inputs, 0, labelByteLength, outputs, 0);
        return ByteBuffer.wrap(outputs);
    }

    /**
     * 生成布谷鸟哈希分桶。
     *
     * @param itemList 元素列表。
     * @param binNum   指定桶数量。
     * @param hashKeys 哈希算法密钥。
     * @return 布谷鸟哈希分桶是否成功。
     */
    private boolean generateCuckooHashBin(ArrayList<ByteBuffer> itemList, int binNum, byte[][] hashKeys)
        throws MpcAbortException {
        // 初始化布谷鸟哈希
        cuckooHashBin = createCuckooHashBin(envType, cuckooHashBinType, clientElementSize, binNum, hashKeys);
        MpcAbortPreconditions.checkArgument(
            maxClientRetrievalElementSize <= getMaxItemSize(cuckooHashBinType, params.getBinNum()));
        boolean success = false;
        // 将客户端消息插入到CuckooHash中
        cuckooHashBin.insertItems(itemList);
        if (cuckooHashBin.itemNumInStash() == 0) {
            success = true;
        }
        // 如果成功，则向布谷鸟哈希的空余位置插入空元素
        cuckooHashBin.insertPaddingItems(botElementByteBuffer);
        return success;
    }

    /**
     * 返回查询信息的编码。
     *
     * @return 查询信息的编码。
     */
    public List<long[][]> encodeQuery() {
        int ciphertextNum = params.getBinNum() / (params.getPolyModulusDegree() / params.getItemEncodedSlotSize());
        int itemPerCiphertext = params.getPolyModulusDegree() / params.getItemEncodedSlotSize();
        long[][] items = new long[ciphertextNum][params.getPolyModulusDegree()];
        for (int i = 0; i < ciphertextNum; i++) {
            for (int j = 0; j < itemPerCiphertext; j++) {
                long[] item = params.getHashBinEntryEncodedArray(
                    cuckooHashBin.getHashBinEntry(i * itemPerCiphertext + j), true
                );
                System.arraycopy(item, 0, items[i], j * params.getItemEncodedSlotSize(), params.getItemEncodedSlotSize());
            }
            for (int j = itemPerCiphertext * params.getItemEncodedSlotSize(); j < params.getPolyModulusDegree(); j++) {
                items[i][j] = 0;
            }
        }
        IntStream intStream = parallel ? IntStream.range(0, ciphertextNum).parallel() : IntStream.range(0, ciphertextNum);
        return intStream
            .mapToObj(i ->
                PolynomialUtils.computePowers(items[i], params.getPlainModulus(), params.getQueryPowers()))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 客户端从服务端元素集合中随机选取查询元素集合。
     *
     * @return 查询元素集合。
     */
    private ArrayList<ByteBuffer> randomSelectRetrievalElementSet() {
        ArrayList<ByteBuffer> clientRetrievalElementArrayList = new ArrayList<>();
        for (int i = 0; i < clientElementSize; i++) {
            int index;
            do {
                index = Math.abs(secureRandom.nextInt()) % serverElementArrayList.size();
            } while (clientRetrievalElementArrayList.contains(serverElementArrayList.get(index)));
            clientRetrievalElementArrayList.add(serverElementArrayList.get(index));
        }
        return clientRetrievalElementArrayList;
    }

    /**
     * 生成盲化元素。
     *
     * @param retrievalElementArrayList 客户端查询元素列表。
     * @return 盲化元素。
     */
    private List<byte[]> generateBlindElements(ArrayList<ByteBuffer> retrievalElementArrayList) {
        Ecc ecc = EccFactory.createInstance(envType);
        BigInteger n = ecc.getN();
        inverseBetas = new BigInteger[retrievalElementArrayList.size()];
        IntStream batchIntStream = IntStream.range(0, retrievalElementArrayList.size());
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        return batchIntStream
            .mapToObj(index -> {
                // 生成盲化因子
                BigInteger beta = BigIntegerUtils.randomPositive(n, secureRandom);
                inverseBetas[index] = beta.modInverse(n);
                // hash to point
                ECPoint element = ecc.hashToCurve(retrievalElementArrayList.get(index).array());
                // 盲化
                return ecc.multiply(element, beta);
            })
            .map(element -> ecc.encode(element, true))
            .collect(Collectors.toList());
    }

    /**
     * 处理盲化元素PRF。
     *
     * @param blindPrf 盲化元素PRF。
     * @return 元素PRF。
     * @throws MpcAbortException 如果协议异常中止。
     */
    private ArrayList<ByteBuffer> handleBlindPrf(List<byte[]> blindPrf) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(blindPrf.size() == clientElementSize);
        byte[][] blindPrfArray = blindPrf.toArray(new byte[0][]);
        Ecc ecc = EccFactory.createInstance(envType);
        IntStream batchIntStream = IntStream.range(0, clientElementSize);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        byte[][] bytes = batchIntStream
            .mapToObj(index -> {
                // 解码
                ECPoint element = ecc.decode(blindPrfArray[index]);
                // 去盲化
                return ecc.multiply(element, inverseBetas[index]);
            })
            .map(element -> ecc.encode(element, true))
            .toArray(byte[][]::new);
        IntStream intStream = parallel ? IntStream.range(0, clientElementSize).parallel() : IntStream.range(0, clientElementSize);
        return intStream
            .mapToObj(i -> ByteBuffer.wrap(bytes[i]))
            .collect(Collectors.toCollection(ArrayList::new));
    }

}
