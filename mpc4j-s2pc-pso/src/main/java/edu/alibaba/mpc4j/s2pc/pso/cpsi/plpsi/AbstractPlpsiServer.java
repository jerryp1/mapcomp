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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

/**
 * abstract payload-circuit PSI server.
 *
 * @author Feng Han
 * @date 2023/10/20
 */
public abstract class AbstractPlpsiServer<T, X> extends AbstractTwoPartyPto implements PlpsiServer<T, X> {
    /**
     * max server element size
     */
    private int maxServerElementSize;
    /**
     * max client element size
     */
    private int maxClientElementSize;
    /**
     * server element array list
     */
    protected ArrayList<T> serverElementArrayList;
    /**
     * server element size
     */
    protected int serverElementSize;
    /**
     * client element size
     */
    protected int clientElementSize;
    /**
     * origin position for each element
     */
    protected HashMap<T, Integer> hashMap;
    /**
     * transform payload into byte array
     */
    protected byte[][][] payloadBytes;
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

    protected AbstractPlpsiServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, PlpsiConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
    }

    protected void setInitInput(int maxServerElementSize, int maxClientElementSize) {
        MathPreconditions.checkPositive("maxServerElementSize", maxServerElementSize);
        this.maxServerElementSize = maxServerElementSize;
        MathPreconditions.checkPositive("maxClientElementSize", maxClientElementSize);
        this.maxClientElementSize = maxClientElementSize;
        initState();
    }


    protected void setPtoInput(List<T> serverElementList, int clientElementSize) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("serverElementSize", serverElementList.size(), maxServerElementSize);
        serverElementSize = serverElementList.size();
        serverElementArrayList = new ArrayList<>(serverElementList);
        MathPreconditions.checkPositiveInRangeClosed("clientElementSize", clientElementSize, maxClientElementSize);
        this.clientElementSize = clientElementSize;
        hashMap = new HashMap<>();
        IntStream.range(0, serverElementSize).forEach(i -> hashMap.put(serverElementList.get(i), i));
        extraInfo++;
    }

    protected void setPayload(List<List<X>> serverPayloadLists, int[] payloadBitLs, boolean[] isBinaryShare){
        MathPreconditions.checkEqual("serverPayloadLists.size()", "payloadBitLs.length", serverPayloadLists.size(), payloadBitLs.length);
        for (List<X> serverPayloadList : serverPayloadLists) {
            MathPreconditions.checkEqual("serverPayloadLists.get(i).size()", "serverElementSize", serverPayloadList.size(), serverElementSize);
        }
        int columnNum = serverPayloadLists.size();
        this.payloadBitLs = payloadBitLs;
        payloadByteLs = Arrays.stream(payloadBitLs).map(CommonUtils::getByteLength).toArray();
        payloadBytes = new byte[serverElementSize][columnNum][];
        for(int i = 0; i < serverElementSize; i++){
            for(int j = 0; j < columnNum; j++){
                payloadBytes[i][j] = BytesUtils.fixedByteArrayLength(ObjectUtils.objectToByteArray(serverPayloadLists.get(j).get(i)), payloadByteLs[j]);
            }
        }
        this.isBinaryShare = isBinaryShare;
    }

}
