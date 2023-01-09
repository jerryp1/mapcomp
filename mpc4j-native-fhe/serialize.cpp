#include "serialize.h"

jbyteArray serialize_encryption_parms(JNIEnv *env, const EncryptionParameters& parms) {
    std::ostringstream output;
    parms.save(output, Serialization::compr_mode_default);
    uint32_t len = output.str().size();
    jbyteArray result = env->NewByteArray((jsize) len);
    env->SetByteArrayRegion(
            result, 0, (jsize) len, reinterpret_cast<const jbyte *>(output.str().c_str()));
    return result;
}

seal::EncryptionParameters deserialize_encryption_params(JNIEnv *env, jbyteArray parms_bytes) {
    jbyte* parms_byte_data = env->GetByteArrayElements(parms_bytes, JNI_FALSE);
    std::string str((char*)parms_byte_data, env->GetArrayLength(parms_bytes));
    std::istringstream input(str);
    seal::EncryptionParameters params;
    params.load(input);
    // free
    env->ReleaseByteArrayElements(parms_bytes, parms_byte_data, 0);
    return params;
}

jbyteArray serialize_public_key(JNIEnv *env, const PublicKey& public_key) {
    std::ostringstream output;
    public_key.save(output, Serialization::compr_mode_default);
    uint32_t len = output.str().size();
    jbyteArray result = env->NewByteArray((jsize) len);
    env->SetByteArrayRegion(
            result, 0, (jsize) len, reinterpret_cast<const jbyte *>(output.str().c_str()));
    return result;
}

PublicKey deserialize_public_key(JNIEnv *env, jbyteArray pk_bytes, const SEALContext& context) {
    jbyte* pk_byte_data = env->GetByteArrayElements(pk_bytes, JNI_FALSE);
    std::string str((char*)pk_byte_data, env->GetArrayLength(pk_bytes));
    std::istringstream input(str);
    seal::PublicKey public_key;
    public_key.load(context, input);
    // free
    env->ReleaseByteArrayElements(pk_bytes, pk_byte_data, 0);
    return public_key;
}

jbyteArray serialize_secret_key(JNIEnv *env, const SecretKey& secret_key) {
    std::ostringstream output;
    secret_key.save(output, Serialization::compr_mode_default);
    uint32_t len = output.str().size();
    jbyteArray result = env->NewByteArray((jsize) len);
    env->SetByteArrayRegion(
            result, 0, (jsize) len, reinterpret_cast<const jbyte *>(output.str().c_str()));
    return result;
}

SecretKey deserialize_secret_key(JNIEnv *env, jbyteArray sk_bytes, const SEALContext& context) {
    jbyte* pk_byte_data = env->GetByteArrayElements(sk_bytes, JNI_FALSE);
    std::string str((char*)pk_byte_data, env->GetArrayLength(sk_bytes));
    std::istringstream input(str);
    seal::SecretKey secret_key;
    secret_key.load(context, input);
    // free
    env->ReleaseByteArrayElements(sk_bytes, pk_byte_data, 0);
    return secret_key;
}

jbyteArray serialize_relin_keys(JNIEnv *env, const RelinKeys& relin_keys) {
    std::ostringstream output;
    relin_keys.save(output, Serialization::compr_mode_default);
    uint32_t len = output.str().size();
    jbyteArray result = env->NewByteArray((jsize) len);
    env->SetByteArrayRegion(
            result, 0, (jsize) len, reinterpret_cast<const jbyte *>(output.str().c_str()));
    return result;
}

RelinKeys deserialize_relin_keys(JNIEnv *env, jbyteArray relin_key_bytes, const SEALContext& context) {
    jbyte* bytes = env->GetByteArrayElements(relin_key_bytes, JNI_FALSE);
    string str((char*)bytes, env->GetArrayLength(relin_key_bytes));
    istringstream input(str);
    RelinKeys relin_keys;
    relin_keys.load(context, input);
    // free
    env->ReleaseByteArrayElements(relin_key_bytes, bytes, 0);
    return relin_keys;
}

jbyteArray serialize_galois_keys(JNIEnv *env, const GaloisKeys& galois_keys) {
    std::ostringstream output;
    galois_keys.save(output, Serialization::compr_mode_default);
    uint32_t len = output.str().size();
    jbyteArray result = env->NewByteArray((jsize) len);
    env->SetByteArrayRegion(
            result, 0, (jsize) len, reinterpret_cast<const jbyte *>(output.str().c_str()));
    return result;
}

GaloisKeys deserialize_galois_keys(JNIEnv *env, jbyteArray galois_key_bytes, const SEALContext& context) {
    jbyte* bytes = env->GetByteArrayElements(galois_key_bytes, JNI_FALSE);
    string str((char*)bytes, env->GetArrayLength(galois_key_bytes));
    istringstream input(str);
    GaloisKeys galois_keys;
    galois_keys.load(context, input);
    // free
    env->ReleaseByteArrayElements(galois_key_bytes, bytes, 0);
    return galois_keys;
}

jbyteArray serialize_ciphertext(JNIEnv *env, const Ciphertext& ciphertext) {
    std::ostringstream output;
    ciphertext.save(output, Serialization::compr_mode_default);
    uint32_t len = output.str().size();
    jbyteArray result = env->NewByteArray((jsize) len);
    env->SetByteArrayRegion(
            result, 0, (jsize) len, reinterpret_cast<const jbyte *>(output.str().c_str()));
    return result;
}

Ciphertext deserialize_ciphertext(JNIEnv *env, jbyteArray bytes, const SEALContext& context) {
    jbyte* byte_data = env->GetByteArrayElements(bytes, JNI_FALSE);
    std::string str((char*)byte_data, env->GetArrayLength(bytes));
    std::istringstream input(str);
    Ciphertext ciphertext;
    ciphertext.load(context, input);
    // free
    env->ReleaseByteArrayElements(bytes, byte_data, 0);
    return ciphertext;
}

jobject serialize_ciphertexts(JNIEnv *env, const vector<Ciphertext>& ciphertexts) {
    jclass list_jcs = env->FindClass("java/util/ArrayList");
    jmethodID list_init = env->GetMethodID(list_jcs, "<init>", "()V");
    jobject list_obj = env->NewObject(list_jcs, list_init, "");
    jmethodID list_add = env->GetMethodID(list_jcs, "add", "(Ljava/lang/Object;)Z");
    for (auto & ciphertext : ciphertexts) {
        jbyteArray byte_array = serialize_ciphertext(env, ciphertext);
        env->CallBooleanMethod(list_obj, list_add, byte_array);
        env->DeleteLocalRef(byte_array);
    }
    // free
    env->DeleteLocalRef(list_jcs);
    return list_obj;
}

vector<Ciphertext> deserialize_ciphertexts(JNIEnv *env, jobject list, const SEALContext& context) {
    jclass obj_class = env->FindClass("java/util/ArrayList");
    jmethodID get_method = env->GetMethodID(obj_class, "get", "(I)Ljava/lang/Object;");
    jmethodID size_method = env->GetMethodID(obj_class, "size", "()I");
    int size = env->CallIntMethod(list, size_method);
    vector<Ciphertext> result;
    result.reserve(size);
    for (uint32_t i = 0; i < size; i++) {
        auto bytes = (jbyteArray) env->CallObjectMethod(list, get_method, i);
        result.push_back(deserialize_ciphertext(env, bytes, context));
        env->DeleteLocalRef(bytes);
    }
    // free
    env->DeleteLocalRef(obj_class);
    return result;
}

jbyteArray serialize_plaintext(JNIEnv *env, const Plaintext& plaintext) {
    std::ostringstream output;
    plaintext.save(output, Serialization::compr_mode_default);
    uint32_t len = output.str().size();
    jbyteArray result = env->NewByteArray((jsize) len);
    env->SetByteArrayRegion(
            result, 0, (jsize) len, reinterpret_cast<const jbyte *>(output.str().c_str()));
    return result;
}

Plaintext deserialize_plaintext(JNIEnv *env, jbyteArray bytes, const SEALContext& context) {
    jbyte *byte_data = env->GetByteArrayElements(bytes, JNI_FALSE);
    std::string str((char *) byte_data, env->GetArrayLength(bytes));
    std::istringstream input(str);
    seal::Plaintext plaintext;
    plaintext.load(context, input);
    // free
    env->ReleaseByteArrayElements(bytes, byte_data, 0);
    return plaintext;
}

jobject serialize_plaintexts(JNIEnv *env, const vector<Plaintext>& plaintexts) {
    jclass list_jcs = env->FindClass("java/util/ArrayList");
    jmethodID list_init = env->GetMethodID(list_jcs, "<init>", "()V");
    jobject list_obj = env->NewObject(list_jcs, list_init, "");
    jmethodID list_add = env->GetMethodID(list_jcs, "add", "(Ljava/lang/Object;)Z");
    for (auto & plaintext : plaintexts) {
        jbyteArray byte_array = serialize_plaintext(env, plaintext);
        env->CallBooleanMethod(list_obj, list_add, byte_array);
        env->DeleteLocalRef(byte_array);
    }
    // free
    env->DeleteLocalRef(list_jcs);
    return list_obj;
}

vector<Plaintext> deserialize_plaintexts(JNIEnv *env, jobjectArray array, const SEALContext& context) {
    BatchEncoder encoder(context);
    uint32_t size = env->GetArrayLength(array);
    vector<Plaintext> result;
    result.resize(size);
    for (int i = 0; i < size; i++) {
        auto row = (jlongArray) env->GetObjectArrayElement(array, i);
        jlong* ptr = env->GetLongArrayElements(row, JNI_FALSE);
        vector<uint64_t> temp_vec(ptr, ptr + env->GetArrayLength(row));
        encoder.encode(temp_vec, result[i]);
        env->ReleaseLongArrayElements(row, ptr, 0);
    }
    return result;
}

vector<Plaintext> deserialize_plaintexts(JNIEnv *env, jobject list, const SEALContext& context) {
    jclass obj_class = env->FindClass("java/util/ArrayList");
    jmethodID get_method = env->GetMethodID(obj_class, "get", "(I)Ljava/lang/Object;");
    jmethodID size_method = env->GetMethodID(obj_class, "size", "()I");
    int size = env->CallIntMethod(list, size_method);
    vector<Plaintext> result;
    result.reserve(size);
    for (uint32_t i = 0; i < size; i++) {
        auto bytes = (jbyteArray) env->CallObjectMethod(list, get_method, i);
        result.push_back(deserialize_plaintext(env, bytes, context));
        env->DeleteLocalRef(bytes);
    }
    // free
    env->DeleteLocalRef(obj_class);
    return result;
}

Plaintext deserialize_plaintext_from_coeff(JNIEnv *env, jlongArray coeffs, const SEALContext& context) {
    BatchEncoder encoder(context);
    jsize size = env->GetArrayLength(coeffs);
    jlong *ptr = env->GetLongArrayElements(coeffs, JNI_FALSE);
    vector<uint64_t> enc(ptr, ptr + size);
    Plaintext plaintext(context.first_context_data()->parms().poly_modulus_degree());
    encoder.encode(enc, plaintext);
    // free
    env->ReleaseLongArrayElements(coeffs, ptr, 0);
    return plaintext;
}

vector<Plaintext> deserialize_plaintexts_from_coeff(JNIEnv *env, jobjectArray coeffs_list, const SEALContext& context) {
    int size = env->GetArrayLength(coeffs_list);
    vector<Plaintext> plaintexts(size);
    for (int i = 0; i < size; i++) {
        auto coeffs = (jlongArray) env->GetObjectArrayElement(coeffs_list, i);
        plaintexts[i] = deserialize_plaintext_from_coeff(env, coeffs, context);
        env->DeleteLocalRef(coeffs);
    }
    return plaintexts;
}

vector<Plaintext> deserialize_plaintexts_from_coeff_without_batch_encode(JNIEnv *env, jobject coeff_list,
                                                                         const SEALContext& context) {
    jclass obj_class = env->FindClass("java/util/ArrayList");
    jmethodID get_method = env->GetMethodID(obj_class, "get", "(I)Ljava/lang/Object;");
    jmethodID size_method = env->GetMethodID(obj_class, "size", "()I");
    int size = env->CallIntMethod(coeff_list, size_method);
    vector<Plaintext> result;
    result.reserve(size);
    for (uint32_t i = 0; i < size; i++) {
        Plaintext plaintext(context.first_context_data()->parms().poly_modulus_degree());
        auto coeffs = (jlongArray) env->CallObjectMethod(coeff_list, get_method, i);
        uint32_t len = env->GetArrayLength(coeffs);
        jlong *ptr = env->GetLongArrayElements(coeffs, JNI_FALSE);
        vector<uint64_t> vec(ptr, ptr + len);
        for (int j = 0; j < len; j++) {
            plaintext[j] = vec[j];
        }
        result.push_back(plaintext);
        env->DeleteLocalRef(coeffs);
    }
    // free
    env->DeleteLocalRef(obj_class);
    return result;
}
