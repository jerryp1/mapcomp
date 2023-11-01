package edu.alibaba.mpc4j.s2pc.opf.shuffle;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class ShuffleUtils {
    /**
     * Generate random permutations.
     *
     * @param num the number of elements to be permuted.
     * @return random permutations.
     */
    public static int[] generateRandomPerm(int num) {
        SecureRandom secureRandom = new SecureRandom();
        List<Integer> randomPermList = IntStream.range(0, num)
            .boxed()
            .collect(Collectors.toList());
        Collections.shuffle(randomPermList, secureRandom);
        return randomPermList.stream().mapToInt(permutation -> permutation).toArray();
    }

    /**
     * Compose two permutation.
     *
     * @param perms0 the permutation to be applied first.
     * @param perms1 the permutation to be applied subsequently.
     * @return composed permutation.
     */
    public static int[] composePerms(int[] perms0, int[] perms1) {
        int[] resultPerms = new int[perms0.length];
        for (int i = 0; i < perms0.length; i++) {
            resultPerms[i] = perms0[perms1[i]];
        }
        return resultPerms;
    }

    /**
     * Reverse the permutation.
     *
     * @param perm permutation.
     * @return reversed permutation.
     */
    public static int[] reversePermutation(int[] perm) {
        int[] result = new int[perm.length];
        for (int i = 0; i < perm.length; i++) {
            result[perm[i]] = i;
        }
        return result;
    }

    public static Vector<byte[]> mergeSecret(SquareZ2Vector[][] input, EnvType envType, boolean parallel) {
        int bitLength = input[0][0].bitNum();
        BitVector[] allColumns = Arrays.stream(input).map(x -> Arrays.stream(x).map(single -> {
            assert single.bitNum() == bitLength && (!single.isPlain());
            return single.getBitVector();
        }).collect(Collectors.toList())).flatMap(Collection::stream).toArray(BitVector[]::new);
        return Arrays.stream(ZlDatabase.create(envType, parallel, allColumns).getBytesData()).collect(Collectors.toCollection(Vector::new));
    }

    public static SquareZ2Vector[][] splitSecret(Vector<byte[]> input, int[] bitLength, EnvType envType, boolean parallel) {
        int totalBitLength = Arrays.stream(bitLength).sum();
        BitVector[] trans = ZlDatabase.create(totalBitLength, input.toArray(new byte[0][])).bitPartition(envType, parallel);
        SquareZ2Vector[][] res = new SquareZ2Vector[bitLength.length][];
        for (int i = 0, j = 0; i < bitLength.length; i++) {
            res[i] = new SquareZ2Vector[bitLength[i]];
            for (int k = 0; k < bitLength[i]; k++) {
                res[i][k] = SquareZ2Vector.create(trans[j++], false);
            }
        }
        return res;
    }
}
