package edu.alibaba.mpc4j.s2pc.pcg.ot.lo.hot.kk13;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitmatrix.BitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.BitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.LotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.hot.AbstractLhotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.RcotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.RcotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.hot.kk13.Kk13OriLhotPtoDesc.PtoStep;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * KK13-LHOT协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/5/26
 */
public class Kk13OriLhotReceiver extends AbstractLhotReceiver {
    /**
     * COT协议发送方
     */
    private final RcotSender rcotSender;
    /**
     * 抗关联哈希函数
     */
    private final Crhf crhf;
    /**
     * COT协议发送方输出
     */
    private CotSenderOutput cotSenderOutput;
    /**
     * 布尔矩阵
     */
    private BitMatrix tMatrix;

    public Kk13OriLhotReceiver(Rpc receiverRpc, Party senderParty, Kk13OriLhotConfig config) {
        super(Kk13OriLhotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        rcotSender = RcotFactory.createSender(receiverRpc, senderParty, config.getRcotConfig());
        rcotSender.addLogLevel();
        crhf = CrhfFactory.createInstance(envType, CrhfFactory.CrhfType.MMO);
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        rcotSender.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        rcotSender.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        rcotSender.addLogLevel();
    }

    @Override
    public void init(int inputBitLength, int maxNum) throws MpcAbortException {
        setInitInput(inputBitLength, maxNum);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        byte[] cotDelta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(cotDelta);
        rcotSender.init(cotDelta, outputBitLength);
        cotSenderOutput = rcotSender.send(outputBitLength);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Recv. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public LotReceiverOutput receive(byte[][] choices) throws MpcAbortException {
        setPtoInput(choices);
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        List<byte[]> matrixPayload = generateMatrixPayload();
        DataPacketHeader matrixHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_MATRIX.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(matrixHeader, matrixPayload));
        LotReceiverOutput receiverOutput = generateReceiverOutput();
        stopWatch.stop();
        long keyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), keyGenTime);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return receiverOutput;
    }

    private List<byte[]> generateMatrixPayload() {
        // 初始化密码学原语
        Prg prg = PrgFactory.createInstance(envType, byteNum);
        tMatrix = BitMatrixFactory.createInstance(envType, num, outputBitLength, parallel);
        BitMatrix codeMatrix = BitMatrixFactory.createInstance(envType, outputBitLength, num, parallel);
        // 生成编码，不需要并发操作
        IntStream.range(0, num).forEach(index ->
            codeMatrix.setColumn(index, linearCoder.encode(
                BytesUtils.paddingByteArray(choices[index], linearCoder.getDatawordByteLength())
            ))
        );
        // 将此编码转置
        BitMatrix codeTransposeMatrix = codeMatrix.transpose();
        // 各个列加密
        IntStream tMatrixIntStream = IntStream.range(0, outputBitLength);
        tMatrixIntStream = parallel ? tMatrixIntStream.parallel() : tMatrixIntStream;
        return tMatrixIntStream
            .mapToObj(columnIndex -> {
                // The receiver forms m \times k matrices T_0, T_1 such that t_{j, 0} \oplus t_{j, 1} = C(r_j)
                byte[] columnSeed = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                secureRandom.nextBytes(columnSeed);
                byte[] column0Bytes = prg.extendToBytes(columnSeed);
                BytesUtils.reduceByteArray(column0Bytes, num);
                tMatrix.setColumn(columnIndex, column0Bytes);
                byte[] column1Bytes = BytesUtils.xor(column0Bytes, codeTransposeMatrix.getColumn(columnIndex));
                byte[] message0 = prg.extendToBytes(crhf.hash(cotSenderOutput.getR0(columnIndex)));
                BytesUtils.reduceByteArray(message0, num);
                BytesUtils.xori(message0, column0Bytes);
                byte[] message1 = prg.extendToBytes(crhf.hash(cotSenderOutput.getR1(columnIndex)));
                BytesUtils.reduceByteArray(message1, num);
                BytesUtils.xori(message1, column1Bytes);
                // The sender and the receiver interact with OT^k_m: the receiver acts as OT sender with input t_0, t_1
                return new byte[][]{message0, message1};
            })
            .flatMap(Arrays::stream)
            .collect(Collectors.toList());
    }

    private LotReceiverOutput generateReceiverOutput() {
        // 生成密钥数组，将矩阵T转置，按行获取
        BitMatrix tMatrixTranspose = tMatrix.transpose();
        tMatrix = null;
        byte[][] rbArray = IntStream.range(0, num)
            .mapToObj(tMatrixTranspose::getColumn)
            .toArray(byte[][]::new);

        return LotReceiverOutput.create(inputBitLength, outputBitLength, choices, rbArray);
    }
}
