package edu.alibaba.mpc4j.s2pc.pir.cppir.keyword;

import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.SpamSingleIndexCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.SingleKeywordCpPirFactory.SingleKeywordCpPirType;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.alpr21.Alpr21SingleKeywordCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.llp23.Llp23SingleKeywordCpPirConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * Single Keyword Client-specific Preprocessing PIR test.
 *
 * @author Liqiang Peng
 * @date 2023/9/14
 */
@RunWith(Parameterized.class)
public class SingleKeywordCpPirTest extends AbstractTwoPartyPtoTest {
    /**
     * default element bit length
     */
    private static final int DEFAULT_L = Long.SIZE;
    /**
     * default database size
     */
    private static final int DEFAULT_N = (1 << 16) - 3;
    /**
     * default query num
     */
    private static final int DEFAULT_QUERY_NUM = 2;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // PIANO
        configurations.add(new Object[]{
            SingleKeywordCpPirType.ALPR21_SIMPLE_PIR.name() + " PIANO",
            new Alpr21SingleKeywordCpPirConfig.Builder().build()
        });
        // SPAM
        configurations.add(new Object[]{
            SingleKeywordCpPirType.ALPR21_SIMPLE_PIR.name() + " SPAM + DIGEST BYTE LENGTH 16",
            new Alpr21SingleKeywordCpPirConfig.Builder()
                .setSingleIndexCpPirConfig(new SpamSingleIndexCpPirConfig.Builder().build())
                .build()
        });
        // STREAM
        configurations.add(new Object[]{
            SingleKeywordCpPirType.LLP23_STREAM_PIR.name(), new Llp23SingleKeywordCpPirConfig.Builder().build()
        });
        return configurations;
    }

    /**
     * config
     */
    private final SingleKeywordCpPirConfig config;

    public SingleKeywordCpPirTest(String name, SingleKeywordCpPirConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test1n() {
        testPto(1, DEFAULT_L, 1, false);
    }

    @Test
    public void test2n() {
        testPto(2, DEFAULT_L, 1, false);
    }

    @Test
    public void testSpecificN() {
        testPto(11, DEFAULT_L, 1, false);
    }

    @Test
    public void testLargeQueryNum() {
        testPto(11, DEFAULT_L, 22, false);
    }

    @Test
    public void testSpecificValue() {
        testPto(DEFAULT_N, 11, DEFAULT_QUERY_NUM, false);
    }

    @Test
    public void testDefault() {
        testPto(DEFAULT_N, DEFAULT_L, DEFAULT_QUERY_NUM, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_N, DEFAULT_L, DEFAULT_QUERY_NUM, true);
    }

    @Test
    public void testLargeValue() {
        testPto(DEFAULT_N, 1 << 10, DEFAULT_QUERY_NUM, false);
    }

    @Test
    public void testParallelLargeValue() {
        testPto(DEFAULT_N, 1 << 10, DEFAULT_QUERY_NUM, true);
    }

    @Test
    public void testLarge() {
        testPto(1 << 20, DEFAULT_L, 1 << 10, false);
    }

    @Test
    public void testParallelLarge() {
        testPto(1 << 20, DEFAULT_L, 1 << 6, true);
    }

    public void testPto(int n, int l, int queryNum, boolean parallel) {
        int byteL = CommonUtils.getByteLength(l);
        List<Set<ByteBuffer>> randomSets = PirUtils.generateByteBufferSets(n, queryNum, 1);
        Map<ByteBuffer, ByteBuffer> keywordLabelMap = PirUtils.generateKeywordByteBufferLabelMap(
            randomSets.get(0), byteL
        );
        SingleKeywordCpPirServer server = SingleKeywordCpPirFactory.createServer(firstRpc, secondRpc.ownParty(), config);
        SingleKeywordCpPirClient client = SingleKeywordCpPirFactory.createClient(secondRpc, firstRpc.ownParty(), config);
        server.setParallel(parallel);
        client.setParallel(parallel);
        SingleKeywordCpPirServerThread serverThread = new SingleKeywordCpPirServerThread(
            server, keywordLabelMap, l, queryNum
        );
        SingleKeywordCpPirClientThread clientThread = new SingleKeywordCpPirClientThread(
            client, n, l, new ArrayList<>(randomSets.get(1))
        );
        try {
            serverThread.start();
            clientThread.start();
            serverThread.join();
            clientThread.join();
            // verify result
            Set<ByteBuffer> intersectionSet = new HashSet<>(randomSets.get(1));
            intersectionSet.retainAll(randomSets.get(0));
            Map<ByteBuffer, ByteBuffer> retrievalResult = clientThread.getRetrievalResult();
            Assert.assertEquals(intersectionSet.size(), retrievalResult.size());
            retrievalResult.forEach((key, value) -> Assert.assertEquals(value, keywordLabelMap.get(key)));
            // destroy
            new Thread(server::destroy).start();
            new Thread(client::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
