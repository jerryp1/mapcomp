package edu.alibaba.mpc4j.common.rpc.pto;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PartyState;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Abstract multi-party protocol.
 *
 * @author Weiran Liu
 * @date 2022/4/29
 */
public abstract class AbstractMultiPartyPto implements MultiPartyPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiPartyPto.class);
    /**
     * display log level.
     */
    private static final int DISPLAY_LOG_LEVEL = 2;
    /**
     * maximal number of sub-protocols. Note that some protocols would have many levels (e.g., PSU based on SKE).
     */
    protected static final int MAX_SUB_PROTOCOL_NUM = 5;
    /**
     * maximal tree level
     */
    protected static final int MAX_TREE_LEVEL = (int) Math.floor(Math.log(Integer.MAX_VALUE) / Math.log(MAX_SUB_PROTOCOL_NUM + 1));
    /**
     * the PRF used to extend the task ID.
     */
    protected final Prf taskIdPrf;
    /**
     * protocol description.
     */
    protected final PtoDesc ptoDesc;
    /**
     * the invoked rpc instance.
     */
    protected final Rpc rpc;
    /**
     * other parties' information.
     */
    private final Party[] otherParties;
    /**
     * the stopwatch, used to record times for each step.
     */
    protected final StopWatch stopWatch;
    /**
     * sub protocols
     */
    private final List<MultiPartyPto> subPtos;
    /**
     * tree level
     */
    private int treeLevel;
    /**
     * row level
     */
    private int rowLevel;
    /**
     * task ID
     */
    private int taskId;
    /**
     * the tree ID
     */
    private int treeId;
    /**
     * encode task ID
     */
    protected long encodeTaskId;
    /**
     * the log prefix for beginning a task
     */
    protected String ptoBeginLogPrefix;
    /**
     * the log prefix for each step
     */
    protected String ptoStepLogPrefix;
    /**
     * the log prefix for ending a task
     */
    protected String ptoEndLogPrefix;
    /**
     * the extra information
     */
    protected long extraInfo;
    /**
     * party state
     */
    protected PartyState partyState;

    /**
     * 构建两方计算协议。虽然Rpc可以得到所有参与方信息，但实际协议有可能不会用到全部的参与方，且协议执行时会用到参与方的顺序。
     * 为此，要通过{@code otherParties}指定其他参与方。
     *
     * @param ptoDesc      协议描述信息。
     * @param rpc          通信接口。
     * @param otherParties 其他参与方。
     */
    protected AbstractMultiPartyPto(PtoDesc ptoDesc, Rpc rpc, Party... otherParties) {
        // 验证其他参与方均在通信参与方之中
        Set<Party> partySet = rpc.getPartySet();
        for (Party otherParty : otherParties) {
            assert partySet.contains(otherParty) : otherParty.toString() + " does not in the Party Set";
        }
        this.ptoDesc = ptoDesc;
        this.rpc = rpc;
        this.otherParties = otherParties;
        // 为了保证所有平台都能够使用，这里强制要求用JDK的Prf
        taskIdPrf = PrfFactory.createInstance(PrfFactory.PrfType.JDK_AES_CBC, Long.BYTES);
        taskIdPrf.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
        stopWatch = new StopWatch();
        subPtos = new ArrayList<>(MAX_SUB_PROTOCOL_NUM);
        treeLevel = 0;
        rowLevel = 0;
        treeId = 0;
        taskId = 0;
        encodeTaskId = 0L;
        ptoBeginLogPrefix = "↘";
        ptoStepLogPrefix = "    ↓";
        ptoEndLogPrefix = "↙";
        extraInfo = 0;
        partyState = PartyState.NON_INITIALIZED;
    }

    protected void addSubPtos(MultiPartyPto subPto) {
        int rowLevel = subPtos.size();
        subPtos.add(subPto);
        MathPreconditions.checkLessOrEqual("# of sub-protocols", subPtos.size(), MAX_SUB_PROTOCOL_NUM);
        subPto.addTreeLevel(rowLevel, taskId, treeId);
    }

    @Override
    public void addTreeLevel(int rowLevel, int taskId, int parentTreeId) {
        MathPreconditions.checkNonNegativeInRange("rowLevel", rowLevel, MAX_SUB_PROTOCOL_NUM);
        treeLevel++;
        MathPreconditions.checkNonNegativeInRange("treeLevel", treeLevel, MAX_TREE_LEVEL);
        this.rowLevel = rowLevel;
        this.taskId = taskId;
        treeId = (int) Math.pow(MAX_SUB_PROTOCOL_NUM, treeLevel) * (rowLevel + 1) + parentTreeId;
        encodeTaskId = (((long) treeId) << Integer.SIZE) + taskId;
        ptoBeginLogPrefix = "    " + ptoBeginLogPrefix;
        ptoStepLogPrefix = "    " + ptoStepLogPrefix;
        ptoEndLogPrefix = "    " + ptoEndLogPrefix;
        // set sub-protocols
        for (int subPtoIndex = 0; subPtoIndex < subPtos.size(); subPtoIndex++) {
            subPtos.get(subPtoIndex).addTreeLevel(subPtoIndex, taskId, treeId);
        }
    }

    @Override
    public void setTaskId(int taskId) {
        // taskId >= 0
        MathPreconditions.checkNonNegative("taskId", taskId);
        // only the root protocol (treeId = 0) can set the task ID.
        MathPreconditions.checkEqual("treeId", "0", treeId, 0);

        this.taskId = taskId;
        encodeTaskId = (((long) treeId) << Integer.SIZE) + taskId;
        // set sub-protocols
        for (MultiPartyPto subPto : subPtos) {
            subPto.setEncodeTaskId(taskId, treeId);
        }
    }

    @Override
    public int getTaskId() {
        return taskId;
    }

    @Override
    public void setEncodeTaskId(int taskId, int parentTreeId) {
        // taskId >= 0
        MathPreconditions.checkNonNegative("taskId", taskId);
        // parentTreeId >= 0
        MathPreconditions.checkNonNegative("treeId", treeId);
        // tree level must be greater than 0, so that it is not the root protocol
        MathPreconditions.checkPositive("treeLevel", treeLevel);

        this.taskId = taskId;
        treeId = (int) Math.pow(MAX_SUB_PROTOCOL_NUM, treeLevel) * (rowLevel + 1) + parentTreeId;
        encodeTaskId = (((long) treeId) << Integer.SIZE) + taskId;
        // set sub-protocols
        for (MultiPartyPto subPto : subPtos) {
            subPto.setEncodeTaskId(taskId, treeId);
        }
    }

    @Override
    public long getEncodeTaskId() {
        return encodeTaskId;
    }

    @Override
    public Rpc getRpc() {
        return rpc;
    }

    @Override
    public PtoDesc getPtoDesc() {
        return ptoDesc;
    }

    @Override
    public Party[] otherParties() {
        return otherParties;
    }

    @Override
    public PartyState getPartyState() {
        return partyState;
    }

    /**
     * init, check and update party state.
     */
    protected void initState() {
        switch (partyState) {
            case NON_INITIALIZED:
            case INITIALIZED:
                partyState = PartyState.INITIALIZED;
                return;
            case DESTROYED:
            default:
                throw new IllegalStateException("Party state must not be " + PartyState.DESTROYED);
        }
    }

    /**
     * check ready state.
     */
    protected void checkReadyState() {
        switch (partyState) {
            case INITIALIZED:
                return;
            case NON_INITIALIZED:
            case DESTROYED:
            default:
                throw new IllegalStateException("Party state must not be " + PartyState.DESTROYED);
        }
    }

    @Override
    public void destroy() {
        switch (partyState) {
            case NON_INITIALIZED:
            case INITIALIZED:
                partyState = PartyState.DESTROYED;
                return;
            case DESTROYED:
            default:
                throw new IllegalStateException("Party state must not be " + PartyState.DESTROYED);
        }
    }

    protected void logBeginEndInfo(PtoState ptoState) {
        switch (ptoState) {
            case INIT_BEGIN:
                info("{}{} {} Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName(), ownParty().getPartyName());
                break;
            case INIT_END:
                info("{}{} {} Init end", ptoEndLogPrefix, getPtoDesc().getPtoName(), ownParty().getPartyName());
                break;
            case PTO_BEGIN:
                info("{}{} {} Pto begin", ptoBeginLogPrefix, getPtoDesc().getPtoName(), ownParty().getPartyName());
                break;
            case PTO_END:
                info("{}{} {} Pto end", ptoEndLogPrefix, getPtoDesc().getPtoName(), ownParty().getPartyName());
                break;
            default:
                throw new IllegalStateException("Invalid " + PtoState.class.getSimpleName() + ": " + ptoState);
        }
    }

    protected void logStepInfo(PtoState ptoState, int currentStepIndex, int totalStepIndex, long time) {
        assert currentStepIndex >= 0 && currentStepIndex <= totalStepIndex
            : "current step index must be in range [0, " + totalStepIndex + "]: " + currentStepIndex;
        switch (ptoState) {
            case INIT_STEP:
                info("{}{} {} init Step {}/{} ({}ms)",
                    ptoStepLogPrefix, getPtoDesc().getPtoName(), ownParty().getPartyName(),
                    currentStepIndex, totalStepIndex, time);
                break;
            case PTO_STEP:
                info("{}{} {} Step {}/{} ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), ownParty().getPartyName(),
                    currentStepIndex, totalStepIndex, time);
                break;
            default:
                throw new IllegalStateException("Invalid " + PtoState.class.getSimpleName() + ": " + ptoState);
        }
    }

    /**
     * Log a message at the INFO level if {@code logLevel} is not greater than {@code DISPLAY_LOG_LEVEL}.
     *
     * @param message the message string to be logged.
     */
    protected void info(String message) {
        if (treeLevel < DISPLAY_LOG_LEVEL) {
            LOGGER.info(message);
        }
    }

    /**
     * Log a message at the INFO level according to the specified format and arguments, if {@code logLevel} is not
     * greater than {@code DISPLAY_LOG_LEVEL}.
     *
     * @param format the format string.
     * @param arg0   the first argument.
     * @param arg1   the second argument.
     */
    protected void info(String format, Object arg0, Object arg1) {
        if (treeLevel < DISPLAY_LOG_LEVEL) {
            LOGGER.info(format, arg0, arg1);
        }
    }

    /**
     * Log a message at the INFO level according to the specified format and arguments, if {@code logLevel} is not
     * greater than {@code DISPLAY_LOG_LEVEL}.
     *
     * @param format    the format string.
     * @param arguments a list of 3 or more arguments
     */
    protected void info(String format, Object... arguments) {
        if (treeLevel < DISPLAY_LOG_LEVEL) {
            LOGGER.info(format, arguments);
        }
    }
}
