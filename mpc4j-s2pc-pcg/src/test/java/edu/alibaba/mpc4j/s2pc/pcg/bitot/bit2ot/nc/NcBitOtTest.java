package edu.alibaba.mpc4j.s2pc.pcg.bitot.bit2ot.nc;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.lpn.ldpc.LdpcCreatorUtils;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.bit2ot.BitOtReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.bit2ot.BitOtSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.bit2ot.BitOtTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.bit2ot.nc.direct.DirectNcBitOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.*;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.crr21.Crr21NcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.direct.DirectNcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.ywl20.Ywl20NcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.msp.MspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.msp.bcg19.Bcg19RegMspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.msp.ywl20.Ywl20UniMspCotConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * NC-BitOt协议测试。
 *
 * @author Hanwen Feng
 * @date 2022/08/12
 */
@RunWith(Parameterized.class)
public class NcBitOtTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(NcCotTest.class);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 默认数量
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * 默认轮数
     */
    private static final int DEFAULT_ROUND = 2;
    /**
     * 较大数量
     */
    private static final int LARGE_NUM = 1 << 23;
    /**
     * 较大轮数
     */
    private static final int LARGE_ROUND = 5;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        // DIRECT
        configurations.add(new Object[] {
                NcBitOtFactory.NcBitOtType.DIRECT.name() ,
                new DirectNcBitOtConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });

        return configurations;
    }

    /**
     * 发送方
     */
    private final Rpc senderRpc;
    /**
     * 接收方
     */
    private final Rpc receiverRpc;
    /**
     * 协议类型
     */
    private final NcBitOtConfig config;

    public NcBitOtTest(String name, NcBitOtConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testPtoType() {
        NcBitOtSender sender = NcBitOtFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        NcBitOtReceiver receiver = NcBitOtFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        Assert.assertEquals(config.getPtoType(), sender.getPtoType());
        Assert.assertEquals(config.getPtoType(), receiver.getPtoType());
    }

    @Test
    public void test1Round1Num() {
        NcBitOtSender sender = NcBitOtFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        NcBitOtReceiver receiver = NcBitOtFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 1, 1);
    }

    private void testPto(NcBitOtSender sender, NcBitOtReceiver receiver, int num, int round) {
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            NcBitOtSenderThread senderThread = new NcBitOtSenderThread(sender, num, round);
            NcBitOtReceiverThread receiverThread = new NcBitOtReceiverThread(receiver, num, round);
            StopWatch stopWatch = new StopWatch();
            // 开始执行协议
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            long senderByteLength = senderRpc.getSendByteLength();
            senderRpc.reset();
            long receiverByteLength = receiverRpc.getSendByteLength();
            receiverRpc.reset();
            BitOtSenderOutput[] senderOutputs = senderThread.getSenderOutputs();
            BitOtReceiverOutput[] receiverOutputs = receiverThread.getReceiverOutputs();
            // 验证结果
            IntStream.range(0, round).forEach(index ->
                    BitOtTestUtils.assertOutput(num, senderOutputs[index], receiverOutputs[index])
            );
            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                    senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
