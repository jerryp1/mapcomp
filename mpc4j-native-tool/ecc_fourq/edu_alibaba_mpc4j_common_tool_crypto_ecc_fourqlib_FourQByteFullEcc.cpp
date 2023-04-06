/*
 * @Description: 
 * @Author: Qixian Zhou
 * @Date: 2023-04-05 16:12:44
 */


#include "FourQ.h"
#include "FourQ_api.h"
#include "FourQ_internal.h"

#include <cstring>
#include "edu_alibaba_mpc4j_common_tool_crypto_ecc_fourqlib_FourQByteFullEcc.h"


/*
 * Class:     edu_alibaba_mpc4j_common_tool_crypto_ecc_fourqlib_FourQByteFullEcc
 * Method:    nativeMul
 * Signature: ([B[B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_fourqlib_FourQByteFullEcc_nativeMul
  (JNIEnv *env, jobject context, jbyteArray jEcByteArray, jbyteArray jZnByteArray) {

    // 处理 point 
    jbyte* ecBuffer = (*env).GetByteArrayElements(jEcByteArray, nullptr);
    uint8_t p[32];
    memcpy(p, ecBuffer, 32); 
    (*env).ReleaseByteArrayElements(jEcByteArray, ecBuffer, 0);
    // convert p to Point
    point_t A;
    ECCRYPTO_STATUS status = ECCRYPTO_ERROR_UNKNOWN;
    status = decode(p, A);
    if (status != ECCRYPTO_SUCCESS) {
        throw "Decode error, invalid point.";
    }
    
    // 处理 Scalar
    jbyte* znBuffer = (*env).GetByteArrayElements(jZnByteArray, nullptr);
    uint8_t k[32];
    memcpy(k, znBuffer, 32);
    (*env).ReleaseByteArrayElements(jZnByteArray, znBuffer, 0);

    // R = k * A , clear_cofactor is set to false by default.
    point_t R; 
    bool mul_status = ecc_mul(A, (digit_t *)k, R, false); // 核心方法
    if (!mul_status) {
        throw "ecc_mul failed.";
    }
    // encode and return 
    uint8_t res[32]; 
    encode(R, res); // 核心方法
    jbyteArray jMulByteArray = (*env).NewByteArray((jsize)32);
    (*env).SetByteArrayRegion(jMulByteArray, 0, 32, (const jbyte*)res);

    return jMulByteArray;
}


/*
 * Class:     edu_alibaba_mpc4j_common_tool_crypto_ecc_fourqlib_FourQByteFullEcc
 * Method:    nativeBaseMul
 * Signature: ([B[B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_fourqlib_FourQByteFullEcc_nativeBaseMul
  (JNIEnv *env, jobject context, jbyteArray jZnByteArray) {

    // 处理 Scalar
    jbyte* znBuffer = (*env).GetByteArrayElements(jZnByteArray, nullptr);
    uint8_t k[32];
    memcpy(k, znBuffer, 32);
    (*env).ReleaseByteArrayElements(jZnByteArray, znBuffer, 0);

    // R = k * G , G is the generator 
    point_t R; 
    bool mul_status = ecc_mul_fixed((digit_t *)k, R); // 核心方法
    if (!mul_status) {
        throw "ecc_mul failed.";
    }
    // encode and return 
    uint8_t res[32]; 
    encode(R, res); // 核心方法
    jbyteArray jMulByteArray = (*env).NewByteArray((jsize)32);
    (*env).SetByteArrayRegion(jMulByteArray, 0, 32, (const jbyte*)res);

    return jMulByteArray;
}

/*
 * Class:     edu_alibaba_mpc4j_common_tool_crypto_ecc_fourqlib_FourQByteFullEcc
 * Method:    nativeIsValidPoint
 * Signature: ([B[B)[B
 */
JNIEXPORT jboolean JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_fourqlib_FourQByteFullEcc_nativeIsValidPoint
  (JNIEnv *env, jobject context, jbyteArray jEcByteArray) {


     // 处理 point 
    jbyte* ecBuffer = (*env).GetByteArrayElements(jEcByteArray, nullptr);
    uint8_t p[32];
    memcpy(p, ecBuffer, 32); 
    (*env).ReleaseByteArrayElements(jEcByteArray, ecBuffer, 0);
    // convert p to Point
    point_t A;
    ECCRYPTO_STATUS status = ECCRYPTO_ERROR_UNKNOWN;
    status = decode(p, A); // 核心方法

    jboolean res = JNI_FALSE;
    if (status != ECCRYPTO_SUCCESS) {
        return res;
    }else {
        res = JNI_TRUE;
        return res;
    }

}
/*
 * Class:     edu_alibaba_mpc4j_common_tool_crypto_ecc_fourqlib_FourQByteFullEcc
 * Method:    nativeNeg
 * Signature: ([B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_fourqlib_FourQByteFullEcc_nativeNeg
  (JNIEnv *env, jobject context, jbyteArray jEcByteArray) {

    // 处理 point 
    jbyte* ecBuffer = (*env).GetByteArrayElements(jEcByteArray, nullptr);
    uint8_t p[32];
    memcpy(p, ecBuffer, 32); 
    (*env).ReleaseByteArrayElements(jEcByteArray, ecBuffer, 0);
    // convert p to Point
    point_t A;
    ECCRYPTO_STATUS status = ECCRYPTO_ERROR_UNKNOWN;
    status = decode(p, A);
    if (status != ECCRYPTO_SUCCESS) {
        throw "Decode error, invalid point.";
    }
    // 对 A.x 取反 
    fp2neg1271(A->x); // 核心

    // encode and return
    uint8_t res[32]; 
    encode(A, res); // 核心方法
    jbyteArray jMulByteArray = (*env).NewByteArray((jsize)32);
    (*env).SetByteArrayRegion(jMulByteArray, 0, 32, (const jbyte*)res);

    return jMulByteArray;

}

/*
 * Class:     edu_alibaba_mpc4j_common_tool_crypto_ecc_fourqlib_FourQByteFullEcc
 * Method:    nativeAdd
 * Signature: ([B[B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_fourqlib_FourQByteFullEcc_nativeAdd
  (JNIEnv *env, jobject context, jbyteArray jEcByteArray_p, jbyteArray jEcByteArray_q) {


     // 处理 point p
    jbyte* ecBuffer_p = (*env).GetByteArrayElements(jEcByteArray_p, nullptr);
    uint8_t p[32];
    memcpy(p, ecBuffer_p, 32); 
    (*env).ReleaseByteArrayElements(jEcByteArray_p, ecBuffer_p, 0);
    // convert p to Point
    point_t A;
    ECCRYPTO_STATUS status = ECCRYPTO_ERROR_UNKNOWN;
    status = decode(p, A);
    if (status != ECCRYPTO_SUCCESS) {
        throw "Decode error, invalid point.";
    }

    // 处理 point q
    jbyte* ecBuffer_q = (*env).GetByteArrayElements(jEcByteArray_q, nullptr);
    uint8_t q[32];
    memcpy(q, ecBuffer_q, 32); 
    (*env).ReleaseByteArrayElements(jEcByteArray_q, ecBuffer_q, 0);
    // convert p to Point
    point_t B;
    ECCRYPTO_STATUS status_q = ECCRYPTO_ERROR_UNKNOWN;
    status_q = decode(q, B);
    if (status_q != ECCRYPTO_SUCCESS) {
        throw "Decode error, invalid point.";
    }

    // R = A + B
    // void eccadd(point_extproj_precomp_t Q, point_extproj_t P);
    // 需要在 point_t 和 point_extproj_precomp_t 进行一些转换
    
    point_extproj_t AA;
    point_extproj_t BB;
    
    // 先把 point_t 转化为 point_extproj_t
    point_setup(A, AA);
    point_setup(B, BB);
    // 再把 point_extproj_t 转换为为 point_extproj_precomp_t
    // R1_to_R2(point_extproj_t P, point_extproj_precomp_t Q) 
    point_extproj_precomp_t BBQ;
    R1_to_R2(BB, BBQ);
    // eccadd
    eccadd(BBQ, AA); // AA = AA + BB 核心方法

    // convert to point_t
    point_t R;
    eccnorm(AA, R); // void eccnorm(point_extproj *P, point_affine *Q)
    mod1271(R->x[0]); mod1271(R->x[1]);    // Fully reduced P
    mod1271(R->y[0]); mod1271(R->y[1]);

    // encode and return 
    uint8_t res[32]; 
    encode(R, res); // 核心方法
    jbyteArray jMulByteArray = (*env).NewByteArray((jsize)32);
    (*env).SetByteArrayRegion(jMulByteArray, 0, 32, (const jbyte*)res);

    return jMulByteArray;

}



/*
 * Class:     edu_alibaba_mpc4j_common_tool_crypto_ecc_fourqlib_FourQByteFullEcc
 * Method:    nativeHashToCurve
 * Signature: ([B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_fourqlib_FourQByteFullEcc_nativeHashToCurve
  (JNIEnv *env, jobject context, jbyteArray message_hashed) {

    // 处理 message_hashed
    jbyte* ecBuffer = (*env).GetByteArrayElements(message_hashed, nullptr);
    uint8_t m[32];
    memcpy(m, ecBuffer, 32); 
    (*env).ReleaseByteArrayElements(message_hashed, ecBuffer, 0);

    f2elm_t r; // 32-byte
    memcpy(r, m, 32);
    // Reduce r; note that this does not produce a perfectly uniform distribution
    // modulo 2^127-1, but it is good enough.
    mod1271(r[0]);
    mod1271(r[1]);

    point_t Q;
    HashToCurve(r, Q);
    // encode and return
    uint8_t res[32]; 
    encode(Q, res); // 核心方法
    jbyteArray jMulByteArray = (*env).NewByteArray((jsize)32);
    (*env).SetByteArrayRegion(jMulByteArray, 0, 32, (const jbyte*)res);

    return jMulByteArray;

  }

