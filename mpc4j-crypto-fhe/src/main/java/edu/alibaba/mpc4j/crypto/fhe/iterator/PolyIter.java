package edu.alibaba.mpc4j.crypto.fhe.iterator;

import org.apache.commons.lang3.builder.EqualsBuilder;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * Represent multi(>=1)  degree-N poly under RNS representation.
 *
 * @author Qixian Zhou
 * @date 2023/8/20
 */
public class PolyIter implements Iterator {

    //
    private RnsIter[] rnsIters;

    // number of poly
    private int size;


    // k , size of RnsBase
    private int coeffModulusSize;
    // N
    private int polyModulusDegree;
    // pos for rnsIters
    private int pos;


    public PolyIter(RnsIter[] rnsIters, int polyModulusDegree, int coeffModulusSize) {

        assert Arrays.stream(rnsIters).allMatch( n -> n.getPolyModulusDegree() == polyModulusDegree && n.getRnsBaseSize() == coeffModulusSize);

        this.rnsIters = rnsIters;
        this.polyModulusDegree = polyModulusDegree;
        this.coeffModulusSize = coeffModulusSize;

        this.size = rnsIters.length;
        this.pos = 0;
    }

    public PolyIter() {}


    public static PolyIter createEmpty(int size, int polyModulusDegree, int coeffModulusSize) {

        RnsIter[] rnsIters = IntStream.range(0, size)
                                        .mapToObj( i->new RnsIter(coeffModulusSize, polyModulusDegree))
                                        .toArray(RnsIter[]::new);

        return new PolyIter(rnsIters, polyModulusDegree, coeffModulusSize);
    }

    public void setRnsIters(RnsIter[] rnsIters) {
        this.rnsIters = rnsIters;
    }

    public void setRnsIters(RnsIter rnsIters, int index) {
        this.rnsIters[index] = rnsIters;
    }


    public int getSize() {
        return size;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PolyIter) {
            PolyIter that = (PolyIter) obj;
            return new EqualsBuilder()
                    .append(this.rnsIters, that.rnsIters)
                    .append(this.polyModulusDegree, that.polyModulusDegree)
                    .append(this.coeffModulusSize, that.coeffModulusSize)
                    .isEquals();
        }
        return false;
    }


    public RnsIter getRnsIter(int index) {
        return rnsIters[index];
    }

    public int getPolyModulusDegree() {
        return polyModulusDegree;
    }

    public int getCoeffModulusSize() {
        return coeffModulusSize;
    }

    /**
     * Returns {@code true} if the iteration has more elements.
     * (In other words, returns {@code true} if {@link #next} would
     * return an element rather than throwing an exception.)
     *
     * @return {@code true} if the iteration has more elements
     */
    @Override
    public boolean hasNext() {
        return pos < rnsIters.length;
    }

    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration
     * @throws NoSuchElementException if the iteration has no more elements
     */
    @Override
    public RnsIter next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        RnsIter temp = rnsIters[pos];
        pos++;
        return temp;
    }

    /**
     * Performs the given action for each remaining element until all elements
     * have been processed or the action throws an exception.  Actions are
     * performed in the order of iteration, if that order is specified.
     * Exceptions thrown by the action are relayed to the caller.
     *
     * @param action The action to be performed for each element
     * @throws NullPointerException if the specified action is null
     * @implSpec <p>The default implementation behaves as if:
     * <pre>{@code
     *     while (hasNext())
     *         action.accept(next());
     * }</pre>
     * @since 1.8
     */
    @Override
    public void forEachRemaining(Consumer action) {
        while (hasNext()) {
            action.accept(next());
        }
    }
}
