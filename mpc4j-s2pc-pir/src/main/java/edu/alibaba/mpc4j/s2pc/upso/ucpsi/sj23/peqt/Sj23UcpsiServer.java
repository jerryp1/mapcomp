package edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.peqt;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.polynomial.power.PowerNode;
import edu.alibaba.mpc4j.common.tool.polynomial.power.PowerUtils;
import edu.alibaba.mpc4j.common.tool.polynomial.zp64.Zp64Poly;
import edu.alibaba.mpc4j.common.tool.polynomial.zp64.Zp64PolyFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtParty;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.AbstractUcpsiServer;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.peqt.Sj23PeqtUcpsiPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21.Cmg21UpsiNativeUtils;
import edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21.Cmg21UpsiPtoDesc;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * SJ23 unbalanced circuit PSI server.
 *
 * @author Liqiang Peng
 * @date 2023/7/17
 */
public class Sj23UcpsiServer<T> extends AbstractUcpsiServer<T> {
    /**
     * peqt sender
     */
    private final PeqtParty peqtParty;
    /**
     * l_peqt
     */
    private int peqtL;
    /**
     * cuckoo hash num
     */
    private final int hashNum;
    /**
     * hash keys
     */
    private byte[][] hashKeys;
    /**
     * SJ23 UCPSI params
     */
    private Sj23PeqtUcpsiParams params;
    /**
     * alpha
     */
    private int alpha;
    private byte[] publicKey;
    private byte[] relinKeys;

    public Sj23UcpsiServer(Rpc serverRpc, Party clientParty, Sj23UcpsiConfig config) {
        super(Sj23PeqtUcpsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        peqtParty = PeqtFactory.createSender(serverRpc, clientParty, config.getPeqtConfig());
        addSubPtos(peqtParty);
        hashNum = CuckooHashBinFactory.getHashNum(CuckooHashBinType.NO_STASH_PSZ18_3_HASH);
    }

    @Override
    public void init(Set<T> serverElementSet, int maxClientElementSize) throws MpcAbortException {
        setInitInput(serverElementSet, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        params = Sj23PeqtUcpsiParams.SERVER_LOG_SIZE_20_CLIENT_LOG_SIZE_8;

        stopWatch.start();
        // initial hash
        List<byte[]> itemHash = initialHash(CommonUtils.getByteLength(params.l));
        stopWatch.stop();
        long initialHashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 4, initialHashTime);

        stopWatch.start();
        // generate simple hash bin
        hashKeys = CommonUtils.generateRandomKeys(hashNum, secureRandom);
        List<byte[][]> hashBins = generateSimpleHashBin(itemHash);
        // server sends hash keys
        List<byte[]> cuckooHashKeyPayload = Arrays.stream(hashKeys).collect(Collectors.toList());
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_HASH_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
        stopWatch.stop();
        long cuckooHashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 4, cuckooHashTime);

        stopWatch.start();
        // polynomial interpolate
        List<long[][]> coeffs = encodeDatabase(hashBins);
        stopWatch.stop();
        long encodedTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 4, encodedTime);

        DataPacketHeader clientPublicKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> clientPublicKeysPayload = rpc.receive(clientPublicKeysHeader).getPayload();

        stopWatch.start();
        // set public keys
        setPublicKey(clientPublicKeysPayload);
        // initialize peqt
        peqtParty.init(params.l, alpha * params.binNum);
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 4, 4, peqtTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector psi() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        // receive query
        DataPacketHeader queryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> queryPayload =rpc.receive(queryHeader).getPayload();

        stopWatch.start();
        List<byte[]> responsePayload = computeResponse(
            encodeDatabase, queryPayload, encryptionParamsPayload.get(0), encryptionParamsPayload.get(1), binSize
        );
        stopWatch.stop();
        long replyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(responseHeader, responsePayload));
        logStepInfo(PtoState.PTO_STEP, 3, 3, replyTime, "Server generates reply");

        stopWatch.start();
        // private set membership
        SquareZ2Vector z0 = peqtParty.peqt(params.l, new byte[][]{});
        stopWatch.stop();
        long psmTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, psmTime);

        logPhaseInfo(PtoState.PTO_END);
        return z0;
    }

    private List<byte[]> initialHash(int byteL) {
        Hash hash = HashFactory.createInstance(envType, byteL);
        return serverElementArrayList.stream()
            .map(ObjectUtils::objectToByteArray)
            .map(hash::digestToBytes)
            .collect(Collectors.toList());
    }

    private List<byte[][]> generateSimpleHashBin(List<byte[]> itemList) {
        RandomPadHashBin<byte[]> simpleHashBin = new RandomPadHashBin<>(
            envType, params.binNum, serverElementSize, hashKeys
        );
        simpleHashBin.insertItems(itemList);
        List<byte[][]> completeHashBins = IntStream.range(0, params.binNum)
            .mapToObj(i -> new ArrayList<>(simpleHashBin.getBin(i)))
            .map(binItemList -> binItemList.stream()
                .map(hashBinEntry -> BytesUtils.clone(hashBinEntry.getItemByteArray()))
                .toArray(byte[][]::new))
            .collect(Collectors.toList());
        simpleHashBin.clear();
        return completeHashBins;
    }

    /**
     * encode database.
     *
     * @param hashBins hash bin.
     * @return encoded database.
     */
    public List<long[][]> encodeDatabase(List<byte[][]> hashBins) {
        int maxBinSize = hashBins.get(0).length;
        for (int i = 1; i < params.binNum; i++) {
            if (hashBins.get(i).length > maxBinSize) {
                maxBinSize = hashBins.get(i).length;
            }
        }
        Zp64Poly zp64Poly = Zp64PolyFactory.createInstance(envType, (long) params.plainModulus);
        // we will split the hash table into partitions
        alpha = CommonUtils.getUnitNum(maxBinSize, params.maxPartitionSizePerBin);
        int bigPartitionIndex = maxBinSize / params.maxPartitionSizePerBin;
        long[][] coeffs = new long[params.itemEncodedSlotSize * params.itemPerCiphertext][];
        List<long[][]> coeffsPolys = new ArrayList<>();
        long[][] encodedItemArray = new long[params.binNum * params.itemEncodedSlotSize][maxBinSize];
        BigInteger shiftMask = BigInteger.ONE.shiftLeft(params.l).subtract(BigInteger.ONE);
        for (int i = 0; i < params.binNum; i++) {
            for (int j = 0; j < hashBins.get(i).length; j++) {
                long[] item = params.getHashBinEntryEncodedArray(hashBins.get(i)[j], shiftMask);
                for (int l = 0; l < params.itemEncodedSlotSize; l++) {
                    encodedItemArray[i * params.itemEncodedSlotSize + l][j] = item[l];
                }
            }
            for (int j = 0; j < maxBinSize - hashBins.get(i).length; j++) {
                long[] item = IntStream.range(0, params.itemEncodedSlotSize).mapToLong(l -> 1L).toArray();
                for (int l = 0; l < params.itemEncodedSlotSize; l++) {
                    encodedItemArray[i * params.itemEncodedSlotSize + l][j + hashBins.get(i).length] = item[l];
                }
            }
        }
        // for each bucket, compute the coefficients of the polynomial f(x) = \prod_{y in bucket} (x - y)
        for (int i = 0; i < params.ciphertextNum; i++) {
            for (int partition = 0; partition < alpha; partition++) {
                int partitionSize, partitionStart;
                partitionSize = partition < bigPartitionIndex ?
                    params.maxPartitionSizePerBin : maxBinSize % params.maxPartitionSizePerBin;
                partitionStart = params.maxPartitionSizePerBin * partition;
                IntStream intStream = IntStream.range(0, params.itemPerCiphertext * params.itemEncodedSlotSize);
                intStream = parallel ? intStream.parallel() : intStream;
                int finalI = i;
                intStream.forEach(j -> {
                    long[] tempVector = new long[partitionSize];
                    System.arraycopy(
                        encodedItemArray[finalI * params.itemPerCiphertext * params.itemEncodedSlotSize + j],
                        partitionStart,
                        tempVector,
                        0,
                        partitionSize
                    );
                    coeffs[j] = zp64Poly.rootInterpolate(partitionSize, tempVector, 0L);
                });
                long[][] temp = new long[partitionSize + 1][params.polyModulusDegree];
                for (int j = 0; j < partitionSize + 1; j++) {
                    for (int l = 0; l < params.itemPerCiphertext * params.itemEncodedSlotSize; l++) {
                        temp[j][l] = coeffs[l][j];
                    }
                    for (int l = params.itemPerCiphertext * params.itemEncodedSlotSize; l < params.polyModulusDegree; l++) {
                        temp[j][l] = 0;
                    }
                }
                coeffsPolys.add(temp);
            }
        }
        return coeffsPolys;
    }

    private void setPublicKey(List<byte[]> clientPublicKeysPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(clientPublicKeysPayload.size() == 2);
        publicKey = clientPublicKeysPayload.remove(0);
        relinKeys = clientPublicKeysPayload.remove(0);
    }

    /**
     * server generate response.
     *
     * @param database         database.
     * @param queryList        query list.
     * @param encryptionParams encryption params.
     * @param relinKeys        relinearization keys.
     * @param binSize          bin size.
     * @return server response.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private List<byte[]> computeResponse(List<long[][]> database, List<byte[]> queryList, byte[] encryptionParams,
                                         byte[] relinKeys, int binSize) throws MpcAbortException {
        int ciphertextNum = params.binNum / (params.polyModulusDegree / params.itemEncodedSlotSize);
        int partitionCount = CommonUtils.getUnitNum(binSize, params.maxPartitionSizePerBin);
        MpcAbortPreconditions.checkArgument(
            queryList.size() == ciphertextNum * params.queryPowers.length, "The size of query is incorrect"
        );
        int[][] powerDegree = computePowerDegree();
        IntStream intStream = IntStream.range(0, ciphertextNum);
        intStream = parallel ? intStream.parallel() : intStream;
        List<byte[]> queryPowers = intStream
            .mapToObj(i -> Sj23PeqtUcpsiNativeUtils.computeEncryptedPowers(
                encryptionParams,
                relinKeys,
                queryList.subList(i * params.queryPowers.length, (i + 1) * params.queryPowers.length),
                powerDegree,
                params.queryPowers,
                params.psLowDegree)
            )
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(ArrayList::new));
        if (params.psLowDegree > 0) {
            return IntStream.range(0, ciphertextNum)
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount).parallel() : IntStream.range(0, partitionCount))
                        .mapToObj(j -> Sj23PeqtUcpsiNativeUtils.optComputeMatches(
                            encryptionParams,
                            relinKeys,
                            database.get(i * partitionCount + j),
                            queryPowers.subList(i * powerDegree.length, (i + 1) * powerDegree.length),
                            params.psLowDegree)
                        )
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        } else if (params.psLowDegree == 0) {
            return IntStream.range(0, ciphertextNum)
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount).parallel() : IntStream.range(0, partitionCount))
                        .mapToObj(j -> Sj23PeqtUcpsiNativeUtils.naiveComputeMatches(
                                encryptionParams,
                                database.get(i * partitionCount + j),
                                queryPowers.subList(i * powerDegree.length, (i + 1) * powerDegree.length)
                            )
                        )
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        } else {
            throw new MpcAbortException("ps_low_degree is incorrect");
        }
    }

    /**
     * compute power degree.
     *
     * @return power degree.
     */
    public int[][] computePowerDegree() {
        int[][] powerDegree;
        if (params.psLowDegree > 0) {
            Set<Integer> innerPowersSet = new HashSet<>();
            Set<Integer> outerPowersSet = new HashSet<>();
            IntStream.range(0, params.queryPowers.length).forEach(i -> {
                if (params.queryPowers[i] <= params.psLowDegree) {
                    innerPowersSet.add(params.queryPowers[i]);
                } else {
                    outerPowersSet.add(params.queryPowers[i] / (params.psLowDegree + 1));
                }
            });
            PowerNode[] innerPowerNodes = PowerUtils.computePowers(innerPowersSet, params.psLowDegree);
            PowerNode[] outerPowerNodes = PowerUtils.computePowers(
                outerPowersSet, params.maxPartitionSizePerBin / (params.psLowDegree + 1));
            powerDegree = new int[innerPowerNodes.length + outerPowerNodes.length][2];
            int[][] innerPowerNodesDegree = Arrays.stream(innerPowerNodes).map(PowerNode::toIntArray).toArray(int[][]::new);
            int[][] outerPowerNodesDegree = Arrays.stream(outerPowerNodes).map(PowerNode::toIntArray).toArray(int[][]::new);
            System.arraycopy(innerPowerNodesDegree, 0, powerDegree, 0, innerPowerNodesDegree.length);
            System.arraycopy(outerPowerNodesDegree, 0, powerDegree, innerPowerNodesDegree.length, outerPowerNodesDegree.length);
        } else {
            Set<Integer> sourcePowersSet = Arrays.stream(params.queryPowers)
                .boxed()
                .collect(Collectors.toCollection(HashSet::new));
            PowerNode[] powerNodes = PowerUtils.computePowers(sourcePowersSet, params.maxPartitionSizePerBin);
            powerDegree = Arrays.stream(powerNodes).map(PowerNode::toIntArray).toArray(int[][]::new);
        }
        return powerDegree;
    }
}
