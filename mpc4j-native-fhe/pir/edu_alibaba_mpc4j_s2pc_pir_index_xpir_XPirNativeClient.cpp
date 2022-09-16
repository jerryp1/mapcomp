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
    // 生成密钥
    EncryptionParameters parms = deserialize_encryption_params(env, params_byte);
    SEALContext context(parms);
    KeyGenerator key_gen(context);
    const SecretKey& secret_key = key_gen.secret_key();
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
    jbyteArray pk_byte = serialize_public_key(env, public_key);
    jbyteArray sk_byte = serialize_secret_key(env, secret_key);
    // 添加到list
    env->CallBooleanMethod(list_obj, list_add, pk_byte);
    env->CallBooleanMethod(list_obj, list_add, sk_byte);
    return list_obj;
}

[[maybe_unused]] JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_xpir_XPirNativeClient_generateQuery(
        JNIEnv *env, jclass, jbyteArray params_byte, jbyteArray pk_byte, jbyteArray sk_byte, jintArray message_list) {
    EncryptionParameters params = deserialize_encryption_params(env, params_byte);
    SEALContext context(params);
    PublicKey pk = deserialize_public_key(env, pk_byte, context);
    SecretKey sk = deserialize_secret_key(env, sk_byte, context);
    Encryptor encryptor(context, pk, sk);
    auto pool = MemoryManager::GetPool();
    int size = env->GetArrayLength(message_list);
    jint *ptr = env->GetIntArrayElements(message_list, JNI_FALSE);
    vector<uint32_t> vec(ptr, ptr + size);
    vector<Ciphertext> ciphertexts;
    ciphertexts.resize(size);
    for (uint32_t i = 0; i < size; i++) {
        Plaintext plaintext(params.poly_modulus_degree());
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
    SecretKey sk = deserialize_secret_key(env, sk_byte, context);
    Decryptor decryptor(context, sk);
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
            assert(temp.size() == 1);
            return get_plaintext_coeffs(env, tempplain[0]);
        } else {
            tempplain.clear();
            temp = newtemp;
        }
    }
    // This should never be called
    assert(0);
    return nullptr;
}