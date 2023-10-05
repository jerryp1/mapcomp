package edu.alibaba.mpc4j.crypto.fhe.ntt;

import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.zq.MultiplyUintModOperand;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintArithmeticSmallMod;

/**
 * @author Qixian Zhou
 * @date 2023/8/27
 */
public class ModArithLazy {

    private Modulus modulus;

    private long twoTimesModulus;


    public ModArithLazy() {};

    public ModArithLazy(Modulus modulus) {

        this.modulus = modulus;

        this.twoTimesModulus = modulus.getValue() << 1;

    }

    @Override
    public String toString() {
        return "ModArithLazy{" +
                "modulus=" + modulus +
                ", twoTimesModulus=" + twoTimesModulus +
                '}';
    }

    public long add(long a, long b) {
        return  a + b;
    }

    public long sub(long a, long b) {
        return a + twoTimesModulus - b;
    }

    public long mulRoot(long a, MultiplyUintModOperand r) {
        return UintArithmeticSmallMod.multiplyUintModLazy(a, r, modulus);
    }

    public long mulScalar(long a, MultiplyUintModOperand s) {
        return UintArithmeticSmallMod.multiplyUintModLazy(a, s, modulus);
    }


    public MultiplyUintModOperand mulRootScalar(MultiplyUintModOperand r, MultiplyUintModOperand s) {

        MultiplyUintModOperand result = new MultiplyUintModOperand();
        result.set(UintArithmeticSmallMod.multiplyUintMod(r.operand, s, modulus), modulus);
        return result;
    }

    public long guard(long a) {
        return a >= twoTimesModulus ? a - twoTimesModulus: a;
    }

}
