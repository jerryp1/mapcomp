package edu.alibaba.mpc4j.s2pc.groupagg.pto.group;

/**
 * group aggregation type enums
 *
 */
public class GroupTypes {
    public enum AggTypes {
        /**
         * Maximum
         */
        MAX,
        /**
         * Minimum
         */
        MIN,
    }

    public enum GroupPartyTypes {
        /**
         * Role is sender.
         */
        SENDER,
        /**
         * Role is receiver.
         */
        RECEIVER
    }
}
