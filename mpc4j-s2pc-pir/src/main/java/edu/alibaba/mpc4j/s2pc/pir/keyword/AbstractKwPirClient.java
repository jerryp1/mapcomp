package edu.alibaba.mpc4j.s2pc.pir.keyword;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * abstract keyword PIR client.
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public abstract class AbstractKwPirClient<T> extends AbstractTwoPartyPto implements KwPirClient<T> {
    /**
     * max client retrieval size
     */
    protected int maxRetrievalSize;
    /**
     * label byte length
     */
    protected int labelByteLength;
    /**
     * bot element bytebuffer
     */
    protected ByteBuffer botElementByteBuffer;
    /**
     * client retrieval keyword list
     */
    protected List<ByteBuffer> retrievalKeywordList;
    /**
     * bytebuffer object map
     */
    protected Map<ByteBuffer, T> byteArrayObjectMap;
    /**
     * client retrieval size
     */
    protected int retrievalSize;

    protected AbstractKwPirClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, KwPirConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
    }

    protected void setInitInput(int maxRetrievalSize, int labelByteLength) {
        MathPreconditions.checkPositive("labelByteLength", labelByteLength);
        this.labelByteLength = labelByteLength;
        MathPreconditions.checkPositive("maxRetrievalSize", maxRetrievalSize);
        this.maxRetrievalSize = maxRetrievalSize;
        byte[] botElementByteArray = new byte[CommonConstants.STATS_BYTE_LENGTH];
        Arrays.fill(botElementByteArray, (byte)0xFF);
        botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
        initState();
    }

    protected void setPtoInput(Set<T> clientKeywordSet) {
        checkInitialized();
        retrievalSize = clientKeywordSet.size();
        MathPreconditions.checkPositiveInRangeClosed("retrievalSize", retrievalSize, maxRetrievalSize);
        retrievalKeywordList = clientKeywordSet.stream()
            .map(ObjectUtils::objectToByteArray)
            .map(ByteBuffer::wrap)
            .peek(yi -> Preconditions.checkArgument(!yi.equals(botElementByteBuffer), "yi must not equal ⊥"))
            .collect(Collectors.toCollection(ArrayList::new));
        byteArrayObjectMap = new HashMap<>(retrievalSize);
        clientKeywordSet.forEach(clientElementObject ->
            byteArrayObjectMap.put(
                ByteBuffer.wrap(ObjectUtils.objectToByteArray(clientElementObject)), clientElementObject
            )
        );
        extraInfo++;
    }
}
