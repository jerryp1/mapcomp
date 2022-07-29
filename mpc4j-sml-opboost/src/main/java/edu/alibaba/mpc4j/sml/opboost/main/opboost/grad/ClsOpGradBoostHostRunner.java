package edu.alibaba.mpc4j.sml.opboost.main.opboost.grad;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.sml.opboost.grad.ClsOpGradBoostHost;
import edu.alibaba.mpc4j.sml.opboost.grad.ClsOpGradBoostHostConfig;
import edu.alibaba.mpc4j.sml.opboost.main.opboost.AbstractOpBoostHostRunner;
import edu.alibaba.mpc4j.sml.smile.classification.GradientTreeBoost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.validation.metric.Accuracy;

import java.util.concurrent.TimeUnit;

/**
 * 分类OpGardBoost执行方。
 *
 * @author Weiran Liu
 * @date 2022/7/8
 */
class ClsOpGradBoostHostRunner extends AbstractOpBoostHostRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOpBoostHostRunner.class);
    /**
     * 主机
     */
    private final ClsOpGradBoostHost host;
    /**
     * 主机通信接口。
     */
    private final Rpc hostRpc;
    /**
     * 主机配置项
     */
    private final ClsOpGradBoostHostConfig hostConfig;
    /**
     * 训练真实值
     */
    private final int[] trainTruths;
    /**
     * 预测真实值
     */
    private final int[] testTruths;

    ClsOpGradBoostHostRunner(ClsOpGradBoostHost host, ClsOpGradBoostHostConfig hostConfig, int totalRound,
                             Formula formula, DataFrame ownDataFrame,
                             DataFrame trainFeatureDataFrame, int[] trainTruth,
                             DataFrame testFeatureDataFrame, int[] testTruth) {
        super(totalRound, formula, ownDataFrame, trainFeatureDataFrame, testFeatureDataFrame);
        this.host = host;
        hostRpc = host.getRpc();
        this.hostConfig = hostConfig;
        this.trainTruths = trainTruth;
        this.testTruths = testTruth;
    }

    @Override
    public void run() throws MpcAbortException {
        hostRpc.synchronize();
        hostRpc.reset();
        reset();
        // 重复实验，记录数据
        for (int round = 1; round <= totalRound; round++) {
            stopWatch.start();
            GradientTreeBoost model = host.fit(formula, ownDataFrame, hostConfig);
            stopWatch.stop();
            // 记录时间
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            int[] trainPredicts = model.predict(trainFeatureDataFrame);
            double trainMeasure = Accuracy.of(trainTruths, trainPredicts);
            int[] testPredicts = model.predict(testFeatureDataFrame);
            double testMeasure = Accuracy.of(testTruths, testPredicts);
            LOGGER.info("Round {}: Time = {}ms, Train Acc. = {}, Test Acc. = {}",
                round, time, trainMeasure, testMeasure
            );
            totalTrainMeasure += trainMeasure;
            totalTestMeasure += testMeasure;
            totalTime += time;
        }
        totalPacketNum = hostRpc.getSendDataPacketNum();
        totalPayloadByteLength = hostRpc.getPayloadByteLength();
        totalSendByteLength = hostRpc.getSendByteLength();
        hostRpc.reset();
    }
}
