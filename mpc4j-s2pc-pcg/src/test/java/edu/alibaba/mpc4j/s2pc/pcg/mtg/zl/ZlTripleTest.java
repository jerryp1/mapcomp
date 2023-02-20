package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * l比特三元组测试。
 *
 * @author Weiran Liu
 * @date 2022/8/11
 */
public class ZlTripleTest {
    /**
     * 较小数量
     */
    private static final int MIN_NUM = 1;
    /**
     * 较大数量
     */
    private static final int MAX_NUM = 128;
    /**
     * 较小的l
     */
    private static final int SMALL_L = 1;
    /**
     * 较大的l
     */
    private static final int LARGE_L = CommonConstants.BLOCK_BIT_LENGTH;

    @Test
    public void testIllegalInputs() {
        // create triple with num = 0
        Assert.assertThrows(AssertionError.class, () ->
            ZlTriple.create(EnvType.STANDARD, 1, 0, new BigInteger[0], new BigInteger[0], new BigInteger[0])
        );
        int num = 12;
        // create triples with l = 0
        Assert.assertThrows(AssertionError.class, () -> {
            BigInteger[] as = new BigInteger[num];
            Arrays.fill(as, BigInteger.ZERO);
            BigInteger[] bs = new BigInteger[num];
            Arrays.fill(bs, BigInteger.ZERO);
            BigInteger[] cs = new BigInteger[num];
            Arrays.fill(cs, BigInteger.ZERO);
            ZlTriple.create(EnvType.STANDARD, 0, num, as, bs, cs);
        });
        int l = LARGE_L;
        BigInteger element = largestValidElement(l);
        // create triples with less num
        Assert.assertThrows(AssertionError.class, () -> {
            BigInteger[] as = new BigInteger[num - 1];
            Arrays.fill(as, element);
            BigInteger[] bs = new BigInteger[num - 1];
            Arrays.fill(bs, element);
            BigInteger[] cs = new BigInteger[num - 1];
            Arrays.fill(cs, element);
            ZlTriple.create(EnvType.STANDARD, l, num, as, bs, cs);
        });
        // create triples with large num
        Assert.assertThrows(AssertionError.class, () -> {
            BigInteger[] as = new BigInteger[num + 1];
            Arrays.fill(as, element);
            BigInteger[] bs = new BigInteger[num + 1];
            Arrays.fill(bs, element);
            BigInteger[] cs = new BigInteger[num + 1];
            Arrays.fill(cs, element);
            ZlTriple.create(EnvType.STANDARD, l, num, as, bs, cs);
        });
        // create triples with mis-matched length
        Assert.assertThrows(AssertionError.class, () -> {
            BigInteger[] as = new BigInteger[num - 1];
            Arrays.fill(as, element);
            BigInteger[] bs = new BigInteger[num];
            Arrays.fill(bs, element);
            BigInteger[] cs = new BigInteger[num + 1];
            Arrays.fill(cs, element);
            ZlTriple.create(EnvType.STANDARD, l, num, as, bs, cs);
        });
        // create triples with large element
        Assert.assertThrows(AssertionError.class, () -> {
            BigInteger largeElement = largestValidElement(l).add(BigInteger.ONE);
            BigInteger[] as = new BigInteger[num];
            Arrays.fill(as, largeElement);
            BigInteger[] bs = new BigInteger[num];
            Arrays.fill(bs, element);
            BigInteger[] cs = new BigInteger[num];
            Arrays.fill(cs, element);
            ZlTriple.create(EnvType.STANDARD, l, num, as, bs, cs);
        });
        // create triples with negative element
        Assert.assertThrows(AssertionError.class, () -> {
            BigInteger negativeElement = BigInteger.ONE.negate();
            BigInteger[] as = new BigInteger[num];
            Arrays.fill(as, negativeElement);
            BigInteger[] bs = new BigInteger[num];
            Arrays.fill(bs, element);
            BigInteger[] cs = new BigInteger[num];
            Arrays.fill(cs, element);
            ZlTriple.create(EnvType.STANDARD, l, num, as, bs, cs);
        });
    }

    @Test
    public void testReduce() {
        for (int l = SMALL_L; l < LARGE_L; l++) {
            for (int num = MIN_NUM; num < MAX_NUM; num++) {
                testReduce(l, num);
            }
        }
    }

    private void testReduce(int l, int num) {
        BigInteger element = largestValidElement(l);
        // 创建三元组
        BigInteger[] as = new BigInteger[num];
        Arrays.fill(as, element);
        BigInteger[] bs = new BigInteger[num];
        Arrays.fill(bs, element);
        BigInteger[] cs = new BigInteger[num];
        Arrays.fill(cs, element);
        // 减小到1
        ZlTriple triple1 = ZlTriple.create(EnvType.STANDARD, l, num, as, bs, cs);
        triple1.reduce(1);
        assertCorrectness(triple1, 1);
        // 减小到相同长度
        ZlTriple tripleAll = ZlTriple.create(EnvType.STANDARD, l, num, as, bs, cs);
        tripleAll.reduce(num);
        assertCorrectness(tripleAll, num);
        if (num > 1) {
            // 减小n - 1
            ZlTriple tripleN = ZlTriple.create(EnvType.STANDARD, l, num, as, bs, cs);
            tripleN.reduce(num - 1);
            assertCorrectness(tripleN, num - 1);
            // 减小到一半
            ZlTriple tripleHalf = ZlTriple.create(EnvType.STANDARD, l, num, as, bs, cs);
            tripleHalf.reduce(num / 2);
            assertCorrectness(tripleHalf, num / 2);
        }
    }

    @Test
    public void testAllEmptyMerge() {
        for (int l = SMALL_L; l < LARGE_L; l++) {
            ZlTriple triple = ZlTriple.createEmpty(l);
            ZlTriple mergeTriple = ZlTriple.createEmpty(l);
            triple.merge(mergeTriple);
            assertCorrectness(triple, 0);
        }
    }

    @Test
    public void testLeftEmptyMerge() {
        for (int l = SMALL_L; l < LARGE_L; l++) {
            for (int num = MIN_NUM; num < MAX_NUM; num++) {
                testLeftEmptyMerge(l, num);
            }
        }
    }

    private void testLeftEmptyMerge(int l, int num) {
        BigInteger element = largestValidElement(l);
        // 创建三元组
        BigInteger[] as = new BigInteger[num];
        Arrays.fill(as, element);
        BigInteger[] bs = new BigInteger[num];
        Arrays.fill(bs, element);
        BigInteger[] cs = new BigInteger[num];
        Arrays.fill(cs, element);

        ZlTriple triple = ZlTriple.createEmpty(l);
        ZlTriple mergeTriple = ZlTriple.create(EnvType.STANDARD, l, num, as, bs, cs);
        triple.merge(mergeTriple);
        assertCorrectness(triple, num);
    }

    @Test
    public void testRightEmptyMerge() {
        for (int l = SMALL_L; l < LARGE_L; l++) {
            for (int num = MIN_NUM; num < MAX_NUM; num++) {
                testRightEmptyMerge(l, num);
            }
        }
    }

    private void testRightEmptyMerge(int l, int num) {
        BigInteger element = largestValidElement(l);
        // 创建三元组
        BigInteger[] as = new BigInteger[num];
        Arrays.fill(as, element);
        BigInteger[] bs = new BigInteger[num];
        Arrays.fill(bs, element);
        BigInteger[] cs = new BigInteger[num];
        Arrays.fill(cs, element);

        ZlTriple triple = ZlTriple.create(EnvType.STANDARD, l, num, as, bs, cs);
        ZlTriple mergeTriple = ZlTriple.createEmpty(l);
        triple.merge(mergeTriple);
        assertCorrectness(triple, num);
    }

    @Test
    public void testMerge() {
        for (int l = SMALL_L; l < LARGE_L; l++) {
            for (int num1 = MIN_NUM; num1 < MAX_NUM; num1++) {
                for (int num2 = MIN_NUM; num2 < MAX_NUM; num2++) {
                    testMerge(l, num1, num2);
                }
            }
        }
    }

    private void testMerge(int l, int num1, int num2) {
        BigInteger element = largestValidElement(l);
        // 创建第1个三元组
        BigInteger[] a1s = new BigInteger[num1];
        Arrays.fill(a1s, element);
        BigInteger[] b1s = new BigInteger[num1];
        Arrays.fill(b1s, element);
        BigInteger[] c1s = new BigInteger[num1];
        Arrays.fill(c1s, element);
        // 创建第2个三元组
        BigInteger[] a2s = new BigInteger[num2];
        Arrays.fill(a2s, element);
        BigInteger[] b2s = new BigInteger[num2];
        Arrays.fill(b2s, element);
        BigInteger[] c2s = new BigInteger[num2];
        Arrays.fill(c2s, element);
        // 合并三元组并验证结果
        ZlTriple triple = ZlTriple.create(EnvType.STANDARD, l, num1, a1s, b1s, c1s);
        ZlTriple mergerTriple = ZlTriple.create(EnvType.STANDARD, l, num2, a2s, b2s, c2s);
        triple.merge(mergerTriple);
        assertCorrectness(triple, num1 + num2);
    }

    @Test
    public void testSplit() {
        for (int l = SMALL_L; l < LARGE_L; l++) {
            for (int num = MIN_NUM; num < MAX_NUM; num++) {
                testSplit(l, num);
            }
        }
    }

    private void testSplit(int l, int num) {
        BigInteger element = largestValidElement(l);
        // create triple
        BigInteger[] as = new BigInteger[num];
        Arrays.fill(as, element);
        BigInteger[] bs = new BigInteger[num];
        Arrays.fill(bs, element);
        BigInteger[] cs = new BigInteger[num];
        Arrays.fill(cs, element);
        // 切分1比特
        ZlTriple triple1 = ZlTriple.create(EnvType.STANDARD, l, num, as, bs, cs);
        ZlTriple splitTriple1 = triple1.split(1);
        assertCorrectness(triple1, num - 1);
        assertCorrectness(splitTriple1, 1);
        // 切分全部比特
        ZlTriple tripleAll = ZlTriple.create(EnvType.STANDARD, l, num, as, bs, cs);
        ZlTriple splitTripleAll = tripleAll.split(num);
        assertCorrectness(tripleAll, 0);
        assertCorrectness(splitTripleAll, num);
        if (num > 1) {
            // 切分num - 1比特
            ZlTriple tripleN = ZlTriple.create(EnvType.STANDARD, l, num, as, bs, cs);
            ZlTriple splitTripleN = tripleN.split(num - 1);
            assertCorrectness(tripleN, 1);
            assertCorrectness(splitTripleN, num - 1);
            // 切分一半比特
            ZlTriple tripleHalf = ZlTriple.create(EnvType.STANDARD, l, num, as, bs, cs);
            ZlTriple splitTripleHalf = tripleHalf.split(num / 2);
            assertCorrectness(tripleHalf, num - num / 2);
            assertCorrectness(splitTripleHalf, num / 2);
        }
    }

    private void assertCorrectness(ZlTriple zlTriple, int num) {
        if (num == 0) {
            Assert.assertEquals(0, zlTriple.getNum());
            Assert.assertEquals(0, zlTriple.getA().length);
            Assert.assertEquals(0, zlTriple.getB().length);
            Assert.assertEquals(0, zlTriple.getC().length);
        } else {
            Assert.assertEquals(num, zlTriple.getNum());
            int l = zlTriple.getL();
            BigInteger element = largestValidElement(l);
            for (int index = 0; index < num; index++) {
                Assert.assertEquals(element, zlTriple.getA(index));
                Assert.assertEquals(element, zlTriple.getB(index));
                Assert.assertEquals(element, zlTriple.getC(index));
            }
        }
    }

    private static BigInteger largestValidElement(int l) {
        return BigInteger.ONE.shiftLeft(l).subtract(BigInteger.ONE);
    }
}
