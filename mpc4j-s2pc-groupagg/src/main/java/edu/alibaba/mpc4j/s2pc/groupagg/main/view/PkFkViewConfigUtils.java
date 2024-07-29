package edu.alibaba.mpc4j.s2pc.groupagg.main.view;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkViewConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkViewFactory.ViewPtoType;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.baseline.BaselinePkFkViewConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.php24.Php24PkFkViewConfig;
import edu.alibaba.mpc4j.s2pc.pjc.main.pmap.PmapConfigUtils;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.PlpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.main.plpsi.PlpsiConfigUtils;

import java.util.Properties;

/**
 * @author Feng Han
 * @date 2024/7/23
 */
public class PkFkViewConfigUtils {
    private PkFkViewConfigUtils() {
        // empty
    }

    /**
     * 创建配置项。
     *
     * @param properties 配置参数。
     * @return 配置项。
     */
    static PkFkViewConfig createConfig(Properties properties) {
        // 读取协议类型
        String ptoTypeString = PropertiesUtils.readString(properties, "view_pto_name");
        ViewPtoType viewPtoType = ViewPtoType.valueOf(ptoTypeString);
        switch (viewPtoType) {
            case PHP24:
                return createHpl24PkFkViewConfig(properties);
            case BASELINE:
                return createBaselinePkFkViewConfig(properties);
            default:
                throw new IllegalArgumentException(
                    "Invalid " + ViewPtoType.class.getSimpleName() + ":" + ptoTypeString
                );
        }
    }

    private static PkFkViewConfig createBaselinePkFkViewConfig(Properties properties) {
        boolean silent = PropertiesUtils.readBoolean(properties, "silent", false);
        PlpsiConfig plpsiConfig = PlpsiConfigUtils.createPlPsiConfig(properties);
        return new BaselinePkFkViewConfig.Builder(silent).setPlpsiConfig(plpsiConfig).build();
    }

    private static PkFkViewConfig createHpl24PkFkViewConfig(Properties properties) {
        boolean silent = PropertiesUtils.readBoolean(properties, "silent", false);
        PmapConfig pmapConfig = PmapConfigUtils.createConfig(properties);
        return new Php24PkFkViewConfig.Builder(silent).setPmapConfig(pmapConfig).build();
    }
}
