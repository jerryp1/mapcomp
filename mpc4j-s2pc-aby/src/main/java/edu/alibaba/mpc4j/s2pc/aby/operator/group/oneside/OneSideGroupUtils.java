package edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside;

import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import gnu.trove.list.TIntList;
import gnu.trove.list.linked.TIntLinkedList;

import java.util.Arrays;
import java.util.stream.IntStream;

public class OneSideGroupUtils {

    /**
     * get the flags representing which rows store the group aggregation results
     *
     * @param groupFlag if the i-th row is the last one in its group, groupFlag[i] = true, otherwise, groupFlag[i] = false
     * @return the party's output.
     */
    public static int[] getResPosFlag(boolean[] groupFlag) {
        TIntList list = new TIntLinkedList();
        int index = groupFlag.length - 1;
        while (index >= 0) {
            int curr = index - 1;
            while (curr >= 0 && (!groupFlag[curr])) {
                curr--;
            }
            if (curr == -1) {
                list.add(0);
            } else {
                list.add(OneSideGroupUtils.rightMostChildOfLeftSubTreeOfCommonAncestor(curr + 1, index));
            }
            index = curr;
        }
        int[] res = new int[list.size()];
        IntStream.range(0, list.size()).forEach(i -> res[i] = list.get(list.size() - 1 - i));
        return res;
    }

    public static int[] getResPosNew(boolean[] flag){
        // p1. 该分组里面有没有1                      = l.p1 | r.p2
        // p2. 该分组的前一个分组最后一个是不是1         = l.p2
        // r1. 要不要更新p                           l.p2 = 0 & (( l.p1 = 0 & r.p1 = 1) | 自己是右孩子，l.p1 = 0 & r.p1 = 0)
        // r2. 要不要更新s
        // r3. 要不要更新上组的最后一个                ((l.p2 = 1 | l.p1 = 1) & r.p1 = 1) | l.p2 = 1
        // 更新的值
        //          case1: l.p2 = 1, l.p1 = 0, r.p1 = 1 代表左边的分组到了尽头，必须要更新了:
        //                      更新给分组最后一个的值 v = op(l.s,r.p)
        //                      p = null, s = r.s
        //          case2: r.p2 = 1 代表左边最后一个是分位点，无论如何更新下它的值
        //                      更新给分组最后一个的值 v = l.s
        //                      p = l.p, s = r.p1 ? r.s : r.p
        //          case3:

        // 初始化
        // 先做一次判断，如果自己的f = 0，则置为非法值，如果自己的f = 1，且自己的p2 = 1，即自己是独立的值，那么直接赋值给结果
        // 左边p
        //      左孩子:
        //          自己是1的时候
        //              p = null, s = v
        //          自己是0的时候
        //              p = null, s = v
        //      右孩子:
        //          自己是1的时候
        //              p2 = 1: p = null, s = v
        //              p2 = 0: p = v, s = null
        //          自己是0的时候
        //              p = v, s = null
        //
        // 右边s 右孩子 = 0   自己是右孩子，并且l.

        return null;
    }


    public static SquareZ2Vector[][] getPos(SquareZ2Vector[] data, int[] startIndex, int num, int skipLen, boolean parallel){
        SquareZ2Vector[][] res = new SquareZ2Vector[2][data.length];
        IntStream intStream = parallel ? IntStream.range(0, data.length).parallel() : IntStream.range(0, data.length);
        intStream.forEach(i -> {
            res[0][i] = data[i].getPointsWithFixedSpace(startIndex[0], num, skipLen);
            res[1][i] = data[i].getPointsWithFixedSpace(startIndex[1], num, skipLen);
        });
        return res;
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

    public static BitVector crossZeroAndOne(int dataNum, boolean oddIsZero){
        byte a;
        if(oddIsZero){
            a = (byte) ((dataNum & 1) == 0 ? 0b01010101 : 0b10101010);
        }else{
            a = (byte) ((dataNum & 1) == 1 ? 0b01010101 : 0b10101010);
        }
        int byteLen = CommonUtils.getByteLength(dataNum);
        byte[] bytes = new byte[byteLen];
        Arrays.fill(bytes, a);
        bytes[0] &= (byte) ((1<<(dataNum & 7)) - 1);
        return BitVectorFactory.create(dataNum, bytes);
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
