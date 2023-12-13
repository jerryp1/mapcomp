package edu.alibaba.mpc4j.s2pc.sbitmap.utils;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggFactory.GroupAggTypes;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggFactory.PrefixAggTypes;
import org.apache.commons.csv.CSVFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smile.data.DataFrame;
import smile.data.type.StructType;
import smile.io.Read;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;

/**
 * Sbitmap utilities for main.
 *
 * @author Li Peng
 * @date 2023/8/3
 */
public class SbitmapMainUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(SbitmapMainUtils.class);

    public static final String ID = "id";

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
     * set rounds of test.
     *
     * @param properties configuration.
     * @return rounds.
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

    public static boolean setSenderAgg(Properties properties) {
        return PropertiesUtils.readBoolean(properties, "sender_agg", false);
    }

    public static Zl setZl(Properties properties) {
        int l = PropertiesUtils.readInt(properties, "max_l");
        return ZlFactory.createInstance(EnvType.STANDARD, l);
    }

    public static String setOutputDir(Properties properties) {
        return PropertiesUtils.readString(properties, "output_dir");
    }

    public static String setInputDir(Properties properties) {
        return PropertiesUtils.readString(properties, "input_dir");
    }

    public static GroupAggTypes setGroupAggTypes(Properties properties) {
        String groupAggTypes = PropertiesUtils.readString(properties, "group_agg_type");
        return GroupAggTypes.valueOf(groupAggTypes.toUpperCase());
    }

    /**
     * set group length.
     *
     * @param properties configuration.
     * @return group length.
     */
    public static int setSenderGroupBitLength(Properties properties) {
        return PropertiesUtils.readInt(properties, "sender_group_bit_length");
    }

    public static String getCurrentTime() {
        return new SimpleDateFormat("MM-dd*HH:mm:ss").format(Calendar.getInstance().getTime());

    }

    /**
     * set group length.
     *
     * @param properties configuration.
     * @return group length.
     */
    public static int setReceiverGroupBitLength(Properties properties) {
        return PropertiesUtils.readInt(properties, "receiver_group_bit_length");
    }

    /**
     * set data.
     *
     * @param properties configuration.
     * @return data.
     */
    public static int[] setTestDataNums(Properties properties) {
        return PropertiesUtils.readIntArray(properties, "test_data_nums");
    }

    /**
     * set data.
     *
     * @param schema meta data.
     * @return data frame
     */
    public static DataFrame setDataFrame(StructType schema, String path) throws IOException, URISyntaxException {
        if (!new File(path).exists()) {
            LOGGER.info("Dataset file not exist, please generate data first.");
        }
        return Read.csv(path, DEFAULT_CSV_FORMAT, schema);
    }
}