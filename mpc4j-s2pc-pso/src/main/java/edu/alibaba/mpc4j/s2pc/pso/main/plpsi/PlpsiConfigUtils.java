package edu.alibaba.mpc4j.s2pc.pso.main.plpsi;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2k.Gf2kDokvsFactory.Gf2kDokvsType;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtFactory.PeqtType;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.cgs22.Cgs22PeqtConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.naive.NaivePeqtConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.PlpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.PlpsiFactory.PlpsiType;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.psty19.Psty19PlpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.rs21.Rs21PlpsiConfig;

import java.util.Properties;

public class PlpsiConfigUtils {
    /**
     * private constructor.
     */
    private PlpsiConfigUtils() {
        // empty
    }

    public static PlpsiConfig createPlPsiConfig(Properties properties) {
        // read PSI type
        String plPsiTypeString = PropertiesUtils.readString(properties, "payload_psi_pto_name");
        PlpsiType psiType = PlpsiType.valueOf(plPsiTypeString);
        switch (psiType) {
            case PSTY19:
                return createPsty19PlPsiConfig(properties);
            case RS21:
                return createRs21PlPsiConfig(properties);
            default:
                throw new IllegalArgumentException("Invalid " + PlpsiType.class.getSimpleName() + ": " + psiType.name());
        }
    }

    private static PlpsiConfig createPsty19PlPsiConfig(Properties properties) {
        boolean silent = PropertiesUtils.readBoolean(properties, "silent", true);
        String cuckooHashTypeString = PropertiesUtils.readString(
            properties, "cuckoo_hash_bin_type", CuckooHashBinType.NAIVE_4_HASH.toString()
        );
        CuckooHashBinType cuckooHashBinType = CuckooHashBinType.valueOf(cuckooHashTypeString);
        String peqtTypeString = PropertiesUtils.readString(
            properties, "peqt_type", PeqtType.NAIVE.toString()
        );
        PeqtConfig peqtConfig;
        if (peqtTypeString.equals(PeqtType.CGS22.toString())) {
            peqtConfig = new Cgs22PeqtConfig.Builder(SecurityModel.SEMI_HONEST, silent).build();
        } else {
            peqtConfig = new NaivePeqtConfig.Builder(SecurityModel.SEMI_HONEST, silent).build();
        }
        return new Psty19PlpsiConfig.Builder(silent)
            .setCuckooHashBinType(cuckooHashBinType)
            .setPeqtConfig(peqtConfig)
            .build();
    }

    private static PlpsiConfig createRs21PlPsiConfig(Properties properties) {
        boolean silent = PropertiesUtils.readBoolean(properties, "silent", true);
        String cuckooHashTypeString = PropertiesUtils.readString(
            properties, "cuckoo_hash_bin_type", CuckooHashBinType.NAIVE_4_HASH.toString()
        );
        CuckooHashBinType cuckooHashBinType = CuckooHashBinType.valueOf(cuckooHashTypeString);
        String okvsTypeString = PropertiesUtils.readString(
            properties, "okvs_type", Gf2kDokvsType.H3_CLUSTER_FIELD_BLAZE_GCT.toString()
        );
        Gf2kDokvsType okvsType = Gf2kDokvsType.valueOf(okvsTypeString);
        String peqtTypeString = PropertiesUtils.readString(
            properties, "peqt_type", PeqtType.NAIVE.toString()
        );
        PeqtConfig peqtConfig;
        if (peqtTypeString.equals(PeqtType.CGS22.toString())) {
            peqtConfig = new Cgs22PeqtConfig.Builder(SecurityModel.SEMI_HONEST, silent).build();
        } else {
            peqtConfig = new NaivePeqtConfig.Builder(SecurityModel.SEMI_HONEST, silent).build();
        }
        return new Rs21PlpsiConfig.Builder(silent)
            .setCuckooHashBinType(cuckooHashBinType)
            .setOkveType(okvsType)
            .setPeqtConfig(peqtConfig)
            .build();
    }
}
