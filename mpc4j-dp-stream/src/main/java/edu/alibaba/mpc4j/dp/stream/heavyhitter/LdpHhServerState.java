package edu.alibaba.mpc4j.dp.stream.heavyhitter;

/**
 * The state of Heavy Hitter server with Local Differential Privacy.
 *
 * @author Weiran Liu
 * @date 2022/11/21
 */
public enum LdpHhServerState {
    /**
     * warm-up state
     */
    WARMUP,
    /**
     * statistics state
     */
    STATISTICS,
}
