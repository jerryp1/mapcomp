package edu.alibaba.mpc4j.common.tool.polynomial.zp;

import cc.redberry.rings.poly.univar.UnivariatePolynomial;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpManager;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * 用Rings实现的二叉树快速插值。方案描述参见下述论文完整版的附录C：Fast Interpolation and Multi-point Evaluation
 * <p>
 * Pinkas, Benny, Mike Rosulek, Ni Trieu, and Avishay Yanai. Spot-light: Lightweight private set intersection from
 * sparse OT extension. CRYPTO 2019, pp. 401-431. Springer, Cham, 2019.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/10/28
 */
class RingsTreeZpPoly extends AbstractRingsZpPoly {

    RingsTreeZpPoly(int l) {
        super(l, ZpManager.getFiniteField(l));
    }

    @Override
    public ZpPolyFactory.ZpPolyType getType() {
        return ZpPolyFactory.ZpPolyType.RINGS_TREE;
    }

    @Override
    public int coefficientNum(int pointNum, int expectNum) {
        assert pointNum >= 0 && pointNum <= expectNum : "point num must be in range [0, " + expectNum + "]: " + pointNum;
        // 用二叉树快速插值时，pointNum个点的插值多项式包含pointNum + 1个系数，但如果补了随机点，则会等于expectNum
        if (pointNum == expectNum) {
            return expectNum + 1;
        } else {
            return expectNum;
        }
    }

    @Override
    public int rootCoefficientNum(int pointNum, int expectNum) {
        assert pointNum >= 0 && pointNum <= expectNum : "point num must be in range [0, " + expectNum + "]: " + pointNum;
        // 用二叉树快速插值时，expectNum个点的插值多项式包含expectNum + 1个系数
        return expectNum + 1;
    }

    @Override
    protected UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger> polynomialInterpolate(
        BigInteger[] xArray, BigInteger[] yArray) {
        cc.redberry.rings.bigint.BigInteger[] points = Arrays.stream(xArray)
            .map(cc.redberry.rings.bigint.BigInteger::new)
            .toArray(cc.redberry.rings.bigint.BigInteger[]::new);
        cc.redberry.rings.bigint.BigInteger[] values = Arrays.stream(yArray)
            .map(cc.redberry.rings.bigint.BigInteger::new)
            .toArray(cc.redberry.rings.bigint.BigInteger[]::new);
        // 构造满二叉树，二叉树的节点数量 = 2 * numOfLeafNodes - 1
        UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger>[] binaryTreePolynomial = buildBinaryTree(points);
        // 构造导数多项式，注意导数多项式的阶等于points.length，而不是leafNodeNum，cc.rings有求导的快速实现算法
        UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger> derivativePolynomial = binaryTreePolynomial[0]
            .derivative();
        // 计算导数，并存储导数的逆
        cc.redberry.rings.bigint.BigInteger[] derivatives = new cc.redberry.rings.bigint.BigInteger[points.length];
        cc.redberry.rings.bigint.BigInteger[] derivativeInverses = new cc.redberry.rings.bigint.BigInteger[points.length];
        // 这里求值可以使用批处理求值
        int leafNodeNum = getLeafNodeNum(points.length);
        innerEvaluation(derivativePolynomial, binaryTreePolynomial, leafNodeNum, 0, derivatives);
        for (int i = 0; i < derivatives.length; i++) {
            derivativeInverses[i] = finiteField.divideExact(finiteField.getOne(), derivatives[i]);
        }
        return interpolate(values, binaryTreePolynomial, derivativeInverses);
    }

    private UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger> interpolate(
        final cc.redberry.rings.bigint.BigInteger[] values,
        final UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger>[] binaryTreePolynomial,
        final cc.redberry.rings.bigint.BigInteger[] derivativeInverses) {
        int numOfNodes = (binaryTreePolynomial.length + 1) / 2;
        return innerFastInterpolate(values, 0, numOfNodes, binaryTreePolynomial, derivativeInverses);
    }

    private UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger> innerFastInterpolate(
        final cc.redberry.rings.bigint.BigInteger[] values, final int i, final int leafNodeNum,
        final UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger>[] binaryTreePolynomial,
        final cc.redberry.rings.bigint.BigInteger[] derivativeInverses) {
        if (i >= leafNodeNum - 1) {
            // 如果为叶子节点，计算得到二叉树节点索引值所对应的插值点索引值
            int pointIndex = i + 1 - leafNodeNum;
            if (pointIndex >= values.length) {
                // 如果j所对应的叶子节点没有插值点，这意味着j所对应的叶子节点用来补足满二叉树，直接返回1即可
                return UnivariatePolynomial.constant(finiteField, finiteField.getOne());
            } else {
                // 否则，j所对应的叶子节点有插值点，返回y_j * a_j
                return UnivariatePolynomial.constant(finiteField, finiteField.multiply(values[pointIndex], derivativeInverses[pointIndex]));
            }
        }
        int l = leftChildIndex(i);
        int r = rightChildIndex(i);
        UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger> leftPolynomial
            = innerFastInterpolate(values, l, leafNodeNum, binaryTreePolynomial, derivativeInverses);
        UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger> rightPolynomial
            = innerFastInterpolate(values, r, leafNodeNum, binaryTreePolynomial, derivativeInverses);
        return leftPolynomial.clone().multiply(binaryTreePolynomial[r]).add(
            rightPolynomial.clone().multiply(binaryTreePolynomial[l]));
    }

    @Override
    public BigInteger[] evaluate(BigInteger[] coefficients, BigInteger[] xArray) {
        // 恢复多项式
        UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger> polynomial = bigIntegersToPolynomial(coefficients);
        // 求值
        return polynomialEvaluate(polynomial, xArray);
    }

    @Override
    protected BigInteger[] polynomialEvaluate(UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger> polynomial, BigInteger[] xArray) {
        cc.redberry.rings.bigint.BigInteger[] points = Arrays.stream(xArray)
            .map(cc.redberry.rings.bigint.BigInteger::new)
            .toArray(cc.redberry.rings.bigint.BigInteger[]::new);
        if (xArray.length == 1) {
            // 如果只对一个点求值，则直接返回结果
            cc.redberry.rings.bigint.BigInteger y = polynomial.evaluate(points[0]);
            return new BigInteger[]{BigIntegerUtils.byteArrayToBigInteger(y.toByteArray())};
        }
        cc.redberry.rings.bigint.BigInteger[] values = new cc.redberry.rings.bigint.BigInteger[xArray.length];
        // 将结果数组初始化为0
        Arrays.fill(values, finiteField.getZero());
        // 一次可以并行计算的阶数要求是离polynomial.degree()最近的n = 2^k
        int intervalNum = polynomial.degree() == 0 ? 1 : 1 << (LongUtils.ceilLog2(polynomial.degree()) - 1);
        for (int pointIndex = 0; pointIndex < values.length; pointIndex += intervalNum) {
            // 一次取出maxNum个点，如果不足则后面补0
            cc.redberry.rings.bigint.BigInteger[] intervalPoints = new cc.redberry.rings.bigint.BigInteger[intervalNum];
            Arrays.fill(intervalPoints, finiteField.getZero());
            int minCopy = Math.min(intervalNum, values.length - pointIndex);
            System.arraycopy(points, pointIndex, intervalPoints, 0, minCopy);
            cc.redberry.rings.bigint.BigInteger[] intervalValues = evaluation(polynomial.clone(), intervalPoints);
            System.arraycopy(intervalValues, 0, values, pointIndex, minCopy);
        }
        return Arrays.stream(values)
            .map(y -> BigIntegerUtils.byteArrayToBigInteger(y.toByteArray()))
            .toArray(BigInteger[]::new);
    }

    private cc.redberry.rings.bigint.BigInteger[] evaluation(
        UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger> polynomial,
        cc.redberry.rings.bigint.BigInteger[] points) {
        // 批量求值的点数量要小于等于多项式A的阶
        assert points.length <= polynomial.degree()
            : "batched evaluation num must be less than or equal to polynomial degree = " + polynomial.degree()
            + ": " + points.length;
        // 批量求值的点数量要恰好等于2^k，只需要验证n&(n-1)是否为0即可
        assert (points.length & (points.length - 1)) == 0
            : "batched evaluation num must have format 2^k: " + points.length;
        // 构建二叉树
        UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger>[] binaryTreePolynomial = buildBinaryTree(points);
        int leafNodeNum = getLeafNodeNum(points.length);
        cc.redberry.rings.bigint.BigInteger[] values = new cc.redberry.rings.bigint.BigInteger[points.length];
        Arrays.fill(values, finiteField.getZero());
        innerEvaluation(polynomial, binaryTreePolynomial, leafNodeNum, 0, values);
        return values;
    }

    private void innerEvaluation(UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger> polynomialA,
                                 UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger>[] binaryTreePolynomial,
                                 int leafNodeNum, int index, cc.redberry.rings.bigint.BigInteger[] values) {
        UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger> polynomialB = binaryTreePolynomial[index].clone();
        // 如果polynomialA的阶特别小，则继续循环，这是测试时发现的bug，有可能计算完商后就是非常小
        // 此外要注意，当插值多项式的y只有一个元素时，polynomialA的阶会一直特别小，陷入死循环。因此要验证2 * index + 2的长度
        if (polynomialB.degree() > polynomialA.degree() && rightChildIndex(index) <= binaryTreePolynomial.length) {
            innerEvaluation(polynomialA, binaryTreePolynomial, leafNodeNum, leftChildIndex(index), values);
            innerEvaluation(polynomialA, binaryTreePolynomial, leafNodeNum, rightChildIndex(index), values);
        } else {
            int n = polynomialA.degree();
            int m = polynomialB.degree();
            // 当A的阶是n，B的阶是m(m <= n)时，Q的阶是(n - m)，R的阶是(m - 1)，创建多项式Q，依次设置Q的每一个系数
            UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger> polynomialQ
                = UnivariatePolynomial.zero(finiteField);
            UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger> polynomialR = polynomialA.clone();
            for (int i = 0; i <= n - m; i++) {
                UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger> polynomialQuotient
                    = UnivariatePolynomial.zero(finiteField);
                cc.redberry.rings.bigint.BigInteger quotient
                    = finiteField.divideExact(polynomialR.get(n - i), polynomialB.get(m));
                polynomialQuotient.set(n - m - i, quotient);
                polynomialQ.set(n - m - i, quotient);
                polynomialR = polynomialR.subtract(polynomialB.clone().multiply(polynomialQuotient));
            }
            if (index >= leafNodeNum - 1) {
                // 如果为叶子节点，计算得到二叉树节点索引值所对应的插值点索引值
                int j = index + 1 - leafNodeNum;
                if (j < values.length) {
                    // 如果j所对应的叶子节点没有插值点，则不用进行任何操作，否则j所对应的值为R，这里R应该是一个常数
                    assert polynomialR.degree() == 0;
                    values[j] = polynomialR.get(0);
                }
                return;
            }
            // 分别计算左右孩子节点
            innerEvaluation(polynomialR, binaryTreePolynomial, leafNodeNum, leftChildIndex(index), values);
            innerEvaluation(polynomialR, binaryTreePolynomial, leafNodeNum, rightChildIndex(index), values);
        }
    }

    @SuppressWarnings("unchecked")
    private UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger>[] buildBinaryTree
        (final cc.redberry.rings.bigint.BigInteger[] points) {
        int leafNodeNum = getLeafNodeNum(points.length);
        int binaryTreeSize = getBinaryTreeSize(leafNodeNum);
        UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger>[] binaryTreePolynomial
            = new UnivariatePolynomial[binaryTreeSize];
        innerBuildBinaryTree(points, binaryTreePolynomial, leafNodeNum, 0);
        return binaryTreePolynomial;
    }

    /**
     * 迭代构建插值二叉树。
     *
     * @param points               插值点x。
     * @param binaryTreePolynomial 插值二叉树的中间状态。
     * @param leafNodeNum          插值二叉树叶子结点个数。
     * @param index                当前构造的二叉树节点索引值。
     */
    private void innerBuildBinaryTree(final cc.redberry.rings.bigint.BigInteger[] points,
                                      final UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger>[] binaryTreePolynomial,
                                      final int leafNodeNum, final int index) {
        if (index >= leafNodeNum - 1) {
            // 如果为叶子节点，则在对应的位置上构造多项式
            binaryTreePolynomial[index] = UnivariatePolynomial.zero(finiteField);
            if (index + 1 - leafNodeNum < points.length) {
                // 如果有点的位置，则构造x - x_i
                binaryTreePolynomial[index] = binaryTreePolynomial[index]
                    .createLinear(finiteField.negate(points[index + 1 - leafNodeNum]), finiteField.getOne());
            } else {
                // 如果没有点的位置，此多项式设置为1
                binaryTreePolynomial[index] = binaryTreePolynomial[index].createConstant(finiteField.getOne());
            }
            return;
        }
        // 迭代构造左右孩子节点
        innerBuildBinaryTree(points, binaryTreePolynomial, leafNodeNum, leftChildIndex(index));
        innerBuildBinaryTree(points, binaryTreePolynomial, leafNodeNum, rightChildIndex(index));
        binaryTreePolynomial[index] = binaryTreePolynomial[leftChildIndex(index)].clone()
            .multiply(binaryTreePolynomial[rightChildIndex(index)]);
    }

    private int getLeafNodeNum(int pointNum) {
        // 二叉树的叶子节点数量必须是2的阶，例如2^4, 2^8等，找到离points.length最近的n = 2^i
        return pointNum == 0 ? 1 : 1 << LongUtils.ceilLog2(pointNum);
    }

    private int getBinaryTreeSize(int leafNodeNum) {
        // 满二叉树的节点数量 = 2 * leafNodeNum - 1
        return 2 * leafNodeNum - 1;
    }

    private int leftChildIndex(int index) {
        return (index << 1) + 1;
    }

    private int rightChildIndex(int index) {
        return (index << 1) + 2;
    }
}