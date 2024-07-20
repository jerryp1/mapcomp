package edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk;

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
    public static enum PtoType{
        /**
         * baseline method with circuit psi
         */
        BASELINE,
        /**
         * baseline method with private map
         */
        PHP24,
    }
}
