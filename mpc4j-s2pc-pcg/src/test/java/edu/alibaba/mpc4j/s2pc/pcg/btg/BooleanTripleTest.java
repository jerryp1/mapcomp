package edu.alibaba.mpc4j.s2pc.pcg.btg;

import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * 布尔三元组测试。
 *
 * @author Weiran Liu
 * @date 2022/4/8
 */
public class BooleanTripleTest {
    /**
     * 较小数量
     */
    private static final int MIN_NUM = 1;
    /**
     * 较大数量
     */
    private static final int MAX_NUM = 128;

    @Test
    public void testIllegalInputs() {
        try {
            // 创建长度为0的布尔三元组
            BooleanTriple.create(0, new byte[0], new byte[0], new byte[0]);
            throw new IllegalStateException("ERROR: successfully create Boolean Triple with num = 0");
        } catch (AssertionError ignored) {

        }
        int num = 12;
        int byteNum = CommonUtils.getByteLength(num);
        try {
            // 创建长度小的布尔三元组
            BooleanTriple.create(num, new byte[byteNum - 1], new byte[byteNum - 1], new byte[byteNum - 1]);
            throw new IllegalStateException("ERROR: successfully create Boolean Triple with less byte length");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建长度大的布尔三元组
            BooleanTriple.create(num, new byte[byteNum + 1], new byte[byteNum + 1], new byte[byteNum + 1]);
            throw new IllegalStateException("ERROR: successfully create Boolean Triple with large byte length");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建长度不一致的布尔三元组
            BooleanTriple.create(num, new byte[byteNum], new byte[byteNum - 1], new byte[byteNum + 1]);
            throw new IllegalStateException("ERROR: successfully create Boolean Triple with distinct length");
        } catch (AssertionError ignored) {

        }
        // 构造n = 12的三元组
        byte[] a = new byte[]{0x0F, (byte) 0xFF};
        byte[] b = new byte[]{0x0F, (byte) 0xFF};
        byte[] c = new byte[]{0x0F, (byte) 0xFF};
        try {
            // 创建长度过短的布尔三元组
            BooleanTriple.create(num - 1, a, b, c);
            throw new IllegalStateException("ERROR: successfully create Boolean Triple with wrong num");
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testReduce() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testReduce(num);
        }
    }

    private void testReduce(int num) {
        // 创建三元组
        int byteNum = CommonUtils.getByteLength(num);
        byte[] a = new byte[byteNum];
        Arrays.fill(a, (byte)0xFF);
        BytesUtils.reduceByteArray(a, num);
        byte[] b = new byte[byteNum];
        Arrays.fill(b, (byte)0xFF);
        BytesUtils.reduceByteArray(b, num);
        byte[] c = new byte[byteNum];
        Arrays.fill(c, (byte)0xFF);
        BytesUtils.reduceByteArray(c, num);

        // 减小到1
        BooleanTriple triple1 = BooleanTriple.create(num, a, b, c);
        triple1.reduce(1);
        assertCorrectness(triple1, 1);
        // 减小到相同长度
        BooleanTriple tripleAll = BooleanTriple.create(num, a, b, c);
        tripleAll.reduce(num);
        assertCorrectness(tripleAll, num);
        if (num > 1) {
            // 减小n - 1
            BooleanTriple tripleN = BooleanTriple.create(num, a, b, c);
            tripleN.reduce(num - 1);
            assertCorrectness(tripleN, num - 1);
            // 减小到一半
            BooleanTriple tripleHalf = BooleanTriple.create(num, a, b, c);
            tripleHalf.reduce(num / 2);
            assertCorrectness(tripleHalf, num / 2);
        }
    }

    @Test
    public void testAllEmptyMerge() {
        BooleanTriple triple = BooleanTriple.createEmpty();
        BooleanTriple mergeTriple = BooleanTriple.createEmpty();
        triple.merge(mergeTriple);
        assertCorrectness(triple, 0);
    }

    @Test
    public void testLeftEmptyMerge() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testLeftEmptyMerge(num);
        }
    }

    private void testLeftEmptyMerge(int num) {
        // 创建三元组
        int byteNum = CommonUtils.getByteLength(num);
        byte[] a = new byte[byteNum];
        Arrays.fill(a, (byte)0xFF);
        BytesUtils.reduceByteArray(a, num);
        byte[] b = new byte[byteNum];
        Arrays.fill(b, (byte)0xFF);
        BytesUtils.reduceByteArray(b, num);
        byte[] c = new byte[byteNum];
        Arrays.fill(c, (byte)0xFF);
        BytesUtils.reduceByteArray(c, num);

        BooleanTriple triple = BooleanTriple.createEmpty();
        BooleanTriple mergeTriple = BooleanTriple.create(num, a, b, c);
        triple.merge(mergeTriple);
        assertCorrectness(triple, num);
    }

    @Test
    public void testRightEmptyMerge() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testRightEmptyMerge(num);
        }
    }

    private void testRightEmptyMerge(int num) {
        // 创建三元组
        int byteNum = CommonUtils.getByteLength(num);
        byte[] a = new byte[byteNum];
        Arrays.fill(a, (byte)0xFF);
        BytesUtils.reduceByteArray(a, num);
        byte[] b = new byte[byteNum];
        Arrays.fill(b, (byte)0xFF);
        BytesUtils.reduceByteArray(b, num);
        byte[] c = new byte[byteNum];
        Arrays.fill(c, (byte)0xFF);
        BytesUtils.reduceByteArray(c, num);

        BooleanTriple triple = BooleanTriple.create(num, a, b, c);
        BooleanTriple mergeTriple = BooleanTriple.createEmpty();
        triple.merge(mergeTriple);
        assertCorrectness(triple, num);
    }

    @Test
    public void testMerge() {
        for (int num1 = MIN_NUM; num1 < MAX_NUM; num1++) {
            for (int num2 = MIN_NUM; num2 < MAX_NUM; num2++) {
                testMerge(num1, num2);
            }
        }
    }

    private void testMerge(int num1, int num2) {
        // 创建第1个三元组
        int byteNum1 = CommonUtils.getByteLength(num1);
        byte[] a1 = new byte[byteNum1];
        Arrays.fill(a1, (byte)0xFF);
        BytesUtils.reduceByteArray(a1, num1);
        byte[] b1 = new byte[byteNum1];
        Arrays.fill(b1, (byte)0xFF);
        BytesUtils.reduceByteArray(b1, num1);
        byte[] c1 = new byte[byteNum1];
        Arrays.fill(c1, (byte)0xFF);
        BytesUtils.reduceByteArray(c1, num1);
        // 创建第2个三元组
        int byteNum2 = CommonUtils.getByteLength(num2);
        byte[] a2 = new byte[byteNum2];
        Arrays.fill(a2, (byte)0xFF);
        BytesUtils.reduceByteArray(a2, num2);
        byte[] b2 = new byte[byteNum2];
        Arrays.fill(b2, (byte)0xFF);
        BytesUtils.reduceByteArray(b2, num2);
        byte[] c2 = new byte[byteNum2];
        Arrays.fill(c2, (byte)0xFF);
        BytesUtils.reduceByteArray(c2, num2);
        // 合并三元组并验证结果
        BooleanTriple triple = BooleanTriple.create(num1, a1, b1, c1);
        BooleanTriple mergerTriple = BooleanTriple.create(num2, a2, b2, c2);
        triple.merge(mergerTriple);
        assertCorrectness(triple, num1 + num2);
    }

    @Test
    public void testSplit() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testSplit(num);
        }
    }

    private void testSplit(int num) {
        // 创建三元组
        int byteNum = CommonUtils.getByteLength(num);
        byte[] a = new byte[byteNum];
        Arrays.fill(a, (byte)0xFF);
        BytesUtils.reduceByteArray(a, num);
        byte[] b = new byte[byteNum];
        Arrays.fill(b, (byte)0xFF);
        BytesUtils.reduceByteArray(b, num);
        byte[] c = new byte[byteNum];
        Arrays.fill(c, (byte)0xFF);
        BytesUtils.reduceByteArray(c, num);
        // 切分1比特
        BooleanTriple triple1 = BooleanTriple.create(num, a, b, c);
        BooleanTriple splitTriple1 = triple1.split(1);
        assertCorrectness(triple1, num - 1);
        assertCorrectness(splitTriple1, 1);
        // 切分全部比特
        BooleanTriple tripleAll = BooleanTriple.create(num, a, b, c);
        BooleanTriple splitTripleAll = tripleAll.split(num);
        assertCorrectness(tripleAll, 0);
        assertCorrectness(splitTripleAll, num);
        if (num > 1) {
            // 切分n - 1比特
            BooleanTriple tripleN = BooleanTriple.create(num, a, b, c);
            BooleanTriple splitTripleN = tripleN.split(num - 1);
            assertCorrectness(tripleN, 1);
            assertCorrectness(splitTripleN, num - 1);
            // 切分一半比特
            BooleanTriple tripleHalf = BooleanTriple.create(num, a, b, c);
            BooleanTriple splitTripleHalf = tripleHalf.split(num / 2);
            assertCorrectness(tripleHalf, num - num / 2);
            assertCorrectness(splitTripleHalf, num / 2);
        }
    }

    private void assertCorrectness(BooleanTriple booleanTriple, int byteNum) {
        if (byteNum == 0) {
            Assert.assertEquals(0, booleanTriple.getNum());
            Assert.assertEquals(0, booleanTriple.getByteNum());
            Assert.assertEquals("", booleanTriple.getStringA());
            Assert.assertEquals("", booleanTriple.getStringB());
            Assert.assertEquals("", booleanTriple.getStringC());
        } else {
            Assert.assertEquals(byteNum, booleanTriple.getNum());
            Assert.assertEquals(CommonUtils.getByteLength(byteNum), booleanTriple.getByteNum());
            String fullOneString = BigInteger.ONE.shiftLeft(byteNum).subtract(BigInteger.ONE).toString(2);
            Assert.assertEquals(fullOneString, booleanTriple.getStringA());
            Assert.assertEquals(fullOneString, booleanTriple.getStringB());
            Assert.assertEquals(fullOneString, booleanTriple.getStringC());
        }
    }
}
