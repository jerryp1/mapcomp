package edu.alibaba.mpc4j.s2pc.pcg.bitot.bit2ot;

import java.math.BigInteger;

/**
 * BitOT接收方输出。
 *
 * @author Hanwen Feng
 * @date 2022/08/10
 */
public class BitOtReceiverOutput extends AbstractBitOtOutput {

    /**
     * 创建BitOT接收方输出。
     *
     * @param choices 选择数组。
     * @param rbArray rb数组。
     * @return BitOT接收方输出。
     */
    public static BitOtReceiverOutput create(boolean[] choices, boolean[] rbArray) {
        BitOtReceiverOutput output = createEmpty();
        output.setInput(choices, rbArray);
        return output;
    }

    /**
     * 创建BitOT接收方输出。
     * @param num OT数量。
     * @param choiceByteArray 选择比特的Byte数组。
     * @param rbByteArray rb的Byte数组。
     * @return BitOT接收方输出。
     */
    public static BitOtReceiverOutput create(int num, byte[] choiceByteArray, byte[] rbByteArray) {
        BitOtReceiverOutput output = createEmpty();
        output.setInput(num, choiceByteArray, rbByteArray);
        return output;
    }

    /**
     * 创建长度为0的BitOT接收方输出。
     *
     * @return 长度为0的BitOT接收方输出。
     */
    public static BitOtReceiverOutput createEmpty() {
        BitOtReceiverOutput output = new BitOtReceiverOutput();
        output.setZeroInput();
        return output;
    }

    private static BitOtReceiverOutput create(int num, BigInteger choices, BigInteger rbArray) {
        BitOtReceiverOutput output = createEmpty();
        output.setInput(num, choices, rbArray);
        return output;
    }

    /**
     * 私有构造函数。
     */
    private BitOtReceiverOutput() {
        // empty
    }

    /**
     * 获取选择数组。
     *
     * @return 选择数组。
     */
    public byte[] getChoices() {
        return getArray0();
    }

    /**
     * 获取选择比特。
     *
     * @param index 指标值。
     * @return 选择比特。
     */
    public boolean getChoice(int index) {
        return getArray0Element(index);
    }

    /**
     * 获取Rb数组。
     *
     * @return Rb数组。
     */
    public byte[] getRbArray() {
        return getArray1();
    }

    /**
     * 获取Rb比特。
     *
     * @param index 指标值。
     * @return Rb比特。
     */
    public boolean getRb(int index) {
        return getArray1Element(index);
    }

    /**
     * 合并两个接收方输出。
     *
     * @param that 另一个接收方输出。
     */
    public void merge(BitOtReceiverOutput that) {
        super.merge(that);
    }

    /**
     * 从当前输出结果切分出一部分输出结果。
     *
     * @param length 切分出输出结果数量。
     * @return 切分输出结果。
     */
    public BitOtReceiverOutput split(int length) {
        BigInteger[] outputs = splitBigInteger(length);
        return create(length, outputs[0], outputs[1]);
    }


}
