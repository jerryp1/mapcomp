package edu.alibaba.mpc4j.s2pc.pjc.main.pmap;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidFactory;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapFactory;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.hpl24.Hpl24PmapConfig;

import java.util.Properties;

public class PmapConfigUtils {

    private PmapConfigUtils() {
        // empty
    }

    /**
     * 创建配置项。
     *
     * @param properties 配置参数。
     * @return 配置项。
     */
    static PmapConfig createConfig(Properties properties) {
        // 读取协议类型
        String pmapTypeString = PropertiesUtils.readString(properties, "pmap_pto_name");
        PmapFactory.PmapType pmapType = PmapFactory.PmapType.valueOf(pmapTypeString);
        switch (pmapType) {
            case HPL24:
                return createHpl24PmapConfig(properties);
            default:
                throw new IllegalArgumentException(
                    "Invalid " + PidFactory.PidType.class.getSimpleName() + ":" + pmapTypeString
                );
        }
    }

    private static Hpl24PmapConfig createHpl24PmapConfig(Properties properties) {
        // 是否使用压缩编码
        boolean silent = PropertiesUtils.readBoolean(properties, "silent", false);
        int bitLen = PropertiesUtils.readIntWithDefault(properties, "bitLen", 32);

        return new Hpl24PmapConfig.Builder(silent).setBitLength(bitLen).build();
    }
}
