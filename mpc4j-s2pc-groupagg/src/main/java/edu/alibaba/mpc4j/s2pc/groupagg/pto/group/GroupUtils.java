package edu.alibaba.mpc4j.s2pc.groupagg.pto.group;

import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

import java.util.stream.IntStream;

/**
 * group aggregation utilities
 *
 */
public class GroupUtils {
    public static SquareZ2Vector[] getPos(SquareZ2Vector[] data, int startIndex, int num, int skipLen, boolean parallel) {
        SquareZ2Vector[] res = new SquareZ2Vector[data.length];
        IntStream intStream = parallel ? IntStream.range(0, data.length).parallel() : IntStream.range(0, data.length);
        intStream.forEach(i -> res[i] = data[i].getPointsWithFixedSpace(startIndex, num, skipLen));
        return res;
    }

    public static void setPos(SquareZ2Vector[] target, SquareZ2Vector[] source, int startIndex, int num, int skipLen, boolean parallel) {
        IntStream intStream = parallel ? IntStream.range(0, target.length).parallel() : IntStream.range(0, target.length);
        intStream.forEach(i -> target[i].setPointsWithFixedSpace(source[i], startIndex, num, skipLen));
    }
}
