package edu.alibaba.mpc4j.s2pc.sbitmap.main;

import edu.alibaba.mpc4j.dp.ldp.LdpConfig;
import edu.alibaba.mpc4j.dp.ldp.nominal.encode.EncodeLdpConfig;
import edu.alibaba.mpc4j.dp.ldp.numeric.integral.IntegralLdpConfig;
import edu.alibaba.mpc4j.dp.ldp.numeric.real.RealLdpConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pid.bkms20.Bkms20EccPidConfig;
import smile.data.measure.NominalScale;
import smile.data.type.StructField;
import smile.data.type.StructType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Abstract Sbitmap Config Builder.
 *
 * @author Li Peng
 * @date 2023/8/4
 */
public abstract class AbstractSbitmapConfigBuilder<T> implements org.apache.commons.lang3.builder.Builder<T> {
    /**
     * data schema.
     */
    private final StructType schema;
    /**
     * pid config.
     */
    private PidConfig pidConfig;

    public AbstractSbitmapConfigBuilder(StructType schema) {
        this.schema = schema;
        pidConfig = new Bkms20EccPidConfig.Builder().build();
    }

    public void addPidConfig(PidConfig pidConfig) {
        this.pidConfig = pidConfig;
    }

    public StructType getSchema() {
        return schema;
    }

    public PidConfig getPidConfig() {
        return pidConfig;
    }
}

