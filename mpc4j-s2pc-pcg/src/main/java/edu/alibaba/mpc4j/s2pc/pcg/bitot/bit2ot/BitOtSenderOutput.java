package edu.alibaba.mpc4j.s2pc.pcg.bitot.bit2ot;


import java.math.BigInteger;

/**
 * BitOT发送方输出。
 *
 * @author Hanwen Feng
 * @date 2022/08/10
 */
public class BitOtSenderOutput extends AbstractBitOtOutput {
    /**
     * 创建BitOT发送方输出。
     *
     * @param r0Array r0数组。
     * @param r1Array r1数组。
     * @return BitOT发送方输出。
     */
    public static BitOtSenderOutput create(boolean[] r0Array, boolean[] r1Array) {
        BitOtSenderOutput output = createEmpty();
        output.setInput(r0Array, r1Array);
        return output;
    }

    /**
     * 创建BitOT发送方输出。
     * @param num OT数量。
     * @param r0ByteArray r0的Byte数组。
     * @param r1ByteArray r1的Byte数组。
     * @return BitOT发送方输出。
     */
    public static BitOtSenderOutput create(int num, byte[] r0ByteArray, byte[] r1ByteArray) {
        BitOtSenderOutput output = createEmpty();
        output.setInput(num, r0ByteArray, r1ByteArray);
        return output;
    }

    /**
     * 创建长度为0的BitOT发送方输出。
     *
     * @return 长度为0的BitOT发送方输出。
     */
    public static BitOtSenderOutput createEmpty() {
        BitOtSenderOutput output = new BitOtSenderOutput();
        output.setZeroInput();
        return output;
    }

    private static BitOtSenderOutput create(int num, BigInteger r0Array, BigInteger r1Array) {
        BitOtSenderOutput output = createEmpty();
        output.setInput(num, r0Array, r1Array);
        return output;
    }

    /**
     * 私有构造函数。
     */
    private BitOtSenderOutput() {
        // empty
    }

    /**
     * 获取R0数组。
     *
     * @return R0数组。
     */
    public byte[] getR0Array() {
        return getArray0();
    }

    /**
     * 获取R0。
     *
     * @param index 指标值。
     * @return R0[index]
     */
    public boolean getR0(int index) {
        return getArray0Element(index);
    }

    /**
     * 获取R1数组。
     *
     * @return R1数组。
     */
    public byte[] getR1Array() {
        return getArray1();
    }

    /**
     * 获取R1。
     *
     * @param index 指标值。
     * @return R1[index]
     */
    public boolean getR1(int index) {
        return getArray1Element(index);
    }

    /**
     * 合并两个发送方输出。
     *
     * @param that 另一个发送方输出。
     */
    public void merge(BitOtSenderOutput that) {
        super.merge(that);
    }

    /**
     * 从当前输出结果切分出一部分输出结果。
     *
     * @param length 切分出输出结果数量。
     * @return 切分输出结果。
     */
    public BitOtSenderOutput split(int length) {
        BigInteger[] outputs = splitBigInteger(length);
        return create(length, outputs[0], outputs[1]);
    }


}
