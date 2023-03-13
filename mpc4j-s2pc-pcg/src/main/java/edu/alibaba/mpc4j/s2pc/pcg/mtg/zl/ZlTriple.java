package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * the ZL triples.
 *
 * @author Weiran Liu
 * @date 2022/4/11
 */
public class ZlTriple {
    /**
     * the environment
     */
    private EnvType envType;
    /**
     * the Zl operation
     */
    private Zl zl;
    /**
     * the l bit length
     */
    private int l;
    /**
     * the number of triples
     */
    private int num;
    /**
     * a
     */
    private BigInteger[] as;
    /**
     * b
     */
    private BigInteger[] bs;
    /**
     * c
     */
    private BigInteger[] cs;

    /**
     * Creates Zl multiplication triples.
     *
     * @param envType the environment.
     * @param l       the l bit length.
     * @param num     the number of triples.
     * @param as      a.
     * @param bs      b.
     * @param cs      c.
     */
    public static ZlTriple create(EnvType envType, int l, int num, BigInteger[] as, BigInteger[] bs, BigInteger[] cs) {
        assert num > 0 : "num must be greater than 0";
        assert as.length == num : "a.length must be equal to num = " + num;
        assert bs.length == num : "b.length must be equal to num = " + num;
        assert cs.length == num : "c.length must be equal to num = " + num;

        ZlTriple triple = new ZlTriple();
        triple.envType = envType;
        // Zl constructor would verify l
        triple.zl = ZlFactory.createInstance(envType, l);
        triple.l = l;
        triple.num = num;
        triple.as = Arrays.stream(as)
            .peek(a -> {
                assert triple.zl.validateElement(a);
            })
            .toArray(BigInteger[]::new);
        triple.bs = Arrays.stream(bs)
            .peek(b -> {
                assert triple.zl.validateElement(b);
            })
            .toArray(BigInteger[]::new);
        triple.cs = Arrays.stream(cs)
            .peek(c -> {
                assert triple.zl.validateElement(c);
            })
            .toArray(BigInteger[]::new);

        return triple;
    }

    /**
     * Creates an empty triple.
     *
     * @param l the l bit length.
     * @return an empty triple.
     */
    public static ZlTriple createEmpty(int l) {
        assert l > 0 : "l must be greater than 0";

        ZlTriple emptyTriple = new ZlTriple();
        emptyTriple.l = l;
        emptyTriple.num = 0;
        emptyTriple.as = new BigInteger[0];
        emptyTriple.bs = new BigInteger[0];
        emptyTriple.cs = new BigInteger[0];

        return emptyTriple;
    }

    /**
     * private constructor.
     */
    private ZlTriple() {
        // empty
    }

    /**
     * Gets the number of triples.
     *
     * @return the number of triples.
     */
    public int getNum() {
        return num;
    }

    /**
     * Gets the l bit length.
     *
     * @return the l bit length.
     */
    public int getL() {
        return l;
    }

    /**
     * Gets a[i]。
     *
     * @param index the index.
     * @return a[i].
     */
    public BigInteger getA(int index) {
        return as[index];
    }

    /**
     * Gets a.
     *
     * @return a.
     */
    public BigInteger[] getA() {
        return as;
    }

    /**
     * Gets b[i].
     *
     * @param index the index.
     * @return b[i].
     */
    public BigInteger getB(int index) {
        return bs[index];
    }

    /**
     * Gets b.
     *
     * @return b.
     */
    public BigInteger[] getB() {
        return bs;
    }

    /**
     * Gets c[i].
     *
     * @param index the index.
     * @return c[i].
     */
    public BigInteger getC(int index) {
        return cs[index];
    }

    /**
     * Gets c.
     *
     * @return c.
     */
    public BigInteger[] getC() {
        return cs;
    }

    /**
     * Splits the triple with the split num.
     *
     * @param splitNum the split num.
     * @return a new triple with the split num of triple.
     */
    public ZlTriple split(int splitNum) {
        assert splitNum > 0 && splitNum <= num : "splitNum must be in range (0, " + num + "]";
        // split a
        BigInteger[] aSubs = new BigInteger[splitNum];
        BigInteger[] aRemains = new BigInteger[num - splitNum];
        System.arraycopy(as, 0, aSubs, 0, splitNum);
        System.arraycopy(as, splitNum, aRemains, 0, num - splitNum);
        as = aRemains;
        // split b
        BigInteger[] bSubs = new BigInteger[splitNum];
        BigInteger[] bRemains = new BigInteger[num - splitNum];
        System.arraycopy(bs, 0, bSubs, 0, splitNum);
        System.arraycopy(bs, splitNum, bRemains, 0, num - splitNum);
        bs = bRemains;
        // split c
        BigInteger[] cSubs = new BigInteger[splitNum];
        BigInteger[] cRemains = new BigInteger[num - splitNum];
        System.arraycopy(cs, 0, cSubs, 0, splitNum);
        System.arraycopy(cs, splitNum, cRemains, 0, num - splitNum);
        cs = cRemains;
        // update the num
        num = num - splitNum;

        return ZlTriple.create(envType, l, splitNum, aSubs, bSubs, cSubs);
    }

    /**
     * Reduces the triple to the reduced num.
     *
     * @param reduceNum the reduced num.
     */
    public void reduce(int reduceNum) {
        assert reduceNum > 0 && reduceNum <= num : "reduceNum = " + reduceNum + " must be in range (0, " + num + "]";
        // if the reduced num is less than num, split the triple. If not, keep the current state.
        if (reduceNum < num) {
            // reduce a
            BigInteger[] aRemains = new BigInteger[reduceNum];
            System.arraycopy(as, 0, aRemains, 0, reduceNum);
            as = aRemains;
            // reduce b
            BigInteger[] bRemains = new BigInteger[reduceNum];
            System.arraycopy(bs, 0, bRemains, 0, reduceNum);
            bs = bRemains;
            // reduce c
            BigInteger[] cRemains = new BigInteger[reduceNum];
            System.arraycopy(cs, 0, cRemains, 0, reduceNum);
            cs = cRemains;
            // reduce the num
            num = reduceNum;
        }
    }

    /**
     * Merges two triples.
     *
     * @param that the other triple.
     */
    public void merge(ZlTriple that) {
        assert this.l == that.l : "merged " + ZlTriple.class.getSimpleName() + " must have the same l";
        // merge a
        BigInteger[] mergeAs = new BigInteger[this.as.length + that.as.length];
        System.arraycopy(this.as, 0, mergeAs, 0, this.as.length);
        System.arraycopy(that.as, 0, mergeAs, this.as.length, that.as.length);
        as = mergeAs;
        // merge b
        BigInteger[] mergeBs = new BigInteger[this.bs.length + that.bs.length];
        System.arraycopy(this.bs, 0, mergeBs, 0, this.bs.length);
        System.arraycopy(that.bs, 0, mergeBs, this.bs.length, that.bs.length);
        bs = mergeBs;
        // merge c
        BigInteger[] mergeCs = new BigInteger[this.cs.length + that.cs.length];
        System.arraycopy(this.cs, 0, mergeCs, 0, this.cs.length);
        System.arraycopy(that.cs, 0, mergeCs, this.cs.length, that.cs.length);
        cs = mergeCs;
        // update the num
        num += that.num;
    }
}
