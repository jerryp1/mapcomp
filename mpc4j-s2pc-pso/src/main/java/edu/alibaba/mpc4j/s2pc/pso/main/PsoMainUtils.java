package edu.alibaba.mpc4j.s2pc.pso.main;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.impl.netty.NettyParty;
import edu.alibaba.mpc4j.common.rpc.impl.netty.NettyRpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * PSO主函数工具类。
 *
 * @author Weiran Liu
 * @date 2022/5/15
 */
public class PsoMainUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsoMain.class);

    private PsoMainUtils() {
        // empty
    }

    /**
     * 设置通信接口。
     *
     * @param properties 配置项。
     * @return 通信接口。
     */
    public static Rpc setRpc(Properties properties) {
        // 构建参与方信息
        Set<NettyParty> nettyPartySet = new HashSet<>(2);
        Map<String, NettyParty> nettyPartyMap = new HashMap<>(2);
        // 初始化服务端
        String serverName = Preconditions.checkNotNull(
            properties.getProperty("server_name"), "Please set server_name"
        );
        String serverIp = Preconditions.checkNotNull(
            properties.getProperty("server_ip"), "Please set server_ip"
        );
        int serverPort = Integer.parseInt(Preconditions.checkNotNull(
            properties.getProperty("server_port", "Please set server_port")
        ));
        NettyParty serverNettyParty = new NettyParty(0, serverName, serverIp, serverPort);
        nettyPartySet.add(serverNettyParty);
        nettyPartyMap.put(serverName, serverNettyParty);
        // 初始化客户端
        String clientName = Preconditions.checkNotNull(
            properties.getProperty("client_name"), "Please set client_name"
        );
        String clientIp = Preconditions.checkNotNull(
            properties.getProperty("client_ip"), "Please set client_ip"
        );
        int clientPort = Integer.parseInt(Preconditions.checkNotNull(
            properties.getProperty("client_port"), "Please set client_port"
        ));
        NettyParty clientNettyParty = new NettyParty(1, clientName, clientIp, clientPort);
        nettyPartySet.add(clientNettyParty);
        nettyPartyMap.put(clientName, clientNettyParty);
        // 获得自己的参与方信息
        String ownName = Preconditions.checkNotNull(
            properties.getProperty("own_name"), "Please set own_name"
        );
        NettyParty ownParty = Preconditions.checkNotNull(
            nettyPartyMap.get(ownName), "own_name must be %s or %s", serverName, clientName
        );
        if (ownName.equals(serverName)) {
            LOGGER.info("own_name = {} for party_id = 0", serverName);
        } else {
            LOGGER.info("own_name = {} for party_id = 1", clientName);
        }
        return new NettyRpc(ownParty, nettyPartySet);
    }

    /**
     * 读取字符串。
     *
     * @param properties 配置项。
     * @param keyword 关键字。
     * @return 字符串。
     */
    public static String readString(Properties properties, String keyword) {
        return Preconditions.checkNotNull(
            properties.getProperty(keyword), "Please set " + keyword
        );
    }

    /**
     * 读取整数。
     *
     * @param properties 配置项。
     * @param keyword 关键字。
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
     * 读取对数整数数组。
     *
     * @param properties 配置项。
     * @param keyword 关键字。
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
     * 读取整数数组。
     *
     * @param properties 配置项。
     * @param keyword 关键字。
     * @return 整数数组。
     */
    public static int[] readIntArray(Properties properties, String keyword) {
        String intArrayString = readString(properties, keyword);
        int[] intArray = Arrays.stream(intArrayString.split(","))
            .mapToInt(Integer::parseInt)
            .peek(intValue -> Preconditions.checkArgument(
                intValue > 0 && intValue < Integer.MAX_VALUE,
                "Int value must be in range (%s, %s)", 0, Integer.MAX_VALUE))
            .toArray();
        LOGGER.info("{} = {}", keyword, Arrays.toString(intArray));
        return intArray;
    }
}
