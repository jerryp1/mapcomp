package edu.alibaba.mpc4j.s2pc.sbitmap.main;

/**
 * Sbitmap security mode.
 *
 * @author Li Peng
 * @date 2023/8/3
 */
public enum SbitmapSecurityMode {
    /**
     * 明文
     */
    PLAIN,
    /**
     * Full secure
     */
    FULL_SECURE,
    /**
     * Utility-optimal LDP
     */
    ULDP,
}
