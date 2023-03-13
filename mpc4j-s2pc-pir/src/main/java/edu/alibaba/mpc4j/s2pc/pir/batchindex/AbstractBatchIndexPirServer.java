package edu.alibaba.mpc4j.s2pc.pir.batchindex;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * 批量索引PIR协议服务端抽象类。
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public abstract class AbstractBatchIndexPirServer extends AbstractTwoPartyPto implements BatchIndexPirServer {
    /**
     * 服务端元素字节数组
     */
    protected ArrayList<byte[]> elementByteArray = new ArrayList<>();
    /**
     * 服务端元素数量
     */
    protected int serverElementSize;
    /**
     * 特殊空元素字节缓存区
     */
    protected ByteBuffer botElementByteBuffer;
    protected int maxRetrievalSize;

    protected AbstractBatchIndexPirServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, BatchIndexPirConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
    }

    protected void setInitInput(ArrayList<ByteBuffer> elementArrayList, int maxRetrievalSize) throws MpcAbortException {
        MathPreconditions.checkPositive("serverElementSize", elementArrayList.size());
        serverElementSize = elementArrayList.size();
        for (int i = 0; i < serverElementSize; i++) {
            byte[] element = elementArrayList.get(i).array();
            elementByteArray.add(element);
//            boolean value = BinaryUtils.getBoolean(element, element.length * Byte.SIZE - 1);
//            elementByteArray.add(value ? new byte[]{0x01} : new byte[]{0x00});
        }
        // 设置特殊空元素
        byte[] botElementByteArray = new byte[CommonConstants.STATS_BYTE_LENGTH];
        Arrays.fill(botElementByteArray, (byte)0xFF);
        botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
        this.maxRetrievalSize = maxRetrievalSize;
        initState();
    }

    protected void setPtoInput() {
        checkInitialized();
        extraInfo++;
    }
}