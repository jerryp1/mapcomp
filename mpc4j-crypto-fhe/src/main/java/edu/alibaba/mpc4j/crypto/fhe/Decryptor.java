package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.context.Context;
import edu.alibaba.mpc4j.crypto.fhe.iterator.RnsIter;
import edu.alibaba.mpc4j.crypto.fhe.keys.SecretKey;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.ntt.NttTables;
import edu.alibaba.mpc4j.crypto.fhe.ntt.NttTool;
import edu.alibaba.mpc4j.crypto.fhe.params.EncryptionParams;
import edu.alibaba.mpc4j.crypto.fhe.params.ParmsIdType;
import edu.alibaba.mpc4j.crypto.fhe.rq.PolyArithmeticSmallMod;
import edu.alibaba.mpc4j.crypto.fhe.rq.PolyCore;
import edu.alibaba.mpc4j.crypto.fhe.utils.Constants;
import edu.alibaba.mpc4j.crypto.fhe.utils.ValueChecker;
import edu.alibaba.mpc4j.crypto.fhe.zq.Common;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintCore;

import java.util.Arrays;

/**
 * @author Qixian Zhou
 * @date 2023/9/26
 */
public class Decryptor {

    private Context context;

    private int secretKeyArraySize = 0;

    private long[] secretKeyArray;


    public Decryptor(Context context, SecretKey secretKey) {

        if (!context.isParametersSet()) {
            throw new IllegalArgumentException("encryption parameters are not set correctly");
        }

        if (!ValueChecker.isValidFor(secretKey, context)) {
            throw new IllegalArgumentException("secret key is not valid for encryption parameters");
        }
        this.context = context;

        EncryptionParams parms = context.keyContextData().getParms();
        Modulus[] coeffModulus = parms.getCoeffModulus();
        int coeffCount = parms.getPolyModulusDegree();
        int coeffModulusSize = coeffModulus.length;

        // Set the secret_key_array to have size 1 (first power of secret)
        // and copy over data
        secretKeyArray = new long[coeffCount * coeffModulusSize];
        System.arraycopy(secretKey.data().getData(), 0, secretKeyArray, 0, coeffCount * coeffModulusSize);
        secretKeyArraySize = 1;

    }


    public void decrypt(Ciphertext encrypted, Plaintext destination) {

        if (!ValueChecker.isValidFor(encrypted, context)) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }
        // Additionally check that ciphertext doesn't have trivial size
        if (encrypted.getSize() < Constants.CIPHERTEXT_SIZE_MIN) {
            throw new IllegalArgumentException("encrypted is empty");
        }
        // 为何这里直接使用 first context data?
        Context.ContextData contextData = context.firstContextData();
        EncryptionParams parms = contextData.getParms();

        switch (parms.getScheme()) {
            case BFV:
                bfvDecrypt(encrypted, destination);
                return;
            case BGV:
                throw new IllegalArgumentException("now cannot support BGV");
            case CKKS:
                throw new IllegalArgumentException("now cannot support CKKS");
            default:
                throw new IllegalArgumentException("unsupported scheme");
        }
    }

    /**
     * todo: implement this function.
     *
     * @param encrypted
     * @return
     */
    public int invariantNoiseBudget(Ciphertext encrypted) {

        return 0;
    }


    private void bfvDecrypt(Ciphertext encrypted, Plaintext destination) {

        if (encrypted.isNttForm()) {
            // todo: 这是为何？密文为何不能是 NttForm?
            throw new IllegalArgumentException("encrypted cannot be in NTT form");
        }
        // 注意这里的 contextData 是由 密文的 parmsId 所决定的，也就是 加密和解密的时候 用的是同一套参数
        Context.ContextData contextData = context.getContextData(encrypted.getParmsId());
        EncryptionParams parms = contextData.getParms();
        Modulus[] coeffModulus = parms.getCoeffModulus();
        int coeffCount = parms.getPolyModulusDegree();
        int coeffModulusSize = coeffModulus.length;

        // Firstly find c_0 + c_1 *s + ... + c_{count-1} * s^{count-1} mod q
        // This is equal to Delta m + v where ||v|| < Delta/2.
        // Add Delta / 2 and now we have something which is Delta * (m + epsilon) where epsilon < 1
        // Therefore, we can (integer) divide by Delta and the answer will round down to m.

        // Make a temp destination for all the arithmetic mod qi before calling FastBConverse
        long[] tempDestModQ = new long[coeffCount * coeffModulusSize];

        // put < (c_1 , c_2, ... , c_{count-1}) , (s,s^2,...,s^{count-1}) > mod q in destination
        // Now do the dot product of encrypted_copy and the secret key array using NTT.
        // The secret key powers are already NTT transformed.
        dotProductCtSkArray(encrypted, tempDestModQ);

        // Allocate a full size destination to write to
        destination.setParmsId(ParmsIdType.parmsIdZero());
        destination.resize(coeffCount);

        // Divide scaling variant using BEHZ FullRNS techniques
//        RnsIter rnsIter = new RnsIter(tempDestModQ, coeffCount);
//        contextData.getRnsTool().decryptModT(rnsIter, destination.getData());
        contextData.getRnsTool().decryptScaleAndRound(tempDestModQ, coeffCount, destination.getData());


        // How many non-zero coefficients do we really have in the result?
        // 总共 N 个 count, 把高位 是0的count 去掉
        int plainCoeffCout = UintCore.getSignificantUint64CountUint(destination.getData(), coeffCount);

        // Resize destination to appropriate size
        // 至少也要有1个count
        destination.resize(Math.max(plainCoeffCout, 1));
    }

    /**
     * @param encrypted
     * @param destination 表示一个 rnsIter
     */
    private void dotProductCtSkArray(Ciphertext encrypted, long[] destination) {

        Context.ContextData contextData = context.getContextData(encrypted.getParmsId());
        EncryptionParams parms = contextData.getParms();
        Modulus[] coeffModulus = parms.getCoeffModulus();
        int coeffCount = parms.getPolyModulusDegree();
        int coeffModulusSize = coeffModulus.length;
        int keyCoeffModulusSize = context.keyContextData().getParms().getCoeffModulus().length;
        int encryptedSize = encrypted.getSize();
        boolean isNttForm = encrypted.isNttForm();

        NttTables[] nttTables = contextData.getSmallNttTables();

        // Make sure we have enough secret key powers computed
        computeSecretKeyArray(encryptedSize - 1);

        if (encryptedSize == 2) {
            // 密文中 多项式数量是2的情况下，secretKeyArray 中 只包含一个 sk, 可以理解为 就是一个 RnsIter

            // 提取当前 Ciphertext 中的 c0 c1
            int c0StartIndex = 0;
            int c1StartIndex = encrypted.indexAt(1);

            //   现在可以认为 sk c0 c1 都是 RnsIter, 然后在 RnsIter 下进行计算
            if (isNttForm) {
                //todo: 考虑并行化提速
                for (int i = 0; i < coeffModulusSize; i++) {
                    // 再分解了一层，现在处理的是 coeffIter

                    //put < c_1 * s > mod q in destination
                    PolyArithmeticSmallMod.dyadicProductCoeffModCoeffIter(
                            encrypted.getData(),
                            c1StartIndex + i * coeffCount,
                            secretKeyArray,
                            i * coeffCount,
                            coeffCount,
                            coeffModulus[i],
                            i * coeffCount,
                            destination
                    );

                    // add c_0 to the result; note that destination should be in the same (NTT) form as encrypted
                    // 前参数1、2表示 c_1 * s, 参数 3、4表示 c0,
                    PolyArithmeticSmallMod.addPolyCoeffMod(
                            destination,
                            i * coeffCount,
                            encrypted.getData(),
                            c0StartIndex + i * coeffCount,
                            coeffCount,
                            coeffModulus[i],
                            i * coeffCount,
                            destination
                    );
                }
            } else { // 处理 non-NTT 下的计算
                // 同样是分解为 CoeffIter 层面的计算
                for (int i = 0; i < coeffModulusSize; i++) {
                    // 拷贝 c1 给 Destination
                    // 注意各自起点的计算
                    // todo: need setUint? 还是直接 copy 即可?
                    System.arraycopy(encrypted.getData(), c1StartIndex + i * coeffCount, destination, i * coeffCount, coeffCount);
                    // transform c1 to Ntt form
                    NttTool.nttNegAcyclicHarveyLazy(destination, i * coeffCount, nttTables[i]);
                    // put < c_1 * s > mod q in destination
                    PolyArithmeticSmallMod.dyadicProductCoeffModCoeffIter(
                            destination,
                            i * coeffCount,
                            secretKeyArray,
                            i * coeffCount, // 因为可以确定只有1个密文，所以可以当做 RnsIter 来使用
                            coeffCount,
                            coeffModulus[i],
                            i * coeffCount,
                            destination
                    );
                    // transform back
                    NttTool.inverseNttNegAcyclicHarvey(
                            destination,
                            i * coeffCount,
                            nttTables[i]
                    );
                    // add c0 to the result; note that destination should be in the same (NTT) form as encrypted
                    // 密文是 ntt，destination 就是 Ntt, 密文是非 Ntt，destination 就是 non-ntt
                    PolyArithmeticSmallMod.addPolyCoeffMod(
                            destination,
                            i * coeffCount,
                            encrypted.getData(),
                            c0StartIndex + i * coeffCount,
                            coeffCount,
                            coeffModulus[i],
                            i * coeffCount,
                            destination
                    );
                }
            }

        } else { // 密文中 多项式数量 > 2

            // put < (c_1 , c_2, ... , c_{count-1}) , (s,s^2,...,s^{count-1}) > mod q in destination
            // Now do the dot product of encrypted_copy and the secret key array using NTT.
            // The secret key powers are already NTT transformed.

            // 拷贝 (c_1 , c_2, ... , c_{count-1})
            // encrypted 是一个 polyIter
            long[] encryptedCopy = new long[(encryptedSize - 1) * coeffCount * coeffModulusSize];
            // todo: need setPolyArray？ 还是直接调用 System.arraycopy 即可？
            System.arraycopy(encrypted.getData(), encrypted.indexAt(1), encryptedCopy, 0, encryptedCopy.length);

            // Transform c_1, c_2, ... to NTT form unless they already are
            if (!isNttForm) {
                // 这里需要对一个 polyIter 做处理，这里拆成 long[] + startIndex 来表示一个 RnsIter
                // 这里需要遍历 polyIter 中的每一个 RnsIter, 以达到处理整个 PolyIter 的效果
                // todo: 尝试并行化加速
                for (int i = 0; i < (encryptedSize - 1); i++) {
                    // 这是 RnsIter 层面的计算
                    NttTool.nttNegAcyclicHarvey(encryptedCopy, i * coeffCount * coeffModulusSize, encryptedSize - 1, nttTables);
                }
            }

            // Compute dyadic product with secret power array
            // c1 * s, c2 * s^2 ...
            // encryptedCopy 和 secretKeyArray 可以理解为 PolyIter，这里是拆到最低粒度 CoeffIter 进行了处理
            // todo: 是否需要以 RnsIter 为单位进行处理？
            for (int i = 0; i < (encryptedSize - 1); i++) {
                // 处理 单个 RnsIter
                int rnsIterStartIndex = i * coeffCount * coeffModulusSize;
                for (int j = 0; j < coeffModulusSize; j++) {
                    // 处理单个 CoeffIter
                    PolyArithmeticSmallMod.dyadicProductCoeffMod(
                            encryptedCopy,
                            rnsIterStartIndex + j * coeffCount,
                            secretKeyArray,
                            rnsIterStartIndex + j * coeffCount,
                            coeffCount,
                            coeffModulus[j],
                            rnsIterStartIndex + j * coeffCount,
                            encryptedCopy
                    );
                }
            }

            // Aggregate all polynomials together to complete the dot product
            Arrays.fill(destination, 0, coeffCount * coeffModulusSize, 0);
            // encrypted 的结果累加到 destination 中
            // PolyIter + RnsIter
            for (int i = 0; i < (encryptedSize - 1); i++) {
                // 处理 单个 RnsIter
                int rnsIterStartIndex = i * coeffCount * coeffModulusSize;
                for (int j = 0; j < coeffModulusSize; j++) {
                    // 处理单个 CoeffIter, 注意二者的起点不一样
                    PolyArithmeticSmallMod.addPolyCoeffMod(
                            destination,
                            j * coeffCount,
                            encryptedCopy,
                            rnsIterStartIndex + j * coeffCount,
                            coeffCount,
                            coeffModulus[j],
                            j * coeffCount,
                            destination
                    );
                }
            }

            if (!isNttForm) {
                // If the input was not in NTT form, need to transform back
                // 逐 CoeffIter 处理
                for (int i = 0; i < coeffModulusSize; i++) {
                    NttTool.inverseNttNegAcyclicHarvey(
                            destination,
                            i * coeffCount,
                            nttTables[i]
                    );
                }
            }
            // Finally add c_0 to the result; note that destination should be in the same (NTT) form as encrypted
            // c0 和 destination 都是 RnsIter, 拆解为 CoeffIter 处理

            for (int i = 0; i < coeffModulusSize; i++) {
                PolyArithmeticSmallMod.addPolyCoeffMod(
                        destination,
                        i * coeffCount,
                        encrypted.getData(),
                        i * coeffCount, // c0StartIndex = 0
                        coeffCount,
                        coeffModulus[i],
                        i * coeffCount,
                        destination
                );
            }
        }
    }


    /**
     * Compute c_0 + c_1 *s + ... + c_{count-1} * s^{count-1} mod q.
     * Store result in destination in RNS form.
     * destination has the size of an RNS polynomial.
     *
     * @param encrypted
     * @param destination
     */
    private void dotProductCtSkArray(Ciphertext encrypted, RnsIter destination) {

        Context.ContextData contextData = context.getContextData(encrypted.getParmsId());
        EncryptionParams parms = contextData.getParms();
        Modulus[] coeffModulus = parms.getCoeffModulus();
        int coeffCount = parms.getPolyModulusDegree();
        int coeffModulusSize = coeffModulus.length;
        int keyCoeffModulusSize = context.keyContextData().getParms().getCoeffModulus().length;
        int encryptedSize = encrypted.getSize();
        boolean isNttForm = encrypted.isNttForm();

        NttTables[] nttTables = contextData.getSmallNttTables();

        // Make sure we have enough secret key powers computed
        computeSecretKeyArray(encryptedSize - 1);

        if (encryptedSize == 2) {
            // 密文中 多项式数量是2的情况下，secretKeyArray 中 只包含一个 sk, 可以理解为 就是一个 RnsIter

            // 提取当前 Ciphertext 中的 c0 c1
            int c0StartIndex = 0;
            int c1StartIndex = encrypted.indexAt(1);

            //   现在可以认为 sk c0 c1 都是 RnsIter, 然后在 RnsIter 下进行计算
            if (isNttForm) {
                //todo: 考虑并行化提速
                for (int i = 0; i < coeffModulusSize; i++) {
                    // 再分解了一层，现在处理的是 coeffIter

                    //put < c_1 * s > mod q in destination
                    PolyArithmeticSmallMod.dyadicProductCoeffModCoeffIter(
                            encrypted.getData(),
                            c1StartIndex + i * coeffCount,
                            secretKeyArray,
                            i * coeffCount,
                            coeffCount,
                            coeffModulus[i],
                            i * coeffCount,
                            destination.coeffIter
                    );

                    // add c_0 to the result; note that destination should be in the same (NTT) form as encrypted
                    // 前参数1、2表示 c_1 * s, 参数 3、4表示 c0,
                    PolyArithmeticSmallMod.addPolyCoeffMod(
                            destination.coeffIter,
                            i * coeffCount,
                            encrypted.getData(),
                            c0StartIndex + i * coeffCount,
                            coeffCount,
                            coeffModulus[i],
                            i * coeffCount,
                            destination.coeffIter
                    );
                }
            } else { // 处理 non-NTT 下的计算
                // 同样是分解为 CoeffIter 层面的计算
                for (int i = 0; i < coeffModulusSize; i++) {
                    // 拷贝 c1 给 Destination
                    // 注意各自起点的计算
                    // todo: need setUint? 还是直接 copy 即可?

                    System.arraycopy(encrypted.getData(), c1StartIndex + i * coeffCount, destination.coeffIter, i * coeffCount, coeffCount);
                    // transform c1 to Ntt form
                    NttTool.nttNegAcyclicHarveyLazy(destination.coeffIter, i * coeffCount, nttTables[i]);
                    // put < c_1 * s > mod q in destination
                    PolyArithmeticSmallMod.dyadicProductCoeffModCoeffIter(
                            destination.coeffIter,
                            i * coeffCount,
                            secretKeyArray,
                            i * coeffCount,
                            coeffCount,
                            coeffModulus[i],
                            i * coeffCount,
                            destination.coeffIter
                    );
                    // transform back
                    NttTool.inverseNttNegAcyclicHarvey(
                            destination.coeffIter,
                            i * coeffCount,
                            nttTables[i]
                    );
                    // add c0 to the result; note that destination should be in the same (NTT) form as encrypted
                    // 密文是 ntt，destination 就是 Ntt, 密文是非 Ntt，destination 就是 non-ntt
                    PolyArithmeticSmallMod.addPolyCoeffMod(
                            destination.coeffIter,
                            i * coeffCount,
                            encrypted.getData(),
                            c0StartIndex + i * coeffCount,
                            coeffCount,
                            coeffModulus[i],
                            i * coeffCount,
                            destination.coeffIter
                    );
                }
            }

        } else { // 密文中 多项式数量 > 2

            // put < (c_1 , c_2, ... , c_{count-1}) , (s,s^2,...,s^{count-1}) > mod q in destination
            // Now do the dot product of encrypted_copy and the secret key array using NTT.
            // The secret key powers are already NTT transformed.

            // 拷贝 (c_1 , c_2, ... , c_{count-1})
            // encrypted 是一个 polyIter
            long[] encryptedCopy = new long[(encryptedSize - 1) * coeffCount * coeffModulusSize];
            // todo: need setPolyArray？ 还是直接调用 System.arraycopy 即可？
            System.arraycopy(encrypted.getData(), encrypted.indexAt(1), encryptedCopy, 0, encryptedCopy.length);

            // Transform c_1, c_2, ... to NTT form unless they already are
            if (!isNttForm) {
                // 这里需要对一个 polyIter 做处理，这里拆成 long[] + startIndex 来表示一个 RnsIter
                // 这里需要遍历 polyIter 中的每一个 RnsIter, 以达到处理整个 PolyIter 的效果
                // todo: 尝试并行化加速
                for (int i = 0; i < (encryptedSize - 1); i++) {
                    // 这是 RnsIter 层面的计算
                    NttTool.nttNegAcyclicHarvey(encryptedCopy, i * coeffCount * coeffModulusSize, encryptedSize - 1, nttTables);
                }
            }

            // Compute dyadic product with secret power array
            // c1 * s, c2 * s^2 ...
            // encryptedCopy 和 secretKeyArray 可以理解为 PolyIter，这里是拆到最低粒度 CoeffIter 进行了处理
            // todo: 是否需要以 RnsIter 为单位进行处理？
            // todo: keyCoeffModulusSize 和 coeffModulusSize? 为何在 SEAL 里，secret_key_array 用的是 keyCoeffModulusSize
            for (int i = 0; i < (encryptedSize - 1); i++) {
                // 处理 单个 RnsIter
                int rnsIterStartIndex = i * coeffCount * coeffModulusSize;
                for (int j = 0; j < coeffModulusSize; j++) {
                    // 处理单个 CoeffIter
                    PolyArithmeticSmallMod.dyadicProductCoeffMod(
                            encryptedCopy,
                            rnsIterStartIndex + j * coeffCount,
                            secretKeyArray,
                            rnsIterStartIndex + j * coeffCount,
                            coeffCount,
                            coeffModulus[j],
                            rnsIterStartIndex + j * coeffCount,
                            encryptedCopy
                    );
                }
            }

            // Aggregate all polynomials together to complete the dot product
            Arrays.fill(destination.coeffIter, 0, coeffCount * coeffModulusSize, 0);
            // encrypted 的结果累加到 destination 中
            // PolyIter + RnsIter
            for (int i = 0; i < (encryptedSize - 1); i++) {
                // 处理 单个 RnsIter
                int rnsIterStartIndex = i * coeffCount * coeffModulusSize;
                for (int j = 0; j < coeffModulusSize; j++) {
                    // 处理单个 CoeffIter, 注意二者的起点不一样
                    PolyArithmeticSmallMod.addPolyCoeffMod(
                            destination.coeffIter,
                            j * coeffCount,
                            encryptedCopy,
                            rnsIterStartIndex + j * coeffCount,
                            coeffCount,
                            coeffModulus[j],
                            j * coeffCount,
                            destination.coeffIter
                    );
                }
            }

            if (!isNttForm) {
                // If the input was not in NTT form, need to transform back
                // 逐 CoeffIter 处理
                for (int i = 0; i < coeffModulusSize; i++) {
                    NttTool.inverseNttNegAcyclicHarvey(
                            destination.coeffIter,
                            i * coeffCount,
                            nttTables[i]
                    );
                }
            }
            // Finally add c_0 to the result; note that destination should be in the same (NTT) form as encrypted
            // c0 和 destination 都是 RnsIter, 拆解为 CoeffIter 处理

            for (int i = 0; i < coeffModulusSize; i++) {
                PolyArithmeticSmallMod.addPolyCoeffMod(
                        destination.coeffIter,
                        i * coeffCount,
                        encrypted.getData(),
                        i * coeffCount, // c0StartIndex = 0
                        coeffCount,
                        coeffModulus[i],
                        i * coeffCount,
                        destination.coeffIter
                );
            }
        }
    }


    /**
     * 和 KeyGenerator 中 computeSecretKeyArray 完全相同的逻辑
     *
     * @param maxPower
     */
    private void computeSecretKeyArray(int maxPower) {

        assert maxPower >= 1;
        assert !(secretKeyArraySize == 0 || secretKeyArray == null);

        // WARNING: This function must be called with the original context_data

        Context.ContextData contextData = context.keyContextData();
        EncryptionParams parms = contextData.getParms();
        Modulus[] coeffModulus = parms.getCoeffModulus();
        int coeffCount = parms.getPolyModulusDegree();
        int coeffModulusSize = coeffModulus.length;

        int oldSize = secretKeyArraySize;
        int newSize = Math.max(oldSize, maxPower);
        if (oldSize == newSize) {
            return;
        }
        // Need to extend the array
        // Compute powers of secret key until max_power

        // 分配足够大的长度
        long[] newSecretKeyArray = new long[newSize * coeffCount * coeffModulusSize];
        // 先拷贝 old this.secretKeyArray 中的数据
        System.arraycopy(this.secretKeyArray, 0, newSecretKeyArray, 0, oldSize * coeffCount * coeffModulusSize);

        // Since all of the key powers in secret_key_array_ are already NTT transformed, to get the next one we simply
        // need to compute a dyadic product of the last one with the first one [which is equal to NTT(secret_key_)].
        // 计算 sk^1 sk^2 sk^3 ....
        // 计算逻辑是 sk^2 = sk * sk,  sk^3 = sk^2 * sk

        // 注意 i 的起点
        // secretKeyArray: [0, 1 * k * N), [1*k*N, 2*k*N), ..., [(oldSize - 1) * k * N , oldSize * k * N)
        // newSecretKeyArray: [0, 1 * k * N), [1*k*N, 2*k*N), ..., [(oldSize - 1) * k * N , oldSize * k * N),...,[(newSize - 1) * k * N , newSize * k * N)
        // 现在的计算逻辑是这样：newSecretKeyArray[ oldSize * k * N , (oldSize + 1) * k * N ) = newSecretKeyArray[(oldSize - 1) * k * N , oldSize * k * N) *  secretKeyArray[(oldSize - 1) * k * N , oldSize * k * N)
        // 注意 第二项的起点 是不变的

        int oldStartIndex = (oldSize - 1) * coeffCount * coeffModulusSize;
        // 注意到这里是没办法并发的，后一个计算结果 依赖于 前一个计算结果
        for (int i = oldSize - 1; i < (oldSize - 1) + newSize - oldSize; i++) {

            int newStartIndex = i * coeffCount * coeffModulusSize;
            int newStartIndexPlusOne = (i + 1) * coeffCount * coeffModulusSize;

            PolyArithmeticSmallMod.dyadicProductCoeffModRnsIter(
                    newSecretKeyArray,
                    newStartIndex,
                    secretKeyArray,
                    oldStartIndex,
                    coeffModulusSize,
                    coeffCount,
                    coeffModulus,
                    newStartIndexPlusOne,
                    newSecretKeyArray
            );
        }


        // todo: Do we still need to update size?
//        oldSize = secretKeyArraySize;
//        newSize = Math.max(oldSize, maxPower);
//        if (oldSize == newSize) {
//            return;
//        }
        // update size and array
        secretKeyArraySize = newSize;
        secretKeyArray = newSecretKeyArray;

    }


}
