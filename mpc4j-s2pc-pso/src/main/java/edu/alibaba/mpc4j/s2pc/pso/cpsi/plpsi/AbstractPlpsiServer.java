package edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

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
    protected HashMap<T, Integer> hashMap;

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
//        if (serverPayloadLists != null) {
//            MathPreconditions.checkEqual("serverPayloadLists.length", "serverPayloadBitLs.length", serverPayloadLists.length, serverPayloadBitLs.length);
//            serverPayloadArrays = IntStream.range(0, serverPayloadBitLs.length).mapToObj(i -> {
//                MathPreconditions.checkEqual("serverElementList.size()", "serverPayloadList.size()", serverElementList.size(), serverPayloadLists[i].size());
//                int byteL = CommonUtils.getByteLength(serverPayloadBitLs[i]);
//                return serverPayloadLists[i].stream().map(x -> BytesUtils.paddingByteArray(ObjectUtils.objectToByteArray(x), byteL)).toArray(byte[][]::new);
//            }).toArray(byte[][][]::new);
//        }
        extraInfo++;
    }

}
