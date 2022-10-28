//
// Created by Weiran Liu on 2022/10/28.
//
#include <NTL/ZZ_p.h>
#include <NTL/ZZ_pX.h>
#include <NTL/ZZ.h>
#include <vector>

#include "../common/defines.h"

#ifndef MPC4J_NATIVE_TOOL_NTL_TREE_ZP_H
#define MPC4J_NATIVE_TOOL_NTL_TREE_ZP_H

/**
  * 给定byte[]格式的{x_i, y_i}，得到插值多项式。
  *
  * @param num 插值数量。如果实际插值数量不够，则自动补充虚拟元素。
  * @param setX 集合{x_i}。
  * @param setY 集合{y_i}。
  * @param coeffs 插值多项式系数。
  */
void zp_tree_interpolate(uint64_t primeByteLength, uint64_t num, std::vector<uint8_t*> &setX, std::vector<uint8_t*> &setY, std::vector<uint8_t*> &coeffs);

/**
  * 给定byte[]格式的多项式系数和byte[]格式的x，求y = f(x)。
  *
  * @param coeffs byte[]格式的多项式系数。
  * @param setX 集合{x_i}。
  * @param y 求值结果{y_i}。
  */
void zp_tree_evaluate(uint64_t primeByteLength, std::vector<uint8_t*> &coeffs, std::vector<uint8_t*> &setX, std::vector<uint8_t*> &y);

#endif //MPC4J_NATIVE_TOOL_NTL_TREE_ZP_H
