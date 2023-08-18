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

public class Prty19LowMpOprfSenderOutput implements MpOprfSenderOutput {
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
    private Hash h1;
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
    private final Gf2eDokvs<ByteBuffer> okvs;
    /**
     * 伪随机函数输出字节长度
     */
    private final int prfByteLength;

    Prty19LowMpOprfSenderOutput(EnvType envType, int batchSize, byte[][] qs, byte[] s, byte[][] storage, Gf2eDokvsType type, byte[][] keys, boolean parallel) {
        assert batchSize > 0 : "BatchSize must be greater than 0: " + batchSize;
        this.batchSize = batchSize;
        this.s = s;
        this.l = Prty19MpOprfUtils.getL(batchSize);
        assert s.length == CommonUtils.getByteLength(l);
        this.prfByteLength = s.length;
        h1 = HashFactory.createInstance(HashFactory.HashType.BC_SHA3_512, prfByteLength);
        this.storage = storage;
        this.parallel = parallel;
        // OKVS init
        int n = (Gf2eDokvsFactory.isBinary(type) || (batchSize > 1)) ? batchSize: 2;
        this.okvs = Gf2eDokvsFactory.createInstance(envType, type, n, this.prfByteLength * Byte.SIZE, keys);
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
    public byte[] getPrf(byte[] input) {
        byte[] extendedInput = h1.digestToBytes(input);
        // compute Q(x)
        IntStream binaryStream = IntStream.range(0, l);
        binaryStream = parallel ? binaryStream.parallel() : binaryStream;
        boolean[] extendPrf = new boolean[prfByteLength * Byte.SIZE];
        binaryStream.forEach(index -> {
                    extendPrf[index] = fList.get(index).getBoolean(extendedInput);
        });
        byte[] qx = BinaryUtils.binaryToByteArray(extendPrf);
        // Decode计算p(x)
        byte[] px = okvs.decode(storage, ByteBuffer.wrap(extendedInput));
        BytesUtils.andi(px,s);
        return BytesUtils.xor(qx,px);
    }

    @Override
    public int getPrfByteLength() {
        return prfByteLength;
    }

    @Override
    public int getBatchSize() {
        return batchSize;
    }
}