package edu.alibaba.mpc4j.common.tool.okve.okvs;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.okve.okvs.field.PolyFieldOkvs;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory.OkvsType;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Polynomial OKVS.
 *
 * @author Weiran Liu
 * @date 2021/09/13
 */
class PolynomialOkvs implements Okvs<ByteBuffer> {
    /**
     * polynomial field OKVS
     */
    private final PolyFieldOkvs polyFieldOkvs;

    PolynomialOkvs(EnvType envType, int n, int l) {
        polyFieldOkvs = new PolyFieldOkvs(envType, n, l);
    }

    @Override
    public void setParallelEncode(boolean parallelEncode) {
        polyFieldOkvs.setParallelEncode(parallelEncode);
    }

    @Override
    public byte[][] encode(Map<ByteBuffer, byte[]> keyValueMap) throws ArithmeticException {
        return polyFieldOkvs.encode(keyValueMap);
    }

    @Override
    public byte[] decode(byte[][] storage, ByteBuffer key) {
        return polyFieldOkvs.decode(storage, key);
    }

    @Override
    public int getN() {
        return polyFieldOkvs.getN();
    }

    @Override
    public int getL() {
        return polyFieldOkvs.getL();
    }

    @Override
    public int getM() {
        return polyFieldOkvs.getM();
    }

    @Override
    public OkvsType getOkvsType() {
        return OkvsType.POLYNOMIAL;
    }

    @Override
    public int getNegLogFailureProbability() {
        return polyFieldOkvs.getNegLogFailureProbability();
    }
}
