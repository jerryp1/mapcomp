//
// Created by Liqiang Peng on 2022/7/14.
//

#include "apsi.h"
#include "serialize.h"

void try_clear_irrelevant_bits(const EncryptionParameters &parms, Ciphertext &ciphertext) {
    // If the parameter set has only one prime, we can compress the ciphertext by
    // setting low-order bits to zero. This effectively maxes out the noise, but that
    // doesn't matter as long as we don't use quite all noise budget.
    if (parms.coeff_modulus().size() == 1) {
        // The number of data bits we need to have left in each ciphertext coefficient
        int compr_coeff_bit_count =
                parms.plain_modulus().bit_count() +
                seal::util::get_significant_bit_count(parms.poly_modulus_degree())
                // Being pretty aggressive here
                - 1;
        int coeff_mod_bit_count = parms.coeff_modulus()[0].bit_count();
        // The number of bits to set to zero
        int irrelevant_bit_count = coeff_mod_bit_count - compr_coeff_bit_count;
        // Can compression achieve anything?
        if (irrelevant_bit_count > 0) {
            // Mask for zeroing out the irrelevant bits
            uint64_t mask = ~((uint64_t(1) << irrelevant_bit_count) - 1);
            seal_for_each_n(seal::util::iter(ciphertext), ciphertext.size(), [&](auto &&I) {
                // We only have a single RNS component so dereference once more
                seal_for_each_n(
                        *I, parms.poly_modulus_degree(), [&](auto &J) { J &= mask; });
            });
        }
    }
}

parms_id_type get_parms_id_for_chain_idx(const SEALContext& seal_context, size_t chain_idx) {
    // This function returns a parms_id matching the given chain index or -- if the chain
    // index is too large -- for the largest possible parameters (first data level).
    parms_id_type parms_id = seal_context.first_parms_id();
    while (seal_context.get_context_data(parms_id)->chain_index() > chain_idx) {
        parms_id = seal_context.get_context_data(parms_id)->next_context_data()->parms_id();
    }
    return parms_id;
}

jobject JNICALL genEncryptionParameters(JNIEnv *env, jint poly_modulus_degree, jlong plain_modulus,
                                        jintArray coeff_modulus_bits) {
    // 生成方案参数和密钥
    EncryptionParameters parms(scheme_type::bfv);
    parms.set_poly_modulus_degree(poly_modulus_degree);
    uint32_t coeff_size = env->GetArrayLength(coeff_modulus_bits);
    jint* coeff_ptr = env->GetIntArrayElements(coeff_modulus_bits, JNI_FALSE);
    vector<int> coeff_vec(coeff_ptr, coeff_ptr + coeff_size);
    parms.set_coeff_modulus(CoeffModulus::Create(poly_modulus_degree, coeff_vec));
    parms.set_plain_modulus(plain_modulus);
    SEALContext context = SEALContext(parms);
    KeyGenerator key_gen = KeyGenerator(context);
    const SecretKey &secret_key = key_gen.secret_key();
    Serializable relin_keys = key_gen.create_relin_keys();
    Serializable public_key = key_gen.create_public_key();
    // 获取ArrayList类引用
    jclass list_jcs = env->FindClass("java/util/ArrayList");
    if (list_jcs == nullptr) {
        std::cout << "ArrayList not found !" << std::endl;
        return nullptr;
    }
    // 获取ArrayList构造函数id
    jmethodID list_init = env->GetMethodID(list_jcs, "<init>", "()V");
    // 创建一个ArrayList对象
    jobject list_obj = env->NewObject(list_jcs, list_init, "");
    // 获取ArrayList对象的add()的methodID
    jmethodID list_add = env->GetMethodID(list_jcs, "add", "(Ljava/lang/Object;)Z");
    if (list_add == nullptr || list_init == nullptr) {
        std::cout << "'init' or 'add' method not found !" << std::endl;
        return nullptr;
    }
    // 序列化
    jbyteArray params_bytes = serialize_encryption_params(env, parms);
    jbyteArray pk_byte = serialize_public_key(env, public_key);
    jbyteArray relin_keys_byte = serialize_relin_key(env, relin_keys);
    jbyteArray sk_byte = serialize_secret_key(env, secret_key);
    // 添加到list
    env->CallBooleanMethod(list_obj, list_add, params_bytes);
    env->CallBooleanMethod(list_obj, list_add, relin_keys_byte);
    env->CallBooleanMethod(list_obj, list_add, pk_byte);
    env->CallBooleanMethod(list_obj, list_add, sk_byte);
    return list_obj;
}

jobject JNICALL generateQuery(JNIEnv *env, jobjectArray jenc_arrays, jbyteArray params_bytes, jbyteArray pk_bytes,
                              jbyteArray sk_bytes) {
    EncryptionParameters parms = deserialize_encryption_params(env, params_bytes);
    SEALContext context = SEALContext(parms);
    PublicKey public_key = deserialize_public_key(env, pk_bytes, context);
    SecretKey sk = deserialize_secret_key(env, sk_bytes, context);
    BatchEncoder encoder(context);
    Encryptor encryptor(context, public_key);
    encryptor.set_secret_key(sk);
    int size = env->GetArrayLength(jenc_arrays);
    // 获取ArrayList类引用
    jclass list_jcs = env->FindClass("java/util/ArrayList");
    if (list_jcs == nullptr) {
        std::cout << "ArrayList not found !" << std::endl;
        return nullptr;
    }
    // 获取ArrayList构造函数id
    jmethodID list_init = env->GetMethodID(list_jcs, "<init>", "()V");
    // 创建一个ArrayList对象
    jobject list_obj = env->NewObject(list_jcs, list_init, "");
    // 获取ArrayList对象的add()的methodID
    jmethodID list_add = env->GetMethodID(list_jcs, "add", "(Ljava/lang/Object;)Z");
    if (list_add == nullptr || list_init == nullptr) {
        std::cout << "'init' or 'add' method not found !" << std::endl;
        return nullptr;
    }
    for (int i = 0; i < size; i++) {
        auto rows = (jlongArray) env->GetObjectArrayElement(jenc_arrays, i);
        jlong *cols = env->GetLongArrayElements(rows, JNI_FALSE);
        vector<uint64_t> enc(cols, cols + encoder.slot_count());
        Plaintext plaintext;
        encoder.encode(enc, plaintext);
        Serializable ciphertext = encryptor.encrypt_symmetric(plaintext);
        jbyteArray bytes = serialize_ciphertext(env, ciphertext);
        env->CallBooleanMethod(list_obj, list_add, bytes);
    }
    return list_obj;
}


jlongArray JNICALL decodeReply(JNIEnv *env, jbyteArray response, jbyteArray params_bytes, jbyteArray sk_bytes) {
    EncryptionParameters parms = deserialize_encryption_params(env, params_bytes);
    SEALContext context = SEALContext(parms);
    SecretKey secret_key = deserialize_secret_key(env, sk_bytes, context);
    BatchEncoder encoder(context);
    Decryptor decryptor(context, secret_key);
    uint32_t slot_count = encoder.slot_count();
    vector<uint64_t> result;
    Ciphertext encrypted_match = deserialize_ciphertext(env, response, context);
    Plaintext decrypted;
    vector<uint64_t> dec_vec(slot_count);
    decryptor.decrypt(encrypted_match, decrypted);
    encoder.decode(decrypted, dec_vec);
    jlongArray arr;
    arr = env->NewLongArray((jsize) dec_vec.size());
    jlong fill[dec_vec.size()];
    for (int i = 0; i < dec_vec.size(); i++) {
        fill[i] = (jlong) dec_vec[i];
    }
    env->SetLongArrayRegion(arr, 0, (jsize) dec_vec.size(), fill);
    return arr;
}

jobject JNICALL computeEncryptedPowers(JNIEnv *env, jobject query_list, jbyteArray relin_keys_bytes,
                                       jbyteArray params_bytes, jobjectArray jparent_powers,
                                       jintArray jsource_power_index, jint ps_low_power) {
    // 反序列化
    EncryptionParameters params = deserialize_encryption_params(env, params_bytes);
    SEALContext context = SEALContext(params);
    RelinKeys relin_keys = deserialize_relin_key(env, relin_keys_bytes, context);
    Evaluator evaluator(context);
    // 密文查询
    vector<Ciphertext> query = deserialize_ciphertexts(env, query_list, context);
    // compute all the powers of the receiver's input.
    jint* index_ptr = env->GetIntArrayElements(jsource_power_index, JNI_FALSE);
    vector<uint32_t> source_power_index;
    source_power_index.reserve(env->GetArrayLength(jsource_power_index));
    for (int i = 0; i < env->GetArrayLength(jsource_power_index); i++) {
        source_power_index.push_back(index_ptr[i]);
    }
    uint32_t target_power_size = env->GetArrayLength(jparent_powers);
    uint32_t parent_powers[target_power_size][2];
    for (int i = 0; i < target_power_size; i++) {
        auto rows = (jintArray) env->GetObjectArrayElement(jparent_powers, i);
        jint* cols = env->GetIntArrayElements(rows, JNI_FALSE);
        parent_powers[i][0] = cols[0];
        parent_powers[i][1] = cols[1];
    }
    auto high_powers_parms_id = get_parms_id_for_chain_idx(context, 1);
    auto low_powers_parms_id = get_parms_id_for_chain_idx(context, 2);
    vector<Ciphertext> encrypted_powers;
    encrypted_powers.resize(target_power_size);
    if (ps_low_power == 0) {
        // 根据源密文的阶，对发送方密文排序
        for (int i = 0; i < query.size(); i++) {
            encrypted_powers[source_power_index[i] - 1] = query[i];
        }
        // 计算指定阶数密文
        for (int i = 0; i < target_power_size; i++) {
            if (parent_powers[i][1] != 0) {
                if (parent_powers[i][0] - 1 == parent_powers[i][1] - 1) {
                    evaluator.square(encrypted_powers[parent_powers[i][0] - 1], encrypted_powers[i]);
                } else {
                    evaluator.multiply(encrypted_powers[parent_powers[i][0] - 1],
                                       encrypted_powers[parent_powers[i][1] - 1], encrypted_powers[i]);
                }
                evaluator.relinearize_inplace(encrypted_powers[i], relin_keys);
            }
        }
    } else {
        // Paterson-Stockmeyer algorithm.
        uint32_t ps_high_degree = ps_low_power + 1;
        // 根据源密文的阶，对发送方密文排序
        for (int i = 0; i < query.size(); i++) {
            if (source_power_index[i] <= ps_low_power) {
                encrypted_powers[source_power_index[i] - 1] = query[i];
            } else {
                encrypted_powers[ps_low_power + (source_power_index[i] / ps_high_degree) - 1] = query[i];
            }
        }
        // 计算指定阶数密文
        for (int i = 0; i < ps_low_power; i++) {
            if (parent_powers[i][1] != 0) {
                if (parent_powers[i][0] - 1 == parent_powers[i][1] - 1) {
                    evaluator.square(encrypted_powers[parent_powers[i][0] - 1], encrypted_powers[i]);
                } else {
                    evaluator.multiply(encrypted_powers[parent_powers[i][0] - 1],
                                       encrypted_powers[parent_powers[i][1] - 1], encrypted_powers[i]);
                }
                evaluator.relinearize_inplace(encrypted_powers[i], relin_keys);
            }
        }
        for (int i = ps_low_power; i < target_power_size; i++) {
            if (parent_powers[i][1] != 0) {
                if (parent_powers[i][0] - 1 == parent_powers[i][1] - 1) {
                    evaluator.square(encrypted_powers[parent_powers[i][0] - 1 + ps_low_power], encrypted_powers[i]);
                } else {
                    evaluator.multiply(encrypted_powers[parent_powers[i][0] - 1 + ps_low_power],
                                       encrypted_powers[parent_powers[i][1] - 1 + ps_low_power], encrypted_powers[i]);
                }
                evaluator.relinearize_inplace(encrypted_powers[i], relin_keys);
            }
        }
    }
    return serialize_ciphertexts(env, encrypted_powers);
}

jbyteArray JNICALL computeMatches(JNIEnv *env, jobjectArray database_coeffs, jobject query_list,
                                  jbyteArray relin_keys_bytes, jbyteArray params_bytes, jint ps_low_power) {
    // 反序列化
    EncryptionParameters params = deserialize_encryption_params(env, params_bytes);
    SEALContext context = SEALContext(params);
    RelinKeys relin_keys = deserialize_relin_key(env, relin_keys_bytes, context);
    Evaluator evaluator(context);
    // encrypted query powers
    vector<Ciphertext> query_powers = deserialize_ciphertexts(env, query_list, context);
    auto high_powers_parms_id = get_parms_id_for_chain_idx(context, 1);
    auto low_powers_parms_id = get_parms_id_for_chain_idx(context, 2);
    for (int i = 0; i < ps_low_power; i++) {
        // Low powers must be at a higher level than high powers
        evaluator.mod_switch_to_inplace(query_powers[i], low_powers_parms_id);
        // Low powers must be in NTT form
        evaluator.transform_to_ntt_inplace(query_powers[i]);
    }
    for (int i = ps_low_power; i < query_powers.size(); i++) {
        // High powers are only modulus switched
        evaluator.mod_switch_to_inplace(query_powers[i], high_powers_parms_id);
    }
    // 服务端明文多项式
    vector<Plaintext> plaintexts = deserialize_plaintexts(env, database_coeffs, context);
    int ps_high_degree = ps_low_power + 1;
    for (int i = 0; i < plaintexts.size(); i++) {
        if ((i % ps_high_degree) != 0) {
            evaluator.transform_to_ntt_inplace(plaintexts[i], low_powers_parms_id);
        }
    }
    uint32_t degree = plaintexts.size() - 1;
    Ciphertext f_evaluated, cipher_temp, temp_in;
    f_evaluated.resize(context, high_powers_parms_id, 3);
    f_evaluated.is_ntt_form() = false;
    uint32_t ps_high_degree_powers = degree / ps_high_degree;
    // Calculate polynomial for i=1,...,ps_high_degree_powers-1
    for (int i = 1; i < ps_high_degree_powers; i++) {
        // Evaluate inner polynomial. The free term is left out and added later on.
        // The evaluation result is stored in temp_in.
        for (int j = 1; j < ps_high_degree; j++) {
            evaluator.multiply_plain(query_powers[j - 1], plaintexts[j + i * ps_high_degree], cipher_temp);
            if (j == 1) {
                temp_in = cipher_temp;
            } else {
                evaluator.add_inplace(temp_in, cipher_temp);
            }
        }
        // Transform inner polynomial to coefficient form
        evaluator.transform_from_ntt_inplace(temp_in);
        evaluator.mod_switch_to_inplace(temp_in, high_powers_parms_id);
        // The high powers are already in coefficient form
        evaluator.multiply_inplace(temp_in, query_powers[i - 1 + ps_low_power]);
        evaluator.add_inplace(f_evaluated, temp_in);
    }
    // Calculate polynomial for i=ps_high_degree_powers.
    // Done separately because here the degree of the inner poly is degree % ps_high_degree.
    // Once again, the free term will only be added later on.
    if (degree % ps_high_degree > 0 && ps_high_degree_powers > 0) {
        for (int i = 1; i <= degree % ps_high_degree; i++) {
            evaluator.multiply_plain(query_powers[i - 1],
                                     plaintexts[ps_high_degree * ps_high_degree_powers + i],
                                     cipher_temp);
            if (i == 1) {
                temp_in = cipher_temp;
            } else {
                evaluator.add_inplace(temp_in, cipher_temp);
            }
        }
        // Transform inner polynomial to coefficient form
        evaluator.transform_from_ntt_inplace(temp_in);
        evaluator.mod_switch_to_inplace(temp_in, high_powers_parms_id);
        // The high powers are already in coefficient form
        evaluator.multiply_inplace(temp_in, query_powers[ps_high_degree_powers - 1 + ps_low_power]);
        evaluator.add_inplace(f_evaluated, temp_in);
    }
    // Relinearize sum of ciphertext-ciphertext products
    if (!f_evaluated.is_transparent()) {
        evaluator.relinearize_inplace(f_evaluated, relin_keys);
    }
    // Calculate inner polynomial for i=0.
    // Done separately since there is no multiplication with a power of high-degree
    uint32_t length = ps_high_degree_powers == 0 ? degree : ps_low_power;
    for (size_t j = 1; j <= length; j++) {
        evaluator.multiply_plain(query_powers[j-1], plaintexts[j], cipher_temp);
        evaluator.transform_from_ntt_inplace(cipher_temp);
        evaluator.mod_switch_to_inplace(cipher_temp, high_powers_parms_id);
        evaluator.add_inplace(f_evaluated, cipher_temp);
    }
    // Add the constant coefficients of the inner polynomials multiplied by the respective powers of high-degree
    for (size_t i = 1; i < ps_high_degree_powers + 1; i++) {
        evaluator.multiply_plain(query_powers[i - 1 + ps_low_power], plaintexts[ps_high_degree * i], cipher_temp);
        evaluator.mod_switch_to_inplace(cipher_temp, high_powers_parms_id);
        evaluator.add_inplace(f_evaluated, cipher_temp);
    }
    // Add the constant coefficient
    evaluator.add_plain_inplace(f_evaluated, plaintexts[0]);
    while (f_evaluated.parms_id() != context.last_parms_id()) {
        evaluator.mod_switch_to_next_inplace(f_evaluated);
    }
    try_clear_irrelevant_bits(context.last_context_data()->parms(), f_evaluated);
    return serialize_ciphertext(env, f_evaluated);
}

jbyteArray JNICALL computeMatchesNaiveMethod(JNIEnv *env, jobjectArray database_coeffs, jobject query_list,
                                             jbyteArray relin_keys_bytes, jbyteArray params_bytes) {
    // 参数
    EncryptionParameters params = deserialize_encryption_params(env, params_bytes);
    SEALContext context = SEALContext(params);
    RelinKeys relin_keys = deserialize_relin_key(env, relin_keys_bytes, context);
    Evaluator evaluator(context);
    // encrypted query powers
    vector<Ciphertext> query_powers = deserialize_ciphertexts(env, query_list, context);
    auto high_powers_parms_id = get_parms_id_for_chain_idx(context, 1);
    auto low_powers_parms_id = get_parms_id_for_chain_idx(context, 2);
    for (auto & query_power : query_powers) {
        // Only one ciphertext-plaintext multiplication is needed after this
        evaluator.mod_switch_to_inplace(query_power, high_powers_parms_id);
        // All powers must be in NTT form
        evaluator.transform_to_ntt_inplace(query_power);
    }
    // 服务端明文多项式
    vector<Plaintext> plaintexts = deserialize_plaintexts(env, database_coeffs, context);
    for (int i = 1; i < plaintexts.size(); i++) {
        evaluator.transform_to_ntt_inplace(plaintexts[i], low_powers_parms_id);
    }
    uint32_t degree = plaintexts.size() - 1;
    Ciphertext f_evaluated, cipher_temp, temp_in;
    f_evaluated.resize(context, high_powers_parms_id, 3);
    f_evaluated.is_ntt_form() = false;
    for (int i = 1; i <= degree; i++) {
        evaluator.multiply_plain(query_powers[i-1], plaintexts[i], cipher_temp);
        if (i == 1) {
            temp_in = cipher_temp;
        } else {
            evaluator.add_inplace(temp_in, cipher_temp);
        }
    }
    // Add the constant coefficient
    evaluator.transform_from_ntt(temp_in, f_evaluated);
    evaluator.add_plain_inplace(f_evaluated, plaintexts[0]);
    // Make the result as small as possible by modulus switching and possibly clearing irrelevant bits.
    while (f_evaluated.parms_id() != context.last_parms_id()) {
        evaluator.mod_switch_to_next_inplace(f_evaluated);
    }
    try_clear_irrelevant_bits(context.last_context_data()->parms(), f_evaluated);
    return serialize_ciphertext(env, f_evaluated);
}

jbyteArray JNICALL computeMatches(JNIEnv *env, jobjectArray database_coeffs, jobject query_list,
                                  jbyteArray relin_keys_bytes, jbyteArray pk_bytes, jbyteArray params_bytes,
                                  jint ps_low_power) {
    // 反序列化
    EncryptionParameters params = deserialize_encryption_params(env, params_bytes);
    SEALContext context = SEALContext(params);
    PublicKey public_key = deserialize_public_key(env, pk_bytes, context);
    RelinKeys relin_keys = deserialize_relin_key(env, relin_keys_bytes, context);
    Encryptor encryptor(context, public_key);
    Evaluator evaluator(context);
    // encrypted query powers
    vector<Ciphertext> query_powers = deserialize_ciphertexts(env, query_list, context);
    auto high_powers_parms_id = get_parms_id_for_chain_idx(context, 1);
    auto low_powers_parms_id = get_parms_id_for_chain_idx(context, 2);
    for (int i = 0; i < ps_low_power; i++) {
        // Low powers must be at a higher level than high powers
        evaluator.mod_switch_to_inplace(query_powers[i], low_powers_parms_id);
        // Low powers must be in NTT form
        evaluator.transform_to_ntt_inplace(query_powers[i]);
    }
    for (int i = ps_low_power; i < query_powers.size(); i++) {
        // High powers are only modulus switched
        evaluator.mod_switch_to_inplace(query_powers[i], high_powers_parms_id);
    }
    // 服务端明文多项式
    vector<Plaintext> plaintexts = deserialize_plaintexts(env, database_coeffs, context);
    int ps_high_degree = ps_low_power + 1;
    for (int i = 0; i < plaintexts.size(); i++) {
        if ((i % ps_high_degree) != 0) {
            evaluator.transform_to_ntt_inplace(plaintexts[i], low_powers_parms_id);
        }
    }

    uint32_t degree = plaintexts.size() - 1;
    Ciphertext f_evaluated, cipher_temp, temp_in;
    f_evaluated.resize(context, high_powers_parms_id, 3);
    f_evaluated.is_ntt_form() = false;
    uint32_t ps_high_degree_powers = degree / ps_high_degree;
    // Calculate polynomial for i=1,...,ps_high_degree_powers-1
    for (int i = 1; i < ps_high_degree_powers; i++) {
        // Evaluate inner polynomial. The free term is left out and added later on.
        // The evaluation result is stored in temp_in.
        for (int j = 1; j < ps_high_degree; j++) {
            evaluator.multiply_plain(query_powers[j - 1], plaintexts[j + i * ps_high_degree], cipher_temp);
            if (j == 1) {
                temp_in = cipher_temp;
            } else {
                evaluator.add_inplace(temp_in, cipher_temp);
            }
        }
        // Transform inner polynomial to coefficient form
        evaluator.transform_from_ntt_inplace(temp_in);
        evaluator.mod_switch_to_inplace(temp_in, high_powers_parms_id);
        // The high powers are already in coefficient form
        evaluator.multiply_inplace(temp_in, query_powers[i - 1 + ps_low_power]);
        evaluator.add_inplace(f_evaluated, temp_in);
    }
    // Calculate polynomial for i=ps_high_degree_powers.
    // Done separately because here the degree of the inner poly is degree % ps_high_degree.
    // Once again, the free term will only be added later on.
    if (degree % ps_high_degree > 0 && ps_high_degree_powers > 0) {
        for (int i = 1; i <= degree % ps_high_degree; i++) {
            evaluator.multiply_plain(query_powers[i - 1],
                                     plaintexts[ps_high_degree * ps_high_degree_powers + i],
                                     cipher_temp);
            if (i == 1) {
                temp_in = cipher_temp;
            } else {
                evaluator.add_inplace(temp_in, cipher_temp);
            }
        }
        // Transform inner polynomial to coefficient form
        evaluator.transform_from_ntt_inplace(temp_in);
        evaluator.mod_switch_to_inplace(temp_in, high_powers_parms_id);
        // The high powers are already in coefficient form
        evaluator.multiply_inplace(temp_in, query_powers[ps_high_degree_powers - 1 + ps_low_power]);
        evaluator.add_inplace(f_evaluated, temp_in);
    }

    // Relinearize sum of ciphertext-ciphertext products
    if (!f_evaluated.is_transparent()) {
        evaluator.relinearize_inplace(f_evaluated, relin_keys);
    }

    // Calculate inner polynomial for i=0.
    // Done separately since there is no multiplication with a power of high-degree
    uint32_t length = ps_high_degree_powers == 0 ? degree : ps_low_power;
    for (size_t j = 1; j <= length; j++) {
        evaluator.multiply_plain(query_powers[j-1], plaintexts[j], cipher_temp);
        evaluator.transform_from_ntt_inplace(cipher_temp);
        evaluator.mod_switch_to_inplace(cipher_temp, high_powers_parms_id);
        evaluator.add_inplace(f_evaluated, cipher_temp);
    }

    // Add the constant coefficients of the inner polynomials multiplied by the respective powers of high-degree
    for (size_t i = 1; i < ps_high_degree_powers + 1; i++) {
        evaluator.multiply_plain(query_powers[i - 1 + ps_low_power], plaintexts[ps_high_degree * i], cipher_temp);
        evaluator.mod_switch_to_inplace(cipher_temp, high_powers_parms_id);
        evaluator.add_inplace(f_evaluated, cipher_temp);
    }
    // Add the constant coefficient
    if (degree > 0) {
        evaluator.add_plain_inplace(f_evaluated, plaintexts[0]);
    } else {
        encryptor.encrypt(plaintexts[0], f_evaluated);
    }

    while (f_evaluated.parms_id() != context.last_parms_id()) {
        evaluator.mod_switch_to_next_inplace(f_evaluated);
    }
    try_clear_irrelevant_bits(context.last_context_data()->parms(), f_evaluated);

    return serialize_ciphertext(env, f_evaluated);
}

jbyteArray JNICALL computeMatchesNaiveMethod(JNIEnv *env, jobjectArray database_coeffs, jobject query_list,
                                             jbyteArray relin_keys_bytes, jbyteArray params_bytes, jbyteArray pk_bytes) {
    // 参数
    EncryptionParameters params = deserialize_encryption_params(env, params_bytes);
    SEALContext context = SEALContext(params);
    PublicKey public_key = deserialize_public_key(env, pk_bytes, context);
    RelinKeys relin_keys = deserialize_relin_key(env, relin_keys_bytes, context);
    Encryptor encryptor(context, public_key);
    Evaluator evaluator(context);
    // encrypted query powers
    vector<Ciphertext> query_powers = deserialize_ciphertexts(env, query_list, context);
    auto high_powers_parms_id = get_parms_id_for_chain_idx(context, 1);
    auto low_powers_parms_id = get_parms_id_for_chain_idx(context, 2);
    for (auto & query_power : query_powers) {
        // Only one ciphertext-plaintext multiplication is needed after this
        evaluator.mod_switch_to_inplace(query_power, high_powers_parms_id);
        // All powers must be in NTT form
        evaluator.transform_to_ntt_inplace(query_power);
    }
    // 服务端明文多项式
    vector<Plaintext> plaintexts = deserialize_plaintexts(env, database_coeffs, context);
    for (int i = 1; i < plaintexts.size(); i++) {
        evaluator.transform_to_ntt_inplace(plaintexts[i], low_powers_parms_id);
    }
    uint32_t degree = plaintexts.size() - 1;
    Ciphertext f_evaluated, cipher_temp, temp_in;
    f_evaluated.resize(context, high_powers_parms_id, 3);
    f_evaluated.is_ntt_form() = false;

    for (int i = 1; i <= degree; i++) {
        evaluator.multiply_plain(query_powers[i-1], plaintexts[i], cipher_temp);
        if (i == 1) {
            temp_in = cipher_temp;
        } else {
            evaluator.add_inplace(temp_in, cipher_temp);
        }
    }
    // Add the constant coefficient
    if (degree > 0) {
        evaluator.transform_from_ntt(temp_in, f_evaluated);
        evaluator.add_plain_inplace(f_evaluated, plaintexts[0]);
    } else {
        encryptor.encrypt(plaintexts[0], f_evaluated);
    }
    // Make the result as small as possible by modulus switching and possibly clearing irrelevant bits.
    while (f_evaluated.parms_id() != context.last_parms_id()) {
        evaluator.mod_switch_to_next_inplace(f_evaluated);
    }
    try_clear_irrelevant_bits(context.last_context_data()->parms(), f_evaluated);
    return serialize_ciphertext(env, f_evaluated);
}