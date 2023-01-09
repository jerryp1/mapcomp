#pragma once
#include <cmath>
#include <cstdint>
#include <vector>

struct targetP {
    static constexpr size_t l_ = 7; // lvl2 param
    // static const std::vector<int> coeff = {60, 60, 60};
    static constexpr size_t digits = 122; // uint64_t
    static constexpr size_t Bgbit = 17;   // lvl2 param
    static constexpr uint64_t Bg = 1 << Bgbit;
};
