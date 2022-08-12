package edu.alibaba.mpc4j.s2pc.pcg.bitot.bit2ot;

import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import org.junit.Assert;

import java.util.stream.IntStream;

/**
 * BitOt测试工具。
 *
 * @author Hanwen Feng
 * @date 2022/08/11
 */
public class BitOtTestUtils {

    /**
     * 验证输出结果。
     *
     * @param num            数量。
     * @param senderOutput   发送方输出。
     * @param receiverOutput 接收方输出。
     */
    public static void assertOutput(int num, BitOtSenderOutput senderOutput, BitOtReceiverOutput receiverOutput) {
        if (num == 0) {
            Assert.assertEquals(0, senderOutput.getNum());
            Assert.assertEquals(0, senderOutput.getR0Array().length);
            Assert.assertEquals(0, senderOutput.getR1Array().length);
            Assert.assertEquals(0, receiverOutput.getNum());
            Assert.assertEquals(0, receiverOutput.getRbArray().length);
        } else {
            Assert.assertEquals(num, senderOutput.getNum());
            Assert.assertEquals(num, receiverOutput.getNum());
            IntStream.range(0, num).forEach(index ->
                    Assert.assertEquals(
                            receiverOutput.getRb(index),
                            receiverOutput.getChoice(index) ? senderOutput.getR1(index) : senderOutput.getR0(index)
                    ));
        }

    }
}
