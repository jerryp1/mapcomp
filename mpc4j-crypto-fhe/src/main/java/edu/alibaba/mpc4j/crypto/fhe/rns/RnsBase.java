package edu.alibaba.mpc4j.crypto.fhe.rns;

import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.zq.*;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * RNS Base Class.
 * Represents a group of co-prime moduli(Modulus objects): [q1, q2, ..., qk], q or Q = \prod q_i
 * providing decompose(convert x in Z_q to Z_{q_i} ) and compose (convert x_i \in Z_{q_i} to x \in Z_q)
 * functions.
 *
 * @author Qixian Zhou
 * @date 2023/8/17
 */
public class RnsBase {

    // size of base, number of q_i
    private int size;
    // Z_{q_i}
    private Modulus[] base;
    // q = \prod q_i, note that this is a base-2^64 number, just represented by long[]
    private long[] baseProd;
    // q_i^* = q/q_i, note that each q^* is a base-2^64 number, just represented by long[], there are k q_i^*, so use 2-d array.
    private long[][] puncturedProdArray;
    // \tilde{q_i} = (q_i^*)^{-1} mod base, because single base's max bit is 61-bit, so we can use long represents \tilde{q_i}, total k, so use 1-D array
    // note that we use MultiplyUintModOperand for faster modular multiplication
    private MultiplyUintModOperand[] invPuncturedProdModBaseArray;

    public RnsBase(Modulus[] rnsBase) {
        assert rnsBase.length > 0;
        this.size = rnsBase.length;
        // co-prime check
        for (int i = 0; i < size; i++) {
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
            // in our implementation, a valid modulus must not be zero, so omit this check.
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

    public RnsBase(RnsBase copy) {

        this.size = copy.size;
        long[] baseValues = new long[size];
        for (int i = 0; i < size; i++) {
            baseValues[i] = copy.base[i].getValue();
        }
        this.base = Arrays.stream(baseValues).parallel().mapToObj(Modulus::new).toArray(Modulus[]::new);

        this.baseProd = new long[size];
        System.arraycopy(copy.baseProd, 0, baseProd, 0, size);

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
     * @param value a base-2^64 value
     */
    public void decompose(long[] value) {
        assert value.length == size;
        if (size > 1) {
            long[] valueCopy = new long[value.length];
            UintCore.setUint(value, size, valueCopy);

            IntStream.range(0, size)
                    .parallel()
                    .forEach(i -> value[i] = UintArithmeticSmallMod.moduloUint(valueCopy, size, this.base[i]));
        }
        // todo: if size == 1, no need handle?
        // even if our modulus up to 61-bit, but when value is a long, may > 61-bit, this time we still need to mod.
        // In SEAL, do not handle this.
    }

    /**
     * Note that values is a k * N matrix, N represent the number of values, k represent the length of value, the uint64 count is k.
     * for example:
     * [0 1 0]
     * [1 0 1]
     * [1 1 1]
     * decompose it should by-column, [0, 1, 1]^T is a value, [1, 0, 1]^T is a value, [0, 1, 1]^T is a value.
     * the result in-place store in values.
     *
     *
     * @param values a k * N matrix, N represent the number of values, k represent the length of value, the uint64 count is k.
     */
    public void decomposeArray(long[][] values) {
        assert values.length == size;

        if (size > 1) {
            int count = values[0].length;
            // copy every column, just copy every value
            // N * k , just transpose
            long[][] columns = new long[count][size];
            IntStream.range(0, count).parallel().forEach(
                    j -> {
                        IntStream.range(0, size).parallel().forEach(
                                i -> columns[j][i] = values[i][j]
                        );
                    }
            );
            // now columns[i] represent a value, i \in [0, count)

            // now value mod q_i, the result overwrite the values
            //          c0              c1     ....    cn
            //          |               |               |
            //
            //       [ c0 mod q_1, c1 mod q1, ...., cN mod q1]
            //       [ c0 mod q_2, c1 mod q2, ...., cN mod q2]
            //               ......
            //       [ c0 mod qk, c1 mod qk, ....., cN mod qk]


            // decompose by row
            IntStream.range(0, size).parallel().forEach(
                    i -> {
                        IntStream.range(0, count).parallel().forEach(
                                j -> {
                                    values[i][j] = UintArithmeticSmallMod.moduloUint(columns[j], size, base[i]);
                                }
                        );
                    }
            );

        }
    }

    /**
     * Compose value in-place: [v1 = value mod q1, v2 = value mod q2, ...., vk = value mod qk].
     * the basic idea is: v = (\sum v_i * \tilde{q_i} * q_i^*) mod q
     * specific: v = (\sum [v_i * \tilde{q_i}]_{q_i} * q_i^*) mod q
     *
     * @param value a base-2^64 value
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
                // long[] * long
                UintArithmetic.multiplyUint(puncturedProdArray[i], size, tmpProd, size, tempMpi);
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
    public void composeArray(long[][] values) {
        if (size > 1) {
            int count = values[0].length;
            // copy every column, just copy every value
            // N * k , just transpose
            long[][] columns = new long[count][size];
            IntStream.range(0, count).parallel().forEach(
                    j -> {
                        IntStream.range(0, size).parallel().forEach(
                                i -> columns[j][i] = values[i][j]
                        );
                    }
            );
            // 对每一列 compose, in-place
            IntStream.range(0, count).parallel().forEach(
                   i -> compose(columns[i])
            );
            // 再转置 回 values
            IntStream.range(0, size).parallel().forEach(
                    i -> IntStream.range(0, count).parallel().forEach(
                            j -> values[i][j] = columns[j][i]
                    )
            );
        }
    }


    /**
     * initialize CRT data by given base, mainly compute q_i^* = q/q_i \in Z,  \tilde{q_i} = (q_i^*)^{-1}  mod q_i \in Z_{q_i}
     *
     * @return if initialize success return true, otherwise return false.
     */
    private boolean initialize() {
        // why need check this?
        Common.mulSafe(size, size, false);

        baseProd = new long[size];
        puncturedProdArray = new long[size][size];
        invPuncturedProdModBaseArray = IntStream.range(0, size).parallel().mapToObj(
                i -> new MultiplyUintModOperand()
        ).toArray(MultiplyUintModOperand[]::new);

        if (size > 1) {
            long[] baseValues = IntStream.range(0, size).parallel().mapToLong(i -> base[i].getValue()).toArray();
            boolean invertible = true;
            for (int i = 0; i < size; i++) {
                // q_i* = Q/qi = \prod p_j, j \ne i.
                UintArithmetic.multiplyManyUint64Except(baseValues, size, i, puncturedProdArray[i]);
                // \hat{q_i} = (Q/qi)^{-1} mod qi
                // first reduce (Q/qi) to [0, qi), then compute inv
                long tmp = UintArithmeticSmallMod.moduloUint(puncturedProdArray[i], size, base[i]);
                long[] tmpInv = new long[1];
                invertible = invertible && UintArithmeticSmallMod.tryInvertUintMod(tmp, base[i], tmpInv);
                invPuncturedProdModBaseArray[i].set(tmpInv[0], base[i]);
            }
            // Q = (Q/q0) * q0
            UintArithmetic.multiplyUint(puncturedProdArray[0], size, base[0].getValue(),size, baseProd);

            return invertible;
        }
        // only one q1, q = q1
        baseProd[0] = base[0].getValue();
        puncturedProdArray[0] = new long[]{1L}; // q = q1, q/q1 = 1
        invPuncturedProdModBaseArray[0].set(1, base[0]);

        return true;
    }

    /**
     * @param other another RnsBase object
     * @return a new RnsBase object, which base is [this.base, other.base]
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
        // why use addSafe?
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
     * @param value a Modulus object
     * @return a nwe RnsBase object, which base added a new Modulus value
     */
    public RnsBase extend(Modulus value) {
        // only one pair are not co-prime, then throw exception
        if (Arrays.stream(base).parallel().anyMatch(m -> !Numth.areCoPrime(m.getValue(), value.getValue()))) {
            throw new IllegalArgumentException("cannot extend by given value");
        }


        RnsBase newBase = new RnsBase();
        // why use addSafe?
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
     * @param value a long value, represent a q_i
     * @return a nwe RnsBase object, which base added a new Modulus(value)
     */
    public RnsBase extend(long value) {
        // only one pair are not co-prime, then throw exception
        if (Arrays.stream(base).parallel().anyMatch(m -> !Numth.areCoPrime(m.getValue(), value))) {
            throw new IllegalArgumentException("cannot extend by given value");
        }
        RnsBase newBase = new RnsBase();
        // why need to use addSafe?
        newBase.size = Common.addSafe(size, 1, false);
        newBase.base = new Modulus[newBase.size];
        // shallow copy
        System.arraycopy(this.base, 0, newBase.base, 0, this.size);
        // extend
        newBase.base[size] = new Modulus(value);
        if (!newBase.initialize()) {
            throw new IllegalArgumentException("cannot extend by given value");
        }
        return newBase;
    }

    /**
     * remove the last moduli in current base and return a new RnsBase object
     *
     * @return a new RnsBase object
     */
    public RnsBase drop() {

        if (this.size == 1) {
            throw new RuntimeException("cannot drop from base of size 1");
        }

        RnsBase newBase = new RnsBase();
        newBase.size = this.size - 1;
        newBase.base = new Modulus[newBase.size];
        //shallow copy
        System.arraycopy(this.base, 0, newBase.base, 0, newBase.size);
        // initialize CRT data
        newBase.initialize();
        return newBase;
    }

    /**
     * @param value a moduli, needed to be removed
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
     * @param value a moduli, needed to be removed
     * @return a new RnsBase object, which base removed given value.
     */
    public RnsBase drop(long value) {
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
            if (this.base[sourceIndex].getValue() != value) {
                newBase.base[destIndex] = this.base[sourceIndex];
                destIndex++;
            }
            sourceIndex++;
        }
        // Initialize CRT data
        newBase.initialize();
        return newBase;
    }




    public boolean contains(Modulus value) {
        // objects compare can not use ==
        return Arrays.stream(base).parallel().anyMatch(m -> m.equals(value));
    }

    public boolean isSubBaseOf(RnsBase superBase) {
        return Arrays.stream(base).parallel().allMatch(superBase::contains);
    }

    public boolean isSuperBaseOf(RnsBase subBase) {
        return subBase.isSubBaseOf(this);
    }

    public boolean contains(long value) {
        return Arrays.stream(base).parallel().map(Modulus::getValue).anyMatch(v -> v == value);
    }

    public long[] getBaseProd() {
        return baseProd;
    }

    public MultiplyUintModOperand[] getInvPuncturedProdModBaseArray() {
        return invPuncturedProdModBaseArray;
    }

    public MultiplyUintModOperand getInvPuncturedProdModBaseArray(int index) {
        return invPuncturedProdModBaseArray[index];
    }



    public long[][] getPuncturedProdArray() {
        return puncturedProdArray;
    }

    public long[] getPuncturedProdArray(int index) {
        return puncturedProdArray[index];
    }

    public Modulus[] getBase() {
        return base;
    }

    public Modulus getBase(int index) {
        return base[index];
    }

    public int getSize() {
        return size;
    }
}
