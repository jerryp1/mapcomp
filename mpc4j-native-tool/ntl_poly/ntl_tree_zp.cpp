//
// Created by Weiran Liu on 2022/10/28.
//

#include "ntl_tree_zp.h"

#define LEFT(X) (2 * X + 1)
#define RIGHT(X) (2 * X + 2)

/**
 * 内部构建二叉树。
 *
 * @param prime Zp域。
 * @param binary_tree 二叉树存储地址。
 * @param points 插值点。
 * @param point_num 插值点数量。
 * @param leaf_node_num 叶子节点数量。
 * @param index 当前构造的二叉树节点索引值。
 */
void inner_build_binary_tree(NTL::ZZ_pX* binary_tree, NTL::ZZ_p* points, long point_num, long leaf_node_num, long index) {
    NTL::ZZ_p negated;
    if (index >= leaf_node_num - 1 && index <= 2 * leaf_node_num - 2) {
        // 如果为叶子节点，则在对应的位置上构造多项式
        if (index + 1 - leaf_node_num < point_num) {
            // 如果有点的位置，则构造x - x_i
            NTL::negate(negated, points[index + 1 - leaf_node_num]);
            SetCoeff(binary_tree[index], 0, negated);
            SetCoeff(binary_tree[index], 1, 1);
        } else {
            // 如果没有点的位置，此多项式设置为1
            SetCoeff(binary_tree[index], 0, 1);
        }
        return;
    }
    // 迭代构造左右孩子节点
    inner_build_binary_tree(binary_tree, points, point_num, leaf_node_num, LEFT(index));
    inner_build_binary_tree(binary_tree, points, point_num, leaf_node_num, RIGHT(index));
    binary_tree[index] = binary_tree[LEFT(index)] * binary_tree[RIGHT(index)];
}

void inner_evaluate(NTL::ZZ_pX& polynomial_a, NTL::ZZ_pX* binary_tree, long binary_tree_size, long node_num, long index, NTL::ZZ_p* values, long point_num) {
    NTL::ZZ_pX polynomial_b = binary_tree[index];
    // 如果polynomialA的阶特别小，则继续循环，这是测试时发现的bug，有可能计算完商后就是非常小
    // 此外要注意，当插值多项式的y只有一个元素时，polynomialA的阶会一直特别小，陷入死循环。因此要验证2 * index + 2的长度
    if (NTL::deg(polynomial_b) > NTL::deg(polynomial_a) && RIGHT(index) <= binary_tree_size) {
        inner_evaluate(polynomial_a, binary_tree, binary_tree_size, node_num, LEFT(index), values, point_num);
        inner_evaluate(polynomial_a, binary_tree, binary_tree_size, node_num, RIGHT(index), values, point_num);
    } else {
        long n = NTL::deg(polynomial_a);
        long m = NTL::deg(polynomial_b);
        // 当A的阶是n，B的阶是m(m <= n)时，Q的阶是(n - m)，R的阶是(m - 1)，创建多项式Q，依次设置Q的每一个系数
        NTL::ZZ_pX polynomial_q(0);
        NTL::ZZ_pX polynomial_r(polynomial_a);
        for (long i = 0; i <= n - m; i++) {
            NTL::ZZ_pX polynomial_quotient(0);
            NTL::ZZ_p quotient = NTL::coeff(polynomial_r, n - i) / NTL::coeff(polynomial_b, m);
            SetCoeff(polynomial_quotient, n - m - i, quotient);
            SetCoeff(polynomial_q, n - m - i, quotient);
            polynomial_r = polynomial_r - (polynomial_b * polynomial_quotient);
        }
        if (index >= node_num - 1 && node_num <= 2 * node_num - 1) {
            // 如果为叶子节点，计算得到二叉树节点索引值所对应的插值点索引值
            long j = index + 1 - node_num;
            if (j < point_num) {
                // 如果j所对应的叶子节点没有插值点，则不用进行任何操作，否则j所对应的值为R，这里R应该是一个常数
                values[j] = NTL::coeff(polynomial_r, 0);
            }
            return;
        }
        // 分别计算左右孩子节点
        inner_evaluate(polynomial_r, binary_tree, binary_tree_size, node_num, LEFT(index), values, point_num);
        inner_evaluate(polynomial_r, binary_tree, binary_tree_size, node_num, RIGHT(index), values, point_num);
    }
}

void evaluate(NTL::ZZ_pX& polynomial, NTL::ZZ_p* points, NTL::ZZ_p* values, long point_num) {
    // 二叉树的叶子节点数量必须是2的阶，找到离point_num最近的n = 2^i
    long leaf_node_num = point_num == 0 ? 1 : 1 << ceilLog2(point_num);
    // 构造满二叉树，二叉树的节点数量 = 2 * numOfLeafNodes - 1
    long binary_tree_size = 2 * leaf_node_num - 1;
    auto* binary_tree = new NTL::ZZ_pX[binary_tree_size];
    for (long index = 0; index < binary_tree_size; index++) {
        binary_tree[index] = NTL::ZZ_pX::zero();
    }
    inner_build_binary_tree(binary_tree, points, point_num, leaf_node_num, 0);
    long node_num = (binary_tree_size + 1) / 2;
    inner_evaluate(polynomial, binary_tree, binary_tree_size, node_num, 0, values, point_num);
    delete[] binary_tree;
}

void zp_tree_evaluate(uint64_t primeByteLength, std::vector<uint8_t*> &coeffs, std::vector<uint8_t*> &setX, std::vector<uint8_t*> &setY) {
    // 临时变量
    NTL::ZZ e_ZZ;
    NTL::ZZ_p e_ZZ_p;
    // build polynomial
    uint32_t coeff_num = coeffs.size();
    NTL::ZZ_pX polynomial;
    for (uint32_t i = 0; i < coeff_num; i++) {
        NTL::ZZFromBytes(e_ZZ, coeffs[i], static_cast<long>(primeByteLength));
        e_ZZ_p = NTL::to_ZZ_p(e_ZZ);
        NTL::SetCoeff(polynomial, static_cast<long>(i), e_ZZ_p);
    }
    uint32_t point_num = setX.size();
    if (point_num == 1) {
        setY.resize(1);
        // 如果只对一个点求值，则直接返回结果
        NTL::ZZFromBytes(e_ZZ, setX[0], static_cast<long>(primeByteLength));
        e_ZZ_p = NTL::to_ZZ_p(e_ZZ);
        e_ZZ_p = NTL::eval(polynomial, e_ZZ_p);
        // convert to byte[]
        e_ZZ = NTL::rep(e_ZZ_p);
        setY[0] = new uint8_t[primeByteLength];
        NTL::BytesFromZZ(setY[0], e_ZZ, static_cast<long>(primeByteLength));
    } else {
        // 一次可以并行计算的阶数要求是离polynomial.degree()最近的n = 2^k
        long polynomial_degree = NTL::deg(polynomial);
        long interval_num = NTL::deg(polynomial) == 0 ? 1 : 1 << (ceilLog2(polynomial_degree) - 1);
        long max_num = (long)std::ceil(point_num / (double)interval_num) * interval_num;;
        auto* x = new NTL::ZZ_p[max_num];
        auto* y = new NTL::ZZ_p[max_num];
        // build x and init y
        for (uint32_t i = 0; i < point_num; i++) {
            NTL::ZZFromBytes(e_ZZ, setX[i], static_cast<long>(primeByteLength));
            x[i] = NTL::to_ZZ_p(e_ZZ);
            y[i] = NTL::to_ZZ_p(0);
        }
        // 不足的补0
        for (uint32_t i = point_num; i < max_num; i++) {
            x[i] = NTL::to_ZZ_p(0);
            y[i] = NTL::to_ZZ_p(0);
        }
        for (long point_index = 0; point_index < max_num; point_index += interval_num) {
            // 一次取出interval_num个点
            auto* interval_x = x + point_index;
            auto* interval_y = y + point_index;
            evaluate(polynomial, interval_x, interval_y, interval_num);
        }
        // 返回结果
        setY.resize(point_num);
        for (uint32_t i = 0; i < point_num; i++) {
            e_ZZ = rep(y[i]);
            setY[i] = new uint8_t[primeByteLength];
            NTL::BytesFromZZ(setY[i], e_ZZ, (long) (primeByteLength));
        }
        // 清空内存
        delete[] x;
        delete[] y;
    }
}