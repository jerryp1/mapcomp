package edu.alibaba.mpc4j.s2pc.pir.cppir.keyword;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * abstract Single Keyword Client-specific Preprocessing PIR server.
 *
 * @author Liqiang Peng
 * @date 2023/9/14
 */
public abstract class AbstractSingleKeywordCpPirServer extends AbstractTwoPartyPto implements SingleKeywordCpPirServer {
    /**
     * database size
     */
    protected int n;
    /**
     * value bit length
     */
    protected int l;
    /**
     * value byte length
     */
    protected int byteL;
    /**
     * bot element bytebuffer
     */
    protected ByteBuffer botElementByteBuffer;

    protected AbstractSingleKeywordCpPirServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty,
                                               SingleKeywordCpPirConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
    }

    protected void setInitInput(Map<ByteBuffer, ByteBuffer> keywordLabelMap, int labelBitLength) {
        MathPreconditions.checkPositive("labelBitLength", labelBitLength);
        this.l = labelBitLength;
        this.byteL = CommonUtils.getByteLength(this.l);
        MathPreconditions.checkPositive("keywordNum", keywordLabelMap.size());
        this.n = keywordLabelMap.size();
        byte[] botElementByteArray = new byte[CommonConstants.STATS_BYTE_LENGTH];
        Arrays.fill(botElementByteArray, (byte) 0xFF);
        botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
        keywordLabelMap.forEach(
            (item, value) -> Preconditions.checkArgument(!item.equals(botElementByteBuffer), "xi must not equal ⊥")
        );
        initState();
    }

    protected void setPtoInput() {
        checkInitialized();
        // extra info is managed by the protocol itself
    }
}
