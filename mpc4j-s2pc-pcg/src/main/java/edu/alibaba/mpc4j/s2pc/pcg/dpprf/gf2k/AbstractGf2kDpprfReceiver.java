package edu.alibaba.mpc4j.s2pc.pcg.dpprf.gf2k;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * GF2K-DPPRF接收方抽象类。
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public abstract class AbstractGf2kDpprfReceiver extends AbstractSecureTwoPartyPto implements Gf2kDpprfReceiver {
    /**
     * 配置项
     */
    private final Gf2kDpprfConfig config;
    /**
     * 最大α上界
     */
    protected int maxAlphaBound;
    /**
     * 最大l取值
     */
    protected int maxL;
    /**
     * 最大批处理数量
     */
    private int maxBatchNum;
    /**
     * α上界
     */
    protected int alphaBound;
    /**
     * α数组
     */
    protected int[] alphaArray;
    /**
     * α比特值数组
     */
    protected boolean[][] alphaBinaryArray;
    /**
     * 非α比特值数组
     */
    protected boolean[][] notAlphaBinaryArray;
    /**
     * l取值
     */
    protected int l;
    /**
     * 批处理数量
     */
    protected int batchNum;

    protected AbstractGf2kDpprfReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, Gf2kDpprfConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    @Override
    public Gf2kDpprfFactory.Gf2kDpprfType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxBatchNum, int maxAlphaBound) {
        assert maxBatchNum > 0: "maxBatchNum must be greater than 0:" + maxBatchNum;
        this.maxBatchNum = maxBatchNum;
        assert maxAlphaBound > 0 : "maxAlphaBound must be greater than 0: " + maxAlphaBound;
        this.maxAlphaBound = maxAlphaBound;
        maxL = LongUtils.ceilLog2(maxAlphaBound);
        initialized = false;
    }

    protected void setPtoInput(int[] alphaArray, int alphaBound) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert alphaBound > 0 && alphaBound <= maxAlphaBound
            : "alphaBound must be in range (0, " + maxAlphaBound + "]: " + alphaBound;
        this.alphaBound = alphaBound;
        this.l = LongUtils.ceilLog2(alphaBound);
        batchNum = alphaArray.length;
        assert batchNum > 0 && batchNum <= maxBatchNum : "batch must be in range (0, " + maxBatchNum + "]: " + batchNum;
        this.alphaArray = Arrays.stream(alphaArray)
            .peek(alpha -> {
                assert alpha >= 0 && alpha < alphaBound : "α must be in range [0, " + alphaBound + "): " + alpha;
            })
            .toArray();
        int offset = Integer.SIZE - l;
        alphaBinaryArray = new boolean[batchNum][l];
        notAlphaBinaryArray = new boolean[batchNum][l];
        IntStream.range(0, batchNum).forEach(index -> {
            int alpha = alphaArray[index];
            // 将α展开成二进制
            byte[] alphaBytes = IntUtils.intToByteArray(alpha);
            IntStream.range(0, l).forEach(i -> {
                alphaBinaryArray[index][i] = BinaryUtils.getBoolean(alphaBytes, offset + i);
                notAlphaBinaryArray[index][i] = !alphaBinaryArray[index][i];
            });
        });
        extraInfo++;
    }

    protected void setPtoInput(int[] alphaArray, int alphaBound, CotReceiverOutput preReceiverOutput) {
        setPtoInput(alphaArray, alphaBound);
        assert preReceiverOutput.getNum() >= Gf2kDpprfFactory.getPrecomputeNum(config, batchNum, alphaBound);
    }
}
