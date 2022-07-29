package edu.alibaba.mpc4j.s2pc.pcg.btg;

import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.junit.Assert;

/**
 * BTG协议测试工具类。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public class BtgTestUtils {
    /**
     * 私有构造函数
     */
    private BtgTestUtils() {
        // empty
    }

    public static void assertOutput(int num, BooleanTriple senderOutput, BooleanTriple receiverOutput) {
        Assert.assertEquals(num, senderOutput.getNum());
        Assert.assertEquals(num, receiverOutput.getNum());
        if (num == 0) {
            Assert.assertArrayEquals(new byte[0], senderOutput.getA());
            Assert.assertArrayEquals(new byte[0], senderOutput.getB());
            Assert.assertArrayEquals(new byte[0], senderOutput.getC());
            Assert.assertArrayEquals(new byte[0], receiverOutput.getA());
            Assert.assertArrayEquals(new byte[0], receiverOutput.getB());
            Assert.assertArrayEquals(new byte[0], receiverOutput.getC());
        } else {
            byte[] a0 = senderOutput.getA();
            byte[] b0 = senderOutput.getB();
            byte[] c0 = senderOutput.getC();
            byte[] a1 = receiverOutput.getA();
            byte[] b1 = receiverOutput.getB();
            byte[] c1 = receiverOutput.getC();
            // 分别计算a、b、c
            byte[] a = BytesUtils.xor(a0, a1);
            byte[] b = BytesUtils.xor(b0, b1);
            byte[] c = BytesUtils.xor(c0, c1);
            Assert.assertArrayEquals(c, BytesUtils.and(a, b));
        }
    }
}
