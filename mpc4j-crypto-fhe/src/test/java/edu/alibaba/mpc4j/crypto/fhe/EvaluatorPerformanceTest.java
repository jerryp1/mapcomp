package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.context.Context;
import edu.alibaba.mpc4j.crypto.fhe.keys.PublicKey;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.modulus.PlainModulus;
import edu.alibaba.mpc4j.crypto.fhe.params.EncryptionParams;
import edu.alibaba.mpc4j.crypto.fhe.params.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.rq.PolyArithmeticSmallModEfficiencyTest;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

/**
 * @author Qixian Zhou
 * @date 2023/10/22
 */
public class EvaluatorPerformanceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PolyArithmeticSmallModEfficiencyTest.class);

    /**
     * max loop num
     */
    private static final int MAX_LOOP_NUM = 100000;

    /**
     * time format
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.0000");
    /**
     * the stop watch
     */
    private static final StopWatch STOP_WATCH = new StopWatch();


    private static final int[] NS = new int[]{4096, 8192};


    @Test
    public void testPerformance() {


        for (int N : NS) {
            LOGGER.info("-----------------------------N={}---------------------------", N);
            LOGGER.info("{}\t{}",
                    "                name", "     time(ms)"
            );
            performance(N);
        }
    }


    public void performance(int N) {
        EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
        Modulus plainModulus = PlainModulus.batching(N, 20);
        parms.setPolyModulusDegree(N);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.BfvDefault(N));

        Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keyGenerator.createPublicKey(pk);

        Encryptor encryptor = new Encryptor(context, pk);
        EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);
        Evaluator evaluator = new Evaluator(context);
        Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());


        Ciphertext encrypted1 = new Ciphertext();
        Ciphertext encrypted2 = new Ciphertext();
        Ciphertext destination = new Ciphertext();

        Plaintext plain = new Plaintext();
        Plaintext plain1 = new Plaintext();
        Plaintext plain2 = new Plaintext();
        String hexPoly1;
        String hexPoly2;

        hexPoly1 = "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3";
        hexPoly2 = "1x^18 + 1x^16 + 1x^14 + 1x^9 + 1x^8 + 1x^5 + 1";
        plain1.fromHexPoly(hexPoly1);
        plain2.fromHexPoly(hexPoly2);
        encryptor.encrypt(plain1, encrypted1);
        encryptor.encrypt(plain2, encrypted2);

        // warm up
        for (int i = 0; i < MAX_LOOP_NUM; i++) {
            evaluator.add(encrypted1, encrypted2, destination);
        }
        double time;

        STOP_WATCH.start();
        for (int i = 0; i < MAX_LOOP_NUM; i++) {
            evaluator.add(encrypted1, encrypted2, destination);
        }
        STOP_WATCH.stop();
        time = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / MAX_LOOP_NUM;
        STOP_WATCH.reset();

        // output
        LOGGER.info(
                "{}\t{}",
                StringUtils.leftPad("add", 20),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
        );

        STOP_WATCH.start();
        for (int i = 0; i < MAX_LOOP_NUM; i++) {
            evaluatorParallel.add(encrypted1, encrypted2, destination);
        }
        STOP_WATCH.stop();
        time = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / MAX_LOOP_NUM;
        STOP_WATCH.reset();

        // output
        LOGGER.info(
                "{}\t{}",
                StringUtils.leftPad("add(Parallel)", 20),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
        );


    }


}
