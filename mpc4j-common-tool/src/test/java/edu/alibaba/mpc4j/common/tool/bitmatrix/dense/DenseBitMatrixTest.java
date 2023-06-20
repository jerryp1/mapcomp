package edu.alibaba.mpc4j.common.tool.bitmatrix.dense;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.DenseBitMatrixFactory.DenseBitMatrixType;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * dense bit matrix test.
 *
 * @author Weiran Liu
 * @date 2022/8/2
 */
@RunWith(Parameterized.class)
public class DenseBitMatrixTest {
    /**
     * sizes
     */
    private static final int[] SIZES = new int[]{1, 7, 8, 9, 127, 128, 129};
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * round
     */
    private static final int ROUND = 40;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // LONG_MATRIX
        configurations.add(new Object[]{DenseBitMatrixType.LONG_MATRIX.name(), DenseBitMatrixType.LONG_MATRIX});
        // BYTE_MATRIX
        configurations.add(new Object[]{DenseBitMatrixType.BYTE_MATRIX.name(), DenseBitMatrixType.BYTE_MATRIX});

        return configurations;
    }

    /**
     * type
     */
    private final DenseBitMatrixType type;

    public DenseBitMatrixTest(String name, DenseBitMatrixType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testConstantAdd() {
        for (int rows : SIZES) {
            for (int columns : SIZES) {
                // 0 + 0 = 0
                DenseBitMatrix zero = DenseBitMatrixFactory.createAllZero(type, rows, columns);
                Assert.assertEquals(zero, zero.add(zero));
                DenseBitMatrix inner = DenseBitMatrixFactory.createAllZero(type, rows, columns);
                inner.addi(zero);
                Assert.assertEquals(zero, inner);
                // 0 + 1 = 1
                DenseBitMatrix one = DenseBitMatrixFactory.createAllOne(type, rows, columns);
                Assert.assertEquals(one, one.add(zero));
                Assert.assertEquals(one, zero.add(one));
                inner = DenseBitMatrixFactory.createAllZero(type, rows, columns);
                inner.addi(one);
                Assert.assertEquals(one, inner);
                inner = DenseBitMatrixFactory.createAllOne(type, rows, columns);
                inner.addi(zero);
                Assert.assertEquals(one, inner);
                // 1 + 1 = 0
                Assert.assertEquals(zero, one.add(one));
                inner = DenseBitMatrixFactory.createAllOne(type, rows, columns);
                inner.addi(one);
                Assert.assertEquals(zero, inner);
            }
        }
    }

    @Test
    public void testRandomAdd() {
        for (int rows : SIZES) {
            for (int columns : SIZES) {
                testRandomAdd(rows, columns);
            }
        }
    }

    private void testRandomAdd(int rows, int columns) {
        DenseBitMatrix zero = DenseBitMatrixFactory.createAllZero(type, rows, columns);
        DenseBitMatrix inner;
        // random + random = 0
        for (int round = 0; round < ROUND; round++) {
            DenseBitMatrix random = DenseBitMatrixFactory.createRandom(type, rows, columns, SECURE_RANDOM);
            Assert.assertEquals(zero, random.add(random));
            inner = DenseBitMatrixFactory.createRandom(type, rows, columns, SECURE_RANDOM);
            inner.addi(inner);
            Assert.assertEquals(zero, inner);
        }
        // random + 0 = random
        for (int round = 0; round < ROUND; round++) {
            DenseBitMatrix random = DenseBitMatrixFactory.createRandom(type, rows, columns, SECURE_RANDOM);
            Assert.assertEquals(random, random.add(zero));
        }
    }

    @Test
    public void testRandomMultiply() {
        for (int rows : SIZES) {
            for (int columns : SIZES) {
                testRandomMultiply(rows, columns);
            }
        }
    }

    private void testRandomMultiply(int rows, int columns) {
        for (int rightColumns : SIZES) {
            for (int round = 0; round < ROUND; round++) {
                DenseBitMatrix a = DenseBitMatrixFactory.createRandom(type, rows, columns, SECURE_RANDOM);
                // 右侧矩阵的行数必须等于左侧矩阵的列数
                DenseBitMatrix b = DenseBitMatrixFactory.createRandom(type, columns, rightColumns, SECURE_RANDOM);
                // 测试方法：(A * B)^T = B^T * A^T
                DenseBitMatrix mulTrans = a.multiply(b).transpose(EnvType.STANDARD, false);
                DenseBitMatrix transMul = b.transpose(EnvType.STANDARD, false)
                    .multiply(a.transpose(EnvType.STANDARD, false));
                Assert.assertEquals(mulTrans, transMul);
            }
        }
    }

    @Test
    public void testLmul() {
        for (int rows : SIZES) {
            for (int columns : SIZES) {
                testLmul(rows, columns);
            }
        }
    }

    private void testLmul(int rows, int columns) {
        for (int round = 0; round < ROUND; round++) {
            DenseBitMatrix a = DenseBitMatrixFactory.createRandom(type, rows, rows, SECURE_RANDOM);
            DenseBitMatrix b = DenseBitMatrixFactory.createRandom(type, rows, columns, SECURE_RANDOM);
            byte[][] expectArray = a.multiply(b).getByteArrayData();
            // 将a矩阵分别转换成byte[]分别与b矩阵左乘
            byte[][] byteVectorActualArray = IntStream.range(0, rows)
                .mapToObj(rowIndex -> {
                    byte[] v = a.getByteArrayRow(rowIndex);
                    return b.lmul(v);
                })
                .toArray(byte[][]::new);
            Assert.assertArrayEquals(expectArray, byteVectorActualArray);
            // 将a矩阵转换为布尔矩阵，分别与b矩阵相乘
            byte[][] binaryVectorActualArray = IntStream.range(0, rows)
                .mapToObj(rowIndex -> {
                    boolean[] v = BinaryUtils.byteArrayToBinary(a.getByteArrayRow(rowIndex), rows);
                    return b.lmul(v);
                })
                .map(BinaryUtils::binaryToRoundByteArray)
                .toArray(byte[][]::new);
            Assert.assertArrayEquals(expectArray, binaryVectorActualArray);
        }
    }

    @Test
    public void testLmulAddi() {
        for (int rows : SIZES) {
            for (int columns : SIZES) {
                testLmulAddi(rows, columns);
            }
        }
    }

    private void testLmulAddi(int rows, int columns) {
        for (int round = 0; round < ROUND; round++) {
            DenseBitMatrix a = DenseBitMatrixFactory.createRandom(type, rows, rows, SECURE_RANDOM);
            DenseBitMatrix b = DenseBitMatrixFactory.createRandom(type, rows, columns, SECURE_RANDOM);
            DenseBitMatrix c = DenseBitMatrixFactory.createRandom(type, rows, columns, SECURE_RANDOM);
            byte[][] expectArray = a.multiply(b).add(c).getByteArrayData();
            // 将a矩阵分别转换成byte[]分别与b矩阵左乘
            byte[][] byteVectorActualArray = IntStream.range(0, rows)
                .mapToObj(rowIndex -> {
                    byte[] t = BytesUtils.clone(c.getByteArrayRow(rowIndex));
                    b.lmulAddi(a.getByteArrayRow(rowIndex), t);
                    return t;
                })
                .toArray(byte[][]::new);
            Assert.assertArrayEquals(expectArray, byteVectorActualArray);
        }
        for (int round = 0; round < ROUND; round++) {
            DenseBitMatrix a = DenseBitMatrixFactory.createRandom(type, rows, rows, SECURE_RANDOM);
            DenseBitMatrix b = DenseBitMatrixFactory.createRandom(type, rows, columns, SECURE_RANDOM);
            DenseBitMatrix c = DenseBitMatrixFactory.createRandom(type, rows, columns, SECURE_RANDOM);
            byte[][] expectArray = a.multiply(b).add(c).getByteArrayData();
            // 将a矩阵分别转换成boolean[]分别与b矩阵左乘
            byte[][] byteVectorActualArray = IntStream.range(0, rows)
                .mapToObj(rowIndex -> {
                    boolean[] v = BinaryUtils.byteArrayToBinary(a.getByteArrayRow(rowIndex), rows);
                    boolean[] t = BinaryUtils.byteArrayToBinary(c.getByteArrayRow(rowIndex), columns);
                    b.lmulAddi(v, t);
                    return BinaryUtils.binaryToRoundByteArray(t);
                })
                .toArray(byte[][]::new);
            Assert.assertArrayEquals(expectArray, byteVectorActualArray);
        }
    }

    @Test
    public void testTranspose() {
        for (int rows : SIZES) {
            for (int columns : SIZES) {
                testTranspose(rows, columns);
            }
        }
    }

    private void testTranspose(int rows, int columns) {
        for (int round = 0; round < ROUND; round++) {
            DenseBitMatrix origin = DenseBitMatrixFactory.createRandom(type, rows, columns, SECURE_RANDOM);
            DenseBitMatrix transpose = origin.transpose(EnvType.STANDARD, false);
            DenseBitMatrix recover = transpose.transpose(EnvType.STANDARD, false);
            Assert.assertEquals(origin, recover);
        }
    }

    @Test
    public void testLextMul() {
        for (int rows : SIZES) {
            for (int columns : SIZES) {
                testLextMul(rows, columns);
            }
        }
    }

    private void testLextMul(int rows, int columns) {
        for (int round = 0; round < ROUND; round++) {
            DenseBitMatrix a = DenseBitMatrixFactory.createRandom(type, rows, rows, SECURE_RANDOM);
            DenseBitMatrix b = DenseBitMatrixFactory.createRandom(type, rows, columns, SECURE_RANDOM);
            // 测试方法： (A^T*B)^T = (A.toArrays())*B
            DenseBitMatrix aTranspose = a.transpose(EnvType.STANDARD_JDK, false);
            byte[][] expectArray = aTranspose.multiply(b).transpose(EnvType.STANDARD_JDK, false).getByteArrayData();
            byte[][] byteVectorActualArray = b.lExtMul(a.getByteArrayData());
            Assert.assertArrayEquals(expectArray, byteVectorActualArray);
        }
    }

    @Test
    public void testLextMulAddi(){
        for (int rows : SIZES) {
            for (int columns : SIZES) {
                testLextMulAddi(rows, columns);
            }
        }
    }

    private void testLextMulAddi(int rows, int columns) {
        for (int round = 0; round < ROUND; round++) {
            DenseBitMatrix a = DenseBitMatrixFactory.createRandom(type, rows, rows, SECURE_RANDOM);
            DenseBitMatrix b = DenseBitMatrixFactory.createRandom(type, rows, columns, SECURE_RANDOM);
            DenseBitMatrix c = DenseBitMatrixFactory.createRandom(type, rows, columns, SECURE_RANDOM);
            // 测试方法： ((A^T*B) + C)^T = (A.toArrays())*B + C^T.toArray()
            DenseBitMatrix aTranspose = a.transpose(EnvType.STANDARD_JDK, false);
            byte[][] expectArray = aTranspose.multiply(b).add(c).transpose(EnvType.STANDARD_JDK, false).getByteArrayData();
            byte[][] byteVectorActualArray = c.transpose(EnvType.STANDARD_JDK, false).getByteArrayData();
            b.lExtMulAddi(a.getByteArrayData(), byteVectorActualArray);
            Assert.assertArrayEquals(expectArray, byteVectorActualArray);
        }
    }
}
