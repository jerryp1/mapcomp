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
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/iterator.h#L1304
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/8/20
 */
public class PolyIter {

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

    /**
     * create an empty PolyIter object with size RnsIter Objects, which shape is coeffModulusSize * polyModulusDegree
     *
     * @param size size of poly
     * @param coeffModulusSize k
     * @param polyModulusDegree N
     */
    public PolyIter(int size, int coeffModulusSize, int polyModulusDegree) {

        this.size = size;
        this.coeffModulusSize = coeffModulusSize;
        this.polyModulusDegree = polyModulusDegree;

        rnsIters = IntStream.range(0, size)
                .mapToObj( i->new RnsIter(coeffModulusSize, polyModulusDegree))
                .toArray(RnsIter[]::new);
    }


    public PolyIter() {}


    public static PolyIter createEmpty(int size, int polyModulusDegree, int coeffModulusSize) {

        RnsIter[] rnsIters = IntStream.range(0, size)
                                        .mapToObj( i->new RnsIter(coeffModulusSize, polyModulusDegree))
                                        .toArray(RnsIter[]::new);

        return new PolyIter(rnsIters, polyModulusDegree, coeffModulusSize);
    }


    public static PolyIter from3dArray(long[][][] data) {

        PolyIter polyIter = new PolyIter(data.length, data[0].length, data[0][0].length);


        IntStream.range(0, polyIter.size).parallel().forEach(
                i -> polyIter.rnsIters[i] = RnsIter.from2dArray(data[i])
        );
        return polyIter;
    }

    public static long[][][] to3dArray(PolyIter polyIter) {

        return IntStream.range(0, polyIter.size).parallel()
                .mapToObj(i -> RnsIter.to2dArray(polyIter.rnsIters[i]))
                .toArray(long[][][]::new);
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

}
