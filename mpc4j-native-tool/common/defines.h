//
// Created by Weiran Liu on 2021/12/11.
//
#include <cstdint>
#include <jni.h>
#include <vector>

#ifndef MPC4J_NATIVE_TOOL_DEFINES_H
#define MPC4J_NATIVE_TOOL_DEFINES_H

#define BLOCK_BYTE_LENGTH 16

/**
 * 调换字节数组的顺序。
 *
 * @param data 字节数组。
 * @param size 字节数组的长度。
 */
inline void reverseBytes(uint8_t* data, uint64_t size) {
    uint8_t *low = data;
    uint8_t *high = data + size - 1;
    uint8_t swap;
    while (low < high) {
        swap = *low;
        *low++ = *high;
        *high-- = swap;
    }
}

void initGF2E(JNIEnv *env, jbyteArray jMinBytes);

/**
 * 将jobjectArray表示的二维字节数组数据解析为set<byte[]>。
 *
 * @param env JNI环境。
 * @param jBytesArray 用jobjectArray表示的二维字节数组。
 * @param byteLength 二维字节数组每个维度的长度。
 * @param set 转换结果存储地址。
 */
void jByteArrayToSet(JNIEnv *env, jobjectArray jBytesArray, uint64_t byteLength, std::vector<uint8_t*> &set);

/**
 * 将用set<byte[]>表示的数据转换为jobjectArray表示的二维字节数组。
 *
 * @param env JNI环境。
 * @param set set<byte[]>表示的数据。
 * @param byteLength 二维字节数组每个维度的长度。
 * @param jnum 转换后的byte[][]数组长度。
 * @param jArray 转换结果存储地址。
 */
void setTojByteArray(JNIEnv *env, std::vector<uint8_t*> &set, uint64_t byteLength, jint jnum, jobjectArray &jArray);

#endif //MPC4J_NATIVE_TOOL_DEFINES_H
