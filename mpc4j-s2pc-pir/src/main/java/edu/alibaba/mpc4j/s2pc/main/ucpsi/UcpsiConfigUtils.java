package edu.alibaba.mpc4j.s2pc.main.ucpsi;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.UcpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.cgs22.Cgs22UcpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19.Psty19UcpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.peqt.Sj23PeqtUcpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.pmt.Sj23PmtUcpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.pir.PirUbopprfConfig;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.pir.PirUrbopprfConfig;

import java.util.Properties;

/**
 * UCPSI config utils.
 * 
 * @author Liqiang Peng
 * @date 2023/4/23
 */
public class UcpsiConfigUtils {
    
    private UcpsiConfigUtils() {
        // empty
    }

    /**
     * create config.
     *
     * @param properties properties.
     * @return config.
     */
    public static UcpsiConfig createUcpsiConfig(Properties properties) {
        String ucpsiTypeString = PropertiesUtils.readString(properties, "pto_name");
        UcpsiType ucpsiType = UcpsiType.valueOf(ucpsiTypeString);
        boolean silent = PropertiesUtils.readBoolean(properties, "silent");
        switch (ucpsiType) {
            case CGS22_OKVS:
                return createCgs22UcpsiOkvsConfig(silent);
            case CGS22_PIR:
                return createCgs22UcpsiPirConfig(silent);
            case PSTY19_OKVS:
                return createPsty19UcpsiOkvsConfig(silent);
            case PSTY19_PIR:
                return createPsty19UcpsiPirConfig(silent);
            case SJ23_PEQT:
                return createSj23UcpsiPeqtConfig(silent);
            case SJ23_PSM:
                return createSj23UcpsiPmtConfig(silent);
            default:
                throw new IllegalArgumentException("Invalid " + UcpsiType.class.getSimpleName() + ": " + ucpsiType.name());
        }
    }

    private static UcpsiConfig createPsty19UcpsiOkvsConfig(boolean silent) {
        return new Psty19UcpsiConfig.Builder(SecurityModel.SEMI_HONEST, silent).build();
    }

    private static UcpsiConfig createPsty19UcpsiPirConfig(boolean silent) {
        return new Psty19UcpsiConfig.Builder(SecurityModel.SEMI_HONEST, silent)
            .setUbopprfConfig(new PirUbopprfConfig.Builder().build())
            .build();
    }

    private static UcpsiConfig createCgs22UcpsiOkvsConfig(boolean silent) {
        return new Cgs22UcpsiConfig.Builder(SecurityModel.SEMI_HONEST, silent).build();
    }

    private static UcpsiConfig createCgs22UcpsiPirConfig(boolean silent) {
        return new Cgs22UcpsiConfig.Builder(SecurityModel.SEMI_HONEST, silent)
            .setUrbopprfConfig(new PirUrbopprfConfig.Builder().build())
            .build();
    }

    private static UcpsiConfig createSj23UcpsiPeqtConfig(boolean silent) {
        return new Sj23PeqtUcpsiConfig.Builder(SecurityModel.SEMI_HONEST, silent).build();
    }

    private static UcpsiConfig createSj23UcpsiPmtConfig(boolean silent) {
        return new Sj23PmtUcpsiConfig.Builder(SecurityModel.SEMI_HONEST, silent).build();
    }
}
