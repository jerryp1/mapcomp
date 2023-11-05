package edu.alibaba.mpc4j.s2pc.pir.index.single.seal4jpir;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.crypto.fhe.*;
import edu.alibaba.mpc4j.crypto.fhe.context.Context;
import edu.alibaba.mpc4j.crypto.fhe.keys.GaloisKeys;
import edu.alibaba.mpc4j.crypto.fhe.keys.PublicKey;
import edu.alibaba.mpc4j.crypto.fhe.keys.SecretKey;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.params.EncryptionParams;
import edu.alibaba.mpc4j.crypto.fhe.params.ParmsIdType;
import edu.alibaba.mpc4j.crypto.fhe.params.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.rq.PolyArithmeticSmallMod;
import edu.alibaba.mpc4j.crypto.fhe.utils.SerializationUtils;
import edu.alibaba.mpc4j.crypto.fhe.zq.Numth;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintArithmetic;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintCore;

/**
 * @author Qixian Zhou
 * @date 2023/10/13
 */
public class Acls18SingleIndexPirPureJavaUtils {


    private Acls18SingleIndexPirPureJavaUtils() {
        // empty
    }


    public static List<Plaintext> deserializePlaintextsFromCoeffWithoutBatchEncode(List<long[]> coeffList, Context context) {

        int size = coeffList.size();
        List<Plaintext> plaintexts = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Plaintext plaintext = new Plaintext(context.firstContextData().getParms().getPolyModulusDegree());
            long[] coeffs = coeffList.get(i);
            // just copy
            int len = coeffs.length;
            System.arraycopy(
                    coeffs,
                    0,
                    plaintext.getData(),
                    0,
                    len
            );
            plaintexts.add(plaintext);
        }
        return plaintexts;
    }


    /**
     * generate encryption params.
     *
     * @param polyModulusDegree poly modulus degree.
     * @param plainModulus      plain modulus.
     * @return encryption params.
     */
    public static EncryptionParams generateEncryptionParams(int polyModulusDegree, long plainModulus) {

        EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
        parms.setPolyModulusDegree(polyModulusDegree);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.BfvDefault(polyModulusDegree, CoeffModulus.SecurityLevelType.TC128));

        return parms;
    }


    public static void nttTransformInplace(Context context, List<Plaintext> plaintextList) {
        Evaluator evaluator = new Evaluator(context);
        for (Plaintext plaintext : plaintextList) {
            // NTT Form 的 Plaintext 的 CoeffCount 会发生变化
            evaluator.transformToNttInplace(plaintext, context.getFirstParmsId());
        }
    }


    public static List<byte[]> generateQuery(Context context,
                                             EncryptionParams parms,
                                             PublicKey publicKey,
                                             SecretKey secretKey,
                                             int[] indices,
                                             int[] nvec) {

        Encryptor encryptor = new Encryptor(context, publicKey, secretKey);
        int dimension = indices.length;
        int size = nvec.length;
        assert dimension == size;

        int coeffCount = parms.getPolyModulusDegree();
        Plaintext pt = new Plaintext(coeffCount);
        List<Ciphertext> result = new ArrayList<>();

        for (int i = 0; i < indices.length; i++) {
            int numPtxts = (int) Math.ceil((double) nvec[i] / coeffCount);
            for (int j = 0; j < numPtxts; j++) {
                pt.setZero();

                if (indices[i] >= coeffCount * j && indices[j] <= coeffCount * (j + 1)) {

                    int realIndex = indices[i] - coeffCount * j;
                    int nI = nvec[i];
                    int total = coeffCount;
                    if (j == numPtxts - 1) {
                        total = nI % coeffCount;
                        if (total == 0) {
                            total = coeffCount;
                        }
                    }

                    long logTotal = (long) Math.ceil(DoubleUtils.log2((double) total));
                    long[] temp = new long[1];
                    Numth.tryInvertUintMod(
                            1L << logTotal,
                            parms.getPlainModulus().getValue(),
                            temp);
                    pt.set(realIndex, temp[0]);
                }
                result.add(encryptor.encryptSymmetric(pt));
            }
        }

        List<byte[]> resultBytes = new ArrayList<>();
        for (Ciphertext c : result) {
            resultBytes.add(SerializationUtils.serializeObject(c));
        }
        return resultBytes;
    }

    /**
     * generate response.
     *
     * @return response ciphertexts。
     */
    public static List<byte[]> generateReply(
            Context context,
            EncryptionParams parms,
            GaloisKeys galoisKey,
            List<byte[]> queryListPayload,
            List<Plaintext> database, int[] nvec) {

        Evaluator evaluator = new Evaluator(context);

        List<Ciphertext> queryList = new ArrayList<>(queryListPayload.size());
        for (byte[] bytes : queryListPayload) {
            queryList.add(SerializationUtils.deserializeObject(bytes));
        }


        int d = nvec.length;
        List<List<Ciphertext>> query = new ArrayList<>(d);
        for (int i = 0; i < d; i++) {
            query.add(new ArrayList<>());
        }

        int coeffCount = parms.getPolyModulusDegree();
        int index = 0;
        for (int i = 0; i < d; i++) {
            int numPtxts = (int) Math.ceil((double) nvec[i] / coeffCount);
            for (int j = 0; j < numPtxts; j++) {
                query.get(i).add(queryList.get(index++));
            }
        }

        int product = 1;
        for (int i : nvec) {
            product *= i;
        }

        List<Plaintext> cur = database;
        List<Plaintext> intermediatePlain = new ArrayList<>();
        int expansionRatio = computeExpansionRatio(parms);
        for (int i = 0; i < nvec.length; i++) {
            List<Ciphertext> expandedQuery = new ArrayList<>();

            for (int j = 0; j < query.get(i).size(); j++) {
                long total = coeffCount;
                if (j == query.get(i).size() - 1) {
                    total = nvec[i] % coeffCount;
                    if (total == 0) {
                        total = coeffCount;
                    }
                }

                List<Ciphertext> expandedQueryPart = expandQuery(context, parms, query.get(i).get(j), galoisKey, (int) total);
                expandedQuery.addAll(expandedQueryPart);
                expandedQueryPart.clear();
            }

            assert expandedQuery.size() == nvec[i] : "expandedQuery.size(): " + expandedQuery.size() + ", nvec[" + i + "]: " + nvec[i];

            for (Ciphertext jj : expandedQuery) {
                evaluator.transformToNttInplace(jj);
            }

            if (i > 0) {
                for (Plaintext jj : cur) {
                    evaluator.transformToNttInplace(jj, context.getFirstParmsId());
                }
            }

            product /= nvec[i];
            List<Ciphertext> intermediateCtxts = IntStream.range(0, product).mapToObj(ii -> new Ciphertext()).collect(Collectors.toList());
            Ciphertext temp = new Ciphertext();

            for (int k = 0; k < product; k++) {
                evaluator.multiplyPlain(expandedQuery.get(0), cur.get(k), intermediateCtxts.get(k));
                for (int j = 1; j < nvec[i]; j++) {
                    evaluator.multiplyPlain(expandedQuery.get(j), cur.get(k + j * product), temp);
                    evaluator.addInplace(intermediateCtxts.get(k), temp);
                }
            }

            for (Ciphertext intermediateCtxt : intermediateCtxts) {
                evaluator.transformFromNttInplace(intermediateCtxt);
            }

            if (i == nvec.length - 1) {
                // 序列化密文 并返回
                List<byte[]> resultBytes = new ArrayList<>(intermediateCtxts.size());
                for (Ciphertext ctxt : intermediateCtxts) {
                    resultBytes.add(SerializationUtils.serializeObject(ctxt));
                }
                return resultBytes;
            } else {

                intermediatePlain.clear();
                intermediatePlain = new ArrayList<>(expansionRatio * product);

                cur = intermediatePlain;

                for (int rr = 0; rr < product; rr++) {
                    evaluator.modSwitchToInplace(intermediateCtxts.get(rr), context.getLastParmsId());
                    List<Plaintext> plains = decomposeToPlaintexts(context.lastContextData().getParms(), intermediateCtxts.get(rr));
                    intermediatePlain.addAll(plains);
                }
                product = intermediatePlain.size();
            }
        }
        throw new RuntimeException("generate response failed");
    }


    public static List<Plaintext> decomposeToPlaintexts(EncryptionParams parms, Ciphertext ct) {

        int ptBitsPerCoeff = (int) DoubleUtils.log2(parms.getPlainModulus().getValue());

        int coeffCount = parms.getPolyModulusDegree();
        int coeffModCount = parms.getCoeffModulus().length;
        long ptBitMask = (1L << ptBitsPerCoeff) - 1;

        List<Plaintext> result = IntStream.range(0, computeExpansionRatio(parms) * ct.getSize()).mapToObj(
                e -> new Plaintext()).collect(Collectors.toList());
        int ptIter = 0;

        for (int polyIndex = 0; polyIndex < ct.getSize(); polyIndex++) {
            for (int coeffModIndex = 0; coeffModIndex < coeffModCount; coeffModIndex++) {

                double coeffBitSize = DoubleUtils.log2(parms.getCoeffModulus()[coeffModIndex].getValue());
                // 一个密文系数 可以 扩展为 这么多个明文
                int localExpansionRatio = (int) Math.ceil(coeffBitSize / ptBitsPerCoeff);
                int shift = 0;
                for (int i = 0; i < localExpansionRatio; i++) {
                    result.get(ptIter).resize(coeffCount);
                    for (int c = 0; c < coeffCount; c++) {
                        result.get(ptIter).getData()[c] =
                                (ct.getData()
                                        [ct.indexAt(polyIndex) + coeffModIndex * coeffCount + c] >>> shift) & ptBitMask;

                    }
                    ++ptIter;
                    shift += ptBitsPerCoeff;
                }
            }
        }
        return result;
    }


    public static List<Ciphertext> expandQuery(
            Context context,
            EncryptionParams parms,
            Ciphertext encrypted,
            GaloisKeys galoisKeys,
            int m
    ) {


        Evaluator evaluator = new Evaluator(context);

        int logm = (int) Math.ceil(DoubleUtils.log2(m));
        Plaintext two = new Plaintext("2");
        int n = parms.getPolyModulusDegree();
        int logn = (int) Math.ceil(DoubleUtils.log2(parms.getPolyModulusDegree()));
        assert logm <= logn;

        int[] galoisElts = new int[logn];
        for (int i = 0; i < logn; i++) {
            galoisElts[i] = (int) (
                    (n + UintArithmetic.exponentUint(2L, i))
                            / UintArithmetic.exponentUint(2L, i));
        }

        List<Ciphertext> temp = new ArrayList<>();
        temp.add(encrypted);
//        Ciphertext tempCtxt = new Ciphertext();
        Ciphertext tempCtxtRotated = new Ciphertext();
        Ciphertext tempCtxtShifted = new Ciphertext();
        Ciphertext tempCtxtRotatedShifted = new Ciphertext();

        for (int i = 0; i < logm - 1; i++) {
            List<Ciphertext> newTemp = new ArrayList<>(temp.size() << 1);
            for (int j = 0; j < temp.size() << 1; j++) {
                newTemp.add(new Ciphertext());
            }

            int indexRaw = (n << 1) - (1 << i);
            int index = (indexRaw * galoisElts[i]) % (n << 1);
            for (int a = 0; a < temp.size(); a++) {
                evaluator.applyGalois(
                        temp.get(a),
                        galoisElts[i],
                        galoisKeys,
                        tempCtxtRotated
                );

                evaluator.add(temp.get(a), tempCtxtRotated, newTemp.get(a));
                // debug 下 assert 无法通过，因为 indexRaw > coeffCount
                multiplyPowerOfX(temp.get(a), tempCtxtShifted, indexRaw, context);

                multiplyPowerOfX(tempCtxtRotated, tempCtxtRotatedShifted, index, context);

                evaluator.add(
                        tempCtxtShifted,
                        tempCtxtRotatedShifted,
                        newTemp.get(a + temp.size())
                );
            }
            temp = new ArrayList<>(newTemp);
        }
        // Last step of the loop
        List<Ciphertext> newTemp = IntStream.range(0, temp.size() << 1).mapToObj(
                idx -> new Ciphertext()
        ).collect(Collectors.toList());

        int indexRaw = (n << 1) - (1 << (logm - 1));
        int index = (indexRaw * galoisElts[logm - 1]) % (n << 1);

        for (int a = 0; a < temp.size(); a++) {

            if (a >= (m - (1 << (logm - 1)))) {
                evaluator.multiplyPlain(
                        temp.get(a),
                        two,
                        newTemp.get(a)
                );
            } else {

                evaluator.applyGalois(
                        temp.get(a),
                        galoisElts[logm - 1],
                        galoisKeys,
                        tempCtxtRotated
                );

                evaluator.add(temp.get(a), tempCtxtRotated, newTemp.get(a));

                multiplyPowerOfX(temp.get(a), tempCtxtShifted, indexRaw, context);

                multiplyPowerOfX(tempCtxtRotated, tempCtxtRotatedShifted, index, context);

                evaluator.add(
                        tempCtxtShifted,
                        tempCtxtRotatedShifted,
                        newTemp.get(a + temp.size())
                );
            }
        }

        // auto last = newtemp.begin() + m; 这是获取容器里第 m 个元素，index = m - 1
        // vector<Ciphertext> newVec(first, last); 这是指的是从  [first, last] 这个区间的元素作为 vector
        // 返回[0, m) 的元素
        return newTemp.subList(0, m);
    }


    public static void multiplyPowerOfX(Ciphertext encrypted, Ciphertext destination, int index, Context context) {

        Context.ContextData contextData = context.firstContextData();
        EncryptionParams parms = contextData.getParms();
        int coeffModCount = parms.getCoeffModulus().length;
        int coeffCount = parms.getPolyModulusDegree();
        int encryptedCount = encrypted.getSize();
        destination.copyFrom(encrypted);

        for (int i = 0; i < encryptedCount; i++) {
            // 依次处理每一个 CoeffIter
            for (int j = 0; j < coeffModCount; j++) {
                PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(
                        encrypted.getData(),
                        encrypted.indexAt(i) + j * coeffCount,
                        coeffCount,
                        index,
                        parms.getCoeffModulus()[j],
                        destination.getData(),
                        destination.indexAt(i) + j * coeffCount
                );
            }
        }
    }


    /**
     * decode response.
     *
     * @param secretKey secret key.
     * @param response  response ciphertext.
     * @param dimension dimension.
     * @return BFV plaintext.
     */
    public static long[] decryptReply(Context context, SecretKey secretKey, List<byte[]> response, int dimension) {

        EncryptionParams parms = context.lastContextData().getParms();
        ParmsIdType parmsId = context.getLastParmsId();
        int expRation = computeExpansionRatio(parms);
        int recursionLevel = dimension;

        Decryptor decryptor = new Decryptor(context, secretKey);

        // 反序列化
        List<Ciphertext> temp = new ArrayList<>(response.size());
        for (byte[] bytes : response) {
            temp.add(SerializationUtils.deserializeObject(bytes));
        }
        int ciphertextSize = temp.get(0).getSize();

        for (int i = 0; i < recursionLevel; i++) {
            List<Ciphertext> newTemp = new ArrayList<>();
            List<Plaintext> tempPlain = new ArrayList<>();

            for (int j = 0; j < temp.size(); j++) {
                Plaintext ptxt = new Plaintext();
                decryptor.decrypt(temp.get(j), ptxt);
                tempPlain.add(ptxt);
                if ((j + 1) % (expRation * ciphertextSize) == 0 && j > 0) {
                    // Combine into one ciphertext.
                    Ciphertext combined = new Ciphertext(context, parmsId);
                    composeToCiphertext(parms, tempPlain, combined);
                    newTemp.add(combined);
                    tempPlain.clear();
                }
            }


            if (i == recursionLevel - 1) {
                assert temp.size() == 1;

                long[] result = new long[tempPlain.get(0).getCoeffCount()];
                long[] coeffArray = new long[tempPlain.get(0).getCoeffCount()];

                for (int ii = 0; ii < tempPlain.get(0).getCoeffCount(); ii++) {
                    coeffArray[ii] = tempPlain.get(0).at(ii);
                }
                // copy
                System.arraycopy(coeffArray,
                        0,
                        result,
                        0,
                        tempPlain.get(0).getCoeffCount());
                return result;
            } else {
                tempPlain.clear();
                temp = new ArrayList<>(newTemp);
            }
        }

        throw new IllegalArgumentException("error");
    }


    public static void composeToCiphertext(EncryptionParams parms, List<Plaintext> pts, Ciphertext ct) {

        composeToCiphertext(
                parms,
                pts,
                pts.size() / computeExpansionRatio(parms),
                ct);
    }

    public static void composeToCiphertext(EncryptionParams parms, List<Plaintext> pts, int ctPolyCount, Ciphertext ct) {

        int ptBitsPerCoeff = (int) DoubleUtils.log2(parms.getPlainModulus().getValue());
        int coeffCount = parms.getPolyModulusDegree();
        int coeffModCount = parms.getCoeffModulus().length;

        ct.resize(ctPolyCount);

        int ptIndex = 0;

        for (int polyIndex = 0; polyIndex < ctPolyCount; polyIndex++) {

            for (int coeffModIndex = 0; coeffModIndex < coeffModCount; coeffModIndex++) {


                double coeffBitSize = DoubleUtils.log2(parms.getCoeffModulus()[coeffModIndex].getValue());
                int localExpansionRatio = (int) Math.ceil(coeffBitSize / ptBitsPerCoeff);

                int shift = 0;
                for (int i = 0; i < localExpansionRatio; i++) {
                    for (int c = 0; c < pts.get(ptIndex).getCoeffCount(); c++) {
                        if (shift == 0) {
                            ct.getData()[
                                    ct.indexAt(polyIndex)
                                            + coeffModIndex * coeffCount + c
                                    ] = pts.get(ptIndex).at(c);
                        } else {
                            ct.getData()[
                                    ct.indexAt(polyIndex)
                                            + coeffModIndex * coeffCount + c
                                    ] += (pts.get(ptIndex).at(c) << shift);
                        }
                    }
                    ptIndex++;
                    shift += ptBitsPerCoeff;
                }
            }
        }
    }


    /**
     * compute size ratio between a ciphertext and the largest plaintext that can be encrypted.
     *
     * @param context encryption params.
     * @return expansion ratio.
     */
    public static int expansionRatio(Context context) {
        // 这里为什么使用 lastContext 呢？只用一个moduli，最大可能限度减少密文大小
        return computeExpansionRatio(context.lastContextData().getParms()) << 1;
    }

    private static int computeExpansionRatio(EncryptionParams parms) {

        int expansionRatio = 0;
        int ptBitsPerCoeff = (int) DoubleUtils.log2((double) parms.getPlainModulus().getValue());
        for (Modulus coeffModulus : parms.getCoeffModulus()) {
            double coeffBitSize = DoubleUtils.log2(coeffModulus.getValue());
            expansionRatio += Math.ceil(coeffBitSize / ptBitsPerCoeff);
        }
        return expansionRatio;
    }

    public static GaloisKeys generateGaloisKeys(Context context, KeyGenerator keyGenerator) {

        EncryptionParams parms = context.firstContextData().getParms();
        int degree = parms.getPolyModulusDegree();
        int logN = UintCore.getPowerOfTwo(degree);

        int[] galoisElts = new int[logN];
        for (int i = 0; i < logN; i++) {
            galoisElts[i] = (int) ((degree + UintArithmetic.exponentUint(2L, i)) / UintArithmetic.exponentUint(2L, i));
        }

        GaloisKeys galoisKeys = keyGenerator.createGaloisKeys(galoisElts);
        return galoisKeys;
    }


}
