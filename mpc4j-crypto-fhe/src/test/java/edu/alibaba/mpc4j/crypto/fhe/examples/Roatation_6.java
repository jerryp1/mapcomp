package edu.alibaba.mpc4j.crypto.fhe.examples;

import edu.alibaba.mpc4j.crypto.fhe.*;
import edu.alibaba.mpc4j.crypto.fhe.context.Context;
import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameterQualifiers;
import edu.alibaba.mpc4j.crypto.fhe.keys.GaloisKeys;
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
public class Roatation_6 {


    @Test
    public void exampleRotationBfv() {


        EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
        int polyModulusDegree = 8192;
        parms.setPolyModulusDegree(polyModulusDegree);
        parms.setCoeffModulus(CoeffModulus.BfvDefault(polyModulusDegree));
        parms.setPlainModulus(PlainModulus.batching(polyModulusDegree, 20));


        Context context = new Context(parms);
        Utils.printParameters(context);

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
                "Encode and encrypt."
        );
        // 当作 uint64 进行编码
        batchEncoder.encode(podMatrix, plainMatrix);
        Ciphertext encryptedMatrix = new Ciphertext();
        encryptor.encrypt(plainMatrix, encryptedMatrix);
        System.out.println(
                "    + Noise budget in fresh encryption: "
                        + decryptor.invariantNoiseBudget(encryptedMatrix)
                        + " bits"
        );


        GaloisKeys galoisKeys = new GaloisKeys();
        keygen.createGaloisKeys(galoisKeys);


        System.out.println(
                "Rotate rows 3 steps left."
        );

        evaluator.rotateRowsInplace(encryptedMatrix, 3, galoisKeys);
        Plaintext plainResult = new Plaintext();
        System.out.println(
                "    + Noise budget after rotation: "
                        + decryptor.invariantNoiseBudget(encryptedMatrix)
                        + " bits"
        );
        System.out.println(
                "    + Decrypt and decode ...... Correct."
        );
        decryptor.decrypt(encryptedMatrix, plainResult);
        batchEncoder.decode(plainResult, podMatrix);
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


        /*
        We can also rotate the columns, i.e., swap the rows.
         */

        System.out.println(
                "Rotate columns."
        );
        evaluator.rotateColumnsInplace(encryptedMatrix, galoisKeys);
        System.out.println(
                "    + Noise budget after rotation: "
                        + decryptor.invariantNoiseBudget(encryptedMatrix)
                        + " bits"
        );
        System.out.println(
                "    + Decrypt and decode ...... Correct."
        );
        decryptor.decrypt(encryptedMatrix, plainResult);
        batchEncoder.decode(plainResult, podMatrix);
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


        System.out.println(
                "Rotate rows 4 steps right."
        );

        evaluator.rotateRowsInplace(encryptedMatrix, -4, galoisKeys);

        System.out.println(
                "    + Noise budget after rotation: "
                        + decryptor.invariantNoiseBudget(encryptedMatrix)
                        + " bits"
        );
        System.out.println(
                "    + Decrypt and decode ...... Correct."
        );
        decryptor.decrypt(encryptedMatrix, plainResult);
        batchEncoder.decode(plainResult, podMatrix);
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

    }


}
