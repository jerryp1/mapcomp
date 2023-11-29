package edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator;

import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.PermGenFactory.PermGenTypes;
import edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.PermGenTestUtils.Tuple;
import edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.bitmap.BitmapPermGenConfig;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@RunWith(Parameterized.class)
public class BitmapPermGenTest extends AbstractTwoPartyPtoTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BitmapPermGenTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 18;
    /**
     * bit numbers
     */
    private static final int[] BIT_NUMS = new int[]{4, 8, 16};

    private static final int[] ZLS = new int[]{19, 24};
    /**
     * silent
     */
    private static final boolean silent = true;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        for(int zl : ZLS) {
            Zl tmpZl = ZlFactory.createInstance(EnvType.STANDARD, zl);
            configurations.add(new Object[]{
                PermGenTypes.AHI22_BITMAP.name() + "_" + zl, new BitmapPermGenConfig.Builder(tmpZl, silent).setSilent(false).build()
            });

            // AHI+22 default zl
            configurations.add(new Object[]{
                PermGenTypes.AHI22_BITMAP.name() + "_silent" + "_" + zl, new BitmapPermGenConfig.Builder(tmpZl, silent).setSilent(true).build()
            });
        }

        return configurations;
    }

    /**
     * the config
     */
    private final PermGenConfig config;

    public BitmapPermGenTest(String name, PermGenConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test1Num() {
        for(int bitNum : BIT_NUMS){
            testPto(1, bitNum, false);
        }
    }

    @Test
    public void test2Num() {
        for(int bitNum : BIT_NUMS){
            testPto(2, bitNum, false);
        }
    }

    @Test
    public void test8Num() {
        for(int bitNum : BIT_NUMS){
            testPto(8, bitNum, false);
        }
    }

    @Test
    public void test7Num() {
        for(int bitNum : BIT_NUMS){
            testPto(7, bitNum, false);
        }
    }

    @Test
    public void testDefaultNum() {
        for(int bitNum : BIT_NUMS){
            testPto(DEFAULT_NUM, bitNum, false);
        }
    }

    @Test
    public void testParallelDefaultNum() {
        for(int bitNum : BIT_NUMS){
            testPto(DEFAULT_NUM, bitNum, true);
        }
    }

    @Test
    public void testLargeNum() {
        for(int bitNum : BIT_NUMS){
            testPto(LARGE_NUM, bitNum, false);
        }
    }

    @Test
    public void testParallelLargeNum() {
        for(int bitNum : BIT_NUMS){
            testPto(LARGE_NUM, bitNum, true);
        }
    }

    private void testPto(int num, int bitNum, boolean parallel) {
        MathPreconditions.checkGreaterOrEqual("config.getZl().getL() >= LongUtils.ceilLog2(num)", config.getZl().getL(), LongUtils.ceilLog2(num));
        // create inputs
        BitVector[] origin = IntStream.range(0, bitNum).mapToObj(i -> BitVectorFactory.createZeros(num)).toArray(BitVector[]::new);
        for(int i = 0; i < num; i++){
            int index = SECURE_RANDOM.nextInt(bitNum + 1);
            if(index < bitNum){
                origin[index].set(i, true);
            }
        }
        BitVector[] randoms = IntStream.range(0, bitNum).mapToObj(i -> BitVectorFactory.createRandom(num, SECURE_RANDOM)).toArray(BitVector[]::new);
        SquareZ2Vector[] x0Share = Arrays.stream(randoms).map(x -> SquareZ2Vector.create(x, false)).toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] x1Share = IntStream.range(0, bitNum).mapToObj(i -> SquareZ2Vector.create(origin[i].xor(randoms[i]), false)).toArray(SquareZ2Vector[]::new);

        // init the protocol
        PermGenParty sender = PermGenFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        PermGenParty receiver = PermGenFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            PermGenSenderThread senderThread = new PermGenSenderThread(sender, x0Share, bitNum);
            PermGenReceiverThread receiverThread = new PermGenReceiverThread(receiver, x1Share, bitNum);
            StopWatch stopWatch = new StopWatch();
            // execute the protocol
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            // verify
            LOGGER.info("-----verifying start for bitNum:{}-----", bitNum);
            SquareZlVector shareZ0 = senderThread.getZ0();
            SquareZlVector shareZ1 = receiverThread.getZ1();
            assertOutput(origin, shareZ0, shareZ1);
            printAndResetRpc(time);
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // destroy
        new Thread(sender::destroy).start();
        new Thread(receiver::destroy).start();
    }

    private void assertOutput(BitVector[] origin, SquareZlVector z0, SquareZlVector z1) {
        int num = origin[0].bitNum();
        Assert.assertEquals(num, z0.getNum());
        Assert.assertEquals(num, z1.getNum());

        BigInteger[] elements0 = z0.getZlVector().getElements();
        BigInteger[] elements1 = z1.getZlVector().getElements();
        BigInteger[] resultOrder = IntStream.range(0, num).mapToObj(i -> config.getZl().add(elements0[i], (elements1[i]))).toArray(BigInteger[]::new);
//        LOGGER.info("resultOrder:{}", Arrays.toString(resultOrder));

        // obtain ture order
        ZlDatabase zl = ZlDatabase.create(EnvType.STANDARD, true, origin);
        BigInteger[] tureValue = IntStream.range(0, num).mapToObj(zl::getBigIntegerData).toArray(BigInteger[]::new);
        Tuple[] tuples = IntStream.range(0, num).mapToObj(j -> new Tuple(tureValue[j], BigInteger.valueOf(j))).toArray(Tuple[]::new);
        Arrays.sort(tuples);
        BigInteger[] tureOrder = IntStream.range(0, num).mapToObj(j -> tuples[j].getValue()).toArray(BigInteger[]::new);
        BigInteger[] reverseTureOrder = new BigInteger[num];
        for (int j = 0; j < num; j++) {
            reverseTureOrder[tureOrder[j].intValue()] = BigInteger.valueOf(j);
        }
//        LOGGER.info("origin:{}", Arrays.toString(origin));
//        LOGGER.info("reverseTureOrder:{}", Arrays.toString(reverseTureOrder));
        // verify
        for (int j = 0; j < num; j++) {
            Assert.assertEquals(resultOrder[j], reverseTureOrder[j]);
        }
    }
}
