package edu.alibaba.mpc4j.s2pc.pir.index;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.util.Arrays;

/**
 * 索引PIR协议客户端抽象类。
 *
 * @author Liqiang Peng
 * @date 2022/8/24
 */
public abstract class AbstractIndexPirClient extends AbstractTwoPartyPto implements IndexPirClient {
    /**
     * 元素字节长度
     */
    protected int elementByteLength;
    /**
     * 客户端检索值
     */
    protected int index;
    /**
     * 服务端元素数量
     */
    protected int num;

    protected AbstractIndexPirClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, IndexPirConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
    }

    protected void setInitInput(int num, int elementByteLength) {
        MathPreconditions.checkPositive("elementByteLength", elementByteLength);
        this.elementByteLength = elementByteLength;
        MathPreconditions.checkPositive("num", num);
        this.num = num;
        initState();
    }

    protected void setPtoInput(int index) {
        checkInitialized();
        MathPreconditions.checkNonNegativeInRange("index", index, num);
        this.index = index;
        extraInfo++;
    }
}
