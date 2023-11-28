package edu.alibaba.mpc4j.s2pc.aby.operator.group;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import gnu.trove.list.TIntList;
import gnu.trove.list.linked.TIntLinkedList;

import java.util.Arrays;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.amos22.AbstractAmos22OneSideGroupParty.getPlainBitVectors;

public class GroupUtils {

    /**
     * get the flags representing which rows store the group aggregation results
     *
     * @param groupFlag if the i-th row is the first one in its group, groupFlag[i] = true, otherwise, groupFlag[i] = false
     * @return the party's output.
     */
    public static int[] getResPosFlag(boolean[] groupFlag) {
        TIntList list = new TIntLinkedList();
        int index = groupFlag.length - 1;
        while (index >= 0) {
            if(groupFlag[index]){
                list.add(index);
                index--;
            }else{
                int curr = index - 1;
                while (curr > 0 && (!groupFlag[curr])) {
                    curr--;
                }
                list.add(GroupUtils.rightMostChildOfLeftSubTreeOfCommonAncestor(curr, index));
                index = curr - 1;
            }
        }
        list.sort();
        return list.toArray();
    }

    public static SquareZ2Vector[] getPos(SquareZ2Vector[] data, int startIndex, int num, int skipLen, boolean parallel){
        SquareZ2Vector[] res = new SquareZ2Vector[data.length];
        IntStream intStream = parallel ? IntStream.range(0, data.length).parallel() : IntStream.range(0, data.length);
        intStream.forEach(i -> res[i] = data[i].getPointsWithFixedSpace(startIndex, num, skipLen));
        return res;
    }

    public static void setPos(SquareZ2Vector[] target, SquareZ2Vector[] source, int startIndex, int num, int skipLen, boolean parallel){
        IntStream intStream = parallel ? IntStream.range(0, target.length).parallel() : IntStream.range(0, target.length);
        intStream.forEach(i -> target[i].setPointsWithFixedSpace(source[i], startIndex, num, skipLen));
    }

    public static MpcZ2Vector[] extendData(MpcZ2Vector data, int copyNum){
        return IntStream.range(0, copyNum).mapToObj(i -> data).toArray(MpcZ2Vector[]::new);
    }

    public static int[] getResPosFlag(BitVector groupFlag) {
        BitVector[][] params = getPlainBitVectors(groupFlag);
        BitVector r = params[params.length - 1][0];
        TIntList updateIndexes = new TIntLinkedList();
        for(int i = 0; i < groupFlag.bitNum(); i++){
            if(r.get(i)){
                updateIndexes.add(i);
            }
        }
        return Arrays.stream(updateIndexes.toArray()).sorted().toArray();
    }

    /**
     * the index at the rightmost leaf of the left subtree under the lowest common ancestor of l and r
     */
    public static int rightMostChildOfLeftSubTreeOfCommonAncestor(int l, int r) {
        if (l == r) {
            return l;
        }
        int level = ceilSmallOrEqual(l ^ r);
        return ((r >>> level) << level) - 1;
    }

    /**
     * return the value y such that (1 << y) <= n && (1 << (1 + y)) > n
     */
    public static int ceilSmallOrEqual(int n) {
        if (n == 0) {
            return 0;
        }
        int level = -1;
        int initNum = 1;
        while (initNum <= n) {
            level++;
            initNum <<= 1;
        }
        return level;
    }
}
