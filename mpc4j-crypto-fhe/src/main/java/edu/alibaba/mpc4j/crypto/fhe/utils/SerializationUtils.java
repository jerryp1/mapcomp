package edu.alibaba.mpc4j.crypto.fhe.utils;

import java.io.*;

/**
 * @author Qixian Zhou
 * @date 2023/10/13
 */
public class SerializationUtils {
    public static byte[] serializeObject(Serializable object) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);

            objectOutputStream.writeObject(object);
            objectOutputStream.flush();
            objectOutputStream.close();

            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static <T> T deserializeObject(byte[] serializedBytes) {
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(serializedBytes);
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);

            return (T) objectInputStream.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
