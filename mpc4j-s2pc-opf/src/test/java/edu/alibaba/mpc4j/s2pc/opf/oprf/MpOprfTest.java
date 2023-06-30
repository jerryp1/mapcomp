package edu.alibaba.mpc4j.s2pc.opf.oprf;

import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.opf.oprf.cm20.Cm20MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.fipr05.Fipr05MpOprfConfig;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * multi-query OPRF test.
 *
 * @author Weiran Liu
 * @date 2022/4/9
 */
@RunWith(Parameterized.class)
public class MpOprfTest extends AbstractTwoPartyPtoTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OprfTest.class);
    /**
     * default batch size
     */
    private static final int DEFAULT_BATCH_SIZE = 1000;
    /**
     * large batch size
     */
    private static final int LARGE_BATCH_SIZE = 1 << 12;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // CM20
        configurations.add(new Object[]{
            OprfFactory.OprfType.CM20.name(), new Cm20MpOprfConfig.Builder().build(),
        });
        // FIPR05
        configurations.add(new Object[]{
            OprfFactory.OprfType.FIPR05.name(), new Fipr05MpOprfConfig.Builder().build(),
        });

        return configurations;
    }

    /**
     * the config
     */
    private final MpOprfConfig config;

    public MpOprfTest(String name, MpOprfConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test1N() {
        testPto(1, false);
    }

    @Test
    public void test2N() {
        testPto(2, false);
    }

    @Test
    public void test3N() {
        testPto(3, false);
    }

    @Test
    public void test8N() {
        testPto(8, false);
    }

    @Test
    public void testDefault() {
        testPto(DEFAULT_BATCH_SIZE, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_BATCH_SIZE, true);
    }

    @Test
    public void testLargeN() {
        testPto(LARGE_BATCH_SIZE, false);
    }

    @Test
    public void testParallelLargeN() {
        testPto(LARGE_BATCH_SIZE, true);
    }

    private void testPto(int batchSize, boolean parallel) {
        MpOprfSender sender = OprfFactory.createMpOprfSender(firstRpc, secondRpc.ownParty(), config);
        MpOprfReceiver receiver = OprfFactory.createMpOprfReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {}, batch_size = {}-----", sender.getPtoDesc().getPtoName(), batchSize);
            byte[][] inputs = IntStream.range(0, batchSize)
                .mapToObj(index -> {
                    byte[] input = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                    SECURE_RANDOM.nextBytes(input);
                    return input;
                })
                .toArray(byte[][]::new);
            MpOprfSenderThread senderThread = new MpOprfSenderThread(sender, batchSize);
            MpOprfReceiverThread receiverThread = new MpOprfReceiverThread(receiver, inputs);
            StopWatch stopWatch = new StopWatch();
            // start
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            // stop
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            // verify
            MpOprfSenderOutput senderOutput = senderThread.getSenderOutput();
            MpOprfReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(batchSize, senderOutput, receiverOutput);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(int n, MpOprfSenderOutput senderOutput, MpOprfReceiverOutput receiverOutput) {
        Assert.assertEquals(senderOutput.getPrfByteLength(), receiverOutput.getPrfByteLength());
        Assert.assertEquals(n, senderOutput.getBatchSize());
        Assert.assertEquals(n, receiverOutput.getBatchSize());
        IntStream.range(0, n).forEach(index -> {
            byte[] input = receiverOutput.getInput(index);
            byte[] receiverPrf = receiverOutput.getPrf(index);
            byte[] senderPrf = senderOutput.getPrf(input);
            Assert.assertArrayEquals(senderPrf, receiverPrf);
        });
        // all PRFs should be distinct
        long distinctCount = IntStream.range(0, n)
            .mapToObj(receiverOutput::getPrf)
            .map(ByteBuffer::wrap)
            .distinct()
            .count();
        Assert.assertEquals(receiverOutput.getBatchSize(), distinctCount);
    }
}
