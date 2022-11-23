package edu.alibaba.mpc4j.dp.stream.heavyhitter;

/**
 * The state of Heavy Hitter with Local Differential Privacy
 *
 * @author Weiran Liu
 * @date 2022/11/21
 */
public enum HeavyHitterState {
    /**
     * warm-up state
     */
    WARMUP,
    /**
     * statistics state
     */
    STATISTICS,
    /**
     * clean state
     */
    CLEAN,
}
