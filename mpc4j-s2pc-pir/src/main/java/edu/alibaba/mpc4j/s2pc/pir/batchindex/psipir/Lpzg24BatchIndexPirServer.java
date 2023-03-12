package edu.alibaba.mpc4j.s2pc.pir.batchindex.psipir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.polynomial.power.PowerNode;
import edu.alibaba.mpc4j.common.tool.polynomial.power.PowerUtils;
import edu.alibaba.mpc4j.common.tool.polynomial.zp64.Zp64Poly;
import edu.alibaba.mpc4j.common.tool.polynomial.zp64.Zp64PolyFactory;
import edu.alibaba.mpc4j.common.tool.utils.*;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.AbstractBatchIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pso.upsi.cmg21.Cmg21UpsiParams;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static edu.alibaba.mpc4j.s2pc.pir.batchindex.psipir.Lpzg24BatchIndexPirPtoDesc.PtoStep.*;

/**
 * PSI-PIR协议服务端。
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class Lpzg24BatchIndexPirServer extends AbstractBatchIndexPirServer {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * 是否使用压缩编码
     */
    private final boolean compressEncode;
    /**
     * 非平衡PSI方案参数
     */
    private Cmg21UpsiParams params;
    /**
     * 哈希算法密钥
     */
    private byte[][] hashKeys;
    /**
     * SEAL上下文参数
     */
    private byte[] sealContext;
    /**
     * 重线性化密钥
     */
    private byte[] relinKeys;
    /**
     * PRF密钥
     */
    private BigInteger alpha;
    /**
     * 多项式系数
     */
    private ArrayList<long[][]> coeffs = new ArrayList<>();
    /**
     * 序列化的多项式
     */
    private ArrayList<ArrayList<byte[]>> encodedPlaintexts = new ArrayList<>();
    /**
     * 哈希桶索引对应哈希桶的元素数量
     */
    private int maxBinSize;

    public Lpzg24BatchIndexPirServer(Rpc serverRpc, Party clientParty, Lpzg24BatchIndexPirConfig config) {
        super(Lpzg24BatchIndexPirPtoDesc.getInstance(), serverRpc, clientParty, config);
        compressEncode = config.getCompressEncode();
    }

    @Override
    public void init(ArrayList<ByteBuffer> elementArrayList, int elementBitLength, int maxRetrievalSize) throws MpcAbortException {
        setInitInput(elementArrayList, maxRetrievalSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        params = Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_2K_CMP;

        DataPacketHeader bfvParamsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), CLIENT_SEND_ENCRYPTION_PARAMS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> bfvKeyPair = rpc.receive(bfvParamsHeader).getPayload();
        MpcAbortPreconditions.checkArgument(bfvKeyPair.size() == 2);
        sealContext = bfvKeyPair.remove(0);
        relinKeys = bfvKeyPair.remove(0);

        stopWatch.start();
        // 服务端生成并发送哈希密钥
        hashKeys = CommonUtils.generateRandomKeys(params.getCuckooHashKeyNum(), secureRandom);
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = Arrays.stream(hashKeys).collect(Collectors.toList());
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
        stopWatch.stop();
        long cuckooHashKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 4, cuckooHashKeyTime, "Server generates cuckoo hash keys");

        stopWatch.start();
        // 计算PRF
        ArrayList<ByteBuffer> elementPrf = computeElementPrf();
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 4, oprfTime, "Server computes PRFs");

        stopWatch.start();
        // 服务端哈希分桶
        maxBinSize = getMaxBinSize();
        ArrayList<ArrayList<HashBinEntry<ByteBuffer>>> hashBins = generateCompleteHashBin(elementPrf);
        stopWatch.stop();
        long hashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 4, hashTime, "Server bin-hashes key");

        stopWatch.start();
        // 计算多项式系数
        coeffs = encodeDatabase(hashBins);
        IntStream intStream = IntStream.range(0, coeffs.size());
        intStream = parallel ? intStream.parallel() : intStream;
        encodedPlaintexts = intStream
            .mapToObj(i ->
                Lpzg24BatchIndexPirNativeUtils.processDatabase(sealContext, coeffs.get(i), params.getPsLowDegree())
            )
            .collect(Collectors.toCollection(ArrayList::new));
        stopWatch.stop();
        long encodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 4, 4, encodeTime, "Server encodes label");

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void pir() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // 服务端执行OPRF协议
        DataPacketHeader blindHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), CLIENT_SEND_BLIND.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> blindPayload = rpc.receive(blindHeader).getPayload();
        List<byte[]> blindPrfPayload = handleBlindPayload(blindPayload);
        DataPacketHeader blindPrfHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), SERVER_SEND_BLIND_PRF.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(blindPrfHeader, blindPrfPayload));
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, oprfTime, "Server runs OPRFs");

        // 接收客户端的加密查询信息
        DataPacketHeader queryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        ArrayList<byte[]> queryPayload = new ArrayList<>(rpc.receive(queryHeader).getPayload());
        int ciphertextNumber = params.getBinNum() / (params.getPolyModulusDegree() / params.getItemEncodedSlotSize());
        MpcAbortPreconditions.checkArgument(
            queryPayload.size() == ciphertextNumber * params.getQueryPowers().length,
            "The size of query is incorrect"
        );

        stopWatch.start();
        // 密文多项式运算
        List<byte[]> response = computeResponse(queryPayload);
        DataPacketHeader keywordResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(keywordResponseHeader, response));
        stopWatch.stop();
        long replyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, replyTime, "Server generates reply");

        logPhaseInfo(PtoState.PTO_END);
    }

    @Override
    protected void setInitInput(ArrayList<ByteBuffer> elementArrayList, int maxRetrievalSize)
        throws MpcAbortException {
        this.serverElementSize = elementArrayList.size();
        for (int i = 0; i < serverElementSize; i++) {
            byte[] element = elementArrayList.get(i).array();
            boolean value = BinaryUtils.getBoolean(element, element.length * Byte.SIZE - 1);
            if (value) {
                elementByteArray.add(IntUtils.intToByteArray(i));
            }
        }
        this.maxRetrievalSize = maxRetrievalSize;
        initState();
    }

    /**
     * 返回编码后的数据库。
     *
     * @param hashBins 哈希分桶。
     * @return 编码后的数据库（明文多项式的系数）。
     */
    private ArrayList<long[][]> encodeDatabase(ArrayList<ArrayList<HashBinEntry<ByteBuffer>>> hashBins) {
        Zp64Poly zp64Poly = Zp64PolyFactory.createInstance(envType, (long) params.getPlainModulus());
        int itemEncodedSlotSize = params.getItemEncodedSlotSize();
        int itemPerCiphertext = params.getPolyModulusDegree() / itemEncodedSlotSize;
        int ciphertextNum = params.getBinNum() / itemPerCiphertext;
        // we will split the hash table into partitions
        int partitionNum = (maxBinSize + params.getMaxPartitionSizePerBin() - 1) / params.getMaxPartitionSizePerBin();
        int bigPartitionIndex = maxBinSize / params.getMaxPartitionSizePerBin();
        long[][] coeffs = new long[itemEncodedSlotSize * itemPerCiphertext][];
        ArrayList<long[][]> coeffsPolys = new ArrayList<>();
        long[][] encodedItemArray = new long[params.getBinNum() * itemEncodedSlotSize][maxBinSize];
        for (int i = 0; i < params.getBinNum(); i++) {
            IntStream intStream = parallel ? IntStream.range(0, maxBinSize).parallel() : IntStream.range(0, maxBinSize);
            int finalI = i;
            intStream.forEach(j -> {
                long[] item = params.getHashBinEntryEncodedArray(hashBins.get(finalI).get(j), false);
                for (int l = 0; l < itemEncodedSlotSize; l++) {
                    encodedItemArray[finalI * itemEncodedSlotSize + l][j] = item[l];
                }
            });
        }
        // for each bucket, compute the coefficients of the polynomial f(x) = \prod_{y in bucket} (x - y)
        for (int i = 0; i < ciphertextNum; i++) {
            for (int partition = 0; partition < partitionNum; partition++) {
                int partitionSize, partitionStart;
                partitionSize = partition < bigPartitionIndex ?
                    params.getMaxPartitionSizePerBin() : maxBinSize % params.getMaxPartitionSizePerBin();
                partitionStart = params.getMaxPartitionSizePerBin() * partition;
                IntStream intStream = IntStream.range(0, itemPerCiphertext * itemEncodedSlotSize);
                intStream = parallel ? intStream.parallel() : intStream;
                int finalI = i;
                intStream.forEach(j -> {
                    long[] tempVector = new long[partitionSize];
                    System.arraycopy(
                        encodedItemArray[finalI * itemPerCiphertext * itemEncodedSlotSize + j],
                        partitionStart,
                        tempVector,
                        0,
                        partitionSize
                    );
                    coeffs[j] = zp64Poly.rootInterpolate(partitionSize, tempVector, 0L);
                });
                // 转换为列编码
                long[][] temp = new long[partitionSize + 1][params.getPolyModulusDegree()];
                for (int j = 0; j < partitionSize + 1; j++) {
                    for (int l = 0; l < itemPerCiphertext * itemEncodedSlotSize; l++) {
                        temp[j][l] = coeffs[l][j];
                    }
                    for (int l = itemPerCiphertext * itemEncodedSlotSize; l < params.getPolyModulusDegree(); l++) {
                        temp[j][l] = 0;
                    }
                }
                coeffsPolys.add(temp);
            }
        }
        return coeffsPolys;
    }

    /**
     * 服务端计算元素PRF。
     *
     * @return 元素PRF。
     */
    private ArrayList<ByteBuffer> computeElementPrf() {
        Ecc ecc = EccFactory.createInstance(envType);
        Kdf kdf = KdfFactory.createInstance(envType);
        Prg prg = PrgFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH * 2);
        alpha = BigIntegerUtils.randomPositive(ecc.getN(), secureRandom);
        Stream<byte[]> stream = elementByteArray.stream();
        stream = parallel ? stream.parallel() : stream;
        return stream
            .map(ecc::hashToCurve)
            .map(hash -> ecc.multiply(hash, alpha))
            .map(prf -> ecc.encode(prf, false))
            .map(kdf::deriveKey)
            .map(prg::extendToBytes)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 生成完全哈希分桶。
     *
     * @param elementList 元素集合。
     * @return 完全哈希分桶。
     */
    private ArrayList<ArrayList<HashBinEntry<ByteBuffer>>> generateCompleteHashBin(ArrayList<ByteBuffer> elementList) {
        RandomPadHashBin<ByteBuffer> completeHash = new RandomPadHashBin<>(
            envType, params.getBinNum(), serverElementSize, hashKeys
        );
        completeHash.insertItems(elementList);
        ArrayList<ArrayList<HashBinEntry<ByteBuffer>>> completeHashBins = new ArrayList<>();
        HashBinEntry<ByteBuffer> paddingEntry = HashBinEntry.fromEmptyItem(botElementByteBuffer);
        for (int i = 0; i < completeHash.binNum(); i++) {
            ArrayList<HashBinEntry<ByteBuffer>> binItems = new ArrayList<>(completeHash.getBin(i));
            int paddingNum = maxBinSize - completeHash.binSize(i);
            IntStream.range(0, paddingNum).mapToObj(j -> paddingEntry).forEach(binItems::add);
            completeHashBins.add(binItems);
        }
        return completeHashBins;
    }

    /**
     * 返回哈希桶索引对应哈希桶的元素数量。
     *
     * @return 哈希桶索引对应哈希桶的元素数量。
     */
    private int getMaxBinSize() {
        Ecc ecc = EccFactory.createInstance(envType);
        Kdf kdf = KdfFactory.createInstance(envType);
        Prg prg = PrgFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH * 2);
        IntStream stream = IntStream.range(0, serverElementSize);
        stream = parallel ? stream.parallel() : stream;
        ArrayList<ByteBuffer> elementList = stream
            .mapToObj(i -> ecc.hashToCurve(IntUtils.intToByteArray(i)))
            .map(ecPoint -> ecc.encode(ecPoint.multiply(alpha), false))
            .map(kdf::deriveKey)
            .map(prg::extendToBytes)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toCollection(ArrayList::new));
        RandomPadHashBin<ByteBuffer> completeHash = new RandomPadHashBin<>(
            envType, params.getBinNum(), serverElementSize, hashKeys
        );
        completeHash.insertItems(elementList);
        int maxBinSize = completeHash.binSize(0);
        for (int i = 1; i < completeHash.binNum(); i++) {
            if (completeHash.binSize(i) > maxBinSize) {
                maxBinSize = completeHash.binSize(i);
            }
        }
        return maxBinSize;
    }

    /**
     * 处理盲化元素。
     *
     * @param blindElements 盲化元素。
     * @return 盲化元素PRF。
     * @throws MpcAbortException 如果协议异常中止。
     */
    private List<byte[]> handleBlindPayload(List<byte[]> blindElements) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(blindElements.size() > 0);
        Ecc ecc = EccFactory.createInstance(envType);
        Stream<byte[]> blindStream = blindElements.stream();
        blindStream = parallel ? blindStream.parallel() : blindStream;
        return blindStream
            // 解码H(m_c)^β
            .map(ecc::decode)
            // 计算H(m_c)^βα
            .map(element -> ecc.multiply(element, alpha))
            // 编码
            .map(element -> ecc.encode(element, compressEncode))
            .collect(Collectors.toList());
    }

    /**
     * 服务端计算密文匹配结果。
     *
     * @param ciphertextPoly   密文多项式。
     * @return 密文匹配结果。
     * @throws MpcAbortException 如果协议异常中止。
     */
    private List<byte[]> computeResponse(ArrayList<byte[]> ciphertextPoly) throws MpcAbortException {
        int ciphertextNum = params.getBinNum() / (params.getPolyModulusDegree() / params.getItemEncodedSlotSize());
        int partitionCount = (maxBinSize + params.getMaxPartitionSizePerBin() - 1) / params.getMaxPartitionSizePerBin();
        // 计算所有的密文次方
        int[][] powerDegree;
        if (params.getPsLowDegree() > 0) {
            Set<Integer> innerPowersSet = new HashSet<>();
            Set<Integer> outerPowersSet = new HashSet<>();
            IntStream.range(0, params.getQueryPowers().length).forEach(i -> {
                if (params.getQueryPowers()[i] <= params.getPsLowDegree()) {
                    innerPowersSet.add(params.getQueryPowers()[i]);
                } else {
                    outerPowersSet.add(params.getQueryPowers()[i] / (params.getPsLowDegree() + 1));
                }
            });
            PowerNode[] innerPowerNodes = PowerUtils.computePowers(innerPowersSet, params.getPsLowDegree());
            PowerNode[] outerPowerNodes = PowerUtils.computePowers(
                outerPowersSet, params.getMaxPartitionSizePerBin() / (params.getPsLowDegree() + 1));
            powerDegree = new int[innerPowerNodes.length + outerPowerNodes.length][2];
            int[][] innerPowerNodesDegree = Arrays.stream(innerPowerNodes).map(PowerNode::toIntArray).toArray(int[][]::new);
            int[][] outerPowerNodesDegree = Arrays.stream(outerPowerNodes).map(PowerNode::toIntArray).toArray(int[][]::new);
            System.arraycopy(innerPowerNodesDegree, 0, powerDegree, 0, innerPowerNodesDegree.length);
            System.arraycopy(outerPowerNodesDegree, 0, powerDegree, innerPowerNodesDegree.length, outerPowerNodesDegree.length);
        } else {
            Set<Integer> sourcePowersSet = Arrays.stream(params.getQueryPowers())
                .boxed()
                .collect(Collectors.toCollection(HashSet::new));
            PowerNode[] powerNodes = PowerUtils.computePowers(sourcePowersSet, params.getMaxPartitionSizePerBin());
            powerDegree = Arrays.stream(powerNodes).map(PowerNode::toIntArray).toArray(int[][]::new);
        }
        IntStream intStream = IntStream.range(0, ciphertextNum);
        intStream = parallel ? intStream.parallel() : intStream;
        List<byte[]> queryPowers = intStream
            .mapToObj(i -> Lpzg24BatchIndexPirNativeUtils.computeEncryptedPowers(
                sealContext,
                relinKeys,
                ciphertextPoly.subList(i * params.getQueryPowers().length, (i + 1) * params.getQueryPowers().length),
                powerDegree,
                params.getQueryPowers(),
                params.getPsLowDegree())
            )
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(ArrayList::new));
        if (params.getPsLowDegree() > 0) {
            return IntStream.range(0, ciphertextNum)
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount).parallel() : IntStream.range(0, partitionCount))
                        .mapToObj(j -> Lpzg24BatchIndexPirNativeUtils.optComputeMatches(
                            sealContext,
                            relinKeys,
                            encodedPlaintexts.get(i * partitionCount + j),
                            // plaintextPoly.get(i * partitionCount + j),
                            queryPowers.subList(i * powerDegree.length, (i + 1) * powerDegree.length),
                            params.getPsLowDegree())
                        )
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        } else if (params.getPsLowDegree() == 0) {
            return IntStream.range(0, ciphertextNum)
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount).parallel() : IntStream.range(0, partitionCount))
                        .mapToObj(j -> Lpzg24BatchIndexPirNativeUtils.naiveComputeMatches(
                                sealContext,
                                encodedPlaintexts.get(i * partitionCount + j),
                                // plaintextPoly.get(i * partitionCount + j),
                                queryPowers.subList(i * powerDegree.length, (i + 1) * powerDegree.length)
                            )
                        )
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        } else {
            throw new MpcAbortException("ps_low_degree参数设置不正确");
        }
    }
}
