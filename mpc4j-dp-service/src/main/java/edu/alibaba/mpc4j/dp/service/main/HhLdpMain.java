package edu.alibaba.mpc4j.dp.service.main;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.metrics.HeavyHitterMetrics;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpClient;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpServer;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpFactory;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpFactory.HhLdpType;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.BasicHhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HgHhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HhgHhLdpConfig;
import edu.alibaba.mpc4j.dp.service.structure.HeavyGuardian;
import edu.alibaba.mpc4j.dp.service.structure.NaiveStreamCounter;
import edu.alibaba.mpc4j.dp.service.tool.StreamDataUtils;
import org.openjdk.jol.info.GraphLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
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
     * 任务类型名称
     */
    static final String TASK_TYPE_NAME = "LDP_HEAVY_HITTER";
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
     * correct count map
     */
    private final Map<String, Integer> correctCountMap;
    /**
     * correct heavy hitter
     */
    private final List<String> correctHeavyHitters;

    public HhLdpMain(Properties properties) throws IOException {
        reportFilePostfix = PropertiesUtils.readString(properties, "report_file_postfix");
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
        Preconditions.checkArgument(
            domainMinValue < domainMaxValue,
            "domain_min_value (%s) must be less than domain_max_value (%s)",
            domainMinValue, domainMaxValue
        );
        LOGGER.info("Domain Range: [{}, {}]", domainMinValue, domainMaxValue);
        domainSet = IntStream.rangeClosed(domainMinValue, domainMaxValue)
            .mapToObj(String::valueOf).collect(Collectors.toSet());
        int d = domainSet.size();
        // set heavy hitter
        k = PropertiesUtils.readInt(properties, "k");
        Preconditions.checkArgument(k <= d, "k must be less than or equal to %s: %s", d, k);
        // set privacy parameters
        double warmupPercentage = PropertiesUtils.readDouble(properties, "warmup_percentage");
        Preconditions.checkArgument(
            warmupPercentage > 0 && warmupPercentage < 1,
            "warmup_percentage must be in range (0, 1): %s", warmupPercentage
        );
        windowEpsilons = PropertiesUtils.readDoubleArray(properties, "window_epsilon");
        alphas = PropertiesUtils.readDoubleArray(properties, "alpha");
        // set test round
        testRound = PropertiesUtils.readInt(properties, "test_round");
        // num and warmup num
        Stream<String> dataStream = StreamDataUtils.obtainItemStream(datasetPath);
        int num = (int) dataStream.count();
        dataStream.close();
        warmupNum = (int) Math.round(num * warmupPercentage);
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

    public void run() throws IOException {
        // create report file
        LOGGER.info("Create report file");
        String filePath = TASK_TYPE_NAME + "_" + datasetName + "_" + testRound + "_" + reportFilePostfix + ".txt";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // write tab
        String tab = "type\tε_w\tα\tNDCG\tPrecision\tABE\tRE\tMemory(B)";
        printWriter.println(tab);
        runHeavyGuardian(printWriter);
        for (double windowEpsilon : windowEpsilons) {
            runNaiveHeavyHitter(windowEpsilon, printWriter);
        }
        for (double windowEpsilon : windowEpsilons) {
            runBasicHgHeavyHitter(windowEpsilon, printWriter);
        }
        for (double alpha : alphas) {
            for (double windowEpsilon : windowEpsilons) {
                runAdvHhgHeavyHitter(windowEpsilon, alpha, printWriter);
            }
        }
        for (double windowEpsilon : windowEpsilons) {
            runRelaxHhgHeavyHitter(windowEpsilon, printWriter);
        }
        printWriter.close();
        fileWriter.close();
    }

    private void runHeavyGuardian(PrintWriter printWriter) throws IOException {
        String typeName = " PURE_HG";
        LOGGER.info("Run {}", typeName);
        double ndcg = 0.0;
        double precision = 0.0;
        double abe = 0.0;
        double re = 0.0;
        double memory = 0.0;
        for (int round = 0; round < testRound; round++) {
            HeavyGuardian heavyGuardian = new HeavyGuardian(1, k, 0);
            Stream<String> dataStream = StreamDataUtils.obtainItemStream(datasetPath);
            dataStream.forEach(heavyGuardian::insert);
            dataStream.close();
            // heavy hitter map
            Map<String, Double> heavyHitterMap = heavyGuardian.getRecordItemSet().stream()
                .collect(Collectors.toMap(item -> item, item -> (double) heavyGuardian.query(item)));
            Preconditions.checkArgument(
                heavyHitterMap.size() == k,
                "heavy hitter size must be equal to %s: %s", k, heavyHitterMap.size()
            );
            // heavy hitter ordered list
            List<Map.Entry<String, Double>> heavyHitterOrderedList = new ArrayList<>(heavyHitterMap.entrySet());
            heavyHitterOrderedList.sort(Comparator.comparingDouble(Map.Entry::getValue));
            Collections.reverse(heavyHitterOrderedList);
            // heavy hitters
            List<String> heavyHitters = heavyHitterOrderedList.stream().map(Map.Entry::getKey).collect(Collectors.toList());
            // metrics
            ndcg += HeavyHitterMetrics.ndcg(heavyHitters, correctHeavyHitters);
            precision += HeavyHitterMetrics.precision(heavyHitters, correctHeavyHitters);
            abe += HeavyHitterMetrics.absoluteError(heavyHitterMap, correctCountMap);
            re += HeavyHitterMetrics.relativeError(heavyHitterMap, correctCountMap);
            memory += GraphLayout.parseInstance(heavyGuardian).totalSize();
        }
        ndcg = ndcg / testRound;
        precision = precision / testRound;
        abe = abe / testRound;
        re = re / testRound;
        memory = memory / testRound;
        // output report
        printInfo(printWriter, typeName, null, null, ndcg, precision, abe, re, memory);
    }

    private void runNaiveHeavyHitter(double windowEpsilon, PrintWriter printWriter) throws IOException {
        HhLdpType type = HhLdpType.DE_FO;
        LOGGER.info("Run {}, ε_w = {}", type.name(), windowEpsilon);
        double ndcg = 0.0;
        double precision = 0.0;
        double abe = 0.0;
        double re = 0.0;
        double memory = 0.0;
        for (int round = 0; round < testRound; round++) {
            HhLdpConfig config = new BasicHhLdpConfig
                .Builder(type, domainSet, k, windowEpsilon)
                .build();
            HhLdpServer server = HhLdpFactory.createServer(config);
            HhLdpClient client = HhLdpFactory.createClient(config);
            double[] metrics = runLdpHeavyHitter(server, client);
            ndcg += metrics[0];
            precision += metrics[1];
            abe += metrics[2];
            re += metrics[3];
            memory += metrics[4];
        }
        ndcg = ndcg / testRound;
        precision = precision / testRound;
        abe = abe / testRound;
        re = re / testRound;
        memory = memory / testRound;
        printInfo(printWriter, type.name(), windowEpsilon, null, ndcg, precision, abe, re, memory);
    }

    private void runBasicHgHeavyHitter(double windowEpsilon, PrintWriter printWriter) throws IOException {
        HhLdpType type = HhLdpType.BASIC_HG;
        LOGGER.info("Run {}, ε_w = {}", type.name(), windowEpsilon);
        double ndcg = 0.0;
        double precision = 0.0;
        double abe = 0.0;
        double re = 0.0;
        double memory = 0.0;
        for (int round = 0; round < testRound; round++) {
            HgHhLdpConfig config = new HgHhLdpConfig
                .Builder(type, domainSet, k, windowEpsilon)
                .build();
            HhLdpServer server = HhLdpFactory.createHgServer(config);
            HhLdpClient client = HhLdpFactory.createHgClient(config);
            double[] metrics = runLdpHeavyHitter(server, client);
            ndcg += metrics[0];
            precision += metrics[1];
            abe += metrics[2];
            re += metrics[3];
            memory += metrics[4];
        }
        ndcg = ndcg / testRound;
        precision = precision / testRound;
        abe = abe / testRound;
        re = re / testRound;
        memory = memory / testRound;
        printInfo(printWriter, type.name(), windowEpsilon, null, ndcg, precision, abe, re, memory);
    }

    private void runAdvHhgHeavyHitter(double windowEpsilon, double alpha, PrintWriter printWriter) throws IOException {
        HhLdpType type = HhLdpType.ADVAN_HG;
        LOGGER.info("Run {}, ε_w = {}, α = {}", type.name(), windowEpsilon, alpha);
        double ndcg = 0.0;
        double precision = 0.0;
        double abe = 0.0;
        double re = 0.0;
        double memory = 0.0;
        for (int round = 0; round < testRound; round++) {
            HhgHhLdpConfig config = new HhgHhLdpConfig
                .Builder(type, domainSet, k, windowEpsilon)
                .setAlpha(alpha)
                .build();
            HhLdpServer server = HhLdpFactory.createHhgServer(config);
            HhLdpClient client = HhLdpFactory.createHhgClient(config);
            double[] metrics = runLdpHeavyHitter(server, client);
            ndcg += metrics[0];
            precision += metrics[1];
            abe += metrics[2];
            re += metrics[3];
            memory += metrics[4];
        }
        ndcg = ndcg / testRound;
        precision = precision / testRound;
        abe = abe / testRound;
        re = re / testRound;
        memory = memory / testRound;
        printInfo(printWriter, type.name(), windowEpsilon, alpha, ndcg, precision, abe, re, memory);
    }

    private void runRelaxHhgHeavyHitter(double windowEpsilon, PrintWriter printWriter) throws IOException {
        HhLdpType type = HhLdpType.RELAX_HG;
        LOGGER.info("Run {}, ε_w = {}", type.name(), windowEpsilon);
        double ndcg = 0.0;
        double precision = 0.0;
        double abe = 0.0;
        double re = 0.0;
        double memory = 0.0;
        for (int round = 0; round < testRound; round++) {
            HhgHhLdpConfig config = new HhgHhLdpConfig
                .Builder(type, domainSet, k, windowEpsilon)
                .build();
            HhLdpServer server = HhLdpFactory.createHhgServer(config);
            HhLdpClient client = HhLdpFactory.createHhgClient(config);
            double[] metrics = runLdpHeavyHitter(server, client);
            ndcg += metrics[0];
            precision += metrics[1];
            abe += metrics[2];
            re += metrics[3];
            memory += metrics[4];
        }
        ndcg = ndcg / testRound;
        precision = precision / testRound;
        abe = abe / testRound;
        re = re / testRound;
        memory = memory / testRound;
        printInfo(printWriter, type.name(), windowEpsilon, null, ndcg, precision, abe, re, memory);
    }

    private double[] runLdpHeavyHitter(HhLdpServer server, HhLdpClient client) throws IOException {
        // warmup
        AtomicInteger warmupIndex = new AtomicInteger();
        Stream<String> dataStream = StreamDataUtils.obtainItemStream(datasetPath);
        dataStream.filter(item -> warmupIndex.getAndIncrement() <= warmupNum)
            .map(client::warmup)
            .forEach(server::warmupInsert);
        dataStream.close();
        server.stopWarmup();
        // randomize
        AtomicInteger randomizedIndex = new AtomicInteger();
        dataStream = StreamDataUtils.obtainItemStream(datasetPath);
        dataStream.filter(item -> randomizedIndex.getAndIncrement() > warmupNum)
            .map(item -> client.randomize(server.getServerContext(), item))
            .forEach(server::randomizeInsert);
        dataStream.close();
        // heavy hitter map
        Map<String, Double> heavyHitterMap = server.responseHeavyHitters();
        Preconditions.checkArgument(heavyHitterMap.size() == k);
        // heavy hitter ordered list
        List<Map.Entry<String, Double>> heavyHitterOrderedList = new ArrayList<>(heavyHitterMap.entrySet());
        heavyHitterOrderedList.sort(Comparator.comparingDouble(Map.Entry::getValue));
        Collections.reverse(heavyHitterOrderedList);
        // heavy hitters
        List<String> heavyHitters = heavyHitterOrderedList.stream().map(Map.Entry::getKey).collect(Collectors.toList());
        // metrics
        double[] metrics = new double[5];
        metrics[0] = HeavyHitterMetrics.ndcg(heavyHitters, correctHeavyHitters);
        metrics[1] = HeavyHitterMetrics.precision(heavyHitters, correctHeavyHitters);
        metrics[2] = HeavyHitterMetrics.absoluteError(heavyHitterMap, correctCountMap);
        metrics[3] = HeavyHitterMetrics.relativeError(heavyHitterMap, correctCountMap);
        metrics[4] = GraphLayout.parseInstance(server).totalSize();
        return metrics;
    }

    private void printInfo(PrintWriter printWriter, String type, Double windowEpsilon, Double alpha,
                           double ndcg, double precision, double abe, double re, double memory) {
        String windowEpsilonString = windowEpsilon == null ? "-" : String.valueOf(windowEpsilon);
        String alphaString = alpha == null ? "-" : String.valueOf(alpha);
        double roundNdcg = (double) Math.round(ndcg * 10000) / 10000;
        double roundPrecision = (double) Math.round(precision * 10000) / 10000;
        double roundAbe = (double) Math.round(abe * 10000) / 10000;
        double roundRe = (double) Math.round(re * 10000) / 10000;
        double roundMemory = (double) Math.round(memory * 10000) / 10000;
        LOGGER.info(
            "NDCG = {}, Precision = {}, ABE = {}, RE = {}, Memory = {}",
            roundNdcg, roundPrecision, roundAbe, roundRe, roundMemory
        );
        printWriter.println(type + "\t" + windowEpsilonString + "\t" + alphaString + "\t"
            + roundNdcg + "\t" + roundPrecision + "\t" + roundAbe + "\t" + roundRe + "\t" + roundMemory);
    }
}
