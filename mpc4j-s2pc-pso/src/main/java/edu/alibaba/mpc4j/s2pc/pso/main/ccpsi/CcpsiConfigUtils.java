package edu.alibaba.mpc4j.s2pc.pso.main.ccpsi;

import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiFactory.CcpsiType;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.cgs22.Cgs22CcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.psty19.Psty19CcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.rs21.Rs21CcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiConfig;

import java.util.Properties;
/**
 * CCPSI config utilities.
 *
 * @author Feng Han
 * @date 2023/10/10
 */
public class CcpsiConfigUtils {
    /**
     * private constructor.
     */
    private CcpsiConfigUtils() {
        // empty
    }

    public static CcpsiConfig createCPsiConfig(Properties properties) {
        // read PSI type
        String cPsiTypeString = PropertiesUtils.readString(properties, "circuit_psi_pto_name");
        CcpsiType psiType = CcpsiType.valueOf(cPsiTypeString);
        switch (psiType) {
            case PSTY19:
                return createCgs22CcPsiConfig(properties);
            case RS21:
                return createPsty19CcPsiConfig(properties);
            case CGS22:
                return createRs21CcPsiConfig(properties);
            default:
                throw new IllegalArgumentException("Invalid " + CcpsiType.class.getSimpleName() + ": " + psiType.name());
        }
    }

    private static CcpsiConfig createCgs22CcPsiConfig(Properties properties) {
        boolean silent = PropertiesUtils.readBoolean(properties, "silent", true);
        String cuckooHashTypeString = PropertiesUtils.readString(
            properties, "cuckoo_hash_bin_type", CuckooHashBinType.NAIVE_4_HASH.toString()
        );
        CuckooHashBinType cuckooHashBinType = CuckooHashBinType.valueOf(cuckooHashTypeString);
        return new Cgs22CcpsiConfig.Builder(silent).setCuckooHashBinType(cuckooHashBinType).build();
    }
    private static CcpsiConfig createPsty19CcPsiConfig(Properties properties) {
        boolean silent = PropertiesUtils.readBoolean(properties, "silent", true);
        String cuckooHashTypeString = PropertiesUtils.readString(
            properties, "cuckoo_hash_bin_type", CuckooHashBinType.NAIVE_4_HASH.toString()
        );
        CuckooHashBinType cuckooHashBinType = CuckooHashBinType.valueOf(cuckooHashTypeString);
        return new Psty19CcpsiConfig.Builder(silent).setCuckooHashBinType(cuckooHashBinType).build();
    }
    private static CcpsiConfig createRs21CcPsiConfig(Properties properties) {
        boolean silent = PropertiesUtils.readBoolean(properties, "silent", true);
        String cuckooHashTypeString = PropertiesUtils.readString(
            properties, "cuckoo_hash_bin_type", CuckooHashBinType.NAIVE_4_HASH.toString()
        );
        CuckooHashBinType cuckooHashBinType = CuckooHashBinType.valueOf(cuckooHashTypeString);
        return new Rs21CcpsiConfig.Builder(silent).setCuckooHashBinType(cuckooHashBinType).build();
    }

}
