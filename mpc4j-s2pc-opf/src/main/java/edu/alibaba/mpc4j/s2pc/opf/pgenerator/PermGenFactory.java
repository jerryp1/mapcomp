package edu.alibaba.mpc4j.s2pc.opf.pgenerator;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.opf.pgenerator.bitmap.BitmapPermGenConfig;
import edu.alibaba.mpc4j.s2pc.opf.pgenerator.bitmap.BitmapPermGenReceiver;
import edu.alibaba.mpc4j.s2pc.opf.pgenerator.bitmap.BitmapPermGenSender;
import edu.alibaba.mpc4j.s2pc.opf.pgenerator.smallfield.ahi22.Ahi22SmallFieldPermGenConfig;
import edu.alibaba.mpc4j.s2pc.opf.pgenerator.smallfield.ahi22.Ahi22SmallFieldPermGenReceiver;
import edu.alibaba.mpc4j.s2pc.opf.pgenerator.smallfield.ahi22.Ahi22SmallFieldPermGenSender;

/**
 * permutation generator Factory
 *
 */
public class PermGenFactory {

    public enum PartyTypes {
        /**
         * role is sender.
         */
        SENDER,
        /**
         * role is receiver
         */
        RECEIVER
    }

    public enum FieldTypes {
        /**
         * small field input
         */
        SMALL_FIELD,
        /**
         * bitmap input
         */
        BITMAP,
    }

    /**
     * permutation generator type enums.
     */
    public enum PermGenTypes {
        /**
         * AHI+22.
         */
        AHI22_SMALL_FIELD,
        /**
         * AHI+22.
         */
        AHI22_BITMAP,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static PermGenParty createSender(Rpc senderRpc, Party receiverParty, PermGenConfig config) {
        PermGenTypes type = config.getPtoType();
        switch (type) {
            case AHI22_SMALL_FIELD:
                return new Ahi22SmallFieldPermGenSender(senderRpc, receiverParty, (Ahi22SmallFieldPermGenConfig) config);
            case AHI22_BITMAP:
                return new BitmapPermGenSender(senderRpc, receiverParty, (BitmapPermGenConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PermGenTypes.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a receiver.
     *
     * @param receiverRpc the receiver RPC.
     * @param senderParty the sender party.
     * @param config      the config.
     * @return a receiver.
     */
    public static PermGenParty createReceiver(Rpc receiverRpc, Party senderParty, PermGenConfig config) {
        PermGenTypes type = config.getPtoType();
        switch (type) {
            case AHI22_SMALL_FIELD:
                return new Ahi22SmallFieldPermGenReceiver(receiverRpc, senderParty, (Ahi22SmallFieldPermGenConfig) config);
            case AHI22_BITMAP:
                return new BitmapPermGenReceiver(receiverRpc, senderParty, (BitmapPermGenConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PermGenTypes.class.getSimpleName() + ": " + type.name());
        }
    }

}
