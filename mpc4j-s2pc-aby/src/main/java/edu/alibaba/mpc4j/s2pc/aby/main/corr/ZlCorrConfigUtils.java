package edu.alibaba.mpc4j.s2pc.aby.main.corr;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.ZlCorrConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.ZlCorrFactory.ZlCorrType;
import edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.gp23.Gp23ZlCorrConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.rrk20.Rrk20ZlCorrConfig;

import java.util.Properties;

/**
 * Zl Corr config utils.
 * 
 * @author Liqiang Peng
 * @date 2023/10/12
 */
public class ZlCorrConfigUtils {
    
    private ZlCorrConfigUtils() {
        // empty
    }

    /**
     * create config.
     *
     * @param properties properties.
     * @return config.
     */
    public static ZlCorrConfig createZlCorrConfig(Properties properties) {
        String zlCorrTypeString = PropertiesUtils.readString(properties, "pto_name");
        ZlCorrType zlCorrType = ZlCorrType.valueOf(zlCorrTypeString);
        boolean silent = PropertiesUtils.readBoolean(properties, "silent");
        int l = PropertiesUtils.readInt(properties, "zl");
        Zl zl = ZlFactory.createInstance(EnvType.STANDARD, l);
        switch (zlCorrType) {
            case GP23:
                return new Gp23ZlCorrConfig.Builder(silent).build();
            case RRK20:
                return new Rrk20ZlCorrConfig.Builder(zl, silent).build();
            default:
                throw new IllegalArgumentException("Invalid " + ZlCorrType.class.getSimpleName() + ": " + zlCorrType.name());
        }
    }
}