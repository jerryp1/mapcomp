package edu.alibaba.mpc4j.s2pc.pcg.btg.impl.offline;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.btg.AbstractBtgParty;
import edu.alibaba.mpc4j.s2pc.pcg.btg.BooleanTriple;
import edu.alibaba.mpc4j.s2pc.pcg.btg.rbtg.RbtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.btg.rbtg.RbtgParty;

import java.util.concurrent.TimeUnit;

/**
 * 离线BTG协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/4/8
 */
public class OfflineBtgSender extends AbstractBtgParty {
    /**
     * RBTG发送方
     */
    private final RbtgParty rbtgSender;
    /**
     * 更新时的单次数量
     */
    private int updateRoundNum;
    /**
     * 更新时的执行轮数
     */
    private int updateRound;
    /**
     * 缓存区
     */
    private BooleanTriple booleanTripleBuffer;

    public OfflineBtgSender(Rpc senderRpc, Party receiverParty, OfflineBtgConfig config) {
        super(OfflineBtgPtoDesc.getInstance(), senderRpc, receiverParty, config);
        rbtgSender = RbtgFactory.createSender(senderRpc, receiverParty, config.getRbtgConfig());
        rbtgSender.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        rbtgSender.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        rbtgSender.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        rbtgSender.addLogLevel();
    }

    @Override
    public void init(int maxRoundNum, int updateNum) throws MpcAbortException {
        setInitInput(maxRoundNum, updateNum);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        if (updateNum <= config.maxBaseNum()) {
            // 如果最大数量小于支持的单轮最大数量，则执行1轮最大数量即可
            this.updateRoundNum = updateNum;
            updateRound = 1;
        } else {
            // 如果最大数量大于支持的单轮最大数量，则分批执行
            this.updateRoundNum = config.maxBaseNum();
            updateRound = (int)Math.ceil((double) updateNum / config.maxBaseNum());
        }
        rbtgSender.init(this.updateRoundNum);
        booleanTripleBuffer = BooleanTriple.createEmpty();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 1/2.{} ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), updateRound, initTime);

        // 生成所需的布尔三元组
        for (int round = 1; round <= updateRound; round++) {
            stopWatch.start();
            BooleanTriple booleanTriple = rbtgSender.generate(this.updateRoundNum);
            booleanTripleBuffer.merge(booleanTriple);
            stopWatch.stop();
            long roundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            info("{}{} Send. Init Step 2.{}/2.{} ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), round, updateRound, roundTime);
        }
        initialized = true;
        info("{}{} Send. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public BooleanTriple generate(int num) throws MpcAbortException {
        setPtoInput(num);
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        while (num > booleanTripleBuffer.getNum()) {
            // 如果所需的数量大于缓存区数量，则继续生成
            for (int round = 1; round <= updateRound; round++) {
                stopWatch.start();
                BooleanTriple booleanTriple = rbtgSender.generate(updateRoundNum);
                booleanTripleBuffer.merge(booleanTriple);
                stopWatch.stop();
                long roundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
                stopWatch.reset();
                info("{}{} Send. Step 0.{}/0.{} ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), round, updateRound, roundTime);
            }
        }

        stopWatch.start();
        BooleanTriple senderOutput = booleanTripleBuffer.split(num);
        stopWatch.stop();
        long splitTripleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), splitTripleTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return senderOutput;
    }
}
