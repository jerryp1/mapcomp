package edu.alibaba.mpc4j.s2pc.pir.keyword;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.IntStream;

/**
 * PIR协议工具类。
 *
 * @author Liqiang Peng
 * @date 2022/8/1
 */
public class PirUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(PirUtils.class);
    /**
     * 私有构造函数
     */
    private PirUtils() {
        // empty
    }

    /**
     * 生成参与方的测试集合。
     *
     * @param serverSetSize     服务端集合大小。
     * @param clientSetSize     客户端集合大小。
     * @param clientSetNumber   客户端集合个数。
     * @param elementByteLength 元素字节长度。
     * @return 各个参与方的集合。
     */
    public static ArrayList<Set<ByteBuffer>> generateBytesSets(int serverSetSize, int clientSetSize,
                                                               int clientSetNumber, int elementByteLength) {
        assert serverSetSize >= 1 : "server must have at least 1 elements";
        assert clientSetSize >= 1 : "client must have at least 1 elements";
        assert clientSetNumber >= 1 : "client must have at least 1 element sets";
        assert elementByteLength >= CommonConstants.STATS_BYTE_LENGTH;
        // 放置各个参与方的集合
        Set<ByteBuffer> serverSet = new HashSet<>(serverSetSize);
        ArrayList<Set<ByteBuffer>> clientSets = new ArrayList<>();
        // 构建服务端集合
        SecureRandom secureRandom = new SecureRandom();
        IntStream.range(0, serverSetSize).forEach(index -> {
            byte[] bytes = new byte[elementByteLength];
            do {
                secureRandom.nextBytes(bytes);
            } while (!serverSet.add(ByteBuffer.wrap(bytes)));
        });
        ArrayList<ByteBuffer> serverArrayList = new ArrayList<>(serverSet);
        // 构建客户端集合
        if (clientSetSize > 1) {
            int matchedItemSize = clientSetSize / 2;
            for (int i = 0; i < clientSetNumber; i++) {
                clientSets.add(new HashSet<>(clientSetSize));
                for (int j = 0; j < matchedItemSize; j++) {
                    int index = secureRandom.nextInt(serverSetSize);
                    clientSets.get(i).add(serverArrayList.get(index));
                }
                for (int j = matchedItemSize; j < clientSetSize; j++) {
                    byte[] bytes = new byte[elementByteLength];
                    secureRandom.nextBytes(bytes);
                    clientSets.get(i).add(ByteBuffer.wrap(bytes));
                }
            }
        } else {
            for (int i = 0; i < clientSetNumber; i++) {
                clientSets.add(new HashSet<>(clientSetSize));
                if (secureRandom.nextBoolean()) {
                    int index = secureRandom.nextInt(serverSetSize);
                    clientSets.get(i).add(serverArrayList.get(index));
                } else {
                    byte[] bytes = new byte[elementByteLength];
                    secureRandom.nextBytes(bytes);
                    clientSets.get(i).add(ByteBuffer.wrap(bytes));
                }
            }
        }
        // 构建返回结果
        ArrayList<Set<ByteBuffer>> byteArraySetArrayList = new ArrayList<>(2);
        byteArraySetArrayList.add(serverSet);
        byteArraySetArrayList.addAll(clientSets);
        return byteArraySetArrayList;
    }

    /**
     * 生成关键词和标签映射。
     *
     * @param keywordSet      关键词集合。
     * @param labelByteLength 标签字节长度。
     * @return 关键词和标签映射。
     */
    public static Map<ByteBuffer, ByteBuffer> generateKwLabelMap(Set<ByteBuffer> keywordSet, int labelByteLength) {
        ArrayList<ByteBuffer> keywordArrayList = new ArrayList<>(keywordSet);
        SecureRandom secureRandom = new SecureRandom();
        Map<ByteBuffer, ByteBuffer> kwLabelMap = new HashMap<>(keywordSet.size());
        IntStream.range(0, keywordArrayList.size()).forEach(i -> {
            byte[] label = new byte[labelByteLength];
            secureRandom.nextBytes(label);
            kwLabelMap.put(keywordArrayList.get(i), ByteBuffer.wrap(label));
        });
        return kwLabelMap;
    }
}
