package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.context.Context;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.ntt.NttTables;
import edu.alibaba.mpc4j.crypto.fhe.ntt.NttTool;
import edu.alibaba.mpc4j.crypto.fhe.params.EncryptionParams;
import edu.alibaba.mpc4j.crypto.fhe.params.ParmsIdType;
import edu.alibaba.mpc4j.crypto.fhe.params.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.rq.PolyArithmeticSmallMod;
import edu.alibaba.mpc4j.crypto.fhe.rq.PolyCore;
import edu.alibaba.mpc4j.crypto.fhe.utils.ScalingVariant;
import edu.alibaba.mpc4j.crypto.fhe.utils.ValueChecker;
import edu.alibaba.mpc4j.crypto.fhe.zq.*;
import org.checkerframework.checker.units.qual.C;

/**
 * @author Qixian Zhou
 * @date 2023/10/5
 */
public class Evaluator {


    private Context context;


    public Evaluator(Context context) {
        if (!context.isParametersSet()) {
            throw new IllegalArgumentException("encryption parameters are not set correctly");
        }
        this.context = context;
    }


    public void transformToNtt(Plaintext plain, ParmsIdType parmsId, Plaintext destinationNTT) {

        destinationNTT.copyFrom(plain);
        transformToNttInplace(destinationNTT, parmsId);
    }

    public void transformToNtt(Ciphertext encrypted, Ciphertext destinationNTT) {
        destinationNTT.copyFrom(encrypted);
        transformToNttInplace(destinationNTT);
    }

    public void transformFromNtt(Ciphertext encryptedNtt, Ciphertext destination) {
        destination.copyFrom(encryptedNtt);
        transformFromNttInplace(destination);
    }

    /**
     * 明文转换为 Ntt form, 明文没有对应的再转换回 non-Ntt 的方法
     *
     * @param plain
     * @param parmsId
     */
    public void transformToNttInplace(Plaintext plain, ParmsIdType parmsId) {

        if (!ValueChecker.isValidFor(plain, context)) {
            throw new IllegalArgumentException("plain is not valid for encryption parameters");
        }

        Context.ContextData contextData = context.getContextData(parmsId);
        if (contextData == null) {
            throw new IllegalArgumentException("parms_id is not valid for the current context");
        }
        if (plain.isNttForm()) {
            throw new IllegalArgumentException("plain is already in NTT form");
        }
        EncryptionParams parms = contextData.getParms();
        Modulus[] coeffModulus = parms.getCoeffModulus();
        int coeffCount = parms.getPolyModulusDegree();
        int coeffModulusSize = coeffModulus.length;
        // 注意 这个和参数的 N 是不同的
        int plainCoeffCount = plain.getCoeffCount();

        long plainUpperHalfThreshold = contextData.getPlainUpperHalfThreshold();
        long[] plainUpperHalfIncrement = contextData.getPlainUpperHalfIncrement();

        NttTables[] nttTables = contextData.getSmallNttTables();

        if (!Common.productFitsIn(false, coeffCount, coeffModulusSize)) {
            throw new RuntimeException("invalid parameters");
        }
        // Resize to fit the entire NTT transformed (ciphertext size) polynomial
        // Note that the new coefficients are automatically set to 0
        // 现在相当于 明文也是一个 RnsIter了
        plain.resize(coeffCount * coeffModulusSize);

        if (!contextData.getQualifiers().isUsingFastPlainLift()) {

            long[] temp = new long[coeffCount * coeffModulusSize];
            // Allocate temporary space for an entire RNS polynomial
            // Slight semantic misuse of RNSIter here, but this works well
            // 这里的 迭代器拆解逻辑和 multiplyPlainNormal 完全一样，可以对照理解
            for (int i = 0; i < plainCoeffCount; i++) {
                long plainValue = plain.at(i);
                if (plainValue >= plainUpperHalfThreshold) {
                    long[] addTemp = new long[coeffModulusSize];
                    UintArithmetic.addUint(
                            plainUpperHalfIncrement,
                            coeffModulusSize,
                            plainValue,
                            addTemp
                    );
                    // 拷贝回 temp, 注意 temp 的起点
                    System.arraycopy(addTemp, 0, temp, i * coeffModulusSize, coeffModulusSize);
                } else {
                    temp[i * coeffModulusSize] = plainValue;
                }
            }
            //todo: 正确性待验证
            contextData.getRnsTool().getBaseQ().decomposeArray(temp, coeffCount);

            // Copy data back to plain
            System.arraycopy(
                    temp,
                    0,
                    plain.getData(),
                    0,
                    coeffCount * coeffModulusSize
            );

        } else {

            // Note that in this case plain_upper_half_increment holds its value in RNS form modulo the coeff_modulus
            // primes.

            // Create a "reversed" helper iterator that iterates in the reverse order both plain RNS components and
            // the plain_upper_half_increment values.


            // 这里需要逆序的处理 plain(RnsIter) 和 plain_upper_half_increment
            // 也就是依次处理 [(k-1) * N, k * N)  plain_upper_half_increment[k-1]
            //              [(k-2) * N, (k-1) * N) plain_upper_half_increment[k-2]
            // 注意这里的 i , 为了等价实现SEAL 中的逆序迭代
            for (int i = coeffModulusSize - 1; i >= 0; i--) {
                int startIndex = i * coeffCount;
                // 遍历一个区间内的 CoeffIter
                for (int j = 0; j < plainCoeffCount; j++) {
                    // todo: 高度注意这里的索引，容易出错
                    // plain.getData()[j] 是想等价 *plain_iter 的遍历
                    plain.getData()[startIndex + j] =
                            plain.getData()[j] >= plainUpperHalfThreshold
                                    ? plain.getData()[j] + plainUpperHalfIncrement[i]
                                    : plain.getData()[j];

                }
            }
        }

        // 处理 RnsIter
        NttTool.nttNegAcyclicHarveyRnsIter(
                plain.getData(),
                coeffCount, // 注意这个 coeffCount 是 密文的 N, 也是目前 plain 作为 RnsIter 的 N, 注意到这个值一定不能是 plain.getCoeffCount 完全两码事
                coeffModulusSize,
                nttTables
        );
        plain.setParmsId(parmsId);
    }


    /**
     * non-NTT ---> NTT
     *
     * @param encrypted
     */
    public void transformToNttInplace(Ciphertext encrypted) {
        if (!ValueChecker.isMetaDataValidFor(encrypted, context)
                || !ValueChecker.isBufferValid(encrypted)
        ) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }

        Context.ContextData contextData = context.getContextData(encrypted.getParmsId());
        if (contextData == null) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }

        if (encrypted.isNttForm()) {
            throw new IllegalArgumentException("encrypted is already in NTT form");
        }
        EncryptionParams parms = contextData.getParms();
        Modulus[] coeffModulus = parms.getCoeffModulus();
        int coeffCount = parms.getPolyModulusDegree();
        int coeffModulusSize = coeffModulus.length;
        int encryptedSize = encrypted.getSize();

        NttTables[] nttTables = contextData.getSmallNttTables();

        // todo: need check?
        if (!Common.productFitsIn(false, coeffCount, coeffModulusSize)) {
            throw new IllegalArgumentException("invalid parameters");
        }
        // Transform each polynomial to NTT domain
        NttTool.nttNegAcyclicHarveyPolyIter(
                encrypted.getData(),
                coeffCount,
                coeffModulusSize,
                encryptedSize,
                nttTables
        );
        // Finally change the is_ntt_transformed flag
        encrypted.setIsNttForm(true);

        if (encrypted.isTransparent()) {
            throw new RuntimeException("result ciphertext is transparent");
        }
    }

    /**
     * NTT ---> non NTT
     *
     * @param encryptedNtt
     */
    public void transformFromNttInplace(Ciphertext encryptedNtt) {
        if (!ValueChecker.isMetaDataValidFor(encryptedNtt, context)
                || !ValueChecker.isBufferValid(encryptedNtt)
        ) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }

        Context.ContextData contextData = context.getContextData(encryptedNtt.getParmsId());
        if (contextData == null) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }

        if (!encryptedNtt.isNttForm()) {
            throw new IllegalArgumentException("encrypted is not in NTT form");
        }
        EncryptionParams parms = contextData.getParms();
        Modulus[] coeffModulus = parms.getCoeffModulus();
        int coeffCount = parms.getPolyModulusDegree();
        int coeffModulusSize = coeffModulus.length;
        int encryptedSize = encryptedNtt.getSize();

        NttTables[] nttTables = contextData.getSmallNttTables();

        // todo: need check?
        if (!Common.productFitsIn(false, coeffCount, coeffModulusSize)) {
            throw new IllegalArgumentException("invalid parameters");
        }
        // Transform each polynomial to NTT domain
        NttTool.inverseNttNegAcyclicHarveyPolyIter(
                encryptedNtt.getData(),
                coeffCount,
                coeffModulusSize,
                encryptedSize,
                nttTables
        );
        // Finally change the is_ntt_transformed flag
        encryptedNtt.setIsNttForm(false);

        if (encryptedNtt.isTransparent()) {
            throw new RuntimeException("result ciphertext is transparent");
        }
    }


    public void multiplyPlain(Ciphertext encrypted, Plaintext plain, Ciphertext destination) {

        destination.copyFrom(encrypted);
        multiplyPlainInplace(destination, plain);
    }

    public void multiplyPlainInplace(Ciphertext encrypted, Plaintext plain) {

        if (!ValueChecker.isMetaDataValidFor(encrypted, context)
                || !ValueChecker.isBufferValid(encrypted)) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }

        if (!ValueChecker.isMetaDataValidFor(plain, context)
                || !ValueChecker.isBufferValid(plain)) {
            throw new IllegalArgumentException("plain is not valid for encryption parameters");
        }

        if (plain.isNttForm() != encrypted.isNttForm()) {
            throw new IllegalArgumentException("NTT form mismatch");
        }
        // 不同的 密文*明文 实现
        if (encrypted.isNttForm()) {
            multiplyPlainNtt(encrypted, plain);
        } else {
            multiplyPlainNormal(encrypted, plain);
        }

        if (encrypted.isTransparent()) {
            throw new RuntimeException("result ciphertext is transparent");
        }
    }

    private void multiplyPlainNormal(Ciphertext encrypted, Plaintext plain) {

        Context.ContextData contextData = context.getContextData(encrypted.getParmsId());
        EncryptionParams parms = contextData.getParms();
        Modulus[] coeffModulus = parms.getCoeffModulus();
        int coeffCount = parms.getPolyModulusDegree();
        int coeffModulusSize = coeffModulus.length;

        long plainUpperHalfThreshold = contextData.getPlainUpperHalfThreshold();
        long[] plainUpperHalfIncrement = contextData.getPlainUpperHalfIncrement();
        NttTables[] nttTables = contextData.getSmallNttTables();

        int encryptedSize = encrypted.getSize();
        // 一定要注意 plainCoeffCount 和 coeffCount 是不同的东西
        int plainCoeffCount = plain.getCoeffCount();
        // 把明文视为长度为N的数组，这个就是统计数组中 非0元素的数量
        int plainNonZeroCoeffCount = plain.nonZeroCoeffCount();
        // todo: really need check?
        if (!Common.productFitsIn(false, encryptedSize, coeffCount, coeffModulusSize)) {
            throw new IllegalArgumentException("invalid parameter");
        }

        /*
        Optimizations for constant / monomial multiplication can lead to the presence of a timing side-channel in
        use-cases where the plaintext data should also be kept private.
        */
        // 明文是一个单项式的时候，有优化实现
        if (plainNonZeroCoeffCount == 1) {
            // plain.significantCoeffCount() 计算的是 明文最高位的那个值的位置
            // 例如：plain = [0, 0, 0, 1] = 1x^3  ---> plain.significantCoeffCount() 就是 4,index = 3
            //
            int monoExponent = plain.significantCoeffCount() - 1;
            if (plain.at(monoExponent) >= plainUpperHalfThreshold) {

                if (!contextData.getQualifiers().isUsingFastPlainLift()) {
                    // 注意这个数组长度
                    // Allocate temporary space for a single RNS coefficient
                    long[] temp = new long[coeffModulusSize];

                    // We need to adjust the monomial modulo each coeff_modulus prime separately when the coeff_modulus
                    // primes may be larger than the plain_modulus. We add plain_upper_half_increment (i.e., q-t) to
                    // the monomial to ensure it is smaller than coeff_modulus and then do an RNS multiplication. Note
                    // that in this case plain_upper_half_increment contains a multi-precision integer, so after the
                    // addition we decompose the multi-precision integer into RNS components, and then multiply.
                    // plainUpperHalfIncrement[j] + p = temp[j]
                    UintArithmetic.addUint(plainUpperHalfIncrement, coeffModulusSize, plain.at(monoExponent), temp);
                    // temp 对 每一个 qi 取模
                    contextData.getRnsTool().getBaseQ().decompose(temp);
                    // 注意函数签名中的 第5个参数，现在是一个数组, 下面的两次调用都是单个数
                    PolyArithmeticSmallMod.negAcyclicMultiplyPolyMonoCoeffModPolyIter(
                            encrypted.getData(),
                            encrypted.getPolyModulusDegree(),
                            encrypted.getCoeffModulusSize(),
                            encryptedSize,
                            temp,
                            monoExponent,
                            coeffModulus,
                            encrypted.getData(),
                            encrypted.getPolyModulusDegree(),
                            encrypted.getCoeffModulusSize()
                    );
                } else {
                    // Every coeff_modulus prime is larger than plain_modulus, so there is no need to adjust the
                    // monomial. Instead, just do an RNS multiplication.
                    // todo：理解这里的逻辑，这里和下面处理的不是完全一样吗
                    PolyArithmeticSmallMod.negAcyclicMultiplyPolyMonoCoeffModPolyIter(
                            encrypted.getData(),
                            encrypted.getPolyModulusDegree(),
                            encrypted.getCoeffModulusSize(),
                            encryptedSize,
                            plain.at(monoExponent),
                            monoExponent,
                            coeffModulus,
                            encrypted.getData(),
                            encrypted.getPolyModulusDegree(),
                            encrypted.getCoeffModulusSize()
                    );
                }
            } else {
                // The monomial represents a positive number, so no RNS multiplication is needed.
                // todo: 为什么 positive number 就不需要 RNS 乘法了？
                //
                // 注意这个函数签名
                PolyArithmeticSmallMod.negAcyclicMultiplyPolyMonoCoeffModPolyIter(
                        encrypted.getData(),
                        encrypted.getPolyModulusDegree(),
                        encrypted.getCoeffModulusSize(),
                        encryptedSize,
                        plain.at(monoExponent),
                        monoExponent,
                        coeffModulus,
                        encrypted.getData(),
                        encrypted.getPolyModulusDegree(),
                        encrypted.getCoeffModulusSize()
                );
            }

            if (parms.getScheme() == SchemeType.CKKS) {
                // todo: implement CKKS
                throw new IllegalArgumentException("now cannot support CKKS");
            }
            return;
        }

        // Generic case: any plaintext polynomial
        // Allocate temporary space for an entire RNS polynomial
        long[] temp = new long[coeffCount * coeffModulusSize];
        //
        if (!contextData.getQualifiers().isUsingFastPlainLift()) {

            for (int i = 0; i < plainCoeffCount; i++) {
                long plainValue = plain.at(i);
                if (plainValue >= plainUpperHalfThreshold) {
                    long[] addTemp = new long[coeffModulusSize];
                    UintArithmetic.addUint(
                            plainUpperHalfIncrement,
                            coeffModulusSize,
                            plainValue,
                            addTemp
                    );
                    // 拷贝回 temp, 注意 temp 的起点
                    // todo: temp 起点正确性待验证， 总感觉这样的起点不正确
                    System.arraycopy(addTemp, 0, temp, i * coeffModulusSize, coeffModulusSize);
                } else {
                    // todo: 起点的正确性，需要验证
                    temp[i * coeffModulusSize] = plainValue;
                }
            }
            //todo: 正确性待验证
            contextData.getRnsTool().getBaseQ().decomposeArray(temp, coeffCount);


        } else {
            // Note that in this case plain_upper_half_increment holds its value in RNS form modulo the coeff_modulus
            // primes.
            for (int i = 0; i < coeffModulusSize; i++) {
                for (int j = 0; j < plainCoeffCount; j++) {
                    // 注意每一个数组的索引
                    temp[i * coeffCount + j] = plain.at(j) >= plainUpperHalfThreshold ?
                            plain.at(j) + plainUpperHalfIncrement[i] : plain.at(j);

                }
            }
        }

        // Need to multiply each component in encrypted with temp; first step is to transform to NTT form
        // 把 temp 视为 RnsIter, 整体进行处理
        NttTool.nttNegAcyclicHarveyRnsIter(
                temp,
                coeffCount,
                coeffModulusSize,
                nttTables
        );

        for (int i = 0; i < encryptedSize; i++) {
            int rnsStartIndex = i * coeffCount * coeffModulusSize;

            for (int j = 0; j < coeffModulusSize; j++) {
                // Lazy Reduction, 处理 单个 CoeffIter
                NttTool.nttNegAcyclicHarveyLazy(
                        encrypted.getData(),
                        rnsStartIndex + j * coeffCount,
                        nttTables[j]
                );
                PolyArithmeticSmallMod.dyadicProductCoeffModCoeffIter(
                        encrypted.getData(),
                        rnsStartIndex + j * coeffCount,
                        temp,
                        j * coeffCount,
                        coeffCount,
                        coeffModulus[j],
                        rnsStartIndex + j * coeffCount,
                        encrypted.getData()
                );

                NttTool.inverseNttNegAcyclicHarvey(
                        encrypted.getData(),
                        rnsStartIndex + j * coeffCount,
                        nttTables[j]
                );
            }
        }

        if (parms.getScheme() == SchemeType.CKKS) {
            // todo: implement CKKS
            throw new IllegalArgumentException("now cannot support CKKS");
        }
    }


    private void multiplyPlainNtt(Ciphertext encryptedNtt, Plaintext plainNtt) {

        if (!plainNtt.isNttForm()) {
            throw new IllegalArgumentException("plainNtt is not in NTT form");
        }
        if (!encryptedNtt.getParmsId().equals(plainNtt.getParmsId())) {
            throw new IllegalArgumentException("encryptedNtt and plainNtt parameter mismatch");
        }

        Context.ContextData contextData = context.getContextData(encryptedNtt.getParmsId());
        EncryptionParams parms = contextData.getParms();
        Modulus[] coeffModulus = parms.getCoeffModulus();
        int coeffCount = parms.getPolyModulusDegree();
        int coeffModulusSize = coeffModulus.length;
        int encryptedNttSize = encryptedNtt.getSize();
        //todo: really need this check?
        if (!Common.productFitsIn(false, encryptedNttSize, coeffCount, coeffModulusSize)) {
            throw new IllegalArgumentException("invalid parameters");
        }

        // 把Plain 视为 RnsIter, 然后和 Ciphertext 下的每一个密文多项式(RnsIter)  做乘法
        assert encryptedNtt.getPolyModulusDegree() == plainNtt.getCoeffCount();
        assert encryptedNtt.getCoeffModulusSize() == plainNtt.getData().length / plainNtt.getCoeffCount();

        // 遍历每一个密文多项式
        for (int i = 0; i < encryptedNttSize; i++) {
            // 都是 Ntt 形式，直接调用这个函数做 多项式乘法
            PolyArithmeticSmallMod.dyadicProductCoeffModRnsIter(
                    encryptedNtt.getData(),
                    encryptedNtt.indexAt(i),
                    plainNtt.getData(),
                    0, // plain 是 single RnsIter 所以它的起点为 0
                    coeffModulusSize,
                    coeffCount,
                    coeffModulus,
                    encryptedNtt.indexAt(i),
                    encryptedNtt.getData()
            );
        }


    }


    public void subPlain(Ciphertext encrypted, Plaintext plain, Ciphertext destination) {
        destination.copyFrom(encrypted);
        subPlainInplace(destination, plain);
    }

    public void subPlainInplace(Ciphertext encrypted, Plaintext plain) {

        if (!ValueChecker.isMetaDataValidFor(encrypted, context)
                || !ValueChecker.isBufferValid(encrypted)) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }

        if (!ValueChecker.isMetaDataValidFor(plain, context)
                || !ValueChecker.isBufferValid(plain)) {
            throw new IllegalArgumentException("plain is not valid for encryption parameters");
        }

        Context.ContextData contextData = context.getContextData(encrypted.getParmsId());
        EncryptionParams parms = contextData.getParms();

        if (parms.getScheme() == SchemeType.BFV && encrypted.isNttForm()) {
            throw new IllegalArgumentException("BFV encrypted cannot be in NTT form");
        }

        if (parms.getScheme() == SchemeType.CKKS && !encrypted.isNttForm()) {
            throw new IllegalArgumentException("CKKS encrypted must be in NTT form");
        }

        if (parms.getScheme() == SchemeType.BGV && encrypted.isNttForm()) {
            throw new IllegalArgumentException("BGV encrypted cannot be in NTT form");
        }

        if (plain.isNttForm() != encrypted.isNttForm()) {
            throw new IllegalArgumentException("NTT form mismatch");
        }

        if (encrypted.isNttForm() && (!encrypted.getParmsId().equals(plain.getParmsId()))) {
            throw new IllegalArgumentException("encrypted and plain parameter mismatch");
        }

        if (!areSameScale(encrypted, plain)) {
            throw new IllegalArgumentException("scale mismatch");
        }

        Modulus[] coeffModulus = parms.getCoeffModulus();
        int coeffCount = parms.getPolyModulusDegree();
        int coeffModulusSize = coeffModulus.length;
        //todo: really need check?
        if (!Common.productFitsIn(false, coeffCount, coeffModulusSize)) {
            throw new IllegalArgumentException("invalid parameters");
        }

        switch (parms.getScheme()) {
            case BFV:
                // c0 - plain
                // 和 addPlainInplace 唯一的区别就是这里调用的不一样
                ScalingVariant.multiplySubPlainWithScalingVariant(
                        plain,
                        contextData,
                        encrypted.getData(),
                        coeffCount,
                        encrypted.indexAt(0)
                );
                break;
            case CKKS:
                throw new IllegalArgumentException("now un-support CKKS");
            case BGV:
                throw new IllegalArgumentException("now un-support BGV");
            default:
                throw new IllegalArgumentException("unsupported scheme");

        }
        // check
        if (encrypted.isTransparent()) {
            throw new RuntimeException("result ciphertext is transparent");
        }
    }

    public void addPlain(Ciphertext encrypted, Plaintext plain, Ciphertext destination) {
        destination.copyFrom(encrypted);
        addPlainInplace(destination, plain);
    }

    public void addPlainInplace(Ciphertext encrypted, Plaintext plain) {

        if (!ValueChecker.isMetaDataValidFor(encrypted, context)
                || !ValueChecker.isBufferValid(encrypted)) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }

        if (!ValueChecker.isMetaDataValidFor(plain, context)
                || !ValueChecker.isBufferValid(plain)) {
            throw new IllegalArgumentException("plain is not valid for encryption parameters");
        }

        Context.ContextData contextData = context.getContextData(encrypted.getParmsId());
        EncryptionParams parms = contextData.getParms();

        if (parms.getScheme() == SchemeType.BFV && encrypted.isNttForm()) {
            throw new IllegalArgumentException("BFV encrypted cannot be in NTT form");
        }

        if (parms.getScheme() == SchemeType.CKKS && !encrypted.isNttForm()) {
            throw new IllegalArgumentException("CKKS encrypted must be in NTT form");
        }

        if (parms.getScheme() == SchemeType.BGV && encrypted.isNttForm()) {
            throw new IllegalArgumentException("BGV encrypted cannot be in NTT form");
        }

        if (plain.isNttForm() != encrypted.isNttForm()) {
            throw new IllegalArgumentException("NTT form mismatch");
        }

        if (encrypted.isNttForm() && (!encrypted.getParmsId().equals(plain.getParmsId()))) {
            throw new IllegalArgumentException("encrypted and plain parameter mismatch");
        }

        if (!areSameScale(encrypted, plain)) {
            throw new IllegalArgumentException("scale mismatch");
        }

        Modulus[] coeffModulus = parms.getCoeffModulus();
        int coeffCount = parms.getPolyModulusDegree();
        int coeffModulusSize = coeffModulus.length;
        //todo: really need check?
        if (!Common.productFitsIn(false, coeffCount, coeffModulusSize)) {
            throw new IllegalArgumentException("invalid parameters");
        }

        switch (parms.getScheme()) {
            case BFV:
                // c0 + plain
                ScalingVariant.multiplyAddPlainWithScalingVariant(
                        plain,
                        contextData,
                        encrypted.getData(),
                        coeffCount,
                        encrypted.indexAt(0)
                );
                break;
            case CKKS:
                throw new IllegalArgumentException("now un-support CKKS");
            case BGV:
                throw new IllegalArgumentException("now un-support BGV");
            default:
                throw new IllegalArgumentException("unsupported scheme");

        }
        // check
        if (encrypted.isTransparent()) {
            throw new RuntimeException("result ciphertext is transparent");
        }
    }


    /**
     * Negates a ciphertext.
     *
     * @param encrypted The ciphertext to negate
     */
    public void negateInplace(Ciphertext encrypted) {

        if (!ValueChecker.isMetaDataValidFor(encrypted, context)
                || !ValueChecker.isBufferValid(encrypted)) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }
        // Extract encryption parameters.
        Context.ContextData contextData = context.getContextData(encrypted.getParmsId());
        EncryptionParams parms = contextData.getParms();
        Modulus[] coeffModulus = parms.getCoeffModulus();
        int encryptedSize = encrypted.getSize();

        // 对密文中的每一个多项式取反， 这里就是在处理一个完整的PolyIter
        PolyArithmeticSmallMod.negatePolyCoeffModPolyIter(
                encrypted.getData(),
                encrypted.getPolyModulusDegree(),
                encrypted.getCoeffModulusSize(),
                encryptedSize,
                coeffModulus,
                encrypted.getData(),
                encrypted.getPolyModulusDegree(),
                encrypted.getCoeffModulusSize()
        );
    }

    /**
     * Negates a ciphertext and stores the result in the destination parameter.
     *
     * @param encrypted   The ciphertext to negate
     * @param destination The ciphertext to overwrite with the negated result
     */
    public void negate(Ciphertext encrypted, Ciphertext destination) {
        // first copy
        destination.copyFrom(encrypted);
        // then negate
        negateInplace(destination);
    }

    public void add(Ciphertext encrypted1, Ciphertext encrypted2, Ciphertext destination) {
        // 如果两个对象地址相同
        if (encrypted2 == destination) {
            addInplace(destination, encrypted1);
        } else {
            destination.copyFrom(encrypted1);
            addInplace(destination, encrypted2);
        }
    }

    public void addMany(Ciphertext[] encrypteds, Ciphertext destination) {

        if (encrypteds == null || encrypteds.length == 0) {
            throw new IllegalArgumentException("encrypteds cannot be empty");
        }

        for (int i = 0; i < encrypteds.length; i++) {
            // 不能有地址相同的密文
            if (encrypteds[i] == destination) {
                throw new IllegalArgumentException("encrypteds must be different from destination");
            }
        }

        destination.copyFrom(encrypteds[0]);
        for (int i = 1; i < encrypteds.length; i++) {
            addInplace(destination, encrypteds[i]);
        }
    }

    public void addInplace(Ciphertext encrypted1, Ciphertext encrypted2) {

        if (!ValueChecker.isMetaDataValidFor(encrypted1, context) ||
                !ValueChecker.isBufferValid(encrypted1)
        ) {
            throw new IllegalArgumentException("encrypted1 is not valid for encryption parameters");
        }

        if (!ValueChecker.isMetaDataValidFor(encrypted2, context) ||
                !ValueChecker.isBufferValid(encrypted2)
        ) {
            throw new IllegalArgumentException("encrypted2 is not valid for encryption parameters");
        }

        if (!encrypted1.getParmsId().equals(encrypted2.getParmsId())) {
            throw new IllegalArgumentException("encrypted1 and encrypted2 parameter mismatch");
        }

        if (encrypted1.isNttForm() != encrypted2.isNttForm()) {
            throw new IllegalArgumentException("NTT form mismatch");
        }

        if (!areSameScale(encrypted1, encrypted2)) {
            throw new IllegalArgumentException("scale mismatch");
        }

        Context.ContextData contextData = context.getContextData(encrypted1.getParmsId());
        EncryptionParams parms = contextData.getParms();
        Modulus[] coeffModulus = parms.getCoeffModulus();
        Modulus plainModulus = parms.getPlainModulus();
        int coeffCount = parms.getPolyModulusDegree();
        int coeffModulusSize = coeffModulus.length;
        int encrypted1Size = encrypted1.getSize();
        int encrypted2Size = encrypted2.getSize();
        // 两个密文的 size 可能不同，例如 (c1 * c2) + c3
        int maxCount = Math.max(encrypted1Size, encrypted2Size);
        int minCount = Math.min(encrypted1Size, encrypted2Size);
        // todo: really need check this?
        if (!Common.productFitsIn(false, maxCount, coeffCount)) {
            throw new IllegalArgumentException("invalid parameters");
        }
        //todo: correction Facto 是什么？有什么用处？
        //todo: 理解这里的逻辑
        if (encrypted1.getCorrectionFactor() != encrypted2.getCorrectionFactor()) {
            // (f, e1, e2)
            long[] factors = balanceCorrectionFactors(encrypted1.getCorrectionFactor(), encrypted2.getCorrectionFactor(), plainModulus);
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffModPolyIter(
                    encrypted1.getData(),
                    coeffCount,
                    coeffModulusSize,
                    encrypted1.getSize(),
                    factors[1],
                    coeffModulus,
                    encrypted1.getData(),
                    coeffCount,
                    coeffModulusSize
            );

            Ciphertext encrypted2Copy = new Ciphertext();
            encrypted2Copy.copyFrom(encrypted2);
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffModPolyIter(
                    encrypted2.getData(),
                    coeffCount,
                    coeffModulusSize,
                    encrypted2.getSize(),
                    factors[2],
                    coeffModulus,
                    encrypted2Copy.getData(),
                    coeffCount,
                    coeffModulusSize
            );
            // Set new correction factor
            encrypted1.setCorrectionFactor(factors[0]);
            encrypted2Copy.setCorrectionFactor(factors[0]);
            // 递归调用，现在一定是进入到 else 分支
            addInplace(encrypted1, encrypted2Copy);
        } else {
            // prepare destination
            // todo: 一定需要 resize吗？ 如果 encrypted1Size = maxCount 是否就不需要 resize？
            encrypted1.resize(context, contextData.getParmsId(), maxCount);
            // AddCiphertexts
            PolyArithmeticSmallMod.addPolyCoeffModPolyIter(
                    encrypted1.getData(),
                    encrypted1.getPolyModulusDegree(),
                    encrypted1.getCoeffModulusSize(),
                    encrypted2.getData(),
                    encrypted2.getPolyModulusDegree(),
                    encrypted2.getCoeffModulusSize(),
                    minCount, // 注意这个参数，按照 size 较小的密文对齐相加，其余部分直接拷贝
                    coeffModulus,
                    encrypted1.getData(),
                    encrypted1.getPolyModulusDegree(),
                    encrypted1.getCoeffModulusSize()
            );
            // Copy the remaindering polys of the array with larger count into encrypted1
            if (encrypted1Size < encrypted2Size) {
                // 暂时弃用掉PolyCore提供的方法，因为存在一些错误，直接数组拷贝
                System.arraycopy(
                        encrypted2.getData(),
                        encrypted2.indexAt(minCount),
                        encrypted1.getData(),
                        encrypted1.indexAt(encrypted1Size),
                        (encrypted2Size - encrypted1Size) * coeffCount * coeffModulusSize
                );
            }
        }
        // 最后返回的时候 需要做一个  check
        if (encrypted1.isTransparent()) {
            throw new RuntimeException("result ciphertext is transparent");
        }
    }


    public void sub(Ciphertext encrypted1, Ciphertext encrypted2, Ciphertext destination) {
        // 如果两个对象地址相同
        if (encrypted2 == destination) {
            // 需要计算的是 c3 = c1 - c2
            // 现在是 c2 - c1
            subInplace(destination, encrypted1);
            // 所以需要再取一次反
            negateInplace(destination);
        } else {
            destination.copyFrom(encrypted1);
            subInplace(destination, encrypted2);
        }
    }


    public void subInplace(Ciphertext encrypted1, Ciphertext encrypted2) {

        if (!ValueChecker.isMetaDataValidFor(encrypted1, context) ||
                !ValueChecker.isBufferValid(encrypted1)
        ) {
            throw new IllegalArgumentException("encrypted1 is not valid for encryption parameters");
        }

        if (!ValueChecker.isMetaDataValidFor(encrypted2, context) ||
                !ValueChecker.isBufferValid(encrypted2)
        ) {
            throw new IllegalArgumentException("encrypted2 is not valid for encryption parameters");
        }

        if (!encrypted1.getParmsId().equals(encrypted2.getParmsId())) {
            throw new IllegalArgumentException("encrypted1 and encrypted2 parameter mismatch");
        }

        if (encrypted1.isNttForm() != encrypted2.isNttForm()) {
            throw new IllegalArgumentException("NTT form mismatch");
        }

        if (!areSameScale(encrypted1, encrypted2)) {
            throw new IllegalArgumentException("scale mismatch");
        }

        Context.ContextData contextData = context.getContextData(encrypted1.getParmsId());
        EncryptionParams parms = contextData.getParms();
        Modulus[] coeffModulus = parms.getCoeffModulus();
        Modulus plainModulus = parms.getPlainModulus();
        int coeffCount = parms.getPolyModulusDegree();
        int coeffModulusSize = coeffModulus.length;
        int encrypted1Size = encrypted1.getSize();
        int encrypted2Size = encrypted2.getSize();
        // 两个密文的 size 可能不同，例如 (c1 * c2) + c3
        int maxCount = Math.max(encrypted1Size, encrypted2Size);
        int minCount = Math.min(encrypted1Size, encrypted2Size);
        // todo: really need check this?
        if (!Common.productFitsIn(false, maxCount, coeffCount)) {
            throw new IllegalArgumentException("invalid parameters");
        }
        //todo: correction Facto 是什么？有什么用处？
        //todo: 理解这里的逻辑
        if (encrypted1.getCorrectionFactor() != encrypted2.getCorrectionFactor()) {
            // (f, e1, e2)
            long[] factors = balanceCorrectionFactors(encrypted1.getCorrectionFactor(), encrypted2.getCorrectionFactor(), plainModulus);
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffModPolyIter(
                    encrypted1.getData(),
                    coeffCount,
                    coeffModulusSize,
                    encrypted1.getSize(),
                    factors[1],
                    coeffModulus,
                    encrypted1.getData(),
                    coeffCount,
                    coeffModulusSize
            );

            Ciphertext encrypted2Copy = new Ciphertext();
            encrypted2Copy.copyFrom(encrypted2);

            PolyArithmeticSmallMod.multiplyPolyScalarCoeffModPolyIter(
                    encrypted2.getData(),
                    coeffCount,
                    coeffModulusSize,
                    encrypted2.getSize(),
                    factors[2],
                    coeffModulus,
                    encrypted2Copy.getData(),
                    coeffCount,
                    coeffModulusSize
            );
            // Set new correction factor
            encrypted1.setCorrectionFactor(factors[0]);
            encrypted2Copy.setCorrectionFactor(factors[0]);
            // 递归调用，现在一定是进入到 else 分支
            addInplace(encrypted1, encrypted2Copy);
        } else {
            // prepare destination
            // todo: 一定需要 resize吗？ 如果 encrypted1Size = maxCount 是否就不需要 resize？
            encrypted1.resize(context, contextData.getParmsId(), maxCount);
            // AddCiphertexts
            PolyArithmeticSmallMod.subPolyCoeffModPolyIter(
                    encrypted1.getData(),
                    encrypted1.getPolyModulusDegree(),
                    encrypted1.getCoeffModulusSize(),
                    encrypted2.getData(),
                    encrypted2.getPolyModulusDegree(),
                    encrypted2.getCoeffModulusSize(),
                    minCount, // 注意这个参数，按照 size 较小的密文对齐相加，其余部分直接拷贝
                    coeffModulus,
                    encrypted1.getData(),
                    encrypted1.getPolyModulusDegree(),
                    encrypted1.getCoeffModulusSize()
            );
            // Copy the remaindering polys of the array with larger count into encrypted1
            if (encrypted1Size < encrypted2Size) {
                // 暂时弃用掉PolyCore提供的方法，因为存在一些错误，直接数组拷贝
                // 此时 密文2 还有更多的 多项式，需要将其 取反，然后放置在 密文1 后面的位置

                // 这里我目前的 多项式运算实现版本 写起来就有些复杂了，因为这里本质上还是 PolyIter，而且需要提供起点
                // 暂时在这里写一个循环，来依次处理 每一个 RnsIter
                // 多项式起点是 minCount，终点是 encrypted2Size - 1, 我们还需要处理 (encrypted2Size - minCount)
                // 个 RnsIter
                for (int i = minCount; i < encrypted2Size; i++) {
                    PolyArithmeticSmallMod.negatePolyCoeffModRnsIter(
                            encrypted2.getData(),
                            encrypted2.indexAt(i),
                            encrypted2.getPolyModulusDegree(),
                            coeffModulusSize,
                            coeffModulus,
                            encrypted1.getData(),
                            encrypted1.getPolyModulusDegree(),
                            encrypted1.indexAt(i)
                    );
                }
            }
        }
        // 最后返回的时候 需要做一个  check
        if (encrypted1.isTransparent()) {
            throw new RuntimeException("result ciphertext is transparent");
        }
    }


    /**
     * Returns (f, e1, e2) such that
     * (1) e1 * factor1 = e2 * factor2 = f mod p;
     * (2) gcd(e1, p) = 1 and gcd(e2, p) = 1;
     * (3) abs(e1_bal) + abs(e2_bal) is minimal, where e1_bal and e2_bal represent e1 and e2 in (-p/2, p/2].
     *
     * @param factor1
     * @param factor2
     * @param plainModulus
     * @return
     */
    private long[] balanceCorrectionFactors(long factor1, long factor2, Modulus plainModulus) {

        long t = plainModulus.getValue();
        long halfT = t >> 1;

        // ratio = f2/f1 mod p
        long[] ratio = new long[]{1};
        if (!UintArithmeticSmallMod.tryInvertUintMod(factor1, plainModulus, ratio)) {
            throw new IllegalArgumentException("invalid correction factor1");
        }

        ratio[0] = UintArithmeticSmallMod.multiplyUintMod(ratio[0], factor2, plainModulus);
        long e1 = ratio[0];
        long e2 = 1;
        long sum = sumAbs(e1, e2, t, halfT);

        // Extended Euclidean
        long prevA = plainModulus.getValue();
        long prevB = 0;
        long a = ratio[0];
        long b = 1;
        while (a != 0) {

            long q = prevA / a;
            long temp = prevA % a;
            prevA = a;
            a = temp;
            // todo: really need Safe?
            temp = Common.subSafe(prevB, Common.mulSafe(b, q, false), false);
            prevB = b;
            b = temp;

            long aMod = UintArithmeticSmallMod.barrettReduce64(Math.abs(a), plainModulus);
            if (a < 0) {
                aMod = UintArithmeticSmallMod.negateUintMod(aMod, plainModulus);
            }
            long bMod = UintArithmeticSmallMod.barrettReduce64(Math.abs(b), plainModulus);
            if (b < 0) {
                bMod = UintArithmeticSmallMod.negateUintMod(aMod, plainModulus);
            }
            // which also implies gcd(b_mod, t) == 1
            if (aMod != 0 && Numth.gcd(aMod, t) == 1) {
                long newSum = sumAbs(aMod, bMod, t, halfT);
                if (newSum < sum) {
                    sum = newSum;
                    e1 = aMod;
                    e2 = bMod;
                }
            }
        }
        long f = UintArithmeticSmallMod.multiplyUintMod(e1, factor1, plainModulus);
        return new long[]{f, e1, e2};
    }

    private long sumAbs(long x, long y, long t, long halfT) {
        // todo: really need Long.compareUnsigned? can directly compare?
        long xBal = Long.compareUnsigned(x, halfT) > 0 ? x - t : x;
        long yBal = Long.compareUnsigned(y, halfT) > 0 ? y - t : y;

        return Math.abs(xBal) + Math.abs(yBal);
    }


    private boolean areSameScale(Ciphertext value1, Ciphertext value2) {
        return Common.areClose(value1.getScale(), value2.getScale());
    }

    private boolean areSameScale(Ciphertext value1, Plaintext value2) {
        return Common.areClose(value1.getScale(), value2.getScale());
    }

}
