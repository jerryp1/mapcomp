package edu.alibaba.mpc4j.s2pc.pso.main.psi;

import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory.OprfType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.psz14.Psz14OriOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.ra17.Ra17EccSqOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory;
import edu.alibaba.mpc4j.s2pc.pso.psi.cm20.Cm20PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.czz22.Czz22PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.gmr21.Gmr21PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.hfh99.Hfh99ByteEccPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.hfh99.Hfh99EccPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.kkrt16.Kkrt16PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.prty19.Prty19FastPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.prty20.Prty20PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.psz14.Psz14GbfPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.psz14.Psz14PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.ra17.Ra17PsiConfig;

import java.util.Properties;

public class PsiConfigUtils {

    private PsiConfigUtils() {
        // empty
    }

    public static PsiConfig createPsiConfig(Properties properties) {
        // 读取协议类型
        String psiTypeString = PropertiesUtils.readString(properties, "psi_pto_name");
        PsiFactory.PsiType psiType = PsiFactory.PsiType.valueOf(psiTypeString);
        switch (psiType) {
            case HFH99_ECC:
                return createHfh99EccPsiConfig(properties);
            case HFH99_BYTE_ECC:
                return createHfh99ByteEccPsiConfig();
            case KKRT16:
                return createKkrt16PsiConfig(properties);
            case CM20:
                return createCm20PsiConfig();
            case RA17:
                return createRa17PsiConfig(properties);
            case PRTY20:
                return createPrty20PsiConfig(properties);
            case PRTY19_FAST:
                return createPrty19FastPsiConfig(properties);
            case GMR21:
                return createGmr21PsiConfig();
            case CZZ22:
                return createCzz22PsiConfig();
            case PSZ14:
                return createPsz14PsiConfig(properties);
            case PSZ14_GBF:
                return createPsz14GbfPsiConfig();
            default:
                throw new IllegalArgumentException("Invalid " + PsiFactory.PsiType.class.getSimpleName() + ": " + psiType.name());
        }
    }

    private static PsiConfig createHfh99EccPsiConfig(Properties properties) {
        // 是否使用压缩编码
        boolean compressEncode = PropertiesUtils.readBoolean(properties, "compress_encode", true);
        return new Hfh99EccPsiConfig.Builder().setCompressEncode(compressEncode).build();
    }

    private static PsiConfig createHfh99ByteEccPsiConfig() {
        return new Hfh99ByteEccPsiConfig.Builder().build();
    }

    private static PsiConfig createKkrt16PsiConfig(Properties properties) {
        // 布谷鸟哈希类型
        String cuckooHashTypeString = PropertiesUtils.readString(properties, "cuckoo_hash_bin_type",
                CuckooHashBinFactory.CuckooHashBinType.NO_STASH_NAIVE.toString());
        CuckooHashBinFactory.CuckooHashBinType cuckooHashBinType = CuckooHashBinFactory.CuckooHashBinType.valueOf(cuckooHashTypeString);
        return new Kkrt16PsiConfig.Builder().setCuckooHashBinType(cuckooHashBinType).build();
    }

    private static PsiConfig createCm20PsiConfig() {
        return new Cm20PsiConfig.Builder().build();
    }

    private static PsiConfig createRa17PsiConfig(Properties properties) {
        String eccTypeString = PropertiesUtils.readString(properties, "ecc_type", "BYTE_ECC");
        switch (eccTypeString){
            case "BYTE_ECC": return new Ra17PsiConfig.Builder().build();
            case "ECC" : return new Ra17PsiConfig.Builder().setSqOprfConfig(new Ra17EccSqOprfConfig.Builder().build()).build();
            default: throw new IllegalArgumentException("Invalid eccTypeString in RA17-PSI:" + eccTypeString);
        }
    }

    private static PsiConfig createPrty20PsiConfig(Properties properties) {
        // OKVS类型
        String okvsTypeString = PropertiesUtils.readString(properties, "okvs_type",
                Gf2eDokvsType.H2_SINGLETON_GCT.toString());
        Gf2eDokvsType okvsType = Gf2eDokvsType.valueOf(okvsTypeString);
        return new Prty20PsiConfig.Builder().setBinaryOkvsType(okvsType).build();
    }

    private static PsiConfig createPrty19FastPsiConfig(Properties properties) {
        String okvsTypeString = PropertiesUtils.readString(properties, "okvs_type",
            Gf2eDokvsType.H3_NAIVE_CLUSTER_BLAZE_GCT.toString());
        Gf2eDokvsType okvsType = Gf2eDokvsType.valueOf(okvsTypeString);
        return new Prty19FastPsiConfig.Builder().setOkvsType(okvsType).build();
    }

    private static PsiConfig createGmr21PsiConfig() {
        return new Gmr21PsiConfig.Builder().build();
    }

    private static PsiConfig createCzz22PsiConfig() {
        return new Czz22PsiConfig.Builder().build();
    }

    private static PsiConfig createPsz14PsiConfig(Properties properties) {
        String oprfTypeString = PropertiesUtils.readString(properties, "oprf_type",
            OprfType.PSZ14_OPT.toString());
        switch (oprfTypeString){
            case "PSZ14_OPT": return new Psz14PsiConfig.Builder().build();
            case "PSZ14_ORI" : return new Psz14PsiConfig.Builder().setOprfConfig(new Psz14OriOprfConfig.Builder().build()).build();
            default: throw new IllegalArgumentException("Invalid eccTypeString in PSZ14-PSI:" + oprfTypeString);
        }
    }

    private static PsiConfig createPsz14GbfPsiConfig() {
        return new Psz14GbfPsiConfig.Builder().build();
    }
}
