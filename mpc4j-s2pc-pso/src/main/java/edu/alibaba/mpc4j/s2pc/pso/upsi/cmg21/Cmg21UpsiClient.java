package edu.alibaba.mpc4j.s2pc.pso.upsi.cmg21;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.s2pc.pso.oprf.MpOprfReceiver;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pso.upsi.AbstractUpsiClient;
import edu.alibaba.mpc4j.s2pc.pso.upsi.PolynomialUtils;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * CMG21非平衡PSI协议客户端。
 *
 * @author Liqiang Peng
 * @date 2022/5/25
 */
public class Cmg21UpsiClient extends AbstractUpsiClient {
    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * 非平衡PSI方案参数
     */
    private final Cmg21UpsiParams params;
    /**
     * 无贮存区布谷鸟哈希类型
     */
    private final CuckooHashBinFactory.CuckooHashBinType cuckooHashBinType;
    /**
     * 无贮存区布谷鸟哈希分桶
     */
    private CuckooHashBin<ByteBuffer> cuckooHashBin;
    /**
     * 环境类型
     */
    private final EnvType envType;
    /**
     * MP-OPRF协议接收方
     */
    private final MpOprfReceiver oprfReceiver;

    public Cmg21UpsiClient(Rpc clientRpc, Party serverParty, Cmg21UpsiConfig config) {
        super(Cmg21UpsiPtoDesc.getInstance(), clientRpc, serverParty, config);
        this.cuckooHashBinType = config.getCuckooHashBinType();
        this.envType = config.getEnvType();
        this.params = config.getParams();
        this.oprfReceiver = OprfFactory.createMpOprfReceiver(clientRpc, serverParty, config.getMpOprfConfig());
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        oprfReceiver.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
    }

    @Override
    public void init() throws MpcAbortException {
        stopWatch.start();
        info("{}{} Client Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());
        int maxClientElementSize = CuckooHashBinFactory.getMaxItemSize(cuckooHashBinType, params.getBinNum());
        setInitInput(maxClientElementSize);
        oprfReceiver.init(maxClientElementSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Init ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Client Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public Set<ByteBuffer> psi(Set<ByteBuffer> clientElementSet, int elementByteLength) throws MpcAbortException {
        setPtoInput(clientElementSet, elementByteLength);
        info("{}{} Client begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        // 客户端执行MP-OPRF协议
        stopWatch.start();
        ArrayList<ByteBuffer> oprfOutputs = oprf();
        Map<ByteBuffer, ByteBuffer> oprfMap = IntStream.range(0, oprfOutputs.size())
            .boxed()
            .collect(Collectors.toMap(oprfOutputs::get, i -> clientElementArrayList.get(i), (a, b) -> b));
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step OPRF 1/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), oprfTime);

        // 客户端布谷鸟哈希分桶，并发送hash函数的key
        stopWatch.start();
        boolean cuckooHashSucceed;
        byte[][] hashKeys;
        do {
            hashKeys = generateHashKeys(params.getHashNum());
            cuckooHashSucceed = generateCuckooHashBin(oprfOutputs, params.getBinNum(), hashKeys);
        } while (!cuckooHashSucceed);
        DataPacketHeader hashKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Cmg21UpsiPtoDesc.PtoStep.CLIENT_SEND_CUCKOO_HASH_KEYS.ordinal(),
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(hashKeyHeader, Arrays.stream(hashKeys)
            .collect(Collectors.toCollection(ArrayList::new))));
        stopWatch.stop();
        long cuckooHashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step cuckoo hash 2/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cuckooHashTime);

        // 客户端生成BFV算法密钥和参数
        stopWatch.start();
        List<byte[]> encryptionParams = Cmg21UpsiNativeClient.genEncryptionParameters(
            params.getPolyModulusDegree(), params.getPlainModulus(), params.getCoeffModulusBits());
        DataPacketHeader encryptionParamsDataPacketHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Cmg21UpsiPtoDesc.PtoStep.CLIENT_SEND_ENCRYPTION_PARAMS.ordinal(),
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        MpcAbortPreconditions.checkArgument(encryptionParams.size() == 4);
        rpc.send(DataPacket.fromByteArrayList(encryptionParamsDataPacketHeader, encryptionParams.subList(0, 2)));
        stopWatch.stop();
        long keyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step key generation 3/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), keyGenTime);

        // 客户端加密查询信息
        stopWatch.start();
        List<long[][]> encodedQuery = encodeQuery();
        Stream<long[][]> stream = parallel ? encodedQuery.stream().parallel() : encodedQuery.stream();
        List<byte[]> queryCiphertextList = stream
            .map(i -> Cmg21UpsiNativeClient.generateQuery(
                i, encryptionParams.get(0), encryptionParams.get(2), encryptionParams.get(3)))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        DataPacketHeader clientQueryDataPacketHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Cmg21UpsiPtoDesc.PtoStep.CLIENT_SEND_QUERY.ordinal(),
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientQueryDataPacketHeader, queryCiphertextList));
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step query generation 4/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), genQueryTime);

        // 客户端接收服务端的计算结果
        info("{}{} Client receive Server's reply", ptoStepLogPrefix, getPtoDesc().getPtoName());
        DataPacketHeader serverResponseDataPacketSpec = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Cmg21UpsiPtoDesc.PtoStep.SERVER_SEND_RESPONSE.ordinal(),
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> serverResponse = rpc.receive(serverResponseDataPacketSpec).getPayload();

        // 客户端解密密文匹配结果
        stopWatch.start();
        Stream<byte[]> responseStream = parallel ? serverResponse.stream().parallel() : serverResponse.stream();
        List<long[]> decodedResponse = responseStream
            .map(i -> Cmg21UpsiNativeClient.decodeReply(i, encryptionParams.get(0), encryptionParams.get(3)))
            .collect(Collectors.toList());
        Set<ByteBuffer> intersectionSet = recoverPsiResult(decodedResponse, oprfMap);
        stopWatch.stop();
        long decodeResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step (decode response) 5/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(),
            decodeResponseTime);

        info("{}{} Client end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return intersectionSet;
    }

    /**
     * 生成哈希算法密钥。
     *
     * @param hashNum 哈希算法数量。
     * @return 哈希算法密钥。
     */
    private byte[][] generateHashKeys(int hashNum) {
        SecureRandom secureRandom = new SecureRandom();
        byte[][] seeds = new byte[hashNum][CommonConstants.BLOCK_BYTE_LENGTH];
        IntStream.range(0, seeds.length).forEach(i -> secureRandom.nextBytes(seeds[i]));
        return seeds;
    }

    /**
     * 客户端（接收方）执行MP-OPRF协议。
     *
     * @return MP-OPRF接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    private ArrayList<ByteBuffer> oprf() throws MpcAbortException {
        byte[][] oprfReceiverInputs = IntStream.range(0, clientElementSize)
            .mapToObj(i -> clientElementArrayList.get(i).array())
            .toArray(byte[][]::new);
        OprfReceiverOutput oprfReceiverOutput = oprfReceiver.oprf(oprfReceiverInputs);
        IntStream intStream = parallel ? IntStream.range(0, clientElementSize).parallel() :
            IntStream.range(0, clientElementSize);
        return intStream
            .mapToObj(i -> ByteBuffer.wrap(oprfReceiverOutput.getPrf(i)))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 恢复隐私集合交集。
     *
     * @param decryptedResponse 解密后的服务端回复。
     * @param oprfMap           OPRF映射。
     * @return 隐私集合交集。
     */
    private Set<ByteBuffer> recoverPsiResult(List<long[]> decryptedResponse, Map<ByteBuffer, ByteBuffer> oprfMap) {
        Set<ByteBuffer> intersectionSet = new HashSet<>();
        int ciphertextNum = params.getBinNum() / (params.getPolyModulusDegree() / params.getItemEncodedSlotSize());
        int itemPerCiphertext = params.getPolyModulusDegree() / params.getItemEncodedSlotSize();
        int partitionCount = decryptedResponse.size() / ciphertextNum;
        for (int i = 0; i < decryptedResponse.size(); i++) {
            // 找到匹配元素的所在行
            List<Integer> matchedItem = new ArrayList<>();
            for (int j = 0; j < params.getItemEncodedSlotSize() * itemPerCiphertext; j++) {
                if (decryptedResponse.get(i)[j] == 0) {
                    matchedItem.add(j);
                }
            }
            for (int j = 0; j < matchedItem.size() - params.getItemEncodedSlotSize() + 1; j++) {
                if (matchedItem.get(j) % params.getItemEncodedSlotSize() == 0) {
                    if (matchedItem.get(j + params.getItemEncodedSlotSize() - 1) - matchedItem.get(j)
                        == params.getItemEncodedSlotSize() - 1) {
                        int hashBinIndex = (matchedItem.get(j) / params.getItemEncodedSlotSize()) + (i / partitionCount) * itemPerCiphertext;
                        intersectionSet.add(oprfMap.get(cuckooHashBin.getHashBinEntry(hashBinIndex).getItem()));
                        j = j + params.getItemEncodedSlotSize() - 1;
                    }
                }
            }
        }
        return intersectionSet;
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
        cuckooHashBin = CuckooHashBinFactory.createCuckooHashBin(
            envType, cuckooHashBinType, clientElementSize, binNum, hashKeys
        );
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
        int itemPerCiphertext = params.getPolyModulusDegree() / params.getItemEncodedSlotSize();
        int ciphertextNum = params.getBinNum() / (params.getPolyModulusDegree() / params.getItemEncodedSlotSize());
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
            .mapToObj(i -> PolynomialUtils.computePowers(items[i], params.getPlainModulus(), params.getQueryPowers()))
            .collect(Collectors.toCollection(ArrayList::new));
    }

}
