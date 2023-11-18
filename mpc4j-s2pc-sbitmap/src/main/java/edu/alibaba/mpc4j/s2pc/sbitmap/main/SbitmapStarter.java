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
import edu.alibaba.mpc4j.s2pc.opf.groupagg.sorting.SortingGroupAggConfig;
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
     * Metadata of dataset.
     */
    protected StructType schema;
    /**
     * Mapping type.
     */
    protected String mappingType;
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

    public SbitmapStarter(Properties properties) {
        this.properties = properties;
    }

    public void start() throws IOException, MpcAbortException {
        String filePath =
            // dataset name
            datasetName
                // total round of test
                + "_" + totalRound
                // party id
                + "_" + ownRpc.ownParty().getPartyId()
                + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // output table title
        String tab = "name\tε\tθ\tα\tTime(ms)\t" +
            "Train Measure\tTest Measure\t" +
            "Send Packet Num\tSend Payload Bytes(B)\tSend Total Bytes(B)";
        printWriter.println(tab);
        // connect
        ownRpc.connect();
        // Full secure
        runFullSecurePto(printWriter);
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
        // set dataset
        setDataSet();
        // 设置总测试轮数
        totalRound = SbitmapMainUtils.setTotalRound(properties);
    }

    private void setDataSet() throws IOException, URISyntaxException {
        if (receiver) {
            StructField groupField = new StructField("group", DataTypes.StringType);
            StructField aggField = new StructField("agg", DataTypes.IntegerType);
            StructField eField = new StructField("e", DataTypes.IntegerType);
            StructType structType = new StructType(groupField, aggField, eField);
            DataFrame inputDataFrame = SbitmapMainUtils.setDataFrame(properties, structType);
            num = inputDataFrame.nrows();
            String[] groups = inputDataFrame.stringVector("group").stream().toArray(String[]::new);
            long[] agg = inputDataFrame.intVector("agg").stream().mapToLong(i -> i).toArray();
            SquareZ2Vector e = genE(inputDataFrame);
            groupAggInputData = new GroupAggInputData(groups, agg, e);
        } else {
            StructField groupField = new StructField("group", DataTypes.StringType);
            StructField eField = new StructField("e", DataTypes.IntegerType);
            StructType structType = new StructType(groupField, eField);
            DataFrame inputDataFrame = SbitmapMainUtils.setDataFrame(properties, structType);
            num = inputDataFrame.nrows();
            String[] groups = inputDataFrame.stringVector("group").stream().toArray(String[]::new);
            SquareZ2Vector e = genE(inputDataFrame);
            groupAggInputData = new GroupAggInputData(groups, null, e);
        }
    }

    private SquareZ2Vector genE(DataFrame dataFrame) {
        int[] es = dataFrame.intVector("e").stream().toArray();
        BitVector bitVector = BitVectorFactory.createZeros(num);
        IntStream.range(0, num).forEach(i -> bitVector.set(i, es[i] == 1));
        return SquareZ2Vector.create(bitVector, false);
    }

    protected void writeInfo(PrintWriter printWriter,
                             Double time,
                             Double performanceMeasure,
                             long packetNum, long payloadByteLength, long sendByteLength) {
        String information = mappingType
            // time
            + "\t" + (Objects.isNull(time) ? "N/A" : time)
            // performance measure
            + "\t" + (Objects.isNull(performanceMeasure) ? "N/A" : performanceMeasure)
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
    protected void runFullSecurePto(PrintWriter printWriter)
        throws MpcAbortException {
        LOGGER.info("-----Pto aggregation for {}-----", groupAggType.name() + "_" + prefixAggType.name());


        GroupAggConfig groupAggConfig = genGroupAggConfig();

        GroupAggregationPtoRunner ptoRunner = createRunner(groupAggConfig);
        ptoRunner.init();
        ptoRunner.run();
        ptoRunner.stop();
        writeInfo(printWriter, ptoRunner.getTime(),
            null,
            ptoRunner.getPacketNum(), ptoRunner.getPayloadByteLength(), ptoRunner.getSendByteLength()
        );
    }

    private GroupAggConfig genGroupAggConfig() {
        switch (groupAggType) {
            case BITMAP:
                return new BitmapGroupAggConfig.Builder(DEFAULT_ZL, true, prefixAggType).build();
            case SORTING:
                return new SortingGroupAggConfig.Builder(DEFAULT_ZL, true, prefixAggType).build();
            case MIX:
                return new MixGroupAggConfig.Builder(DEFAULT_ZL, true, prefixAggType).build();
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
        return new GroupAggregationPtoRunner(party, groupAggConfig, totalRound, groupAggInputData);
    }


}