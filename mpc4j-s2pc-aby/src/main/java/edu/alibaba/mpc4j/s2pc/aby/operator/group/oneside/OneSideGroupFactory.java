package edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside;

public class OneSideGroupFactory {
    public enum OneSideGroupPartyTypes {
        SENDER,
        RECEIVER
    }
    /**
     * permutation generator type enums.
     */
    public enum OneSideGroupTypes {
        /**
         * AMOS22
         */
        AMOS22_ONE_SIDE,
    }
    public enum AggTypes{
        MAX,
        MIN,
    }
}