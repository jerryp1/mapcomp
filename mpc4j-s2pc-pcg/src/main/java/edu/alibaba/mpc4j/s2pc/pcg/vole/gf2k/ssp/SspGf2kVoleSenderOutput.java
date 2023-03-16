package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.PcgPartyOutput;
import org.bouncycastle.util.encoders.Hex;

import java.util.Arrays;

/**
 * Single single-point GF2K VOLE sender output.
 * <p>
 * The sender gets (x, t) with t = q + Δ·x, where Δ and q is owned by the receiver, and only the α-th x is non-zero.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public class SspGf2kVoleSenderOutput implements PcgPartyOutput {
    /**
     * α
     */
    private int alpha;
    /**
     * x array
     */
    private byte[][] xs;
    /**
     * t array
     */
    private byte[][] ts;

    /**
     * Creates a sender output.
     *
     * @param alpha α.
     * @param xs    x array.
     * @param ts    t array.
     * @return a sender output.
     */
    public static SspGf2kVoleSenderOutput create(int alpha, byte[][] xs, byte[][] ts) {
        SspGf2kVoleSenderOutput receiverOutput = new SspGf2kVoleSenderOutput();
        assert xs.length > 0 : "# of x must be greater than 0: " + xs.length;
        int num = xs.length;
        assert ts.length == num : "# of t must be equal to " + num + ": " + ts.length;
        assert alpha >= 0 && alpha < xs.length : "α must be in range [0, " + num + "): " + alpha;
        receiverOutput.alpha = alpha;
        receiverOutput.xs = Arrays.stream(xs)
            .peek(x -> {
                assert x.length == CommonConstants.BLOCK_BYTE_LENGTH
                    : "x must be in range [0, 2^" + CommonConstants.BLOCK_BIT_LENGTH + "): " + Hex.toHexString(x);
            })
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);
        receiverOutput.ts = Arrays.stream(ts)
            .peek(t -> {
                assert t.length == CommonConstants.BLOCK_BYTE_LENGTH
                    : "t must be in range [0, 2^" + CommonConstants.BLOCK_BIT_LENGTH + "): " + Hex.toHexString(t);
            })
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);
        return receiverOutput;
    }

    /**
     * private constructor.
     */
    private SspGf2kVoleSenderOutput() {
        // empty
    }

    /**
     * Gets α.
     *
     * @return α.
     */
    public int getAlpha() {
        return alpha;
    }

    /**
     * Gets x.
     *
     * @param index the index.
     * @return x.
     */
    public byte[] getX(int index) {
        return xs[index];
    }

    /**
     * Gets t.
     *
     * @param index the index.
     * @return t.
     */
    public byte[] getT(int index) {
        return ts[index];
    }

    @Override
    public int getNum() {
        return xs.length;
    }
}
