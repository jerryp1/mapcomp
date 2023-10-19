package edu.alibaba.mpc4j.crypto.fhe.ntt;

import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.utils.Constants;
import edu.alibaba.mpc4j.crypto.fhe.zq.*;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * @author Qixian Zhou
 * @date 2023/8/27
 */
public class NttTables {


    private long root;

    private long invRoot;

    private int coeffCountPower;

    private int coeffCount;

    private Modulus modulus;

    // Inverse of coeff_count_ modulo modulus_.
    private MultiplyUintModOperand invDegreeModulo;

    // Holds 1~(n-1)-th powers of root_ in bit-reversed order, the 0-th power is left unset.
    MultiplyUintModOperand[] rootPowers;

    // Holds 1~(n-1)-th powers of inv_root_ in scrambled order, the 0-th power is left unset.
    MultiplyUintModOperand[] invRootPowers;

    ModArithLazy modArithLazy;

    NttHandler nttHandler;

    @Override
    public String toString() {
        return "NttTables{" +
                "root=" + root +
                ", invRoot=" + invRoot +
                ", coeffCountPower=" + coeffCountPower +
                ", coeffCount=" + coeffCount +
                ", modulus=" + modulus +
                ", invDegreeModulo=" + invDegreeModulo +
                ", \n rootPowers=" + Arrays.toString(rootPowers) +
                ", \n invRootPowers=" + Arrays.toString(invRootPowers) +
                '}';
    }

    public NttTables() {}


    public NttTables(int coeffCountPower, Modulus modulus) {
        initialize(coeffCountPower, modulus);
    }

    private void initialize(int coeffCountPower, Modulus modulus) {

        assert coeffCountPower >= UintCore.getPowerOfTwo(Constants.POLY_MOD_DEGREE_MIN)
                && coeffCountPower <= UintCore.getPowerOfTwo(Constants.POLY_MOD_DEGREE_MAX);

        this.coeffCountPower = coeffCountPower;
        this.coeffCount = 1 << coeffCountPower;
        this.modulus = modulus;
        // We defer parameter checking to try_minimal_primitive_root(...)

        long[] temp = new long[1];
        // 2n-th unity of root under mod modulus
        if (!Numth.tryMinimalPrimitiveRoot(2L * coeffCount, modulus, temp)) {
            throw new IllegalArgumentException("invalid modulus");
        }
        this.root = temp[0];

        if (!Numth.tryInvertUintMod(root, modulus.getValue(), temp)){
            throw new IllegalArgumentException("invalid modulus");
        }
        this.invRoot = temp[0];

        // Populate tables with powers of root in specific orders.
//        this.rootPowers = IntStream.range(0, coeffCount).parallel()
//                .mapToObj(i -> new MultiplyUintModOperand())
//                .toArray(MultiplyUintModOperand[]::new);
        this.rootPowers = new MultiplyUintModOperand[coeffCount];
        for (int i = 0; i < coeffCount; i++) {
            this.rootPowers[i] = new MultiplyUintModOperand();
        }


        MultiplyUintModOperand rootTemp = new MultiplyUintModOperand();
        rootTemp.set(root, modulus);
        long power = root;
        for (int i = 1; i < coeffCount; i++) {
            rootPowers[Common.reverseBits(i, coeffCountPower)].set(power, modulus);
            power = UintArithmeticSmallMod.multiplyUintMod(power, rootTemp, modulus);
        }
        rootPowers[0].set(1, modulus);

//        this.invRootPowers = IntStream.range(0, coeffCount).parallel()
//                .mapToObj(i -> new MultiplyUintModOperand())
//                .toArray(MultiplyUintModOperand[]::new);
        this.invRootPowers = new MultiplyUintModOperand[coeffCount];
        for (int i = 0; i < coeffCount; i++) {
            this.invRootPowers[i] = new MultiplyUintModOperand();
        }


        rootTemp.set(invRoot, modulus);
        power = invRoot;
        for (int i = 1; i < coeffCount; i++) {
            // 为何和前面的 index 不是对称的？
            invRootPowers[Common.reverseBits(i - 1, coeffCountPower) + 1].set(power, modulus);
            power = UintArithmeticSmallMod.multiplyUintMod(power, rootTemp, modulus);
        }
        invRootPowers[0].set(1, modulus);

        // Compute n^(-1) modulo q.
        if (!Numth.tryInvertUintMod((long) coeffCount, modulus.getValue(), temp)) {
            throw new IllegalArgumentException("invalid modulus");
        }
        invDegreeModulo = new MultiplyUintModOperand();
        invDegreeModulo.set(temp[0], modulus);

        modArithLazy = new ModArithLazy(modulus);
        nttHandler = new NttHandler(modArithLazy);
    }



    public NttTables(NttTables copy) {

        this.root = copy.root;
        this.invRoot = copy.invRoot;
        this.coeffCountPower = copy.coeffCountPower;
        this.coeffCount = copy.coeffCount;
        this.modulus = copy.modulus;
        this.invDegreeModulo = copy.invDegreeModulo;

        this.rootPowers = new MultiplyUintModOperand[coeffCount];
        this.invRootPowers = new MultiplyUintModOperand[coeffCount];

        System.arraycopy(copy.rootPowers, 0, this.rootPowers, 0, coeffCount);
        System.arraycopy(copy.invRootPowers, 0, this.invRootPowers, 0, coeffCount);
    }






    public int getCoeffCount() {
        return coeffCount;
    }

    public int getCoeffCountPower() {
        return coeffCountPower;
    }

    public long getInvRoot() {
        return invRoot;
    }

    public long getRoot() {
        return root;
    }

    public ModArithLazy getModArithLazy() {
        return modArithLazy;
    }

    public Modulus getModulus() {
        return modulus;
    }

    public NttHandler getNttHandler() {
        return nttHandler;
    }

    public MultiplyUintModOperand getInvDegreeModulo() {
        return invDegreeModulo;
    }


    public MultiplyUintModOperand[] getRootPowers() {
        return rootPowers;
    }

    public MultiplyUintModOperand getRootPowers(int index) {
        assert index < coeffCount;

        return rootPowers[index];
    }

    public MultiplyUintModOperand[] getInvRootPowers() {
        return invRootPowers;
    }

    public MultiplyUintModOperand getInvRootPowers(int index) {
        assert index < coeffCount;
        return invRootPowers[index];
    }


}
