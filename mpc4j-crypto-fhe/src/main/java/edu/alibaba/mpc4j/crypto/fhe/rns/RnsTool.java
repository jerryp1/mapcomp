package edu.alibaba.mpc4j.crypto.fhe.rns;

import edu.alibaba.mpc4j.crypto.fhe.iterator.RnsIter;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.ntt.NttTables;
import edu.alibaba.mpc4j.crypto.fhe.rq.PolyArithmeticSmallMod;
import edu.alibaba.mpc4j.crypto.fhe.utils.Constants;
import edu.alibaba.mpc4j.crypto.fhe.zq.*;

import java.util.stream.IntStream;

/**
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
        int finalBaseBSize = baseBSize;
        IntStream.range(0, baseQSize).parallel().forEach(
                i -> {
                    prodBModQ[i] = UintArithmeticSmallMod.moduloUint(baseB.getBaseProd(), finalBaseBSize, baseQ.getBase(i));
                }
        );

        // Compute prod(q)^(-1) mod Bsk, which has many modulus
        invProdQModBsk = IntStream.range(0, baseBskSize).parallel()
                .mapToObj(i -> new MultiplyUintModOperand())
                .toArray(MultiplyUintModOperand[]::new);
        IntStream.range(0, baseBskSize).parallel().forEach(
                // first reduce prod(q) to Bsk[i]
                i -> {
                    long innerTemp = UintArithmeticSmallMod.moduloUint(baseQ.getBaseProd(), baseQSize, baseBsk.getBase(i));
                    // then compute inverse
                    long[] innerInvTemp = new long[1];
                    if (!UintArithmeticSmallMod.tryInvertUintMod(innerTemp, baseBsk.getBase(i), innerInvTemp)) {
                        throw new IllegalArgumentException("invalid rns bases");
                    }
                    // initialize a MultiplyUintModOperand
                    invProdQModBsk[i].set(innerInvTemp[0], baseBsk.getBase(i));
                }
        );
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
        invMTildeModBsk = IntStream.range(0, baseBskSize).
                parallel().
                mapToObj(i -> new MultiplyUintModOperand()).
                toArray(MultiplyUintModOperand[]::new);

        IntStream.range(0, baseBskSize).parallel().forEach(
                i -> {
                    long[] innerInvTemp = new long[1];
                    if (!UintArithmeticSmallMod.tryInvertUintMod(
                            UintArithmeticSmallMod.barrettReduce64(mTilde.getValue(), baseBsk.getBase(i)),
                            baseBsk.getBase(i),
                            innerInvTemp)) {
                        throw new IllegalArgumentException("invalid rns bases");
                    }
                    invMTildeModBsk[i].set(innerInvTemp[0], baseBsk.getBase(i));
                }
        );

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
        IntStream.range(0, baseBskSize).parallel().forEach(
                i -> {
                    prodQModBsk[i] = UintArithmeticSmallMod.moduloUint(baseQ.getBaseProd(), baseQSize, baseBsk.getBase(i));
                }
        );

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
            prodTGammaModQ = IntStream.range(0, baseQSize).parallel()
                    .mapToObj(i -> new MultiplyUintModOperand())
                    .toArray(MultiplyUintModOperand[]::new);
            IntStream.range(0, baseQSize).parallel().forEach(
                    i -> {
                        prodTGammaModQ[i].set(
                                // t * \gamma q_i
                                // todo: why don't use baseTGamma.getBaseProd() mod baseQ.getBase(i), which can avoid repeat multiplication?
                                UintArithmeticSmallMod.multiplyUintMod(baseTGamma.getBase(0).getValue(), baseTGamma.getBase(1).getValue(), baseQ.getBase(i)),
                                baseQ.getBase(i)
                        );
                    }
            );
            // Compute -prod(q)^(-1) mod {t, gamma}
            negInvQModTGamma = IntStream.range(0, baseTGammaSize)
                    .parallel()
                    .mapToObj(i -> new MultiplyUintModOperand())
                    .toArray(MultiplyUintModOperand[]::new);

//            note that we cannot use Arrays.fill to initialize a object array, this way will lead to the every element in this array is a same object
//            negInvQModTGamma = new MultiplyUintModOperand[baseTGammaSize];
//            Arrays.fill(negInvQModTGamma, new MultiplyUintModOperand());

            IntStream.range(0, baseTGammaSize).parallel().forEach(
                    i -> {
                        // q mod t, q mod gamma
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
            );

        }
        // Compute q[last]^(-1) mod q[i] for i = 0..last-1, i.e. q_k mod q_1, q_k mod q_2, ..., q_k mod q_{k-1}
        // This is used by modulus switching and rescaling
        invQLastModQ = IntStream.range(0, baseQSize - 1)
                .mapToObj(i -> new MultiplyUintModOperand())
                .toArray(MultiplyUintModOperand[]::new);


        long qLast = baseQ.getBase(baseQSize - 1).getValue();
        IntStream.range(0, baseQSize - 1).parallel().forEach(
                i -> {
                    long[] curInvTemp = new long[1];
                    if (!UintArithmeticSmallMod.tryInvertUintMod(qLast, baseQ.getBase(i), curInvTemp)) {
                        throw new IllegalArgumentException("invalid rns bases");
                    }
                    invQLastModQ[i].set(curInvTemp[0], baseQ.getBase(i));
                }
        );
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
        IntStream.range(0, baseQSize).parallel().forEach(
                i -> {
                    PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(input.getCoeffIter(i), coeffCount, prodTGammaModQ[i], baseQ.getBase(i), temp.getCoeffIter(i));
                }
        );
        // Make another temp destination to get the poly in mod {t, gamma}
        RnsIter tempTGamma = new RnsIter(baseTGammaSize, coeffCount);
        // Convert from q to {t, gamma}
        baseQToTGammaConv.fastConvertArray(temp, tempTGamma);

        // Multiply by -prod(q)^(-1) mod {t, gamma}
        // line-2
        IntStream.range(0, baseTGammaSize).parallel().forEach(
                i -> {
                    PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(tempTGamma.getCoeffIter(i), coeffCount, negInvQModTGamma[i], baseTGamma.getBase(i), tempTGamma.getCoeffIter(i));
                }
        );
        // Need to correct values in temp_t_gamma (gamma component only) which are
        // larger than floor(gamma/2)
        // for  ||_p --> []_p , i.e. [0, q) ---> [-q/2, q/2)
        long gammaDiv2 = baseTGamma.getBase(1).getValue() >>> 1;

        // Now compute the subtraction to remove error and perform final multiplication by
        // gamma inverse mod t. just : s^{(t)}, s^{(\gamma)}, line-4/5
        IntStream.range(0, coeffCount).parallel().forEach(
                i -> {
                    // tempTGamma.getCoeffIter(1) is the gamma, [i] is the i-th count value
                    if (tempTGamma.getCoeffIter(1)[i] > gammaDiv2) {

                        // Compute -(gamma - a) instead of (a - gamma)
                        destination[i] = UintArithmeticSmallMod.addUintMod(
                                tempTGamma.getCoeffIter(0)[i],
                                UintArithmeticSmallMod.barrettReduce64(gamma.getValue() - tempTGamma.getCoeffIter(1)[i], t),
                                t);
                    } else {
                        // No correction needed, just no need gamma - a, directly use a, beacuse a \in [0, gamma/2)
                        destination[i] = UintArithmeticSmallMod.addUintMod(
                                tempTGamma.getCoeffIter(0)[i],
                                UintArithmeticSmallMod.barrettReduce64(tempTGamma.getCoeffIter(1)[i], t),
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
        );
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
        long[][] tempOut = new long[baseBskSize][coeffCount];
        baseQToBskConv.fastConvertArray(temp, tempOut);

        // Finally convert to {m_tilde}
        // mTilde is a single modulus, so baseSize is 1.
        long[][] tempOut2 = new long[1][coeffCount];
        baseQToMTildeConv.fastConvertArray(temp, tempOut2);

        // update destination, Now input is in Bsk U {\tilde m}
        destination.update(tempOut, tempOut2);
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
        long[] inputMTilde = input.getCoeffIter(baseBskSize);
        long mTildeDiv2 = mTilde.getValue() >>> 1;

        // line-1: r_{\tilde m} = [-c^{''}_{\tilde m} / q]_{\tilde m}
        long[] rMTilde = new long[coeffCount];
        PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(inputMTilde, coeffCount, negInvProdQModMTilde, mTilde, rMTilde);

        // line 2-4
        IntStream.range(0, baseBskSize).parallel().forEach(
                i -> {
                    MultiplyUintModOperand prodQModBskElt = new MultiplyUintModOperand();
                    prodQModBskElt.set(prodQModBsk[i], baseBsk.getBase(i));

                    IntStream.range(0, coeffCount).parallel().forEach(
                            j -> {
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
                    );
                }
        );
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

        IntStream.range(0, baseBsk.getSize()).parallel().forEach(
                i -> {
                    IntStream.range(0, coeffCount).parallel().forEach(
                            j -> {
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
                    );
                }
        );
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
        IntStream.range(0, coeffCount).parallel().forEach(
                // It is not necessary for the negation to be reduced modulo the small prime
                i -> alphaSk[i] = UintArithmeticSmallMod.multiplyUintMod(
                        tempIter.getCoeff(0, i) + (mSk.getValue() - input.getCoeffIter(baseBSize)[i]),
                        invProdBModMsk,
                        mSk
                )
        );
        // alpha_sk is now ready for the Shenoy-Kumaresan conversion; however, note that our
        // alpha_sk here is not a centered reduction, so we need to apply a correction below.

        long mSkDiv2 = mSk.getValue() >>> 1;
        IntStream.range(0, baseQSize).parallel().forEach(
                i -> {
                    MultiplyUintModOperand prodBModQElt = new MultiplyUintModOperand();
                    prodBModQElt.set(prodBModQ[i], baseQ.getBase(i));

                    MultiplyUintModOperand negProdBModQElt = new MultiplyUintModOperand();
                    negProdBModQElt.set(baseQ.getBase(i).getValue() - prodBModQ[i], baseQ.getBase(i));

                    IntStream.range(0, coeffCount).parallel().forEach(
                            j -> {
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
                            });
                });
    }


    public void divideAndRoundQLastInplace(RnsIter input) {

        assert input.getPolyModulusDegree() == coeffCount;
        assert input.getRnsBaseSize() == baseQ.getSize();

        int baseQSize = baseQ.getSize();
        long[] lastInput = input.getCoeffIter(baseQSize - 1);

        // Add (qi-1)/2 to change from flooring to rounding
        Modulus lastModulus = baseQ.getBase(baseQSize - 1);
        long half = lastModulus.getValue() >>> 1;
        PolyArithmeticSmallMod.addPolyScalarCoeffMod(lastInput, coeffCount, half, lastModulus, lastInput);


        IntStream.range(0, baseQSize - 1).parallel().forEach(
                i -> {
                    long[] temp = new long[coeffCount];

                    // (ct mod qk) mod qi
                    PolyArithmeticSmallMod.moduloPolyCoeffs(lastInput, coeffCount, baseQ.getBase(i), temp);

                    // Subtract rounding correction here; the negative sign will turn into a plus in the next subtraction
                    long halfMod = UintArithmeticSmallMod.barrettReduce64(half, baseQ.getBase(i));
                    PolyArithmeticSmallMod.subPolyScalarCoeffMod(temp, coeffCount, halfMod, baseQ.getBase(i), temp);

                    // (ct mod qi) - (ct mod qk) mod qi
                    PolyArithmeticSmallMod.subPolyCoeffMod(input.getCoeffIter(i), temp, coeffCount, baseQ.getBase(i), input.getCoeffIter(i));

                    // qk^(-1) * ((ct mod qi) - (ct mod qk)) mod qi
                    PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(input.getCoeffIter(i), coeffCount, invQLastModQ[i], baseQ.getBase(i), input.getCoeffIter(i));
                }
        );

    }


    public void decrtptModT(RnsIter phase, long[] destination) {

        // Use exact base convension rather than convert the base through the compose API
        baseQToTConv.exactConvertArray(phase, destination);
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
}
