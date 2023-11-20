package edu.alibaba.mpc4j.s2pc.sbitmap.main;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcPropertiesUtils;
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
import edu.alibaba.mpc4j.s2pc.opf.groupagg.mix.MixGroupAggConfig;
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
    protected final Properties properties;
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
     * Own dataset.
     */
    protected DataFrame ownDataFrame;
    /**
     * Metadata of own dataset
     */
    protected StructType ownSchema;
    /**
     * default Zl
     */
    private static final Zl DEFAULT_ZL = ZlFactory.createInstance(EnvType.STANDARD, Long.SIZE);

    private int num;
    private GroupAggInputData groupAggInputData;
    private int senderGroupBitLength;
    private int receiverGroupBitLength;

    private StructType senderSchema;
    private StructType receiverSchema;

    private int[] testDataNums;

    public SbitmapStarter(Properties properties) {
        this.properties = properties;
        setSchema();
    }

    public void start() throws IOException, MpcAbortException, URISyntaxException {
        // output file format：bitmap_sum_s1_r2_s/r.output
        String filePath = "./result/" + groupAggType.name() + "_" + prefixAggType.name() + "_" +
            "s" + senderGroupBitLength + "_" + "r" + receiverGroupBitLength + "_" +
            ((ownRpc.ownParty().getPartyId() == 1) ? "r" : "s") +"_"+ SbitmapMainUtils.getCurrentTime() +  ".out";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // output table title
        String tab = "Data Num(bits)\t" + "Time(ms)\t" +
            "Send Packet Num\tSend Payload Bytes(B)\tSend Total Bytes(B)";
        printWriter.println(tab);
        // connect
        ownRpc.connect();
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
    }

    private void setDataSet(int num) throws IOException, URISyntaxException {
        String dataFileLength = "./dataset/" + (receiver?("r" + receiverGroupBitLength):("s"+senderGroupBitLength)) +"_"+ num + ".csv";
        if (receiver) {
            DataFrame inputDataFrame = SbitmapMainUtils.setDataFrame(receiverSchema, dataFileLength);
            String[] groups = inputDataFrame.stringVector("group").stream().toArray(String[]::new);
            long[] agg = inputDataFrame.intVector("agg").stream().mapToLong(i -> i).toArray();
            SquareZ2Vector e = genE(inputDataFrame);
            groupAggInputData = new GroupAggInputData(groups, agg, e);
        } else {
            DataFrame inputDataFrame = SbitmapMainUtils.setDataFrame(senderSchema, dataFileLength);
            String[] groups = inputDataFrame.stringVector("group").stream().toArray(String[]::new);
            SquareZ2Vector e = genE(inputDataFrame);
            groupAggInputData = new GroupAggInputData(groups, null, e);
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
                             long packetNum, long payloadByteLength, long sendByteLength) {
        String information = num + "\t" +
            // time
             (Objects.isNull(time) ? "N/A" : time)
            // packet num
            + "\t" + packetNum
            // payload byte length
            + "\t" + payloadByteLength
            // send byte length
            + "\t" + sendByteLength;
        printWriter.println(information);
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
            ptoRunner.getPayloadByteLength(), ptoRunner.getSendByteLength()
        );
    }

    private GroupAggConfig genGroupAggConfig() {
        switch (groupAggType) {
            case BITMAP:
                return new BitmapGroupAggConfig.Builder(DEFAULT_ZL, true, prefixAggType).build();
            case MIX:
                return new MixGroupAggConfig.Builder(DEFAULT_ZL, true, prefixAggType).build();
            case SORTING:
                return new SortingGroupAggConfig.Builder(DEFAULT_ZL, true, prefixAggType).build();
            case O_SORTING:
                return new OptimizedSortingGroupAggConfig.Builder(DEFAULT_ZL, true, prefixAggType).build();
            case T_SORTING:
                return new TrivialSortingGroupAggConfig.Builder(DEFAULT_ZL, true, prefixAggType).build();
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
        senderSchema = new StructType(groupField, eField);
        receiverSchema = new StructType(groupField, aggField, eField);
    }

}