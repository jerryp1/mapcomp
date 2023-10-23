package edu.alibaba.mpc4j.s2pc.sbitmap.bitmap;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Secure bitmap description.
 *
 * @author Li Peng
 * @date 2023/8/17
 */
public class SecureBitmapDesc implements PtoDesc {
    /**
     * protocol id.
     */
    private static final int PTO_ID = Math.abs((int) 2047864806225283373L);
    /**
     * protocol name.
     */
    private static final String PTO_NAME = "SECURE_BITMAP";

    /**
     * Protocol step.
     */
    enum PtoStep {
        /**
         * sender send keys of bitmap in share process.
         */
        SENDER_SEND_KEYS_SHARES,
        /**
         * sender send keys of bitmap in reveal process.
         */
        SENDER_SEND_KEYS_REVEAL,
    }

    /**
     * singleton mode.
     */
    private static final SecureBitmapDesc INSTANCE = new SecureBitmapDesc();

    /**
     * private constructor.
     */
    private SecureBitmapDesc() {
        // empty
    }

    /**
     * get static instance.
     */
    public static PtoDesc getInstance() {
        return INSTANCE;
    }

    static {
        PtoDescManager.registerPtoDesc(getInstance());
    }

    @Override
    public int getPtoId() {
        return PTO_ID;
    }

    @Override
    public String getPtoName() {
        return PTO_NAME;
    }
}
