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
import org.junit.Test;

import java.util.Arrays;

/**
 * @author Qixian Zhou
 * @date 2023/10/11
 */
public class Encoders_2 {


    @Test
    public void exampleBatchEncoder() {


        EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
        int polyModulusDegree = 8192;
        parms.setPolyModulusDegree(polyModulusDegree);
        parms.setCoeffModulus(CoeffModulus.bfvDefault(polyModulusDegree));
        parms.setPlainModulus(PlainModulus.batching(polyModulusDegree, 20));

        Context context = new Context(parms);
        Utils.printParameters(context);


        EncryptionParameterQualifiers qualifiers = context.firstContextData().getQualifiers();
        System.out.println(
                "Batching enabled: "
                        + qualifiers.isUsingBatching()
        );

        KeyGenerator keygen = new KeyGenerator(context);
        SecretKey secretKey = keygen.getSecretKey();
        PublicKey publicKey = new PublicKey();
        keygen.createPublicKey(publicKey);

        RelinKeys relinKeys = new RelinKeys();
        keygen.createRelinKeys(relinKeys);

        Encryptor encryptor = new Encryptor(context, publicKey);
        Evaluator evaluator = new Evaluator(context);
        Decryptor decryptor = new Decryptor(context, secretKey);


        BatchEncoder batchEncoder = new BatchEncoder(context);
        int sloutCount = batchEncoder.slotCount();
        int rowSize = sloutCount / 2;

        System.out.println(
                "Plaintext matrix row size: "
                        + rowSize
        );
          /*
            The matrix plaintext is simply given to BatchEncoder as a flattened vector
            of numbers. The first `row_size' many numbers form the first row, and the
            rest form the second row. Here we create the following matrix:

                [ 0,  1,  2,  3,  0,  0, ...,  0 ]
                [ 4,  5,  6,  7,  0,  0, ...,  0 ]
    */

        long[] podMatrix = new long[sloutCount];
        podMatrix[0] = 0;
        podMatrix[1] = 1;
        podMatrix[2] = 2;
        podMatrix[3] = 3;
        podMatrix[rowSize] = 4;
        podMatrix[rowSize + 1] = 5;
        podMatrix[rowSize + 2] = 6;
        podMatrix[rowSize + 3] = 7;

        System.out.println(
                "Input plaintext matrix:"
        );
        System.out.println(
                Arrays.toString(
                        Arrays.copyOfRange(podMatrix, 0, rowSize)
                )
        );
        System.out.println(
                Arrays.toString(
                        Arrays.copyOfRange(podMatrix, rowSize, 2 * rowSize)
                )
        );


        Plaintext plainMatrix = new Plaintext();
        System.out.println(
                "Encode plaintext matrix:"
        );
        // 当作 uint64 进行编码
        batchEncoder.encode(podMatrix, plainMatrix);

        long[] podResult = new long[sloutCount];
        System.out.println(
                "    + Decode plaintext matrix ...... Correct."
        );
        batchEncoder.decode(plainMatrix, podResult);
        System.out.println(
                Arrays.toString(
                        Arrays.copyOfRange(podResult, 0, rowSize)
                )
                        + "\n" +

                        Arrays.toString(
                                Arrays.copyOfRange(podResult, rowSize, 2 * rowSize)
                        )
        );


        Ciphertext encryptedMatrix = new Ciphertext();
        System.out.println(
                "Encrypt plain_matrix to encrypted_matrix."
        );
        encryptor.encrypt(plainMatrix, encryptedMatrix);
        System.out.println(
                "    + Noise budget in encrypted_matrix: "
                        + decryptor.invariantNoiseBudget(encryptedMatrix)
                        + " bits"
        );

        long[] podMatrix2 = new long[sloutCount];
        for (int i = 0; i < sloutCount; i++) {
            podMatrix2[i] = ((i & 0x1) + 1);
        }
        Plaintext plainMatrix2 = new Plaintext();
        batchEncoder.encode(podMatrix2, plainMatrix2);
        System.out.println(
                "Second input plaintext matrix:"
        );
        System.out.println(
                Arrays.toString(
                        Arrays.copyOfRange(podMatrix2, 0, rowSize))
                        + "\n"
                        + Arrays.toString(
                        Arrays.copyOfRange(podMatrix2, rowSize, 2 * rowSize)
                )
        );

        System.out.println(
                "Sum, square, and relinearize."
        );

        evaluator.addPlainInplace(encryptedMatrix, plainMatrix2);
        evaluator.squareInplace(encryptedMatrix);
        evaluator.reLinearizeInplace(encryptedMatrix, relinKeys);

        System.out.println(
                "    + Noise budget in result: "
                        + decryptor.invariantNoiseBudget(encryptedMatrix)
                        + " bits"
        );

        Plaintext plainResult = new Plaintext();
        System.out.println("Decrypt and decode result.");
        decryptor.decrypt(encryptedMatrix, plainResult);
        batchEncoder.decode(plainResult, podResult);
        System.out.println(
                "    + Result plaintext matrix ...... Correct."
        );
        System.out.println(
                Arrays.toString(
                        Arrays.copyOfRange(podResult, 0, rowSize)
                )
                        + "\n" +
                        Arrays.toString(
                                Arrays.copyOfRange(podResult, rowSize, 2 * rowSize)
                        )

        );

    }

}
