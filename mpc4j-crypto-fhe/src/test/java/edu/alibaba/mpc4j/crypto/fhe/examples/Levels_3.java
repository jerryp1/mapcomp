package edu.alibaba.mpc4j.crypto.fhe.examples;

import edu.alibaba.mpc4j.crypto.fhe.*;
import edu.alibaba.mpc4j.crypto.fhe.context.Context;
import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameterQualifiers;
import edu.alibaba.mpc4j.crypto.fhe.keys.PublicKey;
import edu.alibaba.mpc4j.crypto.fhe.keys.RelinKeys;
import edu.alibaba.mpc4j.crypto.fhe.keys.SecretKey;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.modulus.PlainModulus;
import edu.alibaba.mpc4j.crypto.fhe.params.EncryptionParams;
import edu.alibaba.mpc4j.crypto.fhe.params.SchemeType;
import org.checkerframework.checker.units.qual.C;
import org.junit.Test;

import java.util.Arrays;

/**
 * @author Qixian Zhou
 * @date 2023/10/11
 */
public class Levels_3 {


    @Test
    public void exampleLevels() {


        EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
        int polyModulusDegree = 8192;
        parms.setPolyModulusDegree(polyModulusDegree);
        parms.setCoeffModulus(CoeffModulus.create(polyModulusDegree, new int[]{50, 30, 30, 50, 50}));
        parms.setPlainModulus(PlainModulus.batching(polyModulusDegree, 20));

        Context context = new Context(parms);
        // 输出
        Utils.printParameters(context);

        System.out.println(
                "Print the modulus switching chain."
        );

        Context.ContextData contextData = context.keyContextData();
        System.out.println(
                "----> Level (chain index): "
                        + contextData.getChainIndex()
                        + " ...... key_context_data()"
        );
        System.out.println(
                "      parms_id: " + contextData.getParmsId()
        );

        System.out.println(
                "      coeff_modulus primes: "
                        + Arrays.toString(Arrays.stream(contextData.getParms().getCoeffModulus())
                        .map(n -> Long.toHexString(n.getValue())).toArray(String[]::new))
        );


        contextData = context.firstContextData();
        while (contextData != null) {

            System.out.println(
                    " Level (chain index): "
                            + contextData.getChainIndex()
            );

            if (contextData.getParmsId().equals(context.getFirstParmsId())) {
                System.out.println(
                        " ...... first_context_data()"
                );
            } else if (contextData.getParmsId().equals(context.getLastParmsId())) {
                System.out.println(
                        " ...... last_context_data()"
                );
            } else {
                System.out.println();
            }
            System.out.println(
                    "      parms_id: " + contextData.getParmsId()
            );

            System.out.println(
                    "      coeff_modulus primes: "
                            + Arrays.toString(Arrays.stream(contextData.getParms().getCoeffModulus())
                            .map(n -> Long.toHexString(n.getValue())).toArray(String[]::new))
            );

            contextData = contextData.getNextContextData();
        }

        System.out.println(
                " End of chain reached"
        );


        KeyGenerator keygen = new KeyGenerator(context);
        SecretKey secretKey = keygen.getSecretKey();
        PublicKey publicKey = new PublicKey();
        keygen.createPublicKey(publicKey);

        RelinKeys relinKeys = new RelinKeys();
        keygen.createRelinKeys(relinKeys);


        System.out.println(
                "Print the parameter IDs of generated elements."
        );

        System.out.println(
                "    + public_key:  " + publicKey.parmsId()
        );
        System.out.println(
                "    + secret_key:  " + secretKey.parmsId()
        );
        System.out.println(
                "    + relin_keys:  " + relinKeys.parmsId()
        );


        Encryptor encryptor = new Encryptor(context, publicKey);
        Evaluator evaluator = new Evaluator(context);
        Decryptor decryptor = new Decryptor(context, secretKey);


        Plaintext plain = new Plaintext("1x^3 + 2x^2 + 3x^1 + 4");
        Ciphertext encrypted = new Ciphertext();
        encryptor.encrypt(plain, encrypted);
        System.out.println(
                "    + plain:       "
                        + plain.getParmsId() + "(not set in BFV)"
        );
        System.out.println(
                "    + encrypted:   "
                        + encrypted.getParmsId()
        );

        System.out.println(
                "Perform modulus switching on encrypted and print."
        );

        contextData = context.firstContextData();
        System.out.println(
                "---->"
        );
        while (contextData.getNextContextData() != null) {

            System.out.println(
                    " Level (chain index): " + contextData.getChainIndex()
            );

            System.out.println(
                    "      parms_id of encrypted: "
                            + encrypted.getParmsId()
            );
            System.out.println(
                    "      Noise budget at this level: "
                            + decryptor.invariantNoiseBudget(encrypted)
                            + " bits"
            );
            System.out.println("\\");
            System.out.println("\\-->");
            evaluator.modSwitchToNextInplace(encrypted);
            contextData = contextData.getNextContextData();
        }

        System.out.println(
                " Level (chain index): " + contextData.getChainIndex()
        );

        System.out.println(
                "      parms_id of encrypted: "
                        + encrypted.getParmsId()
        );
        System.out.println(
                "      Noise budget at this level: "
                        + decryptor.invariantNoiseBudget(encrypted)
                        + " bits"
        );
        System.out.println("\\");
        System.out.println("\\-->");
        System.out.println("End of chain reached");

        System.out.println(
                "Decrypt still works after modulus switching."
        );
        decryptor.decrypt(encrypted, plain);
        System.out.println(
                "    + Decryption of encrypted: "
                        + plain
                        + " ...... Correct."
        );


        System.out.println(
                "Computation is more efficient with modulus switching."
        );

        System.out.println(
                "Compute the 8th power."
        );

        encryptor.encrypt(plain, encrypted);
        System.out.println(
                "    + Noise budget fresh:                   "
                        + decryptor.invariantNoiseBudget(encrypted)
                        + " bits"
        );

        evaluator.squareInplace(encrypted);
        evaluator.reLinearizeInplace(encrypted, relinKeys);
        System.out.println(
                "    + Noise budget of the 2nd power:         "
                        + decryptor.invariantNoiseBudget(encrypted)
                        + " bits"
        );

        evaluator.squareInplace(encrypted);
        evaluator.reLinearizeInplace(encrypted, relinKeys);
        System.out.println(
                "    + Noise budget of the 4th power:         "
                        + decryptor.invariantNoiseBudget(encrypted)
                        + " bits"
        );

        evaluator.modSwitchToNextInplace(encrypted);
        System.out.println(
                "    + Noise budget after modulus switching:  "
                        + decryptor.invariantNoiseBudget(encrypted)
                        + " bits"
        );

        evaluator.squareInplace(encrypted);
        evaluator.reLinearizeInplace(encrypted, relinKeys);
        System.out.println(
                "    + Noise budget of the 8th power:         "
                        + decryptor.invariantNoiseBudget(encrypted)
                        + " bits"
        );
        evaluator.modSwitchToNextInplace(encrypted);
        System.out.println(
                "    + Noise budget after modulus switching:  "
                        + decryptor.invariantNoiseBudget(encrypted)
                        + " bits"
        );


        decryptor.decrypt(encrypted, plain);
        System.out.println(
                "    + Decryption of the 8th power (hexadecimal) ...... Correct."
        );
        System.out.println(plain);


        context = new Context(parms, false);
        System.out.println(
                "Optionally disable modulus switching chain expansion."
        );

        System.out.println(
                "Print the modulus switching chain."
        );
        System.out.println(
                "---->"
        );

        for (contextData = context.keyContextData(); contextData != null; contextData = contextData.getNextContextData()) {


            System.out.println(
                    " Level (chain index): "
                            + contextData.getChainIndex()
            );

            if (contextData.getParmsId().equals(context.getFirstParmsId())) {
                System.out.println(
                        " ...... first_context_data()"
                );
            } else if (contextData.getParmsId().equals(context.getLastParmsId())) {
                System.out.println(
                        " ...... last_context_data()"
                );
            } else {
                System.out.println();
            }
            System.out.println(
                    "      parms_id: " + contextData.getParmsId()
            );

            System.out.println(
                    "      coeff_modulus primes: "
                            + Arrays.toString(Arrays.stream(contextData.getParms().getCoeffModulus())
                            .map(n -> Long.toHexString(n.getValue())).toArray(String[]::new))
            );

            System.out.println("\\");
            System.out.println("\\--->");
        }


        System.out.println(
                " End of chain reached"
        );

    }

}
