package edu.alibaba.mpc4j.s2pc.pir.batchindex;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 批量索引PIR协议客户端抽象类。
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public abstract class AbstractBatchIndexPirClient extends AbstractTwoPartyPto implements BatchIndexPirClient {
    /**
     * 客户端最大元素数量
     */
    protected int maxRetrievalSize;
    /**
     * 客户端元素数量
     */
    protected int retrievalSize;
    /**
     * 特殊空元素字节缓存区
     */
    protected ByteBuffer botElementByteBuffer;
    /**
     * 客户端检索值
     */
    protected ArrayList<ByteBuffer> indicesByteBuffer;
    /**
     * 服务端元素数量
     */
    protected int serverElementSize;
    /**
     * 元素比特长度
     */
    protected int elementBitLength;
    /**
     * 分块的比特长度
     */
    protected int partitionBitLength;
    /**
     * 分块数目
     */
    protected int partitionCount;

    protected AbstractBatchIndexPirClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, BatchIndexPirConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
    }

    protected void setInitInput(int serverElementSize, int elementBitLength, int maxRetrievalSize,
                                int partitionBitLength) {
        MathPreconditions.checkPositive("serverElementSize", serverElementSize);
        this.serverElementSize = serverElementSize;
        MathPreconditions.checkPositive("maxClientElementSize", maxRetrievalSize);
        this.maxRetrievalSize = maxRetrievalSize;
        MathPreconditions.checkPositive("elementBitLength", elementBitLength);
        this.elementBitLength = elementBitLength;
        MathPreconditions.checkPositiveInRangeClosed("partitionBitLength", partitionBitLength, Integer.SIZE);
        this.partitionBitLength = partitionBitLength;
        this.partitionCount = CommonUtils.getUnitNum(elementBitLength, partitionBitLength);
        // 设置特殊空元素
        byte[] botElementByteArray = new byte[Integer.BYTES];
        Arrays.fill(botElementByteArray, (byte)0xFF);
        botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
        initState();
    }

    protected void setPtoInput(ArrayList<Integer> indices) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("maxRetrievalSize", indices.size(), maxRetrievalSize);
        for (Integer index : indices) {
            MathPreconditions.checkNonNegativeInRange("index", index, serverElementSize);
        }
        this.retrievalSize = indices.size();
        this.indicesByteBuffer = IntStream.range(0, retrievalSize)
            .mapToObj(i -> ByteBuffer.wrap(IntUtils.intToByteArray(indices.get(i))))
            .collect(Collectors.toCollection(ArrayList::new));
        extraInfo++;
    }
}
