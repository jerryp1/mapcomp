package edu.alibaba.mpc4j.dp.stream.main;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.metrics.HeavyHitterMetrics;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHeavyHitter;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHeavyHitterFactory;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHeavyHitterFactory.LdpHeavyHitterType;
import edu.alibaba.mpc4j.dp.stream.structure.HeavyGuardian;
import edu.alibaba.mpc4j.dp.stream.structure.NaiveStreamCounter;
import edu.alibaba.mpc4j.dp.stream.tool.StreamDataUtils;
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

/**
 * Heavy Hitter with Local Differential Privacy main.
 *
 * @author Weiran Liu
 * @date 2022/11/20
 */
public class LdpHeavyHitterMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(LdpHeavyHitterMain.class);
    /**
     * 任务类型名称
     */
    static final String TASK_TYPE_NAME = "LDP_HEAVY_HITTER";
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

    public LdpHeavyHitterMain(Properties properties) throws IOException {
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
            domainMinValue = StreamDataUtils.obtainItemStream(datasetPath)
                .mapToInt(Integer::parseInt)
                .min()
                .orElse(Integer.MIN_VALUE);
            domainMaxValue = StreamDataUtils.obtainItemStream(datasetPath)
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(Integer.MAX_VALUE);
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
        int num = (int)StreamDataUtils.obtainItemStream(datasetPath).count();
        warmupNum = (int)Math.round(num * warmupPercentage);
        // correct counting result
        NaiveStreamCounter streamCounter = new NaiveStreamCounter();
        StreamDataUtils.obtainItemStream(datasetPath).forEach(streamCounter::insert);
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
        String filePath = TASK_TYPE_NAME + "_" + datasetName + "_" + testRound + ".txt";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // write tab
        String tab = "type\tw_epsilon\talpha\tNDCG\tPrecision\tRE\tMemory";
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
        for (double alpha : alphas) {
            for (double windowEpsilon : windowEpsilons) {
                runRelaxHhgHeavyHitter(windowEpsilon, alpha, printWriter);
            }
        }
        printWriter.close();
        fileWriter.close();
    }

    private void runHeavyGuardian(PrintWriter printWriter) throws IOException {
        String typeName = " PURE_HG";
        LOGGER.info("Run {}", typeName);
        double ndcg = 0.0;
        double precision = 0.0;
        double re = 0.0;
        double memory = 0.0;
        for (int round = 0; round < testRound; round++) {
            HeavyGuardian streamCounter = new HeavyGuardian(1, k, 0);
            StreamDataUtils.obtainItemStream(datasetPath).forEach(streamCounter::insert);
            // heavy hitter map
            Map<String, Double> heavyHitterMap = streamCounter.getRecordItemSet().stream()
                .collect(Collectors.toMap(item -> item, item -> (double)streamCounter.query(item)));
            Preconditions.checkArgument(heavyHitterMap.size() == k);
            // heavy hitter ordered list
            List<Map.Entry<String, Double>> heavyHitterOrderedList = new ArrayList<>(heavyHitterMap.entrySet());
            heavyHitterOrderedList.sort(Comparator.comparingDouble(Map.Entry::getValue));
            Collections.reverse(heavyHitterOrderedList);
            // heavy hitters
            List<String> heavyHitters = heavyHitterOrderedList.stream().map(Map.Entry::getKey).collect(Collectors.toList());
            // metrics
            ndcg += HeavyHitterMetrics.ndcg(heavyHitters, correctHeavyHitters);
            precision += HeavyHitterMetrics.precision(heavyHitters, correctHeavyHitters);
            re += HeavyHitterMetrics.relativeError(heavyHitterMap, correctCountMap);
            System.gc();
            memory += GraphLayout.parseInstance(streamCounter).totalSize();
        }
        ndcg = ndcg / testRound;
        precision = precision / testRound;
        re = re / testRound;
        memory = memory / testRound;
        // output report
        printInfo(printWriter, typeName, null, null, ndcg, precision, re, memory);
    }

    private void runNaiveHeavyHitter(double windowEpsilon, PrintWriter printWriter) throws IOException {
        LdpHeavyHitterType type = LdpHeavyHitterType.NAIVE_RR;
        LOGGER.info("Run {}, ε_w = {}", type.name(), windowEpsilon);
        double ndcg = 0.0;
        double precision = 0.0;
        double re = 0.0;
        double memory = 0.0;
        for (int round = 0; round < testRound; round++) {
            LdpHeavyHitter ldpHeavyHitter = LdpHeavyHitterFactory.createInstance(type, domainSet, k, windowEpsilon);
            double[] metrics = runLdpHeavyHitter(ldpHeavyHitter);
            ndcg += metrics[0];
            precision += metrics[1];
            re += metrics[2];
            memory += metrics[3];
        }
        ndcg = ndcg / testRound;
        precision = precision / testRound;
        re = re / testRound;
        memory = memory / testRound;
        printInfo(printWriter, type.name(), windowEpsilon, null, ndcg, precision, re, memory);
    }

    private void runBasicHgHeavyHitter(double windowEpsilon, PrintWriter printWriter) throws IOException {
        LdpHeavyHitterType type = LdpHeavyHitterType.BASIC_HG;
        LOGGER.info("Run {}, ε_w = {}", type.name(), windowEpsilon);
        double ndcg = 0.0;
        double precision = 0.0;
        double re = 0.0;
        double memory = 0.0;
        for (int round = 0; round < testRound; round++) {
            LdpHeavyHitter ldpHeavyHitter = LdpHeavyHitterFactory.createHgInstance(type, domainSet, k, windowEpsilon);
            double[] metrics = runLdpHeavyHitter(ldpHeavyHitter);
            ndcg += metrics[0];
            precision += metrics[1];
            re += metrics[2];
            memory += metrics[3];
        }
        ndcg = ndcg / testRound;
        precision = precision / testRound;
        re = re / testRound;
        memory = memory / testRound;
        printInfo(printWriter, type.name(), windowEpsilon, null, ndcg, precision, re, memory);
    }

    private void runAdvHhgHeavyHitter(double windowEpsilon, double alpha, PrintWriter printWriter) throws IOException {
        LdpHeavyHitterType type = LdpHeavyHitterType.ADVAN_HG;
        LOGGER.info("Run {}, ε_w = {}, α = {}", type.name(), windowEpsilon, alpha);
        double ndcg = 0.0;
        double precision = 0.0;
        double re = 0.0;
        double memory = 0.0;
        for (int round = 0; round < testRound; round++) {
            LdpHeavyHitter ldpHeavyHitter = LdpHeavyHitterFactory.createHhgInstance(type, domainSet, k, windowEpsilon, alpha);
            double[] metrics = runLdpHeavyHitter(ldpHeavyHitter);
            ndcg += metrics[0];
            precision += metrics[1];
            re += metrics[2];
            memory += metrics[3];
        }
        ndcg = ndcg / testRound;
        precision = precision / testRound;
        re = re / testRound;
        memory = memory / testRound;
        printInfo(printWriter, type.name(), windowEpsilon, alpha, ndcg, precision, re, memory);
    }

    private void runRelaxHhgHeavyHitter(double windowEpsilon, double alpha, PrintWriter printWriter) throws IOException {
        LdpHeavyHitterType type = LdpHeavyHitterType.RELAX_HG;
        LOGGER.info("Run {}, ε_w = {}, α = {}", type.name(), windowEpsilon, alpha);
        double ndcg = 0.0;
        double precision = 0.0;
        double re = 0.0;
        double memory = 0.0;
        for (int round = 0; round < testRound; round++) {
            LdpHeavyHitter ldpHeavyHitter = LdpHeavyHitterFactory.createHhgInstance(type, domainSet, k, windowEpsilon, alpha);
            double[] metrics = runLdpHeavyHitter(ldpHeavyHitter);
            ndcg += metrics[0];
            precision += metrics[1];
            re += metrics[2];
            memory += metrics[3];
        }
        ndcg = ndcg / testRound;
        precision = precision / testRound;
        re = re / testRound;
        memory = memory / testRound;
        printInfo(printWriter, type.name(), windowEpsilon, alpha, ndcg, precision, re, memory);
    }

    private double[] runLdpHeavyHitter(LdpHeavyHitter ldpHeavyHitter) throws IOException {
        // warmup
        AtomicInteger warmupIndex = new AtomicInteger();
        StreamDataUtils.obtainItemStream(datasetPath)
            .filter(item -> warmupIndex.getAndIncrement() <= warmupNum)
            .forEach(ldpHeavyHitter::warmupInsert);
        ldpHeavyHitter.stopWarmup();
        // randomize
        AtomicInteger randomizedIndex = new AtomicInteger();
        StreamDataUtils.obtainItemStream(datasetPath)
            .filter(item -> randomizedIndex.getAndIncrement() > warmupNum)
            .map(item -> ldpHeavyHitter.randomize(ldpHeavyHitter.getCurrentDataStructure(), item))
            .forEach(ldpHeavyHitter::randomizeInsert);
        // heavy hitter map
        Map<String, Double> heavyHitterMap = ldpHeavyHitter.responseHeavyHitters();
        Preconditions.checkArgument(heavyHitterMap.size() == k);
        // heavy hitter ordered list
        List<Map.Entry<String, Double>> heavyHitterOrderedList = new ArrayList<>(heavyHitterMap.entrySet());
        heavyHitterOrderedList.sort(Comparator.comparingDouble(Map.Entry::getValue));
        Collections.reverse(heavyHitterOrderedList);
        // heavy hitters
        List<String> heavyHitters = heavyHitterOrderedList.stream().map(Map.Entry::getKey).collect(Collectors.toList());
        // metrics
        double[] metrics = new double[4];
        metrics[0] = HeavyHitterMetrics.ndcg(heavyHitters, correctHeavyHitters);
        metrics[1] = HeavyHitterMetrics.precision(heavyHitters, correctHeavyHitters);
        metrics[2] = HeavyHitterMetrics.relativeError(heavyHitterMap, correctCountMap);
        System.gc();
        metrics[3] = GraphLayout.parseInstance(ldpHeavyHitter).totalSize();
        return metrics;
    }

    private void printInfo(PrintWriter printWriter, String type, Double windowEpsilon, Double alpha,
                           double ndcg, double precision, double re, double memory) {
        String windowEpsilonString = windowEpsilon == null ? "-" : String.valueOf(windowEpsilon);
        String alphaString = alpha == null ? "-" : String.valueOf(alpha);
        double roundNdcg = (double)Math.round(ndcg * 10000) / 10000;
        double roundPrecision = (double)Math.round(precision * 10000) / 10000;
        double roundRe = (double)Math.round(re * 10000) / 10000;
        double roundMemory = (double)Math.round(memory * 10000) / 10000;
        LOGGER.info("NDCG = {}, Precision = {}, RE = {}, Memory = {}", roundNdcg, roundPrecision, roundRe, memory);
        printWriter.println(type + "\t" + windowEpsilonString + "\t" + alphaString + "\t"
            + roundNdcg + "\t" + roundPrecision + "\t" + roundRe + "\t" + roundMemory);
    }
}
