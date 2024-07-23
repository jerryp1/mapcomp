package edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.baseline.BaselinePkFkViewConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.baseline.BaselinePkFkViewReceiver;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.baseline.BaselinePkFkViewSender;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.php24.Php24PkFkViewConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.php24.Php24PkFkViewReceiver;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.php24.Php24PkFkViewSender;

/**
 * view factory
 *
 * @author Feng Han
 * @date 2024/7/19
 */
public class PkFkViewFactory {
    /**
     * protocol types
     */
    public static enum ViewPtoType {
        /**
         * baseline method with circuit psi
         */
        BASELINE,
        /**
         * baseline method with private map
         */
        PHP24,
    }

    public static PkFkViewSender createPkFkViewSender(Rpc senderRpc, Party receiverParty, PkFkViewConfig config){
        switch (config.getPtoType()){
            case BASELINE:
                return new BaselinePkFkViewSender(senderRpc, receiverParty, (BaselinePkFkViewConfig) config);
            case PHP24:
                return new Php24PkFkViewSender(senderRpc, receiverParty, (Php24PkFkViewConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ViewPtoType.class.getSimpleName() + ": " + config.getPtoType().name());
        }
    }

    public static PkFkViewReceiver createPkFkViewReceiver(Rpc receiverRpc, Party senderParty, PkFkViewConfig config){
        switch (config.getPtoType()){
            case BASELINE:
                return new BaselinePkFkViewReceiver(receiverRpc, senderParty, (BaselinePkFkViewConfig) config);
            case PHP24:
                return new Php24PkFkViewReceiver(receiverRpc, senderParty, (Php24PkFkViewConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ViewPtoType.class.getSimpleName() + ": " + config.getPtoType().name());
        }
    }

    public static PkFkViewConfig createDefaultConfig(boolean silent){
        return new Php24PkFkViewConfig.Builder(silent).build();
    }
}
