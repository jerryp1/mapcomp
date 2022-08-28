package edu.alibaba.mpc4j.s2pc.pso.main.pid;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory.OkvsType;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.oprf.MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprf.cm20.Cm20MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprf.ra17.Ra17MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.pid.PidConfig;
import edu.alibaba.mpc4j.s2pc.pso.pid.bkms20.Bkms20PidConfig;
import edu.alibaba.mpc4j.s2pc.pso.pid.gmr21.Gmr21MpPidConfig;
import edu.alibaba.mpc4j.s2pc.pso.pid.gmr21.Gmr21SloppyPidConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.gmr21.Gmr21PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pso.pid.PidFactory;

import java.util.Properties;

/**
 * PID协议配置项工具类。
 *
 * @author Weiran Liu
 * @date 2022/5/16
 */
public class PidConfigUtils {

    private PidConfigUtils() {
        // empty
    }

    /**
     * 创建配置项。
     *
     * @param properties 配置参数。
     * @return 配置项。
     */
    static PidConfig createConfig(Properties properties) {
        // 读取协议类型
        String pidTypeString = PropertiesUtils.readString(properties, "pto_name");
        PidFactory.PidType pidType = PidFactory.PidType.valueOf(pidTypeString);
        switch (pidType) {
            case BKMS20:
                return createBkms20PidConfig(properties);
            case GMR21_MP:
                return createGmr21MpPidConfig(properties);
            case GMR21_SLOPPY:
                return createGmr21SloppyPidConfig(properties);
            default:
                throw new IllegalArgumentException(
                    "Invalid " + PidFactory.PidType.class.getSimpleName() + ":" + pidTypeString
                );
        }
    }

    private static Bkms20PidConfig createBkms20PidConfig(Properties properties) {
        // 是否使用压缩编码
        boolean compressEncode = PropertiesUtils.readBoolean(properties, "compress_encode");

        return new Bkms20PidConfig.Builder()
            .setCompressEncode(compressEncode)
            .build();
    }

    private static Gmr21MpPidConfig createGmr21MpPidConfig(Properties properties) {
        // 多点OPRF类型
        String mpOprfTypeString = PropertiesUtils.readString(properties, "mp_oprf_type");
        OprfFactory.OprfType mpOprfType = OprfFactory.OprfType.valueOf(mpOprfTypeString);
        MpOprfConfig mpOprfConfig;
        switch (mpOprfType) {
            case CM20:
                mpOprfConfig = new Cm20MpOprfConfig.Builder().build();
                break;
            case RA17:
                mpOprfConfig = new Ra17MpOprfConfig.Builder().build();
                break;
            default:
                throw new IllegalArgumentException(
                    "Invalid " + OprfFactory.OprfType.class.getSimpleName() + ": " + mpOprfTypeString
                );
        }
        PsuConfig psuConfig = new Gmr21PsuConfig.Builder().build();

        return new Gmr21MpPidConfig.Builder()
            .setMpOprfConfig(mpOprfConfig)
            .setPsuConfig(psuConfig)
            .build();
    }

    private static Gmr21SloppyPidConfig createGmr21SloppyPidConfig(Properties properties) {
        // Sloppy的OKVS类型
        String sloppyOkvsTypeString = PropertiesUtils.readString(properties, "sloppy_okvs_type");
        OkvsType sloppyOkvsType = OkvsType.valueOf(sloppyOkvsTypeString);
        PsuConfig psuConfig = new Gmr21PsuConfig.Builder().build();
        return new Gmr21SloppyPidConfig.Builder()
            .setSloppyOkvsType(sloppyOkvsType)
            .setPsuConfig(psuConfig)
            .build();
    }
}
