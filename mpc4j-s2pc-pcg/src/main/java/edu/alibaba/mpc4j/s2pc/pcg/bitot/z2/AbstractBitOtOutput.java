package edu.alibaba.mpc4j.s2pc.pcg.bitot.z2;

import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.math.BigInteger;

/**
 * BitOt输出抽象类
 *
 * @author Hanwen Feng
 * @date 2022/08/05
 */
public abstract class AbstractBitOtOutput {
    /**
     * 表示0号数组 （choices或者r0Array）的大整数。
     */
    protected BigInteger array0;
    /**
     * 表示1号数组 (rbArray或者r1Array)的大整数。
     */
    private BigInteger array1;
    /**
     * OT数量。
     */
    private int num;
    /**
     * OT按Byte存储的byte数量。
     */
    private int byteNum;
    /**
     * 偏置量。
     */
    private int offset;

    /**
     * 设置长度为0的输出变量。
     */
    protected void setZeroInput() {
        array0 = BigInteger.ZERO;
        array1 = BigInteger.ZERO;
        setParams(0);
    }

    /**
     * 根据boolean数组设置变量。
     * @param array0 数组0。
     * @param array1 数组1。
     */
    protected void setInput(boolean[] array0, boolean[] array1) {
        assert array0.length == array1.length : "arrays must have equal length";

        setParams(array0.length);
        byte[] byteArray0 = BinaryUtils.binaryToRoundByteArray(array0);
        byte[] byteArray1 = BinaryUtils.binaryToRoundByteArray(array1);
        this.array0 = BigIntegerUtils.byteArrayToNonNegBigInteger(byteArray0);
        this.array1 = BigIntegerUtils.byteArrayToNonNegBigInteger(byteArray1);
    }

    protected void setInput(int num, byte[] byteArray0, byte[] byteArray1) {
        assert byteArray0.length == byteArray1.length : "arrays must have equal length";
        assert num <= byteArray0.length * Byte.SIZE;

        setParams(num);
        this.array0 = BigIntegerUtils.byteArrayToNonNegBigInteger(byteArray0);
        this.array1 = BigIntegerUtils.byteArrayToNonNegBigInteger(byteArray1);
    }

    /**
     * 根据大整数设置变量。
     * @param num OT数量
     * @param array0 表示数组0的大整数。
     * @param array1 表示数组1的大整数。
     */
    protected void setInput(int num, BigInteger array0, BigInteger array1) {
        assert num > 0 : "num must be larger than 0: " + num;

        assert BigIntegerUtils.nonNegative(array0) && array0.bitLength() <= num;
        assert BigIntegerUtils.nonNegative(array1) && array1.bitLength() <= num;

        setParams(num);
        this.array0 = array0;
        this.array1 = array1;
    }


    /**
     * 返回Bit-OT数量
     *
     * @return num
     */
    public int getNum() {
        return num;
    }

    /**
     * 返回Bit-OT字节长度
     *
     * @return byteNum
     */
    public int getByteNum() {
        return byteNum;
    }

    public void reduce(int length) {
        assert length > 0 && length <= num : "reduce length = " + length + " must be in range (0, " + num + "]";
        if (length < num) {
            // 如果给定的数量小于当前数量，则裁剪，否则保持原样不动
            BigInteger mask = BigInteger.ONE.shiftLeft(length).subtract(BigInteger.ONE);
            array0 = array0.and(mask);
            array1 = array1.and(mask);
            setParams(length);
        }
    }

    protected byte[] getArray0() {
        return BigIntegerUtils.nonNegBigIntegerToByteArray(array0, byteNum);
    }

    protected boolean getArray0Element(int index) {
        assert index < num : "Index must be in the range [0, num) : " + index;

        return BinaryUtils.getBoolean(BigIntegerUtils.nonNegBigIntegerToByteArray(array0, byteNum), index + offset);
    }

    protected byte[] getArray1() {
        return BigIntegerUtils.nonNegBigIntegerToByteArray(array1, byteNum);
    }

    protected boolean getArray1Element(int index) {
        assert index < num : "Index must be in the range [0, num) : " + index;

        return BinaryUtils.getBoolean(BigIntegerUtils.nonNegBigIntegerToByteArray(array1, byteNum), index + offset);
    }

    protected BigInteger[] splitBigInteger(int length) {
        assert length > 0 && length <= num : "split length must be in range (0, " + num + "]";
        // 切分方法：分别对2^length取模数和取余数，模数作为split结果，余数作为剩余结果
        BigInteger mask = BigInteger.ONE.shiftLeft(length).subtract(BigInteger.ONE);
        // 由于模数一定是2^length格式，因此可以用位运算更高效地实现
        BigInteger[] output = new BigInteger[2];
        output[0] = array0.and(mask);
        output[1] = array1.and(mask);
        // 更新剩余的Bit-OT
        array0 = array0.shiftRight(length);
        array1 = array1.shiftRight(length);
        setParams(num - length);

        return output;
    }

    protected void merge(AbstractBitOtOutput that) {
        // 移位
        array0 = array0.shiftLeft(that.num).add(that.array0);
        array1 = array1.shiftLeft(that.num).add(that.array1);
        // 更新长度
        setParams(num + that.num);
    }

    private void setParams(int num) {
        this.num = num;
        byteNum = num == 0 ? 0 : CommonUtils.getByteLength(num);
        offset = num == 0 ? 0 : byteNum * Byte.SIZE - num;
    }


}
