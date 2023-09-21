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
import java.util.Arrays;

/**
 * abstract Single Keyword Client-specific Preprocessing PIR client.
 *
 * @author Liqiang Peng
 * @date 2023/9/14
 */
public abstract class AbstractSingleKeywordCpPirClient extends AbstractTwoPartyPto implements SingleKeywordCpPirClient {
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

    protected AbstractSingleKeywordCpPirClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty,
                                               SingleKeywordCpPirConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
    }

    protected void setInitInput(int n, int l) {
        MathPreconditions.checkPositive("n", n);
        this.n = n;
        MathPreconditions.checkPositive("l", l);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        byte[] botElementByteArray = new byte[CommonConstants.STATS_BYTE_LENGTH];
        Arrays.fill(botElementByteArray, (byte)0xFF);
        botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
        initState();
    }

    protected void setPtoInput(ByteBuffer keyword) {
        // extra info is managed by the protocol itself
        checkInitialized();
        Preconditions.checkArgument(!keyword.equals(botElementByteBuffer), "keyword must not equal ⊥");
    }
}
