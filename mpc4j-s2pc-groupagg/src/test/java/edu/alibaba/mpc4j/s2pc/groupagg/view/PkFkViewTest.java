package edu.alibaba.mpc4j.s2pc.groupagg.view;

import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkViewConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkViewFactory;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkViewFactory.ViewPtoType;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkViewReceiver;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkViewSender;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.php24.Php24PkFkViewConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.php24.Php24PmapConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.pidbased.PidBasedPmapConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.psibased.PsiBasedPmapConfig;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * @author Feng Han
 * @date 2024/7/22
 */
@RunWith(Parameterized.class)
public class PkFkViewTest extends AbstractTwoPartyPtoTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PkFkViewTest.class);
    /**
     * bitLen
     */
    private static final int[] bitLens = new int[]{16, 64, 256};
    /**
     * default small size
     */
    private static final int DEFAULT_SMALL_SIZE = 99;
    /**
     * large size
     */
    private static final int LARGE_SIZE = 1 << 14;
    /**
     * duplicate rate
     */
    private static final double DUPLICATE_RATE = 0.1;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

//        // baseline
//        configurations.add(new Object[]{
//            ViewPtoType.BASELINE.name(),
//            new BaselinePkFkViewConfig.Builder(false).build(),
//        });

        configurations.add(new Object[]{
            ViewPtoType.PHP24.name() + " Php24Pmap",
            new Php24PkFkViewConfig.Builder(false).setPmapConfig(new Php24PmapConfig.Builder(false).build()).build(),
        });

        configurations.add(new Object[]{
            ViewPtoType.PHP24.name() + " pidBasedPmap",
            new Php24PkFkViewConfig.Builder(false).setPmapConfig(new PidBasedPmapConfig.Builder(false).build()).build(),
        });

        configurations.add(new Object[]{
            ViewPtoType.PHP24.name() + " psiBasedPmap",
            new Php24PkFkViewConfig.Builder(false).setPmapConfig(new PsiBasedPmapConfig.Builder(false).build()).build(),
        });
        return configurations;
    }

    private final PkFkViewConfig config;

    public PkFkViewTest(String name, PkFkViewConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test2() {
        for (int bitLen : bitLens) {
            testPto(2, 2, bitLen, false);
        }
    }

    @Test
    public void test10() {
        for (int bitLen : bitLens) {
            testPto(10, 10, bitLen, false);
        }
    }

    @Test
    public void testDefault() {
        for (int bitLen : bitLens) {
            testPto(DEFAULT_SMALL_SIZE, DEFAULT_SMALL_SIZE, bitLen, false);
        }
    }

    @Test
    public void testParallelDefault() {
        for (int bitLen : bitLens) {
            testPto(DEFAULT_SMALL_SIZE, DEFAULT_SMALL_SIZE, bitLen, true);
        }
    }

    @Test
    public void testUnbalancedSize() {
        for (int bitLen : bitLens) {
            testPto(LARGE_SIZE, DEFAULT_SMALL_SIZE, bitLen, false);
        }
    }

    @Test
    public void testParallelLarge() {
        for (int bitLen : bitLens) {
            testPto(LARGE_SIZE, LARGE_SIZE, bitLen, true);
        }
    }

    private void testPto(int senderSize, int receiverSize, int payloadBitLen, boolean parallel) {
        PkFkViewSender sender = PkFkViewFactory.createPkFkViewSender(firstRpc, secondRpc.ownParty(), config);
        PkFkViewReceiver receiver = PkFkViewFactory.createPkFkViewReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {}，senderSize = {}，receiverSize = {}, payload bit length = {} -----",
                sender.getPtoDesc().getPtoName(), senderSize, receiverSize, payloadBitLen
            );
            // generate the inputs
            ArrayList<Set<ByteBuffer>> sets = PsoUtils.generateBytesSets(senderSize, receiverSize, CommonConstants.BLOCK_BYTE_LENGTH);
            byte[][] senderElements = sets.get(0).stream().map(ByteBuffer::array).toArray(byte[][]::new);
            byte[][] receiverElements = duplicateKeys(sets.get(1), DUPLICATE_RATE);
            BitVector[] senderPayload1 = genPayload(senderSize, payloadBitLen);
            BitVector[] senderPayload2 = genPayload(senderSize, payloadBitLen);
            BitVector[] receiverPayload1 = genPayload(receiverSize, payloadBitLen);
            BitVector[] receiverPayload2 = genPayload(receiverSize, payloadBitLen);

            PkFkViewSenderThread senderThread = new PkFkViewSenderThread(
                sender, senderElements, senderPayload1, senderPayload2, receiverSize);
            PkFkViewReceiverThread receiverThread = new PkFkViewReceiverThread(
                receiver, receiverElements, receiverPayload1, receiverPayload2, senderSize, payloadBitLen);
            StopWatch stopWatch = new StopWatch();
            LOGGER.info("----- pto starts -----");
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
            // verify todos
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private byte[][] duplicateKeys(Set<ByteBuffer> keys, double rate) {
        assert rate <= 1.0;
        List<ByteBuffer> list = new ArrayList<>(keys);
        byte[][] res = new byte[list.size()][];
        int index = 0;
        int maxIndex = Math.max(1, (int) (rate * list.size()));
        for (ByteBuffer byteBuffer : list) {
            if (index < maxIndex) {
                res[index] = byteBuffer.array();
            } else {
                int target = SECURE_RANDOM.nextInt(maxIndex);
                res[index] = Arrays.copyOf(res[target], res[target].length);
            }
            index++;
        }
        return res;
    }

    private BitVector[] genPayload(int dataSize, int bitLen) {
        return IntStream.range(0, dataSize)
            .mapToObj(i -> BitVectorFactory.createRandom(bitLen, SECURE_RANDOM))
            .toArray(BitVector[]::new);
    }

}
