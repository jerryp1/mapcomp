package edu.alibaba.mpc4j.s2pc.aby;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64Factory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.security.SecureRandom;

/**
 * ABY test utils.
 *
 * @author Weiran Liu
 * @date 2023/5/19
 */
public class AbyTestUtils {
    /**
     * private constructor.
     */
    private AbyTestUtils() {
        // empty
    }

    /**
     * random state
     */
    public static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Zl instances
     */
    public static final Zl[] ZLS = new Zl[]{
        ZlFactory.createInstance(EnvType.STANDARD, 1),
        ZlFactory.createInstance(EnvType.STANDARD, 3),
        ZlFactory.createInstance(EnvType.STANDARD, LongUtils.MAX_L - 1),
        ZlFactory.createInstance(EnvType.STANDARD, LongUtils.MAX_L),
        ZlFactory.createInstance(EnvType.STANDARD, LongUtils.MAX_L + 1),
    };

    /**
     * Zl64 instances
     */
    public static final Zl64[] ZL64S = new Zl64[]{
        Zl64Factory.createInstance(EnvType.STANDARD, 1),
        Zl64Factory.createInstance(EnvType.STANDARD, 3),
        Zl64Factory.createInstance(EnvType.STANDARD, LongUtils.MAX_L),
    };
}
