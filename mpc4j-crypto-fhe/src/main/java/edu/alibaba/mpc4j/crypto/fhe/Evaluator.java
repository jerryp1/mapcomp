package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.context.Context;
import edu.alibaba.mpc4j.crypto.fhe.keys.GaloisKeys;
import edu.alibaba.mpc4j.crypto.fhe.keys.KeySwitchKeys;
import edu.alibaba.mpc4j.crypto.fhe.keys.PublicKey;
import edu.alibaba.mpc4j.crypto.fhe.keys.RelinKeys;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.ntt.NttTables;
import edu.alibaba.mpc4j.crypto.fhe.ntt.NttTool;
import edu.alibaba.mpc4j.crypto.fhe.params.EncryptionParams;
import edu.alibaba.mpc4j.crypto.fhe.params.ParmsIdType;
import edu.alibaba.mpc4j.crypto.fhe.params.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.rns.RnsTool;
import edu.alibaba.mpc4j.crypto.fhe.rq.PolyArithmeticSmallMod;
import edu.alibaba.mpc4j.crypto.fhe.utils.Constants;
import edu.alibaba.mpc4j.crypto.fhe.utils.GaloisTool;
import edu.alibaba.mpc4j.crypto.fhe.utils.ScalingVariant;
import edu.alibaba.mpc4j.crypto.fhe.utils.ValueChecker;
import edu.alibaba.mpc4j.crypto.fhe.zq.*;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/**
 * Provides operations on ciphertexts. Due to the properties of the encryption scheme, the arithmetic operations pass
 * through the encryption layer to the underlying plaintext, changing it according to the type of the operation. Since
 * the plaintext elements are fundamentally polynomials in the polynomial quotient ring Z_T[x]/(X^N+1), where T is the
 * plaintext modulus and X^N+1 is the polynomial modulus, this is the ring where the arithmetic operations will take
 * place. BatchEncoder (batching) provider an alternative possibly more convenient view of the plaintext elements as
 * 2-by-(N2/2) matrices of integers modulo the plaintext modulus. In the batching view the arithmetic operations act on
 * the matrices element-wise. Some of the operations only apply in the batching view, such as matrix row and column
 * rotations. Other operations such as relinearization have no semantic meaning but are necessary for performance
 * reasons.
 * <p>
 * The core operations are arithmetic operations, in particular multiplication and addition of ciphertexts. In addition
 * to these, we also provide negation, subtraction, squaring, exponentiation, and multiplication and addition of
 * several ciphertexts for convenience. in many cases some of the inputs to a computation are plaintext elements rather
 * than ciphertexts. For this we provide fast "plain" operations: plain addition, plain subtraction, and plain
 * multiplication.
 * <p>
 * One of the most important non-arithmetic operations is relinearization, which takes as input a ciphertext of size
 * K+1 and relinearization keys (at least K-1 keys are needed), and changes the size of the ciphertext down to 2
 * (minimum size). For most use-cases only one relinearization key suffices, in which case relinearization should be
 * performed after every multiplication. Homomorphic multiplication of ciphertexts of size K+1 and L+1 outputs a
 * ciphertext of size K+L+1, and the computational cost of multiplication is proportional to K*L. Plain multiplication
 * and addition operations of any type do not change the size. Relinearization requires relinearization keys to have
 * been generated.
 * <p>
 * When batching is enabled, we provide operations for rotating the plaintext matrix rows cyclically left or right, and
 * for rotating the columns (swapping the rows). Rotations require Galois keys to have been generated.
 * <p>
 * We also provide operations for transforming ciphertexts to NTT form and back, and for transforming plaintext
 * polynomials to NTT form. These can be used in a very fast plain multiplication variant, that assumes the inputs to
 * be in NTT form. Since the NTT has to be done in any case in plain multiplication, this function can be used when
 * e.g. one plaintext input is used in several plain multiplication, and transforming it several times would not make
 * sense.
 * <p>
 * When using the BFV/BGV scheme (scheme_type::bfv/bgv), all plaintexts and ciphertexts should remain by default in the
 * usual coefficient representation, i.e., not in NTT form. When using the CKKS scheme (scheme_type::ckks), all
 * plaintexts and ciphertexts should remain by default in NTT form. We call these scheme-specific NTT states the
 * "default NTT form". Some functions, such as add, work even if the inputs are not in the default state, but others,
 * such as multiply, will throw an exception. The output of all evaluation functions will be in the same state as the
 * input(s), with the exception of the transform_to_ntt and transform_from_ntt functions, which change the state.
 * Ideally, unless these two functions are called, all other functions should "just work".
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/evaluator.h
 * </p>
 */
public class Evaluator {

    /**
     * context
     */
    private final Context context;

    public Evaluator(Context context) {
        if (!context.isParametersSet()) {
            throw new IllegalArgumentException("encryption parameters are not set correctly");
        }
        this.context = context;
    }

    public void rotateColumns(Ciphertext encrypted, GaloisKeys galoisKeys, Ciphertext destination) {
        destination.copyFrom(encrypted);
        rotateColumnsInplace(destination, galoisKeys);
    }

    public void rotateColumnsInplace(Ciphertext encrypted, GaloisKeys galoisKeys) {

        SchemeType scheme = context.keyContextData().getParms().getScheme();
        if (scheme != SchemeType.BFV && scheme != SchemeType.BGV) {
            throw new IllegalArgumentException("unsupported scheme");
        }
        conjugateInternal(encrypted, galoisKeys);
    }

    private void conjugateInternal(Ciphertext encrypted, GaloisKeys galoisKeys) {


        Context.ContextData contextData = context.getContextData(encrypted.getParmsId());
        if (contextData == null) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }

        if (!contextData.getQualifiers().isUsingBatching()) {
            throw new IllegalArgumentException("encryption parameters do not support batching");
        }

        GaloisTool galoisTool = contextData.getGaloisTool();
        // Perform rotation and key switching
        applyGaloisInplace(encrypted, galoisTool.getEltFromStep(0), galoisKeys);
    }


    /**
     * only for CKKS
     *
     * @param encrypted
     * @param steps
     * @param galoisKeys
     */
    public void rotateVectorInplace(Ciphertext encrypted, int steps, GaloisKeys galoisKeys) {

        if (context.keyContextData().getParms().getScheme() != SchemeType.CKKS) {
            throw new IllegalArgumentException("unsupported scheme");
        }
        rotateInternal(encrypted, steps, galoisKeys);
    }


    public void rotateRows(Ciphertext encrypted, int steps, GaloisKeys galoisKeys, Ciphertext destination) {

        destination.copyFrom(encrypted);
        rotateRowsInplace(destination, steps, galoisKeys);
    }

    public void rotateRowsInplace(Ciphertext encrypted, int steps, GaloisKeys galoisKeys) {

        SchemeType scheme = context.keyContextData().getParms().getScheme();
        if (scheme != SchemeType.BFV && scheme != SchemeType.BGV) {
            throw new IllegalArgumentException("unsupported scheme");
        }

        rotateInternal(encrypted, steps, galoisKeys);
    }

    private void rotateInternal(Ciphertext encrypted, int steps, GaloisKeys galoisKeys) {

        Context.ContextData contextData = context.getContextData(encrypted.getParmsId());
        if (contextData == null) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }

        if (!contextData.getQualifiers().isUsingBatching()) {
            throw new IllegalArgumentException("encryption parameters do not support batching");
        }

        if (!galoisKeys.parmsId().equals(context.getKeyParmsId())) {
            throw new IllegalArgumentException("galois_keys is not valid for encryption parameters");
        }
        // Is there anything to do?
        if (steps == 0) {
            return;
        }

        int coeffCount = contextData.getParms().getPolyModulusDegree();
        GaloisTool galoisTool = contextData.getGaloisTool();

        // Check if Galois key is generated or not.
        if (galoisKeys.hasKey(galoisTool.getEltFromStep(steps))) {

//            System.out.println("galoisTool.getEltFromStep(steps): "
//                    + galoisTool.getEltFromStep(steps)
//            );

            // Perform rotation and key switching
            applyGaloisInplace(
                encrypted,
                galoisTool.getEltFromStep(steps),
                galoisKeys
            );
        } else {
            // Convert the steps to NAF: guarantees using smallest HW
            TIntArrayList nafSteps = Numth.naf(steps);

//            System.out.println("nafsteps:"
//                            + Arrays.toString(
//                            nafSteps.stream().mapToInt(Integer::intValue).toArray()
//                    )
//            );


            // If naf_steps contains only one element, then this is a power-of-two
            // rotation and we would have expected not to get to this part of the
            // if-statement.
            if (nafSteps.size() == 1) {
                throw new IllegalArgumentException("Galois key not present");
            }

            for (int i = 0; i < nafSteps.size(); i++) {
                int step = nafSteps.get(i);
                if (Math.abs(step) != (coeffCount >> 1)) {
                    // We might have a NAF-term of size coeff_count / 2; this corresponds
                    // to no rotation so we skip it. Otherwise call rotate_internal.
                    // 递归 // Apply rotation for this step
                    rotateInternal(encrypted, step, galoisKeys);
                }
            }
        }
    }


    public void applyGalois(Ciphertext encrypted,
                            int galoisElt,
                            GaloisKeys galoisKeys,
                            Ciphertext destination
    ) {

        destination.copyFrom(encrypted);
        applyGaloisInplace(destination, galoisElt, galoisKeys);
    }

    // galoisElt 视为 uint32_t
    public void applyGaloisInplace(Ciphertext encrypted, int galoisElt, GaloisKeys galoisKeys) {

        if (
            !ValueChecker.isMetaDataValidFor(encrypted, context) ||
                !ValueChecker.isBufferValid(encrypted)
        ) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }

        if (!galoisKeys.parmsId().equals(context.getKeyParmsId())) {
            throw new IllegalArgumentException("galois_keys is not valid for encryption parameters");
        }

        Context.ContextData contextData = context.getContextData(encrypted.getParmsId());
        EncryptionParams parms = contextData.getParms();
        Modulus[] coeffModulus = parms.getCoeffModulus();
        int coeffModulusSize = coeffModulus.length;
        int coeffCount = parms.getPolyModulusDegree();
        int encryptedSize = encrypted.getSize();
        // Use key_context_data where permutation tables exist since previous runs.
        GaloisTool galoisTool = context.keyContextData().getGaloisTool();
        // size check
        if (!Common.productFitsIn(false, coeffModulusSize, coeffCount)) {
            throw new IllegalArgumentException("invalid parameters");
        }

        // Check if Galois key is generated or not.
        if (!galoisKeys.hasKey(galoisElt)) {
            throw new IllegalArgumentException("Galois key not present");
        }
        // todo: need mulSafe?
        long m = 2L * coeffCount;
        if (((galoisElt & 1) == 0) || Common.unsignedGeq((long) galoisElt, m)) {
            throw new IllegalArgumentException("Galois element is not valid");
        }

        if (encryptedSize > 2) {
            throw new IllegalArgumentException("encrypted size must be 2");
        }
        // RnsIter
        long[] temp = new long[coeffCount * coeffModulusSize];

//        System.out.println(
//                "encrypted.getPolyModulusDegree(): "
//                        + encrypted.getPolyModulusDegree()
//        );
//        System.out.println(
//                "coeffCount: "
//                        + coeffCount
//        );

        if (parms.getScheme() == SchemeType.BFV || parms.getScheme() == SchemeType.BGV) {

            // !!! DO NOT CHANGE EXECUTION ORDER!!!

            // First transform encrypted.data(0)
            galoisTool.applyGaloisRnsIter(
                encrypted.getData(),
                0, // 第0 个 RnsIter
                encrypted.getPolyModulusDegree(),
                coeffModulusSize,
                galoisElt,
                coeffModulus,
                temp,
                0,
                coeffCount
            );
            // Copy result to encrypted.data(0)
            System.arraycopy(
                temp,
                0,
                encrypted.getData(),
                0,
                coeffCount * coeffModulusSize
            );
            // Next transform encrypted.data(1)
            galoisTool.applyGaloisRnsIter(
                encrypted.getData(),
                coeffCount * coeffModulusSize, // 第 1 个 RnsIter
                coeffCount,
                coeffModulusSize,
                galoisElt,
                coeffModulus,
                temp,
                0,
                coeffCount
            );
        } else if (parms.getScheme() == SchemeType.CKKS) {
            throw new IllegalArgumentException("scheme not implemented");

        } else {
            throw new IllegalArgumentException("scheme not implemented");
        }
        // Wipe encrypted.data(1)
        // 将 密文1 置0, 注意 toIndex 最好只写成下面的形式， 因为密文底层的 DynArray 的 capacity 可能会很大
        Arrays.fill(encrypted.getData(),
            coeffCount * coeffModulusSize,
            2 * coeffCount * coeffModulusSize,
            0);

        // END: Apply Galois for each ciphertext
        // REORDERING IS SAFE NOW

        // Calculate (temp * galois_key[0], temp * galois_key[1]) + (ct[0], 0)
        switchKeyInplace(
            encrypted,
            temp,
            0,
            coeffCount,
            coeffModulusSize,
            galoisKeys,
            GaloisKeys.getIndex(galoisElt)
        );

        if (encrypted.isTransparent()) {
            throw new IllegalArgumentException("result ciphertext is transparent");
        }

    }


    public void modSwitchTo(Ciphertext encrypted, ParmsIdType targetParmsId, Ciphertext destination) {

        destination.copyFrom(encrypted);
        modSwitchToInplace(destination, targetParmsId);
    }

    public void modSwitchToInplace(Ciphertext encrypted, ParmsIdType targetParmsId) {

        Context.ContextData contextData = context.getContextData(encrypted.getParmsId());
        Context.ContextData targetContextData = context.getContextData(targetParmsId);

        if (contextData == null) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }

        if (targetContextData == null) {
            throw new IllegalArgumentException("targetParmsId is not valid for encryption parameters");
        }
        // 只能从 模数多的 往 模数少的 转换
        // keyContext 的模数最多（输入参数的全部模数）, 位于 chain 顶端(chainIndex 最大)，firstContextData 其次
        if (contextData.getChainIndex() < targetContextData.getChainIndex()) {
            throw new IllegalArgumentException("cannot switch to higher level modulus");
        }

        // 一直往后切换，直到达到目标 参数
        while (!encrypted.getParmsId().equals(targetParmsId)) {
            modSwitchToNextInplace(encrypted);
        }

    }


    public void exponentiate(Ciphertext encrypted, long exponent, RelinKeys relinKeys, Ciphertext destination) {

        destination.copyFrom(encrypted);
        exponentiateInplace(destination, exponent, relinKeys);

    }


    public void exponentiateInplace(Ciphertext encrypted, long exponent, RelinKeys relinKeys) {


        Context.ContextData contextData = context.getContextData(encrypted.getParmsId());
        if (contextData == null) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }
        if (context.getContextData(relinKeys.parmsId()) == null) {
            throw new IllegalArgumentException("relin_keys is not valid for encryption parameters");
        }

        if (exponent == 0) {
            throw new IllegalArgumentException("exponent cannot be 0");
        }

        if (exponent == 1) {
            return;
        }

        // Create a vector of copies of encrypted
        // 只深拷贝1个密文对象，其余的就使用该引用
        // todo: 有更好的方式吗
        Ciphertext encryptedClone = encrypted.clone();
        Ciphertext[] expVector = new Ciphertext[(int) exponent];
        for (int i = 0; i < exponent; i++) {
            expVector[i] = encryptedClone; // 数组的每一个位置 都指向相同的 一个 密文对象
        }
        // 结果覆盖回输入
        multiplyMany(expVector, relinKeys, encrypted);
    }


    public void multiplyMany(Ciphertext[] encrypteds, RelinKeys relinKeys, Ciphertext destination) {
        // todo: 需要这样一个判断吗？
        assert Arrays.stream(encrypteds).allMatch(Objects::nonNull);

        if (encrypteds.length == 0) {
            throw new IllegalArgumentException("encrypteds vector must not be empty");
        }

        for (int i = 0; i < encrypteds.length; i++) {
            // 如果有一个相同的对象
            if (encrypteds[i] == destination) {
                throw new IllegalArgumentException("encrypteds must be different from destination");
            }
        }


        Context.ContextData contextData = context.getContextData(encrypteds[0].getParmsId());
        if (contextData == null) {
            throw new IllegalArgumentException("encrypteds is not valid for encryption parameters");
        }

        EncryptionParams parms = contextData.getParms();
        if (parms.getScheme() != SchemeType.BFV && parms.getScheme() != SchemeType.BGV) {
            throw new IllegalArgumentException("unsupported scheme");
        }

        if (encrypteds.length == 1) {
            destination.copyFrom(encrypteds[0]);
            return;
        }

        // Do first level of multiplications
        // 假设密文数组为 c0 c1 c2 c3 c4 --> (c0 * c1) (c2 * c3) (c4)
        // 所以这里的数组长度 跟  encrypteds.length 的 长度有关，可以精确计算出来
        // 这里用 vector 更方便，但是为了减少开销 就使用 Array
        // 综合考虑后面的逻辑，用 vector 吧

        // 一开始分配好足够用的 长度，避免后面更新capacity
        ArrayList<Ciphertext> productVector = new ArrayList<>(encrypteds.length);
//        Ciphertext[] productVec;
//        if ((encrypteds.length & 1) == 1) {
//            // 最低位为 1, 说明是奇数， n/2 + 1
//            productVec = new Ciphertext[(encrypteds.length >> 1) + 1];
//        }else {
//            // 最低位为0, 偶数 n/2
//            productVec = new Ciphertext[encrypteds.length >> 1];
//        }
//        int productVecIndex = 0;


        // 注意 i += 2
        for (int i = 0; i < encrypteds.length - 1; i += 2) {
            Ciphertext temp = new Ciphertext(context, contextData.getParmsId());
            // 模仿 SEAL 只需要判断 二者DynArray 的首地址相同，即认为二者是相同的 Ciphertext object
            // todo: 有更好的判断方法吗？是否需要重写 Ciphertext 的 equals ？
            if (encrypteds[i].getDynArray() == encrypteds[i + 1].getDynArray()) {
                square(encrypteds[i], temp);
            } else {
                multiply(encrypteds[i], encrypteds[i + 1], temp);
            }
            reLinearizeInplace(temp, relinKeys);
            productVector.add(temp);
//            productVec[productVecIndex++] = temp;
        }
        if ((encrypteds.length & 1) == 1) {
            // 最后一个密文放进来
//            productVec[productVecIndex] = encrypteds[encrypteds.length - 1];
            productVector.add(encrypteds[encrypteds.length - 1]);
        }

        // Repeatedly multiply and add to the back of the vector until the end is reached
        //假设现在 productVec 是: (c0 * c1) (c2 * c3) (c4)
        // 现在同样的两两相乘 就是最后结果
        // 这里用 vector 就很好处理，特别好处理，用 array 的话，逻辑比较复杂，而且可能会新开 array
        // 所以综合考虑下来，用 vector 吧

        for (int i = 0; i < productVector.size() - 1; i += 2) {
            Ciphertext temp = new Ciphertext(context, contextData.getParmsId());
            multiply(productVector.get(i), productVector.get(i + 1), temp);
            reLinearizeInplace(temp, relinKeys);
            productVector.add(temp); // 会导致 size 变化
        }
        // 最后一个密文就是计算结果
        // todo: 尝试更轻量级的返回结果的方法
        destination.copyFrom(productVector.get(productVector.size() - 1));
    }


    public void modSwitchToNextInplace(Ciphertext encrypted) {
        modSwitchToNext(encrypted, encrypted);
    }


    public void modSwitchToNext(
        Ciphertext encrypted,
        Ciphertext destination
    ) {
        if (!ValueChecker.isMetaDataValidFor(encrypted, context) || !ValueChecker.isBufferValid(encrypted)) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }

        Context.ContextData contextData = context.getContextData(encrypted.getParmsId());
        if (context.getLastParmsId().equals(encrypted.getParmsId())) {
            throw new IllegalArgumentException("end of modulus switching chain reached ");
        }

        switch (context.firstContextData().getParms().getScheme()) {
            case BFV:
                // Modulus switching with scaling
                modSwitchScaleToNext(encrypted, destination);
                break;
            case CKKS:
                throw new IllegalArgumentException("not support");
            case BGV:
                throw new IllegalArgumentException("not support");
            default:
                throw new IllegalArgumentException("unsupported scheme");

        }

        if (destination.isTransparent()) {
            throw new RuntimeException("result ciphertext is transparent");
        }
    }


    /**
     * modulus switch, the RNS base of original ciphertext is Q = {q_1, ..., q_{k-1}, q_k},
     * the RNS base of result is Q'={q_1, ..., q_{k-1}}.
     *
     * @param encrypted   the ciphertext to modulus switch.
     * @param destination the ciphertext to overwrite with round(ct * Q'/Q)
     */
    private void modSwitchScaleToNext(Ciphertext encrypted, Ciphertext destination) {
        Context.ContextData contextData = context.getContextData(encrypted.getParmsId());
        if (contextData.getParms().getScheme() == SchemeType.BFV && encrypted.isNttForm()) {
            throw new IllegalArgumentException("BFV encrypted cannot be in NTT form");
        }

        if (contextData.getParms().getScheme() == SchemeType.CKKS && !encrypted.isNttForm()) {
            throw new IllegalArgumentException("CKKS encrypted must be in NTT form");
        }

        if (contextData.getParms().getScheme() == SchemeType.BGV && encrypted.isNttForm()) {
            throw new IllegalArgumentException("BGV encrypted cannot be in NTT form");
        }

        Context.ContextData nextContextData = contextData.getNextContextData();
        EncryptionParams nextParms = nextContextData.getParms();
        RnsTool rnsTool = contextData.getRnsTool();

        int encryptedSize = encrypted.getSize();
        int coeffCount = nextParms.getPolyModulusDegree();
        int nextCoeffModulusSize = nextParms.getCoeffModulus().length;


        Ciphertext encryptedCopy = new Ciphertext();
        encryptedCopy.copyFrom(encrypted);

        switch (nextParms.getScheme()) {
            case BFV:
                // 遍历处理一个 PolyIter 中的每一个 RnsIter
                for (int i = 0; i < encryptedSize; i++) {
                    // 注意起点指向每一个 RnsIter
                    rnsTool.divideAndRoundQLastInplace(
                        encryptedCopy.getData(),
                        i * encryptedCopy.getPolyModulusDegree() * encryptedCopy.getCoeffModulusSize(),
                        encryptedCopy.getPolyModulusDegree(),
                        encryptedCopy.getCoeffModulusSize()
                    );
                }
                break;
            case CKKS:
                throw new IllegalArgumentException("not support");
            case BGV:
                throw new IllegalArgumentException("not support");
            default:
                throw new IllegalArgumentException("unsupported scheme");
        }

        // Copy result to destination, 注意这里修改了 密文的 parmsId
        destination.resize(context, nextContextData.getParmsId(), encryptedSize);
        // 这里不能直接 Copy 整个 PolyIter 背后的数组，而是要 逐步分解为 RnsIter 再Copy
        // 因为两个 密文的 k 不一样
        for (int i = 0; i < encryptedSize; i++) {
            System.arraycopy(
                encryptedCopy.getData(),
                i * coeffCount * encryptedCopy.getCoeffModulusSize(), // k 是不同的
                destination.getData(),
                i * coeffCount * destination.getCoeffModulusSize(),
                coeffCount * nextCoeffModulusSize
            );
        }

        destination.setIsNttForm(encrypted.isNttForm());

        // todo: implement CKKS and BGV
        if (nextParms.getScheme() == SchemeType.CKKS) {

        } else if (nextParms.getScheme() == SchemeType.BGV) {

        }
    }


    public void reLinearize(Ciphertext encrypted, RelinKeys relinKeys, Ciphertext destination) {
        destination.copyFrom(encrypted);
        reLinearizeInternal(destination, relinKeys, 2);
    }


    public void reLinearizeInplace(Ciphertext encrypted, RelinKeys relinKeys) {
        reLinearizeInternal(encrypted, relinKeys, 2);
    }


    private void reLinearizeInternal(Ciphertext encrypted, RelinKeys relinKeys, int destinationSize) {

        Context.ContextData contextData = context.getContextData(encrypted.getParmsId());
        if (contextData == null) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }
        if (!relinKeys.parmsId().equals(context.getKeyParmsId())) {
            throw new IllegalArgumentException("relinKeys is not valid for encryption parameters");
        }

        int encryptedSize = encrypted.getSize();

        if (destinationSize < 2 || destinationSize > encryptedSize) {
            throw new IllegalArgumentException("destinationSize must be at least 2 and less than or equal to current count");
        }
        if (relinKeys.size() < Common.subSafe(encryptedSize, 2, false)) {
            throw new IllegalArgumentException("not enough relinearization keys");
        }

        // If encrypted is already at the desired level, return
        if (destinationSize == encryptedSize) {
            return;
        }

        // Calculate number of relinearize_one_step calls needed
        int reLinsNeeded = encryptedSize - destinationSize;

        // Iterator pointing to the last component of encrypted
        // 起点，指向这样一个 RnsIter, 也就是密文里的最后一个多项式
        int encryptedIter = (encryptedSize - 1) * encrypted.getPolyModulusDegree()
            * encrypted.getCoeffModulusSize();

        for (int i = 0; i < reLinsNeeded; i++) {
            switchKeyInplace(
                encrypted,
                encrypted.getData(),
                encryptedIter,
                encrypted.getPolyModulusDegree(),
                encrypted.getCoeffModulusSize(),
                (KeySwitchKeys) relinKeys,
                RelinKeys.getIndex(encryptedSize - 1 - i)
            );
        }

        encrypted.resize(context, contextData.getParmsId(), destinationSize);

        // Put the output of final relinearization into destination.
        // Prepare destination only at this point because we are resizing down
        if (encrypted.isTransparent()) {
            throw new RuntimeException("result ciphertext is transparent");
        }
    }


    /**
     * long[] + index + N + k 表示一个 RnsIter
     * <p>
     * 逻辑非常复杂，极易出错
     *
     * @param encrypted
     * @param targetIter           a RnsIter
     * @param targetIterStartIndex
     * @param targertIterN
     * @param targetIterK
     * @param keySwitchKeys
     * @param keySwitchKyesIndex
     */
    private void switchKeyInplace(
        Ciphertext encrypted,
        long[] targetIter,
        int targetIterStartIndex,
        int targertIterN,
        int targetIterK,
        KeySwitchKeys keySwitchKeys,
        int keySwitchKyesIndex
    ) {

        ParmsIdType parmsId = encrypted.getParmsId();
        Context.ContextData contextData = context.getContextData(parmsId);
        EncryptionParams parms = contextData.getParms();
        Context.ContextData keyContextData = context.keyContextData();
        EncryptionParams keyParms = keyContextData.getParms();
        SchemeType scheme = parms.getScheme();


        if (!ValueChecker.isMetaDataValidFor(encrypted, context) ||
            !ValueChecker.isBufferValid(encrypted)
        ) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }

        if (targetIter == null) {
            throw new IllegalArgumentException("target_iter cannot be null");
        }

        if (!context.isUsingKeySwitching()) {
            throw new IllegalArgumentException("keyswitching is not supported by the context");
        }

        // Don't validate all of kswitch_keys but just check the parms_id.
        if (!keySwitchKeys.parmsId().equals(context.getKeyParmsId())) {
            throw new IllegalArgumentException("parameter mismatch");
        }

        if (keySwitchKyesIndex >= keySwitchKeys.data().length) {
            throw new ArrayIndexOutOfBoundsException("keySwitchKyesIndex");
        }

        if (scheme == SchemeType.BFV && encrypted.isNttForm()) {
            throw new IllegalArgumentException("BFV encrypted cannot be in NTT form");
        }
        if (scheme == SchemeType.CKKS && !encrypted.isNttForm()) {
            throw new IllegalArgumentException("CKKS encrypted must be in NTT form");
        }
        if (scheme == SchemeType.BGV && encrypted.isNttForm()) {
            throw new IllegalArgumentException("BGV encrypted cannot be in NTT form");
        }

        int coeffCount = parms.getPolyModulusDegree();
        int decompModulusSize = parms.getCoeffModulus().length;
        Modulus[] keyModulus = keyParms.getCoeffModulus();
        int keyModulusSize = keyModulus.length;
        int rnsModulusSize = decompModulusSize + 1;
        NttTables[] keyNttTables = keyContextData.getSmallNttTables();
        MultiplyUintModOperand[] modSwitchFactors = keyContextData.getRnsTool().getInvQLastModQ();

        assert targertIterN == coeffCount;
        assert targetIterK == decompModulusSize;

        // todo: really need check?
        if (!Common.productFitsIn(false, coeffCount, rnsModulusSize, 2)) {
            throw new IllegalArgumentException("invalid parameters");
        }
        // Prepare input
        PublicKey[] keyVector = keySwitchKeys.data()[keySwitchKyesIndex];
        int keyComponentCount = keyVector[0].data().getSize();// 密文的 size

        // Check only the used component in KSwitchKeys.
        for (PublicKey eachKey : keyVector) {

            if (!ValueChecker.isMetaDataValidFor(eachKey, context) ||
                !ValueChecker.isBufferValid(eachKey)
            ) {
                throw new IllegalArgumentException("kswitch_keys is not valid for encryption parameters");
            }
        }

        // Create a copy of target_iter
        long[] tTarget = new long[coeffCount * decompModulusSize];
        System.arraycopy(
            targetIter,
            targetIterStartIndex,
            tTarget,
            0,
            decompModulusSize * coeffCount
        );

        // In CKKS t_target is in NTT form; switch back to normal form
        if (scheme == SchemeType.CKKS) {
            NttTool.inverseNttNegacyclicHarveyRns(
                tTarget,
                coeffCount,
                decompModulusSize,
                keyNttTables
            );
        }
        // Temporary result

        long[] tPolyProd = new long[keyComponentCount * coeffCount * rnsModulusSize];
        for (int i = 0; i < rnsModulusSize; i++) {

            int keyIndex = (i == decompModulusSize ? keyModulusSize - 1 : i);
            // Product of two numbers is up to 60 + 60 = 120 bits, so we can sum up to 256 of them without reduction.
            int lazyReductionSummandBound = Constants.MULTIPLY_ACCUMULATE_USER_MOD_MAX;
            int lazyReductionCounter = lazyReductionSummandBound;

//            System.out.println("i: " + i + ", lazyReductionCounter: " + lazyReductionCounter);
//            System.out.println("i: " + i + ", lazyReductionSummandBound: " + lazyReductionSummandBound);

            // Allocate memory for a lazy accumulator (128-bit coefficients)
            // 2 个 long 表示 128-bit, 可认为是 accumulator_iter
            long[] tPolyLazy = new long[keyComponentCount * coeffCount * 2];
            // Semantic misuse of PolyIter; this is really pointing to the data for a single RNS factor
            // tPolyLazy 视为 一个特殊的PolyIter, k = coeffCount , N = 2
            for (int j = 0; j < decompModulusSize; j++) {
                // a CoeffIter
                long[] tNtt = new long[coeffCount];
                long[] tOperand = new long[coeffCount];
                int tOperandIndex = 0;
                // RNS-NTT form exists in input
                if ((scheme == SchemeType.CKKS) && (i == j)) {
                    // 此时 t_operand 指向 target_iter(RnsIter) 的 第 j 个 CoeffIter
                    tOperandIndex = j * coeffCount;
                } else {// Perform RNS-NTT conversion


                    // No need to perform RNS conversion (modular reduction)
                    if (keyModulus[j].getValue() <= keyModulus[keyIndex].getValue()) {
                        // 直接复制, 注意起点
                        System.arraycopy(
                            tTarget,
                            j * coeffCount,
                            tNtt,
                            0,
                            coeffCount);
                    } else {  // Perform RNS conversion (modular reduction)
                        // 处理一个 CoeffIter
                        PolyArithmeticSmallMod.moduloPolyCoeff(
                            tTarget,
                            j * coeffCount,
                            coeffCount,
                            keyModulus[keyIndex],
                            tNtt,
                            0
                        );
                    }
                    // NTT conversion lazy outputs in [0, 4q)
                    // 直接对整个 CoffIter 处理
                    NttTool.nttNegacyclicHarveyLazy(tNtt, keyNttTables[keyIndex]);
                    // 一定要赋值给 tOperand 吗？
                    tOperand = tNtt;
                }

                // Multiply with keys and modular accumulate products in a lazy fashion
                // 这里的 for 是对 iter(key_vector[J].data(), accumulator_iter) 这两个 PolyIter 的迭代
                // accumulator_iter 就是 tPolyLazy
                Ciphertext keyVectorJ = keyVector[j].data();
                for (int k = 0; k < keyComponentCount; k++) {
                    // keyVectorJ 的第 k 个 RnsIter 的起点
                    int keyVectorJ_K = k * keyVectorJ.getCoeffModulusSize() * keyVectorJ.getPolyModulusDegree();
                    int tPolyLazyK = k * coeffCount * 2;// 注意 k = coeffCount , N = 2
                    // todo: 确实存在这个问题，如果还是按现在的逻辑，这里始终都是 false, 对照SEAL 再看看
                    if (lazyReductionCounter == 0) {
                        // 这个 for 就是对一个 CoeffIter 的迭代了
                        for (int l = 0; l < coeffCount; l++) {
                            long[] qWord = new long[2];
                            UintArithmetic.multiplyUint64(
                                tOperand[l],
                                keyVectorJ.getData()[keyVectorJ_K + keyIndex * keyVectorJ.getPolyModulusDegree() + l],
                                qWord
                            );
                            // Accumulate product of t_operand and t_key_acc to t_poly_lazy and reduce
                            long[] uint128Temp = new long[]{
                                tPolyLazy[tPolyLazyK + l * 2],
                                tPolyLazy[tPolyLazyK + l * 2 + 1],
                            };

                            UintArithmetic.addUint128(
                                qWord,
                                uint128Temp,
                                qWord
                            );
                            // 覆盖回 tPolyLazy
                            tPolyLazy[tPolyLazyK + l * 2] = UintArithmeticSmallMod.barrettReduce128(qWord, keyModulus[keyIndex]);
                            tPolyLazy[tPolyLazyK + l * 2 + 1] = 0;
                        }
                    } else {
                        // Same as above but no reduction
                        // 这个 for 就是对一个 CoeffIter 的迭代了
                        for (int l = 0; l < coeffCount; l++) {
                            long[] qWord = new long[2];
                            UintArithmetic.multiplyUint64(
                                tOperand[l],
                                keyVectorJ.getData()[keyVectorJ_K + keyIndex * keyVectorJ.getPolyModulusDegree() + l],
                                qWord
                            );
                            // Accumulate product of t_operand and t_key_acc to t_poly_lazy and reduce
                            long[] uint128Temp = new long[]{
                                tPolyLazy[tPolyLazyK + l * 2],
                                tPolyLazy[tPolyLazyK + l * 2 + 1],
                            };

                            UintArithmetic.addUint128(
                                qWord,
                                uint128Temp,
                                qWord
                            );
                            // 覆盖回 tPolyLazy
                            tPolyLazy[tPolyLazyK + l * 2] = qWord[0];
                            tPolyLazy[tPolyLazyK + l * 2 + 1] = qWord[1];
                        }
                    } // else end
                } // k end

                if (--lazyReductionCounter == 0) {
                    lazyReductionCounter = lazyReductionSummandBound;
                }
            } // J end
            // PolyIter pointing to the destination t_poly_prod, shifted to the appropriate modulus
            // 指向 tPolyProd 的 一个 PolyIter , N = coeffCount, k = rnsModulusSize
            // todo: 这里可能出错，这里偏移量的理解可能有问题
            int tPolyProdIter = i * coeffCount;

            // Final modular reduction
            // 这里是对 accumulator_iter(tPolyLazy) t_poly_prod 两个 PolyIter 进行迭代
            for (int k = 0; k < keyComponentCount; k++) {
                if (lazyReductionCounter == lazyReductionSummandBound) {

                    for (int l = 0; l < coeffCount; l++) {
                        tPolyProd[tPolyProdIter + k * coeffCount * rnsModulusSize + l] =
                            tPolyLazy[k * coeffCount * 2 + 2 * l];
                    }
                } else {

                    long[] uint128Temp = new long[2];
                    for (int l = 0; l < coeffCount; l++) {
                        uint128Temp[0] = tPolyLazy[k * coeffCount * 2 + 2 * l];
                        uint128Temp[1] = tPolyLazy[k * coeffCount * 2 + 2 * l + 1];
                        tPolyProd[tPolyProdIter + k * coeffCount * rnsModulusSize + l] =
                            UintArithmeticSmallMod.barrettReduce128(
                                uint128Temp,
                                keyModulus[keyIndex]);
                    }
                }
            }// K end
        } // I end
        // Accumulated products are now stored in t_poly_prod

        // Perform modulus switching with scaling
        // 再次把 tPolyProd 视为 PolyIter，只不过现在起点为0, N = coeffCount, k = rnsModulusSize
        // 对 encrypted, t_poly_prod_iter 进行迭代
        for (int i = 0; i < keyComponentCount; i++) {
            if (scheme == SchemeType.BGV) {
                throw new IllegalArgumentException("unsupport BGV");
            } else { // BFV 和 CKKS 的逻辑相同
                // Lazy reduction; this needs to be then reduced mod qi
                // 定位到 t_poly_prod 的 第 i 个 RnsIter 中的 第 decomp_modulus_size 个 CoeffIter
                int tLastIndex = i * coeffCount * rnsModulusSize + decompModulusSize * coeffCount;
                NttTool.inverseNttNegacyclicHarveyLazy(
                    tPolyProd,
                    tLastIndex,
                    keyNttTables[keyModulusSize - 1]
                );
                // Add (p-1)/2 to change from flooring to rounding.
                long qk = keyModulus[keyModulusSize - 1].getValue();
                long qkHalf = qk >>> 1;

                // 处理 tLast
                for (int j = 0; j < coeffCount; j++) {
                    tPolyProd[tLastIndex + j] = UintArithmeticSmallMod.barrettReduce64(
                        tPolyProd[tLastIndex + j] + qkHalf,
                        keyModulus[keyModulusSize - 1]
                    );
                }

                for (int j = 0; j < decompModulusSize; j++) {
                    long[] tNtt = new long[coeffCount];
                    // (ct mod 4qk) mod qi
                    long qi = keyModulus[j].getValue();
                    if (qk > qi) {
                        // This cannot be spared. NTT only tolerates input that is less than 4*modulus (i.e. qk <=4*qi).
                        PolyArithmeticSmallMod.moduloPolyCoeff(
                            tPolyProd,
                            tLastIndex,
                            coeffCount,
                            keyModulus[j],
                            tNtt,
                            0
                        );
                    } else {
                        // 直接 copy
                        System.arraycopy(
                            tPolyProd,
                            tLastIndex,
                            tNtt,
                            0,
                            coeffCount
                        );
                    }
                    // Lazy substraction, results in [0, 2*qi), since fix is in [0, qi].
                    long fix = qi - UintArithmeticSmallMod.barrettReduce64(qkHalf, keyModulus[j]);
                    for (int k = 0; k < coeffCount; k++) {
                        tNtt[k] = tNtt[k] + fix;
                    }
                    // 用来定位 get<0, 1>(J), tPolyProd 中 第 i 个 RnsIter 中的 第 j 个 CoeffIter
                    int zeroOneJ = i * coeffCount * rnsModulusSize + j * coeffCount;
                    long qiLazy = qi << 1;
                    if (scheme == SchemeType.CKKS) {
                        throw new IllegalArgumentException("unsupported CKKS");
                    } else if (scheme == SchemeType.BFV) {
                        NttTool.inverseNttNegacyclicHarveyLazy(
                            tPolyProd,
                            zeroOneJ,
                            keyNttTables[j]
                        );
                    }
                    // ((ct mod qi) - (ct mod qk)) mod qi with output in [0, 2 * qi_lazy)
                    for (int k = 0; k < coeffCount; k++) {
                        tPolyProd[zeroOneJ + k] = tPolyProd[zeroOneJ + k] + qiLazy - tNtt[k];
                    }

                    // qk^(-1) * ((ct mod qi) - (ct mod qk)) mod qi
                    PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(
                        tPolyProd,
                        zeroOneJ,
                        coeffCount,
                        modSwitchFactors[j],
                        keyModulus[j],
                        tPolyProd,
                        zeroOneJ
                    );
                    //todo: 修改更容易理解的变量名
                    int zeroZeroJ = i * encrypted.getPolyModulusDegree() * encrypted.getCoeffModulusSize() + j * encrypted.getPolyModulusDegree();
                    PolyArithmeticSmallMod.addPolyCoeffMod(
                        tPolyProd,
                        zeroOneJ,
                        encrypted.getData(),
                        zeroZeroJ,
                        coeffCount,
                        keyModulus[j],
                        encrypted.getData(),
                        zeroZeroJ
                    );
                }
            }
        }
    }


    public void square(Ciphertext encrypted, Ciphertext destination) {
        destination.copyFrom(encrypted);
        squareInplace(destination);
    }


    public void squareInplace(Ciphertext encrypted) {

        if (!ValueChecker.isMetaDataValidFor(encrypted, context)
            || !ValueChecker.isBufferValid(encrypted)
        ) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }

        Context.ContextData contextData = context.firstContextData();
        switch (contextData.getParms().getScheme()) {
            case BFV:
                bfvSquare(encrypted);
                break;
            case CKKS:
                throw new IllegalArgumentException("do not support CKKS");
            case BGV:
                throw new IllegalArgumentException("do not support BGV");
            default:
                throw new IllegalArgumentException("unsupported scheme");
        }

        if (encrypted.isTransparent()) {
            throw new IllegalArgumentException("result ciphertext is transparent");
        }
    }

    public void bfvSquare(Ciphertext encrypted) {

        if (encrypted.isNttForm()) {
            throw new IllegalArgumentException("encrypted cannot be in NTT form");
        }

        Context.ContextData contextData = context.getContextData(encrypted.getParmsId());
        EncryptionParams parms = contextData.getParms();
        int coeffCount = parms.getPolyModulusDegree();
        int baseQSize = parms.getCoeffModulus().length;
        int encryptedSize = encrypted.getSize();
        long plainModulus = parms.getPlainModulus().getValue();

        RnsTool rnsTool = contextData.getRnsTool();
        int baseBskSize = rnsTool.getBaseBsk().getSize();
        int baseBskMTildeSize = rnsTool.getBaseBskMTilde().getSize();
        // Optimization implemented currently only for size 2 ciphertexts
        if (encryptedSize != 2) {
            bfvMultiply(encrypted, encrypted);
            return;
        }


        // Determine destination.size()
        // 每个密文包含2个多项式的情况下，乘法结果包含3个密文---> 2 + 2 - 1
        // todo: need subSafe?
        int destinationSize = Common.subSafe(Common.addSafe(encryptedSize, encryptedSize, false), 1, false);
        // todo: need check?
        if (!Common.productFitsIn(false, destinationSize, coeffCount, baseBskMTildeSize)) {
            throw new IllegalArgumentException("invalid parameters");
        }
        // Set up iterators for bases
        Modulus[] baseQ = parms.getCoeffModulus();
        Modulus[] baseBsk = rnsTool.getBaseBsk().getBase();

        //
        NttTables[] baseQNttTables = contextData.getSmallNttTables();
        NttTables[] baseBskNttTables = rnsTool.getBaseBskNttTables();
        // Microsoft SEAL uses BEHZ-style RNS multiplication. For details, see Evaluator::bfv_multiply. This function
        // uses additionally Karatsuba multiplication to reduce the complexity of squaring a size-2 ciphertext, but the
        // steps are otherwise the same as in Evaluator::bfv_multiply.

        // Resize encrypted1 to destination size
        encrypted.resize(context, contextData.getParmsId(), destinationSize);
//        System.out.println("encrpted1: \n" + Arrays.toString(encrypted1.getData()));

        // This lambda function takes as input an IterTuple with three components:
        //
        // 1. (Const)RNSIter to read an input polynomial from
        // 2. RNSIter for the output in base q
        // 3. RNSIter for the output in base Bsk
        //
        // It performs steps (1)-(3) of the BEHZ multiplication (see above) on the given input polynomial (given as an
        // RNSIter or ConstRNSIter) and writes the results in base q and base Bsk to the given output
        // iterators.
        // 这里我就不写成 lambda 函数了，也不重新封装为一个 方法，就顺着往下写吧

        // Allocate space for a base q output of behz_extend_base_convert_to_ntt for encrypted1
        // 这是一个 PolyIter
        long[] encryptedQ = new long[encryptedSize * coeffCount * baseQSize];

        // Allocate space for a base Bsk output of behz_extend_base_convert_to_ntt for encrypted1
        long[] encryptedBsk = new long[encryptedSize * coeffCount * baseBskSize];

        // Perform BEHZ steps (1)-(3) for encrypted1
        // 对标: SEAL_ITERATE(iter(encrypted1, encrypted1_q, encrypted1_Bsk), encrypted1_size, behz_extend_base_convert_to_ntt);
        for (int i = 0; i < encryptedSize; i++) {
            // 依次遍历密文中的每一个 RnsIter
            // 这里面的处理逻辑就对应 behz_extend_base_convert_to_ntt

            // 先完成当前RnsIter拷贝，注意起点的计算是不一样的，对标 set_poly(get<0>(I), coeff_count, base_q_size, get<1>(I));
            System.arraycopy(
                encrypted.getData(),
                i * coeffCount * encrypted.getCoeffModulusSize(),
                encryptedQ,
                i * coeffCount * baseQSize,
                coeffCount * baseQSize
            );

//            System.out.println("set poly, encrypted1Q:\n " +
//                    Arrays.toString(
//                            Arrays.copyOfRange(encrypted1Q, i * coeffCount * baseQSize, (i+1) * coeffCount * baseQSize)
//                    )
//            );

            //
            NttTool.nttNegacyclicHarveyLazyPoly(encryptedQ, encryptedSize, coeffCount, baseQSize, i, baseQNttTables);
//            System.out.println("ntt lazy, encrypted1Q: \n" +
//                    Arrays.toString(
//                            Arrays.copyOfRange(encrypted1Q, i * coeffCount * baseQSize, (i+1) * coeffCount * baseQSize)
//                    )
//            );

            // Allocate temporary space for a polynomial in the Bsk U {m_tilde} base
            // 这是一个 RnsIter
            long[] temp = new long[coeffCount * baseBskMTildeSize];

            // 1) Convert from base q to base Bsk U {m_tilde}
            rnsTool.fastBConvMTildeRnsIter(
                encrypted.getData(),
                i * coeffCount * encrypted.getCoeffModulusSize(),
                coeffCount,
                encrypted.getCoeffModulusSize(),
                temp,
                0,
                coeffCount,
                baseBskMTildeSize
            );

            // (2) Reduce q-overflows in with Montgomery reduction, switching base to Bsk
//            rns_tool->sm_mrq(temp, get<2>(I), pool);
            rnsTool.smMrqRnsIter(
                temp,
                0,
                coeffCount,
                baseBskMTildeSize,
                encryptedBsk, // 对应 get<2>(I)
                i * coeffCount * baseBskSize, // 注意起点
                coeffCount,
                baseBskSize
            );

            // Transform to NTT form in base Bsk
            // Lazy reduction
            NttTool.nttNegacyclicHarveyLazyPoly(encryptedBsk, encryptedSize, coeffCount, baseBskSize, i, baseBskNttTables);
        }

        // Allocate temporary space for the output of step (4)
        // We allocate space separately for the base q and the base Bsk components
        // 均为 polyIter
        long[] tempDestinationQ = new long[destinationSize * coeffCount * baseQSize];
        long[] tempDestinationBsk = new long[destinationSize * coeffCount * baseBskSize];

        // Perform BEHZ step (4): dyadic multiplication on arbitrary size ciphertexts
        // todo: 尝试并行化加速
        // 依次处理 每一个 RnsIter
        // 单独一个代码块来处理 behz_ciphertext_square(encrypted_q, base_q, base_q_size, temp_dest_q);
        {
            // compute c0^2
            PolyArithmeticSmallMod.dyadicProductCoeffModRns(
                encryptedQ,
                0, // 注意起点固定为 0
                coeffCount,
                baseQSize,
                encryptedQ,
                0,
                coeffCount,
                baseQSize,
                baseQ,
                tempDestinationQ,
                0,
                coeffCount,
                baseQSize
            );
            // compute 2* c0 * c1
            PolyArithmeticSmallMod.dyadicProductCoeffModRns(
                encryptedQ,
                0, // 注意起点固定为 0
                coeffCount,
                baseQSize,
                encryptedQ,
                coeffCount * baseQSize, // 这里起点固定为 1
                coeffCount,
                baseQSize,
                baseQ,
                tempDestinationQ,
                coeffCount * baseQSize,
                coeffCount,
                baseQSize
            );
            PolyArithmeticSmallMod.addPolyCoeffModRns(
                tempDestinationQ,
                coeffCount * baseQSize, // 起点
                coeffCount,
                baseQSize,
                tempDestinationQ,
                coeffCount * baseQSize, // 起点
                coeffCount,
                baseQSize,
                baseQ,
                tempDestinationQ,
                coeffCount * baseQSize, // 起点
                coeffCount,
                baseQSize
            );

            // Compute c1^2
            PolyArithmeticSmallMod.dyadicProductCoeffModRns(
                encryptedQ,
                coeffCount * baseQSize, // 注意起点固定为 1
                coeffCount,
                baseQSize,
                encryptedQ,
                coeffCount * baseQSize,
                coeffCount,
                baseQSize,
                baseQ,
                tempDestinationQ,
                2 * coeffCount * baseQSize, // 这里的起点就是 2 了
                coeffCount,
                baseQSize
            );
        }
        // behz_ciphertext_square(encrypted_Bsk, base_Bsk, base_Bsk_size, temp_dest_Bsk);
        {
            // compute c0^2
            PolyArithmeticSmallMod.dyadicProductCoeffModRns(
                encryptedBsk,
                0, // 注意起点固定为 0
                coeffCount,
                baseBskSize,
                encryptedBsk,
                0,
                coeffCount,
                baseBskSize,
                baseBsk,
                tempDestinationBsk,
                0,
                coeffCount,
                baseBskSize
            );
            // compute 2* c0 * c1
            PolyArithmeticSmallMod.dyadicProductCoeffModRns(
                encryptedBsk,
                0, // 注意起点固定为 0
                coeffCount,
                baseBskSize,
                encryptedBsk,
                coeffCount * baseBskSize, // 这里起点固定为 1
                coeffCount,
                baseBskSize,
                baseBsk,
                tempDestinationBsk,
                coeffCount * baseBskSize,
                coeffCount,
                baseBskSize
            );
            PolyArithmeticSmallMod.addPolyCoeffModRns(
                tempDestinationBsk,
                coeffCount * baseBskSize, // 起点
                coeffCount,
                baseBskSize,
                tempDestinationBsk,
                coeffCount * baseBskSize, // 起点
                coeffCount,
                baseBskSize,
                baseBsk,
                tempDestinationBsk,
                coeffCount * baseBskSize, // 起点
                coeffCount,
                baseBskSize
            );

            // Compute c1^2
            PolyArithmeticSmallMod.dyadicProductCoeffModRns(
                encryptedBsk,
                coeffCount * baseBskSize, // 注意起点固定为 1
                coeffCount,
                baseBskSize,
                encryptedBsk,
                coeffCount * baseBskSize,
                coeffCount,
                baseBskSize,
                baseBsk,
                tempDestinationBsk,
                2 * coeffCount * baseBskSize, // 这里的起点就是 2 了
                coeffCount,
                baseBskSize
            );
        }


//        System.out.println("step(4) tempDestinationQ: \n" + Arrays.toString(tempDestinationQ));
//        System.out.println("step(4) tempDestinationBsk: \n" + Arrays.toString(tempDestinationBsk));
//


        // Perform BEHZ step (5): transform data from NTT form
        // Lazy reduction here. The following multiply_poly_scalar_coeffmod will correct the value back to [0, p)
        // 处理整个 polyIter
        NttTool.inverseNttNegacyclicHarveyLazyPoly(tempDestinationQ, destinationSize, coeffCount, baseQSize, baseQNttTables);

        NttTool.inverseNttNegacyclicHarveyLazyPoly(tempDestinationBsk, destinationSize, coeffCount, baseBskSize, baseBskNttTables);

//        System.out.println("step(4) tempDestinationQ after inverse ntt \n" + Arrays.toString(tempDestinationQ));
//        System.out.println("step(4) tempDestinationBsk after inverse ntt \n" + Arrays.toString(tempDestinationBsk));


        // Perform BEHZ steps (6)-(8)
        for (int i = 0; i < destinationSize; i++) {
            // Bring together the base q and base Bsk components into a single allocation
            // a RnsIter
            long[] tempQBsk = new long[coeffCount * (baseQSize + baseBskSize)];

            // Step (6): multiply base q components by t (plain_modulus)
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffModRns(
                tempDestinationQ,
                i * coeffCount * baseQSize, // 注意这里的 k
                coeffCount,
                baseQSize,
                plainModulus,
                baseQ,
                tempQBsk,
                0,
                coeffCount, // 注意起点是0, 然后往后的 coeffCount * baseQSize 被占据
                baseQSize
            );

            PolyArithmeticSmallMod.multiplyPolyScalarCoeffModRns(
                tempDestinationBsk,
                i * coeffCount * baseBskSize, // 注意这里的 k
                coeffCount,
                baseBskSize,
                plainModulus,
                baseBsk,
                tempQBsk,
                baseQSize * coeffCount, // 注意起点
                coeffCount,
                baseBskSize
            );

            // Allocate yet another temporary for fast divide-and-floor result in base Bsk
            // a RnsIter
            long[] tempBsk = new long[coeffCount * baseBskSize];

            // Step (7): divide by q and floor, producing a result in base Bsk
            rnsTool.fastFloorRnsIter(
                tempQBsk,
                0,
                coeffCount,
                baseQSize + baseBskSize,
                tempBsk,
                0,
                coeffCount,
                baseBskSize
            );

            // Step (8): use Shenoy-Kumaresan method to convert the result to base q and write to encrypted1
            rnsTool.fastBConvSkRnsIter(
                tempBsk,
                0,
                coeffCount,
                baseBskSize,
                encrypted.getData(),
                i * coeffCount * encrypted.getCoeffModulusSize(),// 注意起点
                encrypted.getPolyModulusDegree(),
                encrypted.getCoeffModulusSize()
            );

        }


    }


    public void multiply(Ciphertext encrypted1, Ciphertext encrypted2, Ciphertext destination) {
        // 如果地址相同
        if (encrypted2 == destination) {
            multiplyInplace(destination, encrypted1);
        } else {
            destination.copyFrom(encrypted1);
            multiplyInplace(destination, encrypted2);
        }
    }


    /**
     * Multiplies two ciphertexts. This functions computes the product of encrypted1 and encrypted2 and stores the
     * result in encrypted1.
     *
     * @param encrypted1 the first ciphertext to multiply.
     * @param encrypted2 the second ciphertext to multiply.
     */
    public void multiplyInplace(Ciphertext encrypted1, Ciphertext encrypted2) {
        if (!ValueChecker.isMetaDataValidFor(encrypted1, context)
            || !ValueChecker.isBufferValid(encrypted1)
        ) {
            throw new IllegalArgumentException("encrypted1 is not valid for encryption parameters");
        }

        if (!ValueChecker.isMetaDataValidFor(encrypted2, context)
            || !ValueChecker.isBufferValid(encrypted2)
        ) {
            throw new IllegalArgumentException("encrypted1 is not valid for encryption parameters");
        }

        if (!encrypted1.getParmsId().equals(encrypted2.getParmsId())) {
            throw new IllegalArgumentException("encrypted1 and encrypted2 parameter mismatch");
        }

        Context.ContextData contextData = context.firstContextData();

        switch (contextData.getParms().getScheme()) {
            case BFV:
                bfvMultiply(encrypted1, encrypted2);
                break;
            case CKKS:
                throw new IllegalArgumentException("not support CKKS");
            case BGV:
                throw new IllegalArgumentException("not support BGV");
            default:
                throw new IllegalArgumentException("unsupported scheme");
        }

        if (encrypted1.isTransparent()) {
            throw new IllegalArgumentException("result ciphertext is transparent");
        }
    }


    /**
     * BFV multiplication. This functions computes the product of encrypted1 and encrypted2 and stores the
     * result in encrypted1.
     *
     * @param encrypted1 the first ciphertext to multiply.
     * @param encrypted2 the second ciphertext to multiply.
     */
    private void bfvMultiply(Ciphertext encrypted1, Ciphertext encrypted2) {
        if (encrypted1.isNttForm() || encrypted2.isNttForm()) {
            throw new IllegalArgumentException("encrypted1 or encrypted2 cannot be in NTT form");
        }
        Context.ContextData contextData = context.getContextData(encrypted1.getParmsId());
        EncryptionParams parms = contextData.getParms();
        int coeffCount = parms.getPolyModulusDegree();
        int baseQSize = parms.getCoeffModulus().length;
        int encrypted1Size = encrypted1.getSize();
        int encrypted2Size = encrypted2.getSize();
        long plainModulus = parms.getPlainModulus().getValue();
        RnsTool rnsTool = contextData.getRnsTool();
        int baseBskSize = rnsTool.getBaseBsk().getSize();
        int baseBskMTildeSize = rnsTool.getBaseBskMTilde().getSize();
        // 每个密文包含2个多项式的情况下，乘法结果包含3个密文---> 2 + 2 - 1
        int destinationSize = Common.subSafe(Common.addSafe(encrypted1Size, encrypted2Size, false), 1, false);
        if (!Common.productFitsIn(false, destinationSize, coeffCount, baseBskMTildeSize)) {
            throw new IllegalArgumentException("invalid parameters");
        }
        // Set up iterators for bases
        Modulus[] baseQ = parms.getCoeffModulus();
        Modulus[] baseBsk = rnsTool.getBaseBsk().getBase();
        NttTables[] baseQNttTables = contextData.getSmallNttTables();
        NttTables[] baseBskNttTables = rnsTool.getBaseBskNttTables();
        // Microsoft SEAL uses BEHZ-style RNS multiplication. This process is somewhat complex and consists of the
        // following steps:
        //
        // (1) Lift encrypted1 and encrypted2 (initially in base q) to an extended base q U Bsk U {m_tilde}
        // (2) Remove extra multiples of q from the results with Montgomery reduction, switching base to q U Bsk
        // (3) Transform the data to NTT form
        // (4) Compute the ciphertext polynomial product using dyadic multiplication
        // (5) Transform the data back from NTT form
        // (6) Multiply the result by t (plain_modulus)
        // (7) Scale the result by q using a divide-and-floor algorithm, switching base to Bsk
        // (8) Use Shenoy-Kumaresan method to convert the result to base q

        // Resize encrypted1 to destination size
        encrypted1.resize(context, contextData.getParmsId(), destinationSize);
        // This lambda function takes as input an IterTuple with three components:
        //
        // 1. (Const)RNSIter to read an input polynomial from
        // 2. RNSIter for the output in base q
        // 3. RNSIter for the output in base Bsk
        //
        // It performs steps (1)-(3) of the BEHZ multiplication (see above) on the given input polynomial (given as an
        // RNSIter or ConstRNSIter) and writes the results in base q and base Bsk to the given output
        // iterators.
        // Allocate space for a base q output of behz_extend_base_convert_to_ntt for encrypted1
        long[] encrypted1Q = new long[encrypted1Size * coeffCount * baseQSize];
        // Allocate space for a base Bsk output of behz_extend_base_convert_to_ntt for encrypted1
        long[] encrypted1Bsk = new long[encrypted1Size * coeffCount * baseBskSize];
        // Perform BEHZ steps (1)-(3) for encrypted1
        for (int i = 0; i < encrypted1Size; i++) {
            System.arraycopy(
                encrypted1.getData(),
                i * coeffCount * encrypted1.getCoeffModulusSize(),
                encrypted1Q,
                i * coeffCount * baseQSize,
                coeffCount * baseQSize
            );
            NttTool.nttNegacyclicHarveyLazyPoly(encrypted1Q, encrypted1Size, coeffCount, baseQSize, i, baseQNttTables);
            // Allocate temporary space for a polynomial in the Bsk U {m_tilde} base
            // 这是一个 RnsIter
            long[] temp = new long[coeffCount * baseBskMTildeSize];
            // 1) Convert from base q to base Bsk U {m_tilde}, temp mod base Bsk U {m_tilde}
            rnsTool.fastBConvMTildeRnsIter(
                encrypted1.getData(),
                i * coeffCount * encrypted1.getCoeffModulusSize(),
                coeffCount,
                encrypted1.getCoeffModulusSize(),
                temp,
                0,
                coeffCount,
                baseBskMTildeSize
            );
            // (2) Reduce q-overflows in with Montgomery reduction, switching base to Bsk
            rnsTool.smMrqRnsIter(
                temp, 0, coeffCount, baseBskMTildeSize, encrypted1Bsk, i * coeffCount * baseBskSize, coeffCount, baseBskSize
            );
            // Transform to NTT form in base Bsk
            // Lazy reduction
            NttTool.nttNegacyclicHarveyLazyPoly(encrypted1Bsk, encrypted1Size, coeffCount, baseBskSize, i, baseBskNttTables);
        }
        // 对 encrypted2 进行同样的处理
        long[] encrypted2Q = new long[encrypted2Size * coeffCount * baseQSize];
        // Allocate space for a base Bsk output of behz_extend_base_convert_to_ntt for encrypted1
        long[] encrypted2Bsk = new long[encrypted2Size * coeffCount * baseBskSize];
        // 同样的for循环, 处理 encrypted2 相关
        for (int i = 0; i < encrypted2Size; i++) {
            System.arraycopy(
                encrypted2.getData(),
                i * coeffCount * encrypted2.getCoeffModulusSize(),
                encrypted2Q,
                i * coeffCount * baseQSize,
                coeffCount * baseQSize
            );
            NttTool.nttNegacyclicHarveyLazyPoly(encrypted2Q, encrypted2Size, coeffCount, baseQSize, i, baseQNttTables);
            // Allocate temporary space for a polynomial in the Bsk U {m_tilde} base
            long[] temp = new long[coeffCount * baseBskMTildeSize];
            // 1) Convert from base q to base Bsk U {m_tilde}
            rnsTool.fastBConvMTildeRnsIter(
                encrypted2.getData(),
                i * coeffCount * encrypted2.getCoeffModulusSize(),
                coeffCount,
                encrypted2.getCoeffModulusSize(),
                temp,
                0,
                coeffCount,
                baseBskMTildeSize
            );
            // (2) Reduce q-overflows in with Montgomery reduction, switching base to Bsk
            rnsTool.smMrqRnsIter(temp,
                0, coeffCount, baseBskMTildeSize, encrypted2Bsk, i * coeffCount * baseBskSize, coeffCount, baseBskSize
            );
            // Transform to NTT form in base Bsk
            // Lazy reduction
            NttTool.nttNegacyclicHarveyLazyPoly(encrypted2Bsk, encrypted2Size, coeffCount, baseBskSize, i, baseBskNttTables);
        }
        // Allocate temporary space for the output of step (4)
        // We allocate space separately for the base q and the base Bsk components
        // 均为 polyIter
        long[] tempDestinationQ = new long[destinationSize * coeffCount * baseQSize];
        long[] tempDestinationBsk = new long[destinationSize * coeffCount * baseBskSize];
        // Perform BEHZ step (4): dyadic multiplication on arbitrary size ciphertexts
        // todo: 尝试并行化加速
        for (int i = 0; i < destinationSize; i++) {
            // We iterate over relevant components of encrypted1 and encrypted2 in increasing order for
            // encrypted1 and reversed (decreasing) order for encrypted2. The bounds for the indices of
            // the relevant terms are obtained as follows.
            int currEncrypted1Last = Math.min(i, encrypted1Size - 1);
            int currEncrypted2First = Math.min(i, encrypted2Size - 1);
            int currEncrypted1First = i - currEncrypted2First;
            // The total number of dyadic products is now easy to compute
            int steps = currEncrypted1Last - currEncrypted1First + 1;
            // 处理 behz_ciphertext_product(encrypted1_q, encrypted2_q, base_q, base_q_size, temp_dest_q);
            // 用一个独立的代码块来处理
            {
                // 其实是一个起点的计算, 注意这里的 k 是 baseQSize
                // 作为 encrypted1_q 的起点
                int shiftedIn1Iter = currEncrypted1First * coeffCount * baseQSize;
                // 同样是起点的计算，不过是从这个起点开始逆向迭代
                // 作为 encrypted2_q 的起点
                int shiftedReversedIn2Iter = currEncrypted2First * coeffCount * baseQSize;
                int shiftedOutIter = i * coeffCount * baseQSize;
                for (int j = 0; j < steps; j++) {
                    for (int k = 0; k < baseQSize; k++) {
                        // 每次只处理一个 coeffIter
                        long[] temp = new long[coeffCount];
                        // c_1 mod q_i, c2_2 mod q_i in NTT form, compute c_1 * c_2 mod q_i
                        PolyArithmeticSmallMod.dyadicProductCoeffMod(
                            encrypted1Q,
                            shiftedIn1Iter + j * coeffCount * baseQSize + k * coeffCount,
                            encrypted2Q,
                            (shiftedReversedIn2Iter - j * coeffCount * baseQSize) + k * coeffCount,
                            coeffCount,
                            baseQ[k],
                            temp,
                            0
                        );
                        // 也只处理一个 CoeffIter
                        // 注意 shiftedOutIter 是按 k 迭代的, 迭代步长为
                        PolyArithmeticSmallMod.addPolyCoeffMod(
                            temp,
                            0,
                            tempDestinationQ,
                            shiftedOutIter + k * coeffCount,
                            coeffCount,
                            baseQ[k],
                            tempDestinationQ,
                            shiftedOutIter + k * coeffCount
                        );
                    }
                }
            }
            // behz_ciphertext_product(encrypted1_Bsk, encrypted2_Bsk, base_Bsk, base_Bsk_size, temp_dest_Bsk);
            {
                int shiftedIn1Iter = currEncrypted1First * coeffCount * baseBskSize;
                int shiftedReversedIn2Iter = currEncrypted2First * coeffCount * baseBskSize;
                int shiftedOutIter = i * coeffCount * baseBskSize;
                for (int j = 0; j < steps; j++) {
                    for (int k = 0; k < baseBskSize; k++) {
                        // 每次只处理一个 coeffIter
                        long[] temp = new long[coeffCount];
                        PolyArithmeticSmallMod.dyadicProductCoeffMod(
                            encrypted1Bsk,
                            shiftedIn1Iter + j * coeffCount * baseBskSize + k * coeffCount,
                            encrypted2Bsk,
                            (shiftedReversedIn2Iter - j * coeffCount * baseBskSize) + k * coeffCount,// 注意这里的 index 是减
                            coeffCount,
                            baseBsk[k],
                            temp,
                            0
                        );
                        // 也只处理一个 CoeffIter
                        PolyArithmeticSmallMod.addPolyCoeffMod(
                            temp,
                            0,
                            tempDestinationBsk,
                            shiftedOutIter + k * coeffCount,
                            coeffCount,
                            baseBsk[k],
                            tempDestinationBsk,
                            shiftedOutIter + k * coeffCount
                        );
                    }
                }
            }
        }
        // Perform BEHZ step (5): transform data from NTT form
        // Lazy reduction here. The following multiply_poly_scalar_coeffmod will correct the value back to [0, p)
        // 处理整个 polyIter
        NttTool.inverseNttNegacyclicHarveyLazyPoly(tempDestinationQ, destinationSize, coeffCount, baseQSize, baseQNttTables);
        NttTool.inverseNttNegacyclicHarveyLazyPoly(tempDestinationBsk, destinationSize, coeffCount, baseBskSize, baseBskNttTables);
        // Perform BEHZ steps (6)-(8)
        for (int i = 0; i < destinationSize; i++) {
            // Bring together the base q and base Bsk components into a single allocation a RnsIter
            long[] tempQBsk = new long[coeffCount * (baseQSize + baseBskSize)];
            // output ct * t in base q and bsk
            // Step (6): multiply base q components by t (plain_modulus), ct_i * t mod q
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffModRns(
                tempDestinationQ,
                i * coeffCount * baseQSize,
                coeffCount,
                baseQSize,
                plainModulus,
                baseQ,
                tempQBsk,
                0,
                coeffCount,
                baseQSize
            );
            // multiply base bsk components by t (plain_modulus), ct_i * t mod bsk
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffModRns(
                tempDestinationBsk,
                i * coeffCount * baseBskSize,
                coeffCount,
                baseBskSize,
                plainModulus,
                baseBsk,
                tempQBsk,
                baseQSize * coeffCount,
                coeffCount,
                baseBskSize
            );
            // Allocate yet another temporary for fast divide-and-floor result in base Bsk a RnsIter
            long[] tempBsk = new long[coeffCount * baseBskSize];
            // Step (7): divide by q and floor, producing a result in base Bsk
            rnsTool.fastFloorRnsIter(
                tempQBsk,
                0,
                coeffCount,
                baseQSize + baseBskSize,
                tempBsk,
                0,
                coeffCount,
                baseBskSize
            );
            // Step (8): use Shenoy-Kumaresan method to convert the result to base q and write to encrypted1
            rnsTool.fastBConvSkRnsIter(
                tempBsk,
                0,
                coeffCount,
                baseBskSize,
                encrypted1.getData(),
                i * coeffCount * encrypted1.getCoeffModulusSize(),
                encrypted1.getPolyModulusDegree(),
                encrypted1.getCoeffModulusSize()
            );
        }
    }


    public void transformToNtt(Plaintext plain, ParmsIdType parmsId, Plaintext destinationNtt) {

        destinationNtt.copyFrom(plain);
        transformToNttInplace(destinationNtt, parmsId);
    }

    public void transformToNtt(Ciphertext encrypted, Ciphertext destinationNtt) {
        destinationNtt.copyFrom(encrypted);
        transformToNttInplace(destinationNtt);
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

//        System.out.println("coeffCount: " + coeffCount);
//        System.out.println("plainCoeffCount: " + plainCoeffCount);

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
        NttTool.nttNegacyclicHarveyRns(
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
        NttTool.nttNegacyclicHarveyPoly(encrypted.getData(), encryptedSize, coeffCount, coeffModulusSize, nttTables);
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
        NttTool.inverseNttNegacyclicHarveyPoly(encryptedNtt.getData(), encryptedSize, coeffCount, coeffModulusSize, nttTables);
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
                    PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffModPoly(
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
                    PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffModPoly(
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
                PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffModPoly(
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
        NttTool.nttNegacyclicHarveyRns(
            temp,
            coeffCount,
            coeffModulusSize,
            nttTables
        );

        for (int i = 0; i < encryptedSize; i++) {
            int rnsStartIndex = i * coeffCount * coeffModulusSize;

            for (int j = 0; j < coeffModulusSize; j++) {
                // Lazy Reduction, 处理 单个 CoeffIter
                NttTool.nttNegacyclicHarveyLazyPoly(encrypted.getData(), encryptedSize, coeffCount, coeffModulusSize, i, j, nttTables);
                PolyArithmeticSmallMod.dyadicProductCoeffMod(
                    encrypted.getData(),
                    rnsStartIndex + j * coeffCount,
                    temp,
                    j * coeffCount,
                    coeffCount,
                    coeffModulus[j],
                    encrypted.getData(),
                    rnsStartIndex + j * coeffCount
                );

                NttTool.inverseNttNegacyclicHarvey(
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
        // 密文的 coeffCount 和 明文的 CoeffCount 是两码事，这个要注意
//        assert encryptedNtt.getPolyModulusDegree() == plainNtt.getCoeffCount();

//        assert encryptedNtt.getCoeffModulusSize() == plainNtt.getData().length / plainNtt.getCoeffCount();

        // 遍历每一个密文多项式
        // 这是是在处理一个 RnsIter
        for (int i = 0; i < encryptedNttSize; i++) {
            // 都是 Ntt 形式，直接调用这个函数做 多项式乘法
            PolyArithmeticSmallMod.dyadicProductCoeffModRns(
                encryptedNtt.getData(),
                encryptedNtt.indexAt(i),
                coeffCount,
                coeffModulusSize,
                plainNtt.getData(),
                0, // plain 是 single RnsIter 所以它的起点为 0
                coeffCount,
                coeffModulusSize,
                coeffModulus,
                encryptedNtt.getData(),
                encryptedNtt.indexAt(i),
                coeffCount,
                coeffModulusSize
            );
        }

        encryptedNtt.setScale(encryptedNtt.getScale() * plainNtt.scale());
        if (!isScaleWithinBounds(encryptedNtt.getScale(), contextData)) {
            throw new IllegalArgumentException("scale out of bounds");
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

    /**
     * add plaintext to ciphertext.
     *
     * @param encrypted the ciphertext to overwrite.
     * @param plain     the plaintext to add.
     */
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
                    plain, contextData, encrypted.getData(), coeffCount, encrypted.indexAt(0)
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
        PolyArithmeticSmallMod.negatePolyCoeffModPoly(
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

    /**
     * compute destination = encrypted1 + encrypted2.
     *
     * @param encrypted1  the first ciphertext to add.
     * @param encrypted2  the second ciphertext to add.
     * @param destination the ciphertext to overwrite with encrypted1 + encrypted2.
     */
    public void add(Ciphertext encrypted1, Ciphertext encrypted2, Ciphertext destination) {
        // 如果两个对象地址相同
        if (encrypted2 == destination) {
            addInplace(destination, encrypted1);
        } else {
            destination.copyFrom(encrypted1);
            addInplace(destination, encrypted2);
        }
    }

    /**
     * compute encrypted1 = \sum encrypteds.
     *
     * @param encrypteds  the ciphertexts to add.
     * @param destination the ciphertext to overwrite with \sum encrypteds.
     */
    public void addMany(Ciphertext[] encrypteds, Ciphertext destination) {
        if (encrypteds == null || encrypteds.length == 0) {
            throw new IllegalArgumentException("encrypteds cannot be empty");
        }
        for (Ciphertext encrypted : encrypteds) {
            // 不能有地址相同的密文
            if (encrypted == destination) {
                throw new IllegalArgumentException("encrypteds must be different from destination");
            }
        }
        destination.copyFrom(encrypteds[0]);
        for (int i = 1; i < encrypteds.length; i++) {
            addInplace(destination, encrypteds[i]);
        }
    }

    /**
     * compute encrypted1 = encrypted1 + encrypted2.
     *
     * @param encrypted1  the first ciphertext to add and overwrite with the encrypted1 + encrypted2.
     * @param encrypted2  the second ciphertext to add.
     */
    public void addInplace(Ciphertext encrypted1, Ciphertext encrypted2) {
        if (!ValueChecker.isMetaDataValidFor(encrypted1, context) || !ValueChecker.isBufferValid(encrypted1)) {
            throw new IllegalArgumentException("encrypted1 is not valid for encryption parameters");
        }
        if (!ValueChecker.isMetaDataValidFor(encrypted2, context) || !ValueChecker.isBufferValid(encrypted2)) {
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
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffModPoly(
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
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffModPoly(
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
            PolyArithmeticSmallMod.addPolyCoeffModPoly(
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

    /**
     * compute destination = encrypted1 - encrypted2.
     *
     * @param encrypted1  the first ciphertext to sub.
     * @param encrypted2  the second ciphertext to sub.
     * @param destination the ciphertext to overwrite with encrypted1 - encrypted2.
     */
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

    /**
     * compute encrypted1 = encrypted1 - encrypted2.
     *
     * @param encrypted1  the first ciphertext to sub and overwrite with the encrypted1 - encrypted2.
     * @param encrypted2  the second ciphertext to sub.
     */
    public void subInplace(Ciphertext encrypted1, Ciphertext encrypted2) {
        if (!ValueChecker.isMetaDataValidFor(encrypted1, context) || !ValueChecker.isBufferValid(encrypted1)) {
            throw new IllegalArgumentException("encrypted1 is not valid for encryption parameters");
        }
        if (!ValueChecker.isMetaDataValidFor(encrypted2, context) || !ValueChecker.isBufferValid(encrypted2)) {
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
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffModPoly(
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
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffModPoly(
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
            PolyArithmeticSmallMod.subPolyCoeffModPoly(
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
                    PolyArithmeticSmallMod.negatePolyCoeffModRns(
                        encrypted2.getData(),
                        encrypted2.indexAt(i),
                        encrypted2.getPolyModulusDegree(),
                        coeffModulusSize,
                        coeffModulus,
                        encrypted1.getData(),
                        encrypted1.indexAt(i),
                        encrypted1.getPolyModulusDegree(),
                        coeffModulusSize
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

    private boolean isScaleWithinBounds(double scale, Context.ContextData contextData) {

        int scaleBitCountBound = 0;
        switch (contextData.getParms().getScheme()) {

            case BFV:
            case BGV:
                scaleBitCountBound = contextData.getParms().getPlainModulus().getBitCount();
                break;
            case CKKS:
                scale = contextData.getTotalCoeffModulusBitCount();
                break;
            default:
                // Unsupported scheme; check will fail
                scaleBitCountBound = -1;
        }
        return !(scale <= 0 || (int) (Math.log(scale) / Math.log(2)) >= scaleBitCountBound);
    }


}