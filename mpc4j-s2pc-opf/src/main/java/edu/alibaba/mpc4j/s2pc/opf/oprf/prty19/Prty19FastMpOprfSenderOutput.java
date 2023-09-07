package edu.alibaba.mpc4j.s2pc.opf.oprf.prty19;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvs;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfSenderOutput;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Prty19FastMpOprfSenderOutput implements MpOprfSenderOutput {
    /**
     * isparallel
     */
    private final boolean parallel;
    /**
     * 批处理数量
     */
    private final int batchSize;
    /**
     * l
     */
    private final int l;
    /**
     * input hash
     */
    private final Hash h1;
    /**
     * binNum
     */
    private final int binNum;
    /**
     * HashBin keys
     */
    byte[][] hashBinKeys;
    /**
     * EnvType
     */
    private EnvType envType;
    /**
     * 关联密钥
     */
    private final byte[][] qs;
    /**
     * okvs storage
     */
    private final byte[][] storage;
    /**
     * delta
     */
    private final byte[] s;
    /**
     * PRF: F: {0,1}^λ × {0,1}^* → [0,1]
     */
    private final List<Prf> fList;
    /**
     * OKVS
     */
    private final List<Gf2eDokvs<ByteBuffer>> okvsList;
    /**
     * 伪随机函数输出字节长度
     */
    private final int prfByteLength;

    Prty19FastMpOprfSenderOutput(EnvType envType, int batchSize, byte[][] hashBinKeys, int binNum, int maxBinSize,
                                 byte[][] qs, byte[] s, byte[][] storage, Gf2eDokvsType type, byte[][] keys, boolean parallel) {
        assert batchSize > 0 : "BatchSize must be greater than 0: " + batchSize;
        this.batchSize = batchSize;
        this.s = s;
        this.l = Prty19MpOprfUtils.getL(batchSize);
        assert s.length == CommonUtils.getByteLength(l);
        this.prfByteLength = s.length;
        h1 = HashFactory.createInstance(HashFactory.HashType.BC_SHA3_512, prfByteLength);
        this.storage = storage;
        this.parallel = parallel;
        this.hashBinKeys = hashBinKeys;
        this.envType = envType;
        // OKVS init
        int hashNum = Gf2eDokvsFactory.getHashKeyNum(type);
        this.binNum = binNum;

        Gf2eDokvs<ByteBuffer>[] tmpOkvsArray = new Gf2eDokvs[binNum];
        IntStream binStream = IntStream.range(0, binNum);
        binStream = parallel ? binStream.parallel() : binStream;
        binStream.forEach(index -> tmpOkvsArray[index] = Gf2eDokvsFactory.createInstance(envType, type, maxBinSize,
            this.prfByteLength * Byte.SIZE, Arrays.copyOfRange(keys, index * hashNum, (index + 1) * hashNum)));
        okvsList = Arrays.stream(tmpOkvsArray).collect(Collectors.toList());
//        IntStream binStream = IntStream.range(0, binNum);
//        binStream = parallel ? binStream.parallel() : binStream;
//        okvsList = binStream.mapToObj(index ->
//                        Gf2eDokvsFactory.createInstance(envType, type, maxBinSize, this.prfByteLength * Byte.SIZE, Arrays.copyOfRange(keys, index * hashNum, (index + 1) * hashNum)))
//                .collect(Collectors.toList());
        this.qs = Arrays.stream(qs)
                .peek(q -> {
                    assert q.length == CommonConstants.BLOCK_BYTE_LENGTH;
                })
                .map(BytesUtils::clone)
                .toArray(byte[][]::new);
        // 初始化伪随机函数
        IntStream initPrfStream = IntStream.range(0,l);
        initPrfStream = parallel ? initPrfStream.parallel() : initPrfStream;
        // 初始化伪随机函数
        this.fList = initPrfStream.mapToObj(index -> PrfFactory.createInstance(envType, 1)).collect(Collectors.toList());
        IntStream keyPrfStream = IntStream.range(0,l);
        keyPrfStream = parallel ? keyPrfStream.parallel() : keyPrfStream;
        keyPrfStream.forEach(index -> {
            fList.get(index).setKey(this.qs[index]);
        });
    }


    @Override
    public byte[] getPrf(int index, byte[] input) {
        // compute Q(x)
        IntStream binaryStream = IntStream.range(0, l);
        binaryStream = parallel ? binaryStream.parallel() : binaryStream;
        byte[] extendInput = h1.digestToBytes(ByteBuffer.allocate(prfByteLength + 1).put(h1.digestToBytes(input)).put(Integer.valueOf(index).byteValue()).array());
        boolean[] extendPrf = new boolean[prfByteLength * Byte.SIZE];
        binaryStream.forEach(bitIndex -> {
            extendPrf[bitIndex] = fList.get(bitIndex).getBoolean(extendInput);
        });
        byte[] qx = BinaryUtils.binaryToByteArray(extendPrf);
        // Decode计算p(x)
        Prf hashPrf = PrfFactory.createInstance(envType, Integer.BYTES);
        hashPrf.setKey(hashBinKeys[index]);
        int binIndex = hashPrf.getInteger(h1.digestToBytes(input), binNum);
        byte[] px = okvsList.get(binIndex).decode(
            Arrays.copyOfRange(storage,binIndex * (storage.length / binNum), (binIndex + 1) * (storage.length / binNum)),
            ByteBuffer.wrap(h1.digestToBytes(ByteBuffer.allocate(prfByteLength + 1)
                .put(h1.digestToBytes(input))
                .put(Integer.valueOf(index).byteValue()).
                array())));
        BytesUtils.andi(px,s);
        return BytesUtils.xor(qx,px);
    }

    @Override
    public byte[] getPrf (byte[] input) { return getPrf(0, input); }

    @Override
    public int getPrfByteLength() {
        return prfByteLength;
    }

    @Override
    public int getBatchSize() {
        return batchSize;
    }
}
