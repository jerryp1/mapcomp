package edu.alibaba.mpc4j.crypto.fhe;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;

/**
 * @author Qixian Zhou
 * @date 2023/10/12
 */
public class FileUtils {

    public static long[] readDataFromFile(String fileName) {
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String dataString = reader.readLine(); // 读取一行数据
            dataString = dataString.replaceAll("[{}\\s]", ""); // 去除括号和空格
            String[] dataValues = dataString.split(",");

            return Arrays.stream(dataValues)
                    .mapToLong(Long::parseLong)
                    .toArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
