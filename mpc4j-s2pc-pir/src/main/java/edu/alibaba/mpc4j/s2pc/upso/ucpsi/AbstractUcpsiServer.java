package edu.alibaba.mpc4j.s2pc.upso.ucpsi;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * abstract unbalanced circuit PSI server.
 *
 * @author Liqiang Peng
 * @date 2023/4/17
 */
public abstract class AbstractUcpsiServer extends AbstractTwoPartyPto implements UcpsiServer {
    /**
     * max client element size
     */
    protected int maxClientElementSize;
    /**
     * server element list
     */
    protected ArrayList<ByteBuffer> serverElementArrayList;
    /**
     * 服务端元素数量
     */
    protected int serverElementSize;
    /**
     * 特殊空元素字节缓存区
     */
    protected ByteBuffer botElementByteBuffer;

    protected AbstractUcpsiServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, UcpsiConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
    }

    protected void setInitInput(Set<ByteBuffer> serverElementSet, int maxClientElementSize) {
        MathPreconditions.checkPositive("maxServerElementSize", serverElementSet.size());
        this.serverElementSize = serverElementSet.size();
        byte[] botElementByteArray = new byte[CommonConstants.STATS_BYTE_LENGTH];
        Arrays.fill(botElementByteArray, (byte)0xFF);
        botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
        serverElementArrayList = serverElementSet.stream()
            .peek(xi -> Preconditions.checkArgument(!xi.equals(botElementByteBuffer), "xi must not equal ⊥"))
            .collect(Collectors.toCollection(ArrayList::new));
        MathPreconditions.checkPositive("maxClientElementSize", maxClientElementSize);
        this.maxClientElementSize = maxClientElementSize;
        initState();
    }

    protected void setPtoInput() {
        checkInitialized();
        extraInfo++;
    }
}
