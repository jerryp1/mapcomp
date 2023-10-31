package edu.alibaba.mpc4j.crypto.fhe.examples;


import edu.alibaba.mpc4j.crypto.fhe.*;
import edu.alibaba.mpc4j.crypto.fhe.context.Context;
import edu.alibaba.mpc4j.crypto.fhe.keys.PublicKey;
import edu.alibaba.mpc4j.crypto.fhe.keys.RelinKeys;
import edu.alibaba.mpc4j.crypto.fhe.keys.SecretKey;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.params.EncryptionParams;
import edu.alibaba.mpc4j.crypto.fhe.params.SchemeType;
import org.junit.Test;

public class BfvBasics_1 {


    @Test
    public void exampleBfvBasics() {

        EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
        int polyModulusDegree = 4096;
        parms.setPolyModulusDegree(polyModulusDegree);

        parms.setCoeffModulus(CoeffModulus.BfvDefault(polyModulusDegree));

        parms.setPlainModulus(1024);

        Context context = new Context(parms);

        System.out.println("~~~~~~ A naive way to calculate 4(x^2+1)(x+1)^2. ~~~~~~");

        KeyGenerator keygen = new KeyGenerator(context);
        SecretKey secretKey = keygen.getSecretKey();
        PublicKey publicKey = new PublicKey();
        keygen.createPublicKey(publicKey);

        Encryptor encryptor = new Encryptor(context, publicKey);
        Evaluator evaluator = new Evaluator(context);
        Decryptor decryptor = new Decryptor(context, secretKey);

        long x = 6;
        Plaintext xPlain = new Plaintext(Utils.uint64ToHexString(x));

        System.out.println("Express x = " + x + " as a plaintext polynomial 0x" + xPlain.toString() + ".");

        Ciphertext xEncrypted = new Ciphertext();

        System.out.println(
                "Encrypt x_plain to x_encrypted."
        );

        encryptor.encrypt(xPlain, xEncrypted);

        System.out.println(
                "    + size of freshly encrypted x: "
                + xEncrypted.getSize()
        );

        System.out.println(
                "    + noise budget in freshly encrypted x: "
                + decryptor.invariantNoiseBudget(xEncrypted)
        );


        Plaintext xDecrypted = new Plaintext();
        System.out.println(
                "    + decryption of x_encrypted:"
        );
        decryptor.decrypt(xEncrypted, xDecrypted);

        System.out.println(xDecrypted.toString() + " ...... Correct.");

        System.out.println("Compute x_sq_plus_one (x^2+1).");

        Ciphertext xSqPlusOne = new Ciphertext();
        evaluator.square(xEncrypted, xSqPlusOne);
        Plaintext plainOne = new Plaintext("1");
        evaluator.addPlainInplace(xSqPlusOne, plainOne);

        System.out.println("    + size of x_sq_plus_one: " + xSqPlusOne.getSize());
        System.out.println( "    + noise budget in x_sq_plus_one: " +
                decryptor.invariantNoiseBudget(xSqPlusOne)
                + " bits"
        );

        Plaintext decryptedResult = new Plaintext();
        decryptor.decrypt(xSqPlusOne, decryptedResult);
        System.out.println("0x"
            + decryptedResult
            + "...... Correct"
        );

        System.out.println("Compute x_plus_one_sq ((x+1)^2)");
        Ciphertext xPlusOneSq = new Ciphertext();
        evaluator.addPlain(xEncrypted, plainOne, xPlusOneSq);
        evaluator.squareInplace(xPlusOneSq);
        System.out.println(
                "    + size of x_plus_one_sq: "
                + xPlusOneSq.getSize()
        );
        System.out.println(
                "    + noise budget in x_plus_one_sq: "
                + decryptor.invariantNoiseBudget(xPlusOneSq)
                + " bits"
        );

        System.out.println(
                "    + decryption of x_plus_one_sq: "
        );
        decryptor.decrypt(xPlusOneSq, decryptedResult);
        System.out.println(
                "0x"
                + decryptedResult
                + " ...... Correct."
        );

        System.out.println("Compute encrypted_result (4(x^2+1)(x+1)^2).");
        Ciphertext encryptedResult = new Ciphertext();
        Plaintext plainFour = new Plaintext("4");
        evaluator.multiplyPlainInplace(xSqPlusOne, plainFour);
        evaluator.multiply(xSqPlusOne, xPlusOneSq, encryptedResult);
        System.out.println(
                "    + size of encrypted_result: " + encryptedResult.getSize()
        );
        System.out.println(
                "    + noise budget in encrypted_result: "
                + decryptor.invariantNoiseBudget(encryptedResult)
                + " bits"
        );
        System.out.println(
                "NOTE: Decryption can be incorrect if noise budget is zero."
        );


        System.out.println(
                "~~~~~~ A better way to calculate 4(x^2+1)(x+1)^2. ~~~~~~"
        );

        System.out.println(
                "Generate relinearization keys."
        );
        RelinKeys relinKeys = new RelinKeys();
        keygen.createRelinKeys(relinKeys);

        System.out.println(
                "Compute and relinearize x_squared (x^2),"
        );

        System.out.println(
                ' ' + "then compute x_sq_plus_one (x^2+1)"
        );
        Ciphertext xSquared = new Ciphertext();
        evaluator.square(xEncrypted, xSquared);

        System.out.println(
                "    + size of x_squared: "
                + xSquared.getSize()
        );

        evaluator.reLinearizeInplace(xSquared, relinKeys);

        System.out.println(
                "    + size of x_squared (after relinearization): " +
                        xSquared.getSize()
        );

        evaluator.addPlain(xSquared, plainOne, xSqPlusOne);
        System.out.println(
                "    + noise budget in x_sq_plus_one: "
                + decryptor.invariantNoiseBudget(xSqPlusOne)
                + " bits"
        );

        System.out.println(
                "    + decryption of x_sq_plus_one: "
        );
        decryptor.decrypt(xSqPlusOne, decryptedResult);
        System.out.println(
                "0x"
                + decryptedResult
                + " ...... Correct."
        );

        Ciphertext xPlusOne = new Ciphertext();
        System.out.println(
                "Compute x_plus_one (x+1),"
        );
        System.out.println(
                "then compute and relinearize x_plus_one_sq ((x+1)^2)."
        );
        evaluator.addPlain(xEncrypted, plainOne, xPlusOne);
        evaluator.square(xPlusOne, xPlusOneSq);
        System.out.println(
                "    + size of x_plus_one_sq: "
                + xPlusOneSq.getSize()
        );

        evaluator.reLinearizeInplace(xPlusOneSq, relinKeys);
        System.out.println(
                "    + size of x_plus_one_sq: "
                + xPlusOneSq.getSize()
        );
        System.out.println(
                "    + noise budget in x_plus_one_sq: "
                + decryptor.invariantNoiseBudget(xPlusOneSq)
                + " bits"
        );

        System.out.println(
                "    + decryption of x_plus_one_sq: "
        );
        decryptor.decrypt(xPlusOneSq, decryptedResult);

        System.out.println(
                "0x"
                + decryptedResult
                + " ...... Correct."
        );

        System.out.println(
                "Compute and relinearize encrypted_result (4(x^2+1)(x+1)^2)."
        );

        evaluator.multiplyPlainInplace(xSqPlusOne, plainFour);
        evaluator.multiply(xSqPlusOne, xPlusOneSq, encryptedResult);
        System.out.println(
                "    + size of encrypted_result: "
                + encryptedResult.getSize()
        );
        evaluator.reLinearizeInplace(encryptedResult, relinKeys);
        System.out.println(
                "    + size of encrypted_result (after relinearization): "
                + encryptedResult.getSize()
        );

        System.out.println(
                "    + noise budget in encrypted_result: "
                + decryptor.invariantNoiseBudget(encryptedResult)
                + " bits"
        );

        System.out.println(
                "NOTE: Notice the increase in remaining noise budget."
        );


        System.out.println(
                "Decrypt encrypted_result (4(x^2+1)(x+1)^2)."
        );
        decryptor.decrypt(encryptedResult, decryptedResult);
        System.out.println(
                "    + decryption of 4(x^2+1)(x+1)^2 = 0x"
                + decryptedResult
                + " ...... Correct."
        );


        System.out.println(
                "An example of invalid parameters"
        );

        parms.setPolyModulusDegree(2048);
        context = new Context(parms);
        System.out.println(
                "Parameter validation (failed): "
                + context.parametersErrorMessage()
        );

    }


}
