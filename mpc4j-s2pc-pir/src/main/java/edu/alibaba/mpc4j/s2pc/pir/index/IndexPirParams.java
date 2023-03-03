package edu.alibaba.mpc4j.s2pc.pir.index;

/**
 * 索引PIR协议参数接口。
 *
 * @author Liqiang Peng
 * @date 2023/3/1
 */
public interface IndexPirParams {

    /**
     * 返回明文模数比特长度。
     *
     * @return 明文模数比特长度。
     */
    int getPlainModulusBitLength();

    /**
     * 返回多项式阶。
     *
     * @return 多项式阶。
     */
    int getPolyModulusDegree();

    /**
     * 返回维数。
     *
     * @return 维数。
     */
    int getDimension();

    /**
     * 返回加密方案参数。
     *
     * @return 加密方案参数。
     */
    byte[] getEncryptionParams();
}
