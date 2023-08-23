package edu.alibaba.mpc4j.s2pc.sbitmap.utils;

import java.io.*;
import java.util.Base64;

/**
 * Converter Utilities.
 *
 * @author Li Peng
 * @date 2023/8/23
 */
public class Converter {
    /**
     * Serialize object to base64 string.
     *
     * @param obj object to be serialized.
     * @return base64 string.
     */
    public static String serializeToBase64(Object obj) {
        return encodeBase64(serialize(obj));
    }

    /**
     * Deserialized object from base64 string.
     *
     * @param base64Str base64 string.
     * @return deserialized object.
     */
    public static Object deserializeFromBase64(String base64Str) {
        return deserialize(decodeBase64(base64Str));
    }

    /**
     * Serialize object to serialized bytes.
     *
     * @param obj object to be serialized.
     * @return serialized bytes.
     */
    private static byte[] serialize(Object obj) {
        try (ByteArrayOutputStream b = new ByteArrayOutputStream()) {
            try (ObjectOutputStream o = new ObjectOutputStream(b)) {
                o.writeObject(obj);
            }
            return b.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error occurs when serialize");
            return null;
        }
    }

    /**
     * Deserialize object from serialized bytes.
     *
     * @param bytes serialized bytes of object.
     * @return object.
     */
    private static Object deserialize(byte[] bytes) {
        try (ByteArrayInputStream b = new ByteArrayInputStream(bytes)) {
            try (ObjectInputStream o = new ObjectInputStream(b)) {
                return o.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("Error occurs when deserialize");
            return null;
        }
    }

    /**
     * Decode bytes from base64 string
     *
     * @param base64String base64 string
     * @return decoded bytes.
     */
    private static byte[] decodeBase64(String base64String) {
        return Base64.getDecoder().decode(base64String);
    }

    /**
     * Encode bytes to base64 string.
     *
     * @param bytes bytes.
     * @return base64 string.
     */
    private static String encodeBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Encode binary string.
     *
     * @param bytes bytes.
     * @return binary string.
     */
    protected static String encodeBinaryString(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
            builder.append("#");
        }
        return builder.toString();
    }
}
