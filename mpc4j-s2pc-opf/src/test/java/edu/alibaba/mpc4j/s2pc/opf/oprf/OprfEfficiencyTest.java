package edu.alibaba.mpc4j.s2pc.opf.oprf;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory.OprfType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.cm20.Cm20MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.fipr05.Fipr05MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.kkrt16.Kkrt16OptOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.kkrt16.Kkrt16OriOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.rs21.Rs21MpOprfConfig;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * OPRF efficiency test.
 *
 * @author Weiran Liu
 * @date 2023/7/27
 */
@Ignore
@RunWith(Parameterized.class)
public class OprfEfficiencyTest extends AbstractTwoPartyPtoTest {
    /**
     * the large size
     */
    private static final int BATCH_SIZE = 1 << 20;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // RS21
        configurations.add(new Object[]{
            OprfType.RS21.name() + " (" + SecurityModel.MALICIOUS + ")",
            new Rs21MpOprfConfig.Builder(SecurityModel.MALICIOUS).build(),
        });
        configurations.add(new Object[]{
            OprfType.RS21.name() + " (" + SecurityModel.SEMI_HONEST + ")",
            new Rs21MpOprfConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        // CM20
        configurations.add(new Object[]{
            OprfType.CM20.name(), new Cm20MpOprfConfig.Builder().build(),
        });
        // KKRT16_ORI
        configurations.add(new Object[]{
            OprfType.KKRT16_ORI.name(), new Kkrt16OriOprfConfig.Builder().build(),
        });
        // KKRT16_OPT
        configurations.add(new Object[]{
            OprfType.KKRT16_OPT.name(), new Kkrt16OptOprfConfig.Builder().build(),
        });
        // FIPR05
        configurations.add(new Object[]{
            OprfType.FIPR05.name(), new Fipr05MpOprfConfig.Builder().build(),
        });

        return configurations;
    }

    /**
     * the config
     */
    private final OprfConfig config;

    public OprfEfficiencyTest(String name, OprfConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testLargeN() {
        testPto(false);
    }

    @Test
    public void testParallelLargeN() {
        testPto(true);
    }

    private void testPto(boolean parallel) {
        OprfSender sender = OprfFactory.createOprfSender(firstRpc, secondRpc.ownParty(), config);
        OprfReceiver receiver = OprfFactory.createOprfReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        int batchSize = BATCH_SIZE;
        try {
            byte[][] inputs = IntStream.range(0, batchSize)
                .mapToObj(index -> {
                    byte[] input = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                    SECURE_RANDOM.nextBytes(input);
                    return input;
                })
                .toArray(byte[][]::new);
            OprfSenderThread senderThread = new OprfSenderThread(sender, batchSize);
            OprfReceiverThread receiverThread = new OprfReceiverThread(receiver, inputs);
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
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
