package edu.alibaba.mpc4j.s2pc.pir.batchindex;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.math.BigInteger;
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
     * 服务端元素数组
     */
    protected ArrayList<byte[][]> elementByteArray = new ArrayList<>();
    /**
     * 服务端元素数量
     */
    protected int serverElementSize;
    /**
     * 特殊空元素字节缓存区
     */
    protected ByteBuffer botElementByteBuffer;
    /**
     * 支持的最大批检索数目
     */
    protected int maxRetrievalSize;
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

    protected AbstractBatchIndexPirServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, BatchIndexPirConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
    }

    protected void setInitInput(byte[][] elementArray, int elementBitLength, int maxRetrievalSize, int partitionBitLength) {
        MathPreconditions.checkPositive("serverElementSize", elementArray.length);
        serverElementSize = elementArray.length;
        MathPreconditions.checkPositive("maxRetrievalSize", maxRetrievalSize);
        this.maxRetrievalSize = maxRetrievalSize;
        MathPreconditions.checkPositive("elementBitLength", elementBitLength);
        this.elementBitLength = elementBitLength;
        MathPreconditions.checkPositiveInRangeClosed("partitionBitLength", partitionBitLength, Integer.SIZE);
        this.partitionBitLength = partitionBitLength;
        this.partitionCount = CommonUtils.getUnitNum(elementBitLength, partitionBitLength);
        BigInteger mod = BigInteger.ONE.shiftLeft(partitionBitLength);
        int byteLength = CommonUtils.getByteLength(partitionBitLength);
        for (int i = 0; i < partitionCount; i++) {
            byte[][] temp = new byte[serverElementSize][byteLength];
            for (int j = 0; j < serverElementSize; j++) {
                BigInteger element = BigIntegerUtils.byteArrayToNonNegBigInteger(elementArray[j]);
                element = element.shiftRight(i * partitionBitLength);
                element = element.mod(mod);
                temp[j] = BigIntegerUtils.nonNegBigIntegerToByteArray(element, byteLength);
            }
            elementByteArray.add(temp);
        }
        // 设置特殊空元素
        byte[] botElementByteArray = new byte[Integer.BYTES];
        Arrays.fill(botElementByteArray, (byte)0xFF);
        botElementByteBuffer = ByteBuffer.wrap(botElementByteArray);
        initState();
    }

    protected void setPtoInput() {
        checkInitialized();
        extraInfo++;
    }
}