package edu.alibaba.mpc4j.s2pc.groupagg.main;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcPropertiesUtils;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.kvh21.Kvh21Bit2aConfig;
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
public class TupleTestStarter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TupleTestStarter.class);
    /**
     * Own rpc
     */
    protected Rpc ownRpc;
    /**
     * The other party
     */
    protected Party otherParty;
    /**
     * Whether current role is receiver.
     */
    protected boolean receiver;
    /**
     * Total round fo test.
     */
    protected int totalRound = 1;
    /**
     * Test number.
     */
    private final int num;
    /**
     * Input parameters.
     */
    private final Properties properties;
    /**
     * Log n.
     */
    private final int logN;

    public TupleTestStarter(int logN, Properties properties) {
        this.logN = logN;
        this.num = 1 << logN;
        this.properties = properties;
    }

    public void start() throws IOException, MpcAbortException, URISyntaxException {
        // output file format：bitmap_sum_s1_r2_s/r.output
        String filePath = "./" + "bit2a_tuple_" + logN + "_" + ((ownRpc.ownParty().getPartyId() == 1) ? "r" : "s") + ".out";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // output table title
        String tab = "Data Num(bits)\t" + "Time(ms)\t" +
            "Send Packet Num\tSend Payload Bytes(B)\tSend Total Bytes(B)\tTriple Num";
        printWriter.println(tab);
        // connect
        ownRpc.connect();
        // Full secure
        runFullSecurePto(printWriter, logN);
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
     * Run  protocol
     *
     * @param printWriter print writer.
     * @throws MpcAbortException the protocol failure aborts.
     */
    protected void runFullSecurePto(PrintWriter printWriter, int num)
        throws MpcAbortException {
        LOGGER.info("-----Pto tuple -----");

        Bit2aConfig groupAggConfig = genBit2aConfig();

        TupleTestRunner ptoRunner = createRunner(groupAggConfig);
        ptoRunner.init();
        ptoRunner.run();
        ptoRunner.stop();
        writeInfo(printWriter, num, ptoRunner.getTime(), ptoRunner.getPacketNum(),
            ptoRunner.getPayloadByteLength(), ptoRunner.getSendByteLength()
        );
    }

    private Bit2aConfig genBit2aConfig() {
        return new Kvh21Bit2aConfig.Builder(ZlFactory.createInstance(EnvType.STANDARD, 64), false).build();
    }

    TupleTestRunner createRunner(Bit2aConfig bit2aConfig) {
        Bit2aParty party;
        if (!receiver) {
            party = Bit2aFactory.createSender(ownRpc, otherParty, bit2aConfig);
        } else {
            party = Bit2aFactory.createReceiver(ownRpc, otherParty, bit2aConfig);
        }
        return new TupleTestRunner(party, totalRound, num);
    }

}