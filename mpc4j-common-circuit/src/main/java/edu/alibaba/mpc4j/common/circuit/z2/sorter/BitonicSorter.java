package edu.alibaba.mpc4j.common.circuit.z2.sorter;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.math.BigInteger;

/**
 * Bitonic Sorter. Bitonic sort has a complexity of O(m log^2 m) comparisons with small constant, and is data-oblivious
 * since its control flow is independent of the input.
 * <p>
 * The scheme comes from the following paper:
 *
 * <p>
 * Kenneth E. Batcher. 1968. Sorting Networks and Their Applications. In American Federation of Information Processing
 * Societies: AFIPS, Vol. 32. Thomson Book Company, Washington D.C., 307–314.
 * </p>
 *
 * @author Li Peng
 * @date 2023/6/12
 */
public class BitonicSorter extends AbstractSorter {
    /**
     * Z2 integer circuit.
     */
    private final Z2IntegerCircuit circuit;

    public BitonicSorter(Z2IntegerCircuit circuit) {
        super(circuit);
        this.circuit = circuit;
    }

    @Override
    public void sort(MpcZ2Vector[][] xiArrays, MpcZ2Vector isAscending) throws MpcAbortException {
        bitonicSort(xiArrays, 0, xiArrays.length, isAscending);
    }

    private void bitonicSort(MpcZ2Vector[][] xiArrays, int start, int len, MpcZ2Vector isAscending) throws MpcAbortException {
        if (len > 1) {
            int m = len / 2;
            bitonicSort(xiArrays, start, m, party.not(isAscending));
            bitonicSort(xiArrays, start + m, len - m, isAscending);
            bitonicMerge(xiArrays, start, len, isAscending);
        }
    }

    private void bitonicMerge(MpcZ2Vector[][] key, int start, int len, MpcZ2Vector isAscending) throws MpcAbortException {
        if (len > 1) {
            int m = 1 << (BigInteger.valueOf(len - 1).bitLength() - 1);
            for (int i = start; i < start + len - m; i++) {
                compare(key, i, i + m, isAscending);
            }
            bitonicMerge(key, start, m, isAscending);
            bitonicMerge(key, start + m, len - m, isAscending);
        }
    }

    private void compare(MpcZ2Vector[][] key, int i, int j, MpcZ2Vector isAscending) throws MpcAbortException {
        MpcZ2Vector swap = party.eq(party.not(circuit.leq(key[i], key[j])), isAscending);
        MpcZ2Vector[] s = mux(key[j], key[i], swap);
        s = party.xor(s, key[i]);
        MpcZ2Vector[] ki = party.xor(key[j], s);
        MpcZ2Vector[] kj = party.xor(key[i], s);
        key[i] = ki;
        key[j] = kj;
    }
}
