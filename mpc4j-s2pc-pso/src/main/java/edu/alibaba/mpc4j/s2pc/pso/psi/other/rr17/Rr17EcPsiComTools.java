package edu.alibaba.mpc4j.s2pc.pso.psi.other.rr17;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.commit.Commit;
import edu.alibaba.mpc4j.common.tool.crypto.commit.CommitFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.PhaseHashBin;
import edu.alibaba.mpc4j.common.tool.utils.*;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;

import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;


public class Rr17EcPsiComTools {
    /**
     * PEQT哈希函数
     */
    public Hash peqtHash;
    /**
     * Input哈希函数
     */
    public Hash h1;
    /**
     * 布谷鸟哈希桶所用的哈希函数
     */
    public PhaseHashBin phaseHashBin;
    /**
     * phase哈希桶个数
     */
    public int binNum;
    /**
     * 哈希桶maxsize
     */
    public int binSize;
    /**
     * LOT input Length
     */
    public int encodeInputByteLength;
    /**
     * Enc PRF
     */
    public Prf prfEnc;
    /**
     * Tag PRF
     */
    public Prf prfTag;
    /**
     * commit functions
     */
    public Commit[] commits;
    /**
     * Tag PRF输出的byte长度
     */
    public int tagPrfByteLength;

    public Rr17EcPsiComTools(EnvType envType, int maxServerElementSize, int maxClientElementSize, byte[][] hashKeys,
                             int binNum, int binSize, int clientElementSize, boolean parallel){
        int maxItemSize = Math.max(maxClientElementSize, maxServerElementSize);
        int l = PsiUtils.getMaliciousPeqtByteLength(maxServerElementSize, maxClientElementSize);
        int peqtByteLength = CommonConstants.STATS_BYTE_LENGTH +
            CommonUtils.getByteLength(2 * LongUtils.ceilLog2(Math.max(2, binSize * clientElementSize)));
        tagPrfByteLength = getTagEncByteLength(maxClientElementSize, maxServerElementSize);
        encodeInputByteLength = CommonUtils.getByteLength(l * Byte.SIZE - (int) Math.round(Math.floor(DoubleUtils.log2(binNum))));

        h1 = HashFactory.createInstance(envType, l);
        prfEnc =  PrfFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
        prfEnc.setKey(hashKeys[1]);
        prfTag = PrfFactory.createInstance(envType, tagPrfByteLength);
        prfTag.setKey(hashKeys[2]);
        peqtHash = HashFactory.createInstance(envType, peqtByteLength);
        phaseHashBin = new PhaseHashBin(envType, binNum, maxItemSize, hashKeys[0]);

        int prfNum = parallel ? ForkJoinPool.getCommonPoolParallelism() : 1;
        commits = IntStream.range(0, prfNum).mapToObj(i -> CommitFactory.createInstance(envType)).toArray(Commit[]::new);
    }

    public static int getTagEncByteLength(int maxClientElementSize, int maxServerElementSize){
//        return CommonConstants.BLOCK_BYTE_LENGTH;
        return CommonConstants.STATS_BYTE_LENGTH + CommonUtils.getByteLength(64 + LongUtils.ceilLog2(maxClientElementSize));
    }
}
