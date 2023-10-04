package edu.alibaba.mpc4j.s2pc.pir.cppir.index;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoSingleIndexCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoSingleIndexCpPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoSingleIndexCpPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleSingleIndexCpPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleSingleIndexCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleSingleIndexCpPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.SpamSingleIndexCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.SpamSingleIndexCpPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.SpamSingleIndexCpPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.shuffle.ShuffleSingleIndexCpPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.shuffle.ShuffleSingleIndexCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.shuffle.ShuffleSingleIndexCpPirServer;

/**
 * Single Index Client-specific Preprocessing PIR factory.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public class SingleIndexCpPirFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private SingleIndexCpPirFactory() {
        // empty
    }

    /**
     * Single Index Client-specific Preprocessing PIR type
     */
    public enum SingleIndexCpPirType {
        /**
         * LLP23 (Shuffle)
         */
        LLP23_SHUFFLE,
        /**
         * MIR23 (SPAM)
         */
        MIR23_SPAM,
        /**
         * ZPSZ23 (PIANO)
         */
        ZPSZ23_PIANO,
        /**
         * HHCM23 (Simple).
         */
        HHCM23_SIMPLE,
    }

    /**
     * create a server.
     *
     * @param serverRpc   server RPC.
     * @param clientParty client party.
     * @param config      config.
     * @return a server.
     */
    public static SingleIndexCpPirServer createServer(Rpc serverRpc, Party clientParty, SingleIndexCpPirConfig config) {
        SingleIndexCpPirType type = config.getPtoType();
        switch (type) {
            case LLP23_SHUFFLE:
                return new ShuffleSingleIndexCpPirServer(serverRpc, clientParty, (ShuffleSingleIndexCpPirConfig) config);
            case MIR23_SPAM:
                return new SpamSingleIndexCpPirServer(serverRpc, clientParty, (SpamSingleIndexCpPirConfig) config);
            case ZPSZ23_PIANO:
                return new PianoSingleIndexCpPirServer(serverRpc, clientParty, (PianoSingleIndexCpPirConfig) config);
            case HHCM23_SIMPLE:
                return new SimpleSingleIndexCpPirServer(serverRpc, clientParty, (SimpleSingleIndexCpPirConfig) config);
            default:
                throw new IllegalArgumentException(
                    "Invalid " + SingleIndexCpPirType.class.getSimpleName() + ": " + type.name()
                );
        }
    }

    /**
     * create a client.
     *
     * @param clientRpc   client RPC.
     * @param serverParty server party.
     * @param config      config.
     * @return a client.
     */
    public static SingleIndexCpPirClient createClient(Rpc clientRpc, Party serverParty, SingleIndexCpPirConfig config) {
        SingleIndexCpPirType type = config.getPtoType();
        switch (type) {
            case LLP23_SHUFFLE:
                return new ShuffleSingleIndexCpPirClient(clientRpc, serverParty, (ShuffleSingleIndexCpPirConfig) config);
            case MIR23_SPAM:
                return new SpamSingleIndexCpPirClient(clientRpc, serverParty, (SpamSingleIndexCpPirConfig) config);
            case ZPSZ23_PIANO:
                return new PianoSingleIndexCpPirClient(clientRpc, serverParty, (PianoSingleIndexCpPirConfig) config);
            case HHCM23_SIMPLE:
                return new SimpleSingleIndexCpPirClient(clientRpc, serverParty, (SimpleSingleIndexCpPirConfig) config);
            default:
                throw new IllegalArgumentException(
                    "Invalid " + SingleIndexCpPirType.class.getSimpleName() + ": " + type.name()
                );
        }
    }
}
