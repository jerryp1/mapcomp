package com.alibaba.mpc4j.common.circuit.z2;

import java.util.Arrays;

/**
 * Plain Boolean Circuit Party.
 *
 * @author Li Peng
 * @date 2023/4/21
 */
public class PlainBcParty implements MpcBcParty {

    @Override
    public PlainZ2Vector createOnes(int bitNum) {
        return PlainZ2Vector.createOnes(bitNum);
    }

    @Override
    public PlainZ2Vector createZeros(int bitNum) {
        return PlainZ2Vector.createZeros(bitNum);
    }

    @Override
    public PlainZ2Vector create(int bitNum, boolean value) {
        return PlainZ2Vector.create(bitNum, value);
    }

    @Override
    public PlainZ2Vector createEmpty() {
        return PlainZ2Vector.createEmpty();
    }

    @Override
    public PlainZ2Vector and(MpcZ2Vector xi, MpcZ2Vector yi) {
        PlainZ2Vector plainXi = (PlainZ2Vector) xi;
        PlainZ2Vector plainYi = (PlainZ2Vector) yi;
        return PlainZ2Vector.create(plainXi.getBitVector().and(plainYi.getBitVector()));
    }

    @Override
    public PlainZ2Vector[] and(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) {
        assert xiArray.length == yiArray.length
            : String.format("xiArray.length (%s) must be equal to yiArray.length (%s)", xiArray.length, yiArray.length);
        if (xiArray.length == 0) {
            return new PlainZ2Vector[0];
        }
        // merge xi and yi
        PlainZ2Vector mergeXiArray = mergePlainZ2Vectors(xiArray);
        PlainZ2Vector mergeYiArray = mergePlainZ2Vectors(yiArray);
        // and operation
        PlainZ2Vector mergeZiArray = and(mergeXiArray, mergeYiArray);
        // split
        int[] lengths = Arrays.stream(xiArray).mapToInt(MpcZ2Vector::getNum).toArray();
        return splitPlainZ2Vector(mergeZiArray, lengths);
    }

    @Override
    public PlainZ2Vector xor(MpcZ2Vector xi, MpcZ2Vector yi) {
        PlainZ2Vector plainXi = (PlainZ2Vector) xi;
        PlainZ2Vector plainYi = (PlainZ2Vector) yi;
        return PlainZ2Vector.create(plainXi.getBitVector().xor(plainYi.getBitVector()));
    }

    @Override
    public PlainZ2Vector[] xor(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) {
        assert xiArray.length == yiArray.length
            : String.format("xiArray.length (%s) must be equal to yiArray.length (%s)", xiArray.length, yiArray.length);
        if (xiArray.length == 0) {
            return new PlainZ2Vector[0];
        }
        // merge xi and yi
        PlainZ2Vector mergeXiArray = mergePlainZ2Vectors(xiArray);
        PlainZ2Vector mergeYiArray = mergePlainZ2Vectors(yiArray);
        // xor operation
        PlainZ2Vector mergeZiArray = xor(mergeXiArray, mergeYiArray);
        // split
        int[] lengths = Arrays.stream(xiArray).mapToInt(MpcZ2Vector::getNum).toArray();
        return splitPlainZ2Vector(mergeZiArray, lengths);
    }

    @Override
    public PlainZ2Vector or(MpcZ2Vector xi, MpcZ2Vector yi) {
        PlainZ2Vector plainXi = (PlainZ2Vector) xi;
        PlainZ2Vector plainYi = (PlainZ2Vector) yi;
        return PlainZ2Vector.create(plainXi.getBitVector().or(plainYi.getBitVector()));
    }

    @Override
    public PlainZ2Vector[] or(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) {
        assert xiArray.length == yiArray.length
            : String.format("xiArray.length (%s) must be equal to yiArray.length (%s)", xiArray.length, yiArray.length);
        if (xiArray.length == 0) {
            return new PlainZ2Vector[0];
        }
        // merge xi and yi
        PlainZ2Vector mergeXiArray = mergePlainZ2Vectors(xiArray);
        PlainZ2Vector mergeYiArray = mergePlainZ2Vectors(yiArray);
        // or operation
        PlainZ2Vector mergeZiArray = or(mergeXiArray, mergeYiArray);
        // split
        int[] lengths = Arrays.stream(xiArray).mapToInt(MpcZ2Vector::getNum).toArray();
        return splitPlainZ2Vector(mergeZiArray, lengths);
    }

    @Override
    public PlainZ2Vector not(MpcZ2Vector xi) {
        return xor(xi, PlainZ2Vector.createOnes(xi.getNum()));
    }

    @Override
    public PlainZ2Vector[] not(MpcZ2Vector[] xiArray) {
        if (xiArray.length == 0) {
            return new PlainZ2Vector[0];
        }
        // merge xi
        PlainZ2Vector mergeXiArray = mergePlainZ2Vectors(xiArray);
        // not operation
        PlainZ2Vector mergeZiArray = not(mergeXiArray);
        // split
        int[] lengths = Arrays.stream(xiArray).mapToInt(MpcZ2Vector::getNum).toArray();
        return splitPlainZ2Vector(mergeZiArray, lengths);
    }

    private PlainZ2Vector mergePlainZ2Vectors(MpcZ2Vector[] mpcZ2Vectors) {
        assert mpcZ2Vectors.length > 0 : "merged vector length must be greater than 0";
        PlainZ2Vector[] plainZ2Vectors = Arrays.stream(mpcZ2Vectors)
            .map(vector -> (PlainZ2Vector) vector)
            .toArray(PlainZ2Vector[]::new);
        PlainZ2Vector mergePlainZ2Vector = createEmpty();
        // we must merge the bit vector in the reverse order
        for (PlainZ2Vector plainZ2Vector : plainZ2Vectors) {
            assert plainZ2Vector.getNum() > 0 : "the number of bits must be greater than 0";
            mergePlainZ2Vector.merge(plainZ2Vector);
        }
        return mergePlainZ2Vector;
    }

    private PlainZ2Vector[] splitPlainZ2Vector(MpcZ2Vector mergeMpcZ2Vector, int[] lengths) {
        PlainZ2Vector mergePlainZ2Vector = (PlainZ2Vector) mergeMpcZ2Vector;
        PlainZ2Vector[] plainZ2Vectors = new PlainZ2Vector[lengths.length];
        for (int index = 0; index < lengths.length; index++) {
            plainZ2Vectors[index] = mergePlainZ2Vector.split(lengths[index]);
        }
        assert mergePlainZ2Vector.getNum() == 0 : "merged vector must remain 0 bits: " + mergePlainZ2Vector.getNum();
        return plainZ2Vectors;
    }
}
