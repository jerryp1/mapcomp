package edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.crypto.stream.StreamCipher;
import edu.alibaba.mpc4j.common.tool.crypto.stream.StreamCipherFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64Factory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pir.keyword.AbstractKwPirClient;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirParams;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirPtoDesc.PtoStep;
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
 * CMG21 keyword PIR client.
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public class Cmg21KwPirClient<T> extends AbstractKwPirClient<T> {
    /**
     * stream cipher
     */
    private final StreamCipher streamCipher;
    /**
     * ecc point compress encode
     */
    private final boolean compressEncode;
    /**
     * CMG21 keyword PIR params
     */
    private Cmg21KwPirParams params;
    /**
     * no stash cuckoo hash bin
     */
    private CuckooHashBin<ByteBuffer> cuckooHashBin;
    /**
     * β^{-1}
     */
    private BigInteger[] inverseBetas;
    /**
     * hash keys
     */
    private byte[][] hashKeys;

    public Cmg21KwPirClient(Rpc clientRpc, Party serverParty, Cmg21KwPirConfig config) {
        super(Cmg21KwPirPtoDesc.getInstance(), clientRpc, serverParty, config);
        compressEncode = config.getCompressEncode();
        streamCipher = StreamCipherFactory.createInstance(envType);
    }

    @Override
    public void init(KwPirParams kwPirParams, int labelByteLength) throws MpcAbortException {
        setInitInput(kwPirParams.maxRetrievalSize(), labelByteLength);
        logPhaseInfo(PtoState.INIT_BEGIN);

        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> hashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();

        stopWatch.start();
        assert (kwPirParams instanceof Cmg21KwPirParams);
        params = (Cmg21KwPirParams) kwPirParams;
        MpcAbortPreconditions.checkArgument(hashKeyPayload.size() == params.getCuckooHashKeyNum());
        hashKeys = hashKeyPayload.toArray(new byte[0][]);
        stopWatch.stop();
        long cuckooHashKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, cuckooHashKeyTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(int maxRetrievalSize, int labelByteLength) throws MpcAbortException {
        MathPreconditions.checkPositive("maxRetrievalSize", maxRetrievalSize);
        if (maxRetrievalSize > 1) {
            params = Cmg21KwPirParams.SERVER_1M_CLIENT_MAX_4096;
        } else {
            params = Cmg21KwPirParams.SERVER_1M_CLIENT_MAX_1;
        }
        setInitInput(params.maxRetrievalSize(), labelByteLength);
        logPhaseInfo(PtoState.INIT_BEGIN);

        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> hashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(hashKeyPayload.size() == params.getCuckooHashKeyNum());
        hashKeys = hashKeyPayload.toArray(new byte[0][]);
        stopWatch.stop();
        long cuckooHashKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, cuckooHashKeyTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Map<T, ByteBuffer> pir(Set<T> retrievalSet) throws MpcAbortException {
        setPtoInput(retrievalSet);
        logPhaseInfo(PtoState.PTO_BEGIN);
        // run MP-OPRF
        stopWatch.start();
        List<byte[]> blindPayload = generateBlindPayload();
        DataPacketHeader blindHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_BLIND.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(blindHeader, blindPayload));
        DataPacketHeader blindPrfHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_BLIND_PRF.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> blindPrfPayload = rpc.receive(blindPrfHeader).getPayload();
        List<ByteBuffer> keywordPrfs = handleBlindPrf(blindPrfPayload);
        Map<ByteBuffer, ByteBuffer> prfKeywordMap = IntStream.range(0, retrievalSize)
            .boxed()
            .collect(Collectors.toMap(keywordPrfs::get, i -> retrievalKeywordList.get(i), (a, b) -> b));
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 5, oprfTime, "Client runs OPRF");

        // cuckoo hash bin
        stopWatch.start();
        generateCuckooHashBin(keywordPrfs, params.getBinNum(), hashKeys);
        stopWatch.stop();
        long cuckooHashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 5, cuckooHashTime, "Client cuckoo hashes keys");

        stopWatch.start();
        // generate encryption params
        List<byte[]> encryptionParams = Cmg21KwPirNativeUtils.genEncryptionParameters(
            params.getPolyModulusDegree(), params.getPlainModulus(), params.getCoeffModulusBits()
        );
        DataPacketHeader encryptionParamsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_FHE_PARAMS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        List<byte[]> encryptionParamsPayload = encryptionParams.subList(0, 3);
        rpc.send(DataPacket.fromByteArrayList(encryptionParamsHeader, encryptionParamsPayload));
        stopWatch.stop();
        long keyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 5, keyGenTime, "Client generates FHE keys");

        stopWatch.start();
        // generate query
        List<byte[]> query = generateQuery(encryptionParams.get(0), encryptionParams.get(2),encryptionParams.get(3));
        DataPacketHeader clientQueryDataPacketHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientQueryDataPacketHeader, query));
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 5, genQueryTime, "Client generates query");

        DataPacketHeader keywordResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_ITEM_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> keywordResponsePayload = rpc.receive(keywordResponseHeader).getPayload();
        DataPacketHeader labelResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_LABEL_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> labelResponsePayload = rpc.receive(labelResponseHeader).getPayload();

        stopWatch.start();
        Map<T, ByteBuffer> pirResult = handleResponse(
            keywordResponsePayload, labelResponsePayload, prfKeywordMap, encryptionParams.get(0), encryptionParams.get(3)
        );
        stopWatch.stop();
        long decodeResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 5, 5, decodeResponseTime, "Client handles reply");

        logPhaseInfo(PtoState.PTO_END);
        return pirResult;
    }

    /**
     * handle server response.
     *
     * @param keywordResponse  keyword response.
     * @param labelResponse    label response.
     * @param prfKeywordMap    prf keyword map.
     * @param encryptionParams encryption params.
     * @param secretKey        secret key.
     * @return retrieval result map.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private Map<T, ByteBuffer> handleResponse(List<byte[]> keywordResponse, List<byte[]> labelResponse,
                                              Map<ByteBuffer, ByteBuffer> prfKeywordMap, byte[] encryptionParams,
                                              byte[] secretKey) throws MpcAbortException {
        int ciphertextNumber = params.getBinNum() / (params.getPolyModulusDegree() / params.getItemEncodedSlotSize());
        MpcAbortPreconditions.checkArgument(keywordResponse.size() % ciphertextNumber == 0);
        MpcAbortPreconditions.checkArgument(labelResponse.size() % ciphertextNumber == 0);
        Stream<byte[]> keywordResponseStream = keywordResponse.stream();
        keywordResponseStream = parallel ? keywordResponseStream.parallel() : keywordResponseStream;
        List<long[]> decryptedKeywordResponse = keywordResponseStream
            .map(i -> Cmg21KwPirNativeUtils.decodeReply(encryptionParams, secretKey, i))
            .collect(Collectors.toCollection(ArrayList::new));
        Stream<byte[]> labelResponseStream = labelResponse.stream();
        labelResponseStream = parallel ? labelResponseStream.parallel() : labelResponseStream;
        List<long[]> decryptedLabelResponse = labelResponseStream
            .map(i -> Cmg21KwPirNativeUtils.decodeReply(encryptionParams, secretKey, i))
            .collect(Collectors.toCollection(ArrayList::new));
        return recoverPirResult(decryptedKeywordResponse, decryptedLabelResponse, prfKeywordMap);
    }

    /**
     * generate query.
     *
     * @param encryptionParams encryption params.
     * @param publicKey        public key.
     * @param secretKey        secret key.
     * @return client query.
     */
    private List<byte[]> generateQuery(byte[] encryptionParams, byte[] publicKey, byte[] secretKey) {
        List<long[][]> encodedQueryList = encodeQuery();
        Stream<long[][]> encodedQueryStream = encodedQueryList.stream();
        encodedQueryStream = parallel ? encodedQueryStream.parallel() : encodedQueryStream;
        return encodedQueryStream
            .map(i -> Cmg21KwPirNativeUtils.generateQuery(encryptionParams, publicKey, secretKey, i))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    /**
     * recover PIR result.
     *
     * @param decryptedKeywordReply decrypted keyword response.
     * @param decryptedLabelReply   decrypted label response.
     * @param oprfMap               OPRF map.
     * @return PIR result.
     */
    private Map<T, ByteBuffer> recoverPirResult(List<long[]> decryptedKeywordReply, List<long[]> decryptedLabelReply,
                                                Map<ByteBuffer, ByteBuffer> oprfMap) {
        Map<T, ByteBuffer> resultMap = new HashMap<>(retrievalSize);
        int itemEncodedSlotSize = params.getItemEncodedSlotSize();
        int itemPerCiphertext = params.getPolyModulusDegree() / itemEncodedSlotSize;
        int ciphertextNum = params.getBinNum() / itemPerCiphertext;
        int itemPartitionNum = decryptedKeywordReply.size() / ciphertextNum;
        int labelPartitionNum = CommonUtils.getUnitNum((labelByteLength + CommonConstants.BLOCK_BYTE_LENGTH) * Byte.SIZE,
            (LongUtils.ceilLog2(params.getPlainModulus()) - 1) * itemEncodedSlotSize);
        int shiftBits = CommonUtils.getUnitNum((labelByteLength + CommonConstants.BLOCK_BYTE_LENGTH) * Byte.SIZE,
            itemEncodedSlotSize * labelPartitionNum);
        for (int i = 0; i < decryptedKeywordReply.size(); i++) {
            List<Integer> matchedItem = new ArrayList<>();
            for (int j = 0; j < itemEncodedSlotSize * itemPerCiphertext; j++) {
                if (decryptedKeywordReply.get(i)[j] == 0) {
                    matchedItem.add(j);
                }
            }
            for (int j = 0; j < matchedItem.size() - itemEncodedSlotSize + 1; j++) {
                if (matchedItem.get(j) % itemEncodedSlotSize == 0) {
                    if (matchedItem.get(j + itemEncodedSlotSize - 1) - matchedItem.get(j) == itemEncodedSlotSize - 1) {
                        int hashBinIndex = matchedItem.get(j) / itemEncodedSlotSize + (i / itemPartitionNum)
                            * itemPerCiphertext;
                        BigInteger label = BigInteger.ZERO;
                        int index = 0;
                        for (int l = 0; l < labelPartitionNum; l++) {
                            for (int k = 0; k < itemEncodedSlotSize; k++) {
                                BigInteger temp = BigInteger.valueOf(decryptedLabelReply.get(i * labelPartitionNum + l)[matchedItem.get(j + k)])
                                    .shiftLeft(shiftBits * index);
                                label = label.add(temp);
                                index++;
                            }
                        }
                        byte[] oprf = cuckooHashBin.getHashBinEntry(hashBinIndex).getItem().array();
                        byte[] keyBytes = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                        System.arraycopy(oprf, 0, keyBytes, 0, CommonConstants.BLOCK_BYTE_LENGTH);
                        byte[] ciphertextLabel = BigIntegerUtils.nonNegBigIntegerToByteArray(
                            label, labelByteLength + CommonConstants.BLOCK_BYTE_LENGTH
                        );
                        byte[] plaintextLabel = streamCipher.ivDecrypt(keyBytes, ciphertextLabel);
                        resultMap.put(
                            byteArrayObjectMap.get(oprfMap.get(cuckooHashBin.getHashBinEntry(hashBinIndex).getItem())),
                            ByteBuffer.wrap(plaintextLabel)
                        );
                        j = j + itemEncodedSlotSize - 1;
                    }
                }
            }
        }
        return resultMap;
    }

    /**
     * generate cuckoo hash bin.
     *
     * @param itemList item list.
     * @param binNum   bin num.
     * @param hashKeys hash keys.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private void generateCuckooHashBin(List<ByteBuffer> itemList, int binNum, byte[][] hashKeys)
        throws MpcAbortException {
        cuckooHashBin = createCuckooHashBin(envType, params.getCuckooHashBinType(), retrievalSize, binNum, hashKeys);
        boolean success = false;
        cuckooHashBin.insertItems(itemList);
        if (cuckooHashBin.itemNumInStash() == 0) {
            success = true;
        }
        cuckooHashBin.insertPaddingItems(botElementByteBuffer);
        MpcAbortPreconditions.checkArgument(success, "cuckoo hash failed.");
    }

    /**
     * encode query.
     *
     * @return encoded query.
     */
    public List<long[][]> encodeQuery() {
        int itemEncodedSlotSize = params.getItemEncodedSlotSize();
        int itemPerCiphertext = params.getPolyModulusDegree() / itemEncodedSlotSize;
        int ciphertextNum = params.getBinNum() / itemPerCiphertext;
        long[][] items = new long[ciphertextNum][params.getPolyModulusDegree()];
        for (int i = 0; i < ciphertextNum; i++) {
            for (int j = 0; j < itemPerCiphertext; j++) {
                long[] item = params.getHashBinEntryEncodedArray(
                    cuckooHashBin.getHashBinEntry(i * itemPerCiphertext + j), true, secureRandom
                );
                System.arraycopy(item, 0, items[i], j * itemEncodedSlotSize, itemEncodedSlotSize);
            }
            for (int j = itemPerCiphertext * itemEncodedSlotSize; j < params.getPolyModulusDegree(); j++) {
                items[i][j] = 0;
            }
        }
        IntStream ciphertextStream = IntStream.range(0, ciphertextNum);
        ciphertextStream = parallel ? ciphertextStream.parallel() : ciphertextStream;
        return ciphertextStream
            .mapToObj(i -> computePowers(items[i], params.getPlainModulus(), params.getQueryPowers()))
            .collect(Collectors.toList());
    }

    /**
     * generate blind elements.
     *
     * @return blind elements.
     */
    private List<byte[]> generateBlindPayload() {
        Ecc ecc = EccFactory.createInstance(envType);
        BigInteger n = ecc.getN();
        inverseBetas = new BigInteger[retrievalKeywordList.size()];
        IntStream retrievalIntStream = IntStream.range(0, retrievalKeywordList.size());
        retrievalIntStream = parallel ? retrievalIntStream.parallel() : retrievalIntStream;
        return retrievalIntStream
            .mapToObj(index -> {
                // generate blind factor
                BigInteger beta = BigIntegerUtils.randomPositive(n, secureRandom);
                inverseBetas[index] = beta.modInverse(n);
                // hash to point
                ECPoint element = ecc.hashToCurve(retrievalKeywordList.get(index).array());
                // blinding
                return ecc.multiply(element, beta);
            })
            .map(element -> ecc.encode(element, compressEncode))
            .collect(Collectors.toList());
    }

    /**
     * handle blind elements PRF.
     *
     * @param blindPrf blind elements PRF.
     * @return elements PRF.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private List<ByteBuffer> handleBlindPrf(List<byte[]> blindPrf) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(blindPrf.size() == retrievalKeywordList.size());
        Kdf kdf = KdfFactory.createInstance(envType);
        Prg prg = PrgFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH * 2);
        byte[][] blindPrfArray = blindPrf.toArray(new byte[0][]);
        Ecc ecc = EccFactory.createInstance(envType);
        IntStream batchIntStream = IntStream.range(0, retrievalSize);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        return batchIntStream
            .mapToObj(index -> {
                // decode
                ECPoint element = ecc.decode(blindPrfArray[index]);
                // handle blind
                return ecc.multiply(element, inverseBetas[index]);
            })
            .map(element -> ecc.encode(element, false))
            .map(kdf::deriveKey)
            .map(prg::extendToBytes)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * compute powers.
     *
     * @param base      base.
     * @param modulus   modulus.
     * @param exponents exponents.
     * @return powers.
     */
    private long[][] computePowers(long[] base, long modulus, int[] exponents) {
        Zp64 zp64 = Zp64Factory.createInstance(envType, modulus);
        long[][] result = new long[exponents.length][];
        assert exponents[0] == 1;
        result[0] = base;
        for (int i = 1; i < exponents.length; i++) {
            long[] temp = new long[base.length];
            for (int j = 0; j < base.length; j++) {
                temp[j] = zp64.pow(base[j], exponents[i]);
            }
            result[i] = temp;
        }
        return result;
    }
}