package edu.alibaba.mpc4j.dp.service.main;

/**
 * Aggregate heavy hitter metrics.
 *
 * @author Weiran Liu
 * @date 2023/2/2
 */
public class HhLdpAggMetrics {
    /**
     * the type string
     */
    private final String typeString;
    /**
     * ε_w
     */
    private final Double windowEpsilon;
    /**
     * α
     */
    private final Double alpha;
    /**
     * round
     */
    private int round;
    /**
     * server time (ms)
     */
    private double serverTimeMs;
    /**
     * client time (ms)
     */
    private double clientTimeMs;
    /**
     * payload bytes
     */
    private long payloadBytes;
    /**
     * memory bytes
     */
    private long memoryBytes;
    /**
     * NDCG
     */
    private double ndcg;
    /**
     * precision
     */
    private double precision;
    /**
     * absolute error
     */
    private double abe;
    /**
     * relative error
     */
    private double re;

    public HhLdpAggMetrics(String typeString, Double windowEpsilon, Double alpha) {
        this.typeString = typeString;
        this.windowEpsilon = windowEpsilon;
        this.alpha = alpha;
    }

    public void addMetrics(HhLdpMetrics metrics) {
        round++;
        serverTimeMs += metrics.getServerTimeMs();
        clientTimeMs += metrics.getClientTimeMs();
        payloadBytes += metrics.getPayloadBytes();
        memoryBytes += metrics.getMemoryBytes();
        ndcg += metrics.getNdcg();
        precision += metrics.getPrecision();
        abe += metrics.getAbe();
        re += metrics.getRe();
    }

    public String getTypeString() {
        return typeString;
    }

    public String getWindowEpsilonString() {
        return windowEpsilon == null ? "-" : String.valueOf(windowEpsilon);
    }

    public String getAlphaString() {
        return alpha == null ? "-" : String.valueOf(alpha);
    }

    public double getServerTimeSecond() {
        double averageTimeMs = Math.round(serverTimeMs / round);
        return averageTimeMs / 1000;
    }

    public double getClientTimeSecond() {
        double averageTimeMs = Math.round(clientTimeMs / round);
        return averageTimeMs / 1000;
    }

    public long getPayloadBytes() {
        return payloadBytes / round;
    }

    public long getMemoryBytes() {
        return memoryBytes / round;
    }

    public double getNdcg() {
        return (double) Math.round(ndcg / round * 10000) / 10000;
    }

    public double getPrecision() {
        return (double) Math.round(precision / round * 10000) / 10000;
    }

    public double getAbe() {
        return (double) Math.round(abe / round * 10000) / 10000;
    }

    public double getRe() {
        return (double) Math.round(re / round * 10000) / 10000;
    }
}
