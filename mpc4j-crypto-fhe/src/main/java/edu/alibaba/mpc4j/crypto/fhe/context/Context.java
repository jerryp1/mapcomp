package edu.alibaba.mpc4j.crypto.fhe.context;

import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.ntt.NttTables;
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

/**
 * Performs sanity checks (validation) and pre-computations for a given set of encryption
 * parameters. While the EncryptionParameters class is intended to be a light-weight class
 * to store the encryption parameters, the SEALContext class is a heavy-weight class that
 * is constructed from a given set of encryption parameters. It validates the parameters
 * for correctness, evaluates their properties, and performs and stores the results of
 * several costly pre-computations.
 * <p>
 * After the user has set at least the poly_modulus, coeff_modulus, and plain_modulus
 * parameters in a given EncryptionParameters instance, the parameters can be validated
 * for correctness and functionality by constructing an instance of SEALContext. The
 * constructor of SEALContext does all of its work automatically, and concludes by
 * constructing and storing an instance of the EncryptionParameterQualifiers class, with
 * its flags set according to the properties of the given parameters. If the created
 * instance of EncryptionParameterQualifiers has the parameters_set flag set to true, the
 * given parameter set has been deemed valid and is ready to be used. If the parameters
 * were for some reason not appropriately set, the parameters_set flag will be false,
 * and a new SEALContext will have to be created after the parameters are corrected.
 * <p>
 * By default, SEALContext creates a chain of SEALContext::ContextData instances. The
 * first one in the chain corresponds to special encryption parameters that are reserved
 * to be used by the various key classes (SecretKey, PublicKey, etc.). These are the exact
 * same encryption parameters that are created by the user and passed to th constructor of
 * SEALContext. The functions key_context_data() and key_parms_id() return the ContextData
 * and the parms_id corresponding to these special parameters. The rest of the ContextData
 * instances in the chain correspond to encryption parameters that are derived from the
 * first encryption parameters by always removing the last one of the moduli in the
 * coeff_modulus, until the resulting parameters are no longer valid, e.g., there are no
 * more primes left. These derived encryption parameters are used by ciphertexts and
 * plaintexts and their respective ContextData can be accessed through the
 * get_context_data(parms_id_type) function. The functions first_context_data() and
 * last_context_data() return the ContextData corresponding to the first and the last
 * set of parameters in the "data" part of the chain, i.e., the second and the last element
 * in the full chain. The chain itself is a doubly linked list, and is referred to as the
 * modulus switching chain.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/context.h#L250
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/9/11
 */
public class Context {
    /**
     * key parms id
     */
    private final ParmsIdType keyParmsId;
    /**
     * first parms id
     */
    private final ParmsIdType firstParmsId;
    /**
     * last parms id
     */
    private ParmsIdType lastParmsId;
    /**
     * context data map
     */
    private final HashMap<ParmsIdType, ContextData> contextDataMap = new HashMap<>();
    /**
     * security level
     */
    private final CoeffModulus.SecurityLevelType securityLevel;
    /**
     * using key switching
     */
    private final boolean usingKeySwitching;

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
     * Creates an instance of context and performs several pre-computations on the given EncryptionParameters.
     * Note that expandModChain is default true and SecurityLevelType is default TC128.
     *
     * @param params the encryption parameters.
     */
    public Context(EncryptionParams params) {
        this(params, true, CoeffModulus.SecurityLevelType.TC128);
    }

    /**
     * Creates an instance of Context and performs several pre-computations on the given EncryptionParameters.
     *
     * @param params         the encryption parameters.
     * @param expandModChain determines whether the modulus switching chain should be created.
     */
    public Context(EncryptionParams params, boolean expandModChain) {
        this(params, expandModChain, CoeffModulus.SecurityLevelType.TC128);
    }

    /**
     * Creates an instance of context, and performs several pre-computations on the given EncryptionParameters.
     *
     * @param parms          the encryption parameters.
     * @param expandModChain determines whether the modulus switching chain should be created.
     * @param securityLevel  the security level.
     */
    public Context(EncryptionParams parms, boolean expandModChain, CoeffModulus.SecurityLevelType securityLevel) {
        this.securityLevel = securityLevel;
        if (parms.getRandomGeneratorFactory() == null) {
            parms.setRandomGeneratorFactory(UniformRandomGeneratorFactory.defaultFactory());
        }
        // Validate parameters and add new ContextData to the map.
        // Note that this happens even if parameters are not valid.
        contextDataMap.put(parms.getParmsId().clone(), validate(parms));
        // keyParmsId [q1, q2, ..., qk]
        keyParmsId = parms.getParmsId().clone();
        // Then create first_parms_id_ if the parameters are valid and there is more than one modulus in coeff_modulus.
        // This is equivalent to expanding the chain by one step. Otherwise, we set first_parms_id_ to equal
        // key_parms_id_.
        // firstParmsId [q1, q2, .., q(k-1)]
        if (!contextDataMap.get(keyParmsId).qualifiers.isParametersSet() || parms.getCoeffModulus().length == 1) {
            firstParmsId = keyParmsId.clone();
        } else {
            ParmsIdType nextParmsId = createNextContextData(keyParmsId);
            firstParmsId = nextParmsId.isZero() ? keyParmsId.clone() : nextParmsId.clone();
        }

        // Set last_parms_id_ to point to first_parms_id_
        lastParmsId = firstParmsId.clone();
        // Check if key switching is available
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
     * Creates context data of a given encryption parameters.
     *
     * @param parms the encryption parameters.
     * @return context data includes pre-computation data for a given set of encryption parameters.
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
        for (Modulus modulus : coeffModulus) {
            if (modulus.getBitCount() > Constants.USER_MOD_BIT_COUNT_MAX ||
                modulus.getBitCount() < Constants.USER_MOD_BIT_COUNT_MIN) {
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
        // todo: Can we use NTT with coeff_modulus?
        // create small NTT tables for all coeff modulus
        contextData.qualifiers.usingNtt = true;
        try {
            NttTables.createNttTables(coeffCountPower, coeffModulus, contextData.smallNttTables);
        } catch (Exception e) {
            contextData.qualifiers.usingNtt = false;
            contextData.qualifiers.parameterError = ErrorType.INVALID_COEFF_MODULUS_NO_NTT;
            return contextData;
        }
        // now only consider bfv
        if (parms.getScheme() == SchemeType.BFV || parms.getScheme() == SchemeType.BGV) {
            // Plain modulus must be at least 2 and at most 60 bits
            if (plainModulus.getBitCount() > Constants.PLAIN_MOD_BIT_COUNT_MAX ||
                plainModulus.getBitCount() < Constants.PLAIN_MOD_BIT_COUNT_MIN) {
                contextData.qualifiers.parameterError = ErrorType.INVALID_PLAIN_MODULUS_BIT_COUNT;
                return contextData;
            }
            // Check that all coeff modulus are relatively prime to plain_modulus
            for (Modulus modulus : coeffModulus) {
                if (!Numth.areCoPrime(modulus.getValue(), plainModulus.getValue())) {
                    contextData.qualifiers.parameterError = ErrorType.INVALID_PLAIN_MODULUS_CO_PRIMALITY;
                    return contextData;
                }
            }
            // Check that plain_modulus is smaller than total coeff modulus
            // todo: consider remove new Array?
            if (!UintCore.isLessThanUint(new long[]{plainModulus.getValue()}, plainModulus.getUint64Count(), contextData.totalCoeffModulus, coeffModulusSize)) {
                contextData.qualifiers.parameterError = ErrorType.INVALID_PLAIN_MODULUS_TOO_LARGE;
                return contextData;
            }
            // plainModulus is a prime and mod 2n = 1
            contextData.qualifiers.usingBatching = true;
            try {
                // create small NTT table for plain modulus
                contextData.plainNttTables = new NttTables(coeffCountPower, plainModulus);
            } catch (Exception e) {
                contextData.qualifiers.usingBatching = false;
            }
            // Check for plain_lift
            // If all the small coefficient modulus are larger than plain modulus, we can quickly
            // lift plain coefficients to RNS form
            // 明文模比其 最小的密文 moduli 更小，那么 convert to RNS 的时候，不需要再取模
            contextData.qualifiers.usingFastPlainLift = true;
            for (Modulus modulus : coeffModulus) {
                contextData.qualifiers.usingFastPlainLift &= (modulus.getValue() > plainModulus.getValue());
            }
            // Calculate coeff_div_plain_modulus (BFV-"Delta") and the remainder upper_half_increment q/t
            long[] tempCoeffDivPlainModulus = new long[coeffModulusSize];
            contextData.coeffDivPlainModulus = new MultiplyUintModOperand[parms.getCoeffModulus().length];
            for (int i = 0; i < parms.getCoeffModulus().length; i++) {
                contextData.coeffDivPlainModulus[i] = new MultiplyUintModOperand();
            }
            contextData.upperHalfIncrement = new long[coeffModulusSize];
            // extend plainModulus's length to coeffModulusSize
            long[] widePlainModulus = UintCore.duplicateUintIfNeeded(new long[]{plainModulus.getValue()}, plainModulus.getUint64Count(), coeffModulusSize, false);
            // q/t , quotient stores in tempCoeffDivPlainModulus, remainder stores in contextData.upperHalfIncrement
            UintArithmetic.divideUint(
                contextData.totalCoeffModulus, widePlainModulus, coeffModulusSize, tempCoeffDivPlainModulus, contextData.upperHalfIncrement
            );
            // Store the non-RNS form of upper_half_increment for BFV encryption
            contextData.coeffModulusModPlainModulus = contextData.upperHalfIncrement[0];
            // Decompose coeff_div_plain_modulus into RNS factors, floor(q/t) % q_i
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
        // Create RNS Tool
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
            contextData.qualifiers.usingDescendingModulusChain &= coeffModulus[i].getValue() > coeffModulus[i + 1].getValue();
        }
        contextData.galoisTool = new GaloisTool(coeffCountPower);
        return contextData;
    }

    /**
     * Create the next context data by dropping the last element from coeff_modulus.
     * If the new encryption parameters are not valid, returns parms_id_zero.
     * Otherwise, returns the parms_id of the next parameter and appends the next context_data to the chain.
     *
     * @param prevParmsId the parms id of previous context data.
     * @return the parms id of next context data.
     */
    private ParmsIdType createNextContextData(ParmsIdType prevParmsId) {
        // note that here EncryptionParams object should be cloned, a new ContextData should hold a new object.
        EncryptionParams nextParms = contextDataMap.get(prevParmsId).parms.clone();
        Modulus[] nextCoeffModulus = nextParms.getCoeffModulus();
        // Create the next set of parameters by removing last modulus
        Modulus[] removedLastModulus = new Modulus[nextCoeffModulus.length - 1];
        System.arraycopy(nextCoeffModulus, 0, removedLastModulus, 0, nextCoeffModulus.length - 1);
        // re-compute parms id
        nextParms.setCoeffModulus(removedLastModulus);
        ParmsIdType nextParmsId = nextParms.getParmsId().clone();
        // Validate next parameters and create next context_data
        ContextData nextContextData = validate(nextParms);
        // If not valid then return zero parms_id
        if (!nextContextData.qualifiers.isParametersSet()) {
            return ParmsIdType.parmsIdZero();
        }
        // Add them to the context_data_map_
        contextDataMap.put(nextParmsId.clone(), nextContextData);
        // Add pointer to next context_data to the previous one (linked list)
        // Add pointer to previous context_data to the next one (doubly linked list)
        contextDataMap.get(prevParmsId).nextContextData = contextDataMap.get(nextParmsId);
        contextDataMap.get(nextParmsId).preContextData = contextDataMap.get(prevParmsId);
        return nextParmsId;
    }

    /**
     * Returns the context data corresponding to encryption parameters with a given parms id.
     * If parameters with the given parms_id are not found then the function returns nullptr.
     *
     * @param parmsId the parms id of the encryption parameters.
     * @return the context data corresponding to encryption parameters with a given parms id.
     */
    public ContextData getContextData(ParmsIdType parmsId) {
        return contextDataMap.getOrDefault(parmsId, null);
    }

    /**
     * Returns the context data corresponding to the first encryption parameters that are used for data.
     *
     * @return the context data corresponding to the first encryption parameters that are used for data.
     */
    public ContextData firstContextData() {
        return contextDataMap.getOrDefault(firstParmsId, null);
    }

    /**
     * Returns the context data corresponding to encryption parameters that are used for keys.
     *
     * @return the context data corresponding to encryption parameters that are used for keys.
     */
    public ContextData keyContextData() {
        return contextDataMap.getOrDefault(keyParmsId, null);
    }

    /**
     * Returns the context data corresponding to the last encryption parameters that are used for data.
     *
     * @return the context data corresponding to the last encryption parameters that are used for data.
     */
    public ContextData lastContextData() {
        return contextDataMap.getOrDefault(lastParmsId, null);
    }

    /**
     * Returns whether the first_context_data's encryption parameters are valid.
     *
     * @return whether the first_context_data's encryption parameters are valid.
     */
    public boolean isParametersSet() {
        return firstContextData() != null && firstContextData().qualifiers.isParametersSet();
    }

    /**
     * Returns the name of encryption parameters' error.
     *
     * @return the name of encryption parameters' error.
     */
    public String parametersErrorName() {
        return firstContextData() != null ? firstContextData().qualifiers.parameterErrorName() : "Context is empty";
    }

    /**
     * Returns a comprehensive message that interprets encryption parameters' error.
     *
     * @return a comprehensive message that interprets encryption parameters' error.
     */
    public String parametersErrorMessage() {
        return firstContextData() != null ? firstContextData().qualifiers.parameterErrorMessage() : "Context is empty";
    }

    /**
     * Returns whether the coefficient modulus supports keyswitching. In practice,
     * support for key switching is required by Evaluator::relinearize,
     * Evaluator::apply_galois, and all rotation and conjugation operations. For
     * keyswitching to be available, the coefficient modulus parameter must consist
     * of at least two prime number factors.
     *
     * @return whether the coefficient modulus supports keyswitching
     */
    public boolean isUsingKeySwitching() {
        return usingKeySwitching;
    }

    /**
     * Returns a parms_id_type corresponding to the last encryption parameters that are used for data.
     *
     * @return a parms_id_type corresponding to the last encryption parameters that are used for data.
     */
    public ParmsIdType getLastParmsId() {
        return lastParmsId;
    }

    /**
     * Returns a parms_id_type corresponding to the first encryption parameters that are used for data.
     *
     * @return a parms_id_type corresponding to the first encryption parameters that are used for data.
     */
    public ParmsIdType getFirstParmsId() {
        return firstParmsId;
    }

    /**
     * Returns a parms_id_type corresponding to the set of encryption parameters that are used for keys.
     *
     * @return a parms_id_type corresponding to the set of encryption parameters that are used for keys.
     */
    public ParmsIdType getKeyParmsId() {
        return keyParmsId;
    }

    /**
     * Class to hold pre-computation data for a given set of encryption parameters.
     * <p>
     * The implementation is from:
     * https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/context.h#L256
     * </p>
     */
    public class ContextData {
        /**
         * encryption parameters
         */
        private EncryptionParams parms;
        /**
         * attributes (qualifiers) of the encryption parameters
         */
        private EncryptionParameterQualifiers qualifiers;
        /**
         * RNS tool
         */
        private RnsTool rnsTool;
        /**
         * small NTT tables
         */
        private NttTables[] smallNttTables;
        /**
         * plain NTT table
         */
        private NttTables plainNttTables;
        /**
         * Galois tool
         */
        private GaloisTool galoisTool;
        /**
         * q = \prod q_i
         */
        private long[] totalCoeffModulus;
        /**
         * bit length of q
         */
        private int totalCoeffModulusBitCount = 0;
        /**
         * floor(q / t) mod q_i
         */
        private MultiplyUintModOperand[] coeffDivPlainModulus;
        /**
         * (t + 1) / 2
         */
        private long plainUpperHalfThreshold = 0;
        /**
         * q_i - t, or q - t
         */
        private long[] plainUpperHalfIncrement;

        private long[] upperHalfThreshold;
        /**
         * (q mod t) mod q_i
         */
        private long[] upperHalfIncrement;
        /**
         * q mod t
         */
        private long coeffModulusModPlainModulus = 0;
        /**
         * the context data corresponding to the previous parameters in the modulus switching chain
         */
        private ContextData preContextData;
        /**
         * the context data corresponding to the next parameters in the modulus switching chain
         */
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

        /**
         * Returns the context data corresponding to the previous parameters in the modulus switching chain.
         * If the current data is the first one in the chain, then the result is nullptr.
         *
         * @return the context data corresponding to the previous parameters in the modulus switching chain.
         */
        public ContextData getPreContextData() {
            return preContextData;
        }

        /**
         * Returns the context data corresponding to the next parameters in the modulus switching chain.
         * If the current data is the last one in the chain, then the result is nullptr.
         *
         * @return the context data corresponding to the next parameters in the modulus switching chain.
         */
        public ContextData getNextContextData() {
            return nextContextData;
        }

        /**
         * Returns the index of the parameter set in a chain. The initial parameters have index 0
         * and the index increases sequentially in the parameter chain.
         *
         * @return the index of the parameter set in a chain.
         */
        public int getChainIndex() {
            return chainIndex;
        }
    }
}