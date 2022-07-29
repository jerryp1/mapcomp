package edu.alibaba.mpc4j.s2pc.pso.upsi.cm21;

import edu.alibaba.mpc4j.s2pc.pso.upsi.cmg21.Cmg21UpsiParams;
import org.junit.Test;

/**
 * CMG21非平衡PSI参数测试。
 *
 * @author Weiran Liu
 * @date 2022/7/12
 */
public class Cmg21UpsiParamTest {

    @Test
    public void testCreateUpsiParams() {
        Cmg21UpsiParams params = new Cmg21UpsiParams(
            3, 1638, 46, 5, 0,
            new int[]{1, 2, 3, 7, 11, 15, 19, 21, 22, 24},
            65537, 4096, new int[]{48, 32, 24}
        );
    }
}
