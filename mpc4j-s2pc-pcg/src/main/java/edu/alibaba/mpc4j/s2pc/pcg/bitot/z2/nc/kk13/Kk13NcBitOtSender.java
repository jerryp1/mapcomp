package edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.nc.kk13;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.BitOtSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.nc.AbstractNcBitOtSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.LotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.nc.NcLotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.nc.NcLotSender;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * KK13-NC-BitOt发送方。
 *
 * @author Hanwen Feng
 * @date 2022/08/18
 */
public class Kk13NcBitOtSender extends AbstractNcBitOtSender {
    /**
     * NC-LOT协议发送方
     */
    private final NcLotSender ncLotSender;
    /**
     * NC-LOT协议对应的l值
     */
    private final int l;
    /**
     * 需要生成LOT的数量
     */
    private int lotNum;
    /**
     * 实际生成的BitOT数量是l的整数倍
     */
    private int roundNum;
    /**
     * r0数组
     */
    private byte[] r0Array;
    /**
     * r1数组
     */
    private byte[] r1Array;
    /**
     * 抗关联函数
     */
    private final Kdf kdf;


    public Kk13NcBitOtSender(Rpc senderRpc, Party receiverParty, Kk13NcBitOtConfig config) {
        super(Kk13NcBitOtPtoDesc.getInstance(), senderRpc, receiverParty, config);
        ncLotSender = NcLotFactory.createSender(senderRpc, receiverParty, config.getNcLotConfig());
        kdf = KdfFactory.createInstance(getEnvType());
        l = config.getL();
        ncLotSender.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        ncLotSender.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        ncLotSender.setParallel(parallel);
    }


    @Override
    public void addLogLevel() {
        super.addLogLevel();
        ncLotSender.addLogLevel();
    }

    @Override
    public void init(int num) throws MpcAbortException {
        setInitInput(num);
        lotNum = (int) Math.ceil((double) num / (double) l);
        roundNum = lotNum * l;
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        ncLotSender.init(l, lotNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 1/1 ({} ms)", ptoBeginLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Send. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public BitOtSenderOutput send() throws MpcAbortException {
        setPtoInput();
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        // 执行LOT协议
        stopWatch.start();
        LotSenderOutput lotSenderOutput = ncLotSender.send();
        stopWatch.stop();
        long lotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 1/2 ({} ms)", ptoBeginLogPrefix, getPtoDesc().getPtoName(), lotTime);

        // 使用LOT协议获得的密钥加密BitOT输出并发送
        stopWatch.start();
        DataPacketHeader sHeader = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), Kk13NcBitOtPtoDesc.PtoStep.SENDER_SEND_CIPHER.ordinal(),
                extraInfo, ownParty().getPartyId(), otherParty().getPartyId()
        );
        List<byte[]> sPayload = generatePayload(lotSenderOutput);
        rpc.send(DataPacket.fromByteArrayList(sHeader, sPayload));
        BitOtSenderOutput output = BitOtSenderOutput.create(roundNum, r0Array, r1Array);
        output.reduce(num);
        stopWatch.stop();
        long sTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 2/2 ({} ms)", ptoBeginLogPrefix, getPtoDesc().getPtoName(), sTime);

        r0Array = null;
        r1Array = null;
        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());

        return output;
    }

    private List<byte[]> generatePayload(LotSenderOutput lotSenderOutput) {
        // 生成随机数组R0和R1， 实际产生的BitOt数量是l的整数倍
        int roundByteNum = CommonUtils.getByteLength(roundNum);
        int offset = roundByteNum * Byte.SIZE - roundNum;
        r0Array = new byte[roundByteNum];
        r1Array = new byte[roundByteNum];
        secureRandom.nextBytes(r0Array);
        secureRandom.nextBytes(r1Array);
        BytesUtils.reduceByteArray(r0Array, roundNum);
        BytesUtils.reduceByteArray(r1Array, roundNum);

        boolean[] r0BooleanArray = BinaryUtils.byteArrayToBinary(r0Array, roundNum);
        boolean[] r1BooleanArray = BinaryUtils.byteArrayToBinary(r1Array, roundNum);
        // 读取存储l的字节数和偏置量
        int lByteNum = lotSenderOutput.getInputByteLength();
        // 将R0和R1的每l比特排列为2^l条消息
       IntStream encodeStream = IntStream.range(0, lotNum);
       encodeStream = parallel ? encodeStream.parallel() : encodeStream;
        return encodeStream.mapToObj(index -> {
                    // 拷贝对应位置的r0和r1
                    boolean[] l0 = new boolean[l];
                    boolean[] l1 = new boolean[l];
                    System.arraycopy(r0BooleanArray, index * l, l0, 0, l);
                    System.arraycopy(r1BooleanArray, index * l, l1, 0, l);
                    byte[] l0ByteArray = BinaryUtils.binaryToRoundByteArray(l0);
                    byte[] shift = BytesUtils.xor(l0ByteArray, BinaryUtils.binaryToRoundByteArray(l1));
                    // 创建存储2^l条消息的数组
                    byte[][] encodeMessages = new byte[1 << l][];
                    for (int i = 0; i < 1 << l; i++) {
                        byte[] choice = BigIntegerUtils.nonNegBigIntegerToByteArray(BigInteger.valueOf(i), lByteNum);
                        // 使用choice对应密钥加密消息
                        byte[] key = Arrays.copyOf(kdf.deriveKey(lotSenderOutput.getRb(index, choice)), lByteNum);
                        encodeMessages[i] = BytesUtils.xor(
                                key, BytesUtils.xor(l0ByteArray, BytesUtils.and(choice, shift))
                        );
                    }
                    return encodeMessages;
                })
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
    }

}
