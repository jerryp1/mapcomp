package edu.alibaba.mpc4j.s2pc.sbitmap.utils;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.dp.ldp.LdpConfig;
import edu.alibaba.mpc4j.dp.ldp.nominal.encode.DirectEncodeLdpConfig;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggFactory.GroupAggTypes;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggFactory.PrefixAggTypes;
import edu.alibaba.mpc4j.s2pc.sbitmap.main.SbitmapConfig;
import edu.alibaba.mpc4j.s2pc.sbitmap.main.SbitmapSecurityMode;
import edu.alibaba.mpc4j.s2pc.sbitmap.main.SbitmapTaskType;
import edu.alibaba.mpc4j.s2pc.sbitmap.pto.*;
import org.apache.commons.csv.CSVFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smile.data.DataFrame;
import smile.data.measure.NominalScale;
import smile.data.type.DataTypes;
import smile.data.type.StructField;
import smile.data.type.StructType;
import smile.data.vector.IntVector;
import smile.io.Read;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Sbitmap utilities for main.
 *
 * @author Li Peng
 * @date 2023/8/3
 */
public class SbitmapMainUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(SbitmapMainUtils.class);

    public static final String ID = "id";
    public static final String PID = "pid";

    /**
     * Private constructor.
     */
    private SbitmapMainUtils() {
        // empty
    }

    /**
     * default csv.
     */
    public static final CSVFormat DEFAULT_CSV_FORMAT = CSVFormat.Builder.create()
        .setHeader()
        .setIgnoreHeaderCase(true)
        .build();

    /**
     * set dataset name.
     *
     * @param properties configurations.
     * @return dataset name.
     */
    public static String setDatasetName(Properties properties) {
        LOGGER.info("-----set dataset name-----");
        return PropertiesUtils.readString(properties, "dataset_name");
    }

    /**
     * 设置总测试轮数。
     *
     * @param properties 配置项。
     * @return 总测试轮数。
     */
    public static int setTotalRound(Properties properties) {
        int totalRound = PropertiesUtils.readInt(properties, "total_round");
        Preconditions.checkArgument(totalRound >= 1, "round must be greater than or equal to 1");
        return totalRound;
    }

    public static PrefixAggTypes setPrefixAggTypes(Properties properties) {
        String prefixAggType = PropertiesUtils.readString(properties, "prefix_agg_type");
        return PrefixAggTypes.valueOf(prefixAggType.toUpperCase());
    }

    public static boolean setSilent(Properties properties) {
        return PropertiesUtils.readBoolean(properties, "silent");
    }

    public static Zl setZl(Properties properties) {
        int l = PropertiesUtils.readInt(properties, "max_l");
        return ZlFactory.createInstance(EnvType.STANDARD, l);
    }

    public static String setOutputDir(Properties properties) {
        return  PropertiesUtils.readString(properties, "output_dir");
    }

    public static String setInputDir(Properties properties) {
        return  PropertiesUtils.readString(properties, "input_dir");
    }

    public static GroupAggTypes setGroupAggTypes(Properties properties) {
        String groupAggTypes = PropertiesUtils.readString(properties, "group_agg_type");
        return GroupAggTypes.valueOf(groupAggTypes.toUpperCase());
    }

    /**
     * 设置分组长度。
     *
     * @param properties 配置项。
     * @return α
     */
    public static int setSenderGroupBitLength(Properties properties) {
        return PropertiesUtils.readInt(properties, "sender_group_bit_length");
    }

    public static String getCurrentTime() {
        return new SimpleDateFormat("MM-dd*HH:mm:ss").format(Calendar.getInstance().getTime());

    }

    /**
     * 设置分组长度。
     *
     * @param properties 配置项。
     * @return α
     */
    public static int setReceiverGroupBitLength(Properties properties) {
        return PropertiesUtils.readInt(properties, "receiver_group_bit_length");
    }

    /**
     * 设置分组长度。
     *
     * @param properties 配置项。
     * @return α
     */
    public static int[] setTestDataNums(Properties properties) {
        return PropertiesUtils.readIntArray(properties, "test_data_nums");
    }

    /**
     * 设置训练数据集。
     *
     * @param schema     元数据信息。
     * @return 训练数据集。
     * @throws IOException        如果出现IO异常。
     * @throws URISyntaxException 如果文件路径有误。
     */
    public static DataFrame setDataFrame(StructType schema, String path) throws IOException, URISyntaxException {
        if (!new File(path).exists()) {
            LOGGER.info("Dataset file not exist, please generate data first.");
        }
        return Read.csv(path, DEFAULT_CSV_FORMAT, schema);
    }

    /**
     * Add id column.
     *
     * @param dataFrame dataframe.
     * @return updated dataframe.
     */
    public static DataFrame addIdColumn(DataFrame dataFrame) {
        int size = dataFrame.size();
        int[] ids = IntStream.range(0, size).toArray();
        IntVector intVector = IntVector.of(ID, ids);
        return dataFrame.merge(intVector);
    }

    /**
     * Select rows based on the party id.
     *
     * @return updated dataframe.
     */
    public static int[] selectRows(int rowNum, int partyId) {
        assert partyId == 0 || partyId == 1 : "party id must be 0 or 1";
        int num = (int) (rowNum * 0.6);
        return partyId == 0 ? IntStream.range(0, num - 1).toArray() : IntStream.range(rowNum - num + 1, rowNum).toArray();
    }

    public static DataFrame setDataset(DataFrame dataFrame, int[] columns, int[] rows) {
        DataFrame temp = SbitmapMainUtils.addIdColumn(dataFrame.select(columns));
        return DataFrame.of(Arrays.stream(rows).mapToObj(temp::get).collect(Collectors.toList()));
    }


    public static SbitmapPtoParty createParty(SbitmapTaskType taskType, Rpc ownRpc, Party otherParty, SbitmapConfig sbitmapConfig) {
        SbitmapPtoParty party;
        switch (ownRpc.ownParty().getPartyId()) {
            case 0:
                party = createReceiver(taskType, ownRpc, otherParty, sbitmapConfig);
                break;
            case 1:
                party = createSender(taskType, ownRpc, otherParty, sbitmapConfig);
                break;
            default:
                throw new IllegalArgumentException("Invalid task_type: " + taskType);
        }
        return party;
    }

    public static SbitmapPtoParty createSender(SbitmapTaskType taskType, Rpc ownRpc, Party otherParty, SbitmapConfig sbitmapConfig) {
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

    public static SbitmapPtoParty createReceiver(SbitmapTaskType taskType, Rpc ownRpc, Party otherParty, SbitmapConfig slaveConfig) {
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