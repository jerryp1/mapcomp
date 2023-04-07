package edu.alibaba.mpc4j.s2pc.pcg.edabit;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pcg.edabit.zl.ZlEdabitReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.edabit.zl.ZlEdabitSender;

/**
 * Edabit工厂类
 *
 * @author Xinwei Gao
 * Date: 2023-04-04
 */
public class EdabitFactory implements PtoFactory {
    /**
     * 私有构造函数
     */
    private EdabitFactory() {
        // empty for now
    }

    /**
     * 协议类型
     */
    public enum EdabitType {
        /**
         * Zl下的Edabit
         */
        ZL_EDABIT,
    }

    public static EdabitParty createReceiver(Rpc receiverRpc, Party receiverParty, EdabitConfig edabitConfig) {
        EdabitType edabitType = edabitConfig.getPtoType();
        switch (edabitType) {
            case ZL_EDABIT:
                return new ZlEdabitReceiver(receiverRpc, receiverParty, edabitConfig);
            default:
                throw new IllegalArgumentException(
                    "Invalid " + EdabitType.class.getSimpleName() + ": " + edabitType.name());
        }
    }

    public static EdabitParty createSender(Rpc senderRpc, Party senderParty, EdabitConfig edabitConfig) {
        EdabitType edabitType = edabitConfig.getPtoType();
        switch (edabitType) {
            case ZL_EDABIT:
                return new ZlEdabitSender(senderRpc, senderParty, edabitConfig);
            default:
                throw new IllegalArgumentException(
                    "Invalid " + EdabitType.class.getSimpleName() + ": " + edabitType.name());
        }
    }
}
