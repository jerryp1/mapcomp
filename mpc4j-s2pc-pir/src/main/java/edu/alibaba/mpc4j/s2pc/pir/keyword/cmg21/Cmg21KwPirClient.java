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
import edu.alibaba.mpc4j.common.tool.galoisfield.Zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.Zp64.Zp64Factory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.s2pc.pir.keyword.AbstractKwPirClient;
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

/**
 * CMG21关键词索引PIR协议客户端。
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public class Cmg21KwPirClient<T> extends AbstractKwPirClient<T> {
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
    /**
     * 哈希算法密钥
     */
    private byte[][] hashKeys;

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
    public void init(int labelByteLength) throws MpcAbortException {
        int maxClientElementSize = CuckooHashBinFactory.getMaxItemSize(cuckooHashBinType, params.getBinNum());
        setInitInput(labelByteLength, maxClientElementSize);

        info("{}{} Client Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());
        // 客户端接收服务端哈希密钥
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Cmg21KwPirPtoDesc.PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(),
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> hashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(hashKeyPayload.size() == params.getHashNum());
        hashKeys = hashKeyPayload.toArray(new byte[hashKeyPayload.size()][]);

        initialized = true;
        info("{}{} Client Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public Map<T, ByteBuffer> pir(Set<T> clientKeywordSet) throws MpcAbortException {
        setPtoInput(clientKeywordSet);

        info("{}{} Client begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());
        // 客户端执行OPRF协议
        stopWatch.start();
        List<byte[]> keywordBlindPayload = generateBlindElements(clientKeywordArrayList);
        DataPacketHeader blindHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Cmg21KwPirPtoDesc.PtoStep.CLIENT_SEND_BLIND.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(blindHeader, keywordBlindPayload));
        DataPacketHeader blindPrfHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Cmg21KwPirPtoDesc.PtoStep.SERVER_SEND_BLIND_PRF.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> blindPrfPayload = rpc.receive(blindPrfHeader).getPayload();
        ArrayList<ByteBuffer> keywordPrfOutputArrayList = handleBlindPrf(blindPrfPayload);
        Map<ByteBuffer, ByteBuffer> prfKeywordMap = IntStream.range(0, clientKeywordSize)
            .boxed()
            .collect(Collectors.toMap(keywordPrfOutputArrayList::get, i -> clientKeywordArrayList.get(i), (a, b) -> b));
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 1/5 OPRF ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), oprfTime);

        // 客户端布谷鸟哈希分桶
        stopWatch.start();
        boolean cuckooHashSucceed = generateCuckooHashBin(keywordPrfOutputArrayList, params.getBinNum(), hashKeys);
        DataPacketHeader cuckooHashResultHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Cmg21KwPirPtoDesc.PtoStep.CLIENT_SEND_CUCKOO_HASH_RESULT.ordinal(), extraInfo,
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
        List<byte[]> encryptionParamsList = Cmg21KwPirNativeClient.genEncryptionParameters(
            params.getPolyModulusDegree(), params.getPlainModulus(), params.getCoeffModulusBits());
        DataPacketHeader encryptionParamsDataPacketHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Cmg21KwPirPtoDesc.PtoStep.CLIENT_SEND_ENCRYPTION_PARAMS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        MpcAbortPreconditions.checkArgument(encryptionParamsList.size() == 4);
        rpc.send(DataPacket.fromByteArrayList(encryptionParamsDataPacketHeader, encryptionParamsList.subList(0, 3)));
        stopWatch.stop();
        long keyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step key generation 3/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), keyGenTime);

        // 客户端加密查询信息
        stopWatch.start();
        List<long[][]> encodedQueryList = encodeQuery();
        Stream<long[][]> stream = parallel ? encodedQueryList.stream().parallel() : encodedQueryList.stream();
        List<byte[]> encryptedQueryList = stream
            .map(i -> Cmg21KwPirNativeClient.generateQuery(
                encryptionParamsList.get(0), encryptionParamsList.get(2), encryptionParamsList.get(3), i))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        DataPacketHeader clientQueryDataPacketHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Cmg21KwPirPtoDesc.PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientQueryDataPacketHeader, encryptedQueryList));
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step query generation 4/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), genQueryTime);

        info("{}{} Client receive Server's reply", ptoStepLogPrefix, getPtoDesc().getPtoName());
        int ciphertextNumber = params.getBinNum() / (params.getPolyModulusDegree() / params.getItemEncodedSlotSize());
        DataPacketHeader serverItemResponseDataPacketSpec = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Cmg21KwPirPtoDesc.PtoStep.SERVER_SEND_ITEM_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> keywordReply = rpc.receive(serverItemResponseDataPacketSpec).getPayload();
        MpcAbortPreconditions.checkArgument(keywordReply.size() % ciphertextNumber == 0);
        DataPacketHeader serverLabelResponseDataPacketSpec = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Cmg21KwPirPtoDesc.PtoStep.SERVER_SEND_LABEL_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> labelReply = rpc.receive(serverLabelResponseDataPacketSpec).getPayload();
        MpcAbortPreconditions.checkArgument(labelReply.size() % ciphertextNumber == 0);

        // 客户端解密服务端回复
        stopWatch.start();
        Stream<byte[]> keywordReplyStream = parallel ? keywordReply.stream().parallel() : keywordReply.stream();
        List<long[]> decryptedKeywordReply = keywordReplyStream
            .map(i -> Cmg21KwPirNativeClient.decodeReply(encryptionParamsList.get(0), encryptionParamsList.get(3), i))
            .collect(Collectors.toList());
        Stream<byte[]> labelReplyStream = parallel ? labelReply.stream().parallel() : labelReply.stream();
        List<long[]> decryptedLabelReply = labelReplyStream
            .map(i -> Cmg21KwPirNativeClient.decodeReply(encryptionParamsList.get(0), encryptionParamsList.get(3), i))
            .collect(Collectors.toList());
        stopWatch.stop();
        long decodeResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step (decode response) 5/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(),
            decodeResponseTime);

        info("{}{} Client end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return recoverPirResult(decryptedKeywordReply, decryptedLabelReply, prfKeywordMap);
    }

    /**
     * 恢复关键词索引PIR结果。
     *
     * @param decryptedKeywordReply 解密后的服务端回复元素。
     * @param decryptedLabelReply   解密后的服务端回复标签。
     * @param oprfMap               OPRF映射。
     * @return 关键词索引PIR结果
     */
    private Map<T, ByteBuffer> recoverPirResult(List<long[]> decryptedKeywordReply, List<long[]> decryptedLabelReply,
                                                Map<ByteBuffer, ByteBuffer> oprfMap) {
        Map<T, ByteBuffer> resultMap = new HashMap<>();
        int ciphertextNum = params.getBinNum() / (params.getPolyModulusDegree() / params.getItemEncodedSlotSize());
        int itemPerCiphertext = params.getPolyModulusDegree() / params.getItemEncodedSlotSize();
        int itemPartitionNum = decryptedKeywordReply.size() / ciphertextNum;
        int labelPartitionNum = (int) Math.ceil((labelByteLength + CommonConstants.BLOCK_BYTE_LENGTH) * 8.0 /
            ((BigInteger.valueOf(params.getPlainModulus()).bitLength() - 1) * params.getItemEncodedSlotSize()));
        int shiftBits = (int) Math.ceil((labelByteLength + CommonConstants.BLOCK_BYTE_LENGTH) * 8.0 /
            (params.getItemEncodedSlotSize() * labelPartitionNum));
        for (int i = 0; i < decryptedKeywordReply.size(); i++) {
            // 找到匹配元素的所在行
            List<Integer> matchedItem = new ArrayList<>();
            for (int j = 0; j < params.getItemEncodedSlotSize() * itemPerCiphertext; j++) {
                if (decryptedKeywordReply.get(i)[j] == 0) {
                    matchedItem.add(j);
                }
            }
            for (int j = 0; j < matchedItem.size() - params.getItemEncodedSlotSize() + 1; j++) {
                if (matchedItem.get(j) % params.getItemEncodedSlotSize() == 0) {
                    if (matchedItem.get(j + params.getItemEncodedSlotSize() - 1) - matchedItem.get(j)
                        == params.getItemEncodedSlotSize() - 1) {
                        int hashBinIndex = matchedItem.get(j) / params.getItemEncodedSlotSize() + (i / itemPartitionNum)
                            * itemPerCiphertext;
                        BigInteger label = BigInteger.ZERO;
                        int index = 0;
                        for (int l = 0; l < labelPartitionNum; l++) {
                            for (int k = 0; k < params.getItemEncodedSlotSize(); k++) {
                                BigInteger temp = BigInteger.valueOf(
                                        decryptedLabelReply.get(i * labelPartitionNum + l)[matchedItem.get(j + k)])
                                    .shiftLeft(shiftBits * index);
                                label = label.add(temp);
                                index++;
                            }
                        }
                        byte[] keyBytes = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                        IntStream.range(0, CommonConstants.BLOCK_BYTE_LENGTH)
                            .forEach(k -> keyBytes[k] = cuckooHashBin.getHashBinEntry(hashBinIndex).getItem().array()[k]);
                        resultMap.put(
                            byteArrayObjectMap.get(oprfMap.get(cuckooHashBin.getHashBinEntry(hashBinIndex).getItem())),
                            ByteBuffer.wrap(labelDecryption(keyBytes, BigIntegerUtils.nonNegBigIntegerToByteArray(label,
                                labelByteLength + CommonConstants.BLOCK_BYTE_LENGTH)))
                        );
                        j = j + params.getItemEncodedSlotSize() - 1;
                    }
                }
            }
        }
        return resultMap;
    }

    /**
     * 返回标签明文。
     *
     * @param keyBytes       密钥字节。
     * @param encryptedLabel 标签密文。
     * @return 标签明文。
     */
    private byte[] labelDecryption(byte[] keyBytes, byte[] encryptedLabel) {
        assert encryptedLabel.length > CommonConstants.BLOCK_BYTE_LENGTH;
        BlockCipher blockCipher = new AESEngine();
        StreamCipher streamCipher = new OFBBlockCipher(blockCipher, 8 * CommonConstants.BLOCK_BYTE_LENGTH);
        byte[] ivBytes = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        System.arraycopy(encryptedLabel, 0, ivBytes, 0, CommonConstants.BLOCK_BYTE_LENGTH);
        byte[] inputs = new byte[encryptedLabel.length - CommonConstants.BLOCK_BYTE_LENGTH];
        System.arraycopy(encryptedLabel, CommonConstants.BLOCK_BYTE_LENGTH, inputs, 0, labelByteLength);
        KeyParameter key = new KeyParameter(keyBytes);
        CipherParameters withIv = new ParametersWithIV(key, ivBytes);
        streamCipher.init(false, withIv);
        byte[] outputs = new byte[labelByteLength];
        streamCipher.processBytes(inputs, 0, labelByteLength, outputs, 0);
        return outputs;
    }

    /**
     * 生成布谷鸟哈希分桶。
     *
     * @param itemList 元素列表。
     * @param binNum   指定桶数量。
     * @param hashKeys 哈希算法密钥。
     * @return 布谷鸟哈希分桶是否成功。
     */
    private boolean generateCuckooHashBin(ArrayList<ByteBuffer> itemList, int binNum, byte[][] hashKeys) {
        // 初始化布谷鸟哈希
        cuckooHashBin = createCuckooHashBin(envType, cuckooHashBinType, clientKeywordSize, binNum, hashKeys);
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
     * 返回查询关键词的编码。
     *
     * @return 查询关键词的编码。
     */
    public List<long[][]> encodeQuery() {
        int ciphertextNum = params.getBinNum() / (params.getPolyModulusDegree() / params.getItemEncodedSlotSize());
        int itemPerCiphertext = params.getPolyModulusDegree() / params.getItemEncodedSlotSize();
        long[][] items = new long[ciphertextNum][params.getPolyModulusDegree()];
        for (int i = 0; i < ciphertextNum; i++) {
            for (int j = 0; j < itemPerCiphertext; j++) {
                long[] item = params.getHashBinEntryEncodedArray(
                    cuckooHashBin.getHashBinEntry(i * itemPerCiphertext + j), true, secureRandom
                );
                System.arraycopy(item, 0, items[i], j*params.getItemEncodedSlotSize(), params.getItemEncodedSlotSize());
            }
            for (int j = itemPerCiphertext * params.getItemEncodedSlotSize(); j < params.getPolyModulusDegree(); j++) {
                items[i][j] = 0;
            }
        }
        IntStream intStream = parallel ? IntStream.range(0, ciphertextNum).parallel() : IntStream.range(0, ciphertextNum);
        return intStream
            .mapToObj(i -> computePowers(items[i], params.getPlainModulus(), params.getQueryPowers()))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 生成盲化元素。
     *
     * @param retrievalKeywordArrayList 客户端查询关键词列表。
     * @return 盲化元素。
     */
    private List<byte[]> generateBlindElements(List<ByteBuffer> retrievalKeywordArrayList) {
        Ecc ecc = EccFactory.createInstance(envType);
        BigInteger n = ecc.getN();
        inverseBetas = new BigInteger[retrievalKeywordArrayList.size()];
        IntStream batchIntStream = IntStream.range(0, retrievalKeywordArrayList.size());
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        return batchIntStream
            .mapToObj(index -> {
                // 生成盲化因子
                BigInteger beta = BigIntegerUtils.randomPositive(n, secureRandom);
                inverseBetas[index] = beta.modInverse(n);
                // hash to point
                ECPoint element = ecc.hashToCurve(retrievalKeywordArrayList.get(index).array());
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
        MpcAbortPreconditions.checkArgument(blindPrf.size() == clientKeywordArrayList.size());
        byte[][] blindPrfArray = blindPrf.toArray(new byte[0][]);
        Ecc ecc = EccFactory.createInstance(envType);
        IntStream batchIntStream = IntStream.range(0, clientKeywordSize);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        ByteBuffer[] byteBuffers = batchIntStream
            .mapToObj(index -> {
                // 解码
                ECPoint element = ecc.decode(blindPrfArray[index]);
                // 去盲化
                return ecc.multiply(element, inverseBetas[index]);
            })
            .map(element -> ecc.encode(element, true))
            .map(ByteBuffer::wrap)
            .toArray(ByteBuffer[]::new);
        return Arrays.stream(byteBuffers, 0, clientKeywordSize)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 计算幂次方。
     *
     * @param base      底数。
     * @param modulus   模数。
     * @param exponents 指数。
     * @return 幂次方。
     */
    private long[][] computePowers(long[] base, long modulus, int[] exponents) {
        Zp64 zp64 = Zp64Factory.createInstance(envType, modulus);
        long[][] result = new long[exponents.length][];
        assert exponents[0] == 1;
        result[0] = base;
        for (int i = 1; i < exponents.length; i++) {
            long[] temp = new long[base.length];
            for (int j = 0; j < base.length; j++) {
                temp[j] = zp64.mulPow(base[j], exponents[i]);
            }
            result[i] = temp;
        }
        return result;
    }
}