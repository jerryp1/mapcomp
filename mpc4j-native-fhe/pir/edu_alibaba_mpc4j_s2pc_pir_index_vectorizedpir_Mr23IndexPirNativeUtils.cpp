//
// Created by pengliqiang on 2023/3/6.
//

#include "edu_alibaba_mpc4j_s2pc_pir_index_vectorizedpir_Mr23IndexPirNativeUtils.h"
#include "seal/seal.h"
#include "../serialize.h"
#include "../index_pir.h"

using namespace std;
using namespace seal;

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_vectorizedpir_Mr23IndexPirNativeUtils_generateSealContext(
        JNIEnv *env, jclass, jint poly_modulus_degree, jint plain_modulus_size, jintArray coeff_modulus_bits) {
    uint32_t coeff_size = env->GetArrayLength(coeff_modulus_bits);
    jint* coeff_ptr = env->GetIntArrayElements(coeff_modulus_bits, JNI_FALSE);
    vector<int32_t> bit_sizes(coeff_ptr, coeff_ptr + coeff_size);
    EncryptionParameters parms = EncryptionParameters(scheme_type::bfv);
    parms.set_poly_modulus_degree(poly_modulus_degree);
    parms.set_plain_modulus(PlainModulus::Batching(poly_modulus_degree, plain_modulus_size + 1));
    parms.set_coeff_modulus(CoeffModulus::Create(poly_modulus_degree, bit_sizes));
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

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_vectorizedpir_Mr23IndexPirNativeUtils_keyGen(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jint dimension_length, jint n_slot) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context = SEALContext(parms);
    KeyGenerator key_gen = KeyGenerator(context);
    const SecretKey &secret_key = key_gen.secret_key();
    PublicKey public_key;
    key_gen.create_public_key(public_key);
    RelinKeys relin_keys;
    key_gen.create_relin_keys(relin_keys);
    GaloisKeys galois_keys;
    vector<int32_t> steps;
    auto g = (int32_t) ((parms.poly_modulus_degree() / 2) / n_slot);
    for (int32_t i = 0; i < dimension_length; i++){
        steps.push_back(- i * g);
    }
    key_gen.create_galois_keys(steps, galois_keys);
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

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_vectorizedpir_Mr23IndexPirNativeUtils_preprocessDatabase(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jlongArray db_array, jint dim_length, jint n_slot, jint total_size) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    BatchEncoder batch_encoder(context);
    Evaluator evaluator(context);
    auto pid = context.first_parms_id();
    uint32_t degree = context.first_context_data()->parms().poly_modulus_degree();
    jint obj_size = env->GetArrayLength(db_array);
    auto *ptr = reinterpret_cast<uint64_t *>(env->GetLongArrayElements(db_array, JNI_FALSE));
    vector<uint64_t> db(ptr, ptr + obj_size);
    vector<Plaintext> encoded_db;
    encoded_db.resize(total_size);
    int32_t g = (int32_t) (degree / 2) / n_slot;
    uint32_t length = dim_length;
    for (auto i = 0; i < (int32_t) ceil((double) obj_size / dim_length); i++) {
        vector<uint64_t> temp(degree, 0ULL);
        if (i == (int32_t) ceil((double) obj_size / dim_length) - 1) {
            length = obj_size - dim_length * i;
        }
        for (int32_t j = 0; j < length; j++) {
            temp[j * g] = db[i * dim_length + j];
        }
        int32_t index = (i % dim_length) * g;
        vector<uint64_t> rotated_vec = rotate_plain(temp, index);
        encoded_db[i].resize(degree);
        batch_encoder.encode(rotated_vec, encoded_db[i]);
    }
    for (auto i = (int32_t) ceil((double) obj_size / dim_length); i < total_size; i++) {
        Plaintext pt(degree);
        vector<uint64_t> temp(degree, 0ULL);
        batch_encoder.encode(temp, pt);
        encoded_db[i] = pt;
    }
    for (auto & i : encoded_db){
        evaluator.transform_to_ntt_inplace(i, pid);
    }
    return serialize_plaintexts(env, encoded_db);
}

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_vectorizedpir_Mr23IndexPirNativeUtils_generateQuery(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray pk_bytes, jbyteArray sk_bytes, jintArray indices_array,
        jint n_slot) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    PublicKey public_key = deserialize_public_key(env, pk_bytes, context);
    SecretKey secret_key = deserialize_secret_key(env, sk_bytes, context);
    Encryptor encryptor(context, public_key, secret_key);
    BatchEncoder batch_encoder(context);
    uint32_t slot_count = batch_encoder.slot_count();
    jint dimension = env->GetArrayLength(indices_array);
    auto *ptr = reinterpret_cast<uint32_t *>(env->GetIntArrayElements(indices_array, JNI_FALSE));
    vector<uint32_t> indices(ptr, ptr + dimension);
    uint32_t degree = context.first_context_data()->parms().poly_modulus_degree();
    vector<Ciphertext> query(dimension);
    uint32_t g = (degree / 2) / n_slot;
    for (uint32_t i = 0; i < dimension; i++) {
        vector<uint64_t> vec(slot_count, 0ULL);
        for (uint32_t j = 0; j < degree; j++) {
            vec[j] = 0;
        }
        vec[indices[i] * g] = 1;
        Plaintext pt(degree);
        batch_encoder.encode(vec, pt);
        encryptor.encrypt(pt, query[i]);
    }
    return serialize_ciphertexts(env, query);
}

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_vectorizedpir_Mr23IndexPirNativeUtils_generateReply(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jobject query_list, jobject db_list, jbyteArray pk_bytes,
        jbyteArray relin_keys_bytes, jbyteArray galois_keys_bytes, jint dim_length, jint n_slot, jint dimension) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    PublicKey public_key = deserialize_public_key(env, pk_bytes, context);
    RelinKeys relin_keys = deserialize_relin_keys(env, relin_keys_bytes, context);
    GaloisKeys galois_keys = deserialize_galois_keys(env, galois_keys_bytes, context);
    Evaluator evaluator(context);
    Encryptor encryptor(context, public_key);
    vector<Ciphertext> query = deserialize_ciphertexts(env, query_list, context);
    vector<Plaintext> encoded_db = deserialize_plaintexts(env, db_list, context);
    BatchEncoder batch_encoder(context);
    uint32_t degree = context.first_context_data()->parms().poly_modulus_degree();
    auto g = (int32_t) ((degree / 2) / n_slot);
    vector<Ciphertext> rotated_ciphertexts(n_slot);
    for (int32_t i = 0; i < dim_length; i++) {
        evaluator.rotate_rows(query[0], - (i * g), galois_keys, rotated_ciphertexts[i]);
        evaluator.transform_to_ntt_inplace(rotated_ciphertexts[i]);
    }
    Ciphertext zero;
    encryptor.encrypt_zero(zero);
    evaluator.transform_to_ntt_inplace(zero);
    vector<Ciphertext> db_prime(encoded_db.size() / dim_length);
    for (int i = 0; i < ceil(encoded_db.size() / dim_length); i++) {
        db_prime[i] = zero;
        for (int j = 0; j < dim_length; j++) {
            Ciphertext temp;
            if (!encoded_db[i * dim_length + j].is_zero()) {
                evaluator.multiply_plain(rotated_ciphertexts[j], encoded_db[i * dim_length + j], temp);
            } else {
                temp = zero;
            }
            evaluator.add_inplace(db_prime[i], temp);
        }
        evaluator.transform_from_ntt_inplace(db_prime[i]);
    }
    vector<Ciphertext> cipher_temp;
    encryptor.encrypt_zero(zero);
    for (int i = 1; i < dimension - 1; i++) {
        cipher_temp.resize(ceil((double) db_prime.size() * 1.0 / (double) dim_length));
        for (int j = 0; j < ceil((double) db_prime.size() * 1.0 / (double) dim_length); j++) {
            cipher_temp[j] = zero;
            for (int k = 0; k < dim_length; k++) {
                Ciphertext t;
                evaluator.multiply(query[i], db_prime[j * dim_length + k], t);
                evaluator.relinearize_inplace(t, relin_keys);
                evaluator.rotate_rows_inplace(t, -k * g, galois_keys);
                evaluator.add_inplace(cipher_temp[j], t);
            }
        }
        db_prime = cipher_temp;
    }
    evaluator.multiply_inplace(db_prime[0], query[dimension - 1]);
    evaluator.relinearize_inplace(db_prime[0], relin_keys);
    if (db_prime[0].parms_id() != context.last_parms_id()) {
        evaluator.mod_switch_to_next_inplace(db_prime[0]);
    }
    return serialize_ciphertext(env, db_prime[0]);
}

JNIEXPORT jlong JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_vectorizedpir_Mr23IndexPirNativeUtils_decryptReply(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray sk_bytes, jbyteArray response_bytes, jint offset,
        jint n_slot) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    SecretKey secret_key = deserialize_secret_key(env, sk_bytes, context);
    Ciphertext response = deserialize_ciphertext(env, response_bytes, context);
    Decryptor decryptor(context, secret_key);
    BatchEncoder batch_encoder(context);
    Plaintext pt;
    decryptor.decrypt(response, pt);
    vector<uint64_t> vec;
    batch_encoder.decode(pt, vec);
    uint32_t degree = context.first_context_data()->parms().poly_modulus_degree();
    uint32_t g = (degree / 2) / n_slot;
    return (jlong) vec[offset * g];
}