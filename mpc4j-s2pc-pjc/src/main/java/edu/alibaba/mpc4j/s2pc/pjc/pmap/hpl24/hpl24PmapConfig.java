package edu.alibaba.mpc4j.s2pc.pjc.pmap.hpl24;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.gmr21.Gmr21OsnConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pid.bkms20.Bkms20EccPidConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pid.bkms20.Bkms20EccPidConfig.Builder;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapFactory.PmapType;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.PlpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.rs21.Rs21PlpsiConfig;

public class hpl24PmapConfig extends AbstractMultiPartyPtoConfig implements PmapConfig {

    private final PlpsiConfig plpsiconfig;

    private final OsnConfig osnConfig;
    private hpl24PmapConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST);
        plpsiconfig = builder.plpsiconfig;
        osnConfig = builder.osnConfig;
    }

    @Override
    public PmapType getPtoType() {
        return PmapType.HPL24;
    }

    public PlpsiConfig getPlpsiconfig(){
        return plpsiconfig;
    }

    public OsnConfig getOsnConfig() {
        return osnConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<hpl24PmapConfig> {
        /**
         * 使用的plpsi配置
         */
        private PlpsiConfig plpsiconfig;

        private OsnConfig osnConfig;

        public Builder(boolean silent) {
            plpsiconfig = new Rs21PlpsiConfig.Builder(silent).build();
            osnConfig = new Gmr21OsnConfig.Builder(silent).build();
        }

        public Builder setPlpsiconfig(PlpsiConfig plpsiconfig) {
            this.plpsiconfig = plpsiconfig;
            return this;
        }

        public Builder setOsnConfig(OsnConfig osnConfig) {
            this.osnConfig = osnConfig;
            return this;
        }

        @Override
        public hpl24PmapConfig build() {
            return new hpl24PmapConfig(this);
        }
    }
}
