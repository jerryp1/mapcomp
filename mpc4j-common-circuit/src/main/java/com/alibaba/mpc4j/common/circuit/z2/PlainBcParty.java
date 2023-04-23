package com.alibaba.mpc4j.common.circuit.z2;

import java.util.Arrays;

/**
 * Plain Boolean Circuit Party.
 *
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2023/4/21
 */
public class PlainBcParty implements MpcBcParty {

    @Override
    public MpcZ2Vector and(MpcZ2Vector xi, MpcZ2Vector yi) {
        return xi.and(yi);
    }

    @Override
    public MpcZ2Vector[] and(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) {
        assert xiArray.length == yiArray.length
                : String.format("xiArray.length (%s) must be equal to yiArray.length (%s)", xiArray.length, yiArray.length);
        if (xiArray.length == 0) {
            return new MpcZ2Vector[0];
        }
        // merge xi and yi
        MpcZ2Vector mergeXiArray = mergeSbitVectors(xiArray);
        MpcZ2Vector mergeYiArray = mergeSbitVectors(yiArray);
        // and operation
        MpcZ2Vector mergeZiArray = and(mergeXiArray, mergeYiArray);
        // split
        int[] lengths = Arrays.stream(xiArray).mapToInt(MpcZ2Vector::getNum).toArray();
        return splitSbitVector(mergeZiArray, lengths);
    }

    @Override
    public MpcZ2Vector xor(MpcZ2Vector xi, MpcZ2Vector yi) {
        return xi.xor(yi, true);
    }

    @Override
    public MpcZ2Vector[] xor(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) {
        assert xiArray.length == yiArray.length
                : String.format("xiArray.length (%s) must be equal to yiArray.length (%s)", xiArray.length, yiArray.length);
        if (xiArray.length == 0) {
            return new MpcZ2Vector[0];
        }
        // merge xi and yi
        MpcZ2Vector mergeXiArray = mergeSbitVectors(xiArray);
        MpcZ2Vector mergeYiArray = mergeSbitVectors(yiArray);
        // xor operation
        MpcZ2Vector mergeZiArray = xor(mergeXiArray, mergeYiArray);
        // split
        int[] lengths = Arrays.stream(xiArray).mapToInt(MpcZ2Vector::getNum).toArray();
        return splitSbitVector(mergeZiArray, lengths);
    }

    @Override
    public MpcZ2Vector or(MpcZ2Vector xi, MpcZ2Vector yi) {
        return xi.or(yi);
    }

    @Override
    public MpcZ2Vector[] or(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) {
        assert xiArray.length == yiArray.length
                : String.format("xiArray.length (%s) must be equal to yiArray.length (%s)", xiArray.length, yiArray.length);
        if (xiArray.length == 0) {
            return new MpcZ2Vector[0];
        }
        // merge xi and yi
        MpcZ2Vector mergeXiArray = mergeSbitVectors(xiArray);
        MpcZ2Vector mergeYiArray = mergeSbitVectors(yiArray);
        // or operation
        MpcZ2Vector mergeZiArray = or(mergeXiArray, mergeYiArray);
        // split
        int[] lengths = Arrays.stream(xiArray).mapToInt(MpcZ2Vector::getNum).toArray();
        return splitSbitVector(mergeZiArray, lengths);
    }

    @Override
    public MpcZ2Vector not(MpcZ2Vector xi) {
        return xi.not();
    }

    @Override
    public MpcZ2Vector[] not(MpcZ2Vector[] xiArray) {
        if (xiArray.length == 0) {
            return new MpcZ2Vector[0];
        }
        // merge xi
        MpcZ2Vector mergeXiArray = mergeSbitVectors(xiArray);
        // not operation
        MpcZ2Vector mergeZiArray = not(mergeXiArray);
        // split
        int[] lengths = Arrays.stream(xiArray).mapToInt(MpcZ2Vector::getNum).toArray();
        return splitSbitVector(mergeZiArray, lengths);
    }

    @Override
    public MpcZ2Type getType() {
        return MpcZ2Type.PLAIN;
    }

    private MpcZ2Vector mergeSbitVectors(MpcZ2Vector[] sbitVectors) {
        assert sbitVectors.length > 0 : "merged vector length must be greater than 0";
        MpcZ2Vector mergeSbitVector = MpcZ2VectorFactory.createEmpty(MpcZ2Type.PLAIN);
        // we must merge the bit vector in the reverse order
        for (MpcZ2Vector sbitVector : sbitVectors) {
            assert sbitVector.getNum() > 0 : "the number of bits must be greater than 0";
            assert sbitVector.getType() == MpcZ2Type.PLAIN : "type of MpcZ2Vector must be plain";
            mergeSbitVector.merge(sbitVector);
        }
        return mergeSbitVector;
    }

    private MpcZ2Vector[] splitSbitVector(MpcZ2Vector mergeSbitVector, int[] lengths) {
        MpcZ2Vector[] sbitVectors = new MpcZ2Vector[lengths.length];
        for (int index = 0; index < lengths.length; index++) {
            sbitVectors[index] = (MpcZ2Vector) mergeSbitVector.split(lengths[index]);
        }
        assert mergeSbitVector.getNum() == 0 : "merged vector must remain 0 bits: " + mergeSbitVector.getNum();
        return sbitVectors;
    }
}
