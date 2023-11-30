package edu.alibaba.mpc4j.s2pc.sbitmap.main;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcPropertiesUtils;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggFactory;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggFactory.GroupAggTypes;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggParty;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.bitmap.BitmapGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.bsorting.BitmapSortingGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.mix.MixGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.osorting.OptimizedSortingGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.sorting.SortingGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.tsorting.TrivialSortingGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggFactory.PrefixAggTypes;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgFactory.Z2MtgType;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgParty;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.cache.CacheZ2MtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.offline.OfflineZ2MtgConfig;
import edu.alibaba.mpc4j.s2pc.sbitmap.pto.GroupAggInputData;
import edu.alibaba.mpc4j.s2pc.sbitmap.utils.SbitmapMainUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smile.data.DataFrame;
import smile.data.type.DataTypes;
import smile.data.type.StructField;
import smile.data.type.StructType;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.opf.groupagg.mix.MixGroupAggSender.MIX_TIME_AGG;
import static edu.alibaba.mpc4j.s2pc.opf.groupagg.mix.MixGroupAggSender.MIX_TRIPLE_AGG;
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

    private int num;

    private Properties properties;
    private int logN;
    private Z2MtgType z2MtgType;


    public TripleTestStarter(int logN, String type, Properties properties) {
        this.logN =logN;
        this.num = 1 << logN;
        z2MtgType = Z2MtgType.valueOf(type);
        this.properties = properties;
    }

    public void start() throws IOException, MpcAbortException, URISyntaxException {
        // output file format：bitmap_sum_s1_r2_s/r.output
        String filePath = "./" + z2MtgType.name() +"_" + "triple_" + logN + "_" + ((ownRpc.ownParty().getPartyId() == 1) ? "r" : "s") +".out";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // output table title
        String tab = "Data Num(bits)\t" + "Time(ms)\t" +
            "Send Packet Num\tSend Payload Bytes(B)\tSend Total Bytes(B)\tTriple Num\tMix Time\tMix Triple";
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
            + "\t" + TRIPLE_NUM + "\t" + MIX_TIME_AGG + "\t" + MIX_TRIPLE_AGG;
        printWriter.println(information);
        TRIPLE_NUM = 0;
        MIX_TIME_AGG = 0;
        MIX_TRIPLE_AGG = 0;
    }


    /**
     * Run full secure protocol
     *
     * @param printWriter print writer.
     * @throws MpcAbortException the protocol failure aborts.
     */
    protected void runFullSecurePto(PrintWriter printWriter, int num)
        throws MpcAbortException {
        LOGGER.info("-----Pto aggregation for {}-----", z2MtgType.name());

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
        switch (z2MtgType){
            case OFFLINE:
                Z2CoreMtgConfig z2CoreMtgConfig = Z2CoreMtgFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, false);
                return new OfflineZ2MtgConfig.Builder(SecurityModel.SEMI_HONEST).setCoreMtgConfig(z2CoreMtgConfig).build();
            case CACHE:
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
            party = Z2MtgFactory.createReceiver(ownRpc,otherParty,groupAggConfig);
        }
        return new TripleTestRunner(party, groupAggConfig, totalRound, num);
    }

}