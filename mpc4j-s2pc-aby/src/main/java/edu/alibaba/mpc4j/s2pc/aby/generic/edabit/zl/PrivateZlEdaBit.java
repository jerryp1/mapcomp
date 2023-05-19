package edu.alibaba.mpc4j.s2pc.aby.generic.edabit.zl;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * private Zl extended daBit.
 *
 * @author Weiran Liu
 * @date 2023/5/11
 */
public class PrivateZlEdaBit implements MergedPcgPartyOutput {
    /**
     * Zl instance
     */
    private final Zl zl;
    /**
     * l
     */
    private final int l;
    /**
     * Zl vector
     */
    private ZlVector zlVector;
    /**
     * Z2 vectors
     */
    private BitVector[] bitVectors;

    /**
     * Creates random private edaBits.
     *
     * @param zl Zl instance.
     * @param num num.
     * @param secureRandom random state.
     * @return private edaBits.
     */
    public static PrivateZlEdaBit createRandom(Zl zl, int num, SecureRandom secureRandom) {
        PrivateZlEdaBit privateEdaBit = new PrivateZlEdaBit(zl);
        MathPreconditions.checkPositive("num", num);
        privateEdaBit.zlVector = ZlVector.createRandom(zl, num, secureRandom);
        privateEdaBit.bitVectors = IntStream.range(0, privateEdaBit.l)
            .mapToObj(i -> BitVectorFactory.createRandom(num, secureRandom))
            .toArray(BitVector[]::new);

        return privateEdaBit;
    }

    /**
     * Creates an empty private edaBits.
     *
     * @param zl Zl instance.
     * @return an empty private edaBits.
     */
    public static PrivateZlEdaBit createEmpty(Zl zl) {
        PrivateZlEdaBit privateEdaBit = new PrivateZlEdaBit(zl);
        privateEdaBit.zlVector = ZlVector.createEmpty(zl);
        privateEdaBit.bitVectors = IntStream.range(0, privateEdaBit.l)
            .mapToObj(i -> BitVectorFactory.createEmpty())
            .toArray(BitVector[]::new);

        return privateEdaBit;
    }

    /**
     * private constructor.
     */
    private PrivateZlEdaBit(Zl zl) {
        this.zl = zl;
        l = zl.getL();
    }

    /**
     * Gets Zl instance.
     *
     * @return Zl instance.
     */
    public Zl getZl() {
        return zlVector.getZl();
    }

    @Override
    public PrivateZlEdaBit split(int splitNum) {
        PrivateZlEdaBit splitPrivateEdaBit = new PrivateZlEdaBit(zl);
        splitPrivateEdaBit.zlVector = zlVector.split(splitNum);
        splitPrivateEdaBit.bitVectors = IntStream.range(0, l)
            .mapToObj(i -> bitVectors[i].split(splitNum))
            .toArray(BitVector[]::new);

        return splitPrivateEdaBit;
    }

    @Override
    public void reduce(int reduceNum) {
        zlVector.reduce(reduceNum);
        IntStream.range(0, l).forEach(i -> bitVectors[i].reduce(reduceNum));
    }

    @Override
    public void merge(MergedPcgPartyOutput other) {
        PrivateZlEdaBit that = (PrivateZlEdaBit) other;
        Preconditions.checkArgument(this.getZl().equals(that.getZl()));
        this.zlVector.merge(that.zlVector);
        IntStream.range(0, l).forEach(i -> this.bitVectors[i].merge(that.bitVectors[i]));
    }

    /**
     * Gets Zl vector.
     *
     * @return Zl vector.
     */
    public ZlVector getZlVector() {
        return zlVector;
    }

    /**
     * Gets bit vectors.
     *
     * @return bit vectors.
     */
    public BitVector[] getBitVectors() {
        return bitVectors;
    }

    @Override
    public int getNum() {
        return zlVector.getNum();
    }
}
