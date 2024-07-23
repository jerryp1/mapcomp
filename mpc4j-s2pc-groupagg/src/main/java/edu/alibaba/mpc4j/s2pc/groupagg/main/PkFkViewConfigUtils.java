package edu.alibaba.mpc4j.s2pc.groupagg.main;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkViewConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkViewFactory.ViewPtoType;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.baseline.BaselinePkFkViewConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.php24.Php24PkFkViewConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapFactory.PmapPtoType;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.php24.Php24PmapConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.pidbased.PidBasedPmapConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.psibased.PsiBasedPmapConfig;

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
        return new BaselinePkFkViewConfig.Builder(silent).build();
    }

    private static PkFkViewConfig createHpl24PkFkViewConfig(Properties properties) {
        boolean silent = PropertiesUtils.readBoolean(properties, "silent", false);
        String pmapTypeString = PropertiesUtils.readString(properties, "pmap_pto_name", PmapPtoType.PHP24.name());
        PmapPtoType pmapPtoType = PmapPtoType.valueOf(pmapTypeString);
        PmapConfig pmapConfig;
        switch (pmapPtoType){
            case PHP24:
                pmapConfig = new Php24PmapConfig.Builder(silent).build();
                break;
            case PSI_BASED:
                pmapConfig = new PsiBasedPmapConfig.Builder(silent).build();
                break;
            case PID_BASED:
                pmapConfig = new PidBasedPmapConfig.Builder(silent).build();
                break;
            default:
                throw new IllegalArgumentException("Invalid " + PmapPtoType.class.getSimpleName() + ":" + pmapTypeString);
        }
        return new Php24PkFkViewConfig.Builder(silent).setPmapConfig(pmapConfig).build();
    }
}
