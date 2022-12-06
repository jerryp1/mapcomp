package edu.alibaba.mpc4j.s2pc.pjc.bitmap.main;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcPropertiesUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.BitmapReceiver;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.BitmapSender;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.SecureBitmapConfig;
import org.apache.log4j.PropertyConfigurator;
import org.roaringbitmap.RoaringBitmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Bitmap主函数。
 *
 * @author Li Peng  
 * @date 2022/12/1
 */
public class BitmapMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(BitmapMain.class);

    public static void main(String[] args) throws Exception {
        // 读取日志配置文件
        LOGGER.info("read log config");
        Properties log4jProperties = new Properties();
        log4jProperties.load(BitmapMain.class.getResourceAsStream("/log4j.properties"));
        PropertyConfigurator.configure(log4jProperties);
        // 读取配置文件
        LOGGER.info("read config file");
        Properties properties = PropertiesUtils.loadProperties(args[0]);
        run(properties);
        System.exit(0);
    }

    public static void run(Properties properties) throws MpcAbortException {
        // 设置通信接口
        Rpc ownRpc = RpcPropertiesUtils.readNettyRpc(properties, "sender", "receiver");
        Party otherParty = ownRpc.ownParty().getPartyId() == 0 ? ownRpc.getParty(1) : ownRpc.getParty(0);

        // 设置总测试轮数
        int totalRound = PropertiesUtils.readInt(properties, "total_round");
        Preconditions.checkArgument(totalRound >= 1, "round must be greater than or equal to 1");
        // 设置最大数量
        int maxNum = PropertiesUtils.readInt(properties, "max_num");
        Preconditions.checkArgument(totalRound > 1, "max_num must be greater than 1");

        SecureBitmapConfig bitmapConfig = new SecureBitmapConfig.Builder().build();

        RoaringBitmap roaringBitmap = new RoaringBitmap();
        ownRpc.connect();
        if (ownRpc.ownParty().getPartyId() == 0) {
            BitmapSender sender = new BitmapSender(ownRpc, otherParty, bitmapConfig);
            BitmapSenderRunner senderRunner = new BitmapSenderRunner(totalRound, maxNum, sender, bitmapConfig, roaringBitmap, false, null, false);
            senderRunner.run();
        } else {
            BitmapReceiver receiver = new BitmapReceiver(ownRpc, otherParty, bitmapConfig);
            BitmapReceiverRunner senderRunner = new BitmapReceiverRunner(totalRound, maxNum, receiver, bitmapConfig, null, false, roaringBitmap, false);
            senderRunner.run();
        }
        ownRpc.disconnect();
    }
}
