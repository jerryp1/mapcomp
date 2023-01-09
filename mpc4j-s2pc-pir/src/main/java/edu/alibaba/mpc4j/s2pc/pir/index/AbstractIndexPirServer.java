package edu.alibaba.mpc4j.s2pc.pir.index;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.IntStream;

/**
 * 索引PIR协议服务端抽象类。
 *
 * @author Liqiang Peng
 * @date 2022/8/24
 */
public abstract class AbstractIndexPirServer extends AbstractSecureTwoPartyPto implements IndexPirServer {
    /**
     * 配置项
     */
    private final IndexPirConfig config;
    /**
     * 服务端元素字节数组
     */
    protected byte[] elementByteArray;
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
        this.config = config;
    }

    @Override
    public IndexPirFactory.IndexPirType getPtoType() {
        return config.getProType();
    }

    protected void setInitInput(ArrayList<ByteBuffer> elementArrayList, int elementByteLength) {
        assert elementByteLength > 0 : "element byte length must be greater than 0: " + elementByteLength;
        this.elementByteLength = elementByteLength;
        assert elementArrayList.size() > 0 : "num must be greater than 0";
        num = elementArrayList.size();
        // 将元素打平
        elementByteArray = new byte[num * elementByteLength];
        IntStream.range(0, num).forEach(index -> {
            byte[] element = elementArrayList.get(index).array();
            assert element.length == elementByteLength
                : "element byte length must be " + elementByteLength + ": " + element.length;
            System.arraycopy(element, 0, elementByteArray, index * elementByteLength, elementByteLength);
        });
        extraInfo++;
        initialized = false;
    }

    protected void setPtoInput() {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        extraInfo++;
    }

    /**
     * 将字节数组转换为指定比特长度的long型数组。
     *
     * @param limit  long型数值的比特长度。
     * @param offset 移位。
     * @param size   字节数组长度。
     * @return long型数组。
     */
    private long[] convertBytesToCoeffs(int limit, int offset, double size) {
        // 需要使用的系数个数
        int longArraySize = (int) Math.ceil(Byte.SIZE * size / (double) limit);
        long[] longArray = new long[longArraySize];
        int room = limit;
        int flag = 0;
        for (int i = 0; i < size; i++) {
            int src = elementByteArray[i+offset];
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

    /**
     * 返回数据库编码后的多项式。
     *
     * @param polyModulusDegree      多项式阶。
     * @param plaintextSize          明文数量。
     * @param coeffBitLength         系数比特长度。
     * @param dimensionLength        各维度向量长度。
     * @param elementSizeOfPlaintext 多项式包含的元素数量。
     * @return 数据库编码后的多项式。
     * @throws MpcAbortException 如果协议异常中止。
     */
    protected ArrayList<long[]> encodeDatabase(int polyModulusDegree, int plaintextSize, int coeffBitLength,
                                             int[] dimensionLength, int elementSizeOfPlaintext)
        throws MpcAbortException {
        // number of FV plaintexts needed to create the d-dimensional matrix
        int prod = Arrays.stream(dimensionLength).reduce(1, (a, b) -> a * b);
        MpcAbortPreconditions.checkArgument(plaintextSize <= prod);
        ArrayList<long[]> coeffsList = new ArrayList<>();
        // 每个多项式包含的字节长度
        int byteSizeOfPlaintext = elementSizeOfPlaintext * elementByteLength;
        // 数据库总字节长度
        int totalByteSize = num * elementByteLength;
        // 一个多项式中需要使用的系数个数
        int usedCoeffSize = elementSizeOfPlaintext * ((int) Math.ceil(Byte.SIZE * elementByteLength / (double) coeffBitLength));
        // 系数个数不大于多项式阶数
        MpcAbortPreconditions.checkArgument(
            usedCoeffSize <= polyModulusDegree,
            "coefficient num = %s must be less than or equal to polynomial degree = %s",
            usedCoeffSize, polyModulusDegree
        );
        // 字节转换为多项式系数
        int offset = 0;
        for (int i = 0; i < plaintextSize; i++) {
            long processByteSize;
            if (totalByteSize <= offset) {
                break;
            } else if (totalByteSize < offset + byteSizeOfPlaintext) {
                processByteSize = totalByteSize - offset;
            } else {
                processByteSize = byteSizeOfPlaintext;
            }
            MpcAbortPreconditions.checkArgument(processByteSize % elementByteLength == 0);
            // Get the coefficients of the elements that will be packed in plaintext i
            long[] coeffsArray = convertBytesToCoeffs(coeffBitLength, offset, processByteSize);
            MpcAbortPreconditions.checkArgument(coeffsArray.length <= usedCoeffSize);
            offset += processByteSize;
            long[] paddingCoeffsArray = new long[polyModulusDegree];
            System.arraycopy(coeffsArray, 0, paddingCoeffsArray, 0, coeffsArray.length);
            // Pad the rest with 1s
            IntStream.range(coeffsArray.length, polyModulusDegree).forEach(j -> paddingCoeffsArray[j] = 1L);
            coeffsList.add(paddingCoeffsArray);
        }
        // Add padding plaintext to make database a matrix
        int currentPlaintextSize = coeffsList.size();
        MpcAbortPreconditions.checkArgument(currentPlaintextSize <= plaintextSize);
        IntStream.range(0, (prod - currentPlaintextSize))
            .mapToObj(i -> IntStream.range(0, polyModulusDegree).mapToLong(i1 -> 1L).toArray())
            .forEach(coeffsList::add);
        return coeffsList;
    }
}
