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
     * ldp config map.
     */
    private final Map<String, LdpConfig> ldpConfigMap;
    /**
     * pid config.
     */
    private PidConfig pidConfig;

    public AbstractSbitmapConfigBuilder(StructType schema) {
        this.schema = schema;
        ldpConfigMap = new HashMap<>(schema.length());
        pidConfig = new Bkms20EccPidConfig.Builder().build();
    }

    public void addPidConfig(PidConfig pidConfig) {
        this.pidConfig = pidConfig;
    }

    /**
     * add ldp config map.
     *
     * @param ldpConfigMap ldp config map.
     * @return ldp config builder.
     */
    protected AbstractSbitmapConfigBuilder<T> addLdpConfig(Map<String, LdpConfig> ldpConfigMap) {
        for (String name : ldpConfigMap.keySet()) {
            innerAddLdpConfig(name, ldpConfigMap.get(name));
        }
        return this;
    }

    /**
     * add ldp config map.
     *
     * @param name      name.
     * @param ldpConfig ldp config.
     * @return ldp config builder.
     */
    protected AbstractSbitmapConfigBuilder<T> addLdpConfig(String name, LdpConfig ldpConfig) {
        innerAddLdpConfig(name, ldpConfig);
        return this;
    }

    private void innerAddLdpConfig(String name, LdpConfig ldpConfig) {
        StructField structField = schema.field(name);
        if (structField.measure instanceof NominalScale) {
            // encodeLdpConfig for nominalScale
            assert ldpConfig instanceof EncodeLdpConfig
                : "LDP for " + name + " must be " + EncodeLdpConfig.class.getSimpleName();
            NominalScale nominalScale = (NominalScale) structField.measure;
            EncodeLdpConfig encodeLdpConfig = (EncodeLdpConfig) ldpConfig;
            Set<String> labelSet = encodeLdpConfig.getLabelSet();
            // assert consistency
            for (String label : nominalScale.levels()) {
                assert labelSet.contains(label) : label + " is not in label set";
            }
            assert labelSet.size() == nominalScale.size() : "# labels in schema does not match # labels in label set";
        } else if (structField.type.isIntegral()) {
            // IntegralLdpConfig for integral
            assert ldpConfig instanceof IntegralLdpConfig
                : "LDP for " + name + " must be " + IntegralLdpConfig.class.getSimpleName();
        } else if (structField.type.isFloating()) {
            // RealLdpConfig for floating
            assert ldpConfig instanceof RealLdpConfig
                : "LDP for " + name + " must be " + RealLdpConfig.class.getSimpleName();
        } else {
            throw new IllegalArgumentException("Does not support LDP for " + name + " with measure: " + structField.measure);
        }
        ldpConfigMap.put(name, ldpConfig);
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
}

