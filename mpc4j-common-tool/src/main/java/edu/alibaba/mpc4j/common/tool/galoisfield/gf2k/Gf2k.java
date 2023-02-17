package edu.alibaba.mpc4j.common.tool.galoisfield.gf2k;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.BytesRing;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory.Gf2kType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * GF(2^128)有限域运算接口。
 *
 * @author Weiran Liu
 * @date 2022/01/15
 */
public interface Gf2k extends BytesRing {
    /**
     * Gets GF(2^λ) type.
     *
     * @return GF(2^λ) type.
     */
    Gf2kType getGf2kType();
}
