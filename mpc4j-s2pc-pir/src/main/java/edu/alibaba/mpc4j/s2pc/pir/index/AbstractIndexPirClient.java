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

    /**
     * 将long型数组转换为字节数组。
     *
     * @param longArray long型数组。
     * @param logt      系数比特长度。
     * @return 字节数组。
     */
    protected byte[] convertCoeffsToBytes(long[] longArray, int logt) {
        int longArrayLength = longArray.length;
        byte[] byteArray = new byte[longArrayLength * logt / Byte.SIZE];
        int room = Byte.SIZE;
        int j = 0;
        for (long l : longArray) {
            long src = l;
            int rest = logt;
            while (rest != 0 && j < byteArray.length) {
                int shift = Math.min(room, rest);
                byteArray[j] = (byte) (byteArray[j] << shift);
                byteArray[j] = (byte) (byteArray[j] | (src >> (logt - shift)));
                src = src << shift;
                room -= shift;
                rest -= shift;
                if (room == 0) {
                    j++;
                    room = Byte.SIZE;
                }
            }
        }
        return byteArray;
    }

    /**
     * 计算各维度的坐标。
     *
     * @param retrievalIndex 索引值。
     * @param dimensionSize  各维度的长度。
     * @return 各维度的坐标。
     */
    protected int[] computeIndices(int retrievalIndex, int[] dimensionSize) {
        long product = Arrays.stream(dimensionSize).asLongStream().reduce(1, (a, b) -> a * b);
        int[] indices = new int[dimensionSize.length];
        for (int i = 0; i < dimensionSize.length; i++) {
            product /= dimensionSize[i];
            int ji = (int) (retrievalIndex / product);
            indices[i] = ji;
            retrievalIndex -= ji * product;
        }
        return indices;
    }
}
