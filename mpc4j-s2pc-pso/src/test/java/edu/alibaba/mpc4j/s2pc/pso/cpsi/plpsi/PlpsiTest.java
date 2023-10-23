package edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi;

import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.PlpsiFactory.PlpsiType;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.psty19.Psty19PlpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.rs21.Rs21PlpsiConfig;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RunWith(Parameterized.class)
public class PlpsiTest extends AbstractTwoPartyPtoTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlpsiTest.class);
    /**
     * default payload bit length
     */
    private static final int PAYLOAD_BIT_LEN = 55;
    /**
     * default size
     */
    private static final int DEFAULT_SIZE = 99;
    /**
     * 较大数量
     */
    private static final int LARGE_SIZE = 1 << 12;
    /**
     * element byte length
     */
    private static final int ELEMENT_BIT_LENGTH = CommonConstants.BLOCK_BIT_LENGTH;
    /**
     * element byte length
     */
    private static final int ELEMENT_BYTE_LENGTH = CommonUtils.getByteLength(ELEMENT_BIT_LENGTH);
    private static final CuckooHashBinType[] CUCKOO_HASH_BIN_TYPES = new CuckooHashBinType[]{
        CuckooHashBinType.NO_STASH_PSZ18_3_HASH,
        CuckooHashBinType.NAIVE_2_HASH,
        CuckooHashBinType.NAIVE_4_HASH,
    };

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        for (CuckooHashBinType type : CUCKOO_HASH_BIN_TYPES) {
            // RS21
            configurations.add(new Object[]{
                PlpsiType.RS21.name() + " (silent, " + type.name() + ")" + "binary",
                new Rs21PlpsiConfig.Builder(true).setCuckooHashBinType(type).build(),
            });
            configurations.add(new Object[]{
                PlpsiType.RS21.name() + " (direct, " + type.name() + ")" + "binary",
                new Rs21PlpsiConfig.Builder(false).setCuckooHashBinType(type).build(),
            });
            configurations.add(new Object[]{
                PlpsiType.RS21.name() + " (silent, " + type.name() + ")" + "arithmetic",
                new Rs21PlpsiConfig.Builder(true).setShareType(false).setCuckooHashBinType(type).build(),
            });
            configurations.add(new Object[]{
                PlpsiType.RS21.name() + " (direct, " + type.name() + ")" + "arithmetic",
                new Rs21PlpsiConfig.Builder(false).setShareType(false).setCuckooHashBinType(type).build(),
            });
            // PSTY19
            configurations.add(new Object[]{
                PlpsiType.PSTY19.name() + " (silent, " + type.name() + ")" + "binary",
                new Psty19PlpsiConfig.Builder(true).setCuckooHashBinType(type).build(),
            });
            configurations.add(new Object[]{
                PlpsiType.PSTY19.name() + " (direct, " + type.name() + ")" + "binary",
                new Psty19PlpsiConfig.Builder(false).setCuckooHashBinType(type).build(),
            });
            configurations.add(new Object[]{
                PlpsiType.PSTY19.name() + " (silent, " + type.name() + ")" + "arithmetic",
                new Psty19PlpsiConfig.Builder(true).setShareType(false).setCuckooHashBinType(type).build(),
            });
            configurations.add(new Object[]{
                PlpsiType.PSTY19.name() + " (direct, " + type.name() + ")" + "arithmetic",
                new Psty19PlpsiConfig.Builder(false).setShareType(false).setCuckooHashBinType(type).build(),
            });
        }

        return configurations;
    }

    /**
     * the config
     */
    private final PlpsiConfig config;

    public PlpsiTest(String name, PlpsiConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test1() {
        testPto(1, 1, false);
    }

    @Test
    public void test2() {
        testPto(2, 2, false);
    }

    @Test
    public void test10() {
        testPto(10, 10, false);
    }

    @Test
    public void testDefault() {
        testPto(DEFAULT_SIZE, DEFAULT_SIZE, true);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_SIZE, DEFAULT_SIZE, true);
    }

    @Test
    public void testLargeServerSize() {
        testPto(LARGE_SIZE, DEFAULT_SIZE, false);
    }

    @Test
    public void testLargeClientSize() {
        testPto(DEFAULT_SIZE, LARGE_SIZE, false);
    }

    @Test
    public void testLarge() {
        testPto(LARGE_SIZE, LARGE_SIZE, false);
    }

    @Test
    public void testParallelLarge() {
        testPto(LARGE_SIZE, LARGE_SIZE, true);
    }

    private void testPto(int serverSetSize, int clientSetSize, boolean parallel) {
        PlpsiServer<ByteBuffer> server = PlpsiFactory.createServer(firstRpc, secondRpc.ownParty(), config);
        PlpsiClient<ByteBuffer> client = PlpsiFactory.createClient(secondRpc, firstRpc.ownParty(), config);
        server.setParallel(parallel);
        client.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        server.setTaskId(randomTaskId);
        client.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {}，server_set_size = {}，client_set_size = {}-----",
                server.getPtoDesc().getPtoName(), serverSetSize, clientSetSize
            );
            // generate the inputs
            ArrayList<Set<ByteBuffer>> sets = PsoUtils.generateBytesSets(serverSetSize, clientSetSize, ELEMENT_BYTE_LENGTH);
            List<ByteBuffer> payload = generatePayload(serverSetSize);
            List<ByteBuffer> serverElementList = sets.get(0).stream().toList();
            List<ByteBuffer> clientElementList = sets.get(1).stream().toList();
            PlpsiServerThread serverThread = new PlpsiServerThread(server, serverElementList, payload, clientSetSize, PAYLOAD_BIT_LEN);
            PlpsiClientThread clientThread = new PlpsiClientThread(client, clientElementList, serverSetSize, PAYLOAD_BIT_LEN);
            StopWatch stopWatch = new StopWatch();
            // start
            stopWatch.start();
            serverThread.start();
            clientThread.start();
            // stop
            serverThread.join();
            clientThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            // verify
            PlpsiServerOutput serverOutput = serverThread.getServerOutput();
            PlpsiClientOutput<ByteBuffer> clientOutput = clientThread.getClientOutput();
            assertOutput(serverElementList, clientElementList, payload, serverOutput, clientOutput, config.isBinaryShare());
            printAndResetRpc(time);
            // destroy
            new Thread(server::destroy).start();
            new Thread(client::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(List<ByteBuffer> serverElementList, List<ByteBuffer> clientElementList, List<ByteBuffer> payload,
                              PlpsiServerOutput serverOutput, PlpsiClientOutput<ByteBuffer> clientOutput, boolean isBinaryShare) {
        int byteL = PAYLOAD_BIT_LEN > 0 ? CommonUtils.getByteLength(PAYLOAD_BIT_LEN) : 0;
        Set<ByteBuffer> expectIntersectionSet = new HashSet<>(serverElementList);
        expectIntersectionSet.retainAll(clientElementList);
        ArrayList<ByteBuffer> table = clientOutput.getTable();
        BitVector z = serverOutput.getZ1().getBitVector().xor(clientOutput.getZ1().getBitVector());
        List<byte[]> serverPayloadShare = null, clientPayloadShare = null;
        Zl zl = null;
        BigInteger[] serverShareA = null, clientShareA = null;
        HashMap<ByteBuffer, ByteBuffer> hashMap = new HashMap<>();
        if(PAYLOAD_BIT_LEN > 0){
            IntStream.range(0, serverElementList.size()).forEach(i -> hashMap.put(serverElementList.get(i), payload.get(i)));
            if (isBinaryShare) {
                serverPayloadShare = trans(serverOutput.getZ2Payload());
                clientPayloadShare = trans(clientOutput.getZ2Payload());
            } else {
                serverShareA = serverOutput.getZlPayload().getZlVector().getElements();
                clientShareA = clientOutput.getZlPayload().getZlVector().getElements();
                zl = serverOutput.getZlPayload().getZl();
            }
        }
        int beta = clientOutput.getBeta();
        for (int i = 0; i < beta; i++) {
            if (table.get(i) == null) {
                Assert.assertFalse(z.get(i));
            } else if (expectIntersectionSet.contains(table.get(i))) {
                Assert.assertTrue(z.get(i));
                if(PAYLOAD_BIT_LEN > 0){
                    if (isBinaryShare) {
                        Assert.assertArrayEquals(hashMap.get(table.get(i)).array(),
                            BytesUtils.xor(serverPayloadShare.get(i), clientPayloadShare.get(i)));
                    } else {
                        Assert.assertArrayEquals(hashMap.get(table.get(i)).array(),
                            BytesUtils.paddingByteArray(BigIntegerUtils.bigIntegerToByteArray(zl.add(serverShareA[i], clientShareA[i])), byteL));
                    }
                }
            } else {
                Assert.assertFalse(z.get(i));
            }
        }
    }

    private List<byte[]> trans(SquareZ2Vector[] vectors) {
        SquareZ2Vector[] tmpRes = Payload.transZ2Share(EnvType.STANDARD, true,
            Arrays.stream(vectors).map(x -> x.getBitVector().getBytes()).toArray(byte[][]::new), vectors[0].bitNum());
        return Arrays.stream(tmpRes).map(x -> x.getBitVector().getBytes()).collect(Collectors.toList());
    }

    private List<ByteBuffer> generatePayload(int serverSetSize) {
        if(PAYLOAD_BIT_LEN == 0){
            return null;
        }
        int payloadByteL = CommonUtils.getByteLength(PAYLOAD_BIT_LEN);
        SecureRandom secureRandom = new SecureRandom();
        return IntStream.range(0, serverSetSize).mapToObj(i ->
            ByteBuffer.wrap(BytesUtils.randomByteArray(payloadByteL, PAYLOAD_BIT_LEN, secureRandom))
        ).collect(Collectors.toList());
    }
}
