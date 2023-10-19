package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.context.Context;
import edu.alibaba.mpc4j.crypto.fhe.examples.Utils;
import edu.alibaba.mpc4j.crypto.fhe.keys.GaloisKeys;
import edu.alibaba.mpc4j.crypto.fhe.keys.PublicKey;
import edu.alibaba.mpc4j.crypto.fhe.keys.RelinKeys;
import edu.alibaba.mpc4j.crypto.fhe.keys.SecretKey;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.modulus.PlainModulus;
import edu.alibaba.mpc4j.crypto.fhe.params.EncryptionParams;
import edu.alibaba.mpc4j.crypto.fhe.params.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.rq.PolyArithmeticSmallModEfficiencyTest;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintArithmetic;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * @author Qixian Zhou
 * @date 2023/10/12
 */
public class BfvPerformanceTest {


    private static final Logger LOGGER = LoggerFactory.getLogger(PolyArithmeticSmallModEfficiencyTest.class);

    /**
     * max loop num
     */
    private static final int MAX_LOOP_NUM = 1000;

    /**
     * time format
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.0000");
    /**
     * the stop watch
     */
    private static final StopWatch STOP_WATCH = new StopWatch();


    private static final int[] NS = new int[] {4096, 8192};


    @Test
    public void testPerformance() {


        for(int N: NS) {
            LOGGER.info("-----------------------------N={}---------------------------", N);
            LOGGER.info("{}\t{}",
                    "                name", "     time(ms)"
            );
            testBfvEfficiency(N);
        }


    }

    private void testBfvEfficiency(int N) {

        double time;

        EncryptionParams parms;
        Context context;



        STOP_WATCH.start();

        parms = new EncryptionParams(SchemeType.BFV);
        parms.setPolyModulusDegree(N);
        parms.setCoeffModulus(CoeffModulus.BfvDefault(N));
        parms.setPlainModulus(PlainModulus.batching(N, 20));
        context = new Context(parms);

        STOP_WATCH.stop();
        time = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
        STOP_WATCH.reset();

        // output
        LOGGER.info(
                "{}\t{}",
                StringUtils.leftPad("ContextGen", 20),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
        );

        // warm up
//        for (int i = 0; i < 10; i++) {
//            KeyGenerator keygen = new KeyGenerator(context);
//        }


        STOP_WATCH.start();
        for (int i = 0; i < MAX_LOOP_NUM; i++) {
            KeyGenerator keygen = new KeyGenerator(context);
        }
        STOP_WATCH.stop();
        time = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / MAX_LOOP_NUM;
        STOP_WATCH.reset();
        // output
        LOGGER.info(
                "{}\t{}",
                StringUtils.leftPad("SkGen", 20),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
        );



        KeyGenerator keygen = new KeyGenerator(context);
        SecretKey secretKey = keygen.getSecretKey();


        STOP_WATCH.start();
        for (int i = 0; i < MAX_LOOP_NUM; i++) {
            PublicKey publicKey = new PublicKey();
            keygen.createPublicKey(publicKey);
        }
        STOP_WATCH.stop();
        time = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / MAX_LOOP_NUM;
        STOP_WATCH.reset();
        // output
        LOGGER.info(
                "{}\t{}",
                StringUtils.leftPad("PkGen", 20),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
        );



        PublicKey publicKey = new PublicKey();
        keygen.createPublicKey(publicKey);


        STOP_WATCH.start();
        for (int i = 0; i < MAX_LOOP_NUM; i++) {
            RelinKeys relinKeys = new RelinKeys();
            keygen.createRelinKeys(relinKeys);
        }
        STOP_WATCH.stop();
        time = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / MAX_LOOP_NUM;
        STOP_WATCH.reset();
        // output
        LOGGER.info(
                "{}\t{}",
                StringUtils.leftPad("RelinerKeyGen", 20),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
        );


        RelinKeys relinKeys = new RelinKeys();
        keygen.createRelinKeys(relinKeys);

        STOP_WATCH.start();
        for (int i = 0; i < 10; i++) {
            GaloisKeys galoisKeys = new GaloisKeys();
            keygen.createGaloisKeys(galoisKeys);
        }
        STOP_WATCH.stop();
        time = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / 10;
        STOP_WATCH.reset();
        // output
        LOGGER.info(
                "{}\t{}",
                StringUtils.leftPad("GaloisKeysGen", 20),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
        );


        GaloisKeys galoisKeys = new GaloisKeys();
        keygen.createGaloisKeys(galoisKeys);



        Encryptor encryptor = new Encryptor(context, publicKey);
        Evaluator evaluator = new Evaluator(context);
        Decryptor decryptor = new Decryptor(context, secretKey);

        long x = 6;
        Plaintext xPlain = new Plaintext(Utils.uint64ToHexString(x));
        Ciphertext xEncrypted = new Ciphertext();
        Ciphertext destination = new Ciphertext();


        STOP_WATCH.start();
        for (int i = 0; i < MAX_LOOP_NUM; i++) {
            encryptor.encrypt(xPlain, xEncrypted);
        }
        STOP_WATCH.stop();
        time = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / MAX_LOOP_NUM;
        STOP_WATCH.reset();
        // output
        LOGGER.info(
                "{}\t{}",
                StringUtils.leftPad("Enc", 20),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
        );


        Plaintext xDecrypted = new Plaintext();

        STOP_WATCH.start();
        for (int i = 0; i < MAX_LOOP_NUM; i++) {
            decryptor.decrypt(xEncrypted, xDecrypted);
        }
        STOP_WATCH.stop();
        time = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / MAX_LOOP_NUM;
        STOP_WATCH.reset();
        // output
        LOGGER.info(
                "{}\t{}",
                StringUtils.leftPad("Dec", 20),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
        );


        Assert.assertEquals(
                xPlain.toString(),
                xDecrypted.toString()
        );


        long x2 = 6;
        Plaintext xPlain2 = new Plaintext(Utils.uint64ToHexString(x2));
        Ciphertext xEncrypted2 = new Ciphertext();
        encryptor.encrypt(xPlain2, xEncrypted2);


        STOP_WATCH.start();
        for (int i = 0; i < MAX_LOOP_NUM; i++) {
            evaluator.add(xEncrypted, xEncrypted2, destination);
        }
        STOP_WATCH.stop();
        time = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / MAX_LOOP_NUM;
        STOP_WATCH.reset();
        // output
        LOGGER.info(
                "{}\t{}",
                StringUtils.leftPad("C+C", 20),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
        );


        STOP_WATCH.start();
        for (int i = 0; i < MAX_LOOP_NUM; i++) {
            evaluator.multiply(xEncrypted, xEncrypted2, destination);
        }
        STOP_WATCH.stop();
        time = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / MAX_LOOP_NUM;
        STOP_WATCH.reset();
        // output
        LOGGER.info(
                "{}\t{}",
                StringUtils.leftPad("C*C", 20),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
        );



        STOP_WATCH.start();
        for (int i = 0; i < MAX_LOOP_NUM; i++) {
            evaluator.reLinearize(destination, relinKeys, destination);
        }
        STOP_WATCH.stop();
        time = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / MAX_LOOP_NUM;
        STOP_WATCH.reset();
        // output
        LOGGER.info(
                "{}\t{}",
                StringUtils.leftPad("relinear", 20),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
        );

        STOP_WATCH.start();
        for (int i = 0; i < MAX_LOOP_NUM; i++) {
            evaluator.rotateRows(xEncrypted, 3, galoisKeys, destination);
        }
        STOP_WATCH.stop();
        time = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / MAX_LOOP_NUM;
        STOP_WATCH.reset();
        // output
        LOGGER.info(
                "{}\t{}",
                StringUtils.leftPad("rotate", 20),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
        );

    }







}
