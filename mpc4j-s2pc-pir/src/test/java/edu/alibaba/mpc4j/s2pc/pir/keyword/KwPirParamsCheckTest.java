package edu.alibaba.mpc4j.s2pc.pir.keyword;

import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirParams;
import org.junit.Test;

/**
 * CMG21关键词PIR协议参数检查器测试。
 *
 * @author Liqiang Peng
 * @date 2022/8/9
 */
public class KwPirParamsCheckTest {

    @Test
    public void testValidCmg21KwPirParams() {
        Cmg21KwPirParams.create(
            CuckooHashBinFactory.CuckooHashBinType.NAIVE_3_HASH, 6552, 200,
            5,
            26, new int[]{1, 5, 8, 27, 135},
            1785857L, 8192, new int[]{50, 56, 56, 50},
            1000000, 4096
        );

        Cmg21KwPirParams.create(
            CuckooHashBinFactory.CuckooHashBinType.NO_STASH_ONE_HASH, 1638, 228,
            5,
            0, new int[]{1, 3, 8, 19, 33, 39, 92, 102},
            65537L, 8192, new int[]{56, 48, 48},
            1000000, 1
        );
    }
}