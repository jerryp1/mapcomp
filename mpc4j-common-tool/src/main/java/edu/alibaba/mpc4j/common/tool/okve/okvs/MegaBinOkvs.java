package edu.alibaba.mpc4j.common.tool.okve.okvs;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.okve.okvs.field.MegaBinFieldOkvs;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory.OkvsType;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * MegaBin OKVS.
 *
 * @author Weiran Liu
 * @date 2021/09/13
 */
class MegaBinOkvs implements Okvs<ByteBuffer> {
    /**
     * MegaBin field OKVS
     */
    private final MegaBinFieldOkvs megaBinFieldOkvs;

    MegaBinOkvs(EnvType envType, int n, int l, byte[] key) {
        megaBinFieldOkvs = new MegaBinFieldOkvs(envType, n, l, key);
    }

    @Override
    public void setParallelEncode(boolean parallelEncode) {
        megaBinFieldOkvs.setParallelEncode(parallelEncode);
    }

    @Override
    public byte[][] encode(Map<ByteBuffer, byte[]> keyValueMap) throws ArithmeticException {
        return megaBinFieldOkvs.encode(keyValueMap);
    }

    @Override
    public byte[] decode(byte[][] storage, ByteBuffer key) {
        return megaBinFieldOkvs.decode(storage, key);
    }

    @Override
    public int getN() {
        return megaBinFieldOkvs.getN();
    }

    @Override
    public int getL() {
        return megaBinFieldOkvs.getL();
    }

    @Override
    public int getM() {
        return megaBinFieldOkvs.getM();
    }

    @Override
    public OkvsType getOkvsType() {
        return OkvsType.MEGA_BIN;
    }

    @Override
    public int getNegLogFailureProbability() {
        return megaBinFieldOkvs.getNegLogFailureProbability();
    }
}
