package edu.alibaba.mpc4j.s2pc.pir.index;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pir.index.fastpir.Ayaa21IndexPirPtoDesc;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.IntStream;

/**
 * 索引PIR协议服务端抽象类。
 *
 * @author Liqiang Peng
 * @date 2022/8/24
 */
public abstract class AbstractIndexPirServer extends AbstractTwoPartyPto implements IndexPirServer {
    /**
     * 服务端元素字节数组
     */
    protected ArrayList<byte[][]> elementByteArray = new ArrayList<>();
    /**
     * 服务端元素数量
     */
    protected int num;
    /**
     * 元素字节长度
     */
    protected int elementByteLength;

    protected AbstractIndexPirServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, IndexPirConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
    }

    protected void setInitInput(ArrayList<ByteBuffer> elementArrayList, int elementByteLength, int binMaxByteLength,
                                String protocolName) {
        MathPreconditions.checkPositive("elementByteLength", elementByteLength);
        this.elementByteLength = elementByteLength;
        MathPreconditions.checkPositive("num", elementArrayList.size());
        num = elementArrayList.size();
        IntStream.range(0, num).forEach(index -> {
            byte[] element = elementArrayList.get(index).array();
            MathPreconditions.checkEqual("element.length", "elementByteLength", element.length, elementByteLength);
            //TODO @庚序 remove here. A candidate solution is to put elementByteLength into the IndexPirParams
            assert !protocolName.equals(Ayaa21IndexPirPtoDesc.getInstance().getPtoName()) || elementByteLength % 2 == 0;
        });
        // 分块数量
        int binNum = (elementByteLength + binMaxByteLength - 1) / binMaxByteLength;
        int lastBinByteLength = elementByteLength % binMaxByteLength == 0 ?
            binMaxByteLength : elementByteLength % binMaxByteLength;
        for (int i = 0; i < binNum; i++) {
            int byteLength = i == binNum - 1 ? lastBinByteLength : binMaxByteLength;
            byte[][] byteArray = new byte[num][byteLength];
            for (int j = 0; j < num; j++) {
                System.arraycopy(elementArrayList.get(j).array(), i * binMaxByteLength, byteArray[j], 0, byteLength);
            }
            elementByteArray.add(byteArray);
        }
        initState();
    }

    protected void setPtoInput() {
        checkReadyState();
        extraInfo++;
    }

    /**
     * 将字节数组转换为指定比特长度的long型数组。
     *
     * @param limit     long型数值的比特长度。
     * @param offset    移位。
     * @param size      待转换的字节数组长度。
     * @param byteArray 字节数组。
     * @return long型数组。
     */
    protected long[] convertBytesToCoeffs(int limit, int offset, int size, byte[] byteArray) {
        // 需要使用的系数个数
        int longArraySize = (int) Math.ceil(Byte.SIZE * size / (double) limit);
        long[] longArray = new long[longArraySize];
        int room = limit;
        int flag = 0;
        for (int i = 0; i < size; i++) {
            int src = byteArray[i+offset];
            if (src < 0) {
                src &= 0xFF;
            }
            int rest = Byte.SIZE;
            while (rest != 0) {
                if (room == 0) {
                    flag++;
                    room = limit;
                }
                int shift = Math.min(room, rest);
                long temp = longArray[flag] << shift;
                longArray[flag] = temp | (src >> (Byte.SIZE - shift));
                int remain = (1 << (Byte.SIZE - shift)) - 1;
                src = (src & remain) << shift;
                room -= shift;
                rest -= shift;
            }
        }
        longArray[flag] = longArray[flag] << room;
        return longArray;
    }
}