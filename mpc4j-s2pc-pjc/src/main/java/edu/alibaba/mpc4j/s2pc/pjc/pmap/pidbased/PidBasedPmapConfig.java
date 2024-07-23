package edu.alibaba.mpc4j.s2pc.pjc.pmap.pidbased;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.naive.NaivePeqtConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pid.gmr21.Gmr21SloppyPidConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapFactory.PmapPtoType;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.jsz22.Jsz22SfcPsuConfig;

/**
 * PID-based map configure
 *
 * @author Feng Han
 * @date 2023/11/20
 */
public class PidBasedPmapConfig extends AbstractMultiPartyPtoConfig implements PmapConfig {
    /**
     * private equality test config
     */
    private final PeqtConfig peqtConfig;
    /**
     * pid config
     */
    private final PidConfig pidConfig;

    private PidBasedPmapConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST);
        peqtConfig = builder.peqtConfig;
        pidConfig = builder.pidConfig;
    }

    @Override
    public PmapPtoType getPtoType() {
        return PmapPtoType.PID_BASED;
    }

    public PeqtConfig getPeqtConfig() {
        return peqtConfig;
    }

    public PidConfig getPidConfig(){
        return pidConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<PidBasedPmapConfig> {
        /**
         * private equality test config
         */
        private PeqtConfig peqtConfig;
        /**
         * pid config
         */
        private PidConfig pidConfig;

        public Builder(boolean silent) {
            PsuConfig psuConfig = new Jsz22SfcPsuConfig.Builder(silent).build();
            pidConfig = new Gmr21SloppyPidConfig.Builder().setPsuConfig(psuConfig).build();
            peqtConfig = new NaivePeqtConfig.Builder(SecurityModel.SEMI_HONEST, silent).build();
        }

        public Builder setPidConfig(PidConfig pidConfig) {
            this.pidConfig = pidConfig;
            return this;
        }

        public Builder setPeqtConfig(PeqtConfig peqtConfig){
            this.peqtConfig = peqtConfig;
            return this;
        }

        @Override
        public PidBasedPmapConfig build() {
            return new PidBasedPmapConfig(this);
        }
    }
}
