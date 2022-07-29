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
 * 离线BTG协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/4/8
 */
public class OfflineBtgReceiver extends AbstractBtgParty {
    /**
     * RBTG接收方
     */
    private final RbtgParty rbtgReceiver;
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

    public OfflineBtgReceiver(Rpc receiverRpc, Party senderParty, OfflineBtgConfig config) {
        super(OfflineBtgPtoDesc.getInstance(), receiverRpc, senderParty, config);
        rbtgReceiver = RbtgFactory.createReceiver(receiverRpc, senderParty, config.getRbtgConfig());
        rbtgReceiver.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        rbtgReceiver.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        rbtgReceiver.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        rbtgReceiver.addLogLevel();
    }

    @Override
    public void init(int maxRoundNum, int updateNum) throws MpcAbortException {
        setInitInput(maxRoundNum, updateNum);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        if (updateNum <= config.maxBaseNum()) {
            // 如果最大数量小于支持的单轮最大数量，则执行1轮最大数量即可
            this.updateRoundNum = updateNum;
            updateRound = 1;
        } else {
            // 如果最大数量大于支持的单轮最大数量，则分批执行
            this.updateRoundNum = config.maxBaseNum();
            updateRound = (int) Math.ceil((double) updateNum / config.maxBaseNum());
        }
        rbtgReceiver.init(this.updateRoundNum);
        booleanTripleBuffer = BooleanTriple.createEmpty();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 1/2.{} ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), updateRound, initTime);

        // 生成所需的布尔三元组
        for (int round = 1; round <= updateRound; round++) {
            stopWatch.start();
            BooleanTriple booleanTriple = rbtgReceiver.generate(this.updateRoundNum);
            booleanTripleBuffer.merge(booleanTriple);
            stopWatch.stop();
            long roundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            info("{}{} Recv. Init Step 2.{}/2.{} ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), round, updateRound, roundTime);
        }

        initialized = true;
        info("{}{} Recv. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public BooleanTriple generate(int num) throws MpcAbortException {
        setPtoInput(num);
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        while (num > booleanTripleBuffer.getNum()) {
            // 如果所需的数量大于缓存区数量，则继续生成
            for (int round = 1; round <= updateRound; round++) {
                stopWatch.start();
                BooleanTriple booleanTriple = rbtgReceiver.generate(updateRoundNum);
                booleanTripleBuffer.merge(booleanTriple);
                stopWatch.stop();
                long roundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
                stopWatch.reset();
                info("{}{} Recv. Step 0.{}/0.{} ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), round, updateRound, roundTime);
            }
        }

        stopWatch.start();
        BooleanTriple receiverOutput = booleanTripleBuffer.split(num);
        stopWatch.stop();
        long splitTripleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), splitTripleTime);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return receiverOutput;
    }
}
