package edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * abstract payload-circuit PSI client.
 *
 * @author Feng Han
 * @date 2023/10/20
 */
public abstract class AbstractPlpsiClient<T> extends AbstractTwoPartyPto implements PlpsiClient<T> {
    /**
     * max client element size
     */
    private int maxClientElementSize;
    /**
     * max server element size
     */
    private int maxServerElementSize;
    /**
     * client element array list
     */
    protected ArrayList<T> clientElementArrayList;
    /**
     * client element size
     */
    protected int clientElementSize;
    /**
     * sever element size
     */
    protected int serverElementSize;
    /**
     * payload bit lengths
     */
    protected int[] payloadBitLs;
    /**
     * payload byte lengths
     */
    protected int[] payloadByteLs;
    /**
     * whether the corresponding payload needs to be binary shared
     */
    protected boolean[] isBinaryShare;

    protected AbstractPlpsiClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, PlpsiConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
    }

    protected void setInitInput(int maxClientElementSize, int maxServerElementSize) {
        MathPreconditions.checkPositive("maxClientElementSize", maxClientElementSize);
        this.maxClientElementSize = maxClientElementSize;
        MathPreconditions.checkPositive("maxServerElementSize", maxServerElementSize);
        this.maxServerElementSize = maxServerElementSize;
        initState();
    }

    protected void setPtoInput(List<T> clientElementList, int serverElementSize) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("clientElementSize", clientElementList.size(), maxClientElementSize);
        clientElementSize = clientElementList.size();
        clientElementArrayList = new ArrayList<>(clientElementList);
        MathPreconditions.checkPositiveInRangeClosed("serverElementSize", serverElementSize, maxServerElementSize);
        this.serverElementSize = serverElementSize;
        extraInfo++;
    }

    protected void setPayload(int[] payloadBitLs, boolean[] isBinaryShare){
        MathPreconditions.checkEqual("payloadBitLs.length", "isBinaryShare.length", payloadBitLs.length, isBinaryShare.length);
        this.payloadBitLs = payloadBitLs;
        payloadByteLs = Arrays.stream(payloadBitLs).map(CommonUtils::getByteLength).toArray();
        this.isBinaryShare = isBinaryShare;
    }
}
