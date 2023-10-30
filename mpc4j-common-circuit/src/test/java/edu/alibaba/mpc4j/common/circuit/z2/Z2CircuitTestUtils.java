package edu.alibaba.mpc4j.common.circuit.z2;

import edu.alibaba.mpc4j.common.circuit.operator.Z2IntegerOperator;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.junit.Assert;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Z2 Circuit Test Utils.
 *
 * @author Li Peng
 * @date 2023/6/7
 */
public class Z2CircuitTestUtils {

    public static void assertOutput(Z2IntegerOperator operator, int l, long[] longXs, long[] longYs, long[] longZs) {
        int num = longXs.length;
        long andMod = (1L << l) - 1;
        switch (operator) {
            case SUB:
                IntStream.range(0, num).forEach(i -> {
                    long expectZ = (longXs[i] - longYs[i]) & andMod;
                    long actualZ = longZs[i];
                    Assert.assertEquals(expectZ, actualZ);
                });
                break;
            case INCREASE_ONE:
                IntStream.range(0, num).forEach(i -> {
                    long expectZ = (longXs[i] + 1) & andMod;
                    long actualZ = longZs[i];
                    Assert.assertEquals(expectZ, actualZ);
                });
                break;
            case ADD:
                IntStream.range(0, num).forEach(i -> {
                    long expectZ = (longXs[i] + longYs[i]) & andMod;
                    long actualZ = longZs[i];
                    Assert.assertEquals(expectZ, actualZ);
                });
                break;
            case MUL:
                IntStream.range(0, num).forEach(i -> {
                    long expectZ = (longXs[i] * longYs[i]) & andMod;
                    long actualZ = longZs[i];
                    Assert.assertEquals(expectZ, actualZ);
                });
                break;
            case LEQ:
                IntStream.range(0, num).forEach(i -> {
                    boolean expectZ = (longXs[i] <= longYs[i]);
                    boolean actualZ = (longZs[i] % 2) == 1;
                    Assert.assertEquals(expectZ, actualZ);
                });
                break;
            case EQ:
                IntStream.range(0, num).forEach(i -> {
                    boolean expectZ = (longXs[i] == longYs[i]);
                    boolean actualZ = (longZs[i] % 2) == 1;
                    Assert.assertEquals(expectZ, actualZ);
                });
                break;
            default:
                throw new IllegalStateException("Invalid " + operator.name() + ": " + operator.name());
        }
    }

    public static void assertSortOutput(int l, long[][] longXs, long[][] longZs) {
        int num = longXs[0].length;
        int numOfSorted = longXs.length;
        long andMod = (1L << l) - 1;
        for (int i = 0; i < num; i++) {
            int finalI = i;
            Long[] expected = IntStream.range(0, numOfSorted).mapToObj(j -> longXs[j][finalI]).toArray(Long[]::new);
            Arrays.sort(expected);
            for (int j = 0; j < numOfSorted; j++) {
                long actual = longZs[j][i] & andMod;
                Assert.assertEquals(expected[j].longValue(), actual);
            }
        }
    }

    public static void assertPsortOutput(int l, long[] longXs, long[][] longPayload, int[] index, long[] sortedX, long[][] sortedPayload) {
        assert longXs.length == sortedX.length;
        assert longPayload.length == sortedPayload.length;
        int numOfSorted = longXs.length;
        int payloadNum = longPayload.length;

        assertValidPermutation(index);
        for (int i = 0; i < numOfSorted; i++) {
            Assert.assertEquals(longXs[index[i]], sortedX[i]);
            for (int j = 0; j < payloadNum; j++) {
                Assert.assertEquals(longPayload[j][index[i]], sortedPayload[j][i]);
            }
            if (i > 0) {
                MathPreconditions.checkGreaterOrEqual("sortedX[i] >= sortedX[i-1]", sortedX[i], sortedX[i - 1]);
            }
        }
    }

    public static void assertValidPermutation(int[] index){
        TIntSet set = new TIntHashSet();
        for(int x : index){
            assert x >= 0 && x < index.length;
            assert !set.contains(x);
            set.add(x);
        }
    }
}
