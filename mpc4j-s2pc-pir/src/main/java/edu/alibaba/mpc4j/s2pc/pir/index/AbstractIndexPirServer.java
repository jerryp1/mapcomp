package edu.alibaba.mpc4j.s2pc.pir.index;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

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

    protected void setInitInput(ArrayList<ByteBuffer> elementArrayList, int elementByteLength, int binMaxByteLength) {
        MathPreconditions.checkPositive("elementByteLength", elementByteLength);
        this.elementByteLength = elementByteLength;
        MathPreconditions.checkPositive("num", elementArrayList.size());
        num = elementArrayList.size();
        IntStream.range(0, num).forEach(index -> {
            byte[] element = elementArrayList.get(index).array();
            MathPreconditions.checkEqual("element.length", "elementByteLength", element.length, elementByteLength);
        });
        // 分块数量
        int binNum = (elementByteLength + binMaxByteLength - 1) / binMaxByteLength;
        int lastBinByteLength = elementByteLength - (binNum - 1) * binMaxByteLength;
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
        checkInitialized();
        extraInfo++;
    }
}