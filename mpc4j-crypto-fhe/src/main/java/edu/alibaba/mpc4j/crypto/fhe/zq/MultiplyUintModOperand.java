package edu.alibaba.mpc4j.crypto.fhe.zq;

import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;

/**
 *  This struct contains a operand and a precomputed quotient: (operand << 64) / modulus, for a specific modulus.
 *  When passed to multiply_uint_mod, a faster variant of Barrett reduction will be performed. Operand must be less than modulus.
 *  In addition, this method is also called Shoup's Modular Multiplication.
 *
 * @author Qixian Zhou
 * @date 2023/8/10
 */
public class MultiplyUintModOperand implements Cloneable {

    public long operand;
    public long quotient;


    public MultiplyUintModOperand() {
        operand = 0;
        quotient = 0;
    }

    /**
     * re-compute the quotient by: 2^64 * operand / modulus.value
     * @param modulus
     */
    public void setQuotient(Modulus modulus) {

        assert operand < modulus.getValue();

        long[] wideQuotient = new long[2];
        // operand <<= 64
        long[] wideCoeff = new long[]{0, operand};

        UintArithmetic.divideUint128Inplace(wideCoeff, modulus.getValue(), wideQuotient);
        quotient = wideQuotient[0];
    }

    /**
     * set operand and computation 2^64 * operand / modulus.value
     * @param newOperand
     * @param modulus
     */
    public void set(long newOperand, Modulus modulus) {
        assert newOperand < modulus.getValue();
        operand = newOperand;
        setQuotient(modulus);
    }


    @Override
    public String toString() {
        return "MultiplyUintModOperand{" +
                "operand=" + operand +
                ", quotient=" + quotient +
                '}';
    }

    @Override
    public MultiplyUintModOperand clone() {
        try {
            MultiplyUintModOperand clone = (MultiplyUintModOperand) super.clone();
            // TODO: copy mutable state here, so the clone can't change the internals of the original
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
