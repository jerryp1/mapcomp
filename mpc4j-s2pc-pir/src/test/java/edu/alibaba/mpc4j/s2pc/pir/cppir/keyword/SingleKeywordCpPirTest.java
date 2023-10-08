package edu.alibaba.mpc4j.s2pc.pir.cppir.keyword;

import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleIndexCpPirFactory.SingleIndexCpPirType;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoSingleIndexCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.shuffle.ShuffleSingleIndexCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleSingleIndexCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.SpamSingleIndexCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.SingleKeywordCpPirFactory.SingleKeywordCpPirType;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.alpr21.Alpr21SingleKeywordCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.shuffle.ShuffleSingleKeywordCpPirConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

        // LLP23_SHUFFLE
        configurations.add(new Object[]{
            SingleKeywordCpPirType.LLP23_SHUFFLE.name(),
            new ShuffleSingleKeywordCpPirConfig.Builder().build()
        });
        // ALPR21 + SHUFFLE
        configurations.add(new Object[]{
            SingleKeywordCpPirType.ALPR21.name() + "(" + SingleIndexCpPirType.LLP23_SHUFFLE + ")",
            new Alpr21SingleKeywordCpPirConfig.Builder()
                .setSingleIndexCpPirConfig(new ShuffleSingleIndexCpPirConfig.Builder().build())
                .build()
        });
        // ALPR21 + SPAM
        configurations.add(new Object[]{
            SingleKeywordCpPirType.ALPR21.name() + "(" + SingleIndexCpPirType.MIR23_SPAM + ")",
            new Alpr21SingleKeywordCpPirConfig.Builder()
                .setSingleIndexCpPirConfig(new SpamSingleIndexCpPirConfig.Builder().build())
                .build()
        });
        // ALPR21 + PIANO
        configurations.add(new Object[]{
            SingleKeywordCpPirType.ALPR21.name() + "(" + SingleIndexCpPirType.ZPSZ23_PIANO + ")",
            new Alpr21SingleKeywordCpPirConfig.Builder()
                .setSingleIndexCpPirConfig(new PianoSingleIndexCpPirConfig.Builder().build())
                .build()
        });
        // ALPR21 + Simple
        configurations.add(new Object[]{
            SingleKeywordCpPirType.ALPR21.name() + "(" + SingleIndexCpPirType.HHCM23_SIMPLE + ")",
            new Alpr21SingleKeywordCpPirConfig.Builder()
                .setSingleIndexCpPirConfig(new SimpleSingleIndexCpPirConfig.Builder().build())
                .build()
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
        testPto(1 << 18, DEFAULT_L, 1 << 6, false);
    }

    @Test
    public void testParallelLarge() {
        testPto(1 << 18, DEFAULT_L, 1 << 6, true);
    }

    public void testPto(int n, int l, int queryNum, boolean parallel) {
        int byteL = CommonUtils.getByteLength(l);
        Map<String, byte[]> keywordValueMap = IntStream.range(0, n)
            .boxed()
            .collect(Collectors.toMap(
                String::valueOf,
                index -> BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM)
            ));
        ArrayList<String> keywordArrayList = new ArrayList<>(keywordValueMap.keySet());
        List<String> queryList = IntStream.range(0, queryNum)
            .mapToObj(index -> {
                if (index % 3 == 0) {
                    return keywordArrayList.get(index % n);
                } else {
                    return  "dummy_" + index;
                }
            })
            .collect(Collectors.toList());
        SingleKeywordCpPirServer<String> server = SingleKeywordCpPirFactory.createServer(
            firstRpc, secondRpc.ownParty(), config
        );
        SingleKeywordCpPirClient<String> client = SingleKeywordCpPirFactory.createClient(
            secondRpc, firstRpc.ownParty(), config
        );
        server.setParallel(parallel);
        client.setParallel(parallel);
        SingleKeywordCpPirServerThread serverThread = new SingleKeywordCpPirServerThread(server, keywordValueMap, l, queryNum);
        SingleKeywordCpPirClientThread clientThread = new SingleKeywordCpPirClientThread(client, n, l, queryList);
        try {
            serverThread.start();
            clientThread.start();
            serverThread.join();
            clientThread.join();
            // verify result
            Map<String, byte[]> retrievalResult = clientThread.getRetrievalResult();
            for (String x : queryList) {
                if (keywordValueMap.containsKey(x)) {
                    Assert.assertArrayEquals(keywordValueMap.get(x), retrievalResult.get(x));
                } else {
                    Assert.assertNull(retrievalResult.get(x));
                }
            }
            // destroy
            new Thread(server::destroy).start();
            new Thread(client::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
