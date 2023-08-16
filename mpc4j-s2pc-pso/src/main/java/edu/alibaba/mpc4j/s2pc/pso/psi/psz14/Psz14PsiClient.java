package edu.alibaba.mpc4j.s2pc.pso.psi.psz14;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.filter.Filter;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Psz14PsiClient <T> extends AbstractPsiClient<T> {
    /**
     * OPRF接收方
     */
    private final OprfReceiver oprfReceiver;
    /**
     * 布谷鸟哈希类型
     */
    private final CuckooHashBinFactory.CuckooHashBinType cuckooHashBinType;
    /**
     * 布谷鸟哈希函数数量
     */
    private final int cuckooHashNum;
    /**
     * PEQT哈希函数
     */
    private Hash peqtHash;
    /**
     * 哈希桶数量
     */
    private int binNum;
    /**
     * 贮存区大小
     */
    private int stashSize;
    /**
     * 布谷鸟哈希
     */
    private CuckooHashBin<T> cuckooHashBin;
    /**
     * 客户端布谷鸟哈希中元素的PRF结果
     */
    private ArrayList<byte[]> clientOprfArrayList;
    /**
     * 服务端哈希桶中元素的PRF结果
     */
    private ArrayList<Filter<byte[]>> serverBinPrfFilterArrayList;
    /**
     * 服务端贮存区中元素的PRF结果
     */
    private ArrayList<Filter<byte[]>> serverStashPrfFilterArrayList;

    public Psz14PsiClient(Rpc clientRpc, Party serverParty, Psz14PsiConfig config) {
        super(Psz14PsiPtoDesc.getInstance(), clientRpc, serverParty, config);
        oprfReceiver = OprfFactory.createOprfReceiver(clientRpc, serverParty, config.getOprfConfig());
        addSubPtos(oprfReceiver);
        cuckooHashBinType = config.getCuckooHashBinType();
        cuckooHashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int maxOprfBatchNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxClientElementSize)
                + CuckooHashBinFactory.getStashSize(cuckooHashBinType, maxClientElementSize);
        oprfReceiver.init(maxOprfBatchNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Set<T> psi(Set<T> clientElementSet, int serverSetSize) throws MpcAbortException {
        setPtoInput(clientElementSet, serverSetSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        int peqtByteLength = PsiUtils.getSemiHonestPeqtByteLength(serverElementSize, clientElementSize);
        peqtHash = HashFactory.createInstance(envType, peqtByteLength);
        binNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, clientElementSize);
        stashSize = CuckooHashBinFactory.getStashSize(cuckooHashBinType, clientElementSize);
        List<byte[]> cuckooHashKeyPayload = generateCuckooHashKeyPayload();
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
                this.encodeTaskId, getPtoDesc().getPtoId(), Psz14PsiPtoDesc.PtoStep.CLIENT_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
        stopWatch.stop();
        long keyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, keyTime);

        stopWatch.start();
        byte[][] clientExtendByteArrays = generateExtendElementByteArrays();
        OprfReceiverOutput oprfReceiverOutput = oprfReceiver.oprf(clientExtendByteArrays);
        IntStream oprfIndexIntStream = IntStream.range(0, binNum + stashSize);
        oprfIndexIntStream = parallel ? oprfIndexIntStream.parallel() : oprfIndexIntStream;
        clientOprfArrayList = oprfIndexIntStream
                .mapToObj(index -> peqtHash.digestToBytes(oprfReceiverOutput.getPrf(index)))
                .collect(Collectors.toCollection(ArrayList::new));
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, oprfTime);

        stopWatch.start();
        // 接收服务端哈希桶PRF过滤器
        serverBinPrfFilterArrayList = new ArrayList<>(cuckooHashNum);
        serverBinPrfFilterArrayList.ensureCapacity(cuckooHashNum);
        for (int hashIndex = 0; hashIndex < cuckooHashNum; hashIndex++) {
            DataPacketHeader serverBinPrfHeader = new DataPacketHeader(
                    this.encodeTaskId, getPtoDesc().getPtoId(), Psz14PsiPtoDesc.PtoStep.SERVER_SEND_BIN_PRFS.ordinal(), extraInfo,
                    otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> serverBinPrfPayload = rpc.receive(serverBinPrfHeader).getPayload();
            extraInfo++;
            handleServerBinPrfPayload(serverBinPrfPayload);
        }
        // 接受服务端贮存区PRF过滤器
        serverStashPrfFilterArrayList = new ArrayList<>(stashSize);
        serverStashPrfFilterArrayList.ensureCapacity(stashSize);
        for (int stashIndex = 0; stashIndex < stashSize; stashIndex++) {
            DataPacketHeader serverBinPrfHeader = new DataPacketHeader(
                    this.encodeTaskId, getPtoDesc().getPtoId(), Psz14PsiPtoDesc.PtoStep.SERVER_SEND_STASH_PRFS.ordinal(), extraInfo,
                    otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> serverBinPrfPayload = rpc.receive(serverBinPrfHeader).getPayload();
            extraInfo++;
            handleServerStashPrfPayload(serverBinPrfPayload);
        }
        // 求交集
        Set<T> intersection = handleServerPrf();
        stopWatch.stop();
        long serverPrfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, serverPrfTime);

        logPhaseInfo(PtoState.PTO_END);
        return intersection;
    }

    private List<byte[]> generateCuckooHashKeyPayload() {
        // 设置布谷鸟哈希，如果发现不能构造成功，则可以重复构造
        boolean success = false;
        byte[][] cuckooHashKeys = null;
        while (!success) {
            try {
                cuckooHashKeys = IntStream.range(0, cuckooHashNum)
                        .mapToObj(hashIndex -> {
                            byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                            secureRandom.nextBytes(key);
                            return key;
                        })
                        .toArray(byte[][]::new);
                cuckooHashBin = CuckooHashBinFactory.createCuckooHashBin(
                        envType, cuckooHashBinType, clientElementSize, cuckooHashKeys
                );
                // 将客户端消息插入到CuckooHash中
                cuckooHashBin.insertItems(clientElementArrayList);
                success = true;
            } catch (ArithmeticException ignored) {
                // 如果插入不成功，就重新插入
            }
        }
        // 如果成功，则向布谷鸟哈希的空余位置插入空元素
        cuckooHashBin.insertPaddingItems(secureRandom);
        return Arrays.stream(cuckooHashKeys).collect(Collectors.toList());
    }

    private byte[][] generateExtendElementByteArrays() {
        // 前面是桶中的元素，后面的是贮存区中的元素
        byte[][] extendElementByteArrays = new byte[binNum + stashSize][];
        IntStream.range(0, binNum).forEach(binIndex -> {
            HashBinEntry<T> hashBinEntry = cuckooHashBin.getHashBinEntry(binIndex);
            int hashIndex = hashBinEntry.getHashIndex();
            byte[] elementByteArray = hashBinEntry.getItemByteArray();
            extendElementByteArrays[binIndex] = ByteBuffer.allocate(elementByteArray.length + Integer.BYTES)
                    .put(elementByteArray)
                    .putInt(hashIndex)
                    .array();
        });
        ArrayList<HashBinEntry<T>> stash = cuckooHashBin.getStash();
        IntStream.range(0, stashSize).forEach(stashIndex ->
                extendElementByteArrays[binNum + stashIndex] = stash.get(stashIndex).getItemByteArray()
        );
        return extendElementByteArrays;
    }

    private void handleServerBinPrfPayload(List<byte[]> serverBinPrfPayload) throws MpcAbortException {
        try {
            Filter<byte[]> serverBinPrfFilter = FilterFactory.createFilter(envType, serverBinPrfPayload);
            serverBinPrfFilterArrayList.add(serverBinPrfFilter);
        } catch (IllegalArgumentException e) {
            throw new MpcAbortException();
        }
    }

    private void handleServerStashPrfPayload(List<byte[]> serverStashPrfPayload) throws MpcAbortException {
        try {
            Filter<byte[]> serverStashPrfFilter = FilterFactory.createFilter(envType, serverStashPrfPayload);
            serverStashPrfFilterArrayList.add(serverStashPrfFilter);
        } catch (IllegalArgumentException e) {
            throw new MpcAbortException();
        }
    }

    private Set<T> handleServerPrf() {
        // 遍历布谷鸟哈希中的哈希桶
        Set<T> intersection = IntStream.range(0, binNum)
                .mapToObj(binIndex -> {
                    HashBinEntry<T> hashBinEntry = cuckooHashBin.getHashBinEntry(binIndex);
                    if (hashBinEntry.getHashIndex() == HashBinEntry.DUMMY_ITEM_HASH_INDEX) {
                        // 虚拟节点，肯定不在交集中
                        return null;
                    }
                    T element = hashBinEntry.getItem();
                    int hashIndex = hashBinEntry.getHashIndex();
                    byte[] elementPrf = clientOprfArrayList.get(binIndex);
                    return serverBinPrfFilterArrayList.get(hashIndex).mightContain(elementPrf) ? element : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        serverBinPrfFilterArrayList = null;
        // 遍历贮存区的元素
        ArrayList<HashBinEntry<T>> stash = cuckooHashBin.getStash();
        Set<T> stashIntersection = IntStream.range(0, cuckooHashBin.stashSize())
                .mapToObj(stashIndex -> {
                    HashBinEntry<T> hashBinEntry = stash.get(stashIndex);
                    if (hashBinEntry.getHashIndex() == HashBinEntry.DUMMY_ITEM_HASH_INDEX) {
                        // 虚拟节点，肯定不在交集中
                        return null;
                    }
                    T element = hashBinEntry.getItem();
                    byte[] elementPrf = clientOprfArrayList.get(binNum + stashIndex);
                    return serverStashPrfFilterArrayList.get(stashIndex).mightContain(elementPrf) ? element : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        intersection.addAll(stashIntersection);
        cuckooHashBin = null;
        clientOprfArrayList = null;
        serverStashPrfFilterArrayList = null;
        return intersection;
    }
}
