package edu.alibaba.mpc4j.s2pc.pso.main.psi;

import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory.PsiType;
import edu.alibaba.mpc4j.s2pc.pso.psi.cuckoo.oos17.Oos17PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.cuckoo.psz14.Psz14PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.dcw13.Dcw13PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.cm20.Cm20PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt.czz22.Czz22PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt.gmr21.Gmr21PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.pke.hfh99.Hfh99ByteEccPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.pke.hfh99.Hfh99EccPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.cuckoo.kkrt16.Kkrt16PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.prty19.Prty19FastPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.prty19.Prty19LowPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.prty20.Prty20SmPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.sqoprf.ra17.Ra17ByteEccPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.sqoprf.ra17.Ra17EccPsiConfig;

import java.util.Properties;

/**
 * PSI config utilities.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/11
 */
public class PsiConfigUtils {
    /**
     * private constructor.
     */
    private PsiConfigUtils() {
        // empty
    }

    public static PsiConfig createPsiConfig(Properties properties) {
        // read PSI type
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
            case RA17_ECC:
                return createRa17EccPsiConfig();
            case RA17_BYTE_ECC:
                return createRa17ByteEccPsiConfig();
            case PRTY20_SEMI_HONEST:
                return createPrty20SmPsiConfig(properties);
            case PRTY19_LOW:
                return createPrty19LowPsiConfig(properties);
            case PRTY19_FAST:
                return createPrty19FastPsiConfig();
            case GMR21:
                return createGmr21PsiConfig(properties);
            case CZZ22:
                return createCzz22PsiConfig();
            case PSZ14:
                return createPsz14PsiConfig();
            case DCW13:
                return createDcw13PsiConfig();
            case OOS17:
                return createOos17PsiConfig();
            default:
                throw new IllegalArgumentException("Invalid " + PsiType.class.getSimpleName() + ": " + psiType.name());
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
        String cuckooHashTypeString = PropertiesUtils.readString(properties, "cuckoo_hash_bin_type",
            CuckooHashBinFactory.CuckooHashBinType.NO_STASH_NAIVE.toString());
        CuckooHashBinFactory.CuckooHashBinType cuckooHashBinType = CuckooHashBinFactory.CuckooHashBinType.valueOf(cuckooHashTypeString);
        return new Kkrt16PsiConfig.Builder().setCuckooHashBinType(cuckooHashBinType).build();
    }

    private static PsiConfig createCm20PsiConfig() {
        return new Cm20PsiConfig.Builder().build();
    }

    private static PsiConfig createRa17EccPsiConfig() {
        return new Ra17EccPsiConfig.Builder().build();
    }

    private static PsiConfig createRa17ByteEccPsiConfig() {
        return new Ra17ByteEccPsiConfig.Builder().build();
    }

    private static PsiConfig createPrty20SmPsiConfig(Properties properties) {
        String okvsTypeString = PropertiesUtils.readString(
            properties, "okvs_type", Gf2eDokvsType.H2_SINGLETON_GCT.toString()
        );
        Gf2eDokvsType okvsType = Gf2eDokvsType.valueOf(okvsTypeString);
        return new Prty20SmPsiConfig.Builder().setPaxosType(okvsType).build();
    }

    private static PsiConfig createPrty19LowPsiConfig(Properties properties) {
        String okvsTypeString = PropertiesUtils.readString(
            properties, "okvs_type", Gf2eDokvsType.H3_NAIVE_CLUSTER_BLAZE_GCT.toString()
        );
        Gf2eDokvsType okvsType = Gf2eDokvsType.valueOf(okvsTypeString);
        return new Prty19LowPsiConfig.Builder().setOkvsType(okvsType).build();
    }

    private static PsiConfig createPrty19FastPsiConfig() {
        return new Prty19FastPsiConfig.Builder().build();
    }

    private static PsiConfig createGmr21PsiConfig(Properties properties) {
        boolean silent = PropertiesUtils.readBoolean(properties, "silent", false);
        return new Gmr21PsiConfig.Builder(silent).build();
    }

    private static PsiConfig createCzz22PsiConfig() {
        return new Czz22PsiConfig.Builder().build();
    }

    private static PsiConfig createPsz14PsiConfig() {
        return new Psz14PsiConfig.Builder().build();
    }

    private static PsiConfig createDcw13PsiConfig() {
        return new Dcw13PsiConfig.Builder().build();
    }

    private static PsiConfig createOos17PsiConfig() {
        return new Oos17PsiConfig.Builder().build();
    }
}
