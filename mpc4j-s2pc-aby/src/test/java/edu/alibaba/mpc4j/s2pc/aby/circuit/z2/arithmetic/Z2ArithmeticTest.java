package edu.alibaba.mpc4j.s2pc.aby.circuit.z2.arithmetic;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.bea91.Bea91BcConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.common.tool.EnvType.STANDARD_JDK;

/**
 * Z2 arithmetic test.
 * https://www.doc.ic.ac.uk/~eedwards/compsys/arithmetic/index.html
 *
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2023/4/13
 */
@RunWith(Parameterized.class)
public class Z2ArithmeticTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(edu.alibaba.mpc4j.s2pc.aby.circuit.mux.zl.ZlMuxTest.class);
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 12;
    /**
     * small Zl
     */
    private static final Zl SMALL_ZL = ZlFactory.createInstance(EnvType.STANDARD, 1);
    /**
     * default Zl
     */
    private static final Zl DEFAULT_ZL = ZlFactory.createInstance(EnvType.STANDARD, Integer.SIZE);

    private static final int DEFAULT_ARITH_LENGTH = 32;

    private static final int LARGE_ARITH_LENGTH = 128;

    private static final EnvType envType = STANDARD_JDK;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        // Beaver91
        configurations.add(new Object[]{
                BcFactory.BcType.Bea91.name(), new Bea91BcConfig.Builder().build()
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
    private final BcConfig config;

    public Z2ArithmeticTest(String name, BcConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        // We cannot use NettyRPC in the test case since it needs multi-thread connect / disconnect.
        // In other word, we cannot connect / disconnect NettyRpc in @Before / @After, respectively.
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Before
    public void connect() {
        senderRpc.connect();
        receiverRpc.connect();
    }

    @After
    public void disconnect() {
        senderRpc.disconnect();
        receiverRpc.disconnect();
    }

    @Test
    public void test1Num() {
        testAllOperators(DEFAULT_ARITH_LENGTH, 1, false);
    }

    @Test
    public void test2Num() {
        testAllOperators(DEFAULT_ARITH_LENGTH, 2, false);
    }

    @Test
    public void test8Num() {
        testAllOperators(DEFAULT_ARITH_LENGTH, 8, false);
    }

    @Test
    public void testDefaultNum() {
        testAllOperators(DEFAULT_ARITH_LENGTH, DEFAULT_NUM, false);
    }

    @Test
    public void testParallelDefaultNum() {
        testAllOperators(DEFAULT_ARITH_LENGTH, DEFAULT_NUM, true);
    }

    @Test
    public void testSmallZl() {
        testAllOperators(DEFAULT_ARITH_LENGTH, DEFAULT_NUM, false);
    }

    @Test
    public void testLargeNum() {
        testAllOperators(DEFAULT_ARITH_LENGTH, LARGE_NUM, false);
    }

    @Test
    public void testParallelLargeNum() {
        testAllOperators(DEFAULT_ARITH_LENGTH, LARGE_NUM, false);
    }

    public void testAllOperators(int arithLength, int num, boolean parallel) {
        // all the operator to be tested
        testPto(ArithmeticOperator.LEQ, arithLength, num, parallel);
//        testPto(ArithmeticOperator.BIT_ADD, arithLength, num, parallel);
//        testPto(ArithmeticOperator.ADD, arithLength, num, parallel);
//        testPto(ArithmeticOperator.SUB, arithLength, num, parallel);
//        testPto(ArithmeticOperator.NOT, arithLength, num, parallel);
    }

    private void testPto(ArithmeticOperator operator, int arithLength, int num, boolean parallel) {
        int arithByteLength = CommonUtils.getByteLength(arithLength);
        // create inputs in [0, 2^(arithLength-1)) to avoid overflow in subtraction.
        byte[][] xBytes = IntStream.range(0, num)
                .mapToObj(i -> new BigInteger(arithLength - 1, SECURE_RANDOM))
                .map(v -> BigIntegerUtils.nonNegBigIntegerToByteArray(v, arithByteLength)).toArray(byte[][]::new);
        byte[][] yBytes = IntStream.range(0, num)
                .mapToObj(i -> new BigInteger(arithLength - 1, SECURE_RANDOM))
                .map(v -> BigIntegerUtils.nonNegBigIntegerToByteArray(v, arithByteLength)).toArray(byte[][]::new);

        ZlDatabase zlDatabaseX = ZlDatabase.create(arithLength, xBytes);
        ZlDatabase zlDatabaseY = ZlDatabase.create(arithLength, yBytes);

        BitVector[] xBitVector = zlDatabaseX.bitPartition(envType, false);
        BitVector[] yBitVector = zlDatabaseY.bitPartition(envType, false);

        BitVector[] x0 = IntStream.range(0, arithLength).mapToObj(i -> BitVectorFactory.createRandom(num, SECURE_RANDOM)).toArray(BitVector[]::new);
        BitVector[] x1 = IntStream.range(0, arithLength).mapToObj(i -> xBitVector[i].xor(x0[i])).toArray(BitVector[]::new);
        SquareShareZ2Vector[] shareX0 = Arrays.stream(x0).map(x -> SquareShareZ2Vector.create(x, false)).toArray(SquareShareZ2Vector[]::new);
        SquareShareZ2Vector[] shareX1 = Arrays.stream(x1).map(x -> SquareShareZ2Vector.create(x, false)).toArray(SquareShareZ2Vector[]::new);

        BitVector[] y0 = IntStream.range(0, arithLength).mapToObj(i -> BitVectorFactory.createRandom(num, SECURE_RANDOM)).toArray(BitVector[]::new);
        BitVector[] y1 = IntStream.range(0, arithLength).mapToObj(i -> yBitVector[i].xor(y0[i])).toArray(BitVector[]::new);
        SquareShareZ2Vector[] shareY0 = Arrays.stream(y0).map(y -> SquareShareZ2Vector.create(y, false)).toArray(SquareShareZ2Vector[]::new);
        SquareShareZ2Vector[] shareY1 = Arrays.stream(y1).map(y -> SquareShareZ2Vector.create(y, false)).toArray(SquareShareZ2Vector[]::new);
        // init the protocol
        BcParty sender = BcFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BcParty receiver = BcFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            Z2ArithmeticSenderThread senderThread = new Z2ArithmeticSenderThread(sender, operator, shareX0, shareY0);
            Z2ArithmeticReceiverThread receiverThread = new Z2ArithmeticReceiverThread(receiver, operator, shareX1, shareY1);
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
            long senderByteLength = senderRpc.getSendByteLength();
            long receiverByteLength = receiverRpc.getSendByteLength();
            senderRpc.reset();
            receiverRpc.reset();
            SquareShareZ2Vector[] shareZ0 = senderThread.getShareZ0();
            SquareShareZ2Vector[] shareZ1 = receiverThread.getShareZ1();

            assertOutput(operator, zlDatabaseX, zlDatabaseY, shareZ0, shareZ1);
            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                    senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sender.destroy();
        receiver.destroy();
    }

    private void assertOutput(ArithmeticOperator operator, ZlDatabase zlDatabaseX, ZlDatabase zlDatabaseY,
                              SquareShareZ2Vector[] shareZ0, SquareShareZ2Vector[] shareZ1) {
        switch (operator) {
            case LEQ:
                assertLeq(zlDatabaseX, zlDatabaseY, shareZ0[0], shareZ1[0]);
                break;
            case BIT_ADD:
                assertBitAdd(zlDatabaseX, zlDatabaseY, shareZ0[0], shareZ1[0]);
                break;
            case ADD:
                assertAdd(zlDatabaseX, zlDatabaseY, shareZ0, shareZ1);
                break;
            case SUB:
                assertSub(zlDatabaseX, zlDatabaseY, shareZ0, shareZ1);
                break;
            case NOT:
                assertNot(zlDatabaseX, shareZ0, shareZ1);
                break;
            default:
                throw new IllegalStateException("Invalid arithmetic operator: " + operator.name());
        }
    }

    private void assertLeq(ZlDatabase zlDatabaseX, ZlDatabase zlDatabaseY,
                           SquareShareZ2Vector shareZ0, SquareShareZ2Vector shareZ1) {
        int rowNum = zlDatabaseX.rows();

        BitVector z = shareZ0.xor(shareZ1, false).getBitVector();

        BigInteger[] xData = Arrays.stream(zlDatabaseX.getBigIntegerData())
                .toArray(BigInteger[]::new);
        BigInteger[] yData = Arrays.stream(zlDatabaseY.getBigIntegerData())
                .toArray(BigInteger[]::new);

        for (int i = 0; i < rowNum; i++) {
            boolean result = z.get(i);
            boolean plainResult = xData[i].compareTo(yData[i]) < 0;
            Assert.assertEquals(result, plainResult);
        }
    }

    private void assertAdd(ZlDatabase zlDatabaseX, ZlDatabase zlDatabaseY,
                           SquareShareZ2Vector[] shareZ0, SquareShareZ2Vector[] shareZ1) {

        BitVector[] z = IntStream.range(0, zlDatabaseX.getL()).mapToObj(i -> shareZ0[i].xor(shareZ1[i], false))
                .map(SquareShareZ2Vector::getBitVector).toArray(BitVector[]::new);

        ZlDatabase zlDatabaseZ = ZlDatabase.create(envType, false, z);

        BigInteger[] xData = zlDatabaseX.getBigIntegerData();
        BigInteger[] yData = zlDatabaseY.getBigIntegerData();
        BigInteger[] xAddYData = IntStream.range(0, xData.length).mapToObj(i -> xData[i].add(yData[i]).mod(BigInteger.ONE.shiftLeft(DEFAULT_ARITH_LENGTH))).toArray(BigInteger[]::new);
        BigInteger[] zData = zlDatabaseZ.getBigIntegerData();

        for (int i = 0; i < zlDatabaseX.rows(); i++) {
            Assert.assertEquals(xAddYData[i], zData[i]);
        }
    }

    private void assertSub(ZlDatabase zlDatabaseX, ZlDatabase zlDatabaseY,
                           SquareShareZ2Vector[] shareZ0, SquareShareZ2Vector[] shareZ1) {

        BitVector[] z = IntStream.range(0, zlDatabaseX.getL()).mapToObj(i -> shareZ0[i].xor(shareZ1[i], false))
                .map(SquareShareZ2Vector::getBitVector).toArray(BitVector[]::new);
        ZlDatabase zlDatabaseZ = ZlDatabase.create(envType, false, z);

        BigInteger[] xData = Arrays.stream(zlDatabaseX.getBigIntegerData())
                .toArray(BigInteger[]::new);
        BigInteger[] yData = Arrays.stream(zlDatabaseY.getBigIntegerData())
                .toArray(BigInteger[]::new);
        BigInteger[] xSubYData = IntStream.range(0, xData.length)
                .mapToObj(i -> xData[i].subtract(yData[i]).mod(BigInteger.ONE.shiftLeft(DEFAULT_ARITH_LENGTH)))
                .map(v -> v.compareTo(BigInteger.ZERO) < 0 ? v.add(BigInteger.ONE.shiftLeft(DEFAULT_ARITH_LENGTH)) : v)
                .toArray(BigInteger[]::new);
        BigInteger[] zData = zlDatabaseZ.getBigIntegerData();

        for (int i = 0; i < zlDatabaseX.rows(); i++) {
            Assert.assertEquals(xSubYData[i], zData[i]);
        }
    }

    private void assertNot(ZlDatabase zlDatabaseX,
                           SquareShareZ2Vector[] shareZ0, SquareShareZ2Vector[] shareZ1) {
        BitVector[] z = IntStream.range(0, zlDatabaseX.getL()).mapToObj(i -> shareZ0[i].xor(shareZ1[i], false))
                .map(SquareShareZ2Vector::getBitVector).toArray(BitVector[]::new);

        ZlDatabase zlDatabaseZ = ZlDatabase.create(envType, false, z);

        BigInteger[] notXData = Arrays.stream(zlDatabaseX.getBigIntegerData())
                .map(xDatum -> xDatum.negate().subtract(BigInteger.ONE).mod(BigInteger.ONE.shiftLeft(DEFAULT_ARITH_LENGTH)))
                .map(v -> v.compareTo(BigInteger.ZERO) < 0 ? v.add(BigInteger.ONE.shiftLeft(DEFAULT_ARITH_LENGTH)) : v)
                .toArray(BigInteger[]::new);
        BigInteger[] zData = zlDatabaseZ.getBigIntegerData();

        for (int i = 0; i < notXData.length; i++) {
            Assert.assertEquals(notXData[i], zData[i]);
        }
    }

    private void assertBitAdd(ZlDatabase zlDatabaseX, ZlDatabase zlDatabaseY,
                              SquareShareZ2Vector shareZ0, SquareShareZ2Vector shareZ1) {
        int rowNum = zlDatabaseX.rows();

        BitVector x = zlDatabaseX.bitPartition(envType, false)[0];
        BitVector y = zlDatabaseY.bitPartition(envType, false)[0];
        BitVector result = shareZ0.xor(shareZ1, false).getBitVector();
        BitVector plainResult = x.xor(y);
        for (int i = 0; i < rowNum; i++) {
            Assert.assertEquals(result.get(i), plainResult.get(i));
        }
    }
}
