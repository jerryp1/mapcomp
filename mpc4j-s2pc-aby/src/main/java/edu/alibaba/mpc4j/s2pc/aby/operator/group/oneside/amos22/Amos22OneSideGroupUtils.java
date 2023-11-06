package edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.amos22;

import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

import java.util.stream.IntStream;

public class Amos22OneSideGroupUtils {
    protected static SquareZ2Vector[][] getPos(SquareZ2Vector[] data, int[] startIndex, int skipLen, int num, boolean parallel){
        SquareZ2Vector[][] res = new SquareZ2Vector[2][data.length];
        IntStream intStream = parallel ? IntStream.range(0, data.length).parallel() : IntStream.range(0, data.length);
        intStream.forEach(i -> {
            res[0][i] = (SquareZ2Vector) data[i].getPointsWithFixedSpace(startIndex[0], num, skipLen);
            res[1][i] = (SquareZ2Vector) data[i].getPointsWithFixedSpace(startIndex[1], num, skipLen);
        });
        return res;
    }

    protected static void setPos(SquareZ2Vector[] target, SquareZ2Vector[] source, int startIndex, int skipLen, int num, boolean parallel){
        IntStream intStream = parallel ? IntStream.range(0, target.length).parallel() : IntStream.range(0, target.length);
        intStream.forEach(i -> {
            target[i].setPointsWithFixedSpace(source[i], startIndex, num, skipLen);
        });
    }
}
