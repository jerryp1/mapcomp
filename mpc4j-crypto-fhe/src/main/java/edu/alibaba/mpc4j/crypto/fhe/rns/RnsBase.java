package edu.alibaba.mpc4j.crypto.fhe.rns;

import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.zq.*;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * RNS Base Class. Represents a group of co-prime moduli(Modulus objects): [q1, q2, ..., qk], q or Q = \prod q_i
 * providing decompose(convert x in Z_q to Z_{q_i} ) and compose (convert x_i \in Z_{q_i} to x \in Z_q)
 * functions. The scheme comes from:
 * <p>
 * A full rns variant of fv like somewhat homomorphic encryption schemes(BEHZ). https://eprint.iacr.org/2016/510
 * <p/>
 * <p>
 * The implementation is from: https://github.com/microsoft/SEAL/blob/main/native/src/seal/util/rns.h#L22
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/8/17
 */
public class RnsBase implements Cloneable {
    /**
     * size of base, number of q_i
     */
    private int size;
    /**
     * Z_{q_i}
     */
    private Modulus[] base;
    /**
     * q = \prod q_i, note that this is a base-2^64 number, just represented by long[]
     */
    private long[] baseProd;
    /**
     * q_i^* = q/q_i, note that each q^* is a base-2^64 number, just represented by long[], there are k q_i^*, so use 2-d array.
     */
    private long[][] puncturedProdArray;
    /**
     * \tilde{q_i} = (q_i^*)^{-1} mod base, because single base's max bit is 61-bit,
     * so we can use long represents \tilde{q_i}, total k, so use 1-D array
     * note that we use MultiplyUintModOperand for faster modular multiplication.
     */
    private MultiplyUintModOperand[] invPuncturedProdModBaseArray;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RnsBase{" + "size=")
            .append(size)
            .append(", base=")
            .append(Arrays.toString(base))
            .append(", baseProd=")
            .append(Arrays.toString(baseProd))
            .append(", puncturedProdArray= ");
        for (long[] longs : puncturedProdArray) {
            sb.append(Arrays.toString(longs));
        }
        sb.append(", invPuncturedProdModBaseArray= ");
        sb.append(Arrays.toString(invPuncturedProdModBaseArray));
        return sb.toString();
    }

    public RnsBase(Modulus[] rnsBase) {
        assert rnsBase.length > 0;
        this.size = rnsBase.length;
        // co-prime check
        for (int i = 0; i < size; i++) {
            if (rnsBase[i].isZero()) {
                throw new IllegalArgumentException("rns base is invalid, modulus can not be zero");
            }
            // in our implementation, a valid modulus must not be zero, so omit this check.
            for (int j = 0; j < i; j++) {
                if (Numth.gcd(rnsBase[i].getValue(), rnsBase[j].getValue()) > 1) {
                    throw new IllegalArgumentException("rns base is invalid, each moduli must be co-prime.");
                }
            }
        }
        // should consider deep copy?
        base = rnsBase;
        if (!initialize()) {
            throw new IllegalArgumentException("rns base is invalid.");
        }
    }

    /**
     * an empty constructor
     */
    public RnsBase() {

    }

    /**
     * @param rnsBase a long[], each element a moduli q_i,
     */
    public RnsBase(long[] rnsBase) {
        assert rnsBase.length > 0;
        this.size = rnsBase.length;
        // co-prime check
        for (int i = 0; i < size; i++) {
            if (rnsBase[i] == 0) {
                throw new IllegalArgumentException("rns base is invalid, modulus can not be zero");
            }
            for (int j = 0; j < i; j++) {
                if (Numth.gcd(rnsBase[i], rnsBase[j]) > 1) {
                    throw new IllegalArgumentException("rns base is invalid, each moduli must be co-prime.");
                }
            }
        }
        this.base = Arrays.stream(rnsBase).parallel().mapToObj(Modulus::new).toArray(Modulus[]::new);
        if (!initialize()) {
            throw new IllegalArgumentException("rns base is invalid.");
        }
    }

    /**
     * @param copy RnsBase.
     */
    public RnsBase(RnsBase copy) {
        this.size = copy.size;
        long[] baseValues = IntStream.range(0, size).mapToLong(i -> copy.base[i].getValue()).toArray();
        this.base = Arrays.stream(baseValues).parallel().mapToObj(Modulus::new).toArray(Modulus[]::new);
        this.baseProd = new long[size];
        System.arraycopy(copy.baseProd, 0, baseProd, 0, size);
        // just shallow copy
        this.invPuncturedProdModBaseArray = new MultiplyUintModOperand[size];
        System.arraycopy(copy.invPuncturedProdModBaseArray, 0, invPuncturedProdModBaseArray, 0, size);
        // 2-D array deepcopy
        this.puncturedProdArray = new long[size][size];
        for (int i = 0; i < size; i++) {
            System.arraycopy(copy.puncturedProdArray[i], 0, puncturedProdArray[i], 0, size);
        }
    }

    /**
     * Decompose value in-place: [value mod q1, value mod q2, ...., value mod qk]
     * Note that the implementation here implies that the length of the value is
     * equal to the size of RnsBase. In fact, we have no limit to the length of the value,
     * so in order to achieve this, we often add 0 to the high position of the value
     * For example, the input value is 1, but my size is 3, then the input needs to be [1, 0, 0].
     * I find this somewhat counter-intuitive, but for the sake of consistency with SEAL, stick with it for now.
     *
     * @param value a base-2^64 value.
     */
    public void decompose(long[] value) {
        assert value.length == size;
        if (size > 1) {
            long[] valueCopy = new long[value.length];
            System.arraycopy(value, 0, valueCopy, 0, value.length);
            for (int i = 0; i < size; i++) {
                value[i] = UintArithmeticSmallMod.moduloUint(valueCopy, size, this.base[i]);
            }
        }
    }

    /**
     * decompose k arrays, each with length N.
     * <p>
     * {1 0 0 2 0 0} count = 2   --->
     * 1 0 0  ---> a base-2^64 value, is 1
     * 2 0 0 ---> a base-2^64 value, is 2
     * 2 values, each size is 3
     * supposing that base is {3, 5, 7}, then result is
     * {1, 2, 1, 2, 1, 2} --->
     * (1 mod 3) (2 mod 3)  ---> 1 2
     * (1 mod 5) (2 mod 5) ---> 1 2
     * (1 mod 7) (2 mod 7) ---> 1 2
     *
     * @param values an array with length k * N, k is the RNS base size, N is the count.
     *               can treat as a matrix with shape (k, N)
     * @param count  N
     */
    public void decomposeArray(long[] values, int count) {
        assert values.length == count * size;
        if (size > 1) {
            // now columns[i] represent a value, i \in [0, count)
            // now value mod q_i, the result overwrite the values
            //          c0              c1     ....    cn
            //          |               |               |
            //
            //       [ c0 mod q_1, c1 mod q1, ...., cN mod q1]
            //       [ c0 mod q_2, c1 mod q2, ...., cN mod q2]
            //               ......
            //       [ c0 mod qk, c1 mod qk, ....., cN mod qk]
            long[] valuesCopy = new long[values.length];
            System.arraycopy(values, 0, valuesCopy, 0, values.length);
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < count; j++) {
                    values[i * count + j] = UintArithmeticSmallMod.moduloUint(valuesCopy, j * size, size, base[i]);
                }
            }
        }
    }

    /**
     * Compose value in-place: [v1 = value mod q1, v2 = value mod q2, ...., vk = value mod qk].
     * the basic idea is: v = (\sum v_i * \tilde{q_i} * q_i^*) mod q
     * specific: v = (\sum [v_i * \tilde{q_i}]_{q_i} * q_i^*) mod q
     *
     * @param value a base-2^64 value.
     */
    public void compose(long[] value) {
        assert value.length == size;
        if (size > 1) {
            long[] tempValue = new long[size];
            UintCore.setUint(value, size, tempValue);
            // clear
            UintCore.setZeroUint(size, value);
            long[] tempMpi = new long[size];
            // \sum x_i * \hat{q_i} * q_i^* mod Q
            for (int i = 0; i < size; i++) {
                // tmp = x_i * \hat{q_i} mod q_i \in Z_{q_i}
                long tmpProd = UintArithmeticSmallMod.multiplyUintMod(tempValue[i], invPuncturedProdModBaseArray[i], base[i]);
                // tmp * q_i^* \in Z_Q, because tmp \in  Z_{q_i}, q_i^* \in Z_{Q/q_i}
                UintArithmetic.multiplyUint(puncturedProdArray[i], size - 1, tmpProd, size, tempMpi);
                // sum(tmp * q_i^*) mod Q \in Z_Q
                UintArithmeticMod.addUintUintMod(tempMpi, value, baseProd, size, value);
            }
        }
    }

    /**
     * compose by columns in-place, since each column represents a value under RnsBase.
     *
     * @param values k * N matrix, each column represent a value under RnsBase.
     */
    public void composeArray(long[] values, int count) {
        assert values.length == count * size;
        if (size > 1) {
            // {1 0 0 2 0 0 } ---> {1 2 1 2 1 2} ---> {1, 1, 1} and {2, 2, 2}
            // {1, 1, 1} ---> {1, 0, 0} and {2, 2, 2} ----> {2, 0, 0}
            long[][] decomposedValues = new long[count][size];
            for (int i = 0; i < count; i++) {
                for (int j = 0; j < size; j++) {
                    decomposedValues[i][j] = values[i + j * count];
                }
            }
            Arrays.stream(decomposedValues, 0, count).forEach(this::compose);
            for (int i = 0; i < count; i++) {
                System.arraycopy(decomposedValues[i], 0, values, i * size, size);
            }
        }
    }

    /**
     * initialize CRT data by given base, mainly compute q_i^* = q/q_i \in Z,
     * \tilde{q_i} = (q_i^*)^{-1}  mod q_i \in Z_{q_i}
     *
     * @return if initialize success return true, otherwise return false.
     */
    private boolean initialize() {
        Common.mulSafe(size, size, false);
        baseProd = new long[size];
        puncturedProdArray = new long[size][size];
        invPuncturedProdModBaseArray = new MultiplyUintModOperand[size];
        for (int i = 0; i < size; i++) {
            invPuncturedProdModBaseArray[i] = new MultiplyUintModOperand();
        }
        if (size > 1) {
            long[] baseValues = IntStream.range(0, size).mapToLong(i -> base[i].getValue()).toArray();
            boolean invertible = true;
            for (int i = 0; i < size; i++) {
                // q_i* = Q/qi = \prod q_j, j \ne i.
                UintArithmetic.multiplyManyUint64Except(baseValues, size, i, puncturedProdArray[i]);
                // \hat{q_i} = (Q/qi)^{-1} mod qi
                // first reduce (Q/qi) to [0, qi), then compute inv
                long tmp = UintArithmeticSmallMod.moduloUint(puncturedProdArray[i], size - 1, base[i]);
                long[] tmpInv = new long[1];
                invertible = invertible && UintArithmeticSmallMod.tryInvertUintMod(tmp, base[i], tmpInv);
                invPuncturedProdModBaseArray[i].set(tmpInv[0], base[i]);
            }
            // Q = (Q/q0) * q0
            UintArithmetic.multiplyUint(puncturedProdArray[0], size - 1, base[0].getValue(), size, baseProd);
            return invertible;
        }
        // only one q1, q = q1
        baseProd[0] = base[0].getValue();
        // \prod q_i = q1, q/q1 = 1
        puncturedProdArray[0] = new long[]{1L};
        invPuncturedProdModBaseArray[0].set(1, base[0]);
        return true;
    }

    /**
     * extend the original RnsBase with a given RnsBase.
     *
     * @param other other RnsBase.
     * @return a new RnsBase object, which base is [this.base, other.base].
     */
    public RnsBase extend(RnsBase other) {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < other.size; j++) {
                if (!Numth.areCoPrime(base[i].getValue(), other.base[j].getValue())) {
                    throw new IllegalArgumentException("cannot extend by given value");
                }
            }
        }
        RnsBase newBase = new RnsBase();
        newBase.size = Common.addSafe(size, other.size, false);
        newBase.base = new Modulus[newBase.size];
        // copy this.base
        System.arraycopy(this.base, 0, newBase.base, 0, this.size);
        // copy other.base
        System.arraycopy(other.base, 0, newBase.base, this.size, other.size);
        if (!newBase.initialize()) {
            throw new IllegalArgumentException("cannot extend by given value");
        }
        return newBase;
    }


    /**
     * extend the original RnsBase with a given Modulus.
     *
     * @param value a Modulus.
     * @return a new RnsBase object, which base added a new Modulus value.
     */
    public RnsBase extend(Modulus value) {
        // only one pair are not co-prime, then throw exception
        if (Arrays.stream(base).parallel().anyMatch(m -> !Numth.areCoPrime(m.getValue(), value.getValue()))) {
            throw new IllegalArgumentException("cannot extend by given value");
        }
        RnsBase newBase = new RnsBase();
        newBase.size = Common.addSafe(size, 1, false);
        newBase.base = new Modulus[newBase.size];
        // note that this is a shallow copy
        System.arraycopy(this.base, 0, newBase.base, 0, this.size);
        // extend
        newBase.base[size] = value;
        if (!newBase.initialize()) {
            throw new IllegalArgumentException("cannot extend by given value");
        }
        return newBase;
    }

    /**
     * extend the original RnsBase with a given 64-bit value.
     *
     * @param value a long value, represent a q_i.
     * @return a new RnsBase object, which base added a new Modulus(value).
     */
    public RnsBase extend(long value) {
        return extend(new Modulus(value));
    }

    /**
     * remove the last moduli in current base and return a new RnsBase object.
     *
     * @return a new RnsBase object.
     */
    public RnsBase drop() {
        if (this.size == 1) {
            throw new RuntimeException("cannot drop from base of size 1");
        }
        RnsBase newBase = new RnsBase();
        newBase.size = this.size - 1;
        newBase.base = new Modulus[newBase.size];
        // shallow copy
        System.arraycopy(this.base, 0, newBase.base, 0, newBase.size);
        // initialize CRT data
        newBase.initialize();
        return newBase;
    }

    /**
     * remove the given moduli from the RnsBase and return a new RnsBase object.
     *
     * @param value a moduli, needed to be removed.
     * @return a new RnsBase object, which base removed given value.
     */
    public RnsBase drop(Modulus value) {
        if (this.size == 1) {
            throw new RuntimeException("cannot drop from base of size 1");
        }
        if (!contains(value)) {
            throw new IllegalArgumentException("base does not contain given value");
        }
        RnsBase newBase = new RnsBase();
        newBase.size = this.size - 1;
        newBase.base = new Modulus[newBase.size];
        int sourceIndex = 0;
        int destIndex = 0;
        while (destIndex < this.size - 1) {
            if (!this.base[sourceIndex].equals(value)) {
                newBase.base[destIndex] = this.base[sourceIndex];
                destIndex++;
            }
            sourceIndex++;
        }
        // Initialize CRT data
        newBase.initialize();
        return newBase;
    }

    /**
     * remove the given moduli from the RnsBase and return a new RnsBase object.
     *
     * @param value a moduli, needed to be removed.
     * @return a new RnsBase object, which base removed given value.
     */
    public RnsBase drop(long value) {
        return drop(new Modulus(value));
    }

    /**
     * whether the RnsBase contains the given moduli.
     *
     * @param value moduli.
     * @return true if the RnsBase contains the moduli, otherwise false.
     */
    public boolean contains(Modulus value) {
        return Arrays.asList(base).contains(value);
    }

    /**
     * whether the RnsBase is a sub base of the given superBase.
     *
     * @param superBase superBase.
     * @return true if the superBase contains all the moduli of RnsBase, otherwise false.
     */
    public boolean isSubBaseOf(RnsBase superBase) {
        return Arrays.stream(base).allMatch(superBase::contains);
    }

    /**
     * whether the RnsBase is a super base of the given subBase.
     *
     * @param subBase superBase.
     * @return true if RnsBase contains all the moduli of subBase, otherwise false.
     */
    public boolean isSuperBaseOf(RnsBase subBase) {
        return subBase.isSubBaseOf(this);
    }

    /**
     * whether the RnsBase contains the given moduli.
     *
     * @param value 64-bit value.
     * @return true if the RnsBase contains the moduli, otherwise false.
     */
    public boolean contains(long value) {
        return Arrays.stream(base).map(Modulus::getValue).anyMatch(v -> v == value);
    }

    /**
     * get q = \prod q_i, represented as base-2^64 form.
     *
     * @return return \prod q_i.
     */
    public long[] getBaseProd() {
        return baseProd;
    }

    /**
     * get \tilde{q_i} = (q_i^*)^{-1} mod base, i=1, ..., rnsbase.size. Each is represented as 64-bit value.
     *
     * @return return \tilde{q_i} = (q_i^*)^{-1} mod base, i=1, ..., rnsbase.size.
     */
    public MultiplyUintModOperand[] getInvPuncturedProdModBaseArray() {
        return invPuncturedProdModBaseArray;
    }

    /**
     * get \tilde{q_i} = (q_i^*)^{-1} mod base, represented as 64-bit value.
     *
     * @param index index.
     * @return return \tilde{q_i} = (q_i^*)^{-1} mod base.
     */
    public MultiplyUintModOperand getInvPuncturedProdModBaseArray(int index) {
        return invPuncturedProdModBaseArray[index];
    }

    /**
     * get q_i^* = q/q_i, i=1, ..., rnsbase.size. Each is represented as 64-bit value.
     *
     * @return return q_i^* = q/q_i, i=1, ..., rnsbase.size.
     */
    public long[][] getPuncturedProdArray() {
        return puncturedProdArray;
    }

    /**
     * get q_i^* = q/q_i, represented as 64-bit value.
     *
     * @param index index.
     * @return return q_i^* = q/q_i.
     */
    public long[] getPuncturedProdArray(int index) {
        return puncturedProdArray[index];
    }

    /**
     * get q_i, i=1, ..., rnsbase.size.
     *
     * @return return q_i, i=1, ..., rnsbase.size.
     */
    public Modulus[] getBase() {
        return base;
    }

    /**
     * get q_i.
     *
     * @param index index.
     * @return return q_i.
     */
    public Modulus getBase(int index) {
        return base[index];
    }

    /**
     * get size of base.
     *
     * @return return size of base.
     */
    public int getSize() {
        return size;
    }

    @Override
    public RnsBase clone() {
        try {
            RnsBase clone = (RnsBase) super.clone();
            clone.base = new Modulus[size];
            for (int i = 0; i < size; i++) {
                clone.base[i] = base[i].clone();
            }
            clone.baseProd = new long[baseProd.length];
            System.arraycopy(baseProd, 0, clone.baseProd, 0, baseProd.length);
            clone.invPuncturedProdModBaseArray = new MultiplyUintModOperand[invPuncturedProdModBaseArray.length];
            for (int i = 0; i < invPuncturedProdModBaseArray.length; i++) {
                clone.invPuncturedProdModBaseArray[i] = invPuncturedProdModBaseArray[i].clone();
            }
            clone.puncturedProdArray = new long[puncturedProdArray.length][puncturedProdArray[0].length];
            for (int i = 0; i < puncturedProdArray.length; i++) {
                System.arraycopy(puncturedProdArray[i], 0, clone.puncturedProdArray[i], 0, puncturedProdArray[0].length);
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
