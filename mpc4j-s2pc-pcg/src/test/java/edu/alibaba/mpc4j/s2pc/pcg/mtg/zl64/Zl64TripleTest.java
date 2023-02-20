package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl64;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * Zl64 triple test cases.
 *
 * @author Weiran Liu
 * @date 2023/2/20
 */
public class Zl64TripleTest {
    /**
     * minimum num
     */
    private static final int MIN_NUM = 1;
    /**
     * maximum num
     */
    private static final int MAX_NUM = 128;
    /**
     * small l
     */
    private static final int SMALL_L = 1;
    /**
     * large l
     */
    private static final int LARGE_L = LongUtils.MAX_L;

    @Test
    public void testIllegalInputs() {
        // create triple with num = 0
        Assert.assertThrows(AssertionError.class, () ->
            Zl64Triple.create(EnvType.STANDARD, 1, 0, new long[0], new long[0], new long[0])
        );
        int num = 12;
        // create triples with l = 0
        Assert.assertThrows(AssertionError.class, () -> {
            long[] as = new long[num];
            Arrays.fill(as, 0L);
            long[] bs = new long[num];
            Arrays.fill(bs, 0L);
            long[] cs = new long[num];
            Arrays.fill(cs, 0L);
            Zl64Triple.create(EnvType.STANDARD, 0, num, as, bs, cs);
        });
        // create triples with large l
        Assert.assertThrows(AssertionError.class, () -> {
            long[] as = new long[num];
            Arrays.fill(as, 0L);
            long[] bs = new long[num];
            Arrays.fill(bs, 0L);
            long[] cs = new long[num];
            Arrays.fill(cs, 0L);
            Zl64Triple.create(EnvType.STANDARD, LongUtils.MAX_L + 1, num, as, bs, cs);
        });
        int l = LARGE_L;
        long element = largestValidElement(l);
        // create triples with less num
        Assert.assertThrows(AssertionError.class, () -> {
            long[] as = new long[num - 1];
            Arrays.fill(as, element);
            long[] bs = new long[num - 1];
            Arrays.fill(bs, element);
            long[] cs = new long[num - 1];
            Arrays.fill(cs, element);
            Zl64Triple.create(EnvType.STANDARD, l, num, as, bs, cs);
        });
        // create triples with large num
        Assert.assertThrows(AssertionError.class, () -> {
            long[] as = new long[num + 1];
            Arrays.fill(as, element);
            long[] bs = new long[num + 1];
            Arrays.fill(bs, element);
            long[] cs = new long[num + 1];
            Arrays.fill(cs, element);
            Zl64Triple.create(EnvType.STANDARD, l, num, as, bs, cs);
        });
        // create triples with mis-matched length
        Assert.assertThrows(AssertionError.class, () -> {
            long[] as = new long[num - 1];
            Arrays.fill(as, element);
            long[] bs = new long[num];
            Arrays.fill(bs, element);
            long[] cs = new long[num + 1];
            Arrays.fill(cs, element);
            Zl64Triple.create(EnvType.STANDARD, l, num, as, bs, cs);
        });
        // create triples with large element
        Assert.assertThrows(AssertionError.class, () -> {
            long largeElement = largestValidElement(l) + 1;
            long[] as = new long[num];
            Arrays.fill(as, largeElement);
            long[] bs = new long[num];
            Arrays.fill(bs, element);
            long[] cs = new long[num];
            Arrays.fill(cs, element);
            Zl64Triple.create(EnvType.STANDARD, l, num, as, bs, cs);
        });
        // create triples with negative element
        Assert.assertThrows(AssertionError.class, () -> {
            long negativeElement = -1L;
            long[] as = new long[num];
            Arrays.fill(as, negativeElement);
            long[] bs = new long[num];
            Arrays.fill(bs, element);
            long[] cs = new long[num];
            Arrays.fill(cs, element);
            Zl64Triple.create(EnvType.STANDARD, l, num, as, bs, cs);
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
        long element = largestValidElement(l);
        // create tuples with valid elements
        long[] as = new long[num];
        Arrays.fill(as, element);
        long[] bs = new long[num];
        Arrays.fill(bs, element);
        long[] cs = new long[num];
        Arrays.fill(cs, element);
        // reduce 1
        Zl64Triple triple1 = Zl64Triple.create(EnvType.STANDARD, l, num, as, bs, cs);
        triple1.reduce(1);
        assertCorrectness(triple1, 1);
        // reduce the same num
        Zl64Triple tripleAll = Zl64Triple.create(EnvType.STANDARD, l, num, as, bs, cs);
        tripleAll.reduce(num);
        assertCorrectness(tripleAll, num);
        if (num > 1) {
            // reduce n - 1
            Zl64Triple tripleN = Zl64Triple.create(EnvType.STANDARD, l, num, as, bs, cs);
            tripleN.reduce(num - 1);
            assertCorrectness(tripleN, num - 1);
            // reduce half
            Zl64Triple tripleHalf = Zl64Triple.create(EnvType.STANDARD, l, num, as, bs, cs);
            tripleHalf.reduce(num / 2);
            assertCorrectness(tripleHalf, num / 2);
        }
    }

    @Test
    public void testAllEmptyMerge() {
        for (int l = SMALL_L; l < LARGE_L; l++) {
            Zl64Triple triple = Zl64Triple.createEmpty(l);
            Zl64Triple mergeTriple = Zl64Triple.createEmpty(l);
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
        long element = largestValidElement(l);
        // create tuples with valid elements
        long[] as = new long[num];
        Arrays.fill(as, element);
        long[] bs = new long[num];
        Arrays.fill(bs, element);
        long[] cs = new long[num];
        Arrays.fill(cs, element);

        Zl64Triple triple = Zl64Triple.createEmpty(l);
        Zl64Triple mergeTriple = Zl64Triple.create(EnvType.STANDARD, l, num, as, bs, cs);
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
        long element = largestValidElement(l);
        // 创建三元组
        long[] as = new long[num];
        Arrays.fill(as, element);
        long[] bs = new long[num];
        Arrays.fill(bs, element);
        long[] cs = new long[num];
        Arrays.fill(cs, element);

        Zl64Triple triple = Zl64Triple.create(EnvType.STANDARD, l, num, as, bs, cs);
        Zl64Triple mergeTriple = Zl64Triple.createEmpty(l);
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
        long element = largestValidElement(l);
        // create the first triple
        long[] a1s = new long[num1];
        Arrays.fill(a1s, element);
        long[] b1s = new long[num1];
        Arrays.fill(b1s, element);
        long[] c1s = new long[num1];
        Arrays.fill(c1s, element);
        // create the second triple
        long[] a2s = new long[num2];
        Arrays.fill(a2s, element);
        long[] b2s = new long[num2];
        Arrays.fill(b2s, element);
        long[] c2s = new long[num2];
        Arrays.fill(c2s, element);
        // 合并三元组并验证结果
        Zl64Triple triple = Zl64Triple.create(EnvType.STANDARD, l, num1, a1s, b1s, c1s);
        Zl64Triple mergerTriple = Zl64Triple.create(EnvType.STANDARD, l, num2, a2s, b2s, c2s);
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
        long element = largestValidElement(l);
        // create the triple
        long[] as = new long[num];
        Arrays.fill(as, element);
        long[] bs = new long[num];
        Arrays.fill(bs, element);
        long[] cs = new long[num];
        Arrays.fill(cs, element);
        // split 1
        Zl64Triple triple1 = Zl64Triple.create(EnvType.STANDARD, l, num, as, bs, cs);
        Zl64Triple splitTriple1 = triple1.split(1);
        assertCorrectness(triple1, num - 1);
        assertCorrectness(splitTriple1, 1);
        // split num
        Zl64Triple tripleAll = Zl64Triple.create(EnvType.STANDARD, l, num, as, bs, cs);
        Zl64Triple splitTripleAll = tripleAll.split(num);
        assertCorrectness(tripleAll, 0);
        assertCorrectness(splitTripleAll, num);
        if (num > 1) {
            // split num - 1
            Zl64Triple tripleN = Zl64Triple.create(EnvType.STANDARD, l, num, as, bs, cs);
            Zl64Triple splitTripleN = tripleN.split(num - 1);
            assertCorrectness(tripleN, 1);
            assertCorrectness(splitTripleN, num - 1);
            // split half
            Zl64Triple tripleHalf = Zl64Triple.create(EnvType.STANDARD, l, num, as, bs, cs);
            Zl64Triple splitTripleHalf = tripleHalf.split(num / 2);
            assertCorrectness(tripleHalf, num - num / 2);
            assertCorrectness(splitTripleHalf, num / 2);
        }
    }

    private void assertCorrectness(Zl64Triple triple, int num) {
        if (num == 0) {
            Assert.assertEquals(0, triple.getNum());
            Assert.assertEquals(0, triple.getA().length);
            Assert.assertEquals(0, triple.getB().length);
            Assert.assertEquals(0, triple.getC().length);
        } else {
            Assert.assertEquals(num, triple.getNum());
            int l = triple.getL();
            long element = largestValidElement(l);
            for (int index = 0; index < num; index++) {
                Assert.assertEquals(element, triple.getA(index));
                Assert.assertEquals(element, triple.getB(index));
                Assert.assertEquals(element, triple.getC(index));
            }
        }
    }

    private static long largestValidElement(int l) {
        return (1L << l) - 1;
    }
}
