package edu.alibaba.mpc4j.crypto.fhe.context;

import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus.SecurityLevelType;
import edu.alibaba.mpc4j.crypto.fhe.params.EncryptionParams;
import edu.alibaba.mpc4j.crypto.fhe.params.ParmsIdType;
import edu.alibaba.mpc4j.crypto.fhe.params.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.rand.UniformRandomGenerator;

import edu.alibaba.mpc4j.crypto.fhe.rand.UniformRandomGeneratorFactory;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Qixian Zhou
 * @date 2023/9/12
 */
public class ContextTest {

    private static final int MAX_LOOP_NUM = 100;


    @Test
    public void bfvContextConstructor() {

        SchemeType scheme = SchemeType.BFV;
        EncryptionParams parms = new EncryptionParams(scheme);
        {

            Context context = new Context(parms, false, SecurityLevelType.NONE);
            EncryptionParameterQualifiers qualifiers = context.firstContextData().getQualifiers();

            Assert.assertFalse(qualifiers.isParametersSet());
            Assert.assertEquals(ErrorType.INVALID_COEFF_MODULUS_SIZE, qualifiers.parameterError);
            Assert.assertFalse(qualifiers.usingFft);
            Assert.assertFalse(qualifiers.usingNtt);
            Assert.assertFalse(qualifiers.usingBatching);
            Assert.assertFalse(qualifiers.usingFastPlainLift);
            Assert.assertFalse(qualifiers.usingDescendingModulusChain);
            Assert.assertEquals(SecurityLevelType.NONE, qualifiers.securityLevel);
            Assert.assertFalse(context.isUsingKeySwitching());
        }

        // Not relatively prime coeff moduli
        parms.setPolyModulusDegree(4);
        parms.setCoeffModulus(new long[]{2, 30});
        parms.setPlainModulus(2);
        parms.setRandomGeneratorFactory(new UniformRandomGeneratorFactory());
        {
            Context context = new Context(parms, false, SecurityLevelType.NONE);
            EncryptionParameterQualifiers qualifiers = context.firstContextData().getQualifiers();

            Assert.assertFalse(qualifiers.isParametersSet());
            Assert.assertEquals(ErrorType.FAILED_CREATING_RNS_BASE, qualifiers.parameterError);
            Assert.assertTrue(qualifiers.usingFft);
            Assert.assertFalse(qualifiers.usingNtt);
            Assert.assertFalse(qualifiers.usingBatching);
            Assert.assertFalse(qualifiers.usingFastPlainLift);
            Assert.assertFalse(qualifiers.usingDescendingModulusChain);
            Assert.assertEquals(SecurityLevelType.NONE, qualifiers.securityLevel);
            Assert.assertFalse(context.isUsingKeySwitching());
        }

        // Plain modulus not relatively prime to coeff moduli
        parms.setPolyModulusDegree(4);
        parms.setCoeffModulus(new long[]{17, 41});
        parms.setPlainModulus(34);
        parms.setRandomGeneratorFactory(new UniformRandomGeneratorFactory());
        {
            Context context = new Context(parms, false, SecurityLevelType.NONE);
            EncryptionParameterQualifiers qualifiers = context.firstContextData().getQualifiers();

            Assert.assertFalse(qualifiers.isParametersSet());
            Assert.assertEquals(ErrorType.INVALID_PLAIN_MODULUS_CO_PRIMALITY, qualifiers.parameterError);
            Assert.assertTrue(qualifiers.usingFft);
            Assert.assertTrue(qualifiers.usingNtt);
            Assert.assertFalse(qualifiers.usingBatching);
            Assert.assertFalse(qualifiers.usingFastPlainLift);
            Assert.assertFalse(qualifiers.usingDescendingModulusChain);
            Assert.assertEquals(SecurityLevelType.NONE, qualifiers.securityLevel);
            Assert.assertFalse(context.isUsingKeySwitching());
        }

        // Plain modulus not smaller than product of coeff moduli
        parms.setPolyModulusDegree(4);
        parms.setCoeffModulus(new long[]{17});
        parms.setPlainModulus(41);
        parms.setRandomGeneratorFactory(new UniformRandomGeneratorFactory());
        {
            Context context = new Context(parms, false, SecurityLevelType.NONE);
            Assert.assertEquals(17, context.firstContextData().getTotalCoeffModulus()[0]);

            EncryptionParameterQualifiers qualifiers = context.firstContextData().getQualifiers();

            Assert.assertFalse(qualifiers.isParametersSet());
            Assert.assertEquals(ErrorType.INVALID_PLAIN_MODULUS_TOO_LARGE, qualifiers.parameterError);
            Assert.assertTrue(qualifiers.usingFft);
            Assert.assertTrue(qualifiers.usingNtt);
            Assert.assertFalse(qualifiers.usingBatching);
            Assert.assertFalse(qualifiers.usingFastPlainLift);
            Assert.assertFalse(qualifiers.usingDescendingModulusChain);
            Assert.assertEquals(SecurityLevelType.NONE, qualifiers.securityLevel);
            Assert.assertFalse(context.isUsingKeySwitching());
        }

        // FFT poly but not NTT modulus, 3 mod 2 * 4 != 1
        parms.setPolyModulusDegree(4);
        parms.setCoeffModulus(new long[]{3});
        parms.setPlainModulus(2);
        parms.setRandomGeneratorFactory(new UniformRandomGeneratorFactory());
        {
            Context context = new Context(parms, false, SecurityLevelType.NONE);
            Assert.assertEquals(3, context.firstContextData().getTotalCoeffModulus()[0]);

            EncryptionParameterQualifiers qualifiers = context.firstContextData().getQualifiers();

            Assert.assertFalse(qualifiers.isParametersSet());
            Assert.assertEquals(ErrorType.INVALID_COEFF_MODULUS_NO_NTT, qualifiers.parameterError);
            Assert.assertTrue(qualifiers.usingFft);
            Assert.assertFalse(qualifiers.usingNtt);
            Assert.assertFalse(qualifiers.usingBatching);
            Assert.assertFalse(qualifiers.usingFastPlainLift);
            Assert.assertFalse(qualifiers.usingDescendingModulusChain);
            Assert.assertEquals(SecurityLevelType.NONE, qualifiers.securityLevel);
            Assert.assertFalse(context.isUsingKeySwitching());
        }

        // Parameters OK; no fast plain lift
        parms.setPolyModulusDegree(4);
        parms.setCoeffModulus(new long[]{17, 41});
        parms.setPlainModulus(18);
        parms.setRandomGeneratorFactory(new UniformRandomGeneratorFactory());
        {
            Context context = new Context(parms, false, SecurityLevelType.NONE);
            Assert.assertEquals(697L, context.firstContextData().getTotalCoeffModulus()[0]);

            EncryptionParameterQualifiers qualifiers = context.firstContextData().getQualifiers();
            Assert.assertTrue(qualifiers.isParametersSet());
            Assert.assertTrue(qualifiers.usingFft);
            Assert.assertTrue(qualifiers.usingNtt);
            Assert.assertFalse(qualifiers.usingBatching);
            Assert.assertFalse(qualifiers.usingFastPlainLift);
            Assert.assertFalse(qualifiers.usingDescendingModulusChain);
            Assert.assertEquals(SecurityLevelType.NONE, qualifiers.securityLevel);
            Assert.assertFalse(context.isUsingKeySwitching());
        }

        // Parameters OK; fast plain lift, plain modulus less than all the coeff moduli
        parms.setPolyModulusDegree(4);
        parms.setCoeffModulus(new long[]{17, 41});
        parms.setPlainModulus(16);
        parms.setRandomGeneratorFactory(new UniformRandomGeneratorFactory());
        {
            Context context = new Context(parms, false, SecurityLevelType.NONE);
            EncryptionParameterQualifiers qualifiers = context.firstContextData().getQualifiers();
            EncryptionParameterQualifiers keyQualifiers = context.keyContextData().getQualifiers();

            Assert.assertEquals(17L, context.firstContextData().getTotalCoeffModulus()[0]);
            Assert.assertEquals(697L, context.keyContextData().getTotalCoeffModulus()[0]);
            Assert.assertTrue(qualifiers.isParametersSet());
            Assert.assertTrue(qualifiers.usingFft);
            Assert.assertTrue(qualifiers.usingNtt);
            Assert.assertFalse(qualifiers.usingBatching);
            Assert.assertTrue(qualifiers.usingFastPlainLift);
            Assert.assertFalse(keyQualifiers.usingDescendingModulusChain);
            Assert.assertEquals(SecurityLevelType.NONE, qualifiers.securityLevel);
            Assert.assertTrue(context.isUsingKeySwitching());
        }

        // Parameters OK; no batching due to non-prime plain modulus
        parms.setPolyModulusDegree(4);
        parms.setCoeffModulus(new long[]{17, 41});
        parms.setPlainModulus(49);
        parms.setRandomGeneratorFactory(new UniformRandomGeneratorFactory());
        {
            Context context = new Context(parms, false, SecurityLevelType.NONE);
            Assert.assertEquals(697L, context.keyContextData().getTotalCoeffModulus()[0]);
            EncryptionParameterQualifiers qualifiers = context.firstContextData().getQualifiers();

            Assert.assertTrue(qualifiers.isParametersSet());
            Assert.assertTrue(qualifiers.usingFft);
            Assert.assertTrue(qualifiers.usingNtt);
            Assert.assertFalse(qualifiers.usingBatching);
            Assert.assertFalse(qualifiers.usingFastPlainLift);
            Assert.assertFalse(qualifiers.usingDescendingModulusChain);
            Assert.assertEquals(SecurityLevelType.NONE, qualifiers.securityLevel);
            Assert.assertFalse(context.isUsingKeySwitching());
        }

        // Parameters OK; batching enabled
        parms.setPolyModulusDegree(4);
        parms.setCoeffModulus(new long[]{17, 41});
        parms.setPlainModulus(73);
        parms.setRandomGeneratorFactory(new UniformRandomGeneratorFactory());
        {
            Context context = new Context(parms, false, SecurityLevelType.NONE);
            Assert.assertEquals(697L, context.keyContextData().getTotalCoeffModulus()[0]);
            EncryptionParameterQualifiers qualifiers = context.firstContextData().getQualifiers();

            Assert.assertTrue(qualifiers.isParametersSet());
            Assert.assertTrue(qualifiers.usingFft);
            Assert.assertTrue(qualifiers.usingNtt);
            Assert.assertTrue(qualifiers.usingBatching);
            Assert.assertFalse(qualifiers.usingFastPlainLift);
            Assert.assertFalse(qualifiers.usingDescendingModulusChain);
            Assert.assertEquals(SecurityLevelType.NONE, qualifiers.securityLevel);
            Assert.assertFalse(context.isUsingKeySwitching());
        }

        // Parameters OK; batching and fast plain lift enabled
        parms.setPolyModulusDegree(4);
        parms.setCoeffModulus(new long[]{137, 193});
        parms.setPlainModulus(73);
        parms.setRandomGeneratorFactory(new UniformRandomGeneratorFactory());
        {
            Context context = new Context(parms, false, SecurityLevelType.NONE);

            Assert.assertEquals(137L, context.firstContextData().getTotalCoeffModulus()[0]);
            Assert.assertEquals(26441L, context.keyContextData().getTotalCoeffModulus()[0]);

            EncryptionParameterQualifiers qualifiers = context.firstContextData().getQualifiers();
            EncryptionParameterQualifiers keyQualifiers = context.keyContextData().getQualifiers();

            Assert.assertTrue(qualifiers.isParametersSet());
            Assert.assertTrue(qualifiers.usingFft);
            Assert.assertTrue(qualifiers.usingNtt);
            Assert.assertTrue(qualifiers.usingBatching);
            Assert.assertTrue(qualifiers.usingFastPlainLift);
            Assert.assertFalse(keyQualifiers.usingDescendingModulusChain);
            Assert.assertEquals(SecurityLevelType.NONE, qualifiers.securityLevel);
            Assert.assertTrue(context.isUsingKeySwitching());
        }

        // Parameters OK; batching and fast plain lift enabled; nullptr RNG
        parms.setPolyModulusDegree(4);
        parms.setCoeffModulus(new long[]{137, 193});
        parms.setPlainModulus(73);
        parms.setRandomGeneratorFactory(new UniformRandomGeneratorFactory());
        {
            Context context = new Context(parms, false, SecurityLevelType.NONE);

            Assert.assertEquals(137L, context.firstContextData().getTotalCoeffModulus()[0]);
            Assert.assertEquals(26441L, context.keyContextData().getTotalCoeffModulus()[0]);

            EncryptionParameterQualifiers qualifiers = context.firstContextData().getQualifiers();
            EncryptionParameterQualifiers keyQualifiers = context.keyContextData().getQualifiers();

            Assert.assertTrue(qualifiers.isParametersSet());
            Assert.assertTrue(qualifiers.usingFft);
            Assert.assertTrue(qualifiers.usingNtt);
            Assert.assertTrue(qualifiers.usingBatching);
            Assert.assertTrue(qualifiers.usingFastPlainLift);
            Assert.assertFalse(keyQualifiers.usingDescendingModulusChain);
            Assert.assertEquals(SecurityLevelType.NONE, qualifiers.securityLevel);
            Assert.assertTrue(context.isUsingKeySwitching());
        }

        // Parameters not OK due to too small poly_modulus_degree and enforce_hes
        parms.setPolyModulusDegree(4);
        parms.setCoeffModulus(new long[]{137, 193});
        parms.setPlainModulus(73);
        parms.setRandomGeneratorFactory(new UniformRandomGeneratorFactory());
        {
            Context context = new Context(parms, false, SecurityLevelType.TC128);
            EncryptionParameterQualifiers qualifiers = context.firstContextData().getQualifiers();

            Assert.assertFalse(qualifiers.isParametersSet());
            Assert.assertEquals(ErrorType.INVALID_PARAMETERS_INSECURE, qualifiers.parameterError);
            Assert.assertEquals(SecurityLevelType.NONE, qualifiers.securityLevel);
            Assert.assertFalse(context.isUsingKeySwitching());
        }

        // Parameters not OK due to too large coeff_modulus and enforce_hes
        parms.setPolyModulusDegree(2048);
        parms.setCoeffModulus(CoeffModulus.BfvDefault(4096, SecurityLevelType.TC128));
        parms.setPlainModulus(73);
        parms.setRandomGeneratorFactory(new UniformRandomGeneratorFactory());
        {
            Context context = new Context(parms, false, SecurityLevelType.TC128);
            EncryptionParameterQualifiers qualifiers = context.firstContextData().getQualifiers();

            Assert.assertFalse(qualifiers.isParametersSet());
            Assert.assertEquals(ErrorType.INVALID_PARAMETERS_INSECURE, qualifiers.parameterError);
            Assert.assertEquals(SecurityLevelType.NONE, qualifiers.securityLevel);
            Assert.assertFalse(context.isUsingKeySwitching());
        }

        // Parameters OK; descending modulus chain
        parms.setPolyModulusDegree(4096);
        parms.setCoeffModulus(new long[]{0xffffee001L, 0xffffc4001L});
        parms.setPlainModulus(73);
        {
            Context context = new Context(parms, false, SecurityLevelType.TC128);
            EncryptionParameterQualifiers qualifiers = context.firstContextData().getQualifiers();

            Assert.assertTrue(qualifiers.isParametersSet());
            Assert.assertTrue(qualifiers.usingFft);
            Assert.assertTrue(qualifiers.usingNtt);
            Assert.assertFalse(qualifiers.usingBatching);
            Assert.assertTrue(qualifiers.usingFastPlainLift);
            Assert.assertTrue(qualifiers.usingDescendingModulusChain);
            Assert.assertEquals(SecurityLevelType.TC128, qualifiers.securityLevel);
            Assert.assertTrue(context.isUsingKeySwitching());
        }

        // Parameters OK; no standard security
        parms.setPolyModulusDegree(4096);
        parms.setCoeffModulus(new long[]{0x1ffffe0001L, 0xffffee001L, 0xffffc4001L});
        parms.setPlainModulus(73);
        {
            Context context = new Context(parms, false, SecurityLevelType.NONE);
            EncryptionParameterQualifiers qualifiers = context.firstContextData().getQualifiers();
            EncryptionParameterQualifiers keyQualifiers = context.keyContextData().getQualifiers();

            Assert.assertTrue(qualifiers.isParametersSet());
            Assert.assertTrue(qualifiers.usingFft);
            Assert.assertTrue(qualifiers.usingNtt);
            Assert.assertFalse(qualifiers.usingBatching);
            Assert.assertTrue(qualifiers.usingFastPlainLift);
            Assert.assertTrue(keyQualifiers.usingDescendingModulusChain);
            Assert.assertEquals(SecurityLevelType.NONE, qualifiers.securityLevel);
            Assert.assertTrue(context.isUsingKeySwitching());
        }

        // Parameters OK; using batching; no keyswitching
        parms.setPolyModulusDegree(2048);
        parms.setCoeffModulus(CoeffModulus.create(2048, new int[]{40}));
        parms.setPlainModulus(65537);
        {
            Context context = new Context(parms, false, SecurityLevelType.NONE);
            EncryptionParameterQualifiers qualifiers = context.firstContextData().getQualifiers();

            Assert.assertTrue(qualifiers.isParametersSet());
            Assert.assertTrue(qualifiers.usingFft);
            Assert.assertTrue(qualifiers.usingNtt);
            Assert.assertTrue(qualifiers.usingBatching);
            Assert.assertTrue(qualifiers.usingFastPlainLift);
            Assert.assertTrue(qualifiers.usingDescendingModulusChain);
            Assert.assertEquals(SecurityLevelType.NONE, qualifiers.securityLevel);
            Assert.assertFalse(context.isUsingKeySwitching());
        }
    }

    @Test
    public void modulusChainExpansion() {

        {
            EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
            parms.setPolyModulusDegree(4);
            parms.setCoeffModulus(new long[]{41, 137, 193, 65537});
            parms.setPlainModulus(73);

            Context context = new Context(parms, true, SecurityLevelType.NONE);
            Context.ContextData contextData = context.keyContextData();
            Assert.assertEquals(2, contextData.getChainIndex());
            Assert.assertEquals(71047416497L, contextData.getTotalCoeffModulus()[0]);
            Assert.assertNull(contextData.getPreContextData());
            Assert.assertEquals(contextData.getParmsId(), context.getKeyParmsId());

            Context.ContextData prevContextData = contextData;
            contextData = contextData.getNextContextData();
            Assert.assertEquals(1, contextData.getChainIndex());
            Assert.assertEquals(1084081L, contextData.getTotalCoeffModulus()[0]);
            Assert.assertEquals(contextData.getPreContextData().getParmsId(), prevContextData.getParmsId());

            prevContextData = contextData;
            contextData = contextData.getNextContextData();
            Assert.assertEquals(0, contextData.getChainIndex());
            Assert.assertEquals(5617L, contextData.getTotalCoeffModulus()[0]);
            Assert.assertEquals(contextData.getPreContextData().getParmsId(), prevContextData.getParmsId());

            Assert.assertNull(contextData.getNextContextData());
            Assert.assertEquals(contextData.getParmsId(), context.getLastParmsId());

            context = new Context(parms, false, SecurityLevelType.NONE);
            Assert.assertEquals(1, context.keyContextData().getChainIndex());
            Assert.assertEquals(0, context.firstContextData().getChainIndex());
            Assert.assertEquals(71047416497L, context.keyContextData().getTotalCoeffModulus()[0]);
            Assert.assertEquals(1084081L, context.firstContextData().getTotalCoeffModulus()[0]);

            Assert.assertNull(context.firstContextData().getNextContextData());
            Assert.assertNotNull(context.firstContextData().getPreContextData());
        }
    }

    @Test
    public void bfvParameterError() {

        SchemeType scheme = SchemeType.BFV;
        EncryptionParams parms = new EncryptionParams(scheme);
        Context context = new Context(parms, false, SecurityLevelType.NONE);
        EncryptionParameterQualifiers qualifiers = context.firstContextData().getQualifiers();

        qualifiers.parameterError = ErrorType.NONE;
        Assert.assertEquals(qualifiers.parameterErrorName(), "none");
        Assert.assertEquals(qualifiers.parameterErrorMessage(), "constructed but not yet validated");

        qualifiers.parameterError = ErrorType.SUCCESS;
        Assert.assertEquals(qualifiers.parameterErrorName(), "success");
        Assert.assertEquals(qualifiers.parameterErrorMessage(), "valid");

        qualifiers.parameterError = ErrorType.INVALID_COEFF_MODULUS_BIT_COUNT;
        Assert.assertEquals(qualifiers.parameterErrorName(), "invalid coeff modulus bit count");
        Assert.assertEquals(qualifiers.parameterErrorMessage(),  "coeffModulus's primes' bit counts are not bounded by USER_MOD_BIT_COUNT_MIN(MAX)");

        parms.setPolyModulusDegree(127);
        parms.setCoeffModulus(new long[] {17, 73});
        parms.setPlainModulus(41);
        parms.setRandomGeneratorFactory(new UniformRandomGeneratorFactory());

        context = new Context(parms, false, SecurityLevelType.NONE);
        Assert.assertFalse(context.isParametersSet());
        Assert.assertEquals(context.parametersErrorName(), "invalid poly modulus degree non power of two");
        Assert.assertEquals(context.parametersErrorMessage(), "polyModulusDegree is not a power of two");



    }


    @Test
    public void someSpecial() {

        ParmsIdType id1 = ParmsIdType.parmsIdZero();
        ParmsIdType id2 = id1.clone();

        System.out.println(id1.equals(id2));
        System.out.println(id1 == id2);


    }


}
