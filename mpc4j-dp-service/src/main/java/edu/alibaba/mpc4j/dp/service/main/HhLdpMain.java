package edu.alibaba.mpc4j.dp.service.main;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.metrics.HeavyHitterMetrics;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.dp.service.fo.FoLdpFactory;
import edu.alibaba.mpc4j.dp.service.fo.FoLdpFactory.FoLdpType;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpClient;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpServer;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpFactory;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpFactory.HhLdpType;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.FoHhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HgHhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HhLdpConfig;
import edu.alibaba.mpc4j.dp.service.structure.HeavyGuardian;
import edu.alibaba.mpc4j.dp.service.structure.NaiveStreamCounter;
import edu.alibaba.mpc4j.dp.service.tool.StreamDataUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.openjdk.jol.info.GraphLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Heavy Hitter LDP main class.
 *
 * @author Weiran Liu
 * @date 2022/11/20
 */
public class HhLdpMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(HhLdpMain.class);
    /**
     * double output format
     */
    private static final DecimalFormat DOUBLE_DECIMAL_FORMAT = new DecimalFormat("0.000");
    /**
     * time output format
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.0000");
    /**
     * int format
     */
    private static final DecimalFormat INTEGER_DECIMAL_FORMAT = new DecimalFormat("0");
    /**
     * 任务类型名称
     */
    static final String TASK_TYPE_NAME = "LDP_HEAVY_HITTER";
    /**
     * server stop watch
     */
    private final StopWatch serverStopWatch;
    /**
     * client stop watch
     */
    private final StopWatch clientStopWatch;
    /**
     * report file postfix
     */
    private final String reportFilePostfix;
    /**
     * dataset name
     */
    private final String datasetName;
    /**
     * dataset path
     */
    private final String datasetPath;
    /**
     * domain set
     */
    private final Set<String> domainSet;
    /**
     * k
     */
    private final int k;
    /**
     * warmup num
     */
    private final int warmupNum;
    /**
     * ε
     */
    private final double[] windowEpsilons;
    /**
     * α
     */
    private final double[] alphas;
    /**
     * test round
     */
    private final int testRound;
    /**
     * if run plain
     */
    private final boolean plain;
    /**
     * Frequency Oracle based type
     */
    private final List<FoLdpType> foTypeList;
    /**
     * HeavyGuardian based type
     */
    private final List<HhLdpType> hgTypeList;
    /**
     * correct count map
     */
    private final Map<String, Integer> correctCountMap;
    /**
     * correct heavy hitter
     */
    private final List<String> correctHeavyHitters;

    public HhLdpMain(Properties properties) throws IOException {
        serverStopWatch = new StopWatch();
        clientStopWatch = new StopWatch();
        reportFilePostfix = PropertiesUtils.readString(properties, "report_file_postfix", "");
        datasetName = PropertiesUtils.readString(properties, "dataset_name");
        // set dataset path
        datasetPath = PropertiesUtils.readString(properties, "dataset_path");
        // set domain set
        boolean containsDomainMinValue = PropertiesUtils.containsKeyword(properties, "domain_min_item");
        boolean containsDomainMaxValue = PropertiesUtils.containsKeyword(properties, "domain_max_item");
        int domainMinValue;
        int domainMaxValue;
        if (containsDomainMinValue && containsDomainMaxValue) {
            // if both values are set
            domainMinValue = PropertiesUtils.readInt(properties, "domain_min_item");
            domainMaxValue = PropertiesUtils.readInt(properties, "domain_max_item");
        } else {
            // automatically set domain
            Stream<String> dataStream = StreamDataUtils.obtainItemStream(datasetPath);
            domainMinValue = dataStream.mapToInt(Integer::parseInt)
                .min()
                .orElse(Integer.MIN_VALUE);
            dataStream.close();
            dataStream = StreamDataUtils.obtainItemStream(datasetPath);
            domainMaxValue = dataStream.mapToInt(Integer::parseInt)
                .max()
                .orElse(Integer.MAX_VALUE);
            dataStream.close();
        }
        MathPreconditions.checkLess("domain_min_value", domainMinValue, domainMaxValue);
        LOGGER.info("Domain Range: [{}, {}]", domainMinValue, domainMaxValue);
        domainSet = IntStream.rangeClosed(domainMinValue, domainMaxValue)
            .mapToObj(String::valueOf).collect(Collectors.toSet());
        int d = domainSet.size();
        // set heavy hitter
        k = PropertiesUtils.readInt(properties, "k");
        MathPreconditions.checkLessOrEqual("k", k, d);
        // set privacy parameters
        double warmupPercentage = PropertiesUtils.readDouble(properties, "warmup_percentage");
        MathPreconditions.checkPositiveInRange("warmup_percentage", warmupPercentage, 1.0);
        windowEpsilons = PropertiesUtils.readDoubleArray(properties, "window_epsilon");
        alphas = PropertiesUtils.readDoubleArrayWithDefault(properties, "alpha");
        // set test round
        testRound = PropertiesUtils.readInt(properties, "test_round");
        // num and warmup num
        Stream<String> dataStream = StreamDataUtils.obtainItemStream(datasetPath);
        int num = (int) dataStream.count();
        dataStream.close();
        warmupNum = (int) Math.round(num * warmupPercentage);
        // set if run plain
        plain = PropertiesUtils.readBoolean(properties, "plain", false);
        // set Frequency Oracle based types
        String[] foTypeStrings = PropertiesUtils.readTrimStringArrayWithDefault(properties, "fo_types");
        foTypeList = Arrays.stream(foTypeStrings)
            .map(FoLdpType::valueOf)
            .collect(Collectors.toList());
        // set HeavyGuardian based types
        String[] hgTypeStrings = PropertiesUtils.readTrimStringArrayWithDefault(properties, "hg_types");
        hgTypeList = Arrays.stream(hgTypeStrings)
            .map(HhLdpType::valueOf)
            // ignore fo types
            .filter(type -> !type.equals(HhLdpType.FO))
            .collect(Collectors.toList());
        // correct counting result
        NaiveStreamCounter streamCounter = new NaiveStreamCounter();
        dataStream = StreamDataUtils.obtainItemStream(datasetPath);
        dataStream.forEach(streamCounter::insert);
        dataStream.close();
        correctCountMap = domainSet.stream()
            .collect(Collectors.toMap(item -> item, streamCounter::query));
        // correct heavy hitter
        List<Map.Entry<String, Integer>> correctOrderedList = new ArrayList<>(correctCountMap.entrySet());
        correctOrderedList.sort(Comparator.comparingInt(Map.Entry::getValue));
        Collections.reverse(correctOrderedList);
        correctHeavyHitters = correctOrderedList.subList(0, k).stream()
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        LOGGER.info("Correct heavy hitters: {}", correctHeavyHitters);
    }

    String getReportFilePostfix() {
        return reportFilePostfix;
    }

    boolean getPlain() {
        return plain;
    }

    List<FoLdpType> getFoLdpList() {
        return foTypeList;
    }

    List<HhLdpType> getHgTypeList() {
        return hgTypeList;
    }

    double[] getAlphas() {
        return alphas;
    }

    public void run() throws IOException {
        // create report file
        LOGGER.info("Create report file");
        String filePath = "".equals(reportFilePostfix)
            ? TASK_TYPE_NAME + "_" + datasetName + "_" + testRound + ".txt"
            : TASK_TYPE_NAME + "_" + datasetName + "_" + testRound + "_" + reportFilePostfix + ".txt";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // write tab
        String tab = "type\tε_w\tα\ts_time(s)\tc_time(s)\tcomm.(B)\tmem.(B)\tMemory(B)\tndcg\tprecision\tabe\tre";
        printWriter.println(tab);
        LOGGER.info("{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}", "                name", "         ε", "         α",
            "           s_time(s)", "           c_time(s)",
            "            comm.(B)", "             mem.(B)",
            "                ndcg", "           precision", "                 abe", "                  re"
        );
        if (plain) {
            HhLdpAggMetrics heavyGuardianAggMetrics = runHeavyGuardian();
            printInfo(printWriter, heavyGuardianAggMetrics);
        }
        for (double windowEpsilon : windowEpsilons) {
            for (FoLdpType type : foTypeList) {
                HhLdpAggMetrics foLdpAggMetrics = runFoHeavyHitter(type, windowEpsilon);
                printInfo(printWriter, foLdpAggMetrics);
            }
        }
        if (hgTypeList.contains(HhLdpType.BASIC)) {
            for (double windowEpsilon : windowEpsilons) {
                HhLdpAggMetrics basicHgLdpAggMetrics = runBasicHgHeavyHitter(windowEpsilon);
                printInfo(printWriter, basicHgLdpAggMetrics);
            }
        }
        if (hgTypeList.contains(HhLdpType.ADV)) {
            for (double alpha : alphas) {
                for (double windowEpsilon : windowEpsilons) {
                    HhLdpAggMetrics advHgLdpAggMetrics = runAdvHhgHeavyHitter(windowEpsilon, alpha);
                    printInfo(printWriter, advHgLdpAggMetrics);
                }
            }
        }
        if (hgTypeList.contains(HhLdpType.RELAX)) {
            for (double windowEpsilon : windowEpsilons) {
                HhLdpAggMetrics relaxHgLdpAggMetrics = runRelaxHhgHeavyHitter(windowEpsilon);
                printInfo(printWriter, relaxHgLdpAggMetrics);
            }
        }
        printWriter.close();
        fileWriter.close();
    }

    private HhLdpAggMetrics runHeavyGuardian() throws IOException {
        String typeName = " PURE_HG";
        HhLdpAggMetrics aggMetrics = new HhLdpAggMetrics(typeName, null, null);
        for (int round = 0; round < testRound; round++) {
            HeavyGuardian heavyGuardian = new HeavyGuardian(1, k, 0);
            Stream<String> dataStream = StreamDataUtils.obtainItemStream(datasetPath);
            serverStopWatch.start();
            serverStopWatch.suspend();
            clientStopWatch.start();
            clientStopWatch.suspend();
            long payloadBytes = dataStream
                .mapToLong(item -> {
                    clientStopWatch.resume();
                    byte[] itemBytes = item.getBytes(HhLdpFactory.DEFAULT_CHARSET);
                    clientStopWatch.suspend();
                    serverStopWatch.resume();
                    String recoverItem = new String(itemBytes, HhLdpFactory.DEFAULT_CHARSET);
                    heavyGuardian.insert(recoverItem);
                    serverStopWatch.suspend();
                    return itemBytes.length;
                })
                .sum();
            dataStream.close();
            serverStopWatch.stop();
            long serverTimeMs = serverStopWatch.getTime(TimeUnit.MILLISECONDS);
            serverStopWatch.reset();
            // client time
            clientStopWatch.stop();
            long clientTimeMs = clientStopWatch.getTime(TimeUnit.MILLISECONDS);
            clientStopWatch.reset();
            long memoryBytes = GraphLayout.parseInstance(heavyGuardian).totalSize();
            // heavy hitter map
            Map<String, Double> heavyHitterMap = heavyGuardian.getRecordItemSet().stream()
                .collect(Collectors.toMap(item -> item, item -> (double) heavyGuardian.query(item)));
            // heavy hitter ordered list
            List<Map.Entry<String, Double>> heavyHitterOrderedList = new ArrayList<>(heavyHitterMap.entrySet());
            heavyHitterOrderedList.sort(Comparator.comparingDouble(Map.Entry::getValue));
            Collections.reverse(heavyHitterOrderedList);
            // heavy hitters
            List<String> heavyHitters = heavyHitterOrderedList.stream().map(Map.Entry::getKey).collect(Collectors.toList());
            // metrics
            HhLdpMetrics metrics = new HhLdpMetrics();
            metrics.setServerTimeMs(serverTimeMs);
            metrics.setClientTimeMs(clientTimeMs);
            metrics.setPayloadBytes(payloadBytes);
            metrics.setMemoryBytes(memoryBytes);
            metrics.setNdcg(HeavyHitterMetrics.ndcg(heavyHitters, correctHeavyHitters));
            metrics.setPrecision(HeavyHitterMetrics.precision(heavyHitters, correctHeavyHitters));
            metrics.setAbe(HeavyHitterMetrics.absoluteError(heavyHitterMap, correctCountMap));
            metrics.setRe(HeavyHitterMetrics.relativeError(heavyHitterMap, correctCountMap));
            aggMetrics.addMetrics(metrics);
        }
        return aggMetrics;
    }

    private HhLdpAggMetrics runFoHeavyHitter(FoLdpType foLdpType, double windowEpsilon) throws IOException {
        HhLdpAggMetrics aggMetrics = new HhLdpAggMetrics("FO (" + foLdpType.name() + ")", windowEpsilon, null);
        for (int round = 0; round < testRound; round++) {
            FoLdpConfig foLdpConfig = FoLdpFactory.createDefaultConfig(foLdpType, domainSet, windowEpsilon);
            HhLdpConfig hhLdpConfig = new FoHhLdpConfig
                .Builder(foLdpConfig, k)
                .build();
            HhLdpServer server = HhLdpFactory.createServer(hhLdpConfig);
            HhLdpClient client = HhLdpFactory.createClient(hhLdpConfig);
            HhLdpMetrics metrics = runLdpHeavyHitter(server, client);
            aggMetrics.addMetrics(metrics);
        }
        return aggMetrics;
    }

    HhLdpAggMetrics runBasicHgHeavyHitter(double windowEpsilon) throws IOException {
        HhLdpType type = HhLdpType.BASIC;
        HhLdpAggMetrics aggMetrics = new HhLdpAggMetrics(HhLdpType.BASIC.name(), windowEpsilon, null);
        for (int round = 0; round < testRound; round++) {
            HgHhLdpConfig config = new HgHhLdpConfig
                .Builder(type, domainSet, k, windowEpsilon)
                .build();
            HhLdpServer server = HhLdpFactory.createServer(config);
            HhLdpClient client = HhLdpFactory.createClient(config);
            HhLdpMetrics metrics = runLdpHeavyHitter(server, client);
            aggMetrics.addMetrics(metrics);
        }
        return aggMetrics;
    }

    HhLdpAggMetrics runAdvHhgHeavyHitter(double windowEpsilon, double alpha) throws IOException {
        HhLdpType type = HhLdpType.ADV;
        HhLdpAggMetrics aggMetrics = new HhLdpAggMetrics(type.name(), windowEpsilon, alpha);
        for (int round = 0; round < testRound; round++) {
            HgHhLdpConfig config = new HgHhLdpConfig
                .Builder(type, domainSet, k, windowEpsilon)
                .setAlpha(alpha)
                .build();
            HhLdpServer server = HhLdpFactory.createServer(config);
            HhLdpClient client = HhLdpFactory.createClient(config);
            HhLdpMetrics metrics = runLdpHeavyHitter(server, client);
            aggMetrics.addMetrics(metrics);
        }
        return aggMetrics;
    }

    HhLdpAggMetrics runRelaxHhgHeavyHitter(double windowEpsilon) throws IOException {
        HhLdpType type = HhLdpType.RELAX;
        HhLdpAggMetrics aggMetrics = new HhLdpAggMetrics(type.name(), windowEpsilon, null);
        for (int round = 0; round < testRound; round++) {
            HgHhLdpConfig config = new HgHhLdpConfig
                .Builder(type, domainSet, k, windowEpsilon)
                .build();
            HhLdpServer server = HhLdpFactory.createServer(config);
            HhLdpClient client = HhLdpFactory.createClient(config);
            HhLdpMetrics metrics = runLdpHeavyHitter(server, client);
            aggMetrics.addMetrics(metrics);
        }
        return aggMetrics;
    }

    HhLdpMetrics runLdpHeavyHitter(HhLdpServer server, HhLdpClient client) throws IOException {
        // warmup
        AtomicInteger warmupIndex = new AtomicInteger();
        Stream<String> dataStream = StreamDataUtils.obtainItemStream(datasetPath);
        dataStream.filter(item -> warmupIndex.getAndIncrement() <= warmupNum)
            .map(client::warmup)
            .forEach(server::warmupInsert);
        dataStream.close();
        server.stopWarmup();
        // randomize
        serverStopWatch.start();
        serverStopWatch.suspend();
        clientStopWatch.start();
        clientStopWatch.suspend();
        AtomicInteger randomizedIndex = new AtomicInteger();
        dataStream = StreamDataUtils.obtainItemStream(datasetPath);
        long payloadBytes = dataStream
            .filter(item -> randomizedIndex.getAndIncrement() > warmupNum)
            .mapToLong(item -> {
                clientStopWatch.resume();
                byte[] itemBytes = client.randomize(server.getServerContext(), item);
                clientStopWatch.suspend();
                serverStopWatch.resume();
                server.randomizeInsert(itemBytes);
                serverStopWatch.suspend();
                return itemBytes.length;
            })
            .sum();
        dataStream.close();
        serverStopWatch.stop();
        long serverTimeMs = serverStopWatch.getTime(TimeUnit.MILLISECONDS);
        serverStopWatch.reset();
        // client time
        clientStopWatch.stop();
        long clientTimeMs = clientStopWatch.getTime(TimeUnit.MILLISECONDS);
        clientStopWatch.reset();
        long memoryBytes = GraphLayout.parseInstance(server).totalSize();
        // heavy hitter map
        Map<String, Double> heavyHitterMap = server.heavyHitters();
        Preconditions.checkArgument(heavyHitterMap.size() == k);
        // ordered heavy hitter
        List<String> heavyHitters = server.orderedHeavyHitters().stream()
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        // metrics
        HhLdpMetrics metrics = new HhLdpMetrics();
        metrics.setServerTimeMs(serverTimeMs);
        metrics.setClientTimeMs(clientTimeMs);
        metrics.setPayloadBytes(payloadBytes);
        metrics.setMemoryBytes(memoryBytes);
        metrics.setNdcg(HeavyHitterMetrics.ndcg(heavyHitters, correctHeavyHitters));
        metrics.setPrecision(HeavyHitterMetrics.precision(heavyHitters, correctHeavyHitters));
        metrics.setAbe(HeavyHitterMetrics.absoluteError(heavyHitterMap, correctCountMap));
        metrics.setRe(HeavyHitterMetrics.relativeError(heavyHitterMap, correctCountMap));
        return metrics;
    }

    private void printInfo(PrintWriter printWriter, HhLdpAggMetrics aggMetrics) {
        String typeString = aggMetrics.getTypeString();
        String windowEpsilonString = aggMetrics.getWindowEpsilonString();
        String alphaString = aggMetrics.getAlphaString();
        double serverTime = aggMetrics.getServerTimeSecond();
        double clientTime = aggMetrics.getClientTimeSecond();
        long payloadBytes = aggMetrics.getPayloadBytes();
        long memoryBytes = aggMetrics.getMemoryBytes();
        double ndcg = aggMetrics.getNdcg();
        double precision = aggMetrics.getPrecision();
        double abe = aggMetrics.getAbe();
        double re = aggMetrics.getRe();
        LOGGER.info("{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}",
            StringUtils.leftPad(typeString, 20),
            StringUtils.leftPad(windowEpsilonString, 10),
            StringUtils.leftPad(alphaString, 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(serverTime), 20),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(clientTime), 20),
            StringUtils.leftPad(INTEGER_DECIMAL_FORMAT.format(payloadBytes), 20),
            StringUtils.leftPad(INTEGER_DECIMAL_FORMAT.format(memoryBytes), 20),
            StringUtils.leftPad(DOUBLE_DECIMAL_FORMAT.format(ndcg), 20),
            StringUtils.leftPad(DOUBLE_DECIMAL_FORMAT.format(precision), 20),
            StringUtils.leftPad(DOUBLE_DECIMAL_FORMAT.format(abe), 20),
            StringUtils.leftPad(DOUBLE_DECIMAL_FORMAT.format(re), 20)
        );
        printWriter.println(typeString + "\t" + windowEpsilonString + "\t" + alphaString + "\t"
            + serverTime + "\t" + clientTime + "\t" + payloadBytes + "\t" + memoryBytes + "\t"
            + ndcg + "\t" + precision + "\t" + abe + "\t" + re);
    }
}
