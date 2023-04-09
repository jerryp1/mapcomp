package edu.alibaba.mpc4j.s2pc.pcg.ot.not;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * 1-out-of-n OT test.
 *
 * @author Weiran Liu
 * @date 2023/4/9
 */
public class NotOutputTest {
    /**
     * minimal num
     */
    private static final int MIN_NUM = 1;
    /**
     * maximal num
     */
    private static final int MAX_NUM = 64;
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * default n
     */
    private static final int DEFAULT_N = 9;

    @Test
    public void testIllegalSenderOutputs() {
        // create a sender output with num = 0
        Assert.assertThrows(AssertionError.class, () -> NotSenderOutput.create(DEFAULT_N, new byte[0][][]));
        // create a sender output with short rs length
        Assert.assertThrows(AssertionError.class, () -> {
            byte[][][] rsArray = IntStream.range(0, MAX_NUM)
                .mapToObj(index ->
                    IntStream.range(0, DEFAULT_N)
                        .mapToObj(choice -> {
                            byte[] ri = new byte[CommonConstants.BLOCK_BYTE_LENGTH - 1];
                            SECURE_RANDOM.nextBytes(ri);
                            return ri;
                        })
                        .toArray(byte[][]::new))
                .toArray(byte[][][]::new);
            NotSenderOutput.create(DEFAULT_N, rsArray);
        });
        // create a sender output with large rs length
        Assert.assertThrows(AssertionError.class, () -> {
            byte[][][] rsArray = IntStream.range(0, MAX_NUM)
                .mapToObj(index ->
                    IntStream.range(0, DEFAULT_N)
                        .mapToObj(choice -> {
                            byte[] ri = new byte[CommonConstants.BLOCK_BYTE_LENGTH + 1];
                            SECURE_RANDOM.nextBytes(ri);
                            return ri;
                        })
                        .toArray(byte[][]::new))
                .toArray(byte[][][]::new);
            NotSenderOutput.create(DEFAULT_N, rsArray);
        });
        // create a sender output with less rs
        Assert.assertThrows(AssertionError.class, () -> {
            byte[][][] rsArray = IntStream.range(0, MAX_NUM)
                .mapToObj(index ->
                    IntStream.range(0, DEFAULT_N - 1)
                        .mapToObj(choice -> {
                            byte[] ri = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                            SECURE_RANDOM.nextBytes(ri);
                            return ri;
                        })
                        .toArray(byte[][]::new))
                .toArray(byte[][][]::new);
            NotSenderOutput.create(DEFAULT_N, rsArray);
        });
        // create a sender output with more rs
        Assert.assertThrows(AssertionError.class, () -> {
            byte[][][] rsArray = IntStream.range(0, MAX_NUM)
                .mapToObj(index ->
                    IntStream.range(0, DEFAULT_N + 1)
                        .mapToObj(choice -> {
                            byte[] ri = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                            SECURE_RANDOM.nextBytes(ri);
                            return ri;
                        })
                        .toArray(byte[][]::new))
                .toArray(byte[][][]::new);
            NotSenderOutput.create(DEFAULT_N, rsArray);
        });
        // merge two sender output with different n
        Assert.assertThrows(AssertionError.class, () -> {
            byte[][][] rsArray0 = IntStream.range(0, MAX_NUM)
                .mapToObj(index ->
                    IntStream.range(0, DEFAULT_N)
                        .mapToObj(choice -> {
                            byte[] ri = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                            SECURE_RANDOM.nextBytes(ri);
                            return ri;
                        })
                        .toArray(byte[][]::new))
                .toArray(byte[][][]::new);
            NotSenderOutput senderOutput0 = NotSenderOutput.create(DEFAULT_N, rsArray0);
            byte[][][] rsArray1 = IntStream.range(0, MAX_NUM)
                .mapToObj(index ->
                    IntStream.range(0, DEFAULT_N + 1)
                        .mapToObj(choice -> {
                            byte[] ri = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                            SECURE_RANDOM.nextBytes(ri);
                            return ri;
                        })
                        .toArray(byte[][]::new))
                .toArray(byte[][][]::new);
            NotSenderOutput senderOutput1 = NotSenderOutput.create(DEFAULT_N + 1, rsArray1);
            senderOutput0.merge(senderOutput1);
        });
    }

    @Test
    public void testIllegalReceiverOutputs() {
        // create a receiver output with num = 0
        Assert.assertThrows(AssertionError.class, () -> NotReceiverOutput.create(DEFAULT_N, new int[0], new byte[0][]));
        // create a receiver output with mismatched num
        Assert.assertThrows(AssertionError.class, () -> {
            int[] choices = IntStream.range(0, MIN_NUM)
                .map(index -> SECURE_RANDOM.nextInt(DEFAULT_N))
                .toArray();
            byte[][] rbArray = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] rb = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                    SECURE_RANDOM.nextBytes(rb);
                    return rb;
                })
                .toArray(byte[][]::new);
            NotReceiverOutput.create(DEFAULT_N, choices, rbArray);
        });
        // create a receiver with negative choice
        Assert.assertThrows(AssertionError.class, () -> {
            int[] choices = IntStream.range(0, MAX_NUM)
                .map(index -> -1)
                .toArray();
            byte[][] rbArray = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] rb = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                    SECURE_RANDOM.nextBytes(rb);
                    return rb;
                })
                .toArray(byte[][]::new);
            NotReceiverOutput.create(DEFAULT_N, choices, rbArray);
        });
        // create a receiver with large choice
        Assert.assertThrows(AssertionError.class, () -> {
            int[] choices = IntStream.range(0, MAX_NUM)
                .map(index -> DEFAULT_N)
                .toArray();
            byte[][] rbArray = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] rb = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                    SECURE_RANDOM.nextBytes(rb);
                    return rb;
                })
                .toArray(byte[][]::new);
            NotReceiverOutput.create(DEFAULT_N, choices, rbArray);
        });
        // create a receiver output with less rb
        Assert.assertThrows(AssertionError.class, () -> {
            int[] choices = IntStream.range(0, MAX_NUM)
                .map(index -> SECURE_RANDOM.nextInt(DEFAULT_N))
                .toArray();
            byte[][] rbArray = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] rb = new byte[CommonConstants.BLOCK_BYTE_LENGTH - 1];
                    SECURE_RANDOM.nextBytes(rb);
                    return rb;
                })
                .toArray(byte[][]::new);
            NotReceiverOutput.create(DEFAULT_N, choices, rbArray);
        });
        // create a receiver output with more rb
        Assert.assertThrows(AssertionError.class, () -> {
            int[] choices = IntStream.range(0, MAX_NUM)
                .map(index -> SECURE_RANDOM.nextInt(DEFAULT_N))
                .toArray();
            byte[][] rbArray = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] rb = new byte[CommonConstants.BLOCK_BYTE_LENGTH + 1];
                    SECURE_RANDOM.nextBytes(rb);
                    return rb;
                })
                .toArray(byte[][]::new);
            NotReceiverOutput.create(DEFAULT_N, choices, rbArray);
        });
        // merge two receiver output with different n
        Assert.assertThrows(AssertionError.class, () -> {
            int[] choices0 = IntStream.range(0, MAX_NUM)
                .map(index -> SECURE_RANDOM.nextInt(DEFAULT_N))
                .toArray();
            byte[][] rbArray0 = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] rb = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                    SECURE_RANDOM.nextBytes(rb);
                    return rb;
                })
                .toArray(byte[][]::new);
            NotReceiverOutput receiverOutput0 = NotReceiverOutput.create(DEFAULT_N, choices0, rbArray0);
            int[] choices1 = IntStream.range(0, MAX_NUM)
                .map(index -> SECURE_RANDOM.nextInt(DEFAULT_N + 1))
                .toArray();
            byte[][] rbArray1 = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> {
                    byte[] rb = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                    SECURE_RANDOM.nextBytes(rb);
                    return rb;
                })
                .toArray(byte[][]::new);
            NotReceiverOutput receiverOutput1 = NotReceiverOutput.create(DEFAULT_N + 1, choices1, rbArray1);
            receiverOutput0.merge(receiverOutput1);
        });
    }

    @Test
    public void testReduce() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testReduce(num);
        }
    }

    private void testReduce(int num) {
        // reduce 1
        NotSenderOutput senderOutput1 = NotTestUtils.genSenderOutput(num, DEFAULT_N, SECURE_RANDOM);
        NotReceiverOutput receiverOutput1 = NotTestUtils.genReceiverOutput(senderOutput1, SECURE_RANDOM);
        senderOutput1.reduce(1);
        receiverOutput1.reduce(1);
        NotTestUtils.assertOutput(1, DEFAULT_N, senderOutput1, receiverOutput1);
        // reduce all
        NotSenderOutput senderOutputAll = NotTestUtils.genSenderOutput(num, DEFAULT_N, SECURE_RANDOM);
        NotReceiverOutput receiverOutputAll = NotTestUtils.genReceiverOutput(senderOutputAll, SECURE_RANDOM);
        senderOutputAll.reduce(num);
        receiverOutputAll.reduce(num);
        NotTestUtils.assertOutput(num, DEFAULT_N, senderOutputAll, receiverOutputAll);
        if (num > 1) {
            // reduce num - 1
            NotSenderOutput senderOutputNum = NotTestUtils.genSenderOutput(num, DEFAULT_N, SECURE_RANDOM);
            NotReceiverOutput receiverOutputNum = NotTestUtils.genReceiverOutput(senderOutputNum, SECURE_RANDOM);
            senderOutputNum.reduce(num - 1);
            receiverOutputNum.reduce(num - 1);
            NotTestUtils.assertOutput(num - 1, DEFAULT_N, senderOutputNum, receiverOutputNum);
            // reduce half
            NotSenderOutput senderOutputHalf = NotTestUtils.genSenderOutput(num, DEFAULT_N, SECURE_RANDOM);
            NotReceiverOutput receiverOutputHalf = NotTestUtils.genReceiverOutput(senderOutputHalf, SECURE_RANDOM);
            senderOutputHalf.reduce(num / 2);
            receiverOutputHalf.reduce(num / 2);
            NotTestUtils.assertOutput(num / 2, DEFAULT_N, senderOutputHalf, receiverOutputHalf);
        }
    }

    @Test
    public void testAllEmptyMerge() {
        NotSenderOutput senderOutput = NotSenderOutput.createEmpty(DEFAULT_N);
        NotSenderOutput mergeSenderOutput = NotSenderOutput.createEmpty(DEFAULT_N);
        NotReceiverOutput receiverOutput = NotReceiverOutput.createEmpty(DEFAULT_N);
        NotReceiverOutput mergeReceiverOutput = NotReceiverOutput.createEmpty(DEFAULT_N);
        // merge
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // verify
        NotTestUtils.assertOutput(0, DEFAULT_N, senderOutput, receiverOutput);
    }

    @Test
    public void testLeftEmptyMerge() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testLeftEmptyMerge(num);
        }
    }

    private void testLeftEmptyMerge(int num) {
        NotSenderOutput senderOutput = NotSenderOutput.createEmpty(DEFAULT_N);
        NotSenderOutput mergeSenderOutput = NotTestUtils.genSenderOutput(num, DEFAULT_N, SECURE_RANDOM);
        NotReceiverOutput receiverOutput = NotReceiverOutput.createEmpty(DEFAULT_N);
        NotReceiverOutput mergeReceiverOutput = NotTestUtils.genReceiverOutput(mergeSenderOutput, SECURE_RANDOM);
        // merge
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // verify
        NotTestUtils.assertOutput(num, DEFAULT_N, senderOutput, receiverOutput);
    }

    @Test
    public void testRightEmptyMerge() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testRightEmptyMerge(num);
        }
    }

    private void testRightEmptyMerge(int num) {
        NotSenderOutput senderOutput = NotTestUtils.genSenderOutput(num, DEFAULT_N, SECURE_RANDOM);
        NotSenderOutput mergeSenderOutput = NotSenderOutput.createEmpty(DEFAULT_N);
        NotReceiverOutput receiverOutput = NotTestUtils.genReceiverOutput(senderOutput, SECURE_RANDOM);
        NotReceiverOutput mergeReceiverOutput = NotReceiverOutput.createEmpty(DEFAULT_N);
        // merge
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // verify
        NotTestUtils.assertOutput(num, DEFAULT_N, senderOutput, receiverOutput);
    }

    @Test
    public void testMerge() {
        for (int num1 = MIN_NUM; num1 < MAX_NUM; num1++) {
            for (int num2 = MIN_NUM; num2 < MAX_NUM; num2++) {
                testMerge(num1, num2);
            }
        }
    }

    private void testMerge(int num1, int num2) {
        NotSenderOutput senderOutput = NotTestUtils.genSenderOutput(num1, DEFAULT_N, SECURE_RANDOM);
        NotSenderOutput mergeSenderOutput = NotTestUtils.genSenderOutput(num2, DEFAULT_N, SECURE_RANDOM);
        NotReceiverOutput receiverOutput = NotTestUtils.genReceiverOutput(senderOutput, SECURE_RANDOM);
        NotReceiverOutput mergeReceiverOutput = NotTestUtils.genReceiverOutput(mergeSenderOutput, SECURE_RANDOM);
        // merge
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // verify
        NotTestUtils.assertOutput(num1 + num2, DEFAULT_N, senderOutput, receiverOutput);
    }

    @Test
    public void testSplit() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testSplit(num);
        }
    }

    private void testSplit(int num) {
        // split 1
        NotSenderOutput senderOutput1 = NotTestUtils.genSenderOutput(num, DEFAULT_N, SECURE_RANDOM);
        NotReceiverOutput receiverOutput1 = NotTestUtils.genReceiverOutput(senderOutput1, SECURE_RANDOM);
        NotSenderOutput splitSenderOutput1 = senderOutput1.split(1);
        NotReceiverOutput splitReceiverOutput1 = receiverOutput1.split(1);
        NotTestUtils.assertOutput(num - 1, DEFAULT_N, senderOutput1, receiverOutput1);
        NotTestUtils.assertOutput(1, DEFAULT_N, splitSenderOutput1, splitReceiverOutput1);
        // split all
        NotSenderOutput senderOutputAll = NotTestUtils.genSenderOutput(num, DEFAULT_N, SECURE_RANDOM);
        NotReceiverOutput receiverOutputAll = NotTestUtils.genReceiverOutput(senderOutputAll, SECURE_RANDOM);
        NotSenderOutput splitSenderOutputAll = senderOutputAll.split(num);
        NotReceiverOutput splitReceiverOutputAll = receiverOutputAll.split(num);
        NotTestUtils.assertOutput(0, DEFAULT_N, senderOutputAll, receiverOutputAll);
        NotTestUtils.assertOutput(num, DEFAULT_N, splitSenderOutputAll, splitReceiverOutputAll);
        if (num > 1) {
            // split num - 1
            NotSenderOutput senderOutputNum = NotTestUtils.genSenderOutput(num, DEFAULT_N, SECURE_RANDOM);
            NotReceiverOutput receiverOutputNum = NotTestUtils.genReceiverOutput(senderOutputNum, SECURE_RANDOM);
            NotSenderOutput splitSenderOutputNum = senderOutputNum.split(num - 1);
            NotReceiverOutput splitReceiverOutputNum = receiverOutputNum.split(num - 1);
            NotTestUtils.assertOutput(1, DEFAULT_N, senderOutputNum, receiverOutputNum);
            NotTestUtils.assertOutput(num - 1, DEFAULT_N, splitSenderOutputNum, splitReceiverOutputNum);
            // split half
            NotSenderOutput senderOutputHalf = NotTestUtils.genSenderOutput(num, DEFAULT_N, SECURE_RANDOM);
            NotReceiverOutput receiverOutputHalf = NotTestUtils.genReceiverOutput(senderOutputHalf, SECURE_RANDOM);
            NotSenderOutput splitSenderOutputHalf = senderOutputHalf.split(num / 2);
            NotReceiverOutput splitReceiverOutputHalf = receiverOutputHalf.split(num / 2);
            NotTestUtils.assertOutput(num - num / 2, DEFAULT_N, senderOutputHalf, receiverOutputHalf);
            NotTestUtils.assertOutput(num / 2, DEFAULT_N, splitSenderOutputHalf, splitReceiverOutputHalf);
        }
    }
}
