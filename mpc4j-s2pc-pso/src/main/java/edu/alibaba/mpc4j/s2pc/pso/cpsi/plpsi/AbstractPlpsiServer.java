package edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

/**
 * abstract payload-circuit PSI server.
 *
 * @author Feng Han
 * @date 2023/10/20
 */
public abstract class AbstractPlpsiServer<T> extends AbstractTwoPartyPto implements PlpsiServer<T> {
    /**
     * whether the sharing type of payload is binary
     */
    protected final boolean isBinaryShare;
    /**
     * max server element size
     */
    private int maxServerElementSize;
    /**
     * max client element size
     */
    private int maxClientElementSize;
    /**
     * max server payload bit length
     */
    protected int serverPayloadBitL;
    /**
     * server element array list
     */
    protected ArrayList<T> serverElementArrayList;
    /**
     * server payload array list
     */
    protected ArrayList<T> serverPayloadArrayList;
    /**
     * server element size
     */
    protected int serverElementSize;
    /**
     * client element size
     */
    protected int clientElementSize;
    protected HashMap<T, byte[]> hashMap;

    protected AbstractPlpsiServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, PlpsiConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
        isBinaryShare = config.isBinaryShare();
    }

    protected void setInitInput(int maxServerElementSize, int maxClientElementSize, int serverPayloadBitL) {
        MathPreconditions.checkPositive("maxServerElementSize", maxServerElementSize);
        this.maxServerElementSize = maxServerElementSize;
        MathPreconditions.checkPositive("maxClientElementSize", maxClientElementSize);
        this.maxClientElementSize = maxClientElementSize;
        MathPreconditions.checkGreaterOrEqual("serverPayloadBitL", serverPayloadBitL, 0);
        this.serverPayloadBitL = serverPayloadBitL;
        initState();
    }

    protected void setPtoInput(List<T> serverElementList, List<T> serverPayloadList, int clientElementSize) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("serverElementSize", serverElementList.size(), maxServerElementSize);
        serverElementSize = serverElementList.size();
        serverElementArrayList = new ArrayList<>(serverElementList);
        MathPreconditions.checkPositiveInRangeClosed("clientElementSize", clientElementSize, maxClientElementSize);
        this.clientElementSize = clientElementSize;
        if(serverPayloadList != null){
            MathPreconditions.checkEqual("serverElementList.size()", "serverPayloadList.size()", serverElementList.size(), serverPayloadList.size());
            serverPayloadArrayList = new ArrayList<>(serverPayloadList);
            hashMap = new HashMap<>();
            int byteL = CommonUtils.getByteLength(serverPayloadBitL);
            IntStream.range(0, serverElementSize).forEach(i ->
                hashMap.put(serverElementList.get(i), BytesUtils.paddingByteArray(ObjectUtils.objectToByteArray(serverPayloadList.get(i)), byteL)));
        }
        extraInfo++;
    }
}
