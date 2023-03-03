package edu.alibaba.mpc4j.s2pc.pir.index.fastpir;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * FastPIR内部协议参数。
 *
 * @author Liqiang Peng
 * @date 2023/3/2
 */
public class Ayaa21IndexPirInnerParams {

    /**
     * 单个元素的列长度
     */
    private final int[] elementColumnLength;
    /**
     * 查询向量的密文个数
     */
    private final int querySize;
    /**
     * 数据库行数
     */
    private final int[] databaseRowNum;
    /**
     * 数据库分块数量
     */
    private final int binNum;
    /**
     * 分块的最长字节长度
     */
    private final int binMaxByteLength;
    /**
     * 最后一个分块的字节长度
     */
    private final int lastBinByteLength;

    public Ayaa21IndexPirInnerParams(Ayaa21IndexPirParams params, int serverElementSize, int elementByteLength) {
        this.querySize = (int) Math.ceil(serverElementSize / (params.getPolyModulusDegree() / 2.0));
        this.binMaxByteLength = params.getPlainModulusBitLength() * params.getPolyModulusDegree() / Byte.SIZE;
        // 数据库分块数量
        this.binNum = (elementByteLength + binMaxByteLength - 1) / binMaxByteLength;
        this.lastBinByteLength = elementByteLength - (binNum - 1) * binMaxByteLength;
        this.elementColumnLength = new int[binNum];
        this.databaseRowNum = new int[binNum];
        IntStream.range(0, binNum).forEach(i -> {
            int byteLength = i == binNum - 1 ? lastBinByteLength : binMaxByteLength;
            elementColumnLength[i] = (int) Math.ceil((byteLength / 2.0) * Byte.SIZE / params.getPlainModulusBitLength());
            databaseRowNum[i] = querySize * elementColumnLength[i];
        });
    }

    /**
     * 返回数据库行数。
     *
     * @return 数据库行数。
     */
    public int[] getDatabaseRowNum() {
        return databaseRowNum;
    }

    /**
     * 返回元素的列长度。
     *
     * @return 元素的列长度。
     */
    public int[] getElementColumnLength() {
        return elementColumnLength;
    }

    /**
     * 返回查询信息的密文数目。
     *
     * @return 查询信息的密文数。
     */
    public int getQuerySize() {
        return querySize;
    }

    /**
     * 返回分块数目。
     *
     * @return 分块数目。
     */
    public int getBinNum() {
        return this.binNum;
    }

    /**
     * 返回分块的最大字节长度。
     *
     * @return 分块的最大字节长度。
     */
    public int getBinMaxByteLength() {
        return binMaxByteLength;
    }

    /**
     * 返回最后一个分块的字节长度。
     *
     * @return 最后一个分块的字节长度。
     */
    public int getLastBinByteLength() {
        return lastBinByteLength;
    }

    @Override
    public String toString() {
        return
            "FastPIR Parameters :" + "\n" +
            "  - query ciphertext size : " + querySize + "\n" +
            "  - element column length : " + Arrays.toString(elementColumnLength) + "\n" +
            "  - database row size : " + Arrays.toString(databaseRowNum);
    }
}
