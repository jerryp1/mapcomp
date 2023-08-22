package edu.alibaba.mpc4j.s2pc.pso.mppsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPto;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.IntStream;

public abstract class AbstractMppsiClient<T> extends AbstractMultiPartyPto implements MppsiClient<T> {
    /**
     * 当前客户端在所有客户端中所处的位序，从0开始计数
     */
    private int selfIndex;
    /**
     * 客户端最大元素数量
     */
    private int[] maxClientElementSizes;
    /**
     * 服务端最大元素数量
     */
    private int maxLeaderElementSize;
    /**
     * 客户端元素集合
     */
    protected ArrayList<T> clientElementArrayList;
    /**
     * 客户端元素数量
     */
    protected int[] clientElementSizes;
    /**
     * 服务端元素数量
     */
    protected int leaderElementSize;
    protected AbstractMppsiClient(PtoDesc ptoDesc, MultiPartyPtoConfig config, Rpc rpc, Party... otherParties) {
        super(ptoDesc, config, rpc, otherParties);
    }

    protected void setInitInput(int[] maxClientElementSizes, int maxLeaderElementSize, int selfIndex) throws MpcAbortException {
        Arrays.stream(maxClientElementSizes).forEach(x -> MathPreconditions.checkPositive("maxClientElementSize", x));
        this.maxClientElementSizes = maxClientElementSizes;
        MathPreconditions.checkPositive("maxLeaderElementSize", maxLeaderElementSize);
        this.maxLeaderElementSize = maxLeaderElementSize;
        MathPreconditions.checkInRange("selfIndex", selfIndex, 0, maxClientElementSizes.length - 1);
        this.selfIndex = selfIndex;
        initState();
    }

    protected void setPtoInput(Set<T> clientElementSet, int leaderElementSize, int[] allClientElementSizes) throws MpcAbortException {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("clientElementSize", clientElementSet.size(), maxClientElementSizes[selfIndex]);
        MathPreconditions.checkEqual("num of init clients", "num of running clients", maxClientElementSizes.length, allClientElementSizes.length);
        IntStream.range(0, allClientElementSizes.length).forEach(i ->
            MathPreconditions.checkPositiveInRangeClosed("clientElementSize[" + i + "]", allClientElementSizes[i], maxClientElementSizes[i]));
        clientElementSizes = allClientElementSizes;
        clientElementArrayList = new ArrayList<>(clientElementSet);
        MathPreconditions.checkPositiveInRangeClosed("leaderElementSize", leaderElementSize, maxLeaderElementSize);
        this.leaderElementSize = leaderElementSize;
        extraInfo++;
    }
}
