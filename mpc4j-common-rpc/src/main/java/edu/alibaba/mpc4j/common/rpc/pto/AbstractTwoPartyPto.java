package edu.alibaba.mpc4j.common.rpc.pto;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;

/**
 * Abstract two-party protocol.
 *
 * @author Weiran Liu
 * @date 2022/4/28
 */
public abstract class AbstractTwoPartyPto extends AbstractMultiPartyPto implements TwoPartyPto {

    protected AbstractTwoPartyPto(PtoDesc ptoDesc, Rpc rpc, Party otherParty) {
        super(ptoDesc, rpc, otherParty);
    }

    @Override
    public Party otherParty() {
        return otherParties()[0];
    }

    protected void logP0BeginEndInfo(PtoState ptoState) {
        switch (ptoState) {
            case INIT_BEGIN:
                info("{}{} P0 Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());
                break;
            case INIT_END:
                info("{}{} P0 Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
                break;
            case PTO_BEGIN:
                info("{}{} P0 Pto begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());
                break;
            case PTO_END:
                info("{}{} P0 Pto end", ptoBeginLogPrefix, getPtoDesc().getPtoName());
                break;
            default:
                throw new IllegalStateException("Invalid " + PtoState.class.getSimpleName() + ": " + ptoState);
        }
    }

    protected void logP0StepInfo(PtoState ptoState, int currentStepIndex, int totalStepIndex, long time) {
        assert currentStepIndex >= 0 && currentStepIndex <= totalStepIndex
            : "current step index must be in range [0, " + totalStepIndex + "]: " + currentStepIndex;
        switch (ptoState) {
            case INIT_STEP:
                info("{}{} P0 init Step {}/{} ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(),
                    currentStepIndex, totalStepIndex, time);
                break;
            case PTO_STEP:
                info("{}{} P0 Step {}/{} ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(),
                    currentStepIndex, totalStepIndex, time);
                break;
            default:
                throw new IllegalStateException("Invalid " + PtoState.class.getSimpleName() + ": " + ptoState);
        }
    }

    protected void logP1BeginEndInfo(PtoState ptoState) {
        switch (ptoState) {
            case INIT_BEGIN:
                info("{}{} P1 Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());
                break;
            case INIT_END:
                info("{}{} P1 Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
                break;
            case PTO_BEGIN:
                info("{}{} P1 Pto begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());
                break;
            case PTO_END:
                info("{}{} P1 Pto end", ptoBeginLogPrefix, getPtoDesc().getPtoName());
                break;
            default:
                throw new IllegalStateException("Invalid " + PtoState.class.getSimpleName() + ": " + ptoState);
        }
    }

    protected void logP1StepInfo(PtoState ptoState, int currentStepIndex, int totalStepIndex, long time) {
        assert currentStepIndex >= 0 && currentStepIndex <= totalStepIndex
            : "current step index must be in range [0, " + totalStepIndex + "]: " + currentStepIndex;
        switch (ptoState) {
            case INIT_STEP:
                info("{}{} P1 init Step {}/{} ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(),
                    currentStepIndex, totalStepIndex, time);
                break;
            case PTO_STEP:
                info("{}{} P1 Step {}/{} ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(),
                    currentStepIndex, totalStepIndex, time);
                break;
            default:
                throw new IllegalStateException("Invalid " + PtoState.class.getSimpleName() + ": " + ptoState);
        }
    }
}
