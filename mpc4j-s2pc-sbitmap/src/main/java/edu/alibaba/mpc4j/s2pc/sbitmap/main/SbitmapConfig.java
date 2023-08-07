package edu.alibaba.mpc4j.s2pc.sbitmap.main;

import edu.alibaba.mpc4j.dp.ldp.LdpConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidConfig;
import smile.data.type.StructType;

import java.util.Map;

/**
 * Sbitmap Xonfig
 *
 * @author Li Peng
 * @date 2023/8/7
 */
public class SbitmapConfig {
    /**
     * data schema.
     */
    private final StructType schema;
    /**
     * ldp config.
     */
    private final Map<String, LdpConfig> ldpConfigMap;

    /**
     * pid config.
     */
    private final PidConfig pidConfig;

    private SbitmapConfig(Builder builder) {
        schema = builder.getSchema();
        ldpConfigMap = builder.getLdpConfigMap();
        pidConfig = builder.getPidConfig();
    }

    public StructType getSchema() {
        return schema;
    }

    public Map<String, LdpConfig> getLdpConfigMap() {
        return ldpConfigMap;
    }

    public PidConfig getPidConfig() {
        return pidConfig;
    }

    public static class Builder extends AbstractSbitmapConfigBuilder<SbitmapConfig> {

        public Builder(StructType schema) {
            super(schema);
        }

        @Override
        public Builder addLdpConfig(Map<String, LdpConfig> ldpConfigMap) {
            return (Builder) super.addLdpConfig(ldpConfigMap);
        }

        @Override
        public Builder addLdpConfig(String name, LdpConfig ldpConfig) {
            return (Builder) super.addLdpConfig(name, ldpConfig);
        }

        @Override
        public SbitmapConfig build() {
            return new SbitmapConfig(this);
        }
    }
}
