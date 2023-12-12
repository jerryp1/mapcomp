package edu.alibaba.mpc4j.s2pc.pjc.pmap;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.naive.NaivePeqtConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapFactory.PmapType;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapPartyOutput.MapType;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.hpl24.Hpl24PmapConfig;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.rs21.Rs21PlpsiConfig;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * PID protocol test.
 *
 * @author Feng Han
 * @date 2023/11/03
 */
@RunWith(Parameterized.class)
public class PmapTest extends AbstractTwoPartyPtoTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PmapTest.class);
    /**
     * bitLen
     */
    private static final int[] bitLens = new int[]{20, 17};
    /**
     * default small size
     */
    private static final int DEFAULT_SMALL_SIZE = 99;
    /**
     * 较大数量
     */
    private static final int LARGE_SIZE = 1 << 14;

    /**
     * default middle size, in order to make 1. n_x >= m_y, 2. m_y > n_x > n_y
     */
    private static final int[] DEFAULT_SIZE_PAIR_0 = new int[]{100, 2};
    private static final int[] DEFAULT_SIZE_PAIR_1 = new int[]{120, 99};
    private static final int[] DEFAULT_SIZE_PAIR_2 = new int[]{200, 20};
    private static final int[] DEFAULT_SIZE_PAIR_3 = new int[]{LARGE_SIZE, DEFAULT_SMALL_SIZE};
    /**
     * silent
     */
    private static final boolean silent = false;


    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

//        configurations.add(new Object[]{
//            PmapType.PID_BASED.name() + "_Gmr21Sloppy_JSZ22_SFC",
//            new PidBasedPmapConfig.Builder(silent).build(),
//        });
//
//        configurations.add(new Object[]{
//            PmapType.PID_BASED.name() + "_Gmr21Mp_JSZ22_SFC",
//            new PidBasedPmapConfig.Builder(silent).setPidConfig(
//                new Gmr21MpPidConfig.Builder().setPsuConfig(
//                    new Jsz22SfcPsuConfig.Builder(false).build()).build()).build(),
//        });
//
//        configurations.add(new Object[]{
//            PmapType.PID_BASED.name() + "_Bkms20_Byte_Ecc",
//            new PidBasedPmapConfig.Builder(silent).setPidConfig(
//                new Bkms20ByteEccPidConfig.Builder().build()).build(),
//        });

        for(int bitLen : bitLens){
            configurations.add(new Object[]{
                PmapType.HPL24.name()+ "_bitLen_" + bitLen,
                new Hpl24PmapConfig.Builder(silent).setBitLength(bitLen, silent).build(),
            });

            configurations.add(new Object[]{
                PmapType.HPL24.name()+ "_naive_peqt_bitLen_" + bitLen,
                new Hpl24PmapConfig.Builder(silent).setPlpsiconfig(
                    new Rs21PlpsiConfig.Builder(silent).setPeqtConfig(
                        new NaivePeqtConfig.Builder(SecurityModel.SEMI_HONEST, false).build()).build())
                    .setBitLength(bitLen, silent).build(),
            });
        }
        return configurations;
    }

    /**
     * element byte length
     */
    private static final int ELEMENT_BIT_LENGTH = CommonConstants.BLOCK_BIT_LENGTH;
    /**
     * element byte length
     */
    private static final int ELEMENT_BYTE_LENGTH = CommonUtils.getByteLength(ELEMENT_BIT_LENGTH);

    /**
     * the config
     */
    private final PmapConfig config;

    public PmapTest(String name, PmapConfig config) {
        super(name);
        this.config = config;
    }

//    @Test
//    public void testLOG() {
//        byte a = (byte) (1<<7);
//        byte b = 1;
//        LOGGER.info(String.valueOf((a)));
//        LOGGER.info(String.valueOf((a >>> 7)));
//        LOGGER.info(String.valueOf((b&1)));
//        LOGGER.info(String.valueOf(((byte)(a >>>7) & 1) == (b&1)));
//    }

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
        testPto(DEFAULT_SMALL_SIZE, DEFAULT_SMALL_SIZE, true);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_SMALL_SIZE, DEFAULT_SMALL_SIZE, true);
    }

    @Test
    public void testUnbalancedSize0() {
        testPto(DEFAULT_SIZE_PAIR_0[0], DEFAULT_SIZE_PAIR_0[1], false);
    }

    @Test
    public void testUnbalancedSize1() {
        testPto(DEFAULT_SIZE_PAIR_1[0], DEFAULT_SIZE_PAIR_1[1], false);
    }

    @Test
    public void testUnbalancedSize2() {
        testPto(DEFAULT_SIZE_PAIR_2[0], DEFAULT_SIZE_PAIR_2[1], false);
    }

    @Test
    public void testUnbalancedSize3() {
        testPto(DEFAULT_SIZE_PAIR_3[0], DEFAULT_SIZE_PAIR_3[1], false);
    }

    @Test
    public void testLarge() {
        testPto(LARGE_SIZE, LARGE_SIZE, false);
    }

    @Test
    public void testParallelLarge() {
        testPto(LARGE_SIZE, LARGE_SIZE, true);
    }

    private void testPto(int serverListSize, int clientListSize, boolean parallel) {
        PmapServer<ByteBuffer> server = PmapFactory.createServer(firstRpc, secondRpc.ownParty(), config);
        PmapClient<ByteBuffer> client = PmapFactory.createClient(secondRpc, firstRpc.ownParty(), config);
        server.setParallel(parallel);
        client.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        server.setTaskId(randomTaskId);
        client.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {}，server_set_size = {}，client_set_size = {}-----",
                server.getPtoDesc().getPtoName(), serverListSize, clientListSize
            );
            // generate the inputs
            ArrayList<Set<ByteBuffer>> sets = PsoUtils.generateBytesSets(serverListSize, clientListSize, ELEMENT_BYTE_LENGTH);
            List<ByteBuffer> serverElementList = new ArrayList<>(sets.get(0));
            List<ByteBuffer> clientElementList = new ArrayList<>(sets.get(1));
            PmapServerThread serverThread = new PmapServerThread(server, serverElementList, clientListSize);
            PmapClientThread clientThread = new PmapClientThread(client, clientElementList, serverListSize);
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
            PmapPartyOutput<ByteBuffer> serverOutput = serverThread.getPmapOutput();
            PmapPartyOutput<ByteBuffer> clientOutput = clientThread.getPmapOutput();
            assertOutput(serverElementList, clientElementList, serverOutput, clientOutput);
            printAndResetRpc(time);
            // destroy
            new Thread(server::destroy).start();
            new Thread(client::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(List<ByteBuffer> serverElementList, List<ByteBuffer> clientElementList,
                              PmapPartyOutput<ByteBuffer> serverOutput, PmapPartyOutput<ByteBuffer> clientOutput){
        HashSet<ByteBuffer> clientSet = new HashSet<>(clientElementList);
        BitVector equalFlag = serverOutput.getEqualFlag().getBitVector().xor(clientOutput.getEqualFlag().getBitVector());
        if(serverOutput.getMapType().equals(MapType.MAP)){
            MathPreconditions.checkEqual("equalFlag.bitNum()", "serverElementList.size()", equalFlag.bitNum(), serverElementList.size());
            MathPreconditions.checkEqual("serverOutput.getIndexMap().size()", "serverElementList.size()", serverOutput.getIndexMap().size(), serverElementList.size());
            MathPreconditions.checkEqual("clientOutput.getIndexMap().size()", "serverElementList.size()", clientOutput.getIndexMap().size(), serverElementList.size());
        } else if (serverOutput.getMapType().equals(MapType.PID)) {
            MathPreconditions.checkGreaterOrEqual("equalFlag.bitNum() >= serverElementList.size()", equalFlag.bitNum(), serverElementList.size());
            MathPreconditions.checkGreaterOrEqual("serverOutput.getIndexMap().size() >= serverElementList.size()", serverOutput.getIndexMap().size(), serverElementList.size());
            MathPreconditions.checkGreaterOrEqual("clientOutput.getIndexMap().size() >= serverElementList.size()", clientOutput.getIndexMap().size(), serverElementList.size());
        }

        Map<Integer, ByteBuffer> serverResMap = serverOutput.getIndexMap();
        Map<Integer, ByteBuffer> clientResMap = clientOutput.getIndexMap();
        for(int i = 0; i < serverResMap.size(); i++){
            ByteBuffer serverEle = serverResMap.get(i);
//            assert Arrays.equals(serverResMap.get(i).array(), serverEle.array());
            if(clientSet.contains(serverEle)){
                assert equalFlag.get(i);
                assert Arrays.equals(serverEle.array(), clientResMap.get(i).array());
            }else{
                assert !equalFlag.get(i);
            }
        }
    }

}
