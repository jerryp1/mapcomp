package edu.alibaba.mpc4j.common.tool.utils;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 参数设置工具类。
 *
 * @author Weiran Liu
 * @date 2022/8/28
 */
public class PropertiesUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesUtils.class);

    private PropertiesUtils() {
        // empty
    }

    /**
     * 读取字符串。
     *
     * @param properties 配置项。
     * @param keyword    关键字。
     * @return 字符串。
     */
    public static String readString(Properties properties, String keyword) {
        return Preconditions.checkNotNull(
            properties.getProperty(keyword), "Please set " + keyword
        );
    }

    /**
     * 读取字符串数组。
     *
     * @param properties 配置项。
     * @param keyword    关键字。
     * @return 字符串数组。
     */
    public static String[] readStringArray(Properties properties, String keyword) {
        String stringArrayString = readString(properties, keyword);
        String[] stringArray = stringArrayString.split(",");
        LOGGER.info("{} = {}", keyword, Arrays.toString(stringArray));
        return stringArray;
    }


    /**
     * 读取布尔值。
     *
     * @param properties 配置项。
     * @param keyword    关键字。
     * @return 布尔值。
     */
    public static boolean readBoolean(Properties properties, String keyword) {
        String booleanString = readString(properties, keyword);
        boolean booleanValue = Boolean.parseBoolean(booleanString);
        LOGGER.info("{} = {}", keyword, booleanValue);
        return booleanValue;
    }

    /**
     * 读取整数。
     *
     * @param properties 配置项。
     * @param keyword    关键字。
     * @return 整数。
     */
    public static int readInt(Properties properties, String keyword) {
        String intString = readString(properties, keyword);
        int intValue = Integer.parseInt(intString);
        Preconditions.checkArgument(
            intValue > 0 && intValue < Integer.MAX_VALUE,
            "Int value must be in range (%s, %s)", 0, Integer.MAX_VALUE
        );
        LOGGER.info("{} = {}", keyword, intValue);
        return intValue;
    }

    /**
     * 读取整数数组。
     *
     * @param properties 配置项。
     * @param keyword    关键字。
     * @return 整数数组。
     */
    public static int[] readIntArray(Properties properties, String keyword) {
        String intArrayString = readString(properties, keyword);
        int[] intArray = Arrays.stream(intArrayString.split(","))
            .mapToInt(Integer::parseInt)
            .toArray();
        LOGGER.info("{} = {}", keyword, Arrays.toString(intArray));
        return intArray;
    }

    /**
     * 读取对数整数数组。
     *
     * @param properties 配置项。
     * @param keyword    关键字。
     * @return 整数数组。
     */
    public static int[] readLogIntArray(Properties properties, String keyword) {
        String intArrayString = readString(properties, keyword);
        int[] logIntArray = Arrays.stream(intArrayString.split(","))
            .mapToInt(Integer::parseInt)
            .peek(logIntValue -> Preconditions.checkArgument(
                logIntValue > 0 && logIntValue < Integer.SIZE,
                "Log int value must be in range (%s, %s)", 0, Integer.SIZE))
            .toArray();
        LOGGER.info("{} = {}", keyword, Arrays.toString(logIntArray));

        return logIntArray;
    }

    /**
     * 读取浮点数数组。
     *
     * @param properties 配置项。
     * @param keyword    关键字。
     * @return 浮点数数组。
     */
    public static double[] readDoubleArray(Properties properties, String keyword) {
        String doubleString = readString(properties, keyword);
        double[] doubleArray = Arrays.stream(doubleString.split(","))
            .mapToDouble(Double::parseDouble)
            .toArray();
        LOGGER.info("{} = {}", keyword, Arrays.toString(doubleArray));
        return doubleArray;
    }
}
