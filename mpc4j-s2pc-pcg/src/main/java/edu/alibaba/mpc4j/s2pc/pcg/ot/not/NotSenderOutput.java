package edu.alibaba.mpc4j.s2pc.pcg.ot.not;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;

import java.util.Arrays;

/**
 * 1-out-of-n OT sender output. The sender gets r_0, r_1, ..., r_{n - 1}.
 *
 * @author Weiran Liu
 * @date 2023/4/9
 */
public class NotSenderOutput implements MergedPcgPartyOutput {
    /**
     * the maximal choice
     */
    private final int n;
    /**
     * rs array
     */
    private byte[][][] rsArray;

    /**
     * Creates a sender output.
     *
     * @param n       the maximal choice.
     * @param rsArray the rs array.
     * @return a sender output.
     */
    public static NotSenderOutput create(int n, byte[][][] rsArray) {
        NotSenderOutput senderOutput = new NotSenderOutput(n);
        assert rsArray.length > 0 : "# of rs must be greater than 0: " + rsArray.length;
        senderOutput.rsArray = Arrays.stream(rsArray)
            .peek(rs -> {
                assert rs.length == n : "# of r must be equal to " + n + ": " + rs.length;
                Arrays.stream(rs).forEach(r -> {
                    assert r.length == CommonConstants.BLOCK_BYTE_LENGTH;
                });
            })
            .map(BytesUtils::clone)
            .toArray(byte[][][]::new);

        return senderOutput;
    }

    /**
     * Creates an empty sender output.
     *
     * @param n the maximal choice.
     * @return an empty sender output.
     */
    public static NotSenderOutput createEmpty(int n) {
        NotSenderOutput senderOutput = new NotSenderOutput(n);
        senderOutput.rsArray = new byte[0][][];

        return senderOutput;
    }

    /**
     * private constructor.
     */
    private NotSenderOutput(int n) {
        assert n > 1 : "n must be greater than 1: " + n;
        this.n = n;
    }

    @Override
    public NotSenderOutput split(int splitNum) {
        int num = getNum();
        assert splitNum > 0 && splitNum <= num : "splitNum must be in range (0, " + num + "]: " + splitNum;
        byte[][][] rsSubArray = new byte[splitNum][][];
        byte[][][] rsRemainArray = new byte[num - splitNum][][];
        System.arraycopy(rsArray, 0, rsSubArray, 0, splitNum);
        System.arraycopy(rsArray, splitNum, rsRemainArray, 0, num - splitNum);
        rsArray = rsRemainArray;

        return NotSenderOutput.create(n, rsSubArray);
    }

    @Override
    public void reduce(int reduceNum) {
        int num = getNum();
        assert reduceNum > 0 && reduceNum <= num : "reduceNum must be in range (0, " + num + "]: " + reduceNum;
        if (reduceNum < num) {
            // we need to reduce only if reduceNum is less than the current num.
            byte[][][] rsRemainArray = new byte[reduceNum][][];
            System.arraycopy(rsArray, 0, rsRemainArray, 0, reduceNum);
            rsArray = rsRemainArray;
        }
    }

    @Override
    public void merge(MergedPcgPartyOutput other) {
        NotSenderOutput that = (NotSenderOutput) other;
        assert this.n == that.n : "n mismatch";
        byte[][][] mergeRsArray = new byte[this.rsArray.length + that.rsArray.length][][];
        System.arraycopy(this.rsArray, 0, mergeRsArray, 0, this.rsArray.length);
        System.arraycopy(that.rsArray, 0, mergeRsArray, this.rsArray.length, that.rsArray.length);
        rsArray = mergeRsArray;
    }

    @Override
    public int getNum() {
        return rsArray.length;
    }

    /**
     * Gets Rb.
     *
     * @param index  the index.
     * @param choice the choice.
     * @return Rb.
     */
    public byte[] getRb(int index, int choice) {
        return rsArray[index][choice];
    }

    /**
     * Gets rs.
     *
     * @param index the index.
     * @return rs.
     */
    public byte[][] getRs(int index) {
        return rsArray[index];
    }

    /**
     * Gets the maximal choice.
     *
     * @return maximal choice.
     */
    public int getN() {
        return n;
    }
}
