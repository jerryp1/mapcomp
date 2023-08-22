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

public abstract class AbstractMppsiLeader<T> extends AbstractMultiPartyPto implements MppsiLeader<T> {
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
    protected ArrayList<T> leaderElementArrayList;
    /**
     * 客户端元素数量
     */
    protected int[] clientElementSizes;
    /**
     * 服务端元素数量
     */
    protected int leaderElementSize;
    protected AbstractMppsiLeader(PtoDesc ptoDesc, MultiPartyPtoConfig config, Rpc rpc, Party... otherParties) {
        super(ptoDesc, config, rpc, otherParties);
    }

    protected void setInitInput(int maxLeaderElementSize, int[] maxClientElementSizes) {
        MathPreconditions.checkPositive("maxLeaderElementSize", maxLeaderElementSize);
        this.maxLeaderElementSize = maxLeaderElementSize;
        Arrays.stream(maxClientElementSizes).forEach(x -> MathPreconditions.checkPositive("maxClientElementSize", x));
        this.maxClientElementSizes = maxClientElementSizes;
        initState();
    }

    protected void setPtoInput(Set<T> leaderElementSet, int[] clientElementSizes) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("leaderElementSize", leaderElementSet.size(), maxLeaderElementSize);
        leaderElementSize = leaderElementSet.size();
        leaderElementArrayList = new ArrayList<>(leaderElementSet);
        MathPreconditions.checkEqual("num of init clients", "num of running clients", maxClientElementSizes.length, clientElementSizes.length);
        IntStream.range(0, clientElementSizes.length).forEach(i ->
            MathPreconditions.checkPositiveInRangeClosed("clientElementSize[" + i + "]", clientElementSizes[i], maxClientElementSizes[i]));
        this.clientElementSizes = clientElementSizes;
        extraInfo++;
    }

}
