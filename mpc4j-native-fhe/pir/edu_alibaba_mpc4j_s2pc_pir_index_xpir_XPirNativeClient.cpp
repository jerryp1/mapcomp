//
// Created by pengliqiang on 2022/9/13.
//

#include "edu_alibaba_mpc4j_s2pc_pir_index_xpir_XPirNativeClient.h"
#include "seal/seal.h"
#include "../index_pir.h"
#include "../serialize.h"

using namespace seal;
using namespace std;

[[maybe_unused]] JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_xpir_XPirNativeClient_keyGeneration(
        JNIEnv *env, jclass, jbyteArray params_byte) {
    EncryptionParameters parms = deserialize_encryption_params(env, params_byte);
    SEALContext context(parms);
    KeyGenerator key_gen(context);
    const SecretKey& secret_key = key_gen.secret_key();
    Serializable public_key = key_gen.create_public_key();
    return serialize_public_key_secret_key(env, public_key, secret_key);
}

[[maybe_unused]] JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_xpir_XPirNativeClient_generateQuery(
        JNIEnv *env, jclass, jbyteArray params_byte, jbyteArray pk_byte, jbyteArray sk_byte, jintArray message_list) {
    EncryptionParameters parms = deserialize_encryption_params(env, params_byte);
    SEALContext context(parms);
    auto exception = env->FindClass("java/lang/Exception");
    PublicKey public_key = deserialize_public_key(env, pk_byte, context);
    if (!is_metadata_valid_for(public_key, context)) {
        env->ThrowNew(exception, "invalid public key for this SEALContext!");
    }
    SecretKey secret_key = deserialize_secret_key(env, sk_byte, context);
    if (!is_metadata_valid_for(secret_key, context)) {
        env->ThrowNew(exception, "invalid secret key for this SEALContext!");
    }
    Encryptor encryptor(context, public_key, secret_key);
    auto pool = MemoryManager::GetPool();
    int size = env->GetArrayLength(message_list);
    jint *ptr = env->GetIntArrayElements(message_list, JNI_FALSE);
    vector<uint32_t> vec(ptr, ptr + size);
    vector<Ciphertext> ciphertexts;
    ciphertexts.resize(size);
    for (uint32_t i = 0; i < size; i++) {
        Plaintext plaintext(parms.poly_modulus_degree());
        plaintext.set_zero();
        if (vec[i] == 1) {
            plaintext[0] = 1;
            encryptor.encrypt_symmetric(plaintext, ciphertexts[i]);
        } else {
            encryptor.encrypt_zero_symmetric(ciphertexts[i]);
        }
    }
    return serialize_ciphertexts(env, ciphertexts);
}

[[maybe_unused]] JNIEXPORT jlongArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_xpir_XPirNativeClient_decodeReply(
        JNIEnv *env, jclass, jbyteArray params_byte, jbyteArray sk_byte, jobject response_list, jint d) {
    EncryptionParameters parms = deserialize_encryption_params(env, params_byte);
    SEALContext context(parms);
    auto exception = env->FindClass("java/lang/Exception");
    SecretKey secret_key = deserialize_secret_key(env, sk_byte, context);
    if (!is_metadata_valid_for(secret_key, context)) {
        env->ThrowNew(exception, "invalid secret key for this SEALContext!");
    }
    Decryptor decryptor(context, secret_key);
    parms = context.last_context_data()->parms();
    parms_id_type parms_id = context.last_parms_id();
    uint32_t exp_ratio = compute_expansion_ratio(parms);
    uint32_t recursion_level = d;
    vector<Ciphertext> temp = deserialize_ciphertexts(env, response_list, context);
    uint32_t ciphertext_size = temp[0].size();
    for (uint32_t i = 0; i < recursion_level; i++) {
#ifdef DEBUG
        cout << "Client: " << i + 1 << "/ " << recursion_level << "-th decryption layer started." << endl;
#endif
        vector<Ciphertext> newtemp;
        vector<Plaintext> tempplain;
        for (uint32_t j = 0; j < temp.size(); j++) {
            Plaintext ptxt;
            decryptor.decrypt(temp[j], ptxt);
            tempplain.push_back(ptxt);
#ifdef DEBUG
            cout << "Client: reply noise budget = " << decryptor.invariant_noise_budget(temp[j]) << endl;
            cout << "decoded (and scaled) plaintext = " << ptxt.to_string() << endl;
            cout << "recursion level : " << i << " noise budget : " << decryptor.invariant_noise_budget(temp[j]) << endl;
#endif
            if ((j + 1) % (exp_ratio * ciphertext_size) == 0 && j > 0) {
                // Combine into one ciphertext.
                Ciphertext combined(context, parms_id);
                compose_to_ciphertext(parms, tempplain, combined);
                newtemp.push_back(combined);
                tempplain.clear();
            }
        }
        if (i == recursion_level - 1) {
            if (temp.size() != 1) {
                env->ThrowNew(exception, "decode response failed!");
            }
            return get_plaintext_coeffs(env, tempplain[0]);
        } else {
            tempplain.clear();
            temp = newtemp;
        }
    }
    // This should never be called
    env->ThrowNew(exception, "decode response failed!");
    return nullptr;
}