//
// Created by pengliqiang on 2022/9/8.
//

#ifndef MPC4J_NATIVE_FHE_UTILS_H
#define MPC4J_NATIVE_FHE_UTILS_H

#include <iomanip>
#include "seal/seal.h"

using namespace seal;

parms_id_type get_parms_id_for_chain_idx(const SEALContext& seal_context, size_t chain_idx);

#endif //MPC4J_NATIVE_FHE_UTILS_H
