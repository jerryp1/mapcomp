package edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk;

import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;

/**
 * @author Feng Han
 * @date 2024/7/19
 */
public class PkFkUtils {
    /**
     * add index for data
     */
    public static byte[][] addIndex(byte[][] input){
        int n = input.length;
        HashMap<BigInteger, int[]> map = new HashMap<>();
        byte[][] res = new byte[n][];
        for(int i = 0 ; i < input.length; i++){
            BigInteger data = new BigInteger(input[i]);
            res[i] = new byte[input[i].length + 4];
            int[] cnt;
            if(map.containsKey(data)){
                cnt = map.get(data);
                cnt[0] = cnt[0] + 1;
            }else{
                cnt = new int[]{0};
                map.put(data, cnt);
            }
            System.arraycopy(input[i], 0, res[i], 0, input[i].length);
            System.arraycopy(IntUtils.intToByteArray(cnt[0]), 0, res[i], input[i].length, 4);
        }
        return res;
    }

    /**
     * sort data to get the permutation
     */
    public static int[] permutation4Sort(byte[][] input){
        BigInteger[] bigIntegers = Arrays.stream(input).map(BigIntegerUtils::byteArrayToNonNegBigInteger).toArray(BigInteger[]::new);
        HashMap<BigInteger, Integer> map = new HashMap<>();
        for(int i = 0; i < input.length; i++){
            map.put(bigIntegers[i], i);
        }
        Arrays.parallelSort(bigIntegers, BigInteger::compareTo);
        return Arrays.stream(bigIntegers).mapToInt(map::get).toArray();
    }
}
