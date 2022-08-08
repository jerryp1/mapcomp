package edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21;

import com.google.common.primitives.Bytes;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
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
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pir.keyword.AbstractKwPirServer;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirParams;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirPtoDesc.PtoStep;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.OFBBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * CMG21关键词索引PIR协议服务端。
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public class Cmg21KwPirServer<T> extends AbstractKwPirServer<T> {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * 是否使用压缩编码
     */
    private final boolean compressEncode;
    /**
     * 关键词索引PIR方案参数
     */
    private Cmg21KwPirParams params;
    /**
     * 哈希分桶
     */
    private List<ArrayList<HashBinEntry<ByteBuffer>>> hashBins;
    /**
     * 服务端元素编码
     */
    private long[][][] serverKeywordEncode;
    /**
     * 服务端标签编码
     */
    private long[][][] serverLabelEncode;
    /**
     * PRF密钥
     */
    private BigInteger alpha;
    /**
     * 关键词回复
     */
    private ArrayList<byte[]> keywordResponsePayload;
    /**
     * 标签回复
     */
    private ArrayList<byte[]> labelResponsePayload;
    /**
     * 哈希算法密钥
     */
    private byte[][] hashKeys;

    public Cmg21KwPirServer(Rpc serverRpc, Party clientParty, Cmg21KwPirConfig config) {
        super(Cmg21KwPirPtoDesc.getInstance(), serverRpc, clientParty, config);
        compressEncode = config.getCompressEncode();
    }

    @Override
    public void init(KwPirParams kwPirParams, Map<T, ByteBuffer> serverKeywordLabelMap, int labelByteLength) {
        setInitInput(serverKeywordLabelMap, labelByteLength);
        info("{}{} Server Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        assert (kwPirParams instanceof Cmg21KwPirParams);
        params = (Cmg21KwPirParams) kwPirParams;
        // 服务端生成并发送哈希密钥
        hashKeys = CommonUtils.generateRandomKeys(params.getCuckooHashKeyNum(), secureRandom);
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = Arrays.stream(hashKeys).collect(Collectors.toList());
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
        stopWatch.stop();
        long cuckooHashKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Init Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cuckooHashKeyTime);

        stopWatch.start();
        // 计算PRF
        ArrayList<ByteBuffer> keywordPrfs = computeKeywordPrf();
        Map<ByteBuffer, ByteBuffer> prfLabelMap = IntStream.range(0, keywordSize)
            .boxed()
            .collect(Collectors.toMap(
                keywordPrfs::get,
                i -> serverKeywordLabelMap.get(byteArrayObjectMap.get(keywordArrayList.get(i))),
                (a, b) -> b)
            );
        // 计算完全哈希分桶
        hashBins = generateCompleteHashBin(keywordPrfs, params.getBinNum());
        // 计算多项式系数
        encodeDatabase(prfLabelMap);
        stopWatch.stop();
        long encodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Init Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), encodeTime);

        initialized = true;
        info("{}{} Server Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public void pir() throws MpcAbortException {
        setPtoInput();
        info("{}{} Server begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 服务端执行OPRF协议
        DataPacketHeader blindHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_BLIND.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> blindPayload = rpc.receive(blindHeader).getPayload();
        List<byte[]> blindPrfPayload = handleBlindPayload(blindPayload);
        DataPacketHeader blindPrfHeader = new DataPacketHeader(
            taskId, ptoDesc.getPtoId(), PtoStep.SERVER_SEND_BLIND_PRF.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(blindPrfHeader, blindPrfPayload));
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 1/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), oprfTime);

        stopWatch.start();
        // 接收客户端布谷鸟哈希分桶结果
        DataPacketHeader cuckooHashResultHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_CUCKOO_HASH_RESULT.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> cuckooHashResultPayload = rpc.receive(cuckooHashResultHeader).getPayload();
        handleCuckooHashResultPayload(cuckooHashResultPayload);
        stopWatch.stop();
        long cuckooHashResultTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 2/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cuckooHashResultTime);

        stopWatch.start();
        // 接收客户端的加密密钥
        DataPacketHeader fheParamsHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_FHE_PARAMS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> fheParamsPayload = rpc.receive(fheParamsHeader).getPayload();
        MpcAbortPreconditions.checkArgument(
            fheParamsPayload.size() == 3, "Failed to receive BFV encryption parameters"
        );
        stopWatch.stop();
        long fheParamsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 3/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), fheParamsTime);

        stopWatch.start();
        // 接收客户端的加密查询信息
        DataPacketHeader queryHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        ArrayList<byte[]> queryPayload = new ArrayList<>(rpc.receive(queryHeader).getPayload());
        int ciphertextNumber = params.getBinNum() / (params.getPolyModulusDegree() / params.getItemEncodedSlotSize());
        MpcAbortPreconditions.checkArgument(
            queryPayload.size() == ciphertextNumber * params.getQueryPowers().length,
            "The size of query is incorrect"
        );
        stopWatch.stop();
        long queryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 4/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), queryTime);

        stopWatch.start();
        // 密文多项式运算
        computeResponse(queryPayload, fheParamsPayload);
        DataPacketHeader keywordResponseHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_ITEM_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(keywordResponseHeader, keywordResponsePayload));
        DataPacketHeader labelResponseHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_LABEL_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(labelResponseHeader, labelResponsePayload));
        stopWatch.stop();
        long replyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 5/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), replyTime);

        info("{}{} Server end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    /**
     * 哈希桶内元素的排序。
     *
     * @param binItems 哈希桶内的元素。
     * @return 排序后的哈希桶。
     * @throws MpcAbortException 如果协议异常中止。
     */
    private ArrayList<HashBinEntry<ByteBuffer>> sortedHashBinEntries(ArrayList<HashBinEntry<ByteBuffer>> binItems)
        throws MpcAbortException {
        ArrayList<ArrayList<HashBinEntry<ByteBuffer>>> partitions = new ArrayList<>(binItems.size());
        List<List<Set<Long>>> partElementSet = new ArrayList<>();
        for (int i = 0; i < binItems.size(); i++) {
            partElementSet.add(i, new ArrayList<>());
            for (int j = 0; j < params.getItemEncodedSlotSize(); j++) {
                partElementSet.get(i).add(j, new HashSet<>());
            }
        }
        for (int i = 0; i < binItems.size(); i++) {
            partitions.add(new ArrayList<>());
        }
        int shiftBits = BigInteger.valueOf(params.getPlainModulus()).bitLength() - 1;
        for (HashBinEntry<ByteBuffer> binItem : binItems) {
            long[] itemParts = new long[params.getItemEncodedSlotSize()];
            BigInteger item = BigIntegerUtils.byteArrayToBigInteger(binItem.getItem().array());
            item = item.mod(BigInteger.ONE.shiftLeft(CommonConstants.BLOCK_BIT_LENGTH));
            for (int i = 0; i < params.getItemEncodedSlotSize(); i++) {
                itemParts[i] = item.mod(BigInteger.ONE.shiftLeft(shiftBits)).longValueExact();
                item = item.shiftRight(shiftBits);
            }
            for (int i = 0; i < partitions.size(); i++) {
                ArrayList<HashBinEntry<ByteBuffer>> partition = partitions.get(i);
                if (partition.size() == 0) {
                    partition.add(binItem);
                    for (int j = 0; j < params.getItemEncodedSlotSize(); j++) {
                        partElementSet.get(i).get(j).add(itemParts[j]);
                    }
                    break;
                } else {
                    if (partition.size() != params.getMaxPartitionSizePerBin()) {
                        int l;
                        for (l = 0; l < partition.size(); l++) {
                            if (checkRepeatedItemPart(partElementSet.get(i), itemParts)) {
                                break;
                            }
                        }
                        if (l == partition.size()) {
                            partition.add(binItem);
                            for (int j = 0; j < params.getItemEncodedSlotSize(); j++) {
                                partElementSet.get(i).get(j).add(itemParts[j]);
                            }
                            break;
                        }
                    }
                }
            }
        }
        ArrayList<HashBinEntry<ByteBuffer>> sortedHashBin = new ArrayList<>();
        partitions.stream()
            .filter(partition -> partition.size() != 0)
            .forEach(partition -> {
                sortedHashBin.addAll(partition);
                IntStream.range(partition.size(), params.getMaxPartitionSizePerBin())
                    .mapToObj(j -> HashBinEntry.fromEmptyItem(botElementByteBuffer))
                    .forEach(sortedHashBin::add);
            });
        return sortedHashBin;
    }

    /**
     * 生成完全哈希分桶。
     *
     * @param itemList 元素集合。
     * @param binNum      哈希桶数量。
     * @return 完全哈希分桶。
     */
    private List<ArrayList<HashBinEntry<ByteBuffer>>> generateCompleteHashBin(ArrayList<ByteBuffer> itemList,
                                                                          int binNum) {
        RandomPadHashBin<ByteBuffer> completeHash = new RandomPadHashBin<>(envType, binNum, keywordSize, hashKeys);
        completeHash.insertItems(itemList);
        List<ArrayList<HashBinEntry<ByteBuffer>>> hashBinList = new ArrayList<>();
        // 对哈希桶内的元素排序，保证同一个分块内不存在部分比特位相同的两个元素
        IntStream.range(0, completeHash.binNum())
            .mapToObj(i -> new ArrayList<>(completeHash.getBin(i)))
            .forEach(binItems -> {
                try {
                    hashBinList.add(sortedHashBinEntries(binItems));
                } catch (MpcAbortException e) {
                    e.printStackTrace();
                }
            });
        int maxBinSize = hashBinList.get(0).size();
        for (int i = 1; i < hashBinList.size(); i++) {
            if (hashBinList.get(i).size() > maxBinSize) {
                maxBinSize = hashBinList.get(i).size();
            }
        }
        HashBinEntry<ByteBuffer> paddingEntry = HashBinEntry.fromEmptyItem(botElementByteBuffer);
        for (ArrayList<HashBinEntry<ByteBuffer>> hashBin : hashBinList) {
            int paddingNum = maxBinSize - hashBin.size();
            IntStream.range(0, paddingNum).mapToObj(j -> paddingEntry).forEach(hashBin::add);
        }
        return hashBinList;
    }

    /**
     * 检查元素分块后，是否与已有的分块重复。
     *
     * @param existingItemParts 已有的元素分块。
     * @param itemParts         元素分块。
     * @return 是否与已有的分块重复。
     * @throws MpcAbortException 如果协议异常中止。
     */
    private boolean checkRepeatedItemPart(List<Set<Long>> existingItemParts, long[] itemParts)
        throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(existingItemParts.size() == itemParts.length);
        return IntStream.range(0, itemParts.length).anyMatch(i -> existingItemParts.get(i).contains(itemParts[i]));
    }

    /**
     * 预计算数据库编码后的多项式系数。
     *
     * @param prfMap prf映射。
     */
    private void encodeDatabase(Map<ByteBuffer, ByteBuffer> prfMap) {
        Zp64Poly zp64Poly = Zp64PolyFactory.createInstance(envType, (long) params.getPlainModulus());
        int ciphertextNum = params.getBinNum() / (params.getPolyModulusDegree() / params.getItemEncodedSlotSize());
        int itemPerCiphertext = params.getPolyModulusDegree() / params.getItemEncodedSlotSize();
        int binSize = hashBins.get(0).size();
        int partitionCount = (binSize + params.getMaxPartitionSizePerBin() - 1) / params.getMaxPartitionSizePerBin();
        int bigPartitionCount = binSize / params.getMaxPartitionSizePerBin();
        int labelPartitionCount = (int) Math.ceil((labelByteLength + CommonConstants.BLOCK_BYTE_LENGTH) * 8.0 /
            ((BigInteger.valueOf(params.getPlainModulus()).bitLength() - 1) * params.getItemEncodedSlotSize()));
        serverKeywordEncode = new long[partitionCount * ciphertextNum][][];
        serverLabelEncode = new long[partitionCount * ciphertextNum * labelPartitionCount][][];
        // for each bucket, compute the coefficients of the polynomial f(x) = \prod_{y in bucket} (x - y)
        // and coeffs of g(x), which has the property g(y) = label(y) for each y in bucket.
        for (int i = 0; i < ciphertextNum; i++) {
            for (int partition = 0; partition < partitionCount; partition++) {
                // 元素的多项式系数
                long[][] fCoeffs = new long[itemPerCiphertext * params.getItemEncodedSlotSize()][];
                // 标签的多项式系数
                long[][][] gCoeffs = new long[labelPartitionCount][itemPerCiphertext * params.getItemEncodedSlotSize()][];
                // 对分块内的元素和标签编码
                int partitionSize, partitionStart;
                partitionSize = partition < bigPartitionCount ? params.getMaxPartitionSizePerBin() : binSize % params.getMaxPartitionSizePerBin();
                partitionStart = params.getMaxPartitionSizePerBin() * partition;
                IntStream intStream = getParallel() ? IntStream.range(0, itemPerCiphertext).parallel() :
                    IntStream.range(0, itemPerCiphertext);
                int finalI = i;
                intStream.forEach(j -> {
                    // 存储每块的元素编码
                    Vector<Vector<Long>> currentBucketElement = new Vector<>();
                    currentBucketElement.setSize(params.getItemEncodedSlotSize());
                    IntStream.range(0, params.getItemEncodedSlotSize()).forEach(l -> currentBucketElement.set(l, new Vector<>()));
                    // 存储每块的标签编码
                    Vector<Vector<Vector<Long>>> currentBucketLabels = new Vector<>();
                    currentBucketLabels.setSize(labelPartitionCount);
                    for (int l = 0; l < labelPartitionCount; l++) {
                        currentBucketLabels.set(l, new Vector<>());
                        currentBucketLabels.get(l).setSize(params.getItemEncodedSlotSize());
                        for (int k = 0; k < params.getItemEncodedSlotSize(); k++) {
                            currentBucketLabels.get(l).set(k, new Vector<>());
                        }
                    }
                    IntStream.range(0, params.getItemEncodedSlotSize()).forEach(l -> currentBucketElement.get(l).setSize(partitionSize));
                    for (int l = 0; l < labelPartitionCount; l++) {
                        for (int k = 0; k < params.getItemEncodedSlotSize(); k++) {
                            currentBucketLabels.get(l).get(k).setSize(partitionSize);
                        }
                    }
                    for (int l = 0; l < partitionSize; l++) {
                        HashBinEntry<ByteBuffer> entry = hashBins.get(finalI*itemPerCiphertext + j).get(partitionStart + l);
                        long[] temp = params.getHashBinEntryEncodedArray(entry, false, secureRandom);
                        for (int k = 0; k < params.getItemEncodedSlotSize(); k++) {
                            currentBucketElement.get(k).set(l, temp[k]);
                        }
                    }
                    for (int l = 0; l < params.getItemEncodedSlotSize(); l++) {
                        long[] temp = new long[partitionSize];
                        for (int index = 0; index < partitionSize; index++) {
                            temp[index] = currentBucketElement.get(l).get(index);
                        }
                        fCoeffs[j * params.getItemEncodedSlotSize() + l] = zp64Poly.rootInterpolate(partitionSize, temp, 0L);
                        assert fCoeffs[j * params.getItemEncodedSlotSize() + l].length == partitionSize + 1;
                    }
                    int nonEmptyBuckets = 0;
                    for (int l = 0; l < partitionSize; l++) {
                        HashBinEntry<ByteBuffer> entry = hashBins.get(finalI*itemPerCiphertext + j).get(partitionStart + l);
                        if (entry.getHashIndex() != -1) {
                            for (int k = 0; k < params.getItemEncodedSlotSize(); k++) {
                                currentBucketElement.get(k).set(nonEmptyBuckets, currentBucketElement.get(k).get(l));
                            }
                            byte[] keyBytes = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                            IntStream.range(0, CommonConstants.BLOCK_BYTE_LENGTH)
                                .forEach(k -> keyBytes[k] = entry.getItem().array()[k]);
                            byte[] encryptedLabel = labelEncryption(keyBytes,
                                prfMap.get(entry.getItem()).array());
                            long[][] temp = params.encodeLabel(encryptedLabel, labelPartitionCount);
                            for (int k = 0; k < labelPartitionCount; k++) {
                                for (int h = 0; h < params.getItemEncodedSlotSize(); h++) {
                                    currentBucketLabels.get(k).get(h).set(nonEmptyBuckets, temp[k][h]);
                                }
                            }
                            nonEmptyBuckets++;
                        }
                    }
                    for (int l = 0; l < params.getItemEncodedSlotSize(); l++) {
                        currentBucketElement.get(l).setSize(nonEmptyBuckets);
                        for (int k = 0; k < labelPartitionCount; k++) {
                            currentBucketLabels.get(k).get(l).setSize(nonEmptyBuckets);
                            long[] xArray = new long[nonEmptyBuckets];
                            long[] yArray = new long[nonEmptyBuckets];
                            for (int index = 0; index < nonEmptyBuckets; index++) {
                                xArray[index] = currentBucketElement.get(l).get(index);
                                yArray[index] = currentBucketLabels.get(k).get(l).get(index);
                            }
                            if (nonEmptyBuckets > 0) {
                                gCoeffs[k][j * params.getItemEncodedSlotSize() + l] = zp64Poly.interpolate(
                                    nonEmptyBuckets, xArray, yArray
                                );
                            } else {
                                gCoeffs[k][j * params.getItemEncodedSlotSize() + l] = new long[0];
                            }
                        }
                    }
                });
                long[][] encodeElementVector = new long[partitionSize + 1][params.getPolyModulusDegree()];
                int labelSize = IntStream.range(1, gCoeffs[0].length)
                    .map(j -> gCoeffs[0][j].length).filter(j -> j >= 1)
                    .max()
                    .orElse(1);
                long[][][] encodeLabelVector = new long[labelPartitionCount][labelSize][params.getPolyModulusDegree()];
                for (int j = 0; j < partitionSize + 1; j++) {
                    // encode the jth coefficients of all polynomials into a vector
                    for (int l = 0; l < params.getItemEncodedSlotSize() * itemPerCiphertext; l++) {
                        encodeElementVector[j][l] = fCoeffs[l][j];
                    }
                    for (int l = params.getItemEncodedSlotSize() * itemPerCiphertext; l < params.getPolyModulusDegree();
                         l++) {
                        encodeElementVector[j][l] = 0;
                    }
                }
                for (int j = 0; j < labelSize; j++) {
                    for (int k = 0; k < labelPartitionCount; k++) {
                        for (int l = 0; l < params.getItemEncodedSlotSize() * itemPerCiphertext; l++) {
                            if (gCoeffs[k][l].length == 0) {
                                encodeLabelVector[k][j][l] = Math.abs(secureRandom.nextLong()) % params.getPlainModulus();
                            } else {
                                encodeLabelVector[k][j][l] = (j < gCoeffs[k][l].length) ? gCoeffs[k][l][j] : 0;
                            }
                        }
                        for (int l = params.getItemEncodedSlotSize() * itemPerCiphertext; l < params.getPolyModulusDegree();
                             l++) {
                            encodeLabelVector[k][j][l] = 0;
                        }
                    }
                }
                serverKeywordEncode[partition + i * partitionCount] = encodeElementVector;
                for (int j = 0; j < labelPartitionCount; j++) {
                    serverLabelEncode[j + partition * labelPartitionCount + i * partitionCount * labelPartitionCount] =
                        encodeLabelVector[j];
                }
            }
        }
    }

    /**
     * 计算标签密文。
     *
     * @param keyBytes   密钥字节。
     * @param labelBytes 标签字节。
     * @return 标签密文。
     */
    private byte[] labelEncryption(byte[] keyBytes, byte[] labelBytes) {
        BlockCipher blockCipher = new AESEngine();
        StreamCipher streamCipher = new OFBBlockCipher(blockCipher, 8 * CommonConstants.BLOCK_BYTE_LENGTH);
        byte[] ivBytes = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(ivBytes);
        KeyParameter key = new KeyParameter(keyBytes);
        CipherParameters withIv = new ParametersWithIV(key, ivBytes);
        streamCipher.init(true, withIv);
        byte[] outputs = new byte[labelByteLength];
        streamCipher.processBytes(labelBytes, 0, labelByteLength, outputs, 0);
        return Bytes.concat(ivBytes, outputs);
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

    private void handleCuckooHashResultPayload(List<byte[]> cuckooHashResultPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(cuckooHashResultPayload.size() == 1);
        BigInteger cuckooHashResult = BigIntegerUtils.byteArrayToBigInteger(cuckooHashResultPayload.get(0));
        MpcAbortPreconditions.checkArgument(cuckooHashResult.equals(BigInteger.ONE), "cuckoo hash failed.");
    }

    /**
     * 服务端计算密文多项式结果。
     *
     * @param encryptedQueryList   加密查询列表。
     * @param encryptionParamsList 加密方案参数列表。
     * @throws MpcAbortException 如果协议异常中止。
     */
    private void computeResponse(ArrayList<byte[]> encryptedQueryList, List<byte[]> encryptionParamsList)
        throws MpcAbortException {
        int binSize = hashBins.get(0).size();
        int partitionCount = (binSize + params.getMaxPartitionSizePerBin() - 1) / params.getMaxPartitionSizePerBin();
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
        int ciphertextNum = params.getBinNum() / (params.getPolyModulusDegree() / params.getItemEncodedSlotSize());
        int labelPartitionCount = (int) Math.ceil((labelByteLength + CommonConstants.BLOCK_BYTE_LENGTH) * 8.0 /
            ((BigInteger.valueOf(params.getPlainModulus()).bitLength() - 1) * params.getItemEncodedSlotSize()));
        IntStream queryIntStream = parallel ?
            IntStream.range(0, ciphertextNum).parallel() : IntStream.range(0, ciphertextNum);
        ArrayList<byte[]> queryPowers = queryIntStream
            .mapToObj(i -> Cmg21KwPirNativeServer.computeEncryptedPowers(
                encryptionParamsList.get(0),
                encryptionParamsList.get(1),
                encryptedQueryList.subList(i * params.getQueryPowers().length, (i + 1) * params.getQueryPowers().length),
                powerDegree,
                params.getQueryPowers(),
                params.getPsLowDegree()))
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(ArrayList::new));
        if (params.getPsLowDegree() > 0) {
            keywordResponsePayload = (parallel ? IntStream.range(0, ciphertextNum).parallel() : IntStream.range(0, ciphertextNum))
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount).parallel() : IntStream.range(0, partitionCount))
                        .mapToObj(j ->
                            Cmg21KwPirNativeServer.computeMatches(
                                encryptionParamsList.get(0),
                                encryptionParamsList.get(2),
                                encryptionParamsList.get(1),
                                serverKeywordEncode[i * partitionCount + j],
                                queryPowers.subList(i * powerDegree.length, (i + 1) * powerDegree.length),
                                params.getPsLowDegree()))
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toCollection(ArrayList::new));
            labelResponsePayload = (parallel ? IntStream.range(0, ciphertextNum).parallel() : IntStream.range(0, ciphertextNum))
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount * labelPartitionCount).parallel() :
                        IntStream.range(0, partitionCount * labelPartitionCount))
                        .mapToObj(j ->
                            Cmg21KwPirNativeServer.computeMatches(
                                encryptionParamsList.get(0),
                                encryptionParamsList.get(2),
                                encryptionParamsList.get(1),
                                serverLabelEncode[i * partitionCount * labelPartitionCount + j],
                                queryPowers.subList(i * powerDegree.length, (i + 1) * powerDegree.length),
                                params.getPsLowDegree()))
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toCollection(ArrayList::new));
        } else if (params.getPsLowDegree() == 0) {
            keywordResponsePayload = (parallel ? IntStream.range(0, ciphertextNum).parallel() : IntStream.range(0, ciphertextNum))
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount).parallel() : IntStream.range(0, partitionCount))
                        .mapToObj(j ->
                            Cmg21KwPirNativeServer.computeMatchesNaiveMethod(
                                encryptionParamsList.get(0),
                                encryptionParamsList.get(2),
                                encryptionParamsList.get(1),
                                serverKeywordEncode[i * partitionCount + j],
                                queryPowers.subList(i * powerDegree.length, (i + 1) * powerDegree.length)))
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toCollection(ArrayList::new));
            labelResponsePayload = (parallel ? IntStream.range(0, ciphertextNum).parallel() : IntStream.range(0, ciphertextNum))
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount * labelPartitionCount).parallel() :
                        IntStream.range(0, partitionCount * labelPartitionCount))
                        .mapToObj(j ->
                            Cmg21KwPirNativeServer.computeMatchesNaiveMethod(
                                encryptionParamsList.get(0),
                                encryptionParamsList.get(2),
                                encryptionParamsList.get(1),
                                serverLabelEncode[i * partitionCount + j],
                                queryPowers.subList(i * powerDegree.length, (i + 1) * powerDegree.length)))
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toCollection(ArrayList::new));
        } else {
            throw new MpcAbortException("ps_low_degree参数设置不正确");
        }
    }

    /**
     * 服务端计算关键词PRF。
     *
     * @return 关键词PRF。
     */
    private ArrayList<ByteBuffer> computeKeywordPrf() {
        Ecc ecc = EccFactory.createInstance(envType);
        Kdf kdf = KdfFactory.createInstance(envType);
        Prg prg = PrgFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH * 2);
        alpha = BigIntegerUtils.randomPositive(ecc.getN(), secureRandom);
        Stream<ByteBuffer> keywordStream = keywordArrayList.stream();
        keywordStream = parallel ? keywordStream.parallel() : keywordStream;
        return keywordStream
            .map(keyword -> ecc.hashToCurve(keyword.array()))
            .map(hash -> ecc.multiply(hash, alpha))
            .map(prf -> ecc.encode(prf, false))
            .map(kdf::deriveKey)
            .map(prg::extendToBytes)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toCollection(ArrayList::new));
    }
}