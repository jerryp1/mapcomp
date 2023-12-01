package edu.alibaba.mpc4j.s2pc.sbitmap.main;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcPropertiesUtils;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggFactory;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggFactory.GroupAggTypes;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggParty;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.bitmap.BitmapGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.bsorting.BitmapSortingGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.mix.MixGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.omix.OptimizedMixGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.oneside.OneSideGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.osorting.OptimizedSortingGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.sorting.SortingGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.tsorting.TrivialSortingGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggFactory.PrefixAggTypes;
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
 * Sbitmap starter.
 *
 * @author Li Peng
 * @date 2023/8/4
 */
public class SbitmapStarter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SbitmapStarter.class);
    /**
     * Configuration properties.
     */
    protected Properties properties;
    /**
     * Task type.
     */
    protected SbitmapTaskType taskType;
    /**
     * Own rpc
     */
    protected Rpc ownRpc;
    /**
     * The other party
     */
    protected Party otherParty;
    /**
     * Dataset name.
     */
    protected String datasetName;
    /**
     * Group aggregation type.
     */
    protected GroupAggTypes groupAggType;

    protected boolean receiver;

    private DataFrame inputDataFrame;
    private PrefixAggTypes prefixAggType;
    /**
     * Total round fo test.
     */
    protected int totalRound;
    /**
     * default Zl
     */
    private Zl zl;

    private GroupAggInputData groupAggInputData;
    private int senderGroupBitLength;
    private int receiverGroupBitLength;

    private StructType senderSchema;
    private StructType receiverSchema;

    private int[] testDataNums;

    private boolean silent;
    private String inputDir;
    private String outputDir;
    private boolean senderAgg;

    public SbitmapStarter() {
        setSchema();
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public void start() throws IOException, MpcAbortException, URISyntaxException {
        // output file format：bitmap_sum_s1_r2_s/r.output
        String filePath =  "./"+ outputDir + "/" + groupAggType.name() + "_" + prefixAggType.name() + "_" +
            "s" + senderGroupBitLength + "_" + "r" + receiverGroupBitLength + "_" +
            ((ownRpc.ownParty().getPartyId() == 1) ? "r" : "s") +"_"+ SbitmapMainUtils.getCurrentTime() +  ".out";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // output table title
        String tab = "Data Num(bits)\t" + "Time(ms)\t" +
            "Send Packet Num\tSend Payload Bytes(B)\tSend Total Bytes(B)\tTriple Num\t" +
            "GrS1Time\tGrS2Time\tGrS3Time\tGrS4Time\tGrS5Time\tAggTime\t" +
            "GroupTripleNum\tAggTripleNum";
        printWriter.println(tab);

        // Full secure
        for (int numBitLen:testDataNums) {
            int num = 1 << numBitLen;
            setDataSet(numBitLen);
            properties.setProperty("max_num", String.valueOf(num));
            runFullSecurePto(printWriter, numBitLen);
        }
        // clean
        printWriter.close();
        fileWriter.close();

    }

    public void init() throws IOException, URISyntaxException {
        // set dataset name
        datasetName = SbitmapMainUtils.setDatasetName(properties);
        // set aggregation type
        prefixAggType = SbitmapMainUtils.setPrefixAggTypes(properties);
        // set protocol type
        groupAggType = SbitmapMainUtils.setGroupAggTypes(properties);
        // group bit length
        senderGroupBitLength = SbitmapMainUtils.setSenderGroupBitLength(properties);
        receiverGroupBitLength = SbitmapMainUtils.setReceiverGroupBitLength(properties);
        // num
        testDataNums = SbitmapMainUtils.setTestDataNums(properties);
        // 设置总测试轮数
        totalRound = SbitmapMainUtils.setTotalRound(properties);
        // silent
        silent = SbitmapMainUtils.setSilent(properties);
        // silent
        senderAgg = SbitmapMainUtils.setSenderAgg(properties);
        // zl
        zl = SbitmapMainUtils.setZl(properties);
        // output_dir
        outputDir = SbitmapMainUtils.setOutputDir(properties);
        // input_dir
        inputDir = SbitmapMainUtils.setInputDir(properties);
    }

    public void initRpc() {
        // set rpc
        ownRpc = RpcPropertiesUtils.readNettyRpc(properties, "sender", "receiver");
        if (ownRpc.ownParty().getPartyId() == 0) {
            receiver = false;
            otherParty = ownRpc.getParty(1);
        } else {
            receiver = true;
            otherParty = ownRpc.getParty(0);
        }
        // connect
        ownRpc.connect();
    }

    public void stopRpc() {
        ownRpc.disconnect();
    }

    private void setDataSet(int num) throws IOException, URISyntaxException {
        String dataFileLength = "./" + inputDir + "/" + (receiver?("r" + receiverGroupBitLength):("s"+senderGroupBitLength)) +"_"+ num + ".csv";
        if (receiver) {
            DataFrame inputDataFrame = SbitmapMainUtils.setDataFrame(receiverSchema, dataFileLength);
            String[] groups = inputDataFrame.stringVector("group").stream().toArray(String[]::new);
            long[] agg = senderAgg ? null : inputDataFrame.intVector("agg").stream().mapToLong(i -> i).toArray();
            SquareZ2Vector e = genE(inputDataFrame);
            groupAggInputData = new GroupAggInputData(groups, agg, e);
        } else {
            DataFrame inputDataFrame = SbitmapMainUtils.setDataFrame(senderSchema, dataFileLength);
            String[] groups = inputDataFrame.stringVector("group").stream().toArray(String[]::new);
            long[] agg = senderAgg ? inputDataFrame.intVector("agg").stream().mapToLong(i -> i).toArray() : null;
            SquareZ2Vector e = genE(inputDataFrame);
            groupAggInputData = new GroupAggInputData(groups, agg, e);
        }
    }

    private SquareZ2Vector genE(DataFrame dataFrame) {
        int[] es = dataFrame.intVector("e").stream().toArray();
        BitVector bitVector = BitVectorFactory.createZeros(dataFrame.nrows());
        IntStream.range(0, dataFrame.nrows()).forEach(i -> bitVector.set(i, es[i] == 1));
        return SquareZ2Vector.create(bitVector, false);
    }

    protected void writeInfo(PrintWriter printWriter, int num,
                             Double time,
                             long packetNum, long payloadByteLength, long sendByteLength,
                             long grS1Time,long grS2Time,long grS3Time,long grS4Time,long grS5Time,long aggTime,
                             long groupTripleNum, long aggTripleNum) {
        String information = num + "\t" +
            // time
             (Objects.isNull(time) ? "N/A" : time)
            // packet num
            + "\t" + packetNum
            // payload byte length
            + "\t" + payloadByteLength
            // send byte length
            + "\t" + sendByteLength
            + "\t" + TRIPLE_NUM
            + "\t" + grS1Time + "\t" + grS2Time + "\t"+ grS3Time + "\t"+ grS4Time + "\t"+ grS5Time + "\t"+ aggTime
            + "\t" + groupTripleNum + "\t" + aggTripleNum;
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
        LOGGER.info("-----Pto aggregation for {}-----", groupAggType.name() + "_" + prefixAggType.name());

        GroupAggConfig groupAggConfig = genGroupAggConfig();

        GroupAggregationPtoRunner ptoRunner = createRunner(groupAggConfig);
        ptoRunner.init();
        ptoRunner.run();
        ptoRunner.stop();
        writeInfo(printWriter, num, ptoRunner.getTime(), ptoRunner.getPacketNum(),
            ptoRunner.getPayloadByteLength(), ptoRunner.getSendByteLength(),
            ptoRunner.getGroupStep1Time(), ptoRunner.getGroupStep2Time(), ptoRunner.getGroupStep3Time(), ptoRunner.getGroupStep4Time(), ptoRunner.getGroupStep5Time(),
            ptoRunner.getAggTime(), ptoRunner.getGroupTripleNum(), ptoRunner.getAggTripleNum()
        );
    }

    private GroupAggConfig genGroupAggConfig() {

        switch (groupAggType) {
            case BITMAP:
                return new BitmapGroupAggConfig.Builder(zl, silent, prefixAggType).build();
            case MIX:
                return new MixGroupAggConfig.Builder(zl, silent, prefixAggType).build();
            case O_MIX:
                return new OptimizedMixGroupAggConfig.Builder(zl,silent,prefixAggType).build();
            case SORTING:
                return new SortingGroupAggConfig.Builder(zl, silent, prefixAggType).build();
            case O_SORTING:
                return new OptimizedSortingGroupAggConfig.Builder(zl, silent, prefixAggType).build();
            case T_SORTING:
                return new TrivialSortingGroupAggConfig.Builder(zl, silent, prefixAggType).build();
            case B_SORTING:
                return new BitmapSortingGroupAggConfig.Builder(zl, silent, prefixAggType).build();
            case ONE_SIDE:
                return new OneSideGroupAggConfig.Builder(zl, silent, prefixAggType).build();
            default:
                throw new IllegalArgumentException("Invalid " + GroupAggTypes.class.getSimpleName() + ": " + groupAggType.name());
        }
    }

    GroupAggregationPtoRunner createRunner(GroupAggConfig groupAggConfig) {
        GroupAggParty party;
        if (!receiver) {
            party = GroupAggFactory.createSender(ownRpc, otherParty, groupAggConfig);
        } else {
            party = GroupAggFactory.createReceiver(ownRpc, otherParty, groupAggConfig);
        }
        return new GroupAggregationPtoRunner(party, groupAggConfig, totalRound, groupAggInputData, properties);
    }

    private void setSchema() {
        StructField groupField = new StructField("group", DataTypes.StringType);
        StructField aggField = new StructField("agg", DataTypes.IntegerType);
        StructField eField = new StructField("e", DataTypes.IntegerType);
        senderSchema = new StructType(groupField,aggField, eField);
        receiverSchema = new StructType(groupField, aggField, eField);
    }

}