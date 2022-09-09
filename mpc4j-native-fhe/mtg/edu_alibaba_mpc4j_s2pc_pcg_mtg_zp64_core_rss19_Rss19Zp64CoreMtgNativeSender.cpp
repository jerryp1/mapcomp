//
// Created by pengliqiang on 2022/9/7.
//

#include "edu_alibaba_mpc4j_s2pc_pcg_mtg_zp64_core_rss19_Rss19Zp64CoreMtgNativeSender.h"
#include "seal/seal.h"
#include "../serialize.h"
#include "../utils.h"

using namespace std;
using namespace seal;

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pcg_mtg_zp64_core_rss19_Rss19Zp64CoreMtgNativeSender_keyGen(
        JNIEnv *env, jclass, jint poly_modulus_degree, jlong plain_modulus) {
    // 生成方案参数和密钥
    EncryptionParameters parms(scheme_type::bfv);
    parms.set_poly_modulus_degree(poly_modulus_degree);
    parms.set_coeff_modulus(CoeffModulus::BFVDefault(poly_modulus_degree, sec_level_type::tc128));
    parms.set_plain_modulus(plain_modulus);
    SEALContext context = SEALContext(parms);
    KeyGenerator key_gen = KeyGenerator(context);
    const SecretKey &secret_key = key_gen.secret_key();
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
    jbyteArray sk_byte = serialize_secret_key(env, secret_key);
    // 添加到list
    env->CallBooleanMethod(list_obj, list_add, params_bytes);
    env->CallBooleanMethod(list_obj, list_add, pk_byte);
    env->CallBooleanMethod(list_obj, list_add, sk_byte);
    return list_obj;
}

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pcg_mtg_zp64_core_rss19_Rss19Zp64CoreMtgNativeSender_encryption(
        JNIEnv *env, jclass, jbyteArray params_bytes, jbyteArray public_key_bytes, jbyteArray secret_key_bytes,
        jlongArray message0, jlongArray message1) {
    EncryptionParameters params = deserialize_encryption_params(env, params_bytes);
    SEALContext context(params);
    PublicKey public_key = deserialize_public_key(env, public_key_bytes, context);
    SecretKey secret_key = deserialize_secret_key(env, secret_key_bytes, context);
    Encryptor encryptor(context, public_key);
    encryptor.set_secret_key(secret_key);
    Evaluator evaluator(context);
    BatchEncoder encoder(context);
    int size = env->GetArrayLength(message0);
    long *ptr0 = env->GetLongArrayElements(message0, JNI_FALSE);
    long *ptr1 = env->GetLongArrayElements(message1, JNI_FALSE);
    vector<uint64_t> vec0(ptr0, ptr0 + size), vec1(ptr1, ptr1 + size);
    Plaintext plaintext0, plaintext1;
    encoder.encode(vec0, plaintext0);
    encoder.encode(vec1, plaintext1);
    vector<Ciphertext> ct(2);
    encryptor.encrypt_symmetric(plaintext0, ct[0]);
    encryptor.encrypt_symmetric(plaintext1, ct[1]);
    auto parms_id = get_parms_id_for_chain_idx(context, 1);
    for (auto & i : ct) {
        // Only one ciphertext-plaintext multiplication is needed after this
        evaluator.mod_switch_to_inplace(i, parms_id);
        // All ciphertexts must be in NTT form
        evaluator.transform_to_ntt_inplace(i);
    }
    return serialize_ciphertexts(env, ct);
}

JNIEXPORT jlongArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pcg_mtg_zp64_core_rss19_Rss19Zp64CoreMtgNativeSender_decryption(
        JNIEnv *env, jclass, jbyteArray params_bytes, jbyteArray secret_key_bytes, jbyteArray ciphertext_bytes) {
    EncryptionParameters params = deserialize_encryption_params(env, params_bytes);
    SEALContext context(params);
    SecretKey secret_key = deserialize_secret_key(env, secret_key_bytes, context);
    Ciphertext ciphertext = deserialize_ciphertext(env, ciphertext_bytes, context);
    Decryptor decryptor(context, secret_key);
    Plaintext plaintext;
    decryptor.decrypt(ciphertext, plaintext);
    BatchEncoder encoder(context);
    vector<uint64_t> coeffs;
    coeffs.resize(params.poly_modulus_degree());
    encoder.decode(plaintext, coeffs);
    jlongArray result;
    result = env->NewLongArray((jsize) params.poly_modulus_degree());
    jlong temp[params.poly_modulus_degree()];
    for (int i = 0; i < params.poly_modulus_degree(); i++) {
        temp[i] = (jlong) coeffs[i];
    }
    env->SetLongArrayRegion(result, 0, (jsize) params.poly_modulus_degree(), temp);
    return result;
}