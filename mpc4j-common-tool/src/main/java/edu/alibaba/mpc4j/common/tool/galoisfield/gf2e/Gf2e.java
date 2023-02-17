package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import edu.alibaba.mpc4j.common.tool.galoisfield.BytesField;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory.Gf2eType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * GF(2^l) interface.
 *
 * @author Weiran Liu
 * @date 2022/4/27
 */
public interface Gf2e extends BytesField {
    /**
     * 返回运算类型。
     *
     * @return 运算类型。
     */
    Gf2eType getGf2eType();
}
