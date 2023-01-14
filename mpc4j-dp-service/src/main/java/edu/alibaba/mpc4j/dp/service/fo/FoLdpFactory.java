package edu.alibaba.mpc4j.dp.service.fo;

/**
 * LDP frequency oracle Factory.
 *
 * @author Weiran Liu
 * @date 2022/11/18
 */
public class FoLdpFactory {

    private FoLdpFactory() {
        // empty
    }

    public enum FoLdpType {
        /**
         * direct encoding
         */
        DE,
        /**
         * RAPPOR
         */
        RAPPOR,
    }
}
