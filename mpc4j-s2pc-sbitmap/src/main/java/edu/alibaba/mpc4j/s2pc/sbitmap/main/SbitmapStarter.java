package edu.alibaba.mpc4j.s2pc.sbitmap.main;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcPropertiesUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.dp.ldp.LdpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smile.data.DataFrame;
import smile.data.type.StructType;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Map;
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
     * Whole Dataset.
     */
    protected DataFrame wholeDataFrame;
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
     * Ldp columns map.
     */
    protected Map<String, Boolean> ldpColumnsMap;
    /**
     * ε
     */
    protected double[] epsilons;

    public SbitmapStarter(Properties properties) {
        this.properties = properties;
    }

    public void start() throws IOException, MpcAbortException {
        String filePath = taskType
            // dataset name
            + "_" + datasetName
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
        // plain mode
        runPlainPto(printWriter);
        // Full secure
        runFullSecurePto(printWriter, SbitmapSecurityMode.ULDP, ownRpc.ownParty().getPartyId());
        // dp secure
        runDpPto(printWriter, SbitmapSecurityMode.ULDP, ownRpc.ownParty().getPartyId());
        // clean
        printWriter.close();
        fileWriter.close();
        ownRpc.disconnect();
    }

    public void init() throws IOException, URISyntaxException {
        // set rpc
        ownRpc = RpcPropertiesUtils.readNettyRpc(properties, "host", "slave");
        if (ownRpc.ownParty().getPartyId() == 0) {
            otherParty = ownRpc.getParty(1);
        } else {
            otherParty = ownRpc.getParty(0);
        }
        // set dataset name
        datasetName = SbitmapMainUtils.setDatasetName(properties);
        // set metadata
        schema = SbitmapMainUtils.setSchema(properties);
        // set dataset
        setDataSet();
        // 设置总测试轮数
        totalRound = SbitmapMainUtils.setTotalRound(properties);
        // 设置LDP列
        ldpColumnsMap = SbitmapMainUtils.setLdpColumnsMap(properties, schema);
        // 设置LDP参数
        setLdpParameters();
        taskType = SbitmapTaskType.valueOf(PropertiesUtils.readString(properties, "task_type"));

    }

    private void setDataSet() throws IOException, URISyntaxException {
        LOGGER.info("-----set whole dataset-----");
        int ncols = schema.length();
        DataFrame readTrainDataFrame = SbitmapMainUtils.setTrainDataFrame(properties, schema);
        LOGGER.info("-----set own dataframe-----");
        int[] partyColumns = PropertiesUtils.readIntArray(properties, "party_columns");
        Preconditions.checkArgument(partyColumns.length == ncols, "# of party_column must match column_num");
        Arrays.stream(partyColumns).forEach(partyId ->
            Preconditions.checkArgument(
                partyId == 0 || partyId == 1,
                "Invalid party_column: %s, party_colum must be 0 or 1", partyId)
        );
        int[] ownColumns = IntStream.range(0, ncols)
            .filter(columnIndex -> partyColumns[columnIndex] == ownRpc.ownParty().getPartyId())
            .toArray();
        Preconditions.checkArgument(
            ownColumns.length > 0,
            "At least one column should belongs to party_id = %s", ownRpc.ownParty().getPartyId()
        );
        LOGGER.info("own_columns = {}", Arrays.toString(ownColumns));
        ownDataFrame = readTrainDataFrame.select(ownColumns);
        ownSchema = ownDataFrame.schema();
        // 挑选列后，数据列会发生变化，因此也需要调整输入列
        wholeDataFrame = readTrainDataFrame.select(ownColumns).merge(readTrainDataFrame.drop(ownColumns));
    }

    protected void setLdpParameters() {
        LOGGER.info("-----set LDP parameters-----");
        // set ε
        epsilons = SbitmapMainUtils.setEpsilons(properties);
    }

    /**
     * Create ldp configs.
     *
     * @param ldpType LDP type.
     * @param epsilon ε.
     * @return ldp configs.
     */
    protected Map<String, LdpConfig> createLdpConfigs(SbitmapSecurityMode ldpType, double epsilon) {
        return SbitmapMainUtils.createLdpConfigs(ownDataFrame, ldpColumnsMap, ldpType, epsilon);
    }

    protected void writeInfo(PrintWriter printWriter,
                             String name, Double epsilon, Double time,
                             Double performanceMeasure,
                             long packetNum, long payloadByteLength, long sendByteLength) {
        String information = name
            // ε
            + "\t" + (Objects.isNull(epsilon) ? "N/A" : epsilon)
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
     * Run plain protocol
     *
     * @param printWriter printWriter
     */
    protected void runPlainPto(PrintWriter printWriter) {
        // empty
    }


    /**
     * Run full secure protocol
     *
     * @param printWriter print writer.
     * @param ldpType     ldp type.
     * @param partyId     party id.
     * @throws MpcAbortException the protocol failure aborts.
     */
    protected void runFullSecurePto(PrintWriter printWriter, SbitmapSecurityMode ldpType, int partyId)
        throws MpcAbortException {
        LOGGER.info("-----Pto {} LDP training for {}-----", ldpType.name(), taskType);

        SbitmapConfig sbitmapConfig = new SbitmapConfig.Builder(ownSchema)
            .build();
        SbitmapPtoRunner ptoRunner = createRunner(sbitmapConfig, partyId);
        ptoRunner.init();
        ptoRunner.run();
        ptoRunner.stop();
        writeInfo(printWriter, ldpType.name(), null, ptoRunner.getTime(),
            null,
            ptoRunner.getPacketNum(), ptoRunner.getPayloadByteLength(), ptoRunner.getSendByteLength()
        );
    }

    /**
     * Run dp protocol
     *
     * @param printWriter print writer.
     * @param ldpType     ldp type.
     * @param partyId     party id.
     * @throws MpcAbortException the protocol failure aborts.
     */
    protected void runDpPto(PrintWriter printWriter, SbitmapSecurityMode ldpType, int partyId)
        throws MpcAbortException {
        LOGGER.info("-----Pto {} LDP training for {}-----", ldpType.name(), taskType);
        for (double epsilon : epsilons) {
            Map<String, LdpConfig> ldpConfigs = createLdpConfigs(ldpType, epsilon);
            SbitmapConfig slaveConfig = new SbitmapConfig.Builder(ownSchema)
                .addLdpConfig(ldpConfigs)
                .build();
            SbitmapPtoRunner ptoRunner = createRunner(slaveConfig, partyId);
            ptoRunner.init();
            ptoRunner.run();
            ptoRunner.stop();
            writeInfo(printWriter, ldpType.name(), epsilon, ptoRunner.getTime(),
                null,
                ptoRunner.getPacketNum(), ptoRunner.getPayloadByteLength(), ptoRunner.getSendByteLength()
            );
        }
    }

    SbitmapPtoRunner createRunner(SbitmapConfig sbitmapConfig, int partyId) {
        SbitmapPtoParty party;
        switch (partyId) {
            case 0:
                party = createReceiver(sbitmapConfig);
                break;
            case 1:
                party = createSender(sbitmapConfig);
                break;
            default:
                throw new IllegalArgumentException("Invalid task_type: " + taskType);
        }
        return new SbitmapPtoRunner(party, sbitmapConfig, totalRound, ownDataFrame);
    }

    SbitmapPtoParty createSender(SbitmapConfig sbitmapConfig) {
        SbitmapPtoParty pto;
        switch (taskType) {
            case SET_OPERATIONS:
                pto = new SetOperationsSender(ownRpc, otherParty, sbitmapConfig);
                break;
            case GROUP_AGGREGATIONS:
                pto = new GroupAggregationsSender(ownRpc, otherParty, sbitmapConfig);
                break;
            default:
                throw new IllegalArgumentException("Invalid task_type: " + taskType);
        }
        return pto;
    }

    SbitmapPtoParty createReceiver(SbitmapConfig slaveConfig) {
        SbitmapPtoParty pto;
        switch (taskType) {
            case SET_OPERATIONS:
                pto = new SetOperationsReceiver(ownRpc, otherParty, slaveConfig);
                break;
            case GROUP_AGGREGATIONS:
                pto = new GroupAggregationsReceiver(ownRpc, otherParty, slaveConfig);
                break;
            default:
                throw new IllegalArgumentException("Invalid task_type: " + taskType);
        }
        return pto;
    }
}