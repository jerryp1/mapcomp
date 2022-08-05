package edu.alibaba.mpc4j.s2pc.pso.main.psu;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory.OkvsType;
import edu.alibaba.mpc4j.common.tool.okve.ovdm.ecc.EccOvdmFactory.EccOvdmType;
import edu.alibaba.mpc4j.common.tool.okve.ovdm.gf2e.Gf2eOvdmFactory.Gf2eOvdmType;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcConfig;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcFactory;
import edu.alibaba.mpc4j.s2pc.aby.bc.bea91.Bea91BcConfig;
import edu.alibaba.mpc4j.s2pc.pcg.btg.BtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.btg.impl.file.FileBtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.cache.CacheCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.rto.RtoCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.RcotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.RcotFactory;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprf.cm20.Cm20MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprf.kkrt16.Kkrt16OptOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprp.OprpConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprp.OprpFactory;
import edu.alibaba.mpc4j.s2pc.pso.oprp.lowmc.LowMcOprpConfig;
import edu.alibaba.mpc4j.s2pc.pso.osn.OsnConfig;
import edu.alibaba.mpc4j.s2pc.pso.osn.gmr21.Gmr21OsnConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory.PsuType;
import edu.alibaba.mpc4j.s2pc.pso.psu.gmr21.Gmr21PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.jsz22.Jsz22SfcPsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.jsz22.Jsz22SfsPsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.krtw19.Krtw19OptPsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.krtw19.Krtw19OriPsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.zcl22.Zcl22PkePsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.zcl22.Zcl22SkePsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfFactory;

import java.util.Properties;

/**
 * PSU协议配置项工具类。
 *
 * @author Weiran Liu
 * @date 2022/02/16
 */
public class PsuConfigUtils {

    private PsuConfigUtils() {
        // empty
    }

    /**
     * 创建配置项。
     *
     * @param properties 配置参数。
     * @return 配置项。
     */
    static PsuConfig createPsuConfig(Properties properties) {
        // 读取协议类型
        String psuTypeString = Preconditions.checkNotNull(
            properties.getProperty("pto_name"), "Please set pto_name"
        );
        PsuType psuType = PsuType.valueOf(psuTypeString);
        switch (psuType) {
            case KRTW19_ORI:
                return createKrtw19OriPsuConfig(properties);
            case KRTW19_OPT:
                return createKrtw19OptPsuConfig();
            case GMR21:
                return generateGmr21PsuConfig(properties);
            case ZCL22_PKE:
                return createZcl22PkePsuConfig(properties);
            case ZCL22_SKE:
                return createZcl22SkePsuConfig(properties);
            case JSZ22_SFC:
                return createJsz22SfcPsuConfig(properties);
            case JSZ22_SFS:
                return createJsz22SfsPsuConfig(properties);
            default:
                throw new IllegalArgumentException("Invalid PsuType: " + psuType.name());
        }
    }

    private static PsuConfig createKrtw19OriPsuConfig(Properties properties) {
        // OKVS类型
        String okvsTypeString = Preconditions.checkNotNull(
            properties.getProperty("okvs_type"), "Please set okvs_type"
        );
        OkvsType okvsType = OkvsType.valueOf(okvsTypeString);

        return new Krtw19OriPsuConfig.Builder()
            .setRcotConfig(RcotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST))
            .setOkvsType(okvsType)
            .build();
    }

    private static Krtw19OptPsuConfig createKrtw19OptPsuConfig() {
        return new Krtw19OptPsuConfig.Builder()
            .setRcotConfig(RcotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST))
            .build();
    }

    private static Gmr21PsuConfig generateGmr21PsuConfig(Properties properties) {
        // OKVS类型
        String okvsTypeString = Preconditions.checkNotNull(
            properties.getProperty("okvs_type"), "Please set okvs_type"
        );
        OkvsType okvsType = OkvsType.valueOf(okvsTypeString);
        // 是否使用安静OT
        boolean silentCot = Boolean.parseBoolean(Preconditions.checkNotNull(
            properties.getProperty("silent_cot"), "Please set silent_cot"
        ));
        OsnConfig osnConfig = silentCot
            ? new Gmr21OsnConfig.Builder()
                .setCotConfig(new CacheCotConfig.Builder(SecurityModel.SEMI_HONEST).build())
                .build()
            : new Gmr21OsnConfig.Builder()
                .setCotConfig(new RtoCotConfig.Builder(SecurityModel.SEMI_HONEST).build())
                .build();
        RcotConfig rcotConfig = RcotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);

        return new Gmr21PsuConfig.Builder()
            .setRcotConfig(rcotConfig)
            .setOsnConfig(osnConfig)
            .setOkvsType(okvsType)
            .build();
    }

    private static Zcl22SkePsuConfig createZcl22SkePsuConfig(Properties properties) {
        // OVDM类型
        String gf2eOvdmTypeString = Preconditions.checkNotNull(
            properties.getProperty("gf2e_ovdm_type"), "Please set gf2e_ovdm_type"
        );
        Gf2eOvdmType gf2eOvdmType = Gf2eOvdmType.valueOf(gf2eOvdmTypeString);
        boolean ignoreBtg = Boolean.parseBoolean(Preconditions.checkNotNull(
            properties.getProperty("ignore_btg"), "Please set ignore_btg"
        ));
        if (ignoreBtg) {
            BtgConfig fileBtgConfig = new FileBtgConfig.Builder(SecurityModel.SEMI_HONEST).build();
            BcConfig fileBcConfig = new Bea91BcConfig.Builder()
                .setBtgConfig(fileBtgConfig)
                .build();
            OprpConfig fileOprpConfig = new LowMcOprpConfig.Builder()
                .setBcConfig(fileBcConfig)
                .build();
            return new Zcl22SkePsuConfig.Builder()
                .setRcotConfig(RcotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST))
                .setOprpConfig(fileOprpConfig)
                .setBcConfig(fileBcConfig)
                .setGf2eOvdmType(gf2eOvdmType)
                .build();
        } else {
            return new Zcl22SkePsuConfig.Builder()
                .setRcotConfig(RcotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST))
                .setOprpConfig(OprpFactory.createDefaultConfig(SecurityModel.SEMI_HONEST))
                .setBcConfig(BcFactory.createDefaultConfig(SecurityModel.SEMI_HONEST))
                .setGf2eOvdmType(gf2eOvdmType)
                .build();
        }
    }

    private static Zcl22PkePsuConfig createZcl22PkePsuConfig(Properties properties) {
        // OVDM类型
        String eccOvdmTypeString = Preconditions.checkNotNull(
            properties.getProperty("ecc_ovdm_type"), "Please set ecc_odvm_type"
        );
        EccOvdmType eccOvdmType = EccOvdmType.valueOf(eccOvdmTypeString);
        // 是否使用压缩编码
        boolean compressEncode = Boolean.parseBoolean(Preconditions.checkNotNull(
            properties.getProperty("compress_encode"), "Please set compress_encode"
        ));

        return new Zcl22PkePsuConfig.Builder()
            .setRcotConfig(RcotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST))
            .setCompressEncode(compressEncode)
            .setEccOvdmType(eccOvdmType)
            .build();
    }

    private static Jsz22SfcPsuConfig createJsz22SfcPsuConfig(Properties properties) {
        // OPRF类型
        String oprfTypeString = Preconditions.checkNotNull(
            properties.getProperty("oprf_type"), "Please set oprf_type"
        );
        OprfFactory.OprfType oprfType = OprfFactory.OprfType.valueOf(oprfTypeString);
        OprfConfig oprfConfig;
        switch (oprfType) {
            case KKRT16_OPT:
                oprfConfig = new Kkrt16OptOprfConfig.Builder().build();
                break;
            case CM20:
                oprfConfig = new Cm20MpOprfConfig.Builder().build();
                break;
            default:
                throw new IllegalArgumentException("JSZ22_SFC_PSU does not support OprfType: " + oprfType);
        }
        // 布谷鸟哈希类型
        String cuckooHashTypeString = Preconditions.checkNotNull(
            properties.getProperty("cuckoo_hash_bin_type"), "Please set cuckoo_hash_bin_type"
        );
        CuckooHashBinType cuckooHashBinType = CuckooHashBinType.valueOf(cuckooHashTypeString);
        // 是否使用安静OT
        boolean silentCot = Boolean.parseBoolean(Preconditions.checkNotNull(
            properties.getProperty("silent_cot"), "Please set silent_cot"
        ));
        OsnConfig osnConfig = silentCot
            ? new Gmr21OsnConfig.Builder()
                .setCotConfig(new CacheCotConfig.Builder(SecurityModel.SEMI_HONEST).build())
                .build()
            : new Gmr21OsnConfig.Builder()
                .setCotConfig(new RtoCotConfig.Builder(SecurityModel.SEMI_HONEST).build())
                .build();
        RcotConfig rcotConfig = RcotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);

        return new Jsz22SfcPsuConfig.Builder()
            .setRcotConfig(rcotConfig)
            .setOsnConfig(osnConfig)
            .setOprfConfig(oprfConfig)
            .setCuckooHashBinType(cuckooHashBinType)
            .build();
    }

    private static Jsz22SfsPsuConfig createJsz22SfsPsuConfig(Properties properties) {
        // OPRF类型
        String oprfTypeString = Preconditions.checkNotNull(
            properties.getProperty("oprf_type"), "Please set oprf_type"
        );
        OprfFactory.OprfType oprfType = OprfFactory.OprfType.valueOf(oprfTypeString);
        OprfConfig oprfConfig;
        switch (oprfType) {
            case KKRT16_OPT:
                oprfConfig = new Kkrt16OptOprfConfig.Builder().build();
                break;
            case CM20:
                oprfConfig = new Cm20MpOprfConfig.Builder().build();
                break;
            default:
                throw new IllegalArgumentException("JSZ22_SFC_PSU does not support OprfType: " + oprfType);
        }
        // 布谷鸟哈希类型
        String cuckooHashTypeString = Preconditions.checkNotNull(
            properties.getProperty("cuckoo_hash_bin_type"), "Please set cuckoo_hash_bin_type"
        );
        CuckooHashBinType cuckooHashBinType = CuckooHashBinType.valueOf(cuckooHashTypeString);
        // 是否使用安静OT
        boolean silentCot = Boolean.parseBoolean(Preconditions.checkNotNull(
            properties.getProperty("silent_cot"), "Please set silent_cot"
        ));
        OsnConfig osnConfig = silentCot
            ? new Gmr21OsnConfig.Builder()
            .setCotConfig(new CacheCotConfig.Builder(SecurityModel.SEMI_HONEST).build())
            .build()
            : new Gmr21OsnConfig.Builder()
                .setCotConfig(new RtoCotConfig.Builder(SecurityModel.SEMI_HONEST).build())
                .build();

        return new Jsz22SfsPsuConfig.Builder()
            .setOsnConfig(osnConfig)
            .setOprfConfig(oprfConfig)
            .setCuckooHashBinType(cuckooHashBinType)
            .build();
    }
}
