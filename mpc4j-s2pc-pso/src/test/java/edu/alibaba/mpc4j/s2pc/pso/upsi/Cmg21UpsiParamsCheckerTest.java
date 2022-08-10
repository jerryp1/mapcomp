package edu.alibaba.mpc4j.s2pc.pso.upsi;

import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.s2pc.pso.upsi.cmg21.Cmg21UpsiParams;
import org.junit.Test;

/**
 * CMG21非平衡PSI协议参数检查器测试。
 *
 * @author Liqiang Peng
 * @date 2022/8/9
 */
public class Cmg21UpsiParamsCheckerTest {

    @Test
    public void testValidCmg21UpsiParams() {


        Cmg21UpsiParams.create(
            CuckooHashBinFactory.CuckooHashBinType.NO_STASH_ONE_HASH, 512, 15,
            8,
            0, new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15},
            40961, 4096, new int[]{24, 24, 24},
            2000, 1
        );

        Cmg21UpsiParams.create(
            CuckooHashBinFactory.CuckooHashBinType.NO_STASH_ONE_HASH, 512, 20,
            8,
            0, new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20},
            40961, 4096, new int[]{24, 24, 24},
            100000, 1
        );

        Cmg21UpsiParams.create(
            CuckooHashBinFactory.CuckooHashBinType.NAIVE_3_HASH, 2046, 101,
            6,
            0, new int[]{1, 3, 4, 5, 8, 14, 20, 26, 32, 38, 44, 47, 48, 49, 51, 52},
            40961, 4096, new int[]{40, 34, 34},
            1000000, 1024
        );

        Cmg21UpsiParams.create(
            CuckooHashBinFactory.CuckooHashBinType.NAIVE_3_HASH, 1638, 125,
            5,
            5, new int[]{1, 2, 3, 4, 5, 6, 18, 30, 42, 54, 60},
            188417, 4096, new int[]{48, 36, 25},
            1000000, 1024
        );

        Cmg21UpsiParams.create(
            CuckooHashBinFactory.CuckooHashBinType.NAIVE_3_HASH, 16384, 98,
            4,
            8, new int[]{1, 3, 4, 9, 27},
            1785857, 8192, new int[]{56, 56, 24, 24},
            1000000, 11041
        );

        Cmg21UpsiParams.create(
            CuckooHashBinFactory.CuckooHashBinType.NAIVE_3_HASH, 3410, 72,
            6,
            0, new int[]{1, 3, 4, 9, 11, 16, 20, 25, 27, 32, 33, 35, 36},
            40961, 4096, new int[]{42, 32, 32},
            1000000, 2048
        );

        Cmg21UpsiParams.create(
            CuckooHashBinFactory.CuckooHashBinType.NAIVE_3_HASH, 3410, 125,
            6,
            5, new int[]{1, 2, 3, 4, 5, 6, 18, 30, 42, 54, 60},
            65537, 4096, new int[]{48, 30, 30},
            1000000, 2048
        );

        Cmg21UpsiParams.create(
            CuckooHashBinFactory.CuckooHashBinType.NAIVE_3_HASH, 585, 180,
            7,
            0, new int[]{1, 3, 4, 6, 10, 13, 15, 21, 29, 37, 45, 53, 61, 69, 77, 81, 83, 86, 87, 90, 92, 96},
            40961, 4096, new int[]{42, 32, 32},
            1000000, 256
        );

        Cmg21UpsiParams.create(
            CuckooHashBinFactory.CuckooHashBinType.NAIVE_3_HASH, 6552, 40,
            5,
            0, new int[]{1, 3, 4, 9, 11, 16, 17, 19, 20},
            65537, 4096, new int[]{48, 30, 30},
            1000000, 4096
        );

        Cmg21UpsiParams.create(
            CuckooHashBinFactory.CuckooHashBinType.NAIVE_3_HASH, 6825, 98,
            6,
            8, new int[]{1, 3, 4, 9, 27},
            65537, 8192, new int[]{56, 56, 30},
            1000000, 4096
        );

        Cmg21UpsiParams.create(
            CuckooHashBinFactory.CuckooHashBinType.NAIVE_3_HASH, 1364, 128,
            6,
            0, new int[]{1, 3, 4, 5, 8, 14, 20, 26, 32, 38, 44, 50, 56, 59, 60, 61, 63, 64},
            65537, 4096, new int[]{40, 34, 30},
            1000000, 512
        );

        Cmg21UpsiParams.create(
            CuckooHashBinFactory.CuckooHashBinType.NAIVE_3_HASH, 1364, 228,
            6,
            4, new int[]{1, 2, 3, 4, 5, 10, 15, 35, 55, 75, 95, 115, 125, 130, 140},
            65537, 4096, new int[]{48, 34, 27},
            1000000, 512
        );

        Cmg21UpsiParams.create(
            CuckooHashBinFactory.CuckooHashBinType.NAIVE_3_HASH, 8192, 98,
            4,
            8, new int[]{1, 3, 4, 9, 27},
            1785857, 8192, new int[]{56, 56, 24, 24},
            1000000, 5535
        );
    }
}
