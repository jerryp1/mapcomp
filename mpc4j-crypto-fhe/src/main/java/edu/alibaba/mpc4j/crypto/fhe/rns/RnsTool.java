package edu.alibaba.mpc4j.crypto.fhe.rns;

import edu.alibaba.mpc4j.crypto.fhe.iterator.RnsIter;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.ntt.NttTables;
import edu.alibaba.mpc4j.crypto.fhe.ntt.NttTablesCreateIter;
import edu.alibaba.mpc4j.crypto.fhe.ntt.NttTool;
import edu.alibaba.mpc4j.crypto.fhe.rq.PolyArithmeticSmallMod;
import edu.alibaba.mpc4j.crypto.fhe.utils.Constants;
import edu.alibaba.mpc4j.crypto.fhe.zq.*;

import java.util.stream.IntStream;

/**
 * This class implements the BEHZ RNS scheme. The scheme comes from:
 * <p>
 * A full rns variant of fv like somewhat homomorphic encryption schemes(BEHZ). https://eprint.iacr.org/2016/510
 * <p/>
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/rns.h#L190
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/8/21
 */
public class RnsTool {

    // N
    int coeffCount;
    // cipher Modulus Q
    RnsBase baseQ;

    RnsBase baseB;

    RnsBase baseBsk;
    // B_sk U \tilde m
    RnsBase baseBskMTilde;
    // {t, \gamma} in Algorithm 1
    RnsBase baseTGamma;

    BaseConverter baseQToBskConv;

    BaseConverter baseQToMTildeConv;

    // Base converter: B --> q
    BaseConverter baseBToQConv;

    BaseConverter baseBToMskConv;

    BaseConverter baseQToTGammaConv;

    BaseConverter baseQToTConv;
    // prod(q)^{-1} mod Bsk, note that this is Shoup Representation for faster multiplication
    MultiplyUintModOperand[] invProdQModBsk;
    // - prod(q)^{-1} mod m_tilde
    MultiplyUintModOperand negInvProdQModMTilde;
    // prod(B)^{-1} mod m_sk
    MultiplyUintModOperand invProdBModMsk;
    // gamma^{-1} mod t
    MultiplyUintModOperand invGammaModT;
    // prod(B) mod q
    long[] prodBModQ;
    // m_tilde^(-1) mod Bsk
    MultiplyUintModOperand[] invMTildeModBsk;
    // prod(q) mod Bsk
    long[] prodQModBsk;

    // -prod(q)^(-1) mod {t, gamma}
    MultiplyUintModOperand[] negInvQModTGamma;

    // prod({t, gamma}) mod q
    MultiplyUintModOperand[] prodTGammaModQ;

    // q[last]^(-1) mod q[i] for i = 0..last-1
    MultiplyUintModOperand[] invQLastModQ;
    // NTTTables for Bsk
    NttTables[] baseBskNttTables;

    Modulus mTilde;

    Modulus mSk;
    // plaintext modulus
    Modulus t;

    Modulus gamma;

    long invQLastModT;

    long qLastModT;


    /**
     * @param polyModulusDegree N
     * @param coeffModulus      Ciphertext coeffs (q or Q) in Rns representation
     * @param plainModulus      Plaintext coeffs t
     */
    public RnsTool(int polyModulusDegree, RnsBase coeffModulus, Modulus plainModulus) {

        initialize(polyModulusDegree, coeffModulus, plainModulus);
    }

    public RnsTool(int polyModulusDegree, long[] coeffModulus, Modulus plainModulus) {
        RnsBase base = new RnsBase(coeffModulus);
        initialize(polyModulusDegree, base, plainModulus);
    }


    private void initialize(int polyModulusDegree, RnsBase q, Modulus t) {

        if (q.getSize() < Constants.COEFF_MOD_COUNT_MIN || q.getSize() > Constants.COEFF_MOD_COUNT_MAX) {
            throw new IllegalArgumentException("rns base is invalid.");
        }
        // Return if coeff_count is not a power of two or out of bounds
        int coeffCountPower = UintCore.getPowerOfTwo(polyModulusDegree);
        if (coeffCountPower < 0 || polyModulusDegree > Constants.POLY_MOD_DEGREE_MAX || polyModulusDegree < Constants.POLY_MOD_DEGREE_MIN) {
            throw new IllegalArgumentException("polyModulusDegree is invalid.");
        }

        this.t = t;
        this.coeffCount = polyModulusDegree;
        // number of moduli in RnsBase q, allocate length for bases q, B, Bsk, Bsk U m_tilde, t_gamma
        int baseQSize = q.getSize();

        // In some cases we might need to increase the size of the base B by one, namely we require
        // K * n * t * q^2 < q * prod(B) * m_sk, where K takes into account cross terms when larger size ciphertexts
        // are used, and n is the "delta factor" for the ring. We reserve 32 bits for K * n. Here the coeff modulus
        // primes q_i are bounded to be SEAL_USER_MOD_BIT_COUNT_MAX (60) bits, and all primes in B and m_sk are
        // SEAL_INTERNAL_MOD_BIT_COUNT (61) bits.

        int totalCoeffBitCount = UintCore.getSignificantBitCountUint(q.getBaseProd(), q.getSize());
        // why baseBSize is this?
        int baseBSize = baseQSize;
        if (32 + this.t.getBitCount() + totalCoeffBitCount >=
                Constants.INTERNAL_MOD_BIT_COUNT * baseQSize + Constants.INTERNAL_MOD_BIT_COUNT) {
            baseBSize++;
        }
        // why use addSafe instead of normal add?
        // base B extend modulus m_{sk} ---> B_{sk}, so size + 1
        int baseBskSize = Common.addSafe(baseBSize, 1, true);
        // base B_{sk} extend single-modulus \tilde m ---> B_{sk} U \tilde m, so size + 1
        int baseBskMTildeSize = Common.addSafe(baseBskSize, 1, true);

        int baseTGammaSize = 0;

        // why need to check coeffCount ?
        Common.mulSafe(coeffCount, 1, true);
        // Sample primes for B and two more primes: m_sk and gamma, size it qSize + 2
        Modulus[] baseConvPrimes = Numth.getPrimes(Common.mulSafe(2, coeffCount, true), Constants.INTERNAL_MOD_BIT_COUNT, baseBskMTildeSize);

        mSk = baseConvPrimes[0];
        gamma = baseConvPrimes[1];
        Modulus[] baseBPrimes = new Modulus[baseBSize];
        System.arraycopy(baseConvPrimes, 2, baseBPrimes, 0, baseBSize);

        // Set m_tilde_ to a non-prime value
        mTilde = new Modulus(1L << 32);
        // Populate the base arrays
        baseQ = new RnsBase(q);
        baseB = new RnsBase(baseBPrimes);
        baseBsk = baseB.extend(mSk);
        baseBskMTilde = baseBsk.extend(mTilde);

        // consider remove this if, because in our implementation, a valid modulus must not be zero
        // this is due to in CKKS, t maybe is 0
        if (!this.t.isZero()) {
            baseTGammaSize = 2;
            baseTGamma = new RnsBase(new Modulus[]{t, gamma});
        }

        // todo: create NTTTablses
        baseBskNttTables = new NttTables[baseBskSize];
        for (int i = 0; i < baseBSize; i++) {
            baseBskNttTables[i] = new NttTables();
        }
        try {
            NttTablesCreateIter.createNttTables(
                    coeffCountPower,
                    baseBsk.getBase(),// 这里是一种浅拷贝
                    baseBskNttTables);
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid rns bases");
        }

        if (!this.t.isZero()) {
            // Set up BaseConvTool for q --> {t}
            baseQToTConv = new BaseConverter(baseQ, new RnsBase(new Modulus[]{this.t}));
        }
        // Set up BaseConverter for q --> Bsk
        baseQToBskConv = new BaseConverter(baseQ, baseBsk);

        // Set up BaseConverter for q --> {m_tilde}
        baseQToMTildeConv = new BaseConverter(baseQ, new RnsBase(new Modulus[]{mTilde}));

        // Set up BaseConverter for B --> q
        baseBToQConv = new BaseConverter(baseB, baseQ);

        // Set up BaseConverter for B --> {m_sk}
        baseBToMskConv = new BaseConverter(baseB, new RnsBase(new Modulus[]{mSk}));

        //
        if (baseTGamma != null) {
            // Set up BaseConverter for q --> {t, gamma}
            baseQToTGammaConv = new BaseConverter(baseQ, baseTGamma);
        }

        // Compute prod(B) mod q = [q1, q2, ..., qk]
        prodBModQ = new long[baseQSize];
        for (int i = 0; i < baseQSize; i++) {
            prodBModQ[i] = UintArithmeticSmallMod.moduloUint(baseB.getBaseProd(), baseBSize, baseQ.getBase(i));

        }

        // Compute prod(q)^(-1) mod Bsk, which has many modulus
        invProdQModBsk = new MultiplyUintModOperand[baseBskSize];
        for (int i = 0; i < baseBskSize; i++) {
            invProdQModBsk[i] = new MultiplyUintModOperand();
        }
//        invProdQModBsk = IntStream.range(0, baseBskSize).parallel()
//                .mapToObj(i -> new MultiplyUintModOperand())
//                .toArray(MultiplyUintModOperand[]::new);


        for (int i = 0; i < baseBskSize; i++) {
            // // first reduce prod(q) to Bsk[i]
            long innerTemp = UintArithmeticSmallMod.moduloUint(baseQ.getBaseProd(), baseQSize, baseBsk.getBase(i));
            // then compute inverse
            long[] innerInvTemp = new long[1];
            if (!UintArithmeticSmallMod.tryInvertUintMod(innerTemp, baseBsk.getBase(i), innerInvTemp)) {
                throw new IllegalArgumentException("invalid rns bases");
            }
            // initialize a MultiplyUintModOperand
            invProdQModBsk[i].set(innerInvTemp[0], baseBsk.getBase(i));
        }
//        IntStream.range(0, baseBskSize).parallel().forEach(
//                // first reduce prod(q) to Bsk[i]
//                i -> {
//                    long innerTemp = UintArithmeticSmallMod.moduloUint(baseQ.getBaseProd(), baseQSize, baseBsk.getBase(i));
//                    // then compute inverse
//                    long[] innerInvTemp = new long[1];
//                    if (!UintArithmeticSmallMod.tryInvertUintMod(innerTemp, baseBsk.getBase(i), innerInvTemp)) {
//                        throw new IllegalArgumentException("invalid rns bases");
//                    }
//                    // initialize a MultiplyUintModOperand
//                    invProdQModBsk[i].set(innerInvTemp[0], baseBsk.getBase(i));
//                }
//        );
        // used for store the result of reduce and invert
        long temp;
        long[] invTemp = new long[1];
        // Compute prod(B)^(-1) mod m_sk, m_sk is a single modulus
        temp = UintArithmeticSmallMod.moduloUint(baseB.getBaseProd(), baseBSize, mSk);
        if (!UintArithmeticSmallMod.tryInvertUintMod(temp, mSk, invTemp)) {
            throw new IllegalArgumentException("invalid rns bases");
        }
        invProdBModMsk = new MultiplyUintModOperand();
        invProdBModMsk.set(invTemp[0], mSk);

        // Compute m_tilde^(-1) mod Bsk
//        invMTildeModBsk = IntStream.range(0, baseBskSize).
//                parallel().
//                mapToObj(i -> new MultiplyUintModOperand()).
//                toArray(MultiplyUintModOperand[]::new);
        invMTildeModBsk = new MultiplyUintModOperand[baseBskSize];
        for (int i = 0; i < baseBskSize; i++) {
            invMTildeModBsk[i] = new MultiplyUintModOperand();
        }

        for (int i = 0; i < baseBskSize; i++) {
            long[] innerInvTemp = new long[1];
            if (!UintArithmeticSmallMod.tryInvertUintMod(
                    UintArithmeticSmallMod.barrettReduce64(mTilde.getValue(), baseBsk.getBase(i)),
                    baseBsk.getBase(i),
                    innerInvTemp)) {
                throw new IllegalArgumentException("invalid rns bases");
            }
            invMTildeModBsk[i].set(innerInvTemp[0], baseBsk.getBase(i));
        }


        // Compute -prod(q)^(-1) mod m_tilde
        temp = UintArithmeticSmallMod.moduloUint(baseQ.getBaseProd(), baseQSize, mTilde);
        if (!UintArithmeticSmallMod.tryInvertUintMod(temp, mTilde, invTemp)) {
            throw new IllegalArgumentException("invalid rns bases");
        }
        // note that the neg
        negInvProdQModMTilde = new MultiplyUintModOperand();
        negInvProdQModMTilde.set(UintArithmeticSmallMod.negateUintMod(invTemp[0], mTilde), mTilde);

        // Compute prod(q) mod Bsk
        prodQModBsk = new long[baseBskSize];
//        IntStream.range(0, baseBskSize).parallel().forEach(
//                i -> {
//                    prodQModBsk[i] = UintArithmeticSmallMod.moduloUint(baseQ.getBaseProd(), baseQSize, baseBsk.getBase(i));
//                }
//        );
        for (int i = 0; i < baseBskSize; i++) {
            prodQModBsk[i] = UintArithmeticSmallMod.moduloUint(baseQ.getBaseProd(), baseQSize, baseBsk.getBase(i));
        }


        if (baseTGamma != null) {
            // Compute gamma^(-1) mod t
            if (!UintArithmeticSmallMod.tryInvertUintMod(
                    UintArithmeticSmallMod.barrettReduce64(gamma.getValue(), this.t),
                    this.t,
                    invTemp)) {
                throw new IllegalArgumentException("invalid rns bases");
            }
            invGammaModT = new MultiplyUintModOperand();
            invGammaModT.set(invTemp[0], this.t);

            // Compute prod({t, gamma}) mod q
            prodTGammaModQ = new MultiplyUintModOperand[baseQSize];
            for (int i = 0; i < baseQSize; i++) {
                prodTGammaModQ[i] = new MultiplyUintModOperand();
            }

            for (int i = 0; i < baseQSize; i++) {
                prodTGammaModQ[i].set(
                        // t * \gamma q_i
                        // todo: why don't use baseTGamma.getBaseProd() mod baseQ.getBase(i), which can avoid repeat multiplication?
                        UintArithmeticSmallMod.multiplyUintMod(baseTGamma.getBase(0).getValue(), baseTGamma.getBase(1).getValue(), baseQ.getBase(i)),
                        baseQ.getBase(i));
            }

            // Compute -prod(q)^(-1) mod {t, gamma}
            negInvQModTGamma = new MultiplyUintModOperand[baseTGammaSize];
            for (int i = 0; i < baseTGammaSize; i++) {
                negInvQModTGamma[i] = new MultiplyUintModOperand();
            }
//            note that we can not use Arrays.fill to initialize a object array, this way will lead to the every element in this array is a same object
//            negInvQModTGamma = new MultiplyUintModOperand[baseTGammaSize];
//            Arrays.fill(negInvQModTGamma, new MultiplyUintModOperand());

            for (int i = 0; i < baseTGammaSize; i++) {
                long curTemp = UintArithmeticSmallMod.moduloUint(baseQ.getBaseProd(), baseQSize, baseTGamma.getBase(i));
                long[] curInvTemp = new long[1];
                if (!UintArithmeticSmallMod.tryInvertUintMod(curTemp, baseTGamma.getBase(i), curInvTemp)) {
                    throw new IllegalArgumentException("invalid rns bases");
                }
                // neg
                negInvQModTGamma[i].set(
                        UintArithmeticSmallMod.negateUintMod(curInvTemp[0], baseTGamma.getBase(i)),
                        baseTGamma.getBase(i)
                );
            }
        }
        // Compute q[last]^(-1) mod q[i] for i = 0..last-1, i.e. q_k mod q_1, q_k mod q_2, ..., q_k mod q_{k-1}
        // This is used by modulus switching and rescaling
//        invQLastModQ = IntStream.range(0, baseQSize - 1)
//                .mapToObj(i -> new MultiplyUintModOperand())
//                .toArray(MultiplyUintModOperand[]::new);
        invQLastModQ = new MultiplyUintModOperand[baseQSize - 1];
        for (int i = 0; i < baseQSize - 1; i++) {
            invQLastModQ[i] = new MultiplyUintModOperand();
        }


        long qLast = baseQ.getBase(baseQSize - 1).getValue();
        for (int i = 0; i < baseQSize - 1; i++) {
            long[] curInvTemp = new long[1];
            if (!UintArithmeticSmallMod.tryInvertUintMod(qLast, baseQ.getBase(i), curInvTemp)) {
                throw new IllegalArgumentException("invalid rns bases");
            }
            invQLastModQ[i].set(curInvTemp[0], baseQ.getBase(i));
        }
        // compute q_k mod t and (q_k)^{-1} mod t
        if (!t.isZero()) {
            if (!UintArithmeticSmallMod.tryInvertUintMod(qLast, this.t, invTemp)) {
                throw new IllegalArgumentException("invalid rns bases");
            }
            invQLastModT = invTemp[0];
            qLastModT = UintArithmeticSmallMod.barrettReduce64(qLast, this.t);
        }
    }

    /**
     * long[] + N = RnsIter
     *
     * @param input
     * @param inputCoeffCount
     * @param destination
     */
    public void decryptScaleAndRound(long[] input, int inputCoeffCount, long[] destination) {

        assert input != null;
        assert inputCoeffCount == coeffCount;
        assert destination.length == coeffCount;

        int baseQSize = baseQ.getSize();
        int baseTGammaSize = baseTGamma.getSize();

        // Compute |gamma * t|_qi * ct(s), note that here is under RNS representation operation, |gamma*t|_{q_i} has been pre-computed, so has baseQSize
        // ct(s) is also a poly under RNS base Q, so has baseQSize fractions.
        // Algorihtm 1 line-2, 的第一个输入, |gamma * t|_{q_i} is the scalar
        // todo: remove RnsIter, just using long[]
        RnsIter temp = new RnsIter(baseQSize, coeffCount);
        for (int i = 0; i < baseQSize; i++) {
            // 注意这里的函数签名，通过指定 startIndex和coeffCount, 每一次处理的值就是
            // coeffIter[startIndex * coeffCount, (startIndx + 1) * coeffCount) 这样避免了原来的调用方式 .getCoeffIter(int i)
            // 因为现在 RnsIter 底层是一个一维数组，这样可以避免 对一维数组进行 split 操作带来的 new long[] 的开销
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(
                    input,
                    i * coeffCount,
                    coeffCount,
                    prodTGammaModQ[i],
                    baseQ.getBase(i),
                    i * coeffCount,
                    temp.coeffIter);
        }

        // Make another temp destination to get the poly in mod {t, gamma}
        RnsIter tempTGamma = new RnsIter(baseTGammaSize, coeffCount);
        // Convert from q to {t, gamma}
        baseQToTGammaConv.fastConvertArray(temp, tempTGamma);

        // Multiply by -prod(q)^(-1) mod {t, gamma}
        // line-2
        for (int i = 0; i < baseTGammaSize; i++) {
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(
                    tempTGamma.coeffIter,
                    i * coeffCount,
                    coeffCount,
                    negInvQModTGamma[i],
                    baseTGamma.getBase(i),
                    i * coeffCount,
                    tempTGamma.coeffIter);
        }
        // Need to correct values in temp_t_gamma (gamma component only) which are
        // larger than floor(gamma/2)
        // for  ||_p --> []_p , i.e. [0, q) ---> [-q/2, q/2)
        long gammaDiv2 = baseTGamma.getBase(1).getValue() >>> 1;

        // Now compute the subtraction to remove error and perform final multiplication by
        // gamma inverse mod t. just : s^{(t)}, s^{(\gamma)}, line-4/5
        for (int i = 0; i < coeffCount; i++) {
            // tempTGamma.getCoeffIter(1) is the gamma, [i] is the i-th count value
            if (tempTGamma.getCoeff(1, i) > gammaDiv2) {

                // Compute -(gamma - a) instead of (a - gamma)
                destination[i] = UintArithmeticSmallMod.addUintMod(
                        tempTGamma.getCoeff(0, i),
                        UintArithmeticSmallMod.barrettReduce64(gamma.getValue() - tempTGamma.getCoeff(1, i), t),
                        t);
            } else {
                // No correction needed, just no need gamma - a, directly use a, beacuse a \in [0, gamma/2)
                destination[i] = UintArithmeticSmallMod.subUintMod(
                        tempTGamma.getCoeff(0, i),
                        UintArithmeticSmallMod.barrettReduce64(tempTGamma.getCoeff(1, i), t),
                        t);
            }
            // now handle multiplication
            // If this coefficient was non-zero, multiply by gamma^(-1)
            // 对应 line-5 的乘法 ， 如果前面一项等于0， 自然不需要计算乘法了
            if (destination[i] != 0) {
                // Perform final multiplication by gamma inverse mod t
                destination[i] = UintArithmeticSmallMod.multiplyUintMod(
                        destination[i],
                        invGammaModT,
                        t);
            }
        }
    }

    /**
     * In-place decrypt, result store in destination.
     * ref: Algorithm 1 in BEHZ
     *
     * @param input       a poly in RnsBase q, k * N matrix
     * @param destination decrypt result, length is N
     */
    public void decryptScaleAndRound(RnsIter input, long[] destination) {

        assert input != null;
        assert input.getPolyModulusDegree() == coeffCount;
        assert destination.length == coeffCount;

        int baseQSize = baseQ.getSize();
        int baseTGammaSize = baseTGamma.getSize();

        // Compute |gamma * t|_qi * ct(s), note that here is under RNS representation operation, |gamma*t|_{q_i} has been pre-computed, so has baseQSize
        // ct(s) is also a poly under RNS base Q, so has baseQSize fractions.
        // Algorihtm 1 line-2, 的第一个输入, |gamma * t|_{q_i} is the scalar
        RnsIter temp = new RnsIter(baseQSize, coeffCount);
        for (int i = 0; i < baseQSize; i++) {
            // 注意这里的函数签名，通过指定 startIndex和coeffCount, 每一次处理的值就是
            // coeffIter[startIndex * coeffCount, (startIndx + 1) * coeffCount) 这样避免了原来的调用方式 .getCoeffIter(int i)
            // 因为现在 RnsIter 底层是一个一维数组，这样可以避免 对一维数组进行 split 操作带来的 new long[] 的开销
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(input.coeffIter, i * coeffCount, coeffCount, prodTGammaModQ[i], baseQ.getBase(i), i * coeffCount, temp.coeffIter);
        }
        // Make another temp destination to get the poly in mod {t, gamma}
        RnsIter tempTGamma = new RnsIter(baseTGammaSize, coeffCount);
        // Convert from q to {t, gamma}
        baseQToTGammaConv.fastConvertArray(temp, tempTGamma);

        // Multiply by -prod(q)^(-1) mod {t, gamma}
        // line-2
        for (int i = 0; i < baseTGammaSize; i++) {
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(tempTGamma.coeffIter, i * coeffCount, coeffCount, negInvQModTGamma[i], baseTGamma.getBase(i), i * coeffCount, tempTGamma.coeffIter);
        }

        // Need to correct values in temp_t_gamma (gamma component only) which are
        // larger than floor(gamma/2)
        // for  ||_p --> []_p , i.e. [0, q) ---> [-q/2, q/2)
        long gammaDiv2 = baseTGamma.getBase(1).getValue() >>> 1;

        // Now compute the subtraction to remove error and perform final multiplication by
        // gamma inverse mod t. just : s^{(t)}, s^{(\gamma)}, line-4/5
        for (int i = 0; i < coeffCount; i++) {
            // tempTGamma.getCoeffIter(1) is the gamma, [i] is the i-th count value
            if (tempTGamma.getCoeff(1, i) > gammaDiv2) {

                // Compute -(gamma - a) instead of (a - gamma)
                destination[i] = UintArithmeticSmallMod.addUintMod(
                        tempTGamma.getCoeff(0, i),
                        UintArithmeticSmallMod.barrettReduce64(gamma.getValue() - tempTGamma.getCoeff(1, i), t),
                        t);
            } else {
                // No correction needed, just no need gamma - a, directly use a, beacuse a \in [0, gamma/2)
                destination[i] = UintArithmeticSmallMod.subUintMod(
                        tempTGamma.getCoeff(0, i),
                        UintArithmeticSmallMod.barrettReduce64(tempTGamma.getCoeff(1, i), t),
                        t);
            }
            // now handle multiplication
            // If this coefficient was non-zero, multiply by gamma^(-1)
            // 对应 line-5 的乘法 ， 如果前面一项等于0， 自然不需要计算乘法了
            if (destination[i] != 0) {
                // Perform final multiplication by gamma inverse mod t
                destination[i] = UintArithmeticSmallMod.multiplyUintMod(destination[i], invGammaModT, t);
            }
        }

    }

    /**
     * 处理一个 RnsIter, long[] + startIndex + N + k 来表示这个 RnsIter
     *
     * @param input
     * @param destination
     */
    public void fastBConvMTildeRnsIter(long[] input,
                                       int inputStartIndex,
                                       int inputCoeffCount,
                                       int inputCoeffModulusSize,
                                       long[] destination,
                                       int destinationStartIndex,
                                       int destinationCoeffCount,
                                       int destinationCoeffModulusSize
    ) {

        assert input != null && destination != null;
        assert inputCoeffCount == coeffCount;
        assert destinationCoeffCount == coeffCount;
        assert inputCoeffModulusSize == baseQ.getSize();
        assert destinationCoeffModulusSize == baseBskMTilde.getSize();


        int baseQSize = baseQ.getSize();
        int baseBskSize = baseBsk.getSize();

        // We need to multiply first the input with m_tilde mod q
        // This is to facilitate Montgomery reduction in the next step of multiplication
        // This is NOT an ideal approach: as mentioned in BEHZ16, multiplication by
        // m_tilde can be easily merge into the base conversion operation; however, then
        // we could not use the BaseConverter as below without modifications.
        // k * N
        RnsIter temp = new RnsIter(baseQSize, coeffCount);
        // (input * \tilde m) mod q
        PolyArithmeticSmallMod.multiplyPolyScalarCoeffModRnsIter(
                input,
                inputStartIndex,
                inputCoeffCount,
                baseQSize,
                mTilde.getValue(),
                baseQ.getBase(),
                temp.coeffIter,
                0,
                coeffCount
        );


        // Now convert to Bsk
//        long[][] tempOut = new long[baseBskSize][coeffCount];
        long[] tempOut = new long[baseBskSize * coeffCount];
        baseQToBskConv.fastConvertArray(temp, tempOut);

        // Finally convert to {m_tilde}
        // mTilde is a single modulus, so baseSize is 1.
//        long[][] tempOut2 = new long[1][coeffCount];
        // 1 * coeffCount
        long[] tempOut2 = new long[coeffCount];
        baseQToMTildeConv.fastConvertArray(temp, tempOut2);

        // update destination, Now input is in Bsk U {\tilde m}
        // [tempOut, tempOut2] 组合为一个数组拷贝给 destination, 等价于  destination.update(tempOut, tempOut2);

        // copy tempOut
        System.arraycopy(tempOut, 0, destination, destinationStartIndex, tempOut.length);
        // copy tempOut2
        System.arraycopy(tempOut2, 0, destination, destinationStartIndex + tempOut.length, tempOut2.length);

    }


    /**
     * In-place convert
     *
     * @param input       in q
     * @param destination in B_sk U {\tilde m}
     */
    public void fastBConvMTilde(RnsIter input, RnsIter destination) {

        assert input != null && destination != null;
        assert input.getPolyModulusDegree() == coeffCount;
        assert destination.getPolyModulusDegree() == coeffCount;
        assert input.getRnsBaseSize() == baseQ.getSize();
        assert destination.getRnsBaseSize() == baseBskMTilde.getSize();


        int baseQSize = baseQ.getSize();
        int baseBskSize = baseBsk.getSize();

        // We need to multiply first the input with m_tilde mod q
        // This is to facilitate Montgomery reduction in the next step of multiplication
        // This is NOT an ideal approach: as mentioned in BEHZ16, multiplication by
        // m_tilde can be easily merge into the base conversion operation; however, then
        // we could not use the BaseConverter as below without modifications.
        // k * N
        RnsIter temp = new RnsIter(baseQSize, coeffCount);
        // (input * \tilde m) mod q
        PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(input, baseQSize, mTilde.getValue(), baseQ.getBase(), temp);

        // Now convert to Bsk
//        long[][] tempOut = new long[baseBskSize][coeffCount];
        long[] tempOut = new long[baseBskSize * coeffCount];
        baseQToBskConv.fastConvertArray(temp, tempOut);

        // Finally convert to {m_tilde}
        // mTilde is a single modulus, so baseSize is 1.
//        long[][] tempOut2 = new long[1][coeffCount];
        // 1 * coeffCount
        long[] tempOut2 = new long[coeffCount];
        baseQToMTildeConv.fastConvertArray(temp, tempOut2);

        // update destination, Now input is in Bsk U {\tilde m}
        destination.update(tempOut, tempOut2);
    }

    /**
     * 处理单个 RnsIter = long[] + startIndex + N + k
     *
     * @param input
     * @param destination
     */
    public void smMrqRnsIter(
            long[] input,
            int inputStartIndex,
            int inputCoeffCount,
            int inputCoeffModulusSize,
            long[] destination,
            int destinationStartIndex,
            int destinationCoeffCount,
            int destinationCoeffModulusSize) {

        assert input != null;
        assert inputCoeffCount == coeffCount;
        assert destination != null;
        assert destinationCoeffCount == coeffCount;

        // input's base must be Bsk U {\tilde m}
        assert inputCoeffModulusSize == baseBskMTilde.getSize();
        assert destinationCoeffModulusSize == baseBsk.getSize();

        int baseBskSize = baseBsk.getSize();
        // input base size is baseBskSize + 1, so last row index is baseBskSize, just the input mod \tilde m
        int inputMTildeStart = baseBskSize * coeffCount;
        long mTildeDiv2 = mTilde.getValue() >>> 1;

        // line-1: r_{\tilde m} = [-c^{''}_{\tilde m} / q]_{\tilde m}
        long[] rMTilde = new long[coeffCount];
        // using startIndex to avoid new inputMTilde
        // 只处理一个 CoeffIter
        PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(
                input,
                inputStartIndex + inputMTildeStart,
                coeffCount,
                negInvProdQModMTilde,
                mTilde,
                0,
                rMTilde);


        // line 2-4
        for (int i = 0; i < baseBskSize; i++) {

            MultiplyUintModOperand prodQModBskElt = new MultiplyUintModOperand();
            prodQModBskElt.set(prodQModBsk[i], baseBsk.getBase(i));

            for (int j = 0; j < coeffCount; j++) {
                long temp = rMTilde[j];
                if (temp >= mTildeDiv2) {
                    temp += (baseBsk.getBase(i).getValue() - mTilde.getValue());
                }
                // Compute ( input + q * r_m_tilde ) * m_tilde^(-1) mod Bsk
                destination[destinationStartIndex + i * destinationCoeffCount + j] =
                        UintArithmeticSmallMod.multiplyUintMod(
                                UintArithmeticSmallMod.multiplyAddUintMod(
                                        temp,
                                        prodQModBskElt,
                                        input[inputStartIndex + i * inputCoeffCount + j],
                                        baseBsk.getBase(i)),
                                invMTildeModBsk[i],
                                baseBsk.getBase(i)
                        );
            }

        }

    }


    /**
     * Small Montgomery Reduction mod q.
     * Ref Algorithm 2, page-10 in BEHZ16.
     *
     * @param input       in base Bsk U {m_tilde}
     * @param destination in base Bsk
     */
    public void smMrq(RnsIter input, RnsIter destination) {

        assert input.getPolyModulusDegree() == coeffCount;
        assert destination.getPolyModulusDegree() == coeffCount;
        // input's base must be Bsk U {\tilde m}
        assert input.getRnsBaseSize() == baseBskMTilde.getSize();
        assert destination.getRnsBaseSize() == baseBsk.getSize();

        int baseBskSize = baseBsk.getSize();
        // input base size is baseBskSize + 1, so last row index is baseBskSize, just the input mod \tilde m
//        long[] inputMTilde = input.getCoeffIter(baseBskSize);
        long mTildeDiv2 = mTilde.getValue() >>> 1;

        // line-1: r_{\tilde m} = [-c^{''}_{\tilde m} / q]_{\tilde m}
        long[] rMTilde = new long[coeffCount];
//        PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(inputMTilde, coeffCount, negInvProdQModMTilde, mTilde, rMTilde);
        // using startIndex to avoid new inputMTilde
        PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(input.coeffIter, baseBskSize * coeffCount, coeffCount, negInvProdQModMTilde, mTilde, 0, rMTilde);

        // line 2-4
        for (int i = 0; i < baseBskSize; i++) {
            MultiplyUintModOperand prodQModBskElt = new MultiplyUintModOperand();
            prodQModBskElt.set(prodQModBsk[i], baseBsk.getBase(i));

            for (int j = 0; j < coeffCount; j++) {
                long temp = rMTilde[j];
                if (temp >= mTildeDiv2) {
                    temp += (baseBsk.getBase(i).getValue() - mTilde.getValue());
                }
                // Compute ( input + q * r_m_tilde ) * m_tilde^(-1) mod Bsk
                destination.setCoeff(i, j,
                        UintArithmeticSmallMod.multiplyUintMod(
                                UintArithmeticSmallMod.multiplyAddUintMod(
                                        temp, prodQModBskElt, input.getCoeff(i, j), baseBsk.getBase(i)),
                                invMTildeModBsk[i],
                                baseBsk.getBase(i)
                        )
                );
            }
        }
    }


    public void fastFloorRnsIter(
            long[] input,
            int inputStartIndex,
            int inputCoeffCount,
            int inputCoeffModulusSize,
            long[] destination,
            int destinationStartIndex,
            int destinationCoeffCount,
            int destinationCoeffModulusSize) {

        assert input != null;
        assert inputCoeffCount == coeffCount;
        assert destination != null;
        assert destinationCoeffCount == coeffCount;
        // add
        assert inputCoeffModulusSize == baseQ.getSize() + baseBsk.getSize();
        assert destinationCoeffModulusSize == baseBsk.getSize();

        int baseQSize = baseQ.getSize();
        int baseBskSize = baseBsk.getSize();

        // Convert q -> Bsk
        // input 的结构是 [0, baseQSize * N | ..., (baseQSize + baseBskSize) * N)
//        RnsIter inputInBaseQ = input.subIter(0, baseQ.getSize());
        int inputInBaseQ = 0;
        // 处理 [0, baseQSize * N)
        baseQToBskConv.fastConvertArrayRnsIter(
                input,
                inputStartIndex + inputInBaseQ, // + 0
                inputCoeffCount,
                baseQ.getSize(), // 一定要注意这里，这里没有传入 inputCoeffModulusSize
                destination,
                destinationStartIndex,
                destinationCoeffCount,
                destinationCoeffModulusSize
        );
//        RnsIter inputInBaseBsk = input.subIter(baseQ.getSize());
        int inputInBsk = baseQSize * coeffCount;

        for (int i = 0; i < baseBskSize; i++) {
            for (int j = 0; j < coeffCount; j++) {

                // It is not necessary for the negation to be reduced modulo base_Bsk_elt
                destination[destinationStartIndex + i * coeffCount + j] =
                        UintArithmeticSmallMod.multiplyUintMod(
                                input[inputStartIndex + inputInBsk + i * coeffCount + j] + (baseBsk.getBase(i).getValue() - destination[destinationStartIndex + i * coeffCount + j]),
                                invProdQModBsk[i],
                                baseBsk.getBase(i)
                        );
            }
        }
    }


    /**
     * @param input       Input in base q U Bsk
     * @param destination Output in base Bsk
     */
    public void fastFloor(RnsIter input, RnsIter destination) {

        assert input.getPolyModulusDegree() == coeffCount;
        assert destination.getPolyModulusDegree() == coeffCount;
        assert input.getRnsBaseSize() == baseQ.getSize() + baseBsk.getSize();
        assert destination.getRnsBaseSize() == baseBsk.getSize();

        // Convert q -> Bsk
        RnsIter inputInBaseQ = input.subIter(0, baseQ.getSize());
        baseQToBskConv.fastConvertArray(inputInBaseQ, destination);

        //
        RnsIter inputInBaseBsk = input.subIter(baseQ.getSize());

        for (int i = 0; i < baseBsk.getSize(); i++) {

            for (int j = 0; j < coeffCount; j++) {
                // It is not necessary for the negation to be reduced modulo base_Bsk_elt
                destination.setCoeff(
                        i,
                        j,
                        UintArithmeticSmallMod.multiplyUintMod(
                                inputInBaseBsk.getCoeff(i, j) + (baseBsk.getBase(i).getValue() - destination.getCoeff(i, j)),
                                invProdQModBsk[i],
                                baseBsk.getBase(i))
                );
            }
        }
    }

    /**
     * Ref Lemma 6 the equation (13) in BEHZ16
     *
     * @param input       in base Bsk
     * @param destination in base q
     */
    public void fastBConvSkRnsIter(
            long[] input,
            int inputStartIndex,
            int inputCoeffCount,
            int inputCoeffModulusSize,
            long[] destination,
            int destinationStartIndex,
            int destinationCoeffCount,
            int destinationCoeffModulusSize) {

        assert inputCoeffCount == coeffCount;
        assert inputCoeffModulusSize == baseBsk.getSize();
        assert destinationCoeffModulusSize == baseQ.getSize();
        assert destinationCoeffCount == coeffCount;

        int baseQSize = baseQ.getSize();
        int baseBSize = baseB.getSize();

        // Fast convert B -> q; input is in Bsk but we only use B
        // 处理 [0, baseBSize * N)
//        RnsIter inputInBaseB = input.subIter(0, baseBSize);
        int inputInBaseB = 0;
        baseBToQConv.fastConvertArrayRnsIter(
                input,
                inputStartIndex + inputInBaseB,
                coeffCount,
                baseBSize,
                destination,
                destinationStartIndex,
                destinationCoeffCount,
                destinationCoeffModulusSize
        );

        // Compute alpha_sk
        // Fast convert B -> {m_sk}; input is in Bsk but we only use B
        long[] temp = new long[coeffCount];
//        RnsIter tempIter = new RnsIter(temp, coeffCount);
        // Compute alpha_sk
        // Fast convert B -> {m_sk}; input is in Bsk but we only use B
        baseBToMskConv.fastConvertArrayRnsIter(
                input,
                inputStartIndex + inputInBaseB,
                coeffCount,
                baseBSize,
                temp,
                0,
                coeffCount,
                1
        );
        // temp will be changed?

        // Take the m_sk part of input, subtract from temp, and multiply by inv_prod_B_mod_m_sk_
        // Note: input_sk is allocated in input[base_B_size]
        long[] alphaSk = new long[coeffCount];
        for (int i = 0; i < coeffCount; i++) {
            // It is not necessary for the negation to be reduced modulo the small prime
            alphaSk[i] = UintArithmeticSmallMod.multiplyUintMod(
                    temp[i] + (mSk.getValue() - input[inputStartIndex + baseBSize * coeffCount + i]),
                    invProdBModMsk,
                    mSk
            );
        }
        // alpha_sk is now ready for the Shenoy-Kumaresan conversion; however, note that our
        // alpha_sk here is not a centered reduction, so we need to apply a correction below.

        long mSkDiv2 = mSk.getValue() >>> 1;
        for (int i = 0; i < baseQSize; i++) {
            MultiplyUintModOperand prodBModQElt = new MultiplyUintModOperand();
            prodBModQElt.set(prodBModQ[i], baseQ.getBase(i));

            MultiplyUintModOperand negProdBModQElt = new MultiplyUintModOperand();
            negProdBModQElt.set(baseQ.getBase(i).getValue() - prodBModQ[i], baseQ.getBase(i));

            for (int j = 0; j < coeffCount; j++) {
                // Correcting alpha_sk since it represents a negative value
                if (alphaSk[j] > mSkDiv2) {
                    destination[destinationStartIndex + i * coeffCount + j] =
                            UintArithmeticSmallMod.multiplyAddUintMod(
                                    UintArithmeticSmallMod.negateUintMod(alphaSk[j], mSk),
                                    prodBModQElt,
                                    destination[destinationStartIndex + i * coeffCount + j],
                                    baseQ.getBase(i)
                            );
                } else {
                    destination[destinationStartIndex + i * coeffCount + j] =
                            UintArithmeticSmallMod.multiplyAddUintMod(
                                    alphaSk[j],
                                    negProdBModQElt,
                                    destination[destinationStartIndex + i * coeffCount + j],
                                    baseQ.getBase(i)

                            );
                }
            }
        }
    }

    /**
     * Ref Lemma 6 the equation (13) in BEHZ16
     *
     * @param input       in base Bsk
     * @param destination in base q
     */
    public void fastBConvSk(RnsIter input, RnsIter destination) {

        assert input.getPolyModulusDegree() == coeffCount;
        assert input.getRnsBaseSize() == baseBsk.getSize();
        assert destination.getRnsBaseSize() == baseQ.getSize();

        int baseQSize = baseQ.getSize();
        int baseBSize = baseB.getSize();

        // Fast convert B -> q; input is in Bsk but we only use B
        RnsIter inputInBaseB = input.subIter(0, baseBSize);
        baseBToQConv.fastConvertArray(inputInBaseB, destination);

        // Compute alpha_sk
        // Fast convert B -> {m_sk}; input is in Bsk but we only use B
        long[] temp = new long[coeffCount];
        RnsIter tempIter = new RnsIter(temp, coeffCount);
        // Compute alpha_sk
        // Fast convert B -> {m_sk}; input is in Bsk but we only use B
        baseBToMskConv.fastConvertArray(inputInBaseB, tempIter);
        // temp will be changed?

        // Take the m_sk part of input, subtract from temp, and multiply by inv_prod_B_mod_m_sk_
        // Note: input_sk is allocated in input[base_B_size]
        long[] alphaSk = new long[coeffCount];
        for (int i = 0; i < coeffCount; i++) {
            // It is not necessary for the negation to be reduced modulo the small prime
            alphaSk[i] = UintArithmeticSmallMod.multiplyUintMod(
                    tempIter.getCoeff(0, i) + (mSk.getValue() - input.getCoeff(baseBSize, i)),
                    invProdBModMsk,
                    mSk
            );
        }

        // alpha_sk is now ready for the Shenoy-Kumaresan conversion; however, note that our
        // alpha_sk here is not a centered reduction, so we need to apply a correction below.

        long mSkDiv2 = mSk.getValue() >>> 1;
        for (int i = 0; i < baseQSize; i++) {

            MultiplyUintModOperand prodBModQElt = new MultiplyUintModOperand();
            prodBModQElt.set(prodBModQ[i], baseQ.getBase(i));

            MultiplyUintModOperand negProdBModQElt = new MultiplyUintModOperand();
            negProdBModQElt.set(baseQ.getBase(i).getValue() - prodBModQ[i], baseQ.getBase(i));

            for (int j = 0; j < coeffCount; j++) {
                // Correcting alpha_sk since it represents a negative value
                if (alphaSk[j] > mSkDiv2) {
                    destination.setCoeff(
                            i,
                            j,
                            UintArithmeticSmallMod.multiplyAddUintMod(
                                    UintArithmeticSmallMod.negateUintMod(alphaSk[j], mSk),
                                    prodBModQElt,
                                    destination.getCoeff(i, j),
                                    baseQ.getBase(i)
                            )
                    );
                } else {
                    destination.setCoeff(
                            i,
                            j,
                            UintArithmeticSmallMod.multiplyAddUintMod(
                                    alphaSk[j],
                                    negProdBModQElt,
                                    destination.getCoeff(i, j),
                                    baseQ.getBase(i)
                            )
                    );
                }

            }
        }
    }


    /**
     * 输入是一个 polyIter, 长度 size * k * N, 函数处理的是一个 RnsIter, 用 startIndex 表示处理的是哪一个 RnsIter
     *
     * @param polyIter
     * @param polyIterCoeffCount
     * @param polyCoeffModulusSize
     * @param startIndex
     */
    public void divideAndRoundQLastInplace(long[] polyIter, int polyIterCoeffCount, int polyCoeffModulusSize, int startIndex) {

        assert polyIterCoeffCount == coeffCount;
        assert polyCoeffModulusSize == baseQ.getSize();

        int baseQSize = baseQ.getSize();
        // 注意起点的计算
        int lastInputIndex = startIndex + (baseQSize - 1) * coeffCount;

        // Add (qi-1)/2 to change from flooring to rounding
        Modulus lastModulus = baseQ.getBase(baseQSize - 1);
        long half = lastModulus.getValue() >>> 1;
        // 注意这里的函数签名，利用 startIndex 避免 new array
        // 这里本质上是在处理 lastInput
        PolyArithmeticSmallMod.addPolyScalarCoeffMod(
                polyIter,
                lastInputIndex,
                coeffCount,
                half,
                lastModulus,
                lastInputIndex,
                polyIter);

        for (int i = 0; i < baseQSize - 1; i++) {
            long[] temp = new long[coeffCount];

            // (ct mod qk) mod qi
            PolyArithmeticSmallMod.moduloPolyCoeffs(polyIter, lastInputIndex, coeffCount, baseQ.getBase(i), 0, temp);

            // Subtract rounding correction here; the negative sign will turn into a plus in the next subtraction
            long halfMod = UintArithmeticSmallMod.barrettReduce64(half, baseQ.getBase(i));
            PolyArithmeticSmallMod.subPolyScalarCoeffMod(temp, coeffCount, halfMod, baseQ.getBase(i), temp);

            // (ct mod qi) - (ct mod qk) mod qi
            // [i * N, (i+1) * N) is current ct
            // 注意起点的计算
            PolyArithmeticSmallMod.subPolyCoeffMod(polyIter, startIndex + i * coeffCount, temp, 0, coeffCount, baseQ.getBase(i), startIndex + i * coeffCount, polyIter);

            // qk^(-1) * ((ct mod qi) - (ct mod qk)) mod qi
            // [i * N, (i+1) * N) is current ct
            // 注意起点的计算
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(polyIter, startIndex + i * coeffCount, coeffCount, invQLastModQ[i], baseQ.getBase(i), startIndex + i * coeffCount, polyIter);
        }
    }


    public void divideAndRoundQLastInplace(RnsIter input) {

        assert input.getPolyModulusDegree() == coeffCount;
        assert input.getRnsBaseSize() == baseQ.getSize();

        int baseQSize = baseQ.getSize();
//        long[] lastInput = input.getCoeffIter(baseQSize - 1);
        int lastInputIndex = (baseQSize - 1) * coeffCount;

        // Add (qi-1)/2 to change from flooring to rounding
        Modulus lastModulus = baseQ.getBase(baseQSize - 1);
        long half = lastModulus.getValue() >>> 1;
        // 注意这里的函数签名，利用 startIndex 避免 new array
        // 这里本质上是在处理 lastInput
        PolyArithmeticSmallMod.addPolyScalarCoeffMod(input.coeffIter, lastInputIndex, coeffCount, half, lastModulus, lastInputIndex, input.coeffIter);

        for (int i = 0; i < baseQSize - 1; i++) {
            long[] temp = new long[coeffCount];

            // (ct mod qk) mod qi
            PolyArithmeticSmallMod.moduloPolyCoeffs(input.coeffIter, lastInputIndex, coeffCount, baseQ.getBase(i), 0, temp);

            // Subtract rounding correction here; the negative sign will turn into a plus in the next subtraction
            long halfMod = UintArithmeticSmallMod.barrettReduce64(half, baseQ.getBase(i));
            PolyArithmeticSmallMod.subPolyScalarCoeffMod(temp, coeffCount, halfMod, baseQ.getBase(i), temp);

            // (ct mod qi) - (ct mod qk) mod qi
            // [i * N, (i+1) * N) is current ct
            PolyArithmeticSmallMod.subPolyCoeffMod(input.coeffIter, i * coeffCount, temp, 0, coeffCount, baseQ.getBase(i), i * coeffCount, input.coeffIter);

            // qk^(-1) * ((ct mod qi) - (ct mod qk)) mod qi
            // [i * N, (i+1) * N) is current ct
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(input.coeffIter, i * coeffCount, coeffCount, invQLastModQ[i], baseQ.getBase(i), i * coeffCount, input.coeffIter);
        }

    }

    /**
     * 输入是一个 polyIyer: size * k * N , 这个 function 处理其中的一个 RnsIter, startIndex 指定哪一个RnsIter
     *
     * @param polyIter
     * @param startIndex
     * @param rnsNttTables
     */
    public void divideAndRoundQLastNttInplace(long[] polyIter, int polyIterCoeffCount, int polyIterCoeffModulusSize, int startIndex, NttTables[] rnsNttTables) {

        assert polyIter != null;
        assert polyIterCoeffCount == coeffCount;
        // todo: need this check and the in-parameter?
        assert polyIterCoeffModulusSize == rnsNttTables.length;
        assert rnsNttTables != null;

        int baseQSize = baseQ.getSize();
        // 注意起点
        int lastInputIndex = startIndex + (baseQSize - 1) * coeffCount;

        // convert to non-NTT form
        NttTool.inverseNttNegAcyclicHarvey(
                polyIter,
                lastInputIndex,
                rnsNttTables[baseQSize - 1]
        );

        // Add (qi-1)/2 to change from flooring to rounding
        Modulus lastModulus = baseQ.getBase(baseQSize - 1);
        long half = lastModulus.getValue() >>> 1;
        // 注意这里的函数签名，利用 startIndex 避免 new array
        // 这里本质上是在处理 lastInput
        PolyArithmeticSmallMod.addPolyScalarCoeffMod(polyIter, lastInputIndex, coeffCount, half, lastModulus, lastInputIndex, polyIter);

        for (int i = 0; i < baseQSize - 1; i++) {
            long[] temp = new long[coeffCount];

            if (baseQ.getBase(i).getValue() < lastModulus.getValue()) {
                PolyArithmeticSmallMod.moduloPolyCoeffs(
                        polyIter,
                        lastInputIndex,
                        coeffCount,
                        baseQ.getBase(i),
                        0,
                        temp
                );
            } else {
                System.arraycopy(polyIter, lastInputIndex, temp, 0, coeffCount);
            }

            // Lazy subtraction here. ntt_negacyclic_harvey_lazy can take 0 < x < 4*qi input.
            long negHalfMod = baseQ.getBase(i).getValue() - UintArithmeticSmallMod.barrettReduce64(half, baseQ.getBase(i));
            for (int j = 0; j < coeffCount; j++) {
                temp[j] += negHalfMod;
            }
            long qiLazy;
            if (Constants.USER_MOD_BIT_COUNT_MAX <= 60) {
                // Since SEAL uses at most 60-bit moduli, 8*qi < 2^63.
                // This ntt_negacyclic_harvey_lazy results in [0, 4*qi).

                qiLazy = baseQ.getBase(i).getValue() << 2;
                // temp 就是 单个 CoeffIter， 直接调用
                NttTool.nttNegAcyclicHarveyLazy(temp, rnsNttTables[i]);
            } else {
                // 2^60 < pi < 2^62, then 4*pi < 2^64, we perfrom one reduction
                // from [0, 4*qi) to [0, 2*qi) after ntt.
                qiLazy = baseQ.getBase(i).getValue() << 1;
                // temp 就是 单个 CoeffIter， 直接调用
                NttTool.nttNegAcyclicHarveyLazy(temp, rnsNttTables[i]);

                // 对 temp 做第一次 reduce
                for (int j = 0; j < coeffCount; j++) {
                    // -1 = 0xFFFFFFFF..FF，等于 -1 时 temp[j] -= qiLazy
                    // = 0 时, temp[j] 不变
                    // 其实就是 temp[j] -= (temp[j] >= qiLazy ? qiLazy: 0)
                    temp[j] -= (qiLazy & (temp[j] >= qiLazy ? -1 : 0));
                }
            }
            // Lazy subtraction again, results in [0, 2*qi_lazy),
            // The reduction [0, 2*qi_lazy) -> [0, qi) is done implicitly in multiply_poly_scalar_coeffmod.
            for (int j = 0; j < coeffCount; j++) {
                polyIter[startIndex + i * coeffCount + j] += (qiLazy - temp[j]);
            }

            // qk^(-1) * ((ct mod qi) - (ct mod qk)) mod qi
            // 注意这里的函数签名
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(
                    polyIter,
                    startIndex + i * coeffCount,
                    coeffCount,
                    invQLastModQ[i],
                    baseQ.getBase(i),
                    startIndex + i * coeffCount,
                    polyIter
            );
        }
    }

    public void divideAndRoundQLastNttInplace(RnsIter input, NttTables[] rnsNttTables) {

        assert input != null;
        assert input.getPolyModulusDegree() == coeffCount;
        assert rnsNttTables != null;

        int baseQSize = baseQ.getSize();
        int lastInputIndex = (baseQSize - 1) * coeffCount;

        // convert to non-NTT form
        NttTool.inverseNttNegAcyclicHarvey(
                input.coeffIter,
                lastInputIndex,
                rnsNttTables[baseQSize - 1]
        );

        // Add (qi-1)/2 to change from flooring to rounding
        Modulus lastModulus = baseQ.getBase(baseQSize - 1);
        long half = lastModulus.getValue() >>> 1;
        // 注意这里的函数签名，利用 startIndex 避免 new array
        // 这里本质上是在处理 lastInput
        PolyArithmeticSmallMod.addPolyScalarCoeffMod(input.coeffIter, lastInputIndex, coeffCount, half, lastModulus, lastInputIndex, input.coeffIter);

        for (int i = 0; i < baseQSize - 1; i++) {
            long[] temp = new long[coeffCount];

            if (baseQ.getBase(i).getValue() < lastModulus.getValue()) {
                PolyArithmeticSmallMod.moduloPolyCoeffs(
                        input.coeffIter,
                        lastInputIndex,
                        coeffCount,
                        baseQ.getBase(i),
                        0,
                        temp
                );
            } else {
                System.arraycopy(input.coeffIter, lastInputIndex, temp, 0, coeffCount);
            }

            // Lazy subtraction here. ntt_negacyclic_harvey_lazy can take 0 < x < 4*qi input.
            long negHalfMod = baseQ.getBase(i).getValue() - UintArithmeticSmallMod.barrettReduce64(half, baseQ.getBase(i));
            for (int j = 0; j < coeffCount; j++) {
                temp[j] += negHalfMod;
            }
            long qiLazy;
            if (Constants.USER_MOD_BIT_COUNT_MAX <= 60) {
                // Since SEAL uses at most 60-bit moduli, 8*qi < 2^63.
                // This ntt_negacyclic_harvey_lazy results in [0, 4*qi).

                qiLazy = baseQ.getBase(i).getValue() << 2;
                // temp 就是 单个 CoeffIter， 直接调用
                NttTool.nttNegAcyclicHarveyLazy(temp, rnsNttTables[i]);
            } else {
                // 2^60 < pi < 2^62, then 4*pi < 2^64, we perfrom one reduction
                // from [0, 4*qi) to [0, 2*qi) after ntt.
                qiLazy = baseQ.getBase(i).getValue() << 1;
                // temp 就是 单个 CoeffIter， 直接调用
                NttTool.nttNegAcyclicHarveyLazy(temp, rnsNttTables[i]);

                // 对 temp 做第一次 reduce
                for (int j = 0; j < coeffCount; j++) {
                    // -1 = 0xFFFFFFFF..FF，等于 -1 时 temp[j] -= qiLazy
                    // = 0 时, temp[j] 不变
                    // 其实就是 temp[j] -= (temp[j] >= qiLazy ? qiLazy: 0)
                    temp[j] -= (qiLazy & (temp[j] >= qiLazy ? -1 : 0));
                }
            }
            // Lazy subtraction again, results in [0, 2*qi_lazy),
            // The reduction [0, 2*qi_lazy) -> [0, qi) is done implicitly in multiply_poly_scalar_coeffmod.
            for (int j = 0; j < coeffCount; j++) {
                input.coeffIter[i * coeffCount + j] += (qiLazy - temp[j]);
            }

            // qk^(-1) * ((ct mod qi) - (ct mod qk)) mod qi
            // 注意这里的函数签名
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(
                    input.coeffIter,
                    i * coeffCount,
                    coeffCount,
                    invQLastModQ[i],
                    baseQ.getBase(i),
                    i * coeffCount,
                    input.coeffIter
            );
        }
    }


    public void decryptModT(RnsIter phase, long[] destination) {

        // Use exact base convension rather than convert the base through the compose API
        baseQToTConv.exactConvertArray(phase, destination);
    }

    public long getInvQLastModT() {
        return invQLastModT;
    }

    public BaseConverter getBaseBToMskConv() {
        return baseBToMskConv;
    }

    public Modulus getMTilde() {
        return mTilde;
    }

    public RnsBase getBaseB() {
        return baseB;
    }

    public Modulus getGamma() {
        return gamma;
    }

    public Modulus getMSk() {
        return mSk;
    }

    public Modulus getT() {
        return t;
    }

    public RnsBase getBaseBskMTilde() {
        return baseBskMTilde;
    }

    public RnsBase getBaseQ() {
        return baseQ;
    }

    public RnsBase getBaseBsk() {
        return baseBsk;
    }

    public MultiplyUintModOperand[] getInvQLastModQ() {
        return invQLastModQ;
    }

    public NttTables[] getBaseBskNttTables() {
        return baseBskNttTables;
    }
}
