package edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21;

import com.google.common.primitives.Bytes;
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
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.s2pc.pir.keyword.AbstractKwPirServer;
import edu.alibaba.mpc4j.s2pc.pir.keyword.PolynomialUtils;
import edu.alibaba.mpc4j.s2pc.pir.keyword.PowersNode;
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

/**
 * CMG21关键词索引PIR协议服务端。
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public class Cmg21KwPirServer extends AbstractKwPirServer {
    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * 关键词索引PIR方案参数
     */
    private final Cmg21KwPirParams params;
    /**
     * 哈希分桶
     */
    private List<ArrayList<HashBinEntry<ByteBuffer>>> hashBins;
    /**
     * 环境类型
     */
    private final EnvType envType;
    /**
     * 服务端元素编码
     */
    private long[][][] encodedElement;
    /**
     * 服务端标签编码
     */
    private long[][][] encodedLabels;
    /**
     * PRF密钥
     */
    private BigInteger alpha;
    /**
     * 元素回复
     */
    private List<byte[]> itemResponse;
    /**
     * 标签回复
     */
    private List<byte[]> labelResponse;
    /**
     * 哈希算法密钥
     */
    private byte[][] hashKeys;

    public Cmg21KwPirServer(Rpc serverRpc, Party clientParty, Cmg21KwPirConfig config) {
        super(Cmg21KwPirPtoDesc.getInstance(), serverRpc, clientParty, config);
        this.envType = config.getEnvType();
        this.params = config.getParams();
    }

    @Override
    public void init(Map<ByteBuffer, ByteBuffer> serverElementSet, int keywordByteLength, int labelByteLength) {
        setInitInput(serverElementSet, keywordByteLength, labelByteLength);

        info("{}{} Server Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());
        // 服务端计算PRF输出
        stopWatch.start();
        ArrayList<ByteBuffer> prfOutputs = prf();
        Map<ByteBuffer, ByteBuffer> prfMapLabel = IntStream.range(0, serverElementSize)
            .boxed()
            .collect(Collectors.toMap(prfOutputs::get, i -> serverElementMap.get(serverElementArrayList.get(i)), (a, b) -> b));
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Init 1/3 PRF ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), oprfTime);

        // 服务端计算完全哈希分桶
        stopWatch.start();
        hashKeys = genHashKeys(params.getHashNum());
        hashBins = generateCompleteHashBin(prfOutputs, params.getBinNum());
        stopWatch.stop();
        long completeHashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Init 2/3 complete hash ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), completeHashTime);

        // 计算多项式系数
        stopWatch.start();
        encodeDatabase(prfMapLabel);
        stopWatch.stop();
        long encodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Init 3/3 encode database into plaintexts ({}ms)", ptoStepLogPrefix,
            getPtoDesc().getPtoName(), encodeTime);

        initialized = true;
        info("{}{} Server Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public void pir(int retrievalNum) throws MpcAbortException {
        setPtoInput(retrievalNum);
        info("{}{} Server begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        // 发送哈希分桶密钥
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Cmg21KwPirPtoDesc.PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(),
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader,
            Arrays.stream(hashKeys).collect(Collectors.toCollection(ArrayList::new))));

        for (int retrievalIndex = 0; retrievalIndex < retrievalNum; retrievalIndex++) {
            info("{}{} Server Retrieval Number {})", ptoStepLogPrefix, getPtoDesc().getPtoName(), retrievalIndex + 1);

            // 服务端执行OPRF协议
            stopWatch.start();
            DataPacketHeader blindHeader = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), Cmg21KwPirPtoDesc.PtoStep.CLIENT_SEND_BLIND.ordinal(), retrievalIndex,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> blindPayload = rpc.receive(blindHeader).getPayload();
            List<byte[]> blindPrf = handleBlindPayload(blindPayload);
            DataPacketHeader blindPrfHeader = new DataPacketHeader(
                taskId, ptoDesc.getPtoId(), Cmg21KwPirPtoDesc.PtoStep.SERVER_SEND_BLIND_PRF.ordinal(), retrievalIndex,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(blindPrfHeader, blindPrf));
            stopWatch.stop();
            long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            info("{}{} Server Step OPRF 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), oprfTime);

            // 接收客户端布谷鸟哈希分桶结果
            DataPacketHeader cuckooHashResultHeader = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), Cmg21KwPirPtoDesc.PtoStep.CLIENT_SEND_CUCKOO_HASH_RESULT.ordinal(),
                retrievalIndex, otherParty().getPartyId(), rpc.ownParty().getPartyId()
            );
            byte[] cuckooHashResultBytes = rpc.receive(cuckooHashResultHeader).getPayload().get(0);
            MpcAbortPreconditions.checkArgument(BigIntegerUtils.byteArrayToBigInteger(
                cuckooHashResultBytes).equals(BigInteger.ONE), "cuckoo hash failed.");

            // 接收客户端的加密密钥
            DataPacketHeader encryptionParamsDataPacketHeader = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), Cmg21KwPirPtoDesc.PtoStep.CLIENT_SEND_ENCRYPTION_PARAMS.ordinal(),
                retrievalIndex, otherParty().getPartyId(), rpc.ownParty().getPartyId()
            );
            List<byte[]> encryptionParams = rpc.receive(encryptionParamsDataPacketHeader).getPayload();
            MpcAbortPreconditions.checkArgument(encryptionParams.size() == 3,
                "Failed to receive BFV encryption parameters");

            // 接收客户端的加密查询信息
            DataPacketHeader receiverQueryDataPacketHeader = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), Cmg21KwPirPtoDesc.PtoStep.CLIENT_SEND_QUERY.ordinal(),
                retrievalIndex, otherParty().getPartyId(), rpc.ownParty().getPartyId()
            );
            List<byte[]> queryList = rpc.receive(receiverQueryDataPacketHeader).getPayload();
            int ciphertextNum = params.getBinNum() / (params.getPolyModulusDegree() / params.getItemEncodedSlotSize());
            MpcAbortPreconditions.checkArgument(queryList.size() == ciphertextNum * params.getQueryPowers().length,
                "The size of query is incorrect");

            // 计算密文多项式运算
            stopWatch.start();
            computeResponse(queryList, encryptionParams);
            DataPacketHeader serverItemResponseDataPacketSpec = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), Cmg21KwPirPtoDesc.PtoStep.SERVER_SEND_ITEM_RESPONSE.ordinal(),
                retrievalIndex, rpc.ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(serverItemResponseDataPacketSpec, itemResponse));
            DataPacketHeader serverLabelResponseDataPacketSpec = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), Cmg21KwPirPtoDesc.PtoStep.SERVER_SEND_LABEL_RESPONSE.ordinal(),
                retrievalIndex, rpc.ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(serverLabelResponseDataPacketSpec, labelResponse));
            stopWatch.stop();
            long genReplyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            info("{}{} Server Step 2/2 response generation ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(),
                genReplyTime);
        }

        info("{}{} Server end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    /**
     * 生成哈希算法密钥。
     *
     * @return 哈希算法密钥。
     */
    private byte[][] genHashKeys(int hashCount) {
        byte[][] seeds = new byte[hashCount][CommonConstants.BLOCK_BYTE_LENGTH];
        IntStream.range(0, seeds.length).forEach(i -> secureRandom.nextBytes(seeds[i]));
        return seeds;
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
        int encodedItemBitLength = (BigInteger.valueOf(params.getPlainModulus()).bitLength() - 1) * params.getItemEncodedSlotSize();
        for (HashBinEntry<ByteBuffer> binItem : binItems) {
            long[] itemParts = new long[params.getItemEncodedSlotSize()];
            BigInteger item = BigIntegerUtils.byteArrayToBigInteger(binItem.getItem().array());
            item = item.shiftRight(item.bitLength() - encodedItemBitLength);
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
     * @param elementList 元素集合。
     * @param binNum      哈希桶数量。
     * @return 完全哈希分桶。
     */
    private List<ArrayList<HashBinEntry<ByteBuffer>>> generateCompleteHashBin(ArrayList<ByteBuffer> elementList,
                                                                              int binNum) {
        RandomPadHashBin<ByteBuffer> completeHash = new RandomPadHashBin<>(envType, binNum, serverElementSize, hashKeys);
        completeHash.insertItems(elementList);
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
        int ciphertextNum = params.getBinNum() / (params.getPolyModulusDegree() / params.getItemEncodedSlotSize());
        int itemPerCiphertext = params.getPolyModulusDegree() / params.getItemEncodedSlotSize();
        int binSize = hashBins.get(0).size();
        int partitionCount = (binSize + params.getMaxPartitionSizePerBin() - 1) / params.getMaxPartitionSizePerBin();
        int bigPartitionCount = binSize / params.getMaxPartitionSizePerBin();
        int labelPartitionCount = (int) Math.ceil((labelByteLength + CommonConstants.BLOCK_BYTE_LENGTH) * 8.0 /
            ((BigInteger.valueOf(params.getPlainModulus()).bitLength() - 1) * params.getItemEncodedSlotSize()));
        encodedElement = new long[partitionCount * ciphertextNum][][];
        encodedLabels = new long[partitionCount * ciphertextNum * labelPartitionCount][][];
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
                        HashBinEntry<ByteBuffer> entry = hashBins.get(finalI * itemPerCiphertext + j).get(partitionStart + l);
                        long[] temp = params.getHashBinEntryEncodedArray(entry, false);
                        for (int k = 0; k < params.getItemEncodedSlotSize(); k++) {
                            currentBucketElement.get(k).set(l, temp[k]);
                        }
                    }
                    for (int l = 0; l < params.getItemEncodedSlotSize(); l++) {
                        fCoeffs[j * params.getItemEncodedSlotSize() + l] =
                            PolynomialUtils.polynomialFromRoots(currentBucketElement.get(l), params.getPlainModulus());
                        assert fCoeffs[j * params.getItemEncodedSlotSize() + l].length == partitionSize + 1;
                    }
                    int nonEmptyBuckets = 0;
                    for (int l = 0; l < partitionSize; l++) {
                        HashBinEntry<ByteBuffer> entry = hashBins.get(finalI * itemPerCiphertext + j).get(partitionStart + l);
                        if (entry.getHashIndex() != -1) {
                            for (int k = 0; k < params.getItemEncodedSlotSize(); k++) {
                                currentBucketElement.get(k).set(nonEmptyBuckets, currentBucketElement.get(k).get(l));
                            }
                            byte[] keyBytes = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                            IntStream.range(0, CommonConstants.BLOCK_BYTE_LENGTH)
                                .forEach(k -> keyBytes[k] = entry.getItem().array()[k]);
                            ByteBuffer encryptedLabel = labelEncryption(keyBytes, prfMap.get(entry.getItem()));
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
                            gCoeffs[k][j * params.getItemEncodedSlotSize() + l] = PolynomialUtils.polynomialFromPoints(
                                currentBucketElement.get(l), currentBucketLabels.get(k).get(l), params.getPlainModulus());
                        }
                    }
                });
                long[][] encodeElementVector = new long[partitionSize + 1][params.getPolyModulusDegree()];
                int labelSize = IntStream.range(1, gCoeffs[0].length)
                    .map(j -> gCoeffs[0][j].length).filter(j -> j >= 1).max()
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
                            encodeLabelVector[k][j][l] = (j < gCoeffs[k][l].length) ? gCoeffs[k][l][j] : 0;
                        }
                        for (int l = params.getItemEncodedSlotSize() * itemPerCiphertext; l < params.getPolyModulusDegree();
                             l++) {
                            encodeLabelVector[k][j][l] = 0;
                        }
                    }
                }
                encodedElement[partition + i * partitionCount] = encodeElementVector;
                for (int j = 0; j < labelPartitionCount; j++) {
                    encodedLabels[j + partition * labelPartitionCount + i * partitionCount * labelPartitionCount] =
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
    private ByteBuffer labelEncryption(byte[] keyBytes, ByteBuffer labelBytes) {
        BlockCipher blockCipher = new AESEngine();
        StreamCipher streamCipher = new OFBBlockCipher(blockCipher, 8 * CommonConstants.BLOCK_BYTE_LENGTH);
        byte[] ivBytes = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(ivBytes);
        KeyParameter key = new KeyParameter(keyBytes);
        CipherParameters withIv = new ParametersWithIV(key, ivBytes);
        streamCipher.init(true, withIv);
        byte[] outputs = new byte[labelByteLength];

        streamCipher.processBytes(labelBytes.array(), 0, labelByteLength, outputs, 0);
        return ByteBuffer.wrap(Bytes.concat(ivBytes, outputs));
    }

    /**
     * 计算给定范围内的幂次方。
     *
     * @param sourcePowers 源幂次方。
     * @param upperBound   上界。
     * @return 给定范围内的幂次方。
     */
    private int[][] computePowers(Set<Integer> sourcePowers, int upperBound) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(upperBound > 1, "upperBound must be greater than 1 : " + upperBound);
        Set<Integer> targetPowers = IntStream.rangeClosed(1, upperBound)
            .boxed()
            .collect(Collectors.toCollection(HashSet::new));
        Integer[] sortSourcePowers = Arrays.stream(sourcePowers.toArray(new Integer[0]))
            .sorted()
            .toArray(Integer[]::new);
        MpcAbortPreconditions.checkArgument(sortSourcePowers[sortSourcePowers.length - 1] <= upperBound, "Source powers "
            + "must be a subset of target powers");
        PowersNode[] powersNodes = new PowersNode[upperBound];
        IntStream.range(0, sortSourcePowers.length)
            .forEach(i -> powersNodes[sortSourcePowers[i] - 1] = new PowersNode(sortSourcePowers[i], 0));
        int currDepth = 0;
        for (int currPower = 1; currPower <= upperBound; currPower++) {
            if (powersNodes[currPower - 1] != null) {
                continue;
            }
            int optimalDepth = currPower - 1;
            int optimalS1 = currPower - 1;
            int optimalS2 = 1;
            for (int s1 = 1; s1 <= targetPowers.size(); s1++) {
                if (s1 >= currPower) {
                    break;
                }
                int s2 = currPower - s1;
                if (!targetPowers.contains(s2)) {
                    continue;
                }
                int depth = Math.max(powersNodes[s1 - 1].depth, powersNodes[s2 - 1].depth) + 1;
                if (depth < optimalDepth) {
                    optimalDepth = depth;
                    optimalS1 = s1;
                    optimalS2 = s2;
                }
            }
            powersNodes[currPower - 1] = new PowersNode(currPower, optimalDepth, optimalS1, optimalS2);
            currDepth = Math.max(currDepth, optimalDepth);
        }
        return IntStream.range(0, upperBound).mapToObj(i -> {
            int[] parentPowers = new int[2];
            parentPowers[0] = powersNodes[i].leftParentPower;
            parentPowers[1] = powersNodes[i].rightParentPower;
            return parentPowers;
        }).toArray(int[][]::new);
    }

    /**
     * 服务端计算密文多项式结果。
     *
     * @param ciphertextPoly   密文多项式。
     * @param encryptionParams 加密方案参数。
     * @throws MpcAbortException 如果协议异常中止。
     */
    private void computeResponse(List<byte[]> ciphertextPoly, List<byte[]> encryptionParams)
        throws MpcAbortException {
        int binSize = hashBins.get(0).size();
        int partitionCount = (binSize + params.getMaxPartitionSizePerBin() - 1) / params.getMaxPartitionSizePerBin();
        // 计算所有的密文次方
        int[][] powers;
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
            int[][] innerPowers = computePowers(innerPowersSet, params.getPsLowDegree());
            int[][] outerPowers = computePowers(outerPowersSet, params.getMaxPartitionSizePerBin() / (params.getPsLowDegree() + 1));
            powers = new int[innerPowers.length + outerPowers.length][2];
            System.arraycopy(innerPowers, 0, powers, 0, innerPowers.length);
            System.arraycopy(outerPowers, 0, powers, innerPowers.length, outerPowers.length);
        } else {
            Set<Integer> sourcePowersSet = Arrays.stream(params.getQueryPowers())
                .boxed()
                .collect(Collectors.toCollection(HashSet::new));
            powers = computePowers(sourcePowersSet, params.getMaxPartitionSizePerBin());
        }
        int ciphertextNum = params.getBinNum() / (params.getPolyModulusDegree() / params.getItemEncodedSlotSize());
        int labelPartitionCount = (int) Math.ceil((labelByteLength + CommonConstants.BLOCK_BYTE_LENGTH) * 8.0 /
            ((BigInteger.valueOf(params.getPlainModulus()).bitLength() - 1) * params.getItemEncodedSlotSize()));
        IntStream queryIntStream = parallel ?
            IntStream.range(0, ciphertextNum).parallel() : IntStream.range(0, ciphertextNum);
        List<byte[]> queryPowers = queryIntStream
            .mapToObj(i -> Cmg21KwPirNativeServer.computeEncryptedPowers(
                ciphertextPoly.subList(i * params.getQueryPowers().length, (i + 1) * params.getQueryPowers().length),
                encryptionParams.get(1), encryptionParams.get(0), powers, params.getQueryPowers(),
                params.getPsLowDegree()))
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(ArrayList::new));
        if (params.getPsLowDegree() > 0) {
            itemResponse = (parallel ? IntStream.range(0, ciphertextNum).parallel() : IntStream.range(0, ciphertextNum))
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount).parallel() : IntStream.range(0, partitionCount))
                        .mapToObj(j ->
                            Cmg21KwPirNativeServer.computeMatches(
                                encodedElement[i * partitionCount + j],
                                queryPowers.subList(i * powers.length, (i + 1) * powers.length),
                                encryptionParams.get(1),
                                encryptionParams.get(2),
                                encryptionParams.get(0),
                                params.getPsLowDegree()))
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
            labelResponse = (parallel ? IntStream.range(0, ciphertextNum).parallel() : IntStream.range(0, ciphertextNum))
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount * labelPartitionCount).parallel() :
                        IntStream.range(0, partitionCount * labelPartitionCount))
                        .mapToObj(j ->
                            Cmg21KwPirNativeServer.computeMatches(
                                encodedLabels[i * partitionCount * labelPartitionCount + j],
                                queryPowers.subList(i * powers.length, (i + 1) * powers.length),
                                encryptionParams.get(1),
                                encryptionParams.get(2),
                                encryptionParams.get(0),
                                params.getPsLowDegree()))
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        } else if (params.getPsLowDegree() == 0) {
            itemResponse = (parallel ? IntStream.range(0, ciphertextNum).parallel() : IntStream.range(0, ciphertextNum))
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount).parallel() : IntStream.range(0, partitionCount))
                        .mapToObj(j ->
                            Cmg21KwPirNativeServer.computeMatchesNaiveMethod(
                                encodedElement[i * partitionCount + j],
                                queryPowers.subList(i * powers.length, (i + 1) * powers.length),
                                encryptionParams.get(1),
                                encryptionParams.get(0),
                                encryptionParams.get(2)))
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
            labelResponse = (parallel ? IntStream.range(0, ciphertextNum).parallel() : IntStream.range(0, ciphertextNum))
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount * labelPartitionCount).parallel() :
                        IntStream.range(0, partitionCount * labelPartitionCount))
                        .mapToObj(j ->
                            Cmg21KwPirNativeServer.computeMatchesNaiveMethod(
                                encodedLabels[i * partitionCount + j],
                                queryPowers.subList(i * powers.length, (i + 1) * powers.length),
                                encryptionParams.get(1),
                                encryptionParams.get(0),
                                encryptionParams.get(2)))
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        } else {
            throw new MpcAbortException("ps_low_degree参数设置不正确");
        }
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
            .map(element -> ecc.encode(element, true))
            .collect(Collectors.toList());
    }

    /**
     * 服务端计算元素PRF。
     *
     * @return 元素PRF。
     */
    private ArrayList<ByteBuffer> prf() {
        Ecc ecc = EccFactory.createInstance(envType);
        alpha = BigIntegerUtils.randomPositive(ecc.getN(), secureRandom);
        ByteBuffer[] prfOutputArray = new ByteBuffer[serverElementSize];
        IntStream intStream = parallel ? IntStream.range(0, serverElementSize).parallel() : IntStream.range(0, serverElementSize);
        intStream.forEach(i -> {
            ECPoint ecPoint = ecc.multiply(ecc.hashToCurve(serverElementArrayList.get(i).array()), alpha);
            prfOutputArray[i] = ByteBuffer.wrap(ecc.encode(ecPoint, true));
        });
        return new ArrayList<>(Arrays.asList(prfOutputArray));
    }
}