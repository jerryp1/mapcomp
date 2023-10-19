package edu.alibaba.mpc4j.crypto.fhe.context;

import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.ntt.NttTables;
import edu.alibaba.mpc4j.crypto.fhe.ntt.NttTablesCreateIter;
import edu.alibaba.mpc4j.crypto.fhe.params.EncryptionParams;
import edu.alibaba.mpc4j.crypto.fhe.params.ParmsIdType;
import edu.alibaba.mpc4j.crypto.fhe.params.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.rand.UniformRandomGeneratorFactory;
import edu.alibaba.mpc4j.crypto.fhe.rns.RnsBase;
import edu.alibaba.mpc4j.crypto.fhe.rns.RnsTool;
import edu.alibaba.mpc4j.crypto.fhe.utils.Constants;
import edu.alibaba.mpc4j.crypto.fhe.utils.GaloisTool;
import edu.alibaba.mpc4j.crypto.fhe.zq.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.IntStream;

/**
 * @author Qixian Zhou
 * @date 2023/9/11
 */
public class Context {


    private ParmsIdType keyParmsId;

    private ParmsIdType firstParmsId;

    private ParmsIdType lastParmsId;

    private HashMap<ParmsIdType, ContextData> contextDataMap = new HashMap<>();

    private CoeffModulus.SecurityLevelType securityLevel;

    private boolean usingKeySwitching;


    @Override
    public String toString() {
        return "Context{" +
                "keyParmsId=" + keyParmsId +
                ", firstParmsId=" + firstParmsId +
                ", lastParmsId=" + lastParmsId +
                ", contextDataMap size=" + contextDataMap.size() +
                ", securityLevel=" + securityLevel +
                ", usingKeySwitching=" + usingKeySwitching +
                '}';
    }

    /**
     * Creates an instance of Context and performs several pre-computations
     * on the given EncryptionParameters.
     * Note that expandModChain is default true and SecurityLevelType is default TC128.
     *
     * @param params The encryption parameters
     */
    public Context(EncryptionParams params) {
        this(params, true, CoeffModulus.SecurityLevelType.TC128);
    }

    public Context(EncryptionParams params,  boolean expandModChain) {
        this(params, expandModChain, CoeffModulus.SecurityLevelType.TC128);
    }

    /**
     * Creates an instance of Context, and performs several pre-computations
     * on the given EncryptionParameters.
     *
     * @param parms          The encryption parameters
     * @param expandModChain Determines whether the modulus switching chain
     *                       should be created
     * @param securityLevel  Determines whether a specific security level should be
     *                       enforced according to HomomorphicEncryption.org security standard
     */
    public Context(EncryptionParams parms, boolean expandModChain, CoeffModulus.SecurityLevelType securityLevel) {

        this.securityLevel = securityLevel;
        if (parms.getRandomGeneratorFactory() == null) {
            parms.setRandomGeneratorFactory(UniformRandomGeneratorFactory.defaultFactory());
        }

        // Validate parameters and add new ContextData to the map
        // Note that this happens even if parameters are not valid
        contextDataMap.put(parms.getParmsId().clone(), validate(parms));
        // keyParmsId 对应完整的 modulus: [q1, q2, ..., qk]
        keyParmsId = parms.getParmsId().clone();

        // Then create first_parms_id_ if the parameters are valid and there is
        // more than one modulus in coeff_modulus.
        // This is equivalent to expanding the chain by one step. Otherwise, we set first_parms_id_ to equal
        // key_parms_id_.

        // 这里就是计算 firstParmsId, firstParmsId 对应：[q1, q2, .., q(k-1)]
        if (!contextDataMap.get(keyParmsId).qualifiers.isParametersSet()
                || parms.getCoeffModulus().length == 1) {
            firstParmsId = keyParmsId.clone();
        } else {
            ParmsIdType nextParmsId = createNextContextData(keyParmsId);
            firstParmsId = nextParmsId.isZero() ? keyParmsId.clone() : nextParmsId.clone();
        }

        // Set last_parms_id_ to point to first_parms_id_
        lastParmsId = firstParmsId.clone();
        // Check if keyswitching is available
        // 什么情况下是 false呢？就是上面的 if/else 进入了 if 或者 创建下一个 ContextData 对象失败
        usingKeySwitching = !firstParmsId.equals(keyParmsId);

        // If modulus switching chain is to be created, compute the remaining parameter sets as long as they are valid
        // to use (i.e., parameters_set() == true).
        if (expandModChain && contextDataMap.get(firstParmsId).qualifiers.isParametersSet()) {
            ParmsIdType prevParmsId = firstParmsId.clone();
            // 从 first [q1, q2, ..., q(k-1)] 递减计算至： [q1]
            while (contextDataMap.get(prevParmsId).parms.getCoeffModulus().length > 1) {
                ParmsIdType nextParmsId = createNextContextData(prevParmsId);
                // 如果等于0, 说明创建 下一个 ContextData 失败了
                if (nextParmsId.isZero()) {
                    break;
                }
                // 当前的 参数Id，作为下一轮计算的 ID
                prevParmsId = nextParmsId.clone();
                // 更新 last参数ID，最终指向最后一个参数 Id , 即 [q1] 对应的 ContextData 对象
                lastParmsId = nextParmsId.clone();
            }
        }
        // Set the chain_index for each context_data
        int parmsCount = contextDataMap.size();
        ContextData contextDataPtr = contextDataMap.get(keyParmsId);
        // 每一个 ContextData 又维护一个 chainIndex 来指明在 chain 中的位置，这里就是倒过来看了
        // [q1] ---> [q1, q2] ---> [q1, q2, q3] --> .... --> [q1, q2, ..., qk]
        while (contextDataPtr != null) {
            contextDataPtr.chainIndex = --parmsCount;
            contextDataPtr = contextDataPtr.nextContextData;
        }
    }


    /**
     * @param parms
     * @return
     */
    private ContextData validate(EncryptionParams parms) {

        ContextData contextData = new ContextData(parms);

        contextData.qualifiers.parameterError = ErrorType.SUCCESS;

        if (parms.getScheme() == SchemeType.NONE) {
            contextData.qualifiers.parameterError = ErrorType.INVALID_SCHEME;
            return contextData;
        }

        Modulus[] coeffModulus = parms.getCoeffModulus();
        Modulus plainModulus = parms.getPlainModulus();

        // The number of coeff moduli is restricted to 64 to prevent unexpected behaviors
        if (coeffModulus.length > Constants.COEFF_MOD_COUNT_MAX || coeffModulus.length < Constants.COEFF_MOD_COUNT_MIN) {
            contextData.qualifiers.parameterError = ErrorType.INVALID_COEFF_MODULUS_SIZE;
            return contextData;
        }
        // 检测每一个 qi 的 bit count 是否合法 [2, 60]
        int coeffModulusSize = coeffModulus.length;
        for (int i = 0; i < coeffModulusSize; i++) {
            if (coeffModulus[i].getValue() >>> Constants.USER_MOD_BIT_COUNT_MAX > 0 ||
                    (coeffModulus[i].getValue() >>> (Constants.USER_MOD_BIT_COUNT_MIN - 1)) == 0
            ) {
                contextData.qualifiers.parameterError = ErrorType.INVALID_COEFF_MODULUS_BIT_COUNT;
                return contextData;
            }
        }
        // Compute the product of all coeff moduli
        contextData.totalCoeffModulus = new long[coeffModulusSize];
        long[] coeffModulusValues = Arrays.stream(coeffModulus).mapToLong(Modulus::getValue).toArray();
        UintArithmetic.multiplyManyUint64(coeffModulusValues, coeffModulusSize, contextData.totalCoeffModulus);

        contextData.totalCoeffModulusBitCount = UintCore.getSignificantBitCountUint(contextData.totalCoeffModulus, coeffModulusSize);

        // Check polynomial modulus degree and create poly_modulus
        // x^N + 1, N \in [2, 131073]
        int polyModulusDegree = parms.getPolyModulusDegree();
        if (polyModulusDegree < Constants.POLY_MOD_DEGREE_MIN || polyModulusDegree > Constants.POLY_MOD_DEGREE_MAX) {

            contextData.qualifiers.parameterError = ErrorType.INVALID_POLY_MODULUS_DEGREE;
            return contextData;
        }

        int coeffCountPower = UintCore.getPowerOfTwo(polyModulusDegree);
        if (coeffCountPower < 0) {
            contextData.qualifiers.parameterError = ErrorType.INVALID_POLY_MODULUS_DEGREE_NON_POWER_OF_TWO;
            return contextData;
        }

        // Quick sanity check
        // todo: really need this check?
        if (!Common.productFitsIn(false, coeffModulusSize, polyModulusDegree)) {
            contextData.qualifiers.parameterError = ErrorType.INVALID_PARAMETERS_TOO_LARGE;
            return contextData;
        }

        // Polynomial modulus X^(2^k) + 1 is guaranteed at this point
        contextData.qualifiers.usingFft = true;

        // Assume parameters satisfy desired security level
        contextData.qualifiers.securityLevel = securityLevel;

        // Check if the parameters are secure according to HomomorphicEncryption.org security standard
        if (contextData.totalCoeffModulusBitCount > CoeffModulus.maxBitCount(polyModulusDegree, securityLevel)) {
            // Not secure according to HomomorphicEncryption.org security standard
            contextData.qualifiers.securityLevel = CoeffModulus.SecurityLevelType.NONE;
            if (securityLevel != CoeffModulus.SecurityLevelType.NONE) {
                contextData.qualifiers.parameterError = ErrorType.INVALID_PARAMETERS_INSECURE;
                return contextData;
            }
        }
        // Set up RNSBase for coeff_modulus
        // RNSBase's constructor may fail due to:
        //   (1) coeff_mod not coprime
        //   (2) cannot find inverse of punctured products (because of (1))
        RnsBase coeffModulusBase;
        try {
            coeffModulusBase = new RnsBase(coeffModulus);
        } catch (Exception e) {
            contextData.qualifiers.parameterError = ErrorType.FAILED_CREATING_RNS_BASE;
            return contextData;
        }

        // Can we use NTT with coeff_modulus?
        contextData.qualifiers.usingNtt = true;
        try {
            NttTablesCreateIter.createNttTables(coeffCountPower, coeffModulus, contextData.smallNttTables);
        } catch (Exception e) {
            contextData.qualifiers.usingNtt = false;
            contextData.qualifiers.parameterError = ErrorType.INVALID_COEFF_MODULUS_NO_NTT;
            return contextData;
        }

        // now only consider bfv
        if (parms.getScheme() == SchemeType.BFV || parms.getScheme() == SchemeType.BGV) {

            // Plain modulus must be at least 2 and at most 60 bits
            if (plainModulus.getValue() >>> Constants.PLAIN_MOD_BIT_COUNT_MAX != 0 ||
                    plainModulus.getValue() >>> (Constants.PLAIN_MOD_BIT_COUNT_MIN - 1) == 0
            ) {
                contextData.qualifiers.parameterError = ErrorType.INVALID_PLAIN_MODULUS_BIT_COUNT;
                return contextData;
            }

            // Check that all coeff moduli are relatively prime to plain_modulus
            for (int i = 0; i < coeffModulusSize; i++) {
                if (!Numth.areCoPrime(coeffModulus[i].getValue(), plainModulus.getValue())) {
                    contextData.qualifiers.parameterError = ErrorType.INVALID_PLAIN_MODULUS_CO_PRIMALITY;
                    return contextData;
                }
            }

            // Check that plain_modulus is smaller than total coeff modulus
            if (!UintCore.isLessThanUint(
                    new long[]{plainModulus.getValue()}, // todo: consider remove new Array?
                    plainModulus.getUint64Count(),
                    contextData.totalCoeffModulus,
                    coeffModulusSize)) {
                contextData.qualifiers.parameterError = ErrorType.INVALID_PLAIN_MODULUS_TOO_LARGE;
                return contextData;
            }

            // Can we use batching? (NTT with plain_modulus)
            // plainModulus is a prime and mod 2n = 1
            contextData.qualifiers.usingBatching = true;
            try {
                contextData.plainNttTables = new NttTables(coeffCountPower, plainModulus);
//                NttTablesCreateIter.createNttTables(coeffCountPower, new Modulus[]{plainModulus}, contextData.plainNttTables);
            } catch (Exception e) {
                contextData.qualifiers.usingBatching = false;
            }

            // Check for plain_lift
            // If all the small coefficient moduli are larger than plain modulus, we can quickly
            // lift plain coefficients to RNS form
            // 明文模比其 最小的密文 moduli 更小，那么 convert to RNS 的时候，不需要再取模
            contextData.qualifiers.usingFastPlainLift = true;
            for (int i = 0; i < coeffModulusSize; i++) {
                contextData.qualifiers.usingFastPlainLift &= (coeffModulus[i].getValue() > plainModulus.getValue());
            }

            // Calculate coeff_div_plain_modulus (BFV-"Delta") and the remainder upper_half_increment
            // q/t
            long[] tempCoeffDivPlainModulus = new long[coeffModulusSize];
//            contextData.coeffDivPlainModulus = IntStream.range(0, parms.getCoeffModulus().length)
//                    .mapToObj(i -> new MultiplyUintModOperand())
//                    .toArray(MultiplyUintModOperand[]::new);
            contextData.coeffDivPlainModulus = new MultiplyUintModOperand[parms.getCoeffModulus().length];
            for (int i = 0; i < parms.getCoeffModulus().length; i++) {
                contextData.coeffDivPlainModulus[i] = new MultiplyUintModOperand();
            }


            contextData.upperHalfIncrement = new long[coeffModulusSize];
            // extend plainModulus's length to coeffModulusSize
            long[] widePlainModulus = UintCore.duplicateUintIfNeeded(new long[]{plainModulus.getValue()}, plainModulus.getUint64Count(), coeffModulusSize, false);
            // q/t , quotient stores in tempCoeffDivPlainModulus, remainder stores in contextData.upperHalfIncrement
            UintArithmetic.divideUint(contextData.totalCoeffModulus, widePlainModulus, coeffModulusSize,
                    tempCoeffDivPlainModulus, contextData.upperHalfIncrement);

            // Store the non-RNS form of upper_half_increment for BFV encryption
            contextData.coeffModulusModPlainModulus = contextData.upperHalfIncrement[0];

            // Decompose coeff_div_plain_modulus into RNS factors
            coeffModulusBase.decompose(tempCoeffDivPlainModulus);


            for (int i = 0; i < coeffModulusSize; i++) {
                contextData.coeffDivPlainModulus[i].set(tempCoeffDivPlainModulus[i], coeffModulusBase.getBase(i));
            }

            // Decompose upper_half_increment into RNS factors
            coeffModulusBase.decompose(contextData.upperHalfIncrement);

            // Calculate (plain_modulus + 1) / 2.
            contextData.plainUpperHalfThreshold = (plainModulus.getValue() + 1) >>> 1;

            // Calculate coeff_modulus - plain_modulus.
            contextData.plainUpperHalfIncrement = new long[coeffModulusSize];
            if (contextData.qualifiers.usingFastPlainLift) {
                // Calculate coeff_modulus[i] - plain_modulus if using_fast_plain_lift
                for (int i = 0; i < coeffModulusSize; i++) {
                    contextData.plainUpperHalfIncrement[i] = coeffModulus[i].getValue() - plainModulus.getValue();
                }
            } else {
                // directly sub
                UintArithmetic.subUint(contextData.totalCoeffModulus, widePlainModulus, coeffModulusSize, contextData.plainUpperHalfIncrement);
            }
        } else if (parms.getScheme() == SchemeType.CKKS) {
            //todo: implement CKKS
            throw new IllegalArgumentException("now cannot support CKKS");
        } else {

            contextData.qualifiers.parameterError = ErrorType.INVALID_SCHEME;
            return contextData;
        }

        // Create RNSTool
        // RNSTool's constructor may fail due to:
        //   (1) auxiliary base being too large
        //   (2) cannot find inverse of punctured products in auxiliary base
        try {
            contextData.rnsTool = new RnsTool(polyModulusDegree, coeffModulusBase, plainModulus);
        } catch (Exception e) {
            contextData.qualifiers.parameterError = ErrorType.FAILED_CREATING_RNS_TOOL;
            return contextData;
        }

        // Check whether the coefficient modulus consists of a set of primes that are in decreasing order
        contextData.qualifiers.usingDescendingModulusChain = true;
        for (int i = 0; i < coeffModulusSize - 1; i++) {
            contextData.qualifiers.usingDescendingModulusChain &=
                    (coeffModulus[i].getValue() > coeffModulus[i + 1].getValue());
        }

        contextData.galoisTool = new GaloisTool(coeffCountPower);

        return contextData;
    }


    private ParmsIdType createNextContextData(ParmsIdType prevParmsId) {

        // note that here EncryptionParams object should be cloned, a new ContextData should hold a new  object.

        // 这里需要一个新的 加密参数对象，这个对象是从 prevParmsId 得到的，然后咋这里会做一些修改
        EncryptionParams nextParms = contextDataMap.get(prevParmsId).parms.clone();
        Modulus[] nextCoeffModulus = nextParms.getCoeffModulus();
        // Create the next set of parameters by removing last modulus
        // 最后一个 moduli 被移除，然后得到的新的 Modulus[] 被赋值给 nextParms
        Modulus[] removedLastModulus = new Modulus[nextCoeffModulus.length - 1];
        System.arraycopy(nextCoeffModulus, 0, removedLastModulus, 0, nextCoeffModulus.length - 1);
        nextParms.setCoeffModulus(removedLastModulus);
        // 随着参数被改变，parmsId 也会被修改
        ParmsIdType nextParmsId = nextParms.getParmsId().clone();

        // Validate next parameters and create next context_data
        // 根据新的参数，计算新的 ContextData
        ContextData nextContextData = validate(nextParms);

        // If not valid then return zero parms_id
        // 如果当前参数 计算出来的 ContextData 不合法，则直接返回
        if (!nextContextData.qualifiers.isParametersSet()) {
            return ParmsIdType.parmsIdZero();
        }

        // Add them to the context_data_map_
        // 这里还需要一次 clone 吗？ 上面已经 clone 过一次了
        contextDataMap.put(nextParmsId.clone(), nextContextData);

        // Add pointer to next context_data to the previous one (linked list)
        // Add pointer to previous context_data to the next one (doubly linked list)
        // We need to remove constness first to modify this

        // 前一个参数 对应的 nextContextData, 就是 当前参数Id 对应的 ContextData
        contextDataMap.get(prevParmsId).nextContextData = contextDataMap.get(nextParmsId);
        // 当前参数Id 对应的 preContextData 就是 前一个参数Id 对应的 Context
        contextDataMap.get(nextParmsId).preContextData = contextDataMap.get(prevParmsId);

        return nextParmsId;
    }


    public ContextData getContextData(ParmsIdType parmsId) {

        return contextDataMap.getOrDefault(parmsId, null);
    }

    public ContextData firstContextData() {
        return contextDataMap.getOrDefault(firstParmsId, null);
    }

    public ContextData keyContextData() {
        return contextDataMap.getOrDefault(keyParmsId, null);
    }

    public ContextData lastContextData() {
        return contextDataMap.getOrDefault(lastParmsId, null);
    }

    public boolean isParametersSet() {
        return firstContextData() != null ? firstContextData().qualifiers.isParametersSet() : false;
    }

    public String parametersErrorName() {
        return firstContextData() != null ? firstContextData().qualifiers.parameterErrorName() : "Context is empty";
    }

    public String parametersErrorMessage() {
        return firstContextData() != null ? firstContextData().qualifiers.parameterErrorMessage() : "Context is empty";
    }


    /**
     * Returns whether the coefficient modulus supports keyswitching. In practice,
     * support for keyswitching is required by Evaluator::relinearize,
     * Evaluator::apply_galois, and all rotation and conjugation operations. For
     * keyswitching to be available, the coefficient modulus parameter must consist
     * of at least two prime number factors.
     *
     * @return whether the coefficient modulus supports keyswitching
     */
    public boolean isUsingKeySwitching() {
        return usingKeySwitching;
    }

    public ParmsIdType getLastParmsId() {
        return lastParmsId;
    }


    public ParmsIdType getFirstParmsId() {
        return firstParmsId;
    }

    public ParmsIdType getKeyParmsId() {
        return keyParmsId;
    }

    /**
     * Class to hold pre-computation data for a given set of encryption parameters.
     */
    public class ContextData {

        private EncryptionParams parms;

        private EncryptionParameterQualifiers qualifiers;

        private RnsTool rnsTool;

        private NttTables[] smallNttTables;
        /**
         * only one modulus, so using single NttTables is enough.
         */
        private NttTables plainNttTables;

        private GaloisTool galoisTool;

        private long[] totalCoeffModulus;

        private int totalCoeffModulusBitCount = 0;

        private MultiplyUintModOperand[] coeffDivPlainModulus;

        private long plainUpperHalfThreshold = 0;

        private long[] plainUpperHalfIncrement;

        private long[] upperHalfThreshold;

        private long[] upperHalfIncrement;

        private long coeffModulusModPlainModulus = 0;

        private ContextData preContextData;

        private ContextData nextContextData;

        private int chainIndex = 0;

        public ContextData(EncryptionParams parms) {
            this.parms = parms;
            qualifiers = new EncryptionParameterQualifiers();
            smallNttTables = new NttTables[parms.getCoeffModulus().length];
        }


        public EncryptionParams getParms() {
            return parms;
        }

        public ParmsIdType getParmsId() {
            return parms.getParmsId();
        }

        public EncryptionParameterQualifiers getQualifiers() {
            return qualifiers;
        }

        public long[] getTotalCoeffModulus() {
            return totalCoeffModulus;
        }


        public int getTotalCoeffModulusBitCount() {
            return totalCoeffModulusBitCount;
        }


        public RnsTool getRnsTool() {
            return rnsTool;
        }

        public NttTables[] getSmallNttTables() {
            return smallNttTables;
        }

        public NttTables getPlainNttTables() {
            return plainNttTables;
        }


        public GaloisTool getGaloisTool() {
            return galoisTool;
        }


        public MultiplyUintModOperand[] getCoeffDivPlainModulus() {
            return coeffDivPlainModulus;
        }


        public long getPlainUpperHalfThreshold() {
            return plainUpperHalfThreshold;
        }

        public long[] getPlainUpperHalfIncrement() {
            return plainUpperHalfIncrement;
        }

        public long[] getUpperHalfThreshold() {
            return upperHalfThreshold;
        }


        public long[] getUpperHalfIncrement() {
            return upperHalfIncrement;
        }


        public long getCoeffModulusModPlainModulus() {
            return coeffModulusModPlainModulus;
        }


        public ContextData getPreContextData() {
            return preContextData;
        }

        public ContextData getNextContextData() {
            return nextContextData;
        }

        public int getChainIndex() {
            return chainIndex;
        }
    }


}
