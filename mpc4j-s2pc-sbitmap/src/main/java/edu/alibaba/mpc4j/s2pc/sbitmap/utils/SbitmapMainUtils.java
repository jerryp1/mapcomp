package edu.alibaba.mpc4j.s2pc.sbitmap.utils;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.dp.ldp.LdpConfig;
import edu.alibaba.mpc4j.dp.ldp.nominal.encode.DirectEncodeLdpConfig;
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
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
     * set metadata
     *
     * @param properties configurations.
     * @return metadata
     */
    public static StructType setSchema(Properties properties) {
        LOGGER.info("-----set whole schema-----");
        String[] columnTypes = PropertiesUtils.readTrimStringArray(properties, "column_types");
        String[] columnNames = PropertiesUtils.readTrimStringArray(properties, "column_names");
        Preconditions.checkArgument(
            Arrays.stream(columnNames).collect(Collectors.toSet()).size() == columnNames.length,
            "column_names contains duplicated names"
        );
        Preconditions.checkArgument(
            columnTypes.length == columnNames.length,
            "# of column type = %s, # of column name = %s, must be the same",
            columnTypes.length, columnNames.length
        );
        int ncols = columnTypes.length;
        StructField[] structFields = IntStream.range(0, ncols)
            .mapToObj(columnIndex -> {
                String columnName = columnNames[columnIndex];
                String columnType = columnTypes[columnIndex];
                switch (columnType) {
                    case "N":
                        // nominal，枚举类，必须为one-hot格式
                        return new StructField(columnName, DataTypes.ByteType, new NominalScale("0", "1"));
                    case "I":
                        // int，整数类
                        return new StructField(columnName, DataTypes.IntegerType);
                    case "F":
                        // float，浮点数类
                        return new StructField(columnName, DataTypes.FloatType);
                    case "D":
                        // double，双精度浮点数类
                        return new StructField(columnName, DataTypes.DoubleType);
                    case "C":
                        // 分类任务的标签类型
                        String[] classTypes = PropertiesUtils.readTrimStringArray(properties, "class_types");
                        return new StructField(columnName, DataTypes.ByteType, new NominalScale(classTypes));
                    default:
                        throw new IllegalArgumentException("Invalid columnType: " + columnType);
                }
            })
            .toArray(StructField[]::new);
        StructType schema = DataTypes.struct(structFields);
        return schema;
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

    /**
     * 设置LDP列映射。
     *
     * @param properties 配置项。
     * @param schema     元数据信息。
     * @return LDP列映射。
     */
    public static Map<String, Boolean> setLdpColumnsMap(Properties properties, StructType schema) {
        LOGGER.info("-----set LDP columns-----");
        int ncols = schema.length();
        int[] dpColumns = PropertiesUtils.readIntArray(properties, "ldp_columns");
        Preconditions.checkArgument(dpColumns.length == ncols, "# ldp_column must match column_num");
        Arrays.stream(dpColumns).forEach(value ->
            Preconditions.checkArgument(
                value == 0 || value == 1,
                "Invalid ldp_column: %s, only support 0 or 1", value)
        );

        return IntStream.range(0, ncols)
            .boxed()
            .collect(Collectors.toMap(
                schema::fieldName,
                columnIndex -> (dpColumns[columnIndex] == 1)
            ));
    }

    /**
     * 设置ε。
     *
     * @param properties 配置项。
     * @return ε。
     */
    public static double[] setEpsilons(Properties properties) {
        return PropertiesUtils.readDoubleArray(properties, "epsilon");
    }

    /**
     * 设置θ。
     *
     * @param properties 配置项。
     * @return θ。
     */
    public static int[] setThetas(Properties properties) {
        return PropertiesUtils.readIntArray(properties, "theta");
    }

    /**
     * 设置α。
     *
     * @param properties 配置项。
     * @return α
     */
    public static double[] setAlphas(Properties properties) {
        return PropertiesUtils.readDoubleArray(properties, "alpha");
    }

    /**
     * 设置训练数据集。
     *
     * @param properties 配置项。
     * @param schema     元数据信息。
     * @return 训练数据集。
     * @throws IOException        如果出现IO异常。
     * @throws URISyntaxException 如果文件路径有误。
     */
    public static DataFrame setDataFrame(Properties properties, StructType schema) throws IOException, URISyntaxException {
        String trainDatasetPath = PropertiesUtils.readString(properties, "train_dataset_path");
        return Read.csv(trainDatasetPath, DEFAULT_CSV_FORMAT, schema);
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

    /**
     * 读取上下界。
     *
     * @param dataFrame   数据帧。
     * @param structField 列信息。
     * @return [下界, 上界]。
     */
    private static int[] readIntBounds(DataFrame dataFrame, StructField structField) {
        int[] dataArray = dataFrame.column(structField.name).toIntArray();
        int[] bounds = new int[2];
        bounds[0] = Arrays.stream(dataArray).min().orElse(0);
        bounds[1] = Arrays.stream(dataArray).max().orElse(0);
        Preconditions.checkArgument(
            bounds[0] < bounds[1],
            "column %s: lowerBound (%s) must be less than upperBound (%s)",
            structField.name, bounds[0], bounds[1]
        );
        return bounds;
    }

    /**
     * 读取上下界。
     *
     * @param dataFrame   数据帧。
     * @param structField 列信息。
     * @return [下界, 上界]。
     */
    private static double[] readDoubleBounds(DataFrame dataFrame, StructField structField) {
        double[] dataArray = dataFrame.column(structField.name).toDoubleArray();
        double[] bounds = new double[2];
        bounds[0] = Arrays.stream(dataArray).min().orElse(0);
        bounds[1] = Arrays.stream(dataArray).max().orElse(0);
        Preconditions.checkArgument(
            bounds[0] < bounds[1],
            "column %s: lowerBound (%s) must be less than upperBound (%s)",
            structField.name, bounds[0], bounds[1]
        );
        return bounds;
    }

    /**
     * 创建LDP配置项。
     *
     * @param dataFrame     数据帧。
     * @param ldpColumnsMap LDP列映射。
     * @param ldpType       LDP类型。
     * @param epsilon       ε。
     * @return LDP配置项。
     */
    public static Map<String, LdpConfig> createLdpConfigs(DataFrame dataFrame, Map<String, Boolean> ldpColumnsMap,
                                                          SbitmapSecurityMode ldpType, double epsilon) {
        StructType schema = dataFrame.schema();
        Map<String, LdpConfig> ldpConfigMap = new HashMap<>(schema.length());
        for (StructField structField : schema.fields()) {
            boolean dp = ldpColumnsMap.get(structField.name);
            if (dp) {
                LdpConfig ldpConfig;
                if (structField.measure instanceof NominalScale) {
                    NominalScale nominalScale = (NominalScale) structField.measure;
                    ldpConfig = new DirectEncodeLdpConfig
                        .Builder(epsilon, Arrays.stream(nominalScale.levels()).collect(Collectors.toList()))
                        .build();
                } else if (structField.type.isIntegral()) {
                    int[] bounds = readIntBounds(dataFrame, structField);
                    switch (ldpType) {
//                        case PIECEWISE:
//                            ldpConfig = new NaiveRangeIntegralLdpConfig
//                                .Builder(new PiecewiseLdpConfig.Builder(epsilon).build(), bounds[0], bounds[1])
//                                .build();
//                            break;
                        default:
                            throw new IllegalArgumentException("Invalid LdpType: " + ldpType);
                    }
                } else if (structField.type.isFloating()) {
                    // 浮点数类型，创建浮点数LDP机制
                    double[] bounds = readDoubleBounds(dataFrame, structField);
                    switch (ldpType) {
//                        case PIECEWISE:
//                            ldpConfig = new NaiveRangeRealLdpConfig
//                                .Builder(new PiecewiseLdpConfig.Builder(epsilon).build(), bounds[0], bounds[1])
//                                .build();
//                            break;
//                        case GLOBAL_MAP:
//                        case GLOBAL_EXP_MAP:
//                            ldpConfig = new GlobalMapRealLdpConfig
//                                .Builder(epsilon, bounds[0], bounds[1])
//                                .build();
//                            break;
                        default:
                            throw new IllegalArgumentException("Invalid LdpType: " + ldpType);
                    }
                } else {
                    throw new IllegalArgumentException("Invalid type: " + structField.type);
                }
                ldpConfigMap.put(structField.name, ldpConfig);
            }
        }
        return ldpConfigMap;
    }

    /**
     * 创建LDP配置项。
     *
     * @param dataFrame     数据帧。
     * @param ldpColumnsMap LDP列映射。
     * @param ldpType       LDP类型。
     * @param epsilon       ε。
     * @param theta         θ。
     * @return LDP配置项。
     */
    public static Map<String, LdpConfig> createLdpConfigs(DataFrame dataFrame, Map<String, Boolean> ldpColumnsMap,
                                                          SbitmapSecurityMode ldpType, double epsilon, int theta) {
        StructType schema = dataFrame.schema();
        Map<String, LdpConfig> ldpConfigMap = new HashMap<>(schema.length());
        for (StructField structField : schema.fields()) {
            boolean dp = ldpColumnsMap.get(structField.name);
            if (dp) {
                LdpConfig ldpConfig;
                if (structField.measure instanceof NominalScale) {
                    NominalScale nominalScale = (NominalScale) structField.measure;
                    ldpConfig = new DirectEncodeLdpConfig
                        .Builder(epsilon, Arrays.stream(nominalScale.levels()).collect(Collectors.toList()))
                        .build();
                } else if (structField.type.isIntegral()) {
                    int[] bounds = readIntBounds(dataFrame, structField);
                    switch (ldpType) {
//                        case LOCAL_MAP:
//                            ldpConfig = new LocalMapIntegralLdpConfig
//                                .Builder(epsilon, theta, bounds[0], bounds[1])
//                                .build();
//                            break;
                        default:
                            throw new IllegalArgumentException("Invalid LdpType: " + ldpType);
                    }
                } else if (structField.type.isFloating()) {
                    double[] bounds = readDoubleBounds(dataFrame, structField);
                    switch (ldpType) {
//                        case LOCAL_MAP:
//                        case LOCAL_EXP_MAP:
//                            ldpConfig = new LocalMapRealLdpConfig
//                                .Builder(epsilon, theta, bounds[0], bounds[1])
//                                .build();
//                            break;
                        default:
                            throw new IllegalArgumentException("Invalid LdpType: " + ldpType);
                    }
                } else {
                    throw new IllegalArgumentException("Invalid type: " + structField.type);
                }
                ldpConfigMap.put(structField.name, ldpConfig);
            }
        }
        return ldpConfigMap;
    }

    /**
     * 创建调整映射LDP配置项。
     *
     * @param dataFrame     数据帧。
     * @param ldpColumnsMap LDP列映射。
     * @param ldpType       LDP类型。
     * @param epsilon       ε。
     * @param theta         θ。
     * @param alpha         α。
     * @return LDP配置项。
     */
    public static Map<String, LdpConfig> createLdpConfigs(DataFrame dataFrame, Map<String, Boolean> ldpColumnsMap,
                                                          SbitmapSecurityMode ldpType, double epsilon, int theta, double alpha) {
        StructType schema = dataFrame.schema();
        Map<String, LdpConfig> ldpConfigMap = new HashMap<>(schema.length());
        for (StructField structField : schema.fields()) {
            boolean dp = ldpColumnsMap.get(structField.name);
            if (dp) {
                LdpConfig ldpConfig;
                if (structField.measure instanceof NominalScale) {
                    NominalScale nominalScale = (NominalScale) structField.measure;
                    ldpConfig = new DirectEncodeLdpConfig
                        .Builder(epsilon, Arrays.stream(nominalScale.levels()).collect(Collectors.toList()))
                        .build();
                } else if (structField.type.isIntegral()) {
                    int[] bounds = readIntBounds(dataFrame, structField);
                    switch (ldpType) {
//                        case ADJ_MAP:
//                            ldpConfig = new AdjMapIntegralLdpConfig
//                                .Builder(epsilon, theta, bounds[0], bounds[1])
//                                .setAlpha(alpha)
//                                .build();
//                            break;
//                        case ADJ_EXP_MAP:
//                            ldpConfig = new AdjExpMapIntegralLdpConfig
//                                .Builder(epsilon, theta, bounds[0], bounds[1])
//                                .setAlpha(alpha)
//                                .build();
//                            break;
                        default:
                            throw new IllegalArgumentException("Invalid LdpType: " + ldpType);
                    }
                } else if (structField.type.isFloating()) {
                    double[] bounds = readDoubleBounds(dataFrame, structField);
                    switch (ldpType) {
//                        case ADJ_MAP:
//                        case ADJ_EXP_MAP:
//                            ldpConfig = new AdjMapRealLdpConfig
//                                .Builder(epsilon, theta, bounds[0], bounds[1])
//                                .setAlpha(alpha)
//                                .build();
//                            break;
                        default:
                            throw new IllegalArgumentException("Invalid LdpType: " + ldpType);
                    }
                } else {
                    throw new IllegalArgumentException("Invalid type: " + structField.type);
                }
                ldpConfigMap.put(structField.name, ldpConfig);
            }
        }
        return ldpConfigMap;
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