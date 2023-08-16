//
// Created by pengliqiang on 2023/3/10.
//

#include "edu_alibaba_mpc4j_s2pc_pir_index_batch_vectorizedpir_Mr23BatchIndexPirNativeUtils.h"
#include "seal/seal.h"
#include "../serialize.h"
#include "../index_pir.h"
#include "../utils.h"

using namespace std;
using namespace seal;

[[maybe_unused]] JNIEXPORT
jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_batch_vectorizedpir_Mr23BatchIndexPirNativeUtils_generateEncryptionParams(
        JNIEnv *env, jclass, jint poly_modulus_degree, jint plain_modulus_size) {
    EncryptionParameters parms = EncryptionParameters(scheme_type::bfv);
    parms.set_poly_modulus_degree(poly_modulus_degree);
    parms.set_plain_modulus(PlainModulus::Batching(poly_modulus_degree, plain_modulus_size));
    parms.set_coeff_modulus(CoeffModulus::Create(poly_modulus_degree, {55, 55, 48, 60}));
    SEALContext context = SEALContext(parms);
    jclass exception = env->FindClass("java/lang/Exception");
    if (!context.parameters_set()) {
        env->ThrowNew(exception, "SEAL parameters not valid.");
        return nullptr;
    }
    if (!context.first_context_data()->qualifiers().using_batching) {
        env->ThrowNew(exception, "SEAL parameters do not support batching.");
        return nullptr;
    }
    if (!context.using_keyswitching()) {
        env->ThrowNew(exception, "SEAL parameters do not support key switching.");
        return nullptr;
    }
    return serialize_encryption_parms(env, parms);
}

[[maybe_unused]] JNIEXPORT
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_batch_vectorizedpir_Mr23BatchIndexPirNativeUtils_keyGen(
        JNIEnv *env, jclass, jbyteArray parms_bytes) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context = SEALContext(parms);
    KeyGenerator key_gen = KeyGenerator(context);
    const SecretKey &secret_key = key_gen.secret_key();
    Serializable<PublicKey> public_key = key_gen.create_public_key();
    Serializable<RelinKeys> relin_keys = key_gen.create_relin_keys();
    Serializable<GaloisKeys> galois_keys = key_gen.create_galois_keys();
    jclass list_jcs = env->FindClass("java/util/ArrayList");
    jmethodID list_init = env->GetMethodID(list_jcs, "<init>", "()V");
    jobject list_obj = env->NewObject(list_jcs, list_init, "");
    jmethodID list_add = env->GetMethodID(list_jcs, "add", "(Ljava/lang/Object;)Z");
    jbyteArray galois_keys_bytes = serialize_galois_keys(env, galois_keys);
    jbyteArray relin_keys_bytes = serialize_relin_keys(env, relin_keys);
    jbyteArray pk_bytes = serialize_public_key(env, public_key);
    jbyteArray sk_bytes = serialize_secret_key(env, secret_key);
    env->CallBooleanMethod(list_obj, list_add, pk_bytes);
    env->CallBooleanMethod(list_obj, list_add, sk_bytes);
    env->CallBooleanMethod(list_obj, list_add, relin_keys_bytes);
    env->CallBooleanMethod(list_obj, list_add, galois_keys_bytes);
    return list_obj;
}

[[maybe_unused]] JNIEXPORT
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_batch_vectorizedpir_Mr23BatchIndexPirNativeUtils_preprocessDatabase(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jobjectArray coeffs_list) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    BatchEncoder batch_encoder(context);
    Evaluator evaluator(context);
    auto pid = context.first_parms_id();
    vector<Plaintext> encoded_db = deserialize_plaintexts(env, coeffs_list, context);
    for (auto & i : encoded_db){
        evaluator.transform_to_ntt_inplace(i, pid);
    }
    return serialize_plaintexts(env, encoded_db);
}

[[maybe_unused]] JNIEXPORT
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_batch_vectorizedpir_Mr23BatchIndexPirNativeUtils_generateQuery(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray pk_bytes, jbyteArray sk_bytes, jobjectArray query_array) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    PublicKey public_key = deserialize_public_key(env, pk_bytes, context);
    SecretKey secret_key = deserialize_secret_key(env, sk_bytes, context);
    Encryptor encryptor(context, public_key);
    encryptor.set_secret_key(secret_key);
    vector<Plaintext> plaintexts = deserialize_plaintexts(env, query_array, context);
    vector<Serializable<Ciphertext>> query;
    for (auto & plaintext : plaintexts) {
        query.push_back(encryptor.encrypt_symmetric(plaintext));
    }
    return serialize_ciphertexts(env, query);
}

[[maybe_unused]] JNIEXPORT
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_batch_vectorizedpir_Mr23BatchIndexPirNativeUtils_generateReply(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jobject query_list, jobject db_list, jbyteArray pk_bytes,
        jbyteArray relin_keys_bytes, jbyteArray galois_keys_bytes, jint first_two_dimension_size, jint third_dimension_size) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    PublicKey public_key = deserialize_public_key(env, pk_bytes, context);
    RelinKeys relin_keys = deserialize_relin_keys(env, relin_keys_bytes, context);
    GaloisKeys* galois_keys = deserialize_galois_keys(env, galois_keys_bytes, context);
    Evaluator evaluator(context);
    Encryptor encryptor(context, public_key);
    vector<Ciphertext> query = deserialize_ciphertexts(env, query_list, context);
    vector<Plaintext> encoded_db = deserialize_plaintexts(env, db_list, context);
    BatchEncoder batch_encoder(context);
    uint32_t degree = parms.poly_modulus_degree();
    auto g = (int32_t) ((degree / 2) / first_two_dimension_size);
    vector<Ciphertext> rotated_query(first_two_dimension_size);
    for (int32_t i = 0; i < first_two_dimension_size; i++) {
        evaluator.rotate_rows(query[0], - (i * g), *galois_keys, rotated_query[i]);
        evaluator.transform_to_ntt_inplace(rotated_query[i]);
    }
    // first dimension
    vector<Ciphertext> first_dimension_ciphers;
    Ciphertext ct_acc, ct, ct1;
    auto &coeff_modulus = context.first_context_data()->parms().coeff_modulus();
    size_t coeff_count = parms.poly_modulus_degree();
    size_t coeff_mod_count = coeff_modulus.size();
    size_t encrypted_ntt_size = rotated_query[0].size();
    for (int32_t col_id = 0; col_id < encoded_db.size(); col_id += first_two_dimension_size) {
        std::vector<std::vector<uint128_t>> buffer(encrypted_ntt_size, std::vector<uint128_t>(coeff_count * coeff_mod_count, 1));
        for (int32_t i = 0; i < first_two_dimension_size; i++) {
            for (size_t poly_id = 0; poly_id < encrypted_ntt_size; poly_id++) {
                multiply_poly_acum(rotated_query[i].data(poly_id), encoded_db[col_id + i].data(), coeff_count * coeff_mod_count, buffer[poly_id].data());
            }
        }
        ct_acc = rotated_query[0];
        for (int32_t poly_id = 0; poly_id < encrypted_ntt_size; poly_id++) {
            auto ct_ptr = ct_acc.data(poly_id);
            auto pt_ptr = buffer[poly_id];
            for (int32_t mod_id = 0; mod_id < coeff_mod_count; mod_id++) {
                auto mod_idx = (mod_id * coeff_count);
                for (int coeff_id = 0; coeff_id < coeff_count; coeff_id++) {
                    pt_ptr[coeff_id + mod_idx] = pt_ptr[coeff_id + mod_idx] % static_cast<__uint128_t>(coeff_modulus[mod_id].value());
                    ct_ptr[coeff_id + mod_idx] = static_cast<uint64_t>(pt_ptr[coeff_id + mod_idx]);
                }
            }
        }
        evaluator.transform_from_ntt_inplace(ct_acc);
        first_dimension_ciphers.push_back(ct_acc);
    }
    // second dimension
    vector<Ciphertext> second_dimension_cipher;
    for (int32_t idx = 0; idx < first_dimension_ciphers.size(); idx += third_dimension_size) {
        evaluator.multiply(query[1], first_dimension_ciphers[idx], ct_acc);
        evaluator.mod_switch_to_next_inplace(ct_acc);
        evaluator.relinearize_inplace(ct_acc, relin_keys);
        for (int32_t i = 1; i < third_dimension_size; i++) {
            evaluator.multiply(query[1], first_dimension_ciphers[idx + i], ct1);
            evaluator.mod_switch_to_next_inplace(ct1);
            evaluator.relinearize_inplace(ct1, relin_keys);
            evaluator.rotate_rows_inplace(ct1, -i * g, *galois_keys);
            evaluator.add_inplace(ct_acc, ct1);
        }
        second_dimension_cipher.push_back(ct_acc);
    }
    // third dimension
    vector<Ciphertext> result;
    evaluator.mod_switch_to_inplace(query[2], second_dimension_cipher[0].parms_id());
    for (auto & idx : second_dimension_cipher) {
        evaluator.multiply(query.back(), idx, ct);
        evaluator.relinearize_inplace(ct, relin_keys);
        result.push_back(ct);
    }
    return serialize_ciphertexts(env, result);
}

[[maybe_unused]] JNIEXPORT
jlongArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_batch_vectorizedpir_Mr23BatchIndexPirNativeUtils_decryptReply(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray sk_bytes, jbyteArray response_bytes) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    SecretKey secret_key = deserialize_secret_key(env, sk_bytes, context);
    Ciphertext response = deserialize_ciphertext(env, response_bytes, context);
    Decryptor decryptor(context, secret_key);
    BatchEncoder batch_encoder(context);
    Plaintext pt;
    int32_t noise_budget = decryptor.invariant_noise_budget(response);
    jclass exception = env->FindClass("java/lang/Exception");
    if (noise_budget == 0) {
        env->ThrowNew(exception, "noise budget is 0.");
        return nullptr;
    }
    decryptor.decrypt(response, pt);
    vector<uint64_t> vec;
    batch_encoder.decode(pt, vec);
    uint32_t degree = context.first_context_data()->parms().poly_modulus_degree();
    jlongArray jarr = env->NewLongArray((jsize) degree);
    jlong *arr = env->GetLongArrayElements(jarr, JNI_FALSE);
    for(uint32_t i = 0; i < degree; i++){
        arr[i] = (jlong) vec[i];
    }
    env->ReleaseLongArrayElements(jarr, arr, 0);
    return jarr;
}

[[maybe_unused]] JNIEXPORT
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_batch_vectorizedpir_Mr23BatchIndexPirNativeUtils_mergeResponse(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray galois_keys_bytes, jobject response_list,
        jint num_slots_per_entry, jint first_two_dimension_size) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    GaloisKeys* galois_keys = deserialize_galois_keys(env, galois_keys_bytes, context);
    Evaluator evaluator(context);
    BatchEncoder batch_encoder(context);
    uint32_t row_size = parms.poly_modulus_degree() / 2;
    auto g = (int32_t) (row_size / first_two_dimension_size);
    Ciphertext merged_response, r_cipher;
    vector<Ciphertext> deserialized_response = deserialize_ciphertexts(env, response_list, context);
    uint32_t size = deserialized_response.size() / num_slots_per_entry;
    vector<vector<Ciphertext>> responses;
    responses.reserve(size);
    for (uint32_t i = 0; i < size; i++) {
        vector<Ciphertext> temp;
        temp.reserve(num_slots_per_entry);
        for (uint32_t j = 0; j < num_slots_per_entry; j++) {
            temp.push_back(deserialized_response[j * size + i]);
        }
        responses.push_back(temp);
    }
    uint32_t num_slots_per_entry_rounded = get_next_power_of_two(num_slots_per_entry);
    uint32_t max_empty_slots = first_two_dimension_size;
    auto num_chunk_ctx = ceil(num_slots_per_entry * 1.0 / max_empty_slots);
    vector<Ciphertext> chunk_response;
    for (uint32_t i = 0; i < size; i++) {
        auto remaining_slots_entry = num_slots_per_entry;
        for (uint32_t j = 0; j < num_chunk_ctx; j++) {
            auto chunk_idx = j * max_empty_slots;
            jint loop = std::min((int32_t) max_empty_slots, remaining_slots_entry);
            Ciphertext chunk_ct_acc = responses[i][chunk_idx];
            for (int32_t k = 1; k < loop; k++)
            {
                evaluator.rotate_rows_inplace(responses[i][chunk_idx + k], -k * g, *galois_keys);
                evaluator.add_inplace(chunk_ct_acc, responses[i][chunk_idx + k]);
            }
            remaining_slots_entry -= loop;
            chunk_response.push_back(chunk_ct_acc);
        }
    }
    auto current_fill = g * num_slots_per_entry;
    size_t num_buckets_merged = row_size / current_fill;
    if (ceil(num_slots_per_entry * 1.0 / max_empty_slots) > 1 || num_buckets_merged <= 1 || chunk_response.size() == 1) {
        for (auto & i : chunk_response) {
            if (i.parms_id() != context.last_parms_id()) {
                evaluator.mod_switch_to_next_inplace(i);
            }
        }
        return serialize_ciphertexts(env, chunk_response);
    }
    current_fill = g * (jint) num_slots_per_entry_rounded;
    auto merged_ctx_needed = ceil(((double) chunk_response.size() * current_fill * 1.0) / row_size);
    vector<Ciphertext> chunk_bucket_responses;
    for (int32_t i = 0; i < merged_ctx_needed; i++) {
        Ciphertext ct_acc;
        for (int32_t j = 0; j < num_buckets_merged; j++) {
            if (i * num_buckets_merged + j < chunk_response.size()) {
                Ciphertext copy_ct_acc = chunk_response[i * num_buckets_merged + j];
                Ciphertext tmp_ct = copy_ct_acc;
                for (int32_t k = 1; k < row_size / current_fill; k *= 2) {
                    evaluator.rotate_rows_inplace(tmp_ct, -k * current_fill, *galois_keys);
                    evaluator.add_inplace(copy_ct_acc, tmp_ct);
                    tmp_ct = copy_ct_acc;
                }
                std::vector<uint64_t> selection_vector(parms.poly_modulus_degree(), 0ULL);
                std::fill_n(selection_vector.begin() + (j * current_fill), current_fill, 1ULL);
                std::fill_n(selection_vector.begin() + row_size + (j * current_fill), current_fill, 1ULL);
                Plaintext selection_pt;
                batch_encoder.encode(selection_vector, selection_pt);
                evaluator.multiply_plain_inplace(copy_ct_acc, selection_pt);
                if (j == 0) {
                    ct_acc = copy_ct_acc;
                } else {
                    evaluator.add_inplace(ct_acc, copy_ct_acc);
                }
            }
        }
        chunk_bucket_responses.push_back(ct_acc);
    }
    for (auto & i : chunk_bucket_responses) {
        if (i.parms_id() != context.last_parms_id()) {
            evaluator.mod_switch_to_next_inplace(i);
        }
    }
    return serialize_ciphertexts(env, chunk_bucket_responses);
}