package edu.alibaba.mpc4j.s2pc.pjc.main.pmap;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtFactory.PeqtType;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.cgs22.Cgs22PeqtConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.naive.NaivePeqtConfig;
import edu.alibaba.mpc4j.s2pc.pjc.main.pid.PidConfigUtils;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidFactory;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapFactory.PmapPtoType;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.php24.Php24PmapConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.pidbased.PidBasedPmapConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.psibased.PsiBasedPmapConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.PlpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.PlpsiFactory.PlpsiType;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.psty19.Psty19PlpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.rs21.Rs21PlpsiConfig;

import java.util.Properties;

/**
 * PMAP协议配置项工具类。
 *
 * @author Feng Han
 * @date 2023/11/20
 */
public class PmapConfigUtils {

    private PmapConfigUtils() {
        // empty
    }

    /**
     * 创建配置项。
     *
     * @param properties 配置参数。
     * @return 配置项。
     */
    static PmapConfig createConfig(Properties properties) {
        // 读取协议类型
        String pmapTypeString = PropertiesUtils.readString(properties, "pmap_pto_name");
        PmapPtoType pmapPtoType = PmapPtoType.valueOf(pmapTypeString);
        switch (pmapPtoType) {
            case PHP24:
                return createHpl24PmapConfig(properties);
            case PID_BASED:
                return createPidBasedPmapConfig(properties);
            case PSI_BASED:
                return createPsiBasedPmapConfig(properties);
            default:
                throw new IllegalArgumentException(
                    "Invalid " + PidFactory.PidType.class.getSimpleName() + ":" + pmapTypeString
                );
        }
    }

    private static PsiBasedPmapConfig createPsiBasedPmapConfig(Properties properties) {
        // 是否使用压缩编码
        boolean silent = PropertiesUtils.readBoolean(properties, "silent", false);
        return new PsiBasedPmapConfig.Builder(silent).build();
    }

    private static PidBasedPmapConfig createPidBasedPmapConfig(Properties properties) {
        // 是否使用压缩编码
        boolean silent = PropertiesUtils.readBoolean(properties, "silent", false);

        PidConfig pidConfig = PidConfigUtils.createConfig(properties);
        String peqtTypeString = PropertiesUtils.readString(properties, "peqtType", PeqtType.NAIVE.name());
        PeqtConfig peqtConfig;
        if(peqtTypeString.equals(PeqtType.CGS22.toString())){
            peqtConfig = new Cgs22PeqtConfig.Builder(SecurityModel.SEMI_HONEST, silent).build();
        }else{
            peqtConfig = new NaivePeqtConfig.Builder(SecurityModel.SEMI_HONEST, silent).build();
        }
        return new PidBasedPmapConfig.Builder(silent).setPidConfig(pidConfig).setPeqtConfig(peqtConfig).build();
    }

    private static Php24PmapConfig createHpl24PmapConfig(Properties properties) {
        // 是否使用压缩编码
        boolean silent = PropertiesUtils.readBoolean(properties, "silent", false);
        int bitLen = PropertiesUtils.readIntWithDefault(properties, "bitLen", 32);
        String peqtTypeString = PropertiesUtils.readString(properties, "peqtType", PeqtType.NAIVE.name());
        PeqtConfig peqtConfig;
        if(peqtTypeString.equals(PeqtType.CGS22.toString())){
            peqtConfig = new Cgs22PeqtConfig.Builder(SecurityModel.SEMI_HONEST, silent).build();
        }else{
            peqtConfig = new NaivePeqtConfig.Builder(SecurityModel.SEMI_HONEST, silent).build();
        }
        String cPsiTypeString = PropertiesUtils.readString(properties, "payload_psi_pto_name", PlpsiType.RS21.name());
        PlpsiConfig plpsiConfig;
        if(cPsiTypeString.equals(PlpsiType.PSTY19.name())){
            plpsiConfig = new Psty19PlpsiConfig.Builder(silent).setPeqtConfig(peqtConfig).build();
        }else{
            plpsiConfig = new Rs21PlpsiConfig.Builder(silent).setPeqtConfig(peqtConfig).build();
        }
        return new Php24PmapConfig.Builder(silent).setPlpsiconfig(plpsiConfig).setBitLength(bitLen, silent).build();
    }
}
