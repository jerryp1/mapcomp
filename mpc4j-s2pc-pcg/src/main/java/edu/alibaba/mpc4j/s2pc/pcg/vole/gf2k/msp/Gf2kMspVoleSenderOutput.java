package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.PcgPartyOutput;

import java.util.Arrays;

/**
 * multi single-point GF2K-VOLE sender output.
 *
 * @author Weiran Liu
 * @date 2023/7/22
 */
public class Gf2kMspVoleSenderOutput implements PcgPartyOutput {
    /**
     * α array
     */
    private int[] alphaArray;
    /**
     * single x
     */
    private byte[] x;
    /**
     * t array
     */
    private byte[][] ts;

    /**
     * Creates a sender output.
     *
     * @param alphaArray α array.
     * @param x          x.
     * @param ts         t array.
     * @return a sender output.
     */
    public static Gf2kMspVoleSenderOutput create(int[] alphaArray, byte[] x, byte[][] ts) {
        Gf2kMspVoleSenderOutput senderOutput = new Gf2kMspVoleSenderOutput();
        MathPreconditions.checkPositive("ts.length", ts.length);
        int num = ts.length;
        MathPreconditions.checkPositiveInRangeClosed("alphaArray.length", alphaArray.length, num);
        senderOutput.alphaArray = Arrays.stream(alphaArray)
            .peek(alpha -> MathPreconditions.checkNonNegativeInRange("α", alpha, num))
            .distinct()
            .sorted()
            .toArray();
        MathPreconditions.checkEqual(
            "(distinct) alphaArray.length", "alphaArray.length",
            senderOutput.alphaArray.length, alphaArray.length
        );
        MathPreconditions.checkEqual("x.length", "λ in bytes", x.length, CommonConstants.BLOCK_BYTE_LENGTH);
        senderOutput.x = BytesUtils.clone(x);
        senderOutput.ts = Arrays.stream(ts)
            .peek(t -> MathPreconditions.checkEqual(
                "t.length", "λ in bytes", t.length, CommonConstants.BLOCK_BYTE_LENGTH
            ))
            .toArray(byte[][]::new);
        return senderOutput;
    }

    /**
     * private constructor.
     */
    private Gf2kMspVoleSenderOutput() {
        // empty
    }

    /**
     * Gets α array.
     *
     * @return α array.
     */
    public int[] getAlphaArray() {
        return alphaArray;
    }

    /**
     * Gets x.
     *
     * @return x.
     */
    public byte[] getX() {
        return x;
    }

    /**
     * Gets the assigned t.
     *
     * @param index index.
     * @return t.
     */
    public byte[] getT(int index) {
        return ts[index];
    }

    /**
     * Gets t array.
     *
     * @return t array.
     */
    public byte[][] getTs() {
        return ts;
    }

    @Override
    public int getNum() {
        return ts.length;
    }
}
