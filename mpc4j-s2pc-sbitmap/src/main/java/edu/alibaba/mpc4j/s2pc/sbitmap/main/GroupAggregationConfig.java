package edu.alibaba.mpc4j.s2pc.sbitmap.main;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.ZlMaxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.ZlMaxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pid.bkms20.Bkms20EccPidConfig;
import smile.data.type.StructType;

/**
 * Sbitmap Config
 *
 * @author Li Peng
 * @date 2023/8/7
 */
public class GroupAggregationConfig {
    /**
     * data schema.
     */
    private final StructType schema;
    /**
     * pid config.
     */
    private final PidConfig pidConfig;
    /**
     * z2 circuit config.
     */
    private final Z2cConfig z2cConfig;
    /**
     * zl circuit config.
     */
    private final ZlcConfig zlcConfig;
    /**
     * zl mux config.
     */
    private final ZlMuxConfig zlMuxConfig;
    /**
     * zl max config.
     */
    private final ZlMaxConfig zlMaxConfig;

    private GroupAggregationConfig(Builder builder) {
        schema = builder.schema;
        pidConfig = builder.pidConfig;
        z2cConfig = builder.z2cConfig;
        zlcConfig = builder.zlcConfig;
        zlMuxConfig = builder.zlMuxConfig;
        zlMaxConfig = builder.zlMaxConfig;
    }

    public StructType getSchema() {
        return schema;
    }

    public PidConfig getPidConfig() {
        return pidConfig;
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    public ZlcConfig getZlcConfig() {
        return zlcConfig;
    }

    public ZlMuxConfig getZlMuxConfig() {
        return zlMuxConfig;
    }

    public ZlMaxConfig getZlMaxConfig() {
        return zlMaxConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<GroupAggregationConfig> {
        /**
         * data schema.
         */
        private final StructType schema;
        /**
         * pid config.
         */
        private final PidConfig pidConfig;
        /**
         * z2 circuit config.
         */
        private final Z2cConfig z2cConfig;
        /**
         * zl circuit config.
         */
        private final ZlcConfig zlcConfig;
        /**
         * zl mux config.
         */
        private final ZlMuxConfig zlMuxConfig;
        /**
         * zl max config.
         */
        private final ZlMaxConfig zlMaxConfig;

        public Builder(StructType schema, Zl zl) {
            this.schema = schema;
            pidConfig = new Bkms20EccPidConfig.Builder().build();
            z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true);
            zlcConfig = ZlcFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl);
            zlMuxConfig = ZlMuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true);
            zlMaxConfig = ZlMaxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true, zl);
        }

        @Override
        public GroupAggregationConfig build() {
            return new GroupAggregationConfig(this);
        }
    }
}
