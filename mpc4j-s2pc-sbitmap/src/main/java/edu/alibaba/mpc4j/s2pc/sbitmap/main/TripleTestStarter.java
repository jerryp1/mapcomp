package edu.alibaba.mpc4j.s2pc.sbitmap.main;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcPropertiesUtils;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggFactory.GroupAggTypes;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgFactory.Z2MtgType;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgParty;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.cache.CacheZ2MtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.offline.OfflineZ2MtgConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Properties;

import static edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.hardcode.HardcodeZ2MtgSender.TRIPLE_NUM;

/**
 * Triple test starter.
 *
 * @author Li Peng
 * @date 2023/11/24
 */
public class TripleTestStarter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TripleTestStarter.class);
    /**
     * Own rpc
     */
    protected Rpc ownRpc;
    /**
     * The other party
     */
    protected Party otherParty;
    /**
     * Group aggregation type.
     */
    protected GroupAggTypes groupAggType;

    protected boolean receiver;
    /**
     * Total round fo test.
     */
    protected int totalRound = 1;
    /**
     * Test num.
     */
    private final int num;
    /**
     * Input parameters.
     */
    private final Properties properties;
    /**
     * Log num.
     */
    private final int logN;
    /**
     * Z2 mtg type.
     */
    private final Z2MtgType z2MtgType;

    public TripleTestStarter(int logN, String type, Properties properties) {
        this.logN = logN;
        this.num = 1 << logN;
        z2MtgType = Z2MtgType.valueOf(type);
        this.properties = properties;
    }

    public void start() throws IOException, MpcAbortException, URISyntaxException {
        // output file name
        String filePath = "./" + z2MtgType.name() + "_" + "triple_" + logN + "_" + ((ownRpc.ownParty().getPartyId() == 1) ? "r" : "s") + ".out";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // output table title
        String tab = "Data Num(bits)\t" + "Time(ms)\t" +
            "Send Packet Num\tSend Payload Bytes(B)\tSend Total Bytes(B)\tTriple Num";
        printWriter.println(tab);
        // connect
        ownRpc.connect();
        // Full secure
        runPto(printWriter, logN);
        // clean
        printWriter.close();
        fileWriter.close();
        ownRpc.disconnect();
    }

    public void init() throws IOException, URISyntaxException {
        // set rpc
        ownRpc = RpcPropertiesUtils.readNettyRpc(properties, "sender", "receiver");
        if (ownRpc.ownParty().getPartyId() == 0) {
            receiver = false;
            otherParty = ownRpc.getParty(1);
        } else {
            receiver = true;
            otherParty = ownRpc.getParty(0);
        }
    }

    protected void writeInfo(PrintWriter printWriter, int num,
                             Double time,
                             long packetNum, long payloadByteLength, long sendByteLength) {
        String information = num + "\t" +
            // time
            (Objects.isNull(time) ? "N/A" : time)
            // packet num
            + "\t" + packetNum
            // payload byte length
            + "\t" + payloadByteLength
            // send byte length
            + "\t" + sendByteLength
            + "\t" + TRIPLE_NUM;
        printWriter.println(information);
        TRIPLE_NUM = 0;
    }

    /**
     * Run protocol
     *
     * @param printWriter print writer.
     * @throws MpcAbortException the protocol failure aborts.
     */
    protected void runPto(PrintWriter printWriter, int num)
        throws MpcAbortException {
        LOGGER.info("-----Pto Triple Test for {}-----", z2MtgType.name());

        Z2MtgConfig groupAggConfig = genZ2MtgConfig();

        TripleTestRunner ptoRunner = createRunner(groupAggConfig);
        ptoRunner.init();
        ptoRunner.run();
        ptoRunner.stop();
        writeInfo(printWriter, num, ptoRunner.getTime(), ptoRunner.getPacketNum(),
            ptoRunner.getPayloadByteLength(), ptoRunner.getSendByteLength()
        );
    }

    private Z2MtgConfig genZ2MtgConfig() {
        switch (z2MtgType) {
            case OFFLINE:
                // IKNP-style
                Z2CoreMtgConfig z2CoreMtgConfig = Z2CoreMtgFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, false);
                return new OfflineZ2MtgConfig.Builder(SecurityModel.SEMI_HONEST).setCoreMtgConfig(z2CoreMtgConfig).build();
            case CACHE:
                // SILENT
                return new CacheZ2MtgConfig.Builder(SecurityModel.SEMI_HONEST).build();
            default:
                throw new IllegalArgumentException("Invalid " + Z2MtgConfig.class.getSimpleName() + ": " + groupAggType.name());
        }
    }

    TripleTestRunner createRunner(Z2MtgConfig groupAggConfig) {
        Z2MtgParty party;
        if (!receiver) {
            party = Z2MtgFactory.createSender(ownRpc, otherParty, groupAggConfig);
        } else {
            party = Z2MtgFactory.createReceiver(ownRpc, otherParty, groupAggConfig);
        }
        return new TripleTestRunner(party, totalRound, num);
    }

}